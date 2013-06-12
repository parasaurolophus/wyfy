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
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * Abstract ancestor for any class of {@link FragmentActivity} that uses the
 * {@link NfcAdapter} foreground dispatch mechanism to read or write NFC tags
 * 
 * @param <ContentType>
 *            The type returned by {@link #processTag(Tag, ProcessTagTask)} and
 *            expected by {@link #onTagProcessed(Object, boolean)}; canonically,
 *            the type of data contained in the kind of {@link Tag} processed by
 *            the derived class (e.g. {@link NdefMessage}) but that is not
 *            actually a requirement enforced by this class
 * 
 * @see #createIntentFilters()
 * @see #processTag(Tag, ProcessTagTask)
 * @see #onTagProcessed(Object, boolean)
 * @see NdefReaderActivity
 * @see NdefWriterActivity
 * 
 * @author Kirk
 */
public abstract class ForegroundDispatchActivity<ContentType> extends
        FragmentActivity {

    /**
     * Invoke {@link ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)}
     * on a worker thread,
     * {@link ForegroundDispatchActivity#onTagProcessed(Object, boolean)} on the
     * UI thread
     */
    protected final class ProcessTagTask extends
            AsyncTask<Void, String, ContentType> {

        /**
         * The {@link Tag} to process
         * 
         * Note that this is a field rather than an argument to
         * {@link #doInBackground(Void...)} to emphasize the fact that there is
         * ever only one {@link Tag} to process at a time
         */
        private Tag tag;

        /**
         * Initialize {@link #tag}
         * 
         * @param tag
         *            value for {@link #tag}
         */
        public ProcessTagTask(Tag tag) {

            this.tag = tag;

        }

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
         * If {@link ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)}
         * returns <code>null</code>, this will call {@link #cancel(boolean)}
         * before returning.
         * 
         * @param ignored
         *            ignored
         * 
         * @return the value returned by
         *         {@link ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)}
         *         or <code><code>null</code> if an error occurs
         * 
         * @see android.os.AsyncTask#doInBackground(Tag...)
         * @see #onPostExecute(Object)
         * @see #onCancelled(Object)
         * @see ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)
         */
        @Override
        protected ContentType doInBackground(Void... ignored) {

            try {

                ContentType result = processTag(tag, this);

                // interpret a null result as equivalent to processTag() having
                // called cancel()
                if (result == null) {

                    cancel(false);

                }

                return result;

            } catch (Exception e) {

                Log.e(getClass().getName(), "doInBackground", e); //$NON-NLS-1$
                return null;

            }
        }

        /**
         * Pass <code>null</code> to {@link #onCancelled(Object)}
         * 
         * This override is provided on behalf of devices running versions of
         * Android based on SDK 10 or earlier
         * 
         * @see android.os.AsyncTask#onCancelled()
         * @see #onCancelled(Object)
         */
        @Override
        protected void onCancelled() {

            onCancelled(null);

        }

        /**
         * Pass <code>result</code> and <code>true</code> to
         * {@link ForegroundDispatchActivity#onTagProcessed(Object, boolean)}
         * 
         * Note that the overload of <code>onCancelled</code> that takes a
         * parameter was added in SDK 11, while this code strives to be
         * backwards-compatible to SDK 10, hence the override of
         * {@link #onCancelled()} that calls this one
         * 
         * @param result
         *            the value to pass to
         *            {@link ForegroundDispatchActivity#onTagProcessed(Object, boolean)}
         * 
         * @see android.os.AsyncTask#onCancelled(java.lang.Object)
         * @see #onCancelled()
         */
        @Override
        protected void onCancelled(ContentType result) {

            try {

                onTagProcessed(result, true);

            } catch (Exception e) {

                Log.e(getClass().getName(), "onCancelled", e); //$NON-NLS-1$

            }
        }

        /**
         * Pass <code>result</code> and <code>false</code> to
         * {@link ForegroundDispatchActivity#onTagProcessed(Object, boolean)}
         * 
         * @param result
         *            value returned by
         *            {@link ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)}
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(ContentType result) {

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

            Toast.makeText(ForegroundDispatchActivity.this, strings[0],
                    Toast.LENGTH_SHORT).show();

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
     * Cached {@link IntentFilter} array
     * 
     * @see #onResume()
     */
    private IntentFilter[] filters;

    /**
     * Cached {@link PendingIntent}
     * 
     * @see #onResume()
     */
    private PendingIntent  pendingIntent;

    /**
     * Request code to use with {@link #pendingIntent} when enabling foreground
     * dispatch
     * 
     * @see #onResume()
     */
    private int            requestCode;

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
     * Create the {@link IntentFilter} array to use when foreground dispatch is
     * enabled
     * 
     * @return {@link IntentFilter} that includes filters(s) for the tags and
     *         technologies supported by the derived class
     */
    protected abstract IntentFilter[] createIntentFilters();

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
        filters = createIntentFilters();

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

            new ProcessTagTask(tag).execute();

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
     * without terminating the current activity. If you override this method,
     * you can interpret the parameters as follows:
     * </p>
     * 
     * <table>
     * 
     * <tr>
     * <th><code>result</code></th>
     * <th><code>cancelled</code></th>
     * <th>Outcome</th>
     * </tr>
     * 
     * <tr>
     * <td><code>null</code></td>
     * <td><code>true</code></td>
     * <td>{@link #processTag(Tag, ProcessTagTask)} returned <code>null</code>,
     * with or without calling {@link AsyncTask#cancel(boolean)}</td>
     * </tr>
     * 
     * <tr>
     * <td><code>null</code></td>
     * <td><code>false</code></td>
     * <td>{@link #processTag(Tag, ProcessTagTask)} threw an exception</td>
     * </tr>
     * 
     * <tr>
     * <td>non-<code>null</code></td>
     * <td><code>true</code></td>
     * <td>{@link #processTag(Tag, ProcessTagTask)} called
     * {@link AsyncTask#cancel(boolean)} but also returned non-
     * <code>null</code></td>
     * </tr>
     * 
     * <tr>
     * <td>non-<code>null</code></td>
     * <td><code>false</code></td>
     * <td>successful return from {@link #processTag(Tag, ProcessTagTask)}</td>
     * </tr>
     * 
     * </table>
     * 
     * @param result
     *            the value returned from
     *            {@link #processTag(Tag, ProcessTagTask)} or <code>null</code>
     *            if the latter threw an exception
     * 
     * @param cancelled
     *            <code>true</code> if and only if the {@link ProcessTagTask}
     *            running {@link #processTag(Tag, ProcessTagTask)} was cancelled
     * 
     * @see #processTag(Tag, ProcessTagTask)
     */
    protected abstract void onTagProcessed(ContentType result, boolean cancelled);

    /**
     * Handle a {@link Tag} detected while foreground dispatch was enabled
     * 
     * This method can rely on being called in a worker thread. It can use the
     * given <code>task</code> to cancel the worker thread, give feedback to the
     * user etc. The value returned here will be passed on to
     * {@link #onTagProcessed(Object, boolean)} in the UI thread.
     * 
     * @param tag
     *            the {@link Tag}
     * 
     * @param task
     *            the {@link ProcessTagTask} whose
     *            {@link ProcessTagTask#doInBackground(Void...)} method is
     *            running this method
     * 
     * @return the value to pass as the first parameter to
     *         {@link #onTagProcessed(Object, boolean)}
     * 
     * @see #onTagProcessed(Object, boolean)
     */
    protected abstract ContentType processTag(Tag tag, ProcessTagTask task);

}
