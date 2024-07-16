package me.raulsmartin.storyland;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.HashMap;

import me.raulsmartin.storyland.database.DatabaseHandler;

public class StoryProvider extends ContentProvider {
    public static final String PROVIDER_NAME = "me.raulsmartin.storyland.provider";
    public static final String STORIES_URL = "content://" + PROVIDER_NAME + "/" + DatabaseHandler.STORIES_TABLE_NAME;
    public static final String PLAYERS_URL = "content://" + PROVIDER_NAME + "/" + DatabaseHandler.PLAYERS_TABLE_NAME;
    public static final String STORY_SEQUENCE_URL = "content://" + PROVIDER_NAME + "/" + DatabaseHandler.STORY_SEQUENCE_TABLE_NAME;
    public static final Uri STORIES_CONTENT_URI = Uri.parse(STORIES_URL);
    public static final Uri PLAYERS_CONTENT_URI = Uri.parse(PLAYERS_URL);
    public static final Uri STORY_SEQUENCE_CONTENT_URI = Uri.parse(STORY_SEQUENCE_URL);


    private static HashMap<String, String> STORIES_PROJECTION_MAP;
    private static HashMap<String, String> PLAYERS_PROJECTION_MAP;
    private static HashMap<String, String> STORY_SEQUENCE_PROJECTION_MAP;

    static final int STORIES = 1;
    static final int PLAYERS = 2;
    static final int STORY_SEQUENCE = 3;
    static final int STORY_ID = 4;
    static final int STORY_SEQUENCE_ID = 5;

