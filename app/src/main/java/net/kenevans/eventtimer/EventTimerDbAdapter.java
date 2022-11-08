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
            + COL_START_TIME + " integer not null, "
            + COL_END_TIME + " integer not null, "
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
     * @param stopTime  The stop time.
     * @param name      The name.
     * @return RowId or -1 on failure.
     */
    public long createSession(long startTime, long stopTime, String name) {
        if (mDb == null) {
            Utils.errMsg(mCtx, "Failed to create session data. Database is " +
                    "null.");
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put(COL_START_TIME, startTime);
        values.put(COL_END_TIME, stopTime);
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
                        COL_START_TIME,
                        COL_END_TIME, COL_NAME}, null, null, null,
                null,
                COL_START_TIME + (ascending ? " ASC" : " DESC"));
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
     * Delete the session with the given rowId.
     *
     * @param rowId id of data to delete
     * @return true if deleted, false otherwise.
     */
    public boolean deleteSession(long rowId) {
        return mDb.delete(DB_DATA_TABLE_SESSIONS, COL_ID + "=" + rowId, null) > 0;
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
                        COL_START_TIME, COL_END_TIME, COL_NAME}, COL_ID + "="
                        + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Return a Cursor positioned at the session that matches the given rowId
     *
     * @param rowId id of entry to retrieve.
     * @return Cursor positioned to matching entry, if found.
     * @throws SQLException if entry could not be found/retrieved.
     */
    public Cursor fetchEvent(long rowId, long sessionId) throws SQLException {
        Cursor mCursor = mDb.query(true, DB_DATA_TABLE_EVENTS,
                new String[]{COL_ID, COL_TIME, COL_NOTE, COL_SESSION_ID},
                COL_ID + "=" + rowId + " AND " + COL_SESSION_ID + " = " + sessionId,
                null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Update the session using the details provided. The data to be updated is
     * specified using the rowId, and it is altered to use the values passed in.
     *
     * @param rowId     The row id.
     * @param startTime The start time.
     * @param stopTime  The stop time.
     * @param name      The name.
     * @return If rows were affected
     */
    public boolean updateSession(long rowId, long startTime, long stopTime,
                                 String name) {
        ContentValues values = new ContentValues();
        values.put(COL_START_TIME, startTime);
        values.put(COL_END_TIME, stopTime);
        values.put(COL_NAME, name);

        return mDb.update(DB_DATA_TABLE_SESSIONS, values,
                COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Update the session start time using the details provided. The data to
     * be updated is
     * specified using the rowId, and it is altered to use the values passed in.
     *
     * @param rowId     The row id.
     * @param startTime The start time.
     * @return If rows were affected
     */
    public boolean updateSessionStartTime(long rowId, long startTime) {
        ContentValues values = new ContentValues();
        values.put(COL_START_TIME, startTime);

        return mDb.update(DB_DATA_TABLE_SESSIONS, values,
                COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Update the session stop time using the details provided. The data to
     * be updated is
     * specified using the rowId, and it is altered to use the values passed in.
     *
     * @param rowId    The row id.
     * @param stopTime The stop time.
     * @return If rows were affected
     */
    public boolean updateSessionStopTime(long rowId, long stopTime) {
        ContentValues values = new ContentValues();
        values.put(COL_END_TIME, stopTime);

        return mDb.update(DB_DATA_TABLE_SESSIONS, values,
                COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Update the session name using the details provided. The data to be
     * updated is
     * specified using the rowId, and it is altered to use the values passed in.
     *
     * @param rowId The row id.
     * @param name  The sname.
     * @return If rows were affected
     */
    public boolean updateSessionName(long rowId, String name) {
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);

        return mDb.update(DB_DATA_TABLE_SESSIONS, values,
                COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Update the event using the details provided. The data to be updated is
     * specified using the rowId, and it is altered to use the values passed in.
     *
     * @param rowId     The row id.
     * @param time      The time.
     * @param note      The note.
     * @param sessionId The session id.
     * @return If rows were affected
     */
    public boolean updateEvent(long rowId, long time, String note,
                               long sessionId) {
        ContentValues values = new ContentValues();
        values.put(COL_TIME, time);
        values.put(COL_NOTE, note);
        values.put(COL_SESSION_ID, sessionId);

        return mDb.update(DB_DATA_TABLE_SESSIONS, values,
                COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Update the event time using the details provided. The data to be
     * updated is
     * specified using the rowId, and it is altered to use the values passed in.
     *
     * @param rowId     The row id.
     * @param time      The time.
     * @param sessionId The session id.
     * @return If rows were affected
     */
    public boolean updateEventTime(long rowId, long time, long sessionId) {
        ContentValues values = new ContentValues();
        values.put(COL_TIME, time);
        values.put(COL_SESSION_ID, sessionId);

        return mDb.update(DB_DATA_TABLE_SESSIONS, values,
                COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Update the event note using the details provided. The data to be
     * updated is specified using the rowId, and it is altered to use the
     * values passed in.
     *
     * @param rowId     The row id.
     * @param note      The note.
     * @return If rows were affected
     */
    public boolean updateEventNote(long rowId, String note) {
        ContentValues values = new ContentValues();
        values.put(COL_NOTE, note);

        return mDb.update(DB_DATA_TABLE_EVENTS, values,
                COL_ID + "=" + rowId, null) > 0;
    }

//    //
// ///////////////////////////////////////////////////////////////////////
//    // Get data for start date only (ForStartDate)
// //////////////////////////
//    //
// ///////////////////////////////////////////////////////////////////////
//
//    /**
//     * Deletes all data in the database for the interval corresponding to the
//     * given the start date.
//     *
//     * @param start The start date.
//     * @return Whether successful.
//     */
//    public boolean deleteAllDataForStartDate(long start) {
//        return mDb.delete(DB_DATA_TABLE,
//                COL_START_TIME + "=" + start, null) > 0;
//    }
//
//    /**
//     * Return a Cursor over the HR items in the database having the given the
//     * start date.
//     *
//     * @param date The start date.
//     * @return Cursor over items.
//     */
//    public Cursor fetchAllHrDateDataForStartDate(long date) {
//        if (mDb == null) {
//            return null;
//        }
//        return mDb.query(DB_DATA_TABLE, new String[]{COL_TIME, COL_HR},
//                COL_START_TIME + "=" + date, null, null, null,
//                SORT_ASCENDING);
//    }
//
//    /**
//     * Return a Cursor over the HR and RR items in the database having the
//     given
//     * the start date
//     *
//     * @param date The start date.
//     * @return Cursor over items.
//     */
//    public Cursor fetchAllHrRrDateDataForStartDate(long date) {
//        if (mDb == null) {
//            return null;
//        }
//        return mDb
//                .query(DB_DATA_TABLE,
//                        new String[]{COL_TIME, COL_HR, COL_RR},
//                        COL_START_TIME + "=" + date, null, null,
//                        null, SORT_ASCENDING);
//    }
//
//    //
// ///////////////////////////////////////////////////////////////////////
//    // Get data for start date through end date (ForDate)
// ///////////////////
//    //
// ///////////////////////////////////////////////////////////////////////
//
//    /**
//     * Return a Cursor over the HR items in the database for a given start and
//     * end times.
//     *
//     * @param start The start time.
//     * @param end   The end time.
//     * @return Cursor over items.
//     */
//    public Cursor fetchAllHrDateDataForDates(long start, long end) {
//        if (mDb == null) {
//            return null;
//        }
//        return mDb.query(DB_DATA_TABLE, new String[]{COL_TIME, COL_HR},
//                COL_TIME + ">=" + start + " AND " + COL_TIME
//                        + "<=" + end, null, null, null,
//                SORT_ASCENDING);
//    }
//
//    /**
//     * Return a Cursor over the HR and RR items in the database for a given
//     * start and end times.
//     *
//     * @param start The start time.
//     * @param end   The end time.
//     * @return Cursor over items.
//     */
//    public Cursor fetchAllHrRrDateDataForDates(long start, long end) {
//        if (mDb == null) {
//            return null;
//        }
//        return mDb.query(DB_DATA_TABLE,
//                new String[]{COL_TIME, COL_HR, COL_RR}, COL_TIME + ">="
//                        + start + " AND " + COL_TIME + "<="
//                        + end, null, null, null, SORT_ASCENDING);
//    }
//
//    //
// ///////////////////////////////////////////////////////////////////////
//    // Get data for start date and later (StartingAtDate)
// ///////////////////
//    //
// ///////////////////////////////////////////////////////////////////////
//
//    /**
//     * Return a Cursor over the list of all items in the database for a given
//     * time and later.
//     *
//     * @param date The time.
//     * @return Cursor over items.
//     */
//    public Cursor fetchAllDataStartingAtDate(long date) {
//        if (mDb == null) {
//            return null;
//        }
//        return mDb.query(DB_DATA_TABLE, new String[]{COL_ID, COL_TIME,
//                        COL_START_TIME, COL_HR, COL_RR},
//                COL_TIME + ">=" + date, null, null, null,
//                SORT_ASCENDING);
//    }
//
//    /**
//     * Return a Cursor over the HR and RR items in the database for a given
//     time
//     * and later.
//     *
//     * @param date The time.
//     * @return Cursor over items.
//     */
//    public Cursor fetchAllHrRrDateDataStartingAtDate(long date) {
//        if (mDb == null) {
//            return null;
//        }
//        return mDb
//                .query(DB_DATA_TABLE,
//                        new String[]{COL_TIME, COL_HR, COL_RR}, COL_TIME
//                                + ">=" + date, null, null, null,
//                        SORT_ASCENDING);
//    }

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
