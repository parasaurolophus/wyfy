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
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
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
import android.widget.RadioGroup.OnCheckedChangeListener;

/**
 * {@link Fragment} for the WIFI settings UI
 * 
 * @author Kirk
 */
public class WifiSettingsFragment extends Fragment implements
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
         * 
         * @param settings
         *            {@link WifiSettings}
         */
        void onWifiSettingsChanged(WifiSettings settings);

    }

    /**
     * Use a worker thread initialize {@link WifiSettingsFragment#settings}
     * 
     * @author Kirk
     */
    private class GetActiveSettingsTask extends
            AsyncTask<Void, Void, WifiSettings> {

        /**
         * Invoke {@link WifiManager} API in a worker thread
         * 
         * @param params
         *            ignored
         * 
         * @return {@link WifiSettings} initialized accordng to the current
         *         active connection or <code>null</code>
         * 
         * @see android.os.AsyncTask#doInBackground(Void...)
         */
        @Override
        protected WifiSettings doInBackground(Void... params) {

            try {

                WifiManager manager = (WifiManager) getActivity()
                        .getSystemService(Context.WIFI_SERVICE);
                return WifiSettings.getActive(manager);

            } catch (Exception e) {

                Log.e(getClass().getName(), "doInBackground", e); //$NON-NLS-1$
                return null;

            }
        }

        /**
         * Update the UI to match settings values obtained using the
         * {@link WifiManager} API in a worker thread
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(WifiSettings result) {

            if (result == null) {

                updateWidgets(new WifiSettings());

            } else {

                updateWidgets(result);

            }
        }

    }

    /**
     * Arguments bundle key for the {@link WifiSettings} parameter
     */
    private static final String WIFI_SETTINGS = "WIFI_SETTINGS"; //$NON-NLS-1$

    /**
     * Create a new instance of {@link WifiSettingsFragment}
     * 
     * @param settings
     *            {@link WifiSettings}
     * 
     * @return {@link WifiSettingsFragment}
     */
    public static WifiSettingsFragment newInstance(WifiSettings settings) {

        WifiSettingsFragment fragment = new WifiSettingsFragment();
        Bundle arguments = new Bundle();

        if (settings != null) {

            arguments.putSerializable(WIFI_SETTINGS, settings);

        }

        fragment.setArguments(arguments);
        return fragment;

    }

    /**
     * {@link CheckBox} for a wifi access point with a SSID that isn't broadcast
     */
    CheckBox                              hiddenCheckBox;

    /**
     * {@link EditText} for a wifi access point password string or WEP key
     */
    EditText                              passwordText;

    /**
     * {@link RadioGroup} to select a wifi access point's security protocol
     */
    RadioGroup                            securityGroup;

    /**
     * {@link EditText} for a wifi access point SSID string
     */
    EditText                              ssidText;

    /**
     * The {@link OnWifiSettingsChangedListener} to notify of changes to
     * {@link #settings}
     */
    private OnWifiSettingsChangedListener listener;

    /**
     * Model
     */
    private WifiSettings                  settings;

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

        if (settings == null) {

            return;

        }

        settings.setSsid(ssidText.getText().toString());
        settings.setPassword(passwordText.getText().toString());
        notifyListener(settings);

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

        if (settings == null) {

            return;

        }

        switch (button.getId()) {

            case R.id.hidden_checkbox:

                settings.setHidden(checked);
                notifyListener(settings);
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

        if (settings == null) {

            return;

        }

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

        notifyListener(settings);

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
        settings = null;
        Bundle arguments = getArguments();

        if (arguments != null) {

            settings = (WifiSettings) arguments.getSerializable(WIFI_SETTINGS);

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

        View view = inflater.inflate(R.layout.wifi_settings_fragment,
                container, false);
        ssidText = (EditText) view.findViewById(R.id.ssid_text);
        ssidText.addTextChangedListener(this);
        passwordText = (EditText) view.findViewById(R.id.password_text);
        passwordText.addTextChangedListener(this);
        securityGroup = (RadioGroup) view.findViewById(R.id.security_group);
        securityGroup.setOnCheckedChangeListener(this);
        hiddenCheckBox = (CheckBox) view.findViewById(R.id.hidden_checkbox);
        hiddenCheckBox.setOnCheckedChangeListener(this);

        if (settings != null) {

            updateWidgets(settings);

        }

        View focus = view.findFocus();

        if (focus != null) {

            securityGroup.requestFocus();

        }

        return view;

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
     * Show this instance
     * 
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {

        super.onResume();

        if (settings == null) {

            new GetActiveSettingsTask().execute();

        }
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
     * Update the state of the UI widgets to match the current state of
     * {@link #settings}
     * 
     * @param settings
     *            {@link WifiSettings}
     */
    public void updateWidgets(WifiSettings settings) {

        this.settings = settings;
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
    }

    /**
     * Notify {@link #listener} that the state of {@link #settings} has been
     * changed by the user
     * 
     * @param settings
     *            {@link WifiSettings}
     */
    private void notifyListener(WifiSettings settings) {

        if (listener != null) {

            listener.onWifiSettingsChanged(settings);

        }
    }

}
