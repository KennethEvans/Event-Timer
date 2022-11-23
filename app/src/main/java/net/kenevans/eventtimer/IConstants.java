package net.kenevans.eventtimer;

//Copyright (c) 2011 Kenneth Evans
//
//Permission is hereby granted, free of charge, to any person obtaining
//a copy of this software and associated documentation files (the
//"Software"), to deal in the Software without restriction, including
//without limitation the rights to use, copy, modify, merge, publish,
//distribute, sublicense, and/or sell copies of the Software, and to
//permit persons to whom the Software is furnished to do so, subject to
//the following conditions:
//
//The above copyright notice and this permission notice shall be included
//in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
//EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Holds constant values used by several classes in the application.
 */
interface IConstants {
    String TAG = "EventTimer";

    /**
     * Used for SharedPreferences
     */
    String MAIN_ACTIVITY = "SessionsListActivity";

    /**
     * Name of the package for this application.
     */
    String PACKAGE_NAME = "net.kenevans.eventtimer";

    /**
     * Value for a database date value indicating invalid.
     */
    long INVALID_TIME = Long.MIN_VALUE;

    /**
     * Value for a database String indicating invalid.
     */
    String INVALID_STRING = "Invalid";

    // Formatters
    /**
     * The static formatter to use for formatting dates for file names.
     */
    SimpleDateFormat fileNameFormatter = new SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss", Locale.US);

    /**
     * The formatter to use for formatting dates to ms level.
     */
    SimpleDateFormat sessionSaveFormatter = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    /**
     * The long formatter to use for formatting dates.
     */
    SimpleDateFormat longFormatter = new SimpleDateFormat(
            "MMM dd, yyyy HH:mm:ss Z", Locale.US);

    /**
     * The formatter to use for formatting dates.
     */
    SimpleDateFormat mediumFormatter = new SimpleDateFormat(
            "MMM dd, yyyy HH:mm:ss", Locale.US);

    /**
     * The formatter to use for formatting session dates.
     */
    SimpleDateFormat dateFormatAmPm =
            new SimpleDateFormat("E MMM d, yyyy hh:mm:ss aa", Locale.US);
    /**
     * The formatter to use for formatting session dates.
     */
    SimpleDateFormat dateFormat =
            new SimpleDateFormat("E MMM d, yyyy HH:mm:ss", Locale.US);

    /**
     * The formatter to use for formatting summaries.
     */
    SimpleDateFormat summaryDateFormat =
            new SimpleDateFormat("E HH:mm:ss", Locale.US);

    /**
     * The formatter to use for formatting session dates.
     */
    SimpleDateFormat csvDateFormat =
            new SimpleDateFormat("E MMM d yyyy HH:mm:ss", Locale.US);

    // Session Display
    /**
     * Prefix for session names for CSV files. Will be followed by a date and
     * time.
     */
    String SESSION_CSV_NAME_PREFIX = "EventTimer-";


    // Preferences
    String PREF_TREE_URI = "tree_uri";
    String PREF_START_TIMER_INITIALLY = "timer_start_initially";

    // Requests
    int REQ_ACCESS_FINE_LOCATION = 1;
    int REQ_ACCESS_READ_EXTERNAL_STORAGE = 2;
    int REQ_ACCESS_WRITE_EXTERNAL_STORAGE = 3;
    int REQ_GET_TREE = 10;
    int REQ_CREATE_DOCUMENT = 11;
    int REQ_DB_FILE = 20;
    int REQ_DB_TEMP_FILE = 21;

    /**
     * Intent code for starting a SessionActivity.
     */
    String SESSION_ID_CODE = PACKAGE_NAME
            + ".PlotSessionCode";

    // Database
    /**
     * Simple name of the database.
     */
    String DB_NAME = "EventTimer.db";
    /**
     * Simple name of the data table.
     */
    String DB_DATA_TABLE_SESSIONS = "sessions";
    /**
     * Simple name of the data table.
     */
    String DB_DATA_TABLE_EVENTS = "events";
    /**
     * The database version
     */
    int DB_VERSION = 1;
    /**
     * Database column for the id. Identifies the row.
     */
    String COL_ID = "_id";
    /**
     * Database column for the sessions id. Identifies the row.
     */
    String COL_SESSION_ID = "session_id";
    /**
     * Database column for the start time.
     */
    String COL_CREATE_TIME = "create_time";
    /**
     * Database column for the name.
     */
    String COL_NAME = "name";

    /**
     * Database column for the time.
     */
    String COL_TIME = "time";
    /**
     * Database column for the note.
     */
    String COL_NOTE = "note";

    String saveDatabaseTemplate = "EventTimer.%s.db";
    /**
     * Prefix for the file name for saving the database.
     */
    String SAVE_DATABASE_FILENAME_PREFIX = "EventTimerDatabase";
    /**
     * Suffix for the file name for saving the database.
     */
    String SAVE_DATABASE_FILENAME_SUFFIX = ".csv";
    /**
     * Delimiter for saving CSV files.
     */
    String CSV_DELIM = "\t";
    /**
     * Tab String.
     */
    String TAB = "\t";
    /**
     * Template for creating the file name for saving the database.
     */
    String SAVE_DATABASE_FILENAME_TEMPLATE = SAVE_DATABASE_FILENAME_PREFIX
            + ".%s" + SAVE_DATABASE_FILENAME_SUFFIX;
    /**
     * Name of the file that will be restored. It would typically be a file that
     * was previously saved and then renamed.
     */
    String RESTORE_FILE_NAME = "restore"
            + SAVE_DATABASE_FILENAME_SUFFIX;
    /**
     * Delimiter for saving the database.
     */
    String SAVE_DATABASE_DELIM = ",";
}
