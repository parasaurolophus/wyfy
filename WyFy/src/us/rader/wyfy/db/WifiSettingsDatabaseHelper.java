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
import android.provider.BaseColumns;

/**
 * <code>SQLiteOpenHelper</code> for the WyFy datbase
 * 
 * Note that the methods of this class should generally be called in worker
 * threads separate from the main UI!
 * 
 * @author Kirk
 */
public final class WifiSettingsDatabaseHelper extends SQLiteOpenHelper {

    /**
     * Database selection string to match by SSID
     */
    public static final String  SELECT_BY_SSID     = WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID
                                                           + " LIKE ?";                                        //$NON-NLS-1$

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
                                                           + BaseColumns._ID
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
     * Delete any row(s) matching the given criteria
     * 
     * @param db
     *            <code>SQLiteDatabase</code>
     * 
     * @param selection
     *            the selection string
     * 
     * @param selectionArgs
     *            string with which to replace '?' variables in
     *            <code>selection</code>
     */
    public void delete(SQLiteDatabase db, String selection,
            String... selectionArgs) {

        db.delete(WiFiSettingsContract.WifiSettingsEntry.TABLE_NAME, selection,
                selectionArgs);

    }

    /**
     * Return the password stored in the database for the given SSID
     * 
     * @param db
     *            <code>SQLiteDatabase</code>
     * 
     * @param ssid
     *            SSID
     * 
     * @return password or <code>null</code> to indicate that no entry has yet
     *         been created for the given SSID
     */
    public String lookupPassword(SQLiteDatabase db, String ssid) {

        Cursor cursor = query(db, SELECT_BY_SSID, ssid);

        try {

            if (!cursor.moveToNext()) {

                return null;

            }

            int index = cursor
                    .getColumnIndex(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD);
            String password = cursor.getString(index);
            return password;

        } finally {

            cursor.close();

        }
    }

    /**
     * Create the database from scratch
     * 
     * @param db
     *            <code>SQLiteDatabase</code>
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
     *            <code>SQLiteDatabase</code>
     * 
     * @param oldVersion
     *            version number of the existing database
     * 
     * @param newVersion
     *            requested version of the new database
     * 
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
     *            <code>SQLiteDatabase</code>
     * 
     * @param oldVersion
     *            version number of the existing database
     * 
     * @param newVersion
     *            requested version of the new database
     * 
     * @see #onDowngrade(SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);

    }

    /**
     * Return a <code>Cursor</code> for records matching the current
     * {@link WifiSettings} singleton's SSID
     * 
     * This will return an empty <code>Cursor</code> if no matching row has yet
     * been added to the database. If this ever returns a <code>Cursor</code>
     * with more than one entry, then some data corruption has occurred due to a
     * bug somewhere in the app
     * 
     * @param db
     *            <code>SQLiteDatabase</code>
     * 
     * @param selection
     *            selection SQL parameter
     * 
     * @param selectionArgs
     *            arguments to replace '?' in <code>selection</code>
     * 
     * @return <code>Cursor</code>
     */
    public Cursor query(SQLiteDatabase db, String selection,
            String... selectionArgs) {

        String[] columns = new String[] { BaseColumns._ID,
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID,
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD,
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SECURITY,
                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_HIDDEN };
        Cursor cursor = db.query(
                WiFiSettingsContract.WifiSettingsEntry.TABLE_NAME, columns,
                selection, selectionArgs, null, null, null);
        return cursor;

    }

    /**
     * Update the existing row or insert a new row for the current state of the
     * {@link WifiSettings} singleton
     * 
     * @param db
     *            <code>SQLiteDatabase</code>
     */
    public void storeWifiSettings(SQLiteDatabase db) {

        try {

            WifiSettings settings = WifiSettings.getInstance();
            String ssid = settings.getSsid();
            String selection = WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID
                    + " LIKE ?"; //$NON-NLS-1$
            Cursor cursor = query(db, selection, ssid);

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

        ContentValues values = WifiSettings.getInstance().getContentValues();
        db.insert(WiFiSettingsContract.WifiSettingsEntry.TABLE_NAME, null,
                values);

    }

    /**
     * Update the existing row for the current state of the {@link WifiSettings}
     * singleton
     * 
     * @param db
     *            {@link WifiSettings}
     */
    private void update(SQLiteDatabase db) {

        ContentValues values = WifiSettings.getInstance().getContentValues();
        String whereClause = WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID
                + " LIKE ?"; //$NON-NLS-1$
        String[] whereArgs = { values
                .getAsString(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID) };
        db.update(WiFiSettingsContract.WifiSettingsEntry.TABLE_NAME, values,
                whereClause, whereArgs);

    }

}
