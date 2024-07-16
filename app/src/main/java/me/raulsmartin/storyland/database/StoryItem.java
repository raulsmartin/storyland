package me.raulsmartin.storyland.database;

import android.content.ContentValues;
import android.database.Cursor;

public class StoryItem {
    private long id;
    private long storyId;
    private int sequence;
    private ActionType action;
    private int player;
    private int imageId;
    private String message;
    private String option1;
    private String option2;
    private int option1Sequence;
    private int option2Sequence;
    private int nextSequence;

    public StoryItem(long storyId, ActionType action) {
        this(storyId, action, -1, null);
    }

    public StoryItem(long storyId, ActionType action, int player, String message) {
        this.storyId = storyId;
        this.action = action;
        this.player = player;
        this.message = message;
    }

    public StoryItem(long storyId, int sequence, ActionType action, int player, int imageId, String message, String option1, String option2, int option1Sequence, int option2Sequence, int nextSequence) {
        this.storyId = storyId;
        this.sequence = sequence;
        this.action = action;
        this.player = player;
        this.imageId = imageId;
        this.message = message;
        this.option1 = option1;
        this.option2 = option2;
        this.option1Sequence = option1Sequence;
        this.option2Sequence = option2Sequence;
        this.nextSequence = nextSequence;
    }

    private StoryItem() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getStoryId() {
        return storyId;
    }

    public void setStoryId(long storyId) {
        this.storyId = storyId;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public int getPlayer() {
        return player;
    }

    public void setPlayer(int player) {
        this.player = player;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOption1() {
        return option1;
    }

    public void setOption1(String option1) {
        this.option1 = option1;
    }

    public String getOption2() {
        return option2;
    }

    public void setOption2(String option2) {
        this.option2 = option2;
    }

    public int getOption1Sequence() {
        return option1Sequence;
    }

    public void setOption1Sequence(int option1Sequence) {
        this.option1Sequence = option1Sequence;
    }

    public int getOption2Sequence() {
        return option2Sequence;
    }

    public void setOption2Sequence(int option2Sequence) {
        this.option2Sequence = option2Sequence;
    }

    public int getNextSequence() {
        return nextSequence;
    }

    public void setNextSequence(int nextSequence) {
        this.nextSequence = nextSequence;
    }

    public String prepareToSend() {
        String msg = sequence + "|" + action.ordinal() + "|" + player + "|" + imageId + "|";
        if (message == null || message.isEmpty()) msg += 0 + "|";
        else msg += message.length() + "|" + message + "|";
        if (option1 == null || option1.isEmpty()) msg += 0 + "|";
        else msg += option1.length() + "|" + option1 + "|";
        if (option2 == null || option2.isEmpty()) msg += 0 + "|";
        else msg += option2.length() + "|" + option2 + "|";
        msg += option1Sequence + "|" + option2Sequence + "|" + nextSequence;
        return msg;
    }

    public ContentValues getValues() {
        ContentValues values = new ContentValues();
        values.put(DatabaseHandler.COLUMN_STORY_ID, storyId);
        values.put(DatabaseHandler.COLUMN_SEQUENCE, sequence);
        values.put(DatabaseHandler.COLUMN_ACTION_ID, action.ordinal());
        values.put(DatabaseHandler.COLUMN_PLAYER, player);
        values.put(DatabaseHandler.COLUMN_IMAGE_ID, imageId);
        values.put(DatabaseHandler.COLUMN_MESSAGE, message);
        values.put(DatabaseHandler.COLUMN_OPTION1, option1);
        values.put(DatabaseHandler.COLUMN_OPTION2, option2);
        values.put(DatabaseHandler.COLUMN_OPTION1_SEQUENCE, option1Sequence);
        values.put(DatabaseHandler.COLUMN_OPTION2_SEQUENCE, option2Sequence);
        values.put(DatabaseHandler.COLUMN_NEXT_SEQUENCE, nextSequence);
        return values;
    }

    public static StoryItem from(Cursor c) throws IllegalArgumentException {
        StoryItem item = new StoryItem();
        item.setId(c.getLong(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_ID)));
        item.setStoryId(c.getLong(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_STORY_ID)));
        item.setSequence(c.getInt(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_SEQUENCE)));
        item.setAction(ActionType.from(c.getInt(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_ACTION_ID))));
        item.setPlayer(c.getInt(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_PLAYER)));
        item.setImageId(c.getInt(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_IMAGE_ID)));
        item.setMessage(c.getString(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_MESSAGE)));
        item.setOption1(c.getString(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_OPTION1)));
        item.setOption2(c.getString(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_OPTION2)));
        item.setOption1Sequence(c.getInt(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_OPTION1_SEQUENCE)));
        item.setOption2Sequence(c.getInt(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_OPTION2_SEQUENCE)));
        item.setNextSequence(c.getInt(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_NEXT_SEQUENCE)));
        return item;
    }

    public static StoryItem from(String buffer) {
        StoryItem item = new StoryItem();
        int pos = 0;

        String[] items = buffer.split("\\|");
        try {
            item.setSequence(Integer.parseInt(items[pos]));
            pos++;
            item.setAction(ActionType.from(Integer.parseInt(items[pos])));
            pos++;
            item.setPlayer(Integer.parseInt(items[pos]));
            pos++;
            item.setImageId(Integer.parseInt(items[pos]));
            pos++;
            if (Integer.parseInt(items[pos]) != 0) {
                pos++;
                item.setMessage(items[pos]);
            }
            pos++;
            if (Integer.parseInt(items[pos]) != 0) {
                pos++;
                item.setOption1(items[pos]);
            }
            pos++;
            if (Integer.parseInt(items[pos]) != 0) {
                pos++;
                item.setOption2(items[pos]);
            }
            pos++;
            item.setOption1Sequence(Integer.parseInt(items[pos]));
            pos++;
            item.setOption2Sequence(Integer.parseInt(items[pos]));
            pos++;
            item.setNextSequence(Integer.parseInt(items[pos]));
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException | NullPointerException exception) {
            item = null;
        }
        return item;
    }
}
