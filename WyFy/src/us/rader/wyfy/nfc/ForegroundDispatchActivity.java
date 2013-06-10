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
 * Abstract ancestor for any class of {@link FragmentActivity} that uses the
 * {@link NfcAdapter} foreground dispatch mechanism to read or write NFC tags
 * 
 * Note that <code>ResultType</code> is constrained to extend {@link Parcelable}
 * so that instances can be passed between activities via {@link Intent} extras
 * 
 * @param <ResultType>
 *            The {@link Parcelable} type returned by
 *            {@link #processTag(Tag, ProcessTagTask)} and expected by
 *            {@link #onTagProcessed(Parcelable, boolean)}
 * 
 * @see #onTagProcessed(Parcelable, boolean)
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
         * If {@link ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)}
         * returns <code>null</code>, this will call {@link #cancel(boolean)}
         * before returning.
         * 
         * @param tags
         *            <code>tags[0]</code> is the {@link Tag} to process
         * 
         * @return the value returned by
         *         {@link ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)}
         *         or <code><code>null</code> if an error occurs
         * 
         * @see android.os.AsyncTask#doInBackground(Tag...)
         * @see #onPostExecute(Parcelable)
         * @see #onCancelled(Parcelable)
         * @see ForegroundDispatchActivity#processTag(Tag, ProcessTagTask)
         */
        @Override
        protected ResultType doInBackground(Tag... tags) {

            try {

                ResultType result = processTag(tags[0], this);

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
         * Pass <code>null</code> to {@link #onCancelled(Parcelable)}
         * 
         * This override is provided on behalf of devices running versions of
         * Android based on SDK 10 or earlier
         * 
         * @see android.os.AsyncTask#onCancelled()
         * @see #onCancelled(Parcelable)
         */
        @Override
        protected void onCancelled() {

            onCancelled(null);

        }

        /**
         * Pass <code>result</code> and <code>true</code> to
         * {@link ForegroundDispatchActivity#onTagProcessed(Parcelable, boolean)}
         * 
         * Note that the overload of <code>onCancelled</code> that takes a
         * parameter was added in SDK 11, while this code strives to be
         * backwards-compatible to SDK 10, hence the override of
         * {@link #onCancelled()} that calls this one
         * 
         * @param result
         *            the value to pass to
         *            {@link ForegroundDispatchActivity#onTagProcessed(Parcelable, boolean)}
         * 
         * @see android.os.AsyncTask#onCancelled(java.lang.Object)
         * @see #onCancelled()
         */
        @Override
        protected void onCancelled(ResultType result) {

            try {

                onTagProcessed(result, true);

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

            Toast.makeText(ForegroundDispatchActivity.this, strings[0],
                    Toast.LENGTH_SHORT).show();

        }

    }

    /**
     * {@link Intent} extras key used to return the value passed to
     * {@link #onTagProcessed(Parcelable, boolean)} to the {@link Activity} that
     * launched this one
     */
    public static final String EXTRA_RESULT                = "us.rader.wyfy.nfc.result"; //$NON-NLS-1$

    /**
     * Result code passed to {@link #setResult(int)} or
     * {@link #setResult(int, Intent)} to indicate an error
     */
    public static final int    RESULT_ERROR_PROCESSING_TAG = RESULT_FIRST_USER;

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
     * 
     * @param cancelled
     *            <code>true</code> if and only if the {@link ProcessTagTask}
     *            running {@link #processTag(Tag, ProcessTagTask)} was cancelled
     * 
     * @see #processTag(Tag, ProcessTagTask)
     */
    protected void onTagProcessed(ResultType result, boolean cancelled) {

        int resultCode = (cancelled ? RESULT_CANCELED : RESULT_OK);

        if (result == null) {

            if (resultCode != RESULT_CANCELED) {

                Log.e(getClass().getName(), "result is null but not cancelled"); //$NON-NLS-1$
                resultCode = RESULT_ERROR_PROCESSING_TAG;

            }

            setResult(resultCode);

        } else {

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
