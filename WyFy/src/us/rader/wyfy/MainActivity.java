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

import us.rader.wyfy.model.WifiSettings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Launcher {@link Activity} for <code>WyFy</code> app
 * 
 * @author Kirk
 */
public final class MainActivity extends FragmentActivity implements
        WifiSettingsFragment.OnWifiSettingsChangedListener {

    /**
     * Attempt to connect to wifi in a worker thread
     * 
     * @author Kirk
     */
    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        /**
         * Connect to wifi in a worker thread
         * 
         * @param params
         *            ignored
         * 
         * @see android.os.AsyncTask#doInBackground(Void...)
         */
        @Override
        protected Boolean doInBackground(Void... params) {

            try {

                return settings
                        .connect((WifiManager) getSystemService(WIFI_SERVICE));

            } catch (Exception e) {

                Log.e(getClass().getName(), "error connecting to wifi", e); //$NON-NLS-1$
                return false;

            }
        }

        /**
         * Report outcome to user
         * 
         * @param result
         *            <code>true</code> if and only if connectin attempt was
         *            successful
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {

            String ssid = settings.getSsid();
            alert(result ? getString(R.string.successfully_connected_to_wifi,
                    ssid) : getString(R.string.failed_to_connect_to_wifi, ssid));

        }

    }

    /**
     * if <code>true</code>, call {@link WifiSettings#connect(WifiManager)} in
     * {@link #onResume()}
     */
    private boolean        connect;

    /**
     * {@link QrCodeFragment} to notify when the {@link #settings} change
     * 
     * Note that this will be <code>null</code> on devices that display only a
     * single pane
     */
    private QrCodeFragment qrCodeFragment;

    /**
     * Model
     */
    private WifiSettings   settings;

    /**
     * Initialize {@link #settings} to <code>null</code>
     */
    public MainActivity() {

        settings = null;

    }

    /**
     * Inflate the options {link Menu}
     * 
     * @param menu
     *            options {@ink Menu}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;

    }

    /**
     * Handle an options {@link MenuItem}
     * 
     * @param item
     *            {@link MenuItem} to handle
     * 
     * @return <code>true</code> if and only if event was consumed
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.write_tag_item:

                return writeTag();

            default:

                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Handle notification that the {@link WifiSettings} model state has been
     * changed by the user
     * 
     * @param newSettings
     *            {@link WifiSettings}
     * 
     * @see us.rader.wyfy.WifiSettingsFragment.OnWifiSettingsChangedListener#onWifiSettingsChanged(us.rader.wyfy.model.WifiSettings)
     */
    @Override
    public void onWifiSettingsChanged(WifiSettings newSettings) {

        settings = newSettings;

        if (qrCodeFragment != null) {

            qrCodeFragment.updateQrCode(settings);

        }
    }

    /**
     * Prepare this instance to be displayed
     * 
     * Initialize {@link #settings} and attach the {@link Fragment} instances
     * according to the dynamically loaded layout
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        connect = false;

        if (settings == null) {

            Intent intent = getIntent();

            if (intent != null) {

                Uri uri = intent.getData();

                if (uri != null) {

                    try {

                        settings = WifiSettings.parse(uri);
                        connect = true;

                    } catch (Exception e) {

                        Log.e(getClass().getName(), "error parsing URI", e); //$NON-NLS-1$

                    }
                }
            }
        }

        if (savedInstanceState != null) {

            return;

        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (findViewById(R.id.single_fragment) != null) {

            transaction.add(R.id.single_fragment,
                    WifiSettingsFragment.newInstance(settings));
            qrCodeFragment = null;

        } else {

            transaction.add(R.id.first_fragment,
                    WifiSettingsFragment.newInstance(settings));
            qrCodeFragment = QrCodeFragment.newInstance(settings);
            transaction.add(R.id.second_fragment, qrCodeFragment);

        }

        transaction.commit();

    }

    /**
     * Call {@link WifiSettings#connect(WifiManager)} if {@link #connect} is
     * <code>true</code>
     * 
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {

        super.onResume();

        if (connect && (settings != null)) {

            new ConnectTask().execute();

        }
    }

    /**
     * Display <code>message</code> to the user
     * 
     * @param message
     *            the message text
     */
    private void alert(String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);

        builder.setNeutralButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();

                    }

                });

        builder.show();

    }

    /**
     * Start {@Link WriteTagActivity}
     * 
     * @return <code>true</code>
     */
    private boolean writeTag() {

        Intent intent = new Intent(this, WriteTagActivity.class);
        Uri uri = Uri.parse(settings.toString());
        intent.setData(uri);
        startActivity(intent);
        return true;

    }

}
