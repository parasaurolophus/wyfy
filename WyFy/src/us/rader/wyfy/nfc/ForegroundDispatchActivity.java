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
package us.rader.wyfy.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * Abstract base class of activities that use the {@link NfcAdapter} foreground
 * dispatch mechanism to read or write NFC tags
 * 
 * @param <ResultType>
 *            The {@link Parcelable} type returned by
 *            {@link #processTag(Tag, ProcessTagTask)} and expected by
 *            {@link #onTagProcessed(Parcelable, boolean)}
 * 
 * @author Kirk
 */
public abstract class ForegroundDispatchActivity<ResultType extends Parcelable>
        extends FragmentActivity {

    /**
     * Invoke {@link ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)}
     * on a worker thread,
     * {@link ForegroundDispatchActivity#onTagProcessed(Parcelable, boolean)} on
     * the UI thread
     * 
     * @author Kirk
     */
    protected final class ProcessTagTask extends
            AsyncTask<Tag, String, ResultType> {

        /**
         * Display the given <code>message</code> to the user
         * 
         * @param message
         *            the message
         */
        public void report(String message) {

            publishProgress(message);

        }

        /**
         * Invoke
         * {@link ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)}
         * 
         * @param tags
         *            <code>tags[0]</code> is the {@link Tag} to process
         * 
         * @return the value returned by
         *         {@link ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)}
         *         or <code><code>null</code> if an error occurs
         * 
         * @see android.os.AsyncTask#doInBackground(Tag...)
         */
        @Override
        protected ResultType doInBackground(Tag... tags) {

            try {

                return processTag(tags[0], this);

            } catch (Exception e) {

                Log.e(getClass().getName(), "doInBackground", e); //$NON-NLS-1$
                return null;

            }
        }

        /**
         * Pass <code>null</code> and <code>true</code> to
         * {@link ForegroundDispatchActivity#onTagProcessed(Parcelable, boolean)}
         * 
         * @see android.os.AsyncTask#onCancelled()
         */
        @Override
        protected void onCancelled() {

            try {

                onTagProcessed(null, true);

            } catch (Exception e) {

                Log.e(getClass().getName(), "onCancelled", e); //$NON-NLS-1$

            }

        }

        /**
         * Pass <code>result</code> and <code>false</code> to
         * {@link ForegroundDispatchActivity#onTagProcessed(Parcelable, boolean)}
         * 
         * @param result
         *            value returned by
         *            {@link ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)}
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(ResultType result) {

            try {

                onTagProcessed(result, false);

            } catch (Exception e) {

                Log.e(getClass().getName(), "onPostExecute", e); //$NON-NLS-1$

            }
        }

        /**
         * Display <code>strings[0]</code> to the user
         * 
         * @param strings
         *            message to display to the user
         * 
         * @see android.os.AsyncTask#onProgressUpdate(String...)
         */
        @Override
        protected void onProgressUpdate(String... strings) {

            if (strings.length > 0) {

                final String message = strings[0];

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        Toast.makeText(ForegroundDispatchActivity.this,
                                message, Toast.LENGTH_SHORT).show();

                    }

                });

            }

        }

    }

    /**
     * {@link Intent} extras key used to return the value passed to
     * {@link #onTagProcessed(Parcelable, boolean)} to the {@link Activity} that
     * launched this one
     */
    public static final String EXTRA_RESULT = "us.rader.wyfy.nfc.result"; //$NON-NLS-1$

    /**
     * Cached {@link NfcAdapter}
     * 
     * @see #onPause()
     * @see #onResume()
     */
    private NfcAdapter         adapter;

    /**
     * Cached {@link IntentFilter} array
     * 
     * @see #onResume()
     */
    private IntentFilter[]     filters;

    /**
     * Cached {@link PendingIntent}
     * 
     * @see #onResume()
     */
    private PendingIntent      pendingIntent;

    /**
     * Request code to use with {@link #pendingIntent} when enabling foreground
     * dispatch
     * 
     * @see #onResume()
     */
    private int                requestCode;

    /**
     * Initialize {@link #requestCode} to the given value
     * 
     * @param requestCode
     *            value for {@link #requestCode}
     * 
     * @see #onResume()
     */
    protected ForegroundDispatchActivity(int requestCode) {

        this.requestCode = requestCode;

    }

    /**
     * Initialize the data structures used in conjunction with foreground
     * dispatch
     * 
     * @param savedInstanceState
     *            saved state of <code>null</code>
     * 
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        adapter = NfcAdapter.getDefaultAdapter(this);

        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, requestCode, intent, 0);

        IntentFilter ndefFilter = new IntentFilter(
                NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter tagFilter = new IntentFilter(
                NfcAdapter.ACTION_TAG_DISCOVERED);
        filters = new IntentFilter[] { ndefFilter, tagFilter };

    }

    /**
     * Handle response to foreground dispatch request
     * 
     * @param intent
     *            the {@link Intent} containing the response
     * 
     * @see android.support.v4.app.FragmentActivity#onNewIntent(android.content.Intent)
     */
    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tag != null) {

            new ProcessTagTask().execute(tag);

        }
    }

    /**
     * Disable foreground dispatch
     * 
     * @see android.support.v4.app.FragmentActivity#onPause()
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
     * @see android.support.v4.app.FragmentActivity#onResume()
     * @see #onPause()
     * @see #onNewIntent(Intent)
     */
    @Override
    protected void onResume() {

        super.onResume();
        adapter.enableForegroundDispatch(this, pendingIntent, filters, null);

    }

    /**
     * Handle the result of having called
     * {@link #processTag(Tag, ProcessTagTask)}
     * 
     * <p>
     * This method can rely on being called in the UI thread. This
     * implementation terminates the current activity after passing the
     * appropriate data to {@link Activity#setResult(int, Intent)}
     * </p>
     * 
     * <p>
     * Override this method if you wish to handle the result of processing a tag
     * without terminating the current activity
     * </p>
     * 
     * @param result
     *            the value returned from
     *            {@link #processTag(Tag, ProcessTagTask)} or <code>null</code>
     * 
     * @param cancelled
     *            <code>true</code> if and only if the {@link ProcessTagTask}
     *            running {@link #processTag(Tag, ProcessTagTask)} was cancelled
     */
    protected void onTagProcessed(ResultType result, boolean cancelled) {

        if (result == null) {

            setResult(RESULT_CANCELED);

        } else {

            int resultCode = (cancelled ? RESULT_CANCELED : RESULT_OK);
            Intent intent = new Intent();
            intent.putExtra(EXTRA_RESULT, result);
            setResult(resultCode, intent);

        }

        finish();

    }

    /**
     * Handle a {@link Tag} detected while foreground dispatch was enabled
     * 
     * This method can rely on being called in a worker thread. It can use the
     * given <code>task</code> to cancel the worker thread, give feedback to the
     * user etc. The value returned here will be passed on to
     * {@link #onTagProcessed(Parcelable, boolean)} in the UI thread.
     * 
     * @param tag
     *            the {@link Tag}
     * 
     * @param task
     *            the {@link ProcessTagTask} whose
     *            {@link ProcessTagTask#doInBackground(Tag...)} method is
     *            running this method
     * 
     * @return the value to pass as the first parameter to
     *         {@link #onTagProcessed(Parcelable, boolean)}
     * 
     * @see #onTagProcessed(Parcelable, boolean)
     */
    protected abstract ResultType processTag(Tag tag, ProcessTagTask task);

}
