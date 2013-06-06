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
import us.rader.wyfy.model.WifiSettings.Security;
import android.app.Activity;
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
 * {@link Fragment} for the WIFI wifiSettings UI
 * 
 * @author Kirk
 */
public final class WifiSettingsFragment extends Fragment {

    /**
     * Interface implemented by any {@link Activity} to which this
     * {@link WifiSettingsFragment} may be attached
     * 
     * @author Kirk
     */
    public interface OnWifiSettingsChangedListener {

        /**
         * Notify listener that the user modified the values of the wifi
         * wifiSettings UI
         */
        void onWifiSettingsChanged();

    }

    /**
     * {@link android.widget.CompoundButton.OnCheckedChangeListener} for changes
     * to the state of the checkbox denoting a hidden SSID
     * 
     * @author Kirk
     */
    private class HiddenCheckedChangeListener implements
            CompoundButton.OnCheckedChangeListener {

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

                    wifiSettings.setHidden(checked);
                    notifyListener();
                    break;

                default:

                    break;

            }

        }

    }

    /**
     * {@Link RadioGroup.OnCheckedChangeListener} used to track selected
     * security protocol
     * 
     * @author Kirk
     */
    private class SecurityCheckedChangeListener implements
            RadioGroup.OnCheckedChangeListener {

        /**
         * Notify {@link #listener} of a change to the state of a
         * {@link RadioGroup}
         * 
         * @param group
         *            {@link RadioGroup}
         * 
         * @param id
         *            id of the checked radio button or -1 to indicate check
         *            cleared
         * 
         * @see android.widget.RadioGroup.OnCheckedChangeListener#onCheckedChanged(android
         *      .widget.RadioGroup, int)
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

            notifyListener();

        }

    }

    /**
     * {@link TextWatcher} for edits to SSID and password strings
     * 
     * @author Kirk
     */
    private class WifiSettingsTextWatcher implements TextWatcher {

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

            wifiSettings.setSsid(ssidText.getText().toString());
            wifiSettings.setPassword(passwordText.getText().toString());
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
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {

            // nothing to do here

        }

    }

    /**
     * Cache the singleton instance of {@link WifiSettings}
     */
    private static WifiSettings           wifiSettings;

    static {

        wifiSettings = WifiSettings.getInstance();

    }

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
     * wifiSettings
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
            WifiSettingsTextWatcher textWatcher = new WifiSettingsTextWatcher();
            ssidText.addTextChangedListener(textWatcher);
            passwordText.addTextChangedListener(textWatcher);
            securityGroup
                    .setOnCheckedChangeListener(new SecurityCheckedChangeListener());
            hiddenCheckBox
                    .setOnCheckedChangeListener(new HiddenCheckedChangeListener());
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
     * Update the state of the UI widgets to match the current wi fi
     * wifiSettings model state
     */
    public void updateSettings() {

        // don't queue up a bunch of notifications while updating multiple
        // widgets...
        delayNotifications += 1;

        try {

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

        } finally {

            // notify at most once for all of the preceding updates...
            delayNotifications -= 1;
            notifyListener();

        }
    }

    /**
     * Notify {@link #listener} that the wi fi wifiSettings habe been changed by
     * the user
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

}
