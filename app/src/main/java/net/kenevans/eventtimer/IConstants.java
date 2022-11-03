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

/**
 * Holds constant values used by several classes in the application.
 */
interface IConstants {
    String TAG = "EventTimer";

    /**
     * Value for a database date value indicating invalid.
     */
    long INVALID_DATE = Long.MIN_VALUE;

    // Preferences
    String PREF_TREE_URI = "tree_uri";

    // Requests
    int REQ_ACCESS_FINE_LOCATION = 1;
    int REQ_ACCESS_READ_EXTERNAL_STORAGE = 2;
    int REQ_ACCESS_WRITE_EXTERNAL_STORAGE = 3;
    int REQ_GET_TREE = 10;
    int REQ_CREATE_DOCUMENT = 11;
    int REQ_DB_FILE = 20;
    int REQ_DB_TEMP_FILE = 21;
}
