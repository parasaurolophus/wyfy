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

import us.rader.wyfy.nfc.NdefWriterActivity;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

/**
 * {@link Activity} that writes a WIFI: URI to a NDEF compatible NFC tag
 * 
 * @author Kirk
 */
public final class WriteTagActivity extends NdefWriterActivity {

    /**
     * The {@link Uri} to write
     */
    private Uri uri;

    /**
     * Initialize {@link #uri} to <code>null</code>
     */
    public WriteTagActivity() {

        super(0);
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
     * Create a {@link NdefMessage} from {@link #uri}
     * 
     * @param currentMessage
     *            ignored
     * 
     * @return {@link NdefMessage}
     */
    @Override
    protected NdefMessage createNdefMessage(NdefMessage currentMessage) {

        NdefRecord record = createUri(uri);
        NdefRecord aar = createAar(getClass().getPackage());
        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] { record,
                aar });
        return ndefMessage;

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
        setContentView(R.layout.write_tag_activity);
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

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.write_tag_frame, new WriteTagFragment());
        transaction.commit();
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

}
