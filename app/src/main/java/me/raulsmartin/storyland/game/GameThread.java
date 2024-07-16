package me.raulsmartin.storyland.game;

import android.os.Handler;

import java.util.List;

import me.raulsmartin.storyland.database.ActionType;
import me.raulsmartin.storyland.database.StoryItem;

public class GameThread extends Thread {
    private final Handler activityHandler;

    private List<StoryItem> storySequence;
    private StoryItem currentSequence;
    private volatile boolean talking;
    private volatile int shaken;
    private volatile int option;

    public GameThread(Handler handler, StoryItem startSequence, List<StoryItem> storySequence) {
        this.activityHandler = handler;
        this.currentSequence = startSequence;
        this.storySequence = storySequence;
        this.talking = false;
        this.shaken = -1;
        this.option = -1;
    }

    @Override
    public void run() {
        while (!currentSequence.getAction().equals(ActionType.FINISH)) {
            talking = false;
            if (currentSequence.getMessage() != null) {
                talking = true;
                activityHandler.obtainMessage(GameMessage.TALK.ordinal(), currentSequence.getPlayer(), -1, currentSequence.getMessage()).sendToTarget();
            }

            if (currentSequence.getAction().equals(ActionType.SHAKE)) {
                shaken = -1;
                long start = System.currentTimeMillis();
                while (shaken == -1){
                    if (System.currentTimeMillis() - start > 10000){
                        shaken = 2;
                    }
                }
                if (shaken == 1) {
                    currentSequence.setNextSequence(currentSequence.getOption1Sequence());
                } else {
                    currentSequence.setNextSequence(currentSequence.getOption2Sequence());
                }
            } else if (currentSequence.getAction().equals(ActionType.DECISION)) {
                option = -1;
                while (option == -1);
                if (option == 1) {
                    currentSequence.setNextSequence(currentSequence.getOption1Sequence());
                } else if (option == 2) {
                    currentSequence.setNextSequence(currentSequence.getOption2Sequence());
                }
            } else if (currentSequence.getAction().equals(ActionType.WAIT)) {
                boolean wait = true;
                long start = System.currentTimeMillis();
                while (wait){
                    if (System.currentTimeMillis() - start > 2000){
                        wait = false;
                    }
                }
            }

            while (talking);

            if (currentSequence.getNextSequence() != -1) {
                int player = currentSequence.getPlayer();
                for (StoryItem item : storySequence) {
                    if (item.getSequence() == currentSequence.getNextSequence()) {
                        currentSequence = item;
                        break;
                    }
                }
                activityHandler.obtainMessage(GameMessage.NEXT.ordinal(), player, -1, currentSequence).sendToTarget();
            }

            if (currentSequence.getImageId() != -1) {
                activityHandler.obtainMessage(GameMessage.UPDATE_IMAGE.ordinal(), currentSequence.getImageId()).sendToTarget();
            }
        }
    }

    public synchronized void optionSelected(int option) {
        this.option = option;
    }

    public synchronized void finishedTalking() {
        this.talking = false;
    }

    public synchronized void hasShaken() {
        this.shaken = 1;
    }

    public synchronized void continueGame(int nextSequence) {
        finishedTalking();
        hasShaken();
        optionSelected(0);
        currentSequence.setNextSequence(nextSequence);
    }

    public void cancel() {
        interrupt();
    }
}
