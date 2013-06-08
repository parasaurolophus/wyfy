/*
 * Copyright 2013 Kirk Rader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.rader.wyfy.db;

import us.rader.wyfy.db.WiFiSettingsContract.WifiSettingsEntry;
import us.rader.wyfy.model.WifiSettings;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * {@link SQLiteOpenHelper} for the WyFy datbase
 * 
 * Note that the methods of this class should generally be called in worker
 * threads separate from the main UI!
 * 
 * @author Kirk
 */
public final class WifiSettingsDatabaseHelper extends SQLiteOpenHelper {

    /**
     * Database name
     */
    private static final String DATABASE_NAME      = "WyFy.db";                                                //$NON-NLS-1$

    /**
     * Schema version number
     * 
     * Must be incremented if the schema ever changes
     */
    private static final int    DATABASE_VERSION   = 1;

    /**
     * SQL command to create the {@link WifiSettingsEntry} table
     */
    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " //$NON-NLS-1$
                                                           + WiFiSettingsContract.WifiSettingsEntry.TABLE_NAME
                                                           + " (" //$NON-NLS-1$
                                                           + WiFiSettingsContract.WifiSettingsEntry._ID
                                                           + " INTEGER PRIMARY KEY," //$NON-NLS-1$
                                                           + WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID
                                                           + " TEXT," //$NON-NLS-1$
                                                           + WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD
                                                           + " TEXT," //$NON-NLS-1$
                                                           + WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SECURITY
                                                           + " TEXT," //$NON-NLS-1$
                                                           + WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_HIDDEN
                                                           + " INTEGER)";                                      //$NON-NLS-1$

    /**
     * SQL command to delete the {@link WifiSettingsEntry} table
     */
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " //$NON-NLS-1$
                                                           + WiFiSettingsContract.WifiSettingsEntry.TABLE_NAME;

    /**
     * Initialize this instance on behalf of the given {@link Context}
     * 
     * @param context
     *            {@link Context}
     */
    public WifiSettingsDatabaseHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    /**
     * Return a {@link Cursor} that iterates over all the rows in the database
     * 
     * @param db
     *            {@link SQLiteDatabase}
     * 
     * @return {@link Cursor}
     */
    public Cursor getAll(SQLiteDatabase db) {

        String[] columns = {
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID,
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD,
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SECURITY,
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_HIDDEN };
        Cursor cursor = db.query(
                WiFiSettingsContract.WifiSettingsEntry.TABLE_NAME, columns,
                null, null, null, null, null);
        return cursor;

    }

    /**
     * Return the password stored in the database for the current value of the
     * {@link WifiSettings} singleton's SSID
     * 
     * @return password or <code>null</code> to indicate that no entry has yet
     *         been created for the current SSID
     */
    public String lookupPassword() {

        SQLiteDatabase db = getWritableDatabase();

        try {

            Cursor cursor = query(db);

            try {

                if (!cursor.moveToNext()) {

                    return null;

                }

                int index = cursor
                        .getColumnIndex(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD);
                return cursor.getString(index);

            } finally {

                cursor.close();

            }

        } finally {

            db.close();

        }
    }

    /**
     * Create the database from scratch
     * 
     * @param db
     *            {@link SQLiteDatabase}
     * 
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_ENTRIES);

    }

    /**
     * Downgrade the database to the specified version
     * 
     * Since this is the first and only version of the schema, downgrading and
     * upgrading shouldn't ever actually happen and should be treated the same
     * if they ever do (i.e. due to some bug)
     * 
     * @param db
     *            {@link SQLiteDatabase}
     * 
     * @param oldVersion
     *            version number of the existing database
     * 
     * @param newVersion
     *            requested version of the new database
     * 
     * @see android.database.sqlite.SQLiteOpenHelper#onDowngrade(android.database.sqlite.SQLiteDatabase,
     *      int, int)
     * @see #onUpgrade(SQLiteDatabase, int, int)
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        onUpgrade(db, oldVersion, newVersion);

    }

    /**
     * Upgrade the database to the specified version
     * 
     * Since this is the first and only version of the schema, downgrading and
     * upgrading shouldn't ever actually happen and should be treated as an
     * error requiring that the existing database be deleted and recreated from
     * scratch
     * 
     * @param db
     *            {@link SQLiteDatabase}
     * 
     * @param oldVersion
     *            version number of the existing database
     * 
     * @param newVersion
     *            requested version of the new database
     * 
     * @see android.database.sqlite.SQLiteOpenHelper#onDowngrade(android.database.sqlite.SQLiteDatabase,
     *      int, int)
     * @see #onDowngrade(SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);

    }

    /**
     * Update the existing row or insert a new row for the current state of the
     * {@link WifiSettings} singleton
     */
    public void storeWifiSettings() {

        SQLiteDatabase db = getWritableDatabase();

        try {

            Cursor cursor = query(db);

            try {

                if (!cursor.moveToNext()) {

                    insert(db);

                } else {

                    update(db);

                }

            } finally {

                cursor.close();

            }

        } finally {

            db.close();

        }
    }

    /**
     * Insert a new row for the current state of the {@link WifiSettings}
     * singleton
     * 
     * @param db
     *            {@link WifiSettings}
     */
    private void insert(SQLiteDatabase db) {

        ContentValues values = WiFiSettingsContract.WifiSettingsEntry
                .getContentValues();
        db.insert(WiFiSettingsContract.WifiSettingsEntry.TABLE_NAME,
                "null", values); //$NON-NLS-1$

    }

    /**
     * Return a {@link Cursor} for records matching the current
     * {@link WifiSettings} singleton's SSID
     * 
     * This will return an empty {@link Cursor} if no matching row has yet been
     * added to the database. If this ever returns a {@link Cursor} with more
     * than one entry, then some data corruption has occured due to a bug
     * somewhere in the app
     * 
     * @param db
     *            {@link SQLiteDatabase}
     * 
     * @return {@link Cursor}
     */
    private Cursor query(SQLiteDatabase db) {

        WifiSettings settings = WifiSettings.getInstance();
        String ssid = settings.getSsid();
        String[] columns = {
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID,
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD,
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SECURITY,
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_HIDDEN };
        String selection = WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID
                + " LIKE ?"; //$NON-NLS-1$
        String[] selectionArgs = { ssid };
        Cursor cursor = db.query(
                WiFiSettingsContract.WifiSettingsEntry.TABLE_NAME, columns,
                selection, selectionArgs, null, null, null);
        return cursor;

    }

    /**
     * Update the existing row for the current state of the {@link WifiSettings}
     * singleton
     * 
     * @param db
     *            {@link WifiSettings}
     */
    private void update(SQLiteDatabase db) {

        ContentValues values = WiFiSettingsContract.WifiSettingsEntry
                .getContentValues();
        String whereClause = WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID
                + " LIKE ?"; //$NON-NLS-1$
        String[] whereArgs = { values
                .getAsString(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID) };
        db.update(WiFiSettingsContract.WifiSettingsEntry.TABLE_NAME, values,
                whereClause, whereArgs);

    }

}
