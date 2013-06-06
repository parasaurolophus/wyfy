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
import us.rader.wyfy.nfc.ForegroundDispatchActivity;
import us.rader.wyfy.nfc.NdefReaderActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
     * {@link Activity#startActivityForResult(Intent, int)} request code when
     * launching {@link WriteTagActivity}
     */
    public static final int REQUEST_WRITE_TAG = 1;

    /**
     * {@link QrCodeFragment} to notify when the wi fi settings model state
     * changes
     * 
     * Note that this will be <code>null</code> on devices that display only a
     * single pane
     */
    private QrCodeFragment  qrCodeFragment;

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
     * @see WifiSettingsFragment.OnWifiSettingsChangedListener#onWifiSettingsChanged()
     */
    @Override
    public void onWifiSettingsChanged() {

        if (qrCodeFragment != null) {

            qrCodeFragment.updateQrCode();

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
     * Initialize the wi fi settings model and attach the {@link Fragment}
     * instances according to the dynamically loaded layout
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setFragments(savedInstanceState);

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
        transaction.replace(R.id.left_fragment, new WifiSettingsFragment());
        qrCodeFragment = new QrCodeFragment();
        transaction.replace(R.id.right_fragment, qrCodeFragment);
        transaction.commit();

    }

    /**
     * Initialize the two-pane portrait layout
     */
    private void setPortrait() {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.top_fragment, new WifiSettingsFragment());
        qrCodeFragment = new QrCodeFragment();
        transaction.replace(R.id.bottom_fragment, qrCodeFragment);
        transaction.commit();

    }

    /**
     * Initialize the single-pane layout
     */
    private void setSinglePane() {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.single_fragment, new WifiSettingsFragment());
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
            transaction.replace(R.id.single_fragment, new QrCodeFragment());
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
        Uri uri = Uri.parse(WifiSettings.getInstance().toString());
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_WRITE_TAG);
        return true;

    }

}
