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
import us.rader.wyfy.model.WifiSettings.ConnectionOutcome;
import us.rader.wyfy.nfc.ForegroundDispatchActivity;
import us.rader.wyfy.nfc.NdefReaderActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
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
    private class ConnectTask extends
            AsyncTask<Void, Void, WifiSettings.ConnectionOutcome> {

        /**
         * Connect to wifi in a worker thread
         * 
         * @param params
         *            ignored
         * 
         * @see android.os.AsyncTask#doInBackground(Void...)
         */
        @Override
        protected WifiSettings.ConnectionOutcome doInBackground(Void... params) {

            try {

                return settings
                        .connect((WifiManager) getSystemService(WIFI_SERVICE));

            } catch (Exception e) {

                Log.e(getClass().getName(), "error connecting to wifi", e); //$NON-NLS-1$
                return ConnectionOutcome.FAILED;

            }
        }

        /**
         * Report outcome to user
         * 
         * @param result
         *            value returned by
         *            {@link WifiSettings#connect(WifiManager)} in the worker
         *            thread
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(WifiSettings.ConnectionOutcome result) {

            String ssid = settings.getSsid();

            switch (result) {

                case ADDED:

                    alert(getString(R.string.successfully_added_wifi, ssid));
                    break;

                case ENABLED:

                    alert(getString(R.string.successfully_enabled_wifi, ssid));
                    break;

                case FAILED:

                    alert(getString(R.string.failed_to_enable_wifi, ssid));
                    break;

                default:

                    alert(getString(R.string.unrecognized_connection_outcome,
                            result));
                    break;

            }

            if (qrCodeFragment != null) {

                qrCodeFragment.updateQrCode(settings);

            }
        }

    }

    /**
     * {@link Activity#startActivityForResult(Intent, int)} request code when
     * launching {@link WriteTagActivity}
     */
    public static final int REQUEST_WRITE_TAG = 1;

    /**
     * Value used to enable a call to {@link WifiSettings#connect(WifiManager)}
     * as a side-effect of {@link #onStart()}
     */
    private boolean         connectOnStart;

    /**
     * {@link QrCodeFragment} to notify when the {@link #settings} change
     * 
     * Note that this will be <code>null</code> on devices that display only a
     * single pane
     */
    private QrCodeFragment  qrCodeFragment;

    /**
     * Model
     */
    private WifiSettings    settings;

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

            case R.id.share_qr_item:

                shareQrCode();
                return true;

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
     * Handle the response from another activity started by this one for its
     * result
     * 
     * @param requestCode
     *            the request code to which this is the response
     * 
     * @param resultCode
     *            the result code for the response
     * 
     * @param resultData
     *            the result data from the response
     * 
     * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int,
     *      android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent resultData) {

        super.onActivityResult(requestCode, resultCode, resultData);

        switch (requestCode) {

            case REQUEST_WRITE_TAG:

                onTagWritten(resultCode, resultData);
                break;

            default:

                alert(getString(R.string.unrecognized_request));
                break;

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

        if (settings == null) {

            Intent intent = getIntent();

            if (intent != null) {

                Uri uri = intent.getData();

                if (uri != null) {

                    initialize(uri.toString());

                } else {

                    Parcelable[] ndefMessages = intent
                            .getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                    if ((ndefMessages != null) && (ndefMessages.length > 0)) {

                        initialize((NdefMessage) ndefMessages[0]);

                    }
                }
            }
        }

        setFragments(savedInstanceState);

    }

    /**
     * Call {@link WifiSettings#connect(WifiManager)} if {@link #settings} is
     * not <code>null</code>
     * 
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onStart() {

        super.onStart();

        if (connectOnStart) {

            new ConnectTask().execute();
            connectOnStart = false;

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
     * Initialize from a legacy {@link NdefMessage}
     * 
     * Provide backward compatibility for tags written with older versions of
     * this app
     * 
     * @param ndefMessage
     *            legacy {@link NdefMessage}
     */
    private void initialize(NdefMessage ndefMessage) {

        try {
            NdefRecord[] records = ndefMessage.getRecords();
            if (records.length > 0) {

                NdefRecord record = records[0];

                if (record.getTnf() != NdefRecord.TNF_MIME_MEDIA) {

                    return;

                }

                String type = new String(record.getType(), "US-ASCII"); //$NON-NLS-1$

                if ("application/x-wyfy".equals(type)) { //$NON-NLS-1$

                    String payload = new String(record.getPayload(), "US-ASCII"); //$NON-NLS-1$
                    initialize(payload);

                }
            }

        } catch (Exception e) {

            Log.e(getClass().getName(), "initializeNdefMessage", e); //$NON-NLS-1$

        }
    }

    /**
     * Initialize {@link #settings} from the given WIFI: {@link Uri}
     * 
     * @param uri
     *            WIFI: {@link Uri}
     */
    private void initialize(String uri) {
        try {

            settings = WifiSettings.parse(uri);

            if (settings != null) {

                connectOnStart = true;

            }

        } catch (Exception e) {

            Log.e(getClass().getName(), "error parsing URI", e); //$NON-NLS-1$

        }
    }

    /**
     * Handle result from {@link WriteTagActivity}
     * 
     * @param resultCode
     *            result code
     * 
     * @param resultData
     *            result data
     */
    private void onTagWritten(int resultCode, Intent resultData) {

        switch (resultCode) {

            case RESULT_CANCELED:

                alert(getString(R.string.canceled));
                break;

            case RESULT_OK:

                NdefMessage message = resultData
                        .getParcelableExtra(ForegroundDispatchActivity.EXTRA_RESULT);

                if (message == null) {

                    alert(getString(R.string.null_message));

                } else {

                    NdefRecord[] records = message.getRecords();

                    if ((records == null) || (records.length < 1)) {

                        alert(getString(R.string.empty_message));

                    } else {

                        NdefRecord record = records[0];
                        String payload = NdefReaderActivity
                                .decodePayload(record);

                        if (payload == null) {

                            alert(getString(R.string.unparseable_payload));

                        } else {

                            alert(payload);

                        }
                    }
                }

                break;

            default:

                alert(getString(R.string.unrecognized_result_code, resultCode));

        }

    }

    /**
     * Initialize the UI {@link Fragment} instances according to the current
     * screen layout
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     */
    private void setFragments(Bundle savedInstanceState) {

        if (findViewById(R.id.single_fragment) != null) {

            if (savedInstanceState != null) {

                return;

            }

            setSinglePane();

        } else if (findViewById(R.id.two_fragments_vertical) != null) {

            setPortrait();

        } else {

            setLandscape();

        }

    }

    /**
     * Initialize the two-pane landscape layout
     */
    private void setLandscape() {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.left_fragment,
                WifiSettingsFragment.newInstance(settings));
        qrCodeFragment = QrCodeFragment.newInstance(settings);
        transaction.replace(R.id.right_fragment, qrCodeFragment);
        transaction.commit();

    }

    /**
     * Initialize the two-pane portrait layout
     */
    private void setPortrait() {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.top_fragment,
                WifiSettingsFragment.newInstance(settings));
        qrCodeFragment = QrCodeFragment.newInstance(settings);
        transaction.replace(R.id.bottom_fragment, qrCodeFragment);
        transaction.commit();

    }

    /**
     * Initialize the single-pane layout
     */
    private void setSinglePane() {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.single_fragment,
                WifiSettingsFragment.newInstance(settings));
        qrCodeFragment = null;
        transaction.commit();

    }

    /**
     * Handle "Share QR..." menu item
     * 
     * This shows {@link QrCodeFragment} in single-pane mode, or invokes
     * {@link QrCodeFragment#shareQrCode()} in two-pane mode
     */
    private void shareQrCode() {

        if (qrCodeFragment == null) {

            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.single_fragment,
                    QrCodeFragment.newInstance(settings));
            transaction.addToBackStack(null);
            transaction.commit();

        } else {

            qrCodeFragment.shareQrCode();

        }
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
        startActivityForResult(intent, REQUEST_WRITE_TAG);
        return true;

    }

}
