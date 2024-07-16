package me.raulsmartin.storyland.database;

import android.content.ContentValues;
import android.database.Cursor;

public class StoryInfo {
    private long id;
    private String title;
    private int players;

    public StoryInfo(String title, int players) {
        this.title = title;
        this.players = players;
    }

    private StoryInfo() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPlayers() {
        return players;
    }

    public void setPlayers(int players) {
        this.players = players;
    }

    public ContentValues getValues() {
        ContentValues values = new ContentValues();
        values.put(DatabaseHandler.COLUMN_TITLE, title);
        values.put(DatabaseHandler.COLUMN_PLAYERS, players);
        return values;
    }

    public String prepareToSend() {
        return title + "|" + players;
    }

    public static StoryInfo from(Cursor c) throws IllegalArgumentException {
        StoryInfo info = new StoryInfo();
        info.setId(c.getLong(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_ID)));
        info.setTitle(c.getString(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_TITLE)));
        info.setPlayers(c.getInt(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_PLAYERS)));
        return info;
    }

    public static StoryInfo from(String buffer) {
        StoryInfo info = null;
        String[] items = buffer.split("\\|");
        if (items.length == 2) {
            try {
                info = new StoryInfo(items[0], Integer.parseInt(items[1]));
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException | NullPointerException exception) {
                System.out.println("Invalid story info: " + buffer);
            }
        }
        return info;
    }
}
