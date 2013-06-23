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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;

/**
 * Command queue {@link Handler} for making serialized, asynchronous calls to
 * {@link WifiSettingsDatabaseHelper}
 */
public final class QueryHandler extends Handler {

    /**
     * Interface implemented by objects that receive the results of asyncrhonous
     * database queries
     */
    public interface QueryListener {

        /**
         * The given <code>password</code> was found for the given
         * <code>ssid</code>
         * 
         * @param ssid
         *            SSID to lookup
         * 
         * @param password
         *            the password for the given SSID
         * 
         * @see QueryHandler#lookupPassword(SQLiteDatabase, QueryListener,
         *      String)
         * @see WifiSettingsDatabaseHelper#lookupPassword(SQLiteDatabase,
         *      String)
         */
        void onPasswordResult(String ssid, String password);

        /**
         * Handle the response returned by
         * {@link WifiSettingsDatabaseHelper#query(SQLiteDatabase, String, String...)}
         * 
         * @param cursor
         *            {@link Cursor} returned by
         *            {@link WifiSettingsDatabaseHelper#query(SQLiteDatabase, String, String...)}
         * 
         * @see QueryHandler#query(SQLiteDatabase, QueryListener, String,
         *      String...)
         * @see WifiSettingsDatabaseHelper#query(SQLiteDatabase, String,
         *      String...)
         */
        void onQueryPerformed(Cursor cursor);

    }

    /**
     * The singleton instance
     */
    private static QueryHandler singleton;

    static {

        singleton = null;

    }

    /**
     * Lazy creation of {@link #singleton}
     * 
     * @param context
     *            {@link Context}
     * 
     * @return {@link #singleton}
     */
    public static QueryHandler getInstance(Context context) {

        if (singleton == null) {

            singleton = new QueryHandler(context);

        }

        return singleton;

    }

    /**
     * {@link WifiSettingsDatabaseHelper}
     */
    private WifiSettingsDatabaseHelper helper;

    /**
     * Initialize {@link #helper}
     * 
     * @param context
     *            {@link Context} for
     *            {@link WifiSettingsDatabaseHelper#WifiSettingsDatabaseHelper(Context)}
     */
    private QueryHandler(Context context) {

        helper = new WifiSettingsDatabaseHelper(context);

    }

    /**
     * Close {@link #helper}
     */
    public void close() {

        try {

            if (helper != null) {

                helper.close();
                helper = null;

            }

        } catch (Exception e) {

            Log.e(getClass().getName(), "run", e); //$NON-NLS-1$

        }
    }

    /**
     * Enqueue an asynchronous invocation of
     * {@link WifiSettingsDatabaseHelper#delete(SQLiteDatabase, String, String...)}
     * 
     * @param db
     *            {@link SQLiteDatabase}
     * 
     * @param selection
     *            selection string
     * 
     * @param selectionArgs
     *            selection arguments
     * 
     * @return <code>true</code> if and only if the command was enqueued
     * 
     * @see WifiSettingsDatabaseHelper#delete(SQLiteDatabase, String, String...)
     */
    public boolean delete(final SQLiteDatabase db, final String selection,
            final String... selectionArgs) {

        return post(new Runnable() {

            @Override
            public void run() {

                try {

                    helper.delete(db, selection, selectionArgs);

                } catch (Exception e) {

                    Log.e(getClass().getName(), "run", e); //$NON-NLS-1$

                }
            }

        });

    }

    /**
     * Get {@link #helper}
     * 
     * @return {@link #helper}
     */
    public WifiSettingsDatabaseHelper getHelper() {

        return helper;

    }

    /**
     * Enqueue a command to invoke
     * {@link WifiSettingsDatabaseHelper#lookupPassword(SQLiteDatabase, String)}
     * asynchronously
     * 
     * @param db
     *            {@link SQLiteDatabase}
     * 
     * @param listener
     *            {@link QueryListener}
     * 
     * @param ssid
     *            SSID
     * 
     * @return <code>true</code> if and only if the command was enqueued
     * 
     * @see WifiSettingsDatabaseHelper#lookupPassword(SQLiteDatabase, String)
     */
    public boolean lookupPassword(final SQLiteDatabase db,
            final QueryListener listener, final String ssid) {

        return post(new Runnable() {

            @Override
            public void run() {

                try {

                    String password = helper.lookupPassword(db, ssid);
                    listener.onPasswordResult(ssid, password);

                } catch (Exception e) {

                    Log.e(getClass().getName(), "run", e); //$NON-NLS-1$

                }
            }

        });

    }

    /**
     * Enqueue a command to invoke
     * {@link WifiSettingsDatabaseHelper#query(SQLiteDatabase, String, String...)}
     * asynchronously
     * 
     * @param db
     *            {@link SQLiteDatabase}
     * 
     * @param listener
     *            {@link QueryListener}
     * 
     * @param selection
     *            selection string
     * 
     * @param selectionArgs
     *            selection arguments
     * 
     * @return <code>true</code> if and only if the command was enqueued
     * 
     * @see WifiSettingsDatabaseHelper#query(SQLiteDatabase, String, String...)
     */
    public boolean query(final SQLiteDatabase db, final QueryListener listener,
            final String selection, final String... selectionArgs) {

        return post(new Runnable() {

            @Override
            public void run() {

                try {

                    listener.onQueryPerformed(helper.query(db, selection,
                            selectionArgs));

                } catch (Exception e) {

                    Log.e(getClass().getName(), "run", e); //$NON-NLS-1$

                }
            }

        });

    }

    /**
     * Enqueue an asynchronous invocation of
     * {@link WifiSettingsDatabaseHelper#storeWifiSettings(SQLiteDatabase)}
     * 
     * @param db
     *            {@link SQLiteDatabase}
     * 
     * @return <code>true</code> if and only if command was enqueued
     * 
     * @see WifiSettingsDatabaseHelper#storeWifiSettings(SQLiteDatabase)
     */
    public boolean storeWifiSettings(final SQLiteDatabase db) {

        return post(new Runnable() {

            @Override
            public void run() {

                try {

                    helper.storeWifiSettings(db);

                } catch (Exception e) {

                    Log.e(getClass().getName(), "run", e); //$NON-NLS-1$

                }
            }

        });

    }

    /**
     * Call {@link #close()}
     * 
     * @throws Throwable
     *             if an error occurs
     * 
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {

        close();

    }

}