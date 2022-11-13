package net.kenevans.eventtimer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

/**
 * Simple database access helper class, modified from the Notes example
 * application.
 */
public class EventTimerDbAdapter implements IConstants {
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mCtx;

    /**
     * Database creation SQL statement
     */
    private static final String DB_CREATE_DATA_TABLE_SESSIONS = "create table "
            + DB_DATA_TABLE_SESSIONS + " (_id integer primary key " +
            "autoincrement, "
            + COL_CREATE_TIME + " integer not null, "
            + COL_NAME + " text not null);";

    private static final String DB_CREATE_DATA_TABLE_EVENTS = "create table "
            + DB_DATA_TABLE_EVENTS + " (_id integer primary key autoincrement, "
            + COL_TIME + " integer not null, "
            + COL_NOTE + " text not null, "
            + COL_SESSION_ID + " integer not null);";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx The context.
     */
    public EventTimerDbAdapter(Context ctx) {
        mCtx = ctx;
    }

    /**
     * Open the database. If it cannot be opened, try to create a new instance
     * of the database. If it cannot be created, throw an exception to signal
     * the failure
     *
     * @return this (self reference, allowing this to be chained in an
     * initialization call).
     * @throws SQLException if the database could be neither opened or created.
     */
    public EventTimerDbAdapter open() throws SQLException {
        // Make sure the directory exists and is available
        File dataDir = mCtx.getExternalFilesDir(null);
        try {
            if (!dataDir.exists()) {
                boolean res = dataDir.mkdirs();
                if (!res) {
                    Utils.errMsg(mCtx,
                            "Creating directory failed\n" + dataDir);
                    return null;
                }
                // Try again
                if (!dataDir.exists()) {
                    Utils.errMsg(mCtx,
                            "Unable to create database directory at "
                                    + dataDir);
                    return null;
                }
            }
            mDbHelper = new DatabaseHelper(mCtx, dataDir.getPath()
                    + File.separator + DB_NAME);
            mDb = mDbHelper.getWritableDatabase();
        } catch (Exception ex) {
            Utils.excMsg(mCtx, "Error opening database at " + dataDir, ex);
        }
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    /**
     * Create new session using the parameters provided. If the data is
     * successfully created return the new rowId for that entry, otherwise
     * return a -1 to indicate failure.
     *
     * @param startTime The start time.
     * @param name      The name.
     * @return RowId or -1 on failure.
     */
    public long createSession(long startTime, String name) {
        if (mDb == null) {
            Utils.errMsg(mCtx, "Failed to create session data. Database is " +
                    "null.");
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put(COL_CREATE_TIME, startTime);
        values.put(COL_NAME, name);

        long retVal = mDb.insert(DB_DATA_TABLE_SESSIONS, null, values);
        if (retVal < 0) {
            Utils.errMsg(mCtx, "Failed to create Session");
        }
        return retVal;
    }

    /**
     * Create new event using the parameters provided. If the data is
     * successfully created return the new rowId for that entry, otherwise
     * return a -1 to indicate failure.
     *
     * @param time      The time.
     * @param note      The note.
     * @param sessionId The session id.
     * @return RowId or -1 on failure.
     */
    public long createEvent(long time, String note, long sessionId) {
        if (mDb == null) {
            Utils.errMsg(mCtx, "Failed to create session data. Database is " +
                    "null.");
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put(COL_TIME, time);
        values.put(COL_NOTE, note);
        values.put(COL_SESSION_ID, sessionId);

        long retVal = mDb.insert(DB_DATA_TABLE_EVENTS, null, values);
        if (retVal < 0) {
            Utils.errMsg(mCtx, "Failed to create Event");
        }
        return retVal;
    }

    /**
     * Delete all the data and recreate the tables.
     */
    public void recreateDataTable() {
        mDb.execSQL("DROP TABLE IF EXISTS " + DB_DATA_TABLE_SESSIONS);
        mDb.execSQL(DB_CREATE_DATA_TABLE_SESSIONS);
        mDb.execSQL("DROP TABLE IF EXISTS " + DB_DATA_TABLE_EVENTS);
        mDb.execSQL(DB_CREATE_DATA_TABLE_EVENTS);
    }

    /**
     * Return a Cursor over the list of all sessions in the database.
     *
     * @return Cursor over items.
     */
    public Cursor fetchAllSessionData(boolean ascending) {
        if (mDb == null) {
            return null;
        }
        return mDb.query(DB_DATA_TABLE_SESSIONS, new String[]{COL_ID,
                        COL_CREATE_TIME, COL_NAME},
                null, null, null, null,
                COL_CREATE_TIME + (ascending ? " ASC" : " DESC"));
    }

    /**
     * Return a Cursor over the list of all events in the database
     * corresponding to the given session.
     *
     * @return Cursor over items.
     */
    public Cursor fetchAllEventDataForSession(long sessionId) {
        if (mDb == null) {
            return null;
        }
        return mDb.query(DB_DATA_TABLE_EVENTS,
                new String[]{COL_ID, COL_TIME, COL_NOTE, COL_SESSION_ID},
                COL_SESSION_ID + " = " + sessionId,
                null, null, null,
                COL_TIME + " ASC");
    }

    /**
     * Delete the event with the given rowId and session id.
     *
     * @param rowId     id of data to delete
     * @param sessionId id of session with the data.
     * @return true if deleted, false otherwise.
     */
    public boolean deleteEvent(long rowId, long sessionId) {
        return mDb.delete(DB_DATA_TABLE_EVENTS,
                COL_ID + "=" + rowId + " AND " + COL_SESSION_ID + " = " + sessionId,
                null) > 0;
    }

    /**
     * Return a Cursor positioned at the session that matches the given rowId
     *
     * @param rowId id of entry to retrieve.
     * @return Cursor positioned to matching entry, if found.
     * @throws SQLException if entry could not be found/retrieved.
     */
    public Cursor fetchSession(long rowId) throws SQLException {
        Cursor mCursor = mDb.query(true, DB_DATA_TABLE_SESSIONS,
                new String[]{COL_ID,
                        COL_CREATE_TIME, COL_NAME}, COL_ID + "=" + rowId,
                null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Update the session name using the details provided. The data to be
     * updated is
     * specified using the rowId, and it is altered to use the values passed in.
     *
     * @param rowId The row id.
     * @param name  The name.
     * @return If rows were affected
     */
    public boolean updateSessionName(long rowId, String name) {
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);

        return mDb.update(DB_DATA_TABLE_SESSIONS, values,
                COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Update the event note using the details provided. The data to be
     * updated is specified using the rowId, and it is altered to use the
     * values passed in.
     *
     * @param rowId The row id.
     * @param note  The note.
     * @return If rows were affected
     */
    public boolean updateEventNote(long rowId, String note) {
        ContentValues values = new ContentValues();
        values.put(COL_NOTE, note);

        return mDb.update(DB_DATA_TABLE_EVENTS, values,
                COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Deletes all data in the database for the given session.
     *
     * @param sessionId The session id.
     * @return Whether successful.
     */
    public boolean deleteSessionAndData(long sessionId) {
        boolean success1, success2;
        success1 = mDb.delete(DB_DATA_TABLE_SESSIONS,
                COL_ID + "=" + sessionId, null) > 0;
        success2 = mDb.delete(DB_DATA_TABLE_EVENTS,
                COL_SESSION_ID + "=" + sessionId, null) > 0;
        return success1 && success2;
    }

    /**
     * Clears the working database, attaches the new one, copies all data,
     * detaches the old one.
     *
     * @param newFileName Path to the new database.
     * @param alias       Name for the new database or null to use "SourceDb"
     */
    public void replaceDatabase(String newFileName, String alias) {
        if (alias == null) alias = "TEMP_DB";
        // Clear the working database
        recreateDataTable();
        // Attach the new database
        mDb.execSQL("ATTACH DATABASE '" + newFileName
                + "' AS " + alias);
        // Copy the sessions
        mDb.execSQL("INSERT INTO " + DB_DATA_TABLE_SESSIONS + " SELECT * FROM "
                + alias + "." + DB_DATA_TABLE_SESSIONS);
        // Copy the events
        mDb.execSQL("INSERT INTO " + DB_DATA_TABLE_EVENTS + " SELECT * FROM "
                + alias + "." + DB_DATA_TABLE_EVENTS);
        // Detach the new database
        mDb.execSQL("DETACH DATABASE " + alias);
    }

    /**
     * A SQLiteOpenHelper helper to help manage database creation and version
     * management.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private DatabaseHelper(Context context, String dir) {
            super(context, dir, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DB_CREATE_DATA_TABLE_SESSIONS);
            db.execSQL(DB_CREATE_DATA_TABLE_EVENTS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
            // TODO Re-do this so nothing is lost if there is a need to change
            // the version
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DB_DATA_TABLE_SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + DB_DATA_TABLE_EVENTS);
            onCreate(db);
        }
    }

}
