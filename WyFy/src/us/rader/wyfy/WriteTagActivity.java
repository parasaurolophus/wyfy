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

import java.io.UnsupportedEncodingException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * {@link Activity} that writes a WIFI: URI to a NDEF compatible NFC tag
 * 
 * @author Kirk
 */
public final class WriteTagActivity extends Activity {

    /**
     * Write a {@link Tag} in a worker thread
     * 
     * @author Kirk
     */
    private class ProcessTagTask extends AsyncTask<Tag, Void, String> {

        /**
         * Write a {@link Tag} in a worker thread
         * 
         * @param tags
         *            <code>tags[0]</code> is the {@link Tag} to write
         * 
         * @return message to display to the user describing the outcome
         * 
         * @see android.os.AsyncTask#doInBackground(Tag...)
         */
        @Override
        protected String doInBackground(Tag... tags) {

            try {

                return writeTag(tags[0]);

            } catch (Exception e) {

                Log.e(getClass().getName(), "doInBackground", e); //$NON-NLS-1$
                return getString(R.string.error_procesing_tag);

            }
        }

        /**
         * Display <code>result</code> to the user
         * 
         * @param result
         *            the message to display
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {

            alert(result);

        }

    }

    /**
     * Cached {@link NfcAdapter}
     * 
     * @see #onPause()
     * @see #onResume()
     */
    private NfcAdapter     adapter;

    /**
     * Cached {@link NfcAdapter} used in foreground dispatch
     * 
     * @see #onResume()
     */
    private IntentFilter[] filters;

    /**
     * Cached {@link PendingIntent} used in foreground dispatch
     * 
     * @see #onResume()
     */
    private PendingIntent  pendingIntent;

    /**
     * The {@link Uri} to write
     */
    private Uri            uri;

    /**
     * Initialize {@link #uri} to <code>null</code>
     */
    public WriteTagActivity() {

        uri = null;

    }

    /**
     * Inflate the menu; this adds items to the action bar if it is present
     * 
     * @param menu
     *            options {@link Menu}
     * 
     * @return <code>true</code>
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.write_tag, menu);
        return true;

    }

    /**
     * Handle menu event
     * 
     * @param item
     *            selected {@link MenuItem}
     * 
     * @return <code>true</code> if and only if the event was consumed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:

                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;

            default:

                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Prepare this instance to be displayed
     * 
     * @param savedInstanceState
     *            saved state of <code>null</code>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_tag);
        // Show the Up button in the action bar.
        setupActionBar();
        Intent intent;

        if (uri == null) {

            intent = getIntent();

            if (intent != null) {

                uri = intent.getData();

            }
        }

        if (uri == null) {

            throw new IllegalStateException(
                    "no uri supplied to write tag activity"); //$NON-NLS-1$

        }

        adapter = NfcAdapter.getDefaultAdapter(this);
        intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter ndefFilter = new IntentFilter(
                NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter tagFilter = new IntentFilter(
                NfcAdapter.ACTION_TAG_DISCOVERED);
        filters = new IntentFilter[] { ndefFilter, tagFilter };

    }

    /**
     * Write a {@link Tag} detected by foreground dispatch
     * 
     * @param intent
     *            the {@link Intent} supplied in response to a foreground
     *            dispatch request
     * 
     * @see android.app.Activity#onNewIntent(android.content.Intent)
     * @see #onResume()
     * @see ProcessTagTask
     */
    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        new ProcessTagTask().execute(tag);

    }

    /**
     * Disable foreground dispatch
     * 
     * @see android.app.Activity#onPause()
     * @see #onResume()
     */
    @Override
    protected void onPause() {

        super.onPause();
        adapter.disableForegroundDispatch(this);

    }

    /**
     * Enable foreground dispatch
     * 
     * @see android.app.Activity#onResume()
     * @see #onPause()
     * @see #onNewIntent(Intent)
     */
    @Override
    protected void onResume() {

        super.onResume();
        adapter.enableForegroundDispatch(this, pendingIntent, filters, null);

    }

    /**
     * Display <code>message</code> in an {@link AlertDialog}
     * 
     * @param message
     *            the message to display
     */
    private void alert(String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);

        builder.setNeutralButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                        finish();

                    }
                });

        builder.show();

    }

    /**
     * Create a {@link NdefMessage} from {@link #uri}
     * 
     * @return {@link NdefMessage}
     * 
     * @throws UnsupportedEncodingException
     *             if there is a bug in the Java virtual machine
     */
    private NdefMessage createNdefMessage() throws UnsupportedEncodingException {

        byte[] bytes = uri.toString().getBytes("US-ASCII"); //$NON-NLS-1$
        byte[] payload = new byte[bytes.length + 1];
        payload[0] = 0;
        System.arraycopy(bytes, 0, payload, 1, bytes.length);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_URI, null, payload);
        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] { record });
        return ndefMessage;

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            getActionBar().setDisplayHomeAsUpEnabled(true);

        }

    }

    /**
     * Write {@link #uri} as a NDEF "U" record to the given
     * {@link NdefFormatable} tag
     * 
     * @param formatable
     *            the {@link NdefFormatable} tag
     * 
     * @return message describing the outcome
     */
    private String writeFormatable(NdefFormatable formatable) {

        try {

            formatable.connect();

            try {

                NdefMessage ndefMessage = createNdefMessage();
                formatable.format(ndefMessage);
                return getString(R.string.success_writing_tag);

            } finally {

                formatable.close();
            }

        } catch (Exception e) {

            Log.e(getClass().getName(), "writeFormatable", e); //$NON-NLS-1$
            return getString(R.string.error_formatting_tag);

        }
    }

    /**
     * Write {@link #uri} as a NDEF "U" record to the given {@link Ndef}
     * formatted tag
     * 
     * @param ndef
     *            the {@link Ndef} formatted tag
     * 
     * @return message describing the outcome
     */
    private String writeNdef(Ndef ndef) {

        try {

            if (!ndef.isWritable()) {

                return getString(R.string.read_only_tag);

            }

            NdefMessage message = createNdefMessage();
            byte[] bytes = message.toByteArray();
            int tagSize = ndef.getMaxSize();

            if (bytes.length > tagSize) {

                return getString(R.string.tag_size_exceeded, bytes.length,
                        tagSize);

            }

            ndef.connect();

            try {

                ndef.writeNdefMessage(message);
                return getString(R.string.success_writing_tag);

            } finally {

                ndef.close();

            }

        } catch (Exception e) {

            Log.e(getClass().getName(), "writeNdef", e); //$NON-NLS-1$
            return getString(R.string.error_writing_tag);

        }
    }

    /**
     * Write {@link #uri} as a NDEF "U" message to the given {@link Tag}
     * 
     * @param tag
     *            the {@link Tag}
     * 
     * @return message to display to the user describing the outcome
     */
    private String writeTag(Tag tag) {

        Ndef ndef = Ndef.get(tag);

        if (ndef != null) {

            return writeNdef(ndef);

        }

        NdefFormatable formatable = NdefFormatable.get(tag);

        if (formatable != null) {

            return writeFormatable(formatable);

        }

        return getString(R.string.incompatible_tag);

    }

}
