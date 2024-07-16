package me.raulsmartin.storyland.database;

import androidx.annotation.StringRes;

import me.raulsmartin.storyland.R;

public enum ActionType {
    START(R.string.start_name), TALK(R.string.talk_name), SHAKE(R.string.shake_name), WAIT(R.string.wait_name), DECISION(R.string.decision_name), CONTINUE(R.string.continue_name), FINISH(R.string.finish_name);

    @StringRes
    private final int name;

    ActionType(@StringRes int name) {
        this.name = name;
    }

    @StringRes
    public int getName() {
        return name;
    }

    public static ActionType from(int ordinal) {
        if (ordinal < values().length) {
            return values()[ordinal];
        }

        return START;
    }
}
