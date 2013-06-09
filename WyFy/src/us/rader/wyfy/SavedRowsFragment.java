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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * UI to delete rows from the database
 * 
 * @author Kirk
 */
public class SavedRowsFragment extends Fragment {

    /**
     * Invoke {@link WifiSettingsDatabaseHelper#delete(String, String...)} in a
     * worker thread
     */
    private class DeleteRowTask extends AsyncTask<String, Void, Void> {

        /**
         * Invoke {@link WifiSettingsDatabaseHelper#delete(String, String...)}
         * in a worker thread
         * 
         * @param params
         *            <code>selection</code> and <code>selectionArgs</code>
         *            parameters to
         *            {@link WifiSettingsDatabaseHelper#delete(String, String...)}
         * 
         * @return <code>null</code>
         * 
         * @see android.os.AsyncTask#doInBackground(Object...)
         */
        @Override
        protected Void doInBackground(String... params) {

            new WifiSettingsDatabaseHelper(getActivity()).delete(
                    SELECT_BY_SSID, params[0]);
            return null;

        }

        /**
         * Terminate the current activity
         * 
         * @param result
         *            ignored
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {

            FragmentActivity activity = getActivity();
            activity.setResult(Activity.RESULT_OK);
            activity.finish();

        }

    }

    /**
     * Return the result of a database query to the {@link Activity} that
     * started this one
     */
    private class LoadRowTask extends QueryTask {

        /**
         * Return the result of a database query to the {@link Activity} that
         * started this one
         * 
         * @param rows
         *            the query results
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Map<String, String>> rows) {

            StringBuilder buffer = new StringBuilder("result"); //$NON-NLS-1$

            if (rows.size() > 0) {

                Map<String, String> row = rows.get(0);

                buffer.append('?');
                buffer.append(Uri
                        .encode(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_HIDDEN));
                buffer.append('=');
                buffer.append(Uri.encode(row
                        .get(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_HIDDEN)));

                buffer.append('&');
                buffer.append(Uri
                        .encode(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD));
                buffer.append('=');
                buffer.append(Uri.encode(row
                        .get(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_PASSWORD)));

                buffer.append('&');
                buffer.append(Uri
                        .encode(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SECURITY));
                buffer.append('=');
                buffer.append(Uri.encode(row
                        .get(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SECURITY)));

                buffer.append('&');
                buffer.append(Uri
                        .encode(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID));
                buffer.append('=');
                buffer.append(Uri.encode(row
                        .get(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID)));

            }

            Uri uri = Uri.parse(buffer.toString());
            Intent intent = new Intent();
            intent.setData(uri);
            FragmentActivity activity = getActivity();
            activity.setResult(Activity.RESULT_OK, intent);
            activity.finish();

        }

    }

    /**
     * Populate the list of saved SSID's in a worker thread
     */
    private class PopulateListTask extends QueryTask {

        /**
         * Populate {@link SavedRowsFragment#allRowsList}
         * 
         * @param rows
         *            cached data from all rows in the database
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Map<String, String>> rows) {

            ArrayList<String> ssidList = new ArrayList<String>();

            for (Map<String, String> row : rows) {

                String ssid = row
                        .get(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID);
                ssidList.add(ssid);

            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    getActivity(), R.layout.row_layout, R.id.ssid_row_text,
                    ssidList);
            allRowsList.setAdapter(adapter);

        }
    }

    /**
     * Execute a database query in a worker thread
     */
    private abstract class QueryTask extends
            AsyncTask<String, Void, List<Map<String, String>>> {

        /**
         * Invoke
         * {@link WifiSettingsDatabaseHelper#query(SQLiteDatabase, String, String...)}
         * in a worker thread
         * 
         * @param params
         *            <code>selection</code> and <code>selectionArgs</code>
         *            parameters to {@ink
         *            WifiSettingsDatabaseHelper#query(SQLiteDatabase, String,
         *            String...)}
         * 
         * @return cache of data from the selected rows
         * 
         * @see android.os.AsyncTask#doInBackground(Object...)
         */
        @Override
        protected List<Map<String, String>> doInBackground(String... params) {

            List<Map<String, String>> result = new ArrayList<Map<String, String>>();
            WifiSettingsDatabaseHelper helper = new WifiSettingsDatabaseHelper(
                    getActivity());
            SQLiteDatabase db = helper.getReadableDatabase();

            try {

                String selection = params[0];
                String[] selectionArgs = new String[params.length - 1];

                for (int index = 1; index < params.length; ++index) {

                    selectionArgs[index - 1] = params[index];

                }

                Cursor cursor = helper.query(db, selection, selectionArgs);

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

        View view = inflater.inflate(R.layout.saved_rows_fragment, container,
                false);
        allRowsList = (ListView) view.findViewById(R.id.rows_list);

        allRowsList
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {

                        TextView text = (TextView) view
                                .findViewById(R.id.ssid_row_text);
                        loadRow(text.getText().toString());

                    }

                });

        allRowsList
                .setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent,
                            View view, int postion, long id) {

                        TextView text = (TextView) view
                                .findViewById(R.id.ssid_row_text);
                        deleteRow(text.getText().toString());
                        return true;

                    }
                });

        new PopulateListTask().execute((String) null);
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

                        new DeleteRowTask().execute(ssid);
                        dialog.dismiss();

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

    /**
     * Return the data for the chosen SSID to the {@link Activity} that started
     * this one
     * 
     * @param ssid
     *            the selected SSID
     */
    private void loadRow(String ssid) {

        new LoadRowTask().execute(SELECT_BY_SSID, ssid);

    }

}
