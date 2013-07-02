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

import us.rader.wyfy.db.QueryHandler;
import us.rader.wyfy.db.WifiSettingsDatabaseHelper;
import us.rader.wyfy.model.WifiSettings;
import us.rader.wyfy.model.WifiSettings.Security;
import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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

/**
 * <code>Fragment</code> for the WIFI wifiSettings UI
 * 
 * @author Kirk
 */
public final class WifiSettingsFragment extends Fragment {

    /**
     * Interface implemented by any <code>Activity</code> to which this
     * {@link WifiSettingsFragment} may be attached
     */
    public interface OnWifiSettingsChangedListener {

        /**
         * Notify listener that the user modified the values of the wifi
         * wifiSettings UI
         */
        void onWifiSettingsChanged();

    }

    /**
     * Listener for changes to the state of the checkbox denoting a hidden SSID
     */
    private class HiddenCheckedChangeListener implements
            CompoundButton.OnCheckedChangeListener {

        /**
         * Handle change to checked state of a <code>CompoundButton</code>
         * 
         * @param button
         *            <code>CompoundButton</code>
         * 
         * @param checked
         *            checked state of the <code>button</code>
         * 
         * @see WifiSettingsFragment#onControlsChanged(boolean)
         */
        @Override
        public void onCheckedChanged(CompoundButton button, boolean checked) {

            switch (button.getId()) {

                case R.id.hidden_checkbox:

                    wifiSettings.setHidden(checked);
                    onControlsChanged(true);
                    break;

                default:

                    break;

            }

        }

    }

