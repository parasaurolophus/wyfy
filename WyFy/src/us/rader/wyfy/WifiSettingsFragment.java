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
import us.rader.wyfy.model.WifiSettings.Security;
import android.app.Activity;
import android.content.Context;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

/**
 * {@link Fragment} for the WIFI settings UI
 * 
 * @author Kirk
 */
public final class WifiSettingsFragment extends Fragment implements
        OnCheckedChangeListener,
        android.widget.CompoundButton.OnCheckedChangeListener, TextWatcher {

    /**
     * Interface implemented by any {@link Activity} to which this
     * {@link WifiSettingsFragment} may be attached
     * 
     * @author Kirk
     */
    public interface OnWifiSettingsChangedListener {

        /**
         * Notify listener that the user modified the values of the wifi
         * settings UI
         */
        void onWifiSettingsChanged();

    }

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

                return WifiSettings.getInstance().connect(
                        (WifiManager) getActivity().getSystemService(
                                Context.WIFI_SERVICE));

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

            updateSettings();

        }

    }

    /**
     * Invoke {@link WifiSettings#getActiveConnection(WifiManager)} in a worker
     * thread
     * 
     * @author Kirk
     */
    private class GetActiveConnectionTask extends
            AsyncTask<Void, Void, Boolean> {

        /**
         * Invoke {@link WifiSettings#getActiveConnection(WifiManager)}
         * 
         * @param params
         *            ignored
         * 
         * @return result of calling
         *         {@link WifiSettings#getActiveConnection(WifiManager)}
         * 
         * @see android.os.AsyncTask#doInBackground(Void...)
         */
        @Override
        protected Boolean doInBackground(Void... params) {

            try {

                WifiManager manager = (WifiManager) getActivity()
                        .getSystemService(Context.WIFI_SERVICE);
                return WifiSettings.getInstance().getActiveConnection(manager);

            } catch (Exception e) {

                Log.e(getClass().getName(), "getActiveConnection", e); //$NON-NLS-1$
                return Boolean.FALSE;
            }

        }

        /**
         * Update the QR code if in two-pane mode
         * 
         * @param result
         *            result of calling
         *            {@link WifiSettings#getActiveConnection(WifiManager)}
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {

            updateSettings();

        }

    }

    /**
     * 
     */
    private static final String           HIDDEN_PARAMETER   = "HIDDEN";  //$NON-NLS-1$

    /**
     * 
     */
    private static final String           PASSWORD_PARAMETER = "PASSWORD"; //$NON-NLS-1$

    /**
     * 
     */
    private static final String           SECURITY_PARAMETER = "SECURITY"; //$NON-NLS-1$

    /**
     * 
     */
    private static final String           SSID_PARAMETER     = "SSID";    //$NON-NLS-1$

    /**
     * Turn {@link #notifyListener()} into a no-op while
     * {@link #delayNotifications} is greater than 0
     */
    private int                           delayNotifications;

    /**
     * {@link CheckBox} for a wifi access point with a SSID that isn't broadcast
     */
    private CheckBox                      hiddenCheckBox;

    /**
     * The {@link OnWifiSettingsChangedListener} to notify of changes to wi fi
     * settings
     */
    private OnWifiSettingsChangedListener listener;

    /**
     * {@link EditText} for a wifi access point password string or WEP key
     */
    private EditText                      passwordText;

    /**
     * {@link RadioGroup} to select a wifi access point's security protocol
     */
    private RadioGroup                    securityGroup;

    /**
     * {@link EditText} for a wifi access point SSID string
     */
    private EditText                      ssidText;

    /**
     * Initialize {@link #delayNotifications}
     */
    public WifiSettingsFragment() {

        delayNotifications = 0;

    }

    /**
     * Notify {@link #listener} that SSID or password has been edited by the
     * user
     * 
     * @param editable
     *            ignored due to ill-thought out Android API
     * 
     * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
     */
    @Override
    public void afterTextChanged(Editable editable) {

        WifiSettings settings = WifiSettings.getInstance();
        settings.setSsid(ssidText.getText().toString());
        settings.setPassword(passwordText.getText().toString());
        notifyListener();

    }

    /**
     * Ignored
     * 
     * @param s
     *            ignored
     * 
     * @param start
     *            ignored
     * 
     * @param count
     *            ignored
     * 
     * @param after
     *            ignored
     * 
     * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence,
     *      int, int, int)
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {

        // nothing to do here

    }

    /**
     * Set the {@link #listener}
     * 
     * @param activity
     *            must implement {@link OnWifiSettingsChangedListener}
     * 
     * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        this.listener = (OnWifiSettingsChangedListener) activity;

    }

    /**
     * Handle change to checked state of a {@link CompoundButton}
     * 
     * @param button
     *            {@link CompoundButton}
     * 
     * @param checked
     *            checked state of the <code>button</code>
     * 
     * @see android.widget.CompoundButton.OnCheckedChangeListener#onCheckedChanged(android.widget.CompoundButton,
     *      boolean)
     */
    @Override
    public void onCheckedChanged(CompoundButton button, boolean checked) {

        switch (button.getId()) {

            case R.id.hidden_checkbox:

                WifiSettings.getInstance().setHidden(checked);
                notifyListener();
                break;

            default:

                break;

        }

    }

    /**
     * Notify {@link #listener} of a change to the state of a {@link RadioGroup}
     * 
     * @param group
     *            {@link RadioGroup}
     * 
     * @param id
     *            id of the checked radio button or -1 to indicate check cleared
     * 
     * @see android.widget.RadioGroup.OnCheckedChangeListener#onCheckedChanged(android
     *      .widget.RadioGroup, int)
     */
    @Override
    public void onCheckedChanged(RadioGroup group, int id) {

        WifiSettings settings = WifiSettings.getInstance();

        switch (id) {

            case R.id.wep_radio:

                settings.setSecurity(Security.WEP);
                break;

            case R.id.wpa_radio:

                settings.setSecurity(Security.WPA);
                break;

            default:

                settings.setSecurity(Security.NONE);
                break;

        }

        notifyListener();

    }

    /**
     * Prepare this instance to be displayed
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     * 
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            Intent intent = getActivity().getIntent();

            if (intent != null) {

                if (!parseIntentData(intent)) {

                    getActiveConnection();

                }
            }

        } else {

            WifiSettings settings = WifiSettings.getInstance();
            settings.setSsid(savedInstanceState.getString(SSID_PARAMETER));
            settings.setPassword(savedInstanceState
                    .getString(PASSWORD_PARAMETER));
            settings.setHidden(savedInstanceState.getBoolean(HIDDEN_PARAMETER));
            settings.setSecurity((Security) savedInstanceState
                    .getSerializable(SECURITY_PARAMETER));

        }
    }

    /**
     * Inflate the {@ink View}
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
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     *      android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // don't notify during initialization of UI...
        delayNotifications += 1;

        try {

            View view = inflater.inflate(R.layout.wifi_settings_fragment,
                    container, false);
            ssidText = (EditText) view.findViewById(R.id.ssid_text);
            passwordText = (EditText) view.findViewById(R.id.password_text);
            securityGroup = (RadioGroup) view.findViewById(R.id.security_group);
            hiddenCheckBox = (CheckBox) view.findViewById(R.id.hidden_checkbox);
            updateSettings();
            ssidText.addTextChangedListener(this);
            passwordText.addTextChangedListener(this);
            securityGroup.setOnCheckedChangeListener(this);
            hiddenCheckBox.setOnCheckedChangeListener(this);
            return view;

        } finally {

            // now that things are set up, enable notifications...
            delayNotifications -= 1;

        }
    }

    /**
     * Remove the {@link #listener}
     * 
     * @see android.support.v4.app.Fragment#onDetach()
     */
    @Override
    public void onDetach() {

        super.onDetach();
        this.listener = null;

    }

    /**
     * Save the app-specific state of this instance
     * 
     * @param outState
     *            saved state
     * 
     * @see android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
        WifiSettings settings = WifiSettings.getInstance();
        outState.putString(SSID_PARAMETER, settings.getSsid());
        outState.putString(PASSWORD_PARAMETER, settings.getPassword());
        outState.putBoolean(HIDDEN_PARAMETER, settings.isHidden());
        outState.putSerializable(SECURITY_PARAMETER, settings.getSecurity());

    }

    /**
     * Ignored
     * 
     * @param s
     *            ignored
     * 
     * @param start
     *            ignored
     * 
     * @param before
     *            ignored
     * 
     * @param count
     *            ignored
     * 
     * @see TextWatcher#onTextChanged(CharSequence, int, int, int)
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        // nothing to do here

    }

    /**
     * Update the state of the UI widgets to match the current wi fi settings
     * model state
     */
    public void updateSettings() {

        WifiSettings settings = WifiSettings.getInstance();

        // don't queue up a bunch of notifications while updating multiple
        // widgets...
        delayNotifications += 1;

        try {

            ssidText.setText(settings.getSsid());
            passwordText.setText(settings.getPassword());
            hiddenCheckBox.setChecked(settings.isHidden());

            switch (settings.getSecurity()) {

                case WEP:

                    securityGroup.check(R.id.wep_radio);
                    break;

                case WPA:

                    securityGroup.check(R.id.wpa_radio);
                    break;

                default:

                    securityGroup.check(R.id.nopass_radio);
                    break;

            }

        } finally {

            // notify at most once for all of the preceding updates...
            delayNotifications -= 1;
            notifyListener();

        }
    }

    /**
     * Initialize the wi fi settings model based on the currently active
     * connection, if any
     */
    private void getActiveConnection() {

        new GetActiveConnectionTask().execute();

    }

    /**
     * Notify {@link #listener} that the wi fi settings habe been changed by the
     * user
     */
    private void notifyListener() {

        try {

            if ((delayNotifications == 0) && (listener != null)) {

                listener.onWifiSettingsChanged();

            }

        } catch (Exception e) {

            Log.e(getClass().getName(), "notifyListener", e); //$NON-NLS-1$

        }
    }

    /**
     * Parse the data passed in the given {@link Intent} at launch
     * 
     * @param intent
     *            the {@link Intent}
     * 
     * @return <code>true</code> if and only if an asynchronouse attempt to
     *         connect was launched
     */
    private boolean parseIntentData(Intent intent) {
        Uri uri = intent.getData();

        if (uri != null) {

            return parseUri(uri.toString());

        }

        Parcelable[] ndefMessages = intent
                .getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if ((ndefMessages != null) && (ndefMessages.length > 0)) {

            return parseLegacyMessage((NdefMessage) ndefMessages[0]);

        }

        return false;

    }

    /**
     * Initialize from a legacy {@link NdefMessage}
     * 
     * Provide backward compatibility for tags written with older versions of
     * this app
     * 
     * @param ndefMessage
     *            legacy {@link NdefMessage}
     * 
     * @return <code>true</code> if and only if an asynchronouse attempt to
     *         connect was launched
     */
    private boolean parseLegacyMessage(NdefMessage ndefMessage) {

        try {

            NdefRecord[] records = ndefMessage.getRecords();

            if (records.length > 0) {

                NdefRecord record = records[0];

                if (record.getTnf() != NdefRecord.TNF_MIME_MEDIA) {

                    return false;

                }

                String type = new String(record.getType(), "US-ASCII"); //$NON-NLS-1$

                if ("application/x-wyfy".equals(type)) { //$NON-NLS-1$

                    String payload = new String(record.getPayload(), "US-ASCII"); //$NON-NLS-1$
                    return parseUri(payload);

                }
            }

        } catch (Exception e) {

            Log.e(getClass().getName(), "initializeNdefMessage", e); //$NON-NLS-1$

        }

        return false;

    }

    /**
     * Initialize wi fi model state from the given WIFI: {@link Uri}
     * 
     * @param uri
     *            WIFI: {@link Uri}
     * 
     * @return <code>true</code> if and only if an asynchronous attempt to
     *         connect was launched
     */
    private boolean parseUri(String uri) {

        try {

            if (WifiSettings.getInstance().parse(uri)) {

                new ConnectTask().execute();
                return true;

            }

        } catch (Exception e) {

            Log.e(getClass().getName(), "error parsing URI", e); //$NON-NLS-1$

        }

        return false;

    }

}
