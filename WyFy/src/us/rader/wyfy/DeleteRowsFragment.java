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
package us.rader.wyfy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.rader.wyfy.db.WiFiSettingsContract;
import us.rader.wyfy.db.WifiSettingsDatabaseHelper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * UI to delete rows from the database
 * 
 * @author Kirk
 */
public class DeleteRowsFragment extends Fragment {

    /**
     * Invoke
     * {@link WifiSettingsDatabaseHelper#query(SQLiteDatabase, String, String...)}
     * in a worker thread
     * 
     * @author Kirk
     */
    private class QueryTask extends
            AsyncTask<Void, Void, List<Map<String, String>>> {

        /**
         * F * Invoke
         * {@link WifiSettingsDatabaseHelper#query(SQLiteDatabase, String, String...)}
         * in a worker thread
         * 
         * @param params
         *            ignored
         * 
         * @return cache of data from all rows in the database
         * 
         * @see android.os.AsyncTask#doInBackground(Object...)
         */
        @Override
        protected List<Map<String, String>> doInBackground(Void... params) {

            List<Map<String, String>> result = new ArrayList<Map<String, String>>();
            WifiSettingsDatabaseHelper helper = new WifiSettingsDatabaseHelper(
                    getActivity());
            SQLiteDatabase db = helper.getReadableDatabase();

            try {

                Cursor cursor = helper.query(db, null);

                try {

                    while (cursor.moveToNext()) {

                        Map<String, String> entry = new HashMap<String, String>();
                        int index;
                        index = cursor
                                .getColumnIndex(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_HIDDEN);
                        int hidden = cursor.getInt(index);
                        entry.put(
                                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_HIDDEN,
                                ((hidden == 0) ? "false" //$NON-NLS-1$
                                        : "true")); //$NON-NLS-1$
                        index = cursor
                                .getColumnIndex(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD);
                        entry.put(
                                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD,
                                cursor.getString(index));
                        index = cursor
                                .getColumnIndex(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SECURITY);
                        entry.put(
                                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SECURITY,
                                cursor.getString(index));
                        index = cursor
                                .getColumnIndex(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID);
                        entry.put(
                                WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID,
                                cursor.getString(index));
                        result.add(entry);

                    }

                } finally {

                    cursor.close();

                }

            } finally {

                db.close();

            }

            return result;

        }

        /**
         * Populate {@link DeleteRowsFragment#allRowsList}
         * 
         * @param result
         *            cached data from all rows in the database
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Map<String, String>> result) {

            String[] from = {
                    WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_HIDDEN,
                    WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD,
                    WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SECURITY,
                    WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID };
            int[] to = { R.id.hidden_row_text, R.id.password_row_text,
                    R.id.security_row_text, R.id.ssid_row_text };
            SimpleAdapter adapter = new SimpleAdapter(getActivity(), result,
                    R.layout.row_layout, from, to);
            allRowsList.setAdapter(adapter);

        }
    }

    /**
     * {@link ListView} to populate with data from all rows in the database
     */
    private ListView allRowsList;

    /**
     * Inflate the {@link View}
     * 
     * @param inflater
     *            {@link LayoutInflater}
     * 
     * @param container
     *            {@link ViewGroup}
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     * 
     * @return {@link View}
     * 
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     *      android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.delete_rows_fragment, container,
                false);
        allRowsList = (ListView) view.findViewById(R.id.rows_list);
        new QueryTask().execute();
        return view;

    }

    /**
     * Handle options {@link MenuItem}
     * 
     * @param item
     *            {@link MenuItem}
     * 
     * @return <code>true</code> if and only if the event was consumed
     * 
     * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.delete_selected_item:

                deleteRows();
                return true;

            default:

                return super.onOptionsItemSelected(item);

        }

    }

    /**
     * Delete rows corresponding to checked entries
     */
    private void deleteRows() {

        // TODO: not yet implemented
        Toast.makeText(getActivity(), "Not yet implemented", Toast.LENGTH_SHORT).show(); //$NON-NLS-1$

    }

}