    /**
     * Invoke
     * {@link WifiSettingsDatabaseHelper#lookupPassword(android.database.sqlite.SQLiteDatabase, String)}
     * in a worker thread, update the UI in the main thread
     */
    private final class LookupPasswordListener implements
            QueryHandler.QueryListener {

        /**
         * Update the UI to reflect the result of a database query
         * 
         * @param ssid
         *            the SSID
         * 
         * @param password
         *            the password
         * 
         * @see us.rader.wyfy.db.QueryHandler.QueryListener#onPasswordResult(String,
         *      String)
         */
        @Override
        public void onPasswordResult(final String ssid, final String password) {

            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    if (password != null) {

                        wifiSettings.setPassword(password);

                    }

                    onModelChanged(false);

                }

            });

        }

        /**
         * Nothing to do for this class
         * 
         * @param cursor
         *            ignored
         * 
         * @see us.rader.wyfy.db.QueryHandler.QueryListener#onQueryPerformed(Cursor)
         */
        @Override
        public void onQueryPerformed(Cursor cursor) {

            // nothing to do in this class

        }

    }

    /**
     * <code>TextWatcher</code> for edits to password control
     */
    private final class PasswordTextWatcher extends WifiSettingsTextWatcher {

        /**
         * Notify {@link #listener} that SSID has been edited by the user
         * 
         * @param editable
         *            ignored due to ill-thought out Android API
         * 
         * @see WifiSettingsFragment#onControlsChanged(boolean)
         */
        @Override
        public void afterTextChanged(Editable editable) {

            wifiSettings.setPassword(editable.toString());
            onControlsChanged(true);

        }

    }

    /**
     * Listener used to track selected security protocol
     */
    private class SecurityCheckedChangeListener implements
            RadioGroup.OnCheckedChangeListener {

        /**
         * Notify {@link #listener} of a change to the state of a
         * <code>RadioGroup</code>
         * 
         * @param group
         *            <code>RadioGroup</code>
         * 
         * @param id
         *            id of the checked radio button or -1 to indicate check
         *            cleared
         * 
         * @see WifiSettingsFragment#onControlsChanged(boolean)
         */
        @Override
        public void onCheckedChanged(RadioGroup group, int id) {

            switch (id) {

                case R.id.wep_radio:

                    wifiSettings.setSecurity(Security.WEP);
                    break;

                case R.id.wpa_radio:

                    wifiSettings.setSecurity(Security.WPA);
                    break;

                default:

                    wifiSettings.setSecurity(Security.NONE);
                    break;

            }

            onControlsChanged(true);

        }

    }

    /**
     * <code>TextWatcher</code> for edits to SSID control
     */
    private final class SsidTextWatcher extends WifiSettingsTextWatcher {

        /**
         * Notify {@link #listener} that SSID has been edited by the user
         * 
         * @param editable
         *            ignored due to ill-thought out Android API
         * 
         * @see WifiSettingsFragment#onControlsChanged(boolean)
         */
        @Override
        public void afterTextChanged(Editable editable) {

            wifiSettings.setSsid(editable.toString());
            onControlsChanged(false);

        }

    }

    /**
     * <code>TextWatcher</code> used to track changes to text controls in this
     * fragment's layout
     * 
     * This is just a convenience skeleton to provide implementations of
     * required methods that aren't needed by this app.
     * 
     * @see PasswordTextWatcher
     * @see SsidTextWatcher
     */
    private abstract class WifiSettingsTextWatcher implements TextWatcher {

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
         */
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {

            // nothing to do here

        }

        /**
         * Ignored
         * 
         * @param s
         *            Ignored
         * 
         * @param start
         *            Ignored
         * 
         * @param before
         *            Ignored
         * 
         * @param count
         *            Ignored
         */
        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {

            // nothing to do here

        }

    }

    /**
     * <code>Bundle</code> parameter name used to persist state of
     * {@link WifiSettings#isHidden()} across screen rotations, etc.
     */
    private static final String           HIDDEN_PARAMETER   = "HIDDEN";  //$NON-NLS-1$

    /**
     * <code>Bundle</code> parameter name used to persist state of
     * {@link WifiSettings#getPassword()} across screen rotations, etc.
     */
    private static final String           PASSWORD_PARAMETER = "PASSWORD"; //$NON-NLS-1$

    /**
     * <code>Bundle</code> parameter name used to persist state of
     * {@link WifiSettings#getSecurity()} across screen rotations, etc.
     */
    private static final String           SECURITY_PARAMETER = "SECURITY"; //$NON-NLS-1$

    /**
     * <code>Bundle</code> parameter name used to persist state of
     * {@link WifiSettings#getSsid()} across screen rotations, etc.
     */
    private static final String           SSID_PARAMETER     = "SSID";    //$NON-NLS-1$

    /**
     * Cache the singleton instance of {@link WifiSettings}
     */
    private static WifiSettings           wifiSettings;

    static {

        wifiSettings = WifiSettings.getInstance();

    }

    /**
     * <code>CheckBox</code> for a wifi access point with a SSID that isn't
     * broadcast
     */
    private CheckBox                      hiddenCheckBox;

    /**
     * {@link OnWifiSettingsChangedListener} to notify of changes to
     * {@link WifiSettings}
     */
    private OnWifiSettingsChangedListener listener;

    /**
     * <code>EditText</code> for a wifi access point password string or WEP key
     */
    private EditText                      passwordText;

    /**
     * <code>RadioGroup</code> to select a wifi access point's security protocol
     */
    private RadioGroup                    securityGroup;

    /**
     * <code>EditText</code> for a wifi access point SSID string
     */
    private EditText                      ssidText;

    /**
     * Set the {@link #listener}
     * 
     * @param activity
     *            must implement {@link OnWifiSettingsChangedListener}
     */
    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        this.listener = (OnWifiSettingsChangedListener) activity;

    }

    /**
     * Prepare this instance to be displayed
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {

            wifiSettings.setSsid(savedInstanceState.getString(SSID_PARAMETER));
            wifiSettings.setPassword(savedInstanceState
                    .getString(PASSWORD_PARAMETER));
            wifiSettings.setHidden(savedInstanceState
                    .getBoolean(HIDDEN_PARAMETER));
            wifiSettings.setSecurity((Security) savedInstanceState
                    .getSerializable(SECURITY_PARAMETER));

        }
    }

    /**
     * Inflate the <code>View</code>
     * 
     * @param inflater
     *            <code>LayoutInflater</code>
     * 
     * @param container
     *            <code>ViewGroup</code>
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.wifi_settings_fragment,
                container, false);
        ssidText = (EditText) view.findViewById(R.id.ssid_text);
        passwordText = (EditText) view.findViewById(R.id.p_text);
        securityGroup = (RadioGroup) view.findViewById(R.id.security_group);
        hiddenCheckBox = (CheckBox) view.findViewById(R.id.hidden_checkbox);
        onModelChanged(false);
        ssidText.addTextChangedListener(new SsidTextWatcher());
        passwordText.addTextChangedListener(new PasswordTextWatcher());
        securityGroup
                .setOnCheckedChangeListener(new SecurityCheckedChangeListener());
        hiddenCheckBox
                .setOnCheckedChangeListener(new HiddenCheckedChangeListener());
        return view;

    }

    /**
     * Remove the {@link #listener}
     */
    @Override
    public void onDetach() {

        super.onDetach();
        this.listener = null;

    }

    /**
     * Handle notification that the {@link WifiSettings} singleton was
     * initialized from the active connection
     * 
     * This attempts to obtain the password from the database before updating
     * the UI
     */
    public void onInitializedFromActiveConnection() {

        QueryHandler handler = QueryHandler.getInstance(getActivity());
        WifiSettingsDatabaseHelper helper = handler.getHelper();
        SQLiteDatabase db = helper.getWritableDatabase();
        handler.lookupPassword(db, new LookupPasswordListener(),
                wifiSettings.getSsid());

    }

    /**
     * Update the state of the UI widgets to match the current wi fi settings
     * model state
     * 
     * @param updateDatabase
     *            also store the new settings in the database if and only if
     *            <code>updateDatabase</code> is <code>true</code>
     */
    public void onModelChanged(boolean updateDatabase) {

        ssidText.setText(wifiSettings.getSsid());
        passwordText.setText(wifiSettings.getPassword());
        hiddenCheckBox.setChecked(wifiSettings.isHidden());

        switch (wifiSettings.getSecurity()) {

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

        if (updateDatabase) {

            storeWifiSettings();
        }
    }

    /**
     * Save the app-specific state of this instance
     * 
     * @param outState
     *            saved state
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
        outState.putString(SSID_PARAMETER, wifiSettings.getSsid());
        outState.putString(PASSWORD_PARAMETER, wifiSettings.getPassword());
        outState.putBoolean(HIDDEN_PARAMETER, wifiSettings.isHidden());
        outState.putSerializable(SECURITY_PARAMETER, wifiSettings.getSecurity());

    }

    /**
     * Notify {@link #listener} that the wi fi wifiSettings habe been changed by
     * the user
     * 
     * <p>
     * Also update the database if and only if <code>updateDatabase</code> is
     * <code>true</code>
     * </p>
     * 
     * <p>
     * This is used as the common helper method by all of the UI widget event
     * handlers
     * </p>
     * 
     * @param updateDatabase
     *            update the database, if <code>true</code>
     * 
     * @see HiddenCheckedChangeListener#onCheckedChanged(CompoundButton,
     *      boolean)
     * @see PasswordTextWatcher#afterTextChanged(Editable)
     * @see SecurityCheckedChangeListener#onCheckedChanged(RadioGroup, int)
     * @see SsidTextWatcher#afterTextChanged(Editable)
     */
    private void onControlsChanged(boolean updateDatabase) {

        try {

            if (updateDatabase) {

                storeWifiSettings();

            }

            if (listener != null) {

                listener.onWifiSettingsChanged();

            }

        } catch (Exception e) {

            Log.e(getClass().getName(), "notifyListener", e); //$NON-NLS-1$

        }
    }

    /**
     * Invoke
     * {@link WifiSettingsDatabaseHelper#storeWifiSettings(SQLiteDatabase)}
     * asynchronousy
     */
    private void storeWifiSettings() {

        QueryHandler handler = QueryHandler.getInstance(getActivity());
        WifiSettingsDatabaseHelper helper = handler.getHelper();
        SQLiteDatabase db = helper.getWritableDatabase();
        handler.storeWifiSettings(db);

    }

}
