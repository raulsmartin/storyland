package me.raulsmartin.storyland.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {
    public static final String COLUMN_ID = "_id";

    public static final String STORIES_TABLE_NAME = "stories";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_PLAYERS = "players";

    public static final String STORY_SEQUENCE_TABLE_NAME = "story_sequence";
    public static final String COLUMN_STORY_ID = "story_id";
    public static final String COLUMN_SEQUENCE = "sequence";
    public static final String COLUMN_PLAYER = "player";
    public static final String COLUMN_IMAGE_ID = "image_id";
    public static final String COLUMN_ACTION_ID = "action_id";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_OPTION1 = "option1";
    public static final String COLUMN_OPTION2 = "option2";
    public static final String COLUMN_OPTION1_SEQUENCE = "option1_sequence";
    public static final String COLUMN_OPTION2_SEQUENCE = "option2_sequence";
    public static final String COLUMN_NEXT_SEQUENCE = "next_sequence";

    public static final String PLAYERS_TABLE_NAME = "players";
    public static final String COLUMN_PLAYER_NAME = "player_name";
    public static final String COLUMN_VOICE_NAME = "voice_name";
    public static final String COLUMN_VOICE_PITCH = "voice_pitch";
    public static final String COLUMN_VOICE_SPEED = "voice_speed";

    //information of database
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "storylandDB.db";

    public DatabaseHandler(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_STORIES_TABLE = "CREATE TABLE " + STORIES_TABLE_NAME + "(" + COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + COLUMN_TITLE +
                " TEXT NOT NULL UNIQUE, " + COLUMN_PLAYERS + " INTEGER NOT NULL DEFAULT 2)";
        db.execSQL(CREATE_STORIES_TABLE);

        String CREATE_STORY_SEQUENCE_TABLE = "CREATE TABLE " + STORY_SEQUENCE_TABLE_NAME + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + COLUMN_STORY_ID +
                " INTEGER NOT NULL, " + COLUMN_SEQUENCE + " INTEGER NOT NULL, " + COLUMN_PLAYER +
                " INTEGER NOT NULL DEFAULT -1, " + COLUMN_IMAGE_ID + " INTEGER NOT NULL DEFAULT -1, " +
                COLUMN_ACTION_ID + " INTEGER NOT NULL DEFAULT 0, " + COLUMN_MESSAGE +
                " TEXT DEFAULT NULL, " + COLUMN_OPTION1 + " TEXT DEFAULT NULL, " + COLUMN_OPTION2 +
                " TEXT DEFAULT NULL, " + COLUMN_OPTION1_SEQUENCE + " INTEGER NOT NULL DEFAULT -1, " +
                COLUMN_OPTION2_SEQUENCE + " INTEGER NOT NULL DEFAULT -1, " + COLUMN_NEXT_SEQUENCE +
                " INTEGER NOT NULL DEFAULT -1)";
        db.execSQL(CREATE_STORY_SEQUENCE_TABLE);

        String CREATE_PLAYERS_TABLE = "CREATE TABLE " + PLAYERS_TABLE_NAME + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + COLUMN_PLAYER_NAME +
                " TEXT NOT NULL, " + COLUMN_VOICE_NAME + " TEXT DEFAULT NULL, " + COLUMN_VOICE_PITCH +
                " REAL NOT NULL DEFAULT 1.0, " + COLUMN_VOICE_SPEED + " REAL NOT NULL DEFAULT 1.0)";
        db.execSQL(CREATE_PLAYERS_TABLE);

        StoryInfo info = new StoryInfo("Una tarde en el parque", 2);
        long storyId = db.insert(STORIES_TABLE_NAME, "", info.getValues());
        if (storyId != -1) {
            List<StoryItem> items = new ArrayList<>();
            // | HILO INICIAL
            items.add(new StoryItem(storyId, 0, ActionType.START, -1, 0, null, null, null, -1, -1, 1));
            items.add(new StoryItem(storyId, 1, ActionType.TALK, 0, 1, "Oye %player2%, ¿puedo jugar contigo?", null, null, -1, -1, 2));
            items.add(new StoryItem(storyId, 2, ActionType.TALK, 1, -1, "Claro %player1%, estoy recolectando flores para hacerle un ramo a mi madre", null, null, -1, -1, 3));
            items.add(new StoryItem(storyId, 3, ActionType.TALK, 0, -1, "¡Qué guay! ¿Cuantas llevas ya?", null, null, -1, -1, 4));
            items.add(new StoryItem(storyId, 4, ActionType.TALK, 1, -1, "Pues llevo unas... Oye, ¿qué es eso?", null, null, -1, -1, 5));
            items.add(new StoryItem(storyId, 5, ActionType.WAIT, -1, -1, null, null, null, -1, -1, 6));
            items.add(new StoryItem(storyId, 6, ActionType.SHAKE, 0, 2, "¡ES UNA ABEJA! ¡HAY QUE CORRER!", null, null, 7, 18, -1));

            // |- HILO SACUDE EL MÓVIL
            items.add(new StoryItem(storyId, 7, ActionType.TALK, 1, 3, "¡Pero si no son para tanto! Mi padre es apicultor y me dijo que son buenas", null, null, -1, -1, 8));
            items.add(new StoryItem(storyId, 8, ActionType.TALK, 0, -1, "¡A mi me dan miedo igual! Ya estoy corriendo a casa", null, null, -1, -1, 9));
            items.add(new StoryItem(storyId, 9, ActionType.DECISION, 1, -1, "¡Espera!", "Acompañarle a casa", "Quedarte jugando sólo", 10, 13, -1));
            // |-- ACOMPAÑAR A CASA
            items.add(new StoryItem(storyId, 10, ActionType.TALK, 1, -1, "Te acompaño a casa que no me quiero quedar sólo...", null, null, -1, -1, 11));
            items.add(new StoryItem(storyId, 11, ActionType.TALK, 0, -1, "Venga, pero no tardes mucho, que mi madre está esperando", null, null, -1, -1, 12));
            items.add(new StoryItem(storyId, 12, ActionType.CONTINUE, -1, -1, null, null, null, -1, -1, 21));
            // |-- QUEDARSE PARQUE
            items.add(new StoryItem(storyId, 13, ActionType.TALK, 1, -1, "Me voy a quedar un ratito más en el parque", null, null, -1, -1, 14));
            items.add(new StoryItem(storyId, 14, ActionType.TALK, 0, -1, "Okay, ¡pásalo guay!", null, null, -1, -1, 15));
            items.add(new StoryItem(storyId, 15, ActionType.WAIT, -1, -1, null, null, null, -1, -1, 16));
            items.add(new StoryItem(storyId, 16, ActionType.TALK, 1, -1, "En verdad me voy a marchar a casa, que ya es tarde", null, null, -1, -1, 17));
            items.add(new StoryItem(storyId, 17, ActionType.CONTINUE, -1, -1, null, null, null, -1, -1, 21));

            // |- HILO NO SACUDE EL MOVIL
            items.add(new StoryItem(storyId, 18, ActionType.TALK, 0, -1, "¡ME HA PICADO AAAAUUUU!", null, null, -1, -1, 19));
            items.add(new StoryItem(storyId, 19, ActionType.TALK, 1, -1, "¡Vámonos al agua! Ellas no pueden nadar", null, null, -1, -1, 20));
            items.add(new StoryItem(storyId, 20, ActionType.CONTINUE, -1, -1, null, null, null, -1, -1, 21));

            // | JOIN DE TODAS LAS RAMAS
            items.add(new StoryItem(storyId, 21, ActionType.FINISH, -1, -1, null, null, null, -1, -1, -1));
            for (StoryItem item : items) {
                db.insert(STORY_SEQUENCE_TABLE_NAME, "", item.getValues());
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + STORIES_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + STORY_SEQUENCE_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PLAYERS_TABLE_NAME);
        onCreate(db);
    }
}
