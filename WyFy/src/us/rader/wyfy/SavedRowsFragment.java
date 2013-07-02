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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import us.rader.wyfy.db.QueryHandler;
import us.rader.wyfy.db.WiFiSettingsContract;
import us.rader.wyfy.db.WifiSettingsDatabaseHelper;

/**
 * UI to delete rows from the database
 * 
 * @author Kirk
 */
public class SavedRowsFragment extends Fragment {

    /**
     * Execute a database query in a worker thread
     */
    private abstract class ListQueryListener implements
            QueryHandler.QueryListener {

        /**
         * Ignored in this class
         * 
         * @param ssid
         *            ignored
         * 
         * @param password
         *            ignored
         * 
         * @see us.rader.wyfy.db.QueryHandler.QueryListener#onPasswordResult(java.lang.String,
         *      java.lang.String)
         */
        @Override
        public final void onPasswordResult(String ssid, String password) {

            // nothing to do for this class

        }

        /**
         * Construct a temporary copy of the contents of the given
         * {@link Cursor} and pass it to {@link #handleResult(List)}
         * 
         * @param cursor
         *            {@link Cursor}
         * 
         * @see us.rader.wyfy.db.QueryHandler.QueryListener#onQueryPerformed(android.database.Cursor)
         */
        @Override
        public final void onQueryPerformed(Cursor cursor) {

            try {

                final List<Map<String, String>> result = processRows(cursor);

                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        handleResult(result);

                    }

                });

            } catch (Exception e) {

                Log.e(getClass().getName(), "onQueryPerformed", e); //$NON-NLS-1$

            }
        }

        /**
         * Enqueue a command to invoke
         * {@link QueryHandler#query(SQLiteDatabase, QueryHandler.QueryListener, String, String...)}
         * asynchronously
         * 
         * @param selection
         *            selection string
         * 
         * @param selectionArgs
         *            selection arguments
         * 
         * @return <code>true</code> if and only if command was enqueued
         */
        public final boolean query(final String selection,
                final String... selectionArgs) {

            QueryHandler handler = QueryHandler.getInstance(getActivity());
            WifiSettingsDatabaseHelper helper = handler.getHelper();
            SQLiteDatabase db = helper.getWritableDatabase();
            return handler.query(db, this, selection, selectionArgs);

        }

        /**
         * Process the contents of a {@link Cursor} cached by
         * {@link #onQueryPerformed(Cursor)}
         * 
         * @param result
         *            cached contents of the {@link Cursor}
         */
        protected abstract void handleResult(List<Map<String, String>> result);

        /**
         * Helper used by {@link #onQueryPerformed(Cursor)}
         * 
         * @param cursor
         *            the database query result
         * 
         * @return the parsed data structure
         */
        private List<Map<String, String>> processRows(Cursor cursor) {

            List<Map<String, String>> result = new ArrayList<Map<String, String>>();

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

            return result;

        }

    }

    /**
     * {@link ListQueryListener} that handles a long-press on a particular item
     */
    private final class LoadRowListener extends ListQueryListener {

        /**
         * Call {@link SavedRowsFragment#returnRowToCaller(Map)}
         * 
         * @param result
         *            database query result
         * 
         * @see SavedRowsFragment#returnRowToCaller(Map)
         */
        @Override
        protected void handleResult(List<Map<String, String>> result) {

            if (result.size() > 0) {

                returnRowToCaller(result.get(0));

            } else {

                returnRowToCaller(null);
            }

        }
    }

    /**
     * {@link ListQueryListener} used to populate the list
     * 
     */
    private final class PopulateListListener extends ListQueryListener {

        /**
         * Call {@link SavedRowsFragment#populateList(List)}
         * 
         * @param result
         *            database query result
         * 
         * @see us.rader.wyfy.SavedRowsFragment.ListQueryListener#handleResult(java.util.List)
         */
        @Override
        protected void handleResult(List<Map<String, String>> result) {

            populateList(result);

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

        new PopulateListListener().query(null);
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

                        QueryHandler handler = QueryHandler
                                .getInstance(getActivity());
                        WifiSettingsDatabaseHelper helper = handler.getHelper();
                        SQLiteDatabase db = helper.getWritableDatabase();
                        helper.delete(db, ssid);
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
     * Return the data for the chosen SSID to the <code>Activity</code> that started
     * this one
     * 
     * @param ssid
     *            the selected SSID
     */
    private void loadRow(String ssid) {

        new LoadRowListener().query(WifiSettingsDatabaseHelper.SELECT_BY_SSID,
                ssid);

    }

    /**
     * Populate the list in the UI from the given database query results
     * 
     * @param rows
     *            database query results
     */
    private void populateList(List<Map<String, String>> rows) {

        ArrayList<String> ssidList = new ArrayList<String>();

        for (Map<String, String> row : rows) {

            String ssid = row
                    .get(WiFiSettingsContract.WifiSettingsEntry.COLUMN_NAME_SSID);
            ssidList.add(ssid);

        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.row_layout, R.id.ssid_row_text, ssidList);
        allRowsList.setAdapter(adapter);

    }

    /**
     * Call {@link Activity#setResult(int, Intent)} and
     * {@link Activity#finish()}
     * 
     * @param row
     *            the data to pass back to the <code>Activity</code> that started
     *            this one
     */
    private void returnRowToCaller(Map<String, String> row) {

        FragmentActivity activity = getActivity();

        if (row == null) {

            activity.setResult(Activity.RESULT_FIRST_USER);
            activity.finish();
            return;

        }

        StringBuilder buffer = new StringBuilder("result"); //$NON-NLS-1$
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
        Uri uri = Uri.parse(buffer.toString());
        Intent intent = new Intent();
        intent.setData(uri);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();

    }

}
