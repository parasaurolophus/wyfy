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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * {link Fragment} to display the QR code representation of the current WIFI:
 * URI
 * 
 * @author Kirk
 */
public final class QrCodeFragment extends Fragment {

    /**
     * {@link Bundle} key for {@link WifiSettings} argument
     */
    private static final String WIFI_SETTINGS = "WIFI_SETTINGS"; //$NON-NLS-1$

    /**
     * Factory method for instances of this class
     * 
     * @param settings
     *            {@link WifiSettings}
     * 
     * @return {@link QrCodeFragment}
     */
    public static QrCodeFragment newInstance(WifiSettings settings) {

        QrCodeFragment fragment = new QrCodeFragment();
        Bundle arguments = new Bundle();

        if (settings != null) {

            arguments.putSerializable(WIFI_SETTINGS, settings);

        }

        fragment.setArguments(arguments);
        return fragment;

    }

    /**
     * Model
     */
    private WifiSettings settings;

    /**
     * TODO: replace with QR code image
     */
    private EditText     uriText;

    /**
     * Initialize {@link #settings} to <code>null</code>
     */
    public QrCodeFragment() {

        settings = null;

    }

    /**
     * Process arguments
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     * 
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();

        if (arguments != null) {

            settings = (WifiSettings) arguments.getSerializable(WIFI_SETTINGS);

        }
    }

    /**
     * Inflate the {@Link View}
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

        View view = inflater.inflate(R.layout.qr_code_fragment, container,
                false);
        uriText = (EditText) view.findViewById(R.id.qr_code_text);

        if (settings != null) {

            uriText.setText(settings.toString());

        }

        return view;

    }

    /**
     * Update the QR code to match the specified state
     * 
     * @param settings
     *            {@link WifiSettings}
     */
    public void updateQrCode(WifiSettings settings) {

        this.settings = settings;
        uriText.setText(settings.toString());

    }

}
