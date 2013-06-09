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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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

            ArrayList<String> strings = new ArrayList<String>();

            for (Map<String, String> entry : result) {

                String ssid = entry
                        .get(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID);
                strings.add(ssid);

            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    getActivity(), R.layout.row_layout, R.id.ssid_row_text,
                    strings);
            allRowsList.setAdapter(adapter);

        }
    }

    /**
     * Database selection string to match by SSID
     */
    private static final String SELECT_BY_SSID = WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID
                                                       + " LIKE ?"; //$NON-NLS-1$

    /**
     * {@link ListView} to populate with data from all rows in the database
     */
    private ListView            allRowsList;

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

        allRowsList.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int postion, long id) {

                TextView text = (TextView) view
                        .findViewById(R.id.ssid_row_text);
                deleteRow(text.getText().toString());
                return true;

            }
        });

        new QueryTask().execute();
        return view;

    }

    /**
     * Offer the user the opportunity to delete the specified row from the
     * database
     * 
     * @param ssid
     *            the SSID of the entry to delete
     */
    private void deleteRow(final String ssid) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.delete_ssid_prompt, ssid));

        builder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        FragmentActivity activity = getActivity();
                        new WifiSettingsDatabaseHelper(activity).delete(
                                SELECT_BY_SSID, ssid);
                        dialog.dismiss();
                        activity.finish();

                    }
                });

        builder.setNegativeButton(android.R.string.no,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();

                    }
                });

        builder.show();

    }

}