    static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, DatabaseHandler.STORIES_TABLE_NAME, STORIES);
        uriMatcher.addURI(PROVIDER_NAME, DatabaseHandler.PLAYERS_TABLE_NAME, PLAYERS);
        uriMatcher.addURI(PROVIDER_NAME, DatabaseHandler.STORY_SEQUENCE_TABLE_NAME, STORY_SEQUENCE);
        uriMatcher.addURI(PROVIDER_NAME, DatabaseHandler.STORIES_TABLE_NAME + "/#", STORY_ID);
        uriMatcher.addURI(PROVIDER_NAME, DatabaseHandler.STORY_SEQUENCE_TABLE_NAME + "/#", STORY_SEQUENCE_ID);
    }

    private SQLiteDatabase db;
    private DatabaseHandler dbHandler;

    public StoryProvider() {
    }

    @Override
    public boolean onCreate() {
        dbHandler = new DatabaseHandler(getContext());

        db = dbHandler.getWritableDatabase();
        return db != null;
    }

    @Override
    public void shutdown() {
        super.shutdown();

        if (db != null && db.isOpen()) {
            db.close();
        }

        if (dbHandler != null) {
            dbHandler.close();
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case STORIES:
                qb.setTables(DatabaseHandler.STORIES_TABLE_NAME);
                qb.setProjectionMap(STORIES_PROJECTION_MAP);
                break;
            case PLAYERS:
                qb.setTables(DatabaseHandler.PLAYERS_TABLE_NAME);
                qb.setProjectionMap(PLAYERS_PROJECTION_MAP);
                break;
            case STORY_SEQUENCE:
                qb.setTables(DatabaseHandler.STORY_SEQUENCE_TABLE_NAME);
                qb.setProjectionMap(STORY_SEQUENCE_PROJECTION_MAP);
                break;
            case STORY_ID:
                qb.setTables(DatabaseHandler.STORIES_TABLE_NAME);
                qb.appendWhere(DatabaseHandler.COLUMN_ID + " = " + uri.getPathSegments().get(1));
            case STORY_SEQUENCE_ID:
                qb.setTables(DatabaseHandler.STORY_SEQUENCE_TABLE_NAME);
                qb.appendWhere(DatabaseHandler.COLUMN_ID + " = " + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = DatabaseHandler.COLUMN_ID;
        }

        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        if (getContext() != null) c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long rowID;
        switch (uriMatcher.match(uri)) {
            case STORIES:
                rowID = db.insert(DatabaseHandler.STORIES_TABLE_NAME, "", values);
                if (rowID > 0) {
                    Uri _uri = ContentUris.withAppendedId(STORIES_CONTENT_URI, rowID);
                    if (getContext() != null)
                        getContext().getContentResolver().notifyChange(_uri, null);
                    return _uri;
                }
                break;
            case PLAYERS:
                rowID = db.insert(DatabaseHandler.PLAYERS_TABLE_NAME, "", values);
                if (rowID > 0) {
                    if (getContext() != null)
                        getContext().getContentResolver().notifyChange(uri, null);
                    return uri;
                }
                break;
            case STORY_SEQUENCE:
                rowID = db.insert(DatabaseHandler.STORY_SEQUENCE_TABLE_NAME, "", values);
                if (rowID > 0) {
                    Uri _uri = ContentUris.withAppendedId(STORY_SEQUENCE_CONTENT_URI, rowID);
                    if (getContext() != null)
                        getContext().getContentResolver().notifyChange(_uri, null);
                    return _uri;
                }
                break;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count;
        switch (uriMatcher.match(uri)) {
            case STORIES:
                count = db.update(DatabaseHandler.STORIES_TABLE_NAME, values, selection, selectionArgs);
                break;
            case PLAYERS:
                count = db.update(DatabaseHandler.PLAYERS_TABLE_NAME, values, selection, selectionArgs);
                break;
            case STORY_SEQUENCE:
                count = db.update(DatabaseHandler.STORY_SEQUENCE_TABLE_NAME, values, selection, selectionArgs);
                break;
            case STORY_ID:
                count = db.update(DatabaseHandler.STORIES_TABLE_NAME, values, DatabaseHandler.COLUMN_ID + " = " + uri.getPathSegments().get(1) + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""), selectionArgs);
                break;
            case STORY_SEQUENCE_ID:
                count = db.update(DatabaseHandler.STORY_SEQUENCE_TABLE_NAME, values, DatabaseHandler.COLUMN_ID + " = " + uri.getPathSegments().get(1) + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int count;
        switch (uriMatcher.match(uri)) {
            case STORIES:
                count = db.delete(DatabaseHandler.STORIES_TABLE_NAME, selection, selectionArgs);
                break;
            case PLAYERS:
                count = db.delete(DatabaseHandler.PLAYERS_TABLE_NAME, selection, selectionArgs);
                break;
            case STORY_SEQUENCE:
                count = db.delete(DatabaseHandler.STORY_SEQUENCE_TABLE_NAME, selection, selectionArgs);
                break;
            case STORY_ID:
                count = db.delete(DatabaseHandler.STORIES_TABLE_NAME, DatabaseHandler.COLUMN_ID + " = " + uri.getPathSegments().get(1) + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""), selectionArgs);
                break;
            case STORY_SEQUENCE_ID:
                count = db.delete(DatabaseHandler.STORY_SEQUENCE_TABLE_NAME, DatabaseHandler.COLUMN_ID + " = " + uri.getPathSegments().get(1) + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case STORIES:
                return "vnd.android.cursor.dir/vnd." + PROVIDER_NAME + "." + DatabaseHandler.STORIES_TABLE_NAME;
            case PLAYERS:
                return "vnd.android.cursor.dir/vnd." + PROVIDER_NAME + "." + DatabaseHandler.PLAYERS_TABLE_NAME;
            case STORY_SEQUENCE:
                return "vnd.android.cursor.dir/vnd." + PROVIDER_NAME + "." + DatabaseHandler.STORY_SEQUENCE_TABLE_NAME;
            case STORY_ID:
                return "vnd.android.cursor.item/vnd." + PROVIDER_NAME + "." + DatabaseHandler.STORIES_TABLE_NAME;
            case STORY_SEQUENCE_ID:
                return "vnd.android.cursor.item/vnd." + PROVIDER_NAME + "." + DatabaseHandler.STORY_SEQUENCE_TABLE_NAME;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}