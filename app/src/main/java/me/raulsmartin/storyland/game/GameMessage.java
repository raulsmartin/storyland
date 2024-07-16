package me.raulsmartin.storyland.game;

public enum GameMessage {
    NONE, TALK, FIRST, NEXT, WAITED, UPDATE_IMAGE;

    public static GameMessage from(int ordinal) {
        if (ordinal < values().length) {
            return values()[ordinal];
        }

        return NONE;
    }
}
