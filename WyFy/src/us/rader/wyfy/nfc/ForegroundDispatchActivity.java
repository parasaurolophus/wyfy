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

import us.rader.wyfy.R;
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
 * Boilerplate code for using {@link NfcAdapter} foreground dispatch
 * 
 * <h1>Rationale</h1>
 * 
 * <p>
 * Ordinarily, the Android OS will use the global intent-dispatching mechanism
 * to find an app to handle the contents of a NFC tag when it is detected.
 * Sometimes, however, a running app needs to usurp the notification that would
 * ordinarily be sent to some different app for a particular tag, e.g. when it
 * wishes to overwrite content that might have been written by some other app,
 * access the raw tag contents or underlying tag technology in some way etc.
 * </p>
 * 
 * <p>
 * In order for a running app to request that it be the one to be notified for a
 * given kind of {@link Tag} without regard to the usual global rules, it must
 * use the {@link NfcAdapter} foreground dispatch mechanism. This class provides
 * implementations of those specific {@link Activity} life-cycle methods
 * required to utilize foreground dispatch while deferring processing of the
 * contents of such tags to derived classes via <code>abstract</code> methods.
 * </p>
 * 
 * <h1>Design</h1>
 * 
 * <p>
 * The life-cycle of any {@link Activity} that uses foreground dispatch can be
 * summarized as:</code>
 * 
 * <ol>
 * 
 * <li>
 * <p>
 * Cache some helper objects in {@link #onCreate(Bundle)}:
 * 
 * <ul>
 * 
 * <li>
 * <p>
 * {@link NfcAdapter} singleton
 * </p>
 * </li>
 * 
 * <li>
 * <p>
 * {@link PendingIntent} wrapping a self-invoking
 * {@link Intent#FLAG_ACTIVITY_SINGLE_TOP} intent
 * </p>
 * </li>
 * 
 * <li>
 * <p>
 * {@link IntentFilter} array to select the specific kinds of tags for which it
 * desires to be notified
 * </p>
 * </li>
 * 
 * </ul>
 * 
 * (See
 * {@link NfcAdapter#enableForegroundDispatch(Activity, PendingIntent, IntentFilter[], String[][])}
 * )
 * </p>
 * </li>
 * 
 * <li>
 * <p>
 * Enable foreground dispatch in {@link #onResume()}
 * </p>
 * </li>
 * 
 * <li>
 * <p>
 * Disable foreground dispatch in {@link #onPause()}
 * </p>
 * </li>
 * 
 * <li>
 * <p>
 * Process a {@link Tag} detected while foreground dispatch was enabled in
 * {@link #onNewIntent(Intent)}
 * </p>
 * </li>
 * 
 * </ol>
 * 
 * <h1>Implementation</h1>
 * 
 * <p>
 * In order to allow derived classes to process the {@link Tag} instances in
 * which it is interested that are detected while foreground dispatch is
 * enabled, this class declares the following <code>abstract</code> methods:
 * </p>
 * 
 * <dl>
 * 
 * <dt>{@link #createIntentFilters()}</dt>
 * <dd>
 * <p>
 * Populate the {@link IntentFilter} array that will be used while foreground
 * dispatch is enabled. This is the set of {@link Intent} meta-data that will
 * trigger an invocation of {@link #onNewIntent(Intent)}.
 * </p>
 * 
 * <p>
 * Note: this will be invoked by {@link #onCreate(Bundle)} and its value cached
 * for use by {@link #onResume()} rather than being repeatedly called each time
 * {@link #onResume()} is invoked.
 * </p>
 * </dd>
 * 
 * <dt>{@link #processTag(Intent)}</dt>
 * <dd>
 * <p>
 * Take whatever action is desired for an {@link Intent} passed to
 * {@link #onNewIntent(Intent)} while foreground dispatch is enabled. This will
 * be invoked in a worker thread, separate from the main UI.
 * </p>
 * </dd>
 * 
 * <dt>{@link #onTagProcessed(Object)}</dt>
 * <dd>
 * <p>
 * Take whatever action is desired based on the result of having called
 * {@link #processTag(Intent)} in a worker thread. This will be called in the
 * main UI thread.
 * </p>
 * </dd>
 * 
 * </dl>
 * 
 * <p>
 * {@link #processTag(Intent)} and {@link #onTagProcessed(Object)} are separate
 * methods so that they can be invoked in different threads. Care must be taken
 * to put all labor-intensive or blocking I/O operations in
 * {@link #processTag(Intent)} while putting only non-blocking operations that
 * directly affect the UI in {@link #onTagProcessed(Object)}
 * </p>
 * 
 * <p>
 * As a rule of thumb, methods of <code>abstract</code> classes that are not,
 * themselves, <code>abstract</code> should be <code>final</code>. This class
 * violates that rule for {@link Activity} life-cycle methods like
 * {@link #onCreate(Bundle)}, {@link #onPause()}, {@link #onResume()} etc.
 * because of the high degree of likelihood that derived classes will also need
 * to override some or all of those particular methods. In all such cases, you
 * must be especially vigilant to always invoke <code>super</code>. [Too bad
 * Java doesn't support a mix-in style of programming, but I digress....]
 * </p>
 * 
 * @param <ContentType>
 *            The type returned by {@link #processTag(Intent)} and expected by
 *            {@link #onTagProcessed(Object)}; canonically, the type of data
 *            contained in the kind of {@link Tag} processed by the derived
 *            class (e.g. {@link NdefMessage}) but that is not actually a
 *            requirement enforced by this class
 * 
 * @see NfcAdapter#enableForegroundDispatch(Activity, PendingIntent,
 *      IntentFilter[], String[][])
 * @see #createIntentFilters()
 * @see #processTag(Intent)
 * @see #onTagProcessed(Object)
 * @see #onCreate(Bundle)
 * @see #onPause()
 * @see #onResume()
 * @see #onNewIntent(Intent)
 * @see NdefReaderActivity
 * @see NdefWriterActivity
 * 
 * @author Kirk
 */
public abstract class ForegroundDispatchActivity<ContentType> extends
        FragmentActivity {

    /**
     * Invoke {@link ForegroundDispatchActivity#processTag(Intent)} on a worker
     * thread, {@link ForegroundDispatchActivity#onTagProcessed(Object)} on the
     * UI thread
     */
    private class ProcessTagTask extends AsyncTask<Intent, Void, ContentType> {

        /**
         * Invoke {@link ForegroundDispatchActivity#processTag(Intent)}
         * 
         * @param intents
         *            <code>tags.length</code> must be exactly 1 and
         *            <code>tags[0]</code> must be the {@link Intent} to process
         * 
         * @return the value returned by
         *         {@link ForegroundDispatchActivity#processTag(Intent)} or
         *         <code><code>null</code> if an error occurs
         * 
         * @see android.os.AsyncTask#doInBackground(Tag...)
         * @see #onPostExecute(Object)
         * @see #onCancelled(Object)
         * @see ForegroundDispatchActivity#processTag(Intent)
         */
        @Override
        protected ContentType doInBackground(Intent... intents) {

            ContentType result = null;

            try {

                result = processTag(intents[0]);

            } catch (Exception e) {

                Log.e(getClass().getName(), "doInBackground", e); //$NON-NLS-1$
                toast(getString(R.string.error_processing_tag));

            }

            return result;

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
         * Pass <code>result</code> to
         * {@link ForegroundDispatchActivity#onTagProcessed(Object)}
         * 
         * Note that the overload of <code>onCancelled</code> that takes a
         * parameter was added in SDK 11, while this class strives to be
         * backwards-compatible to SDK 10, hence the override of
         * {@link #onCancelled()} that calls this one
         * 
         * @param result
         *            the value to pass to
         *            {@link ForegroundDispatchActivity#onTagProcessed(Object)};
         *            may (probably will) be <code>null</code>
         * 
         * @see android.os.AsyncTask#onCancelled(java.lang.Object)
         * @see #onCancelled()
         */
        @Override
        protected void onCancelled(ContentType result) {

            try {

                onTagProcessed(result);

            } catch (Exception e) {

                Log.e(getClass().getName(), "onCancelled", e); //$NON-NLS-1$

            }
        }

        /**
         * Pass <code>result</code> to
         * {@link ForegroundDispatchActivity#onTagProcessed(Object)}
         * 
         * @param result
         *            value returned by
         *            {@link ForegroundDispatchActivity#processTag(Intent)}
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(ContentType result) {

            try {

                onTagProcessed(result);

            } catch (Exception e) {

                Log.e(getClass().getName(), "onPostExecute", e); //$NON-NLS-1$
                toast(getString(R.string.error_processing_tag));

            }
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
     * Request code to use when enabling foreground dispatch
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
     * 
     * @see #onCreate(Bundle)
     * @see #onResume()
     */
    protected abstract IntentFilter[] createIntentFilters();

    /**
     * Initialize the data structures used in conjunction with foreground
     * dispatch
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     * 
     * @see FragmentActivity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        adapter = NfcAdapter.getDefaultAdapter(this);
        filters = createIntentFilters();
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, requestCode, intent, 0);

    }

    /**
     * Handle response to foreground dispatch request
     * 
     * @param intent
     *            the {@link Intent} containing the response
     * 
     * @see FragmentActivity#onNewIntent(Intent)
     */
    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        new ProcessTagTask().execute(intent);

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
     * Handle the result of having called {@link #processTag(Intent)}
     * 
     * This method can rely on, and must take account of being called in the UI
     * thread.
     * 
     * @param result
     *            the value returned from {@link #processTag(Intent)} or
     *            <code>null</code> if the latter threw an exception
     * 
     * @see #processTag(Intent)
     */
    protected abstract void onTagProcessed(ContentType result);

    /**
     * Handle a {@link Tag} detected while foreground dispatch was enabled
     * 
     * This method can rely on, and must take account of being called in a
     * worker thread. The value returned here will be passed on to
     * {@link #onTagProcessed(Object)} in the UI thread.
     * 
     * @param intent
     *            the {@link Intent} passed to {@link #onNewIntent(Intent)}
     *            while foreground dispatch was enabled
     * 
     * @return value to pass to {@link #onTagProcessed(Object)}
     * 
     * @see #onTagProcessed(Object)
     */
    protected abstract ContentType processTag(Intent intent);

    /**
     * Convenience method to display a {@link Toast} from any thread
     * 
     * This uses {@link #runOnUiThread(Runnable)} so that it can be called from
     * any thread, e.g. the one running
     * {@link ProcessTagTask#doInBackground(Intent...)}
     * 
     * @param message
     *            the message to display
     */
    protected final void toast(final String message) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                Toast.makeText(ForegroundDispatchActivity.this, message,
                        Toast.LENGTH_SHORT).show();

            }

        });
    }

}
