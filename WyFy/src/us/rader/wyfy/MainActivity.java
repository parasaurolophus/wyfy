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
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;

/**
 * Launcher {@link Activity} for <code>WyFy</code> app
 * 
 * @author Kirk
 */
public class MainActivity extends FragmentActivity implements
        WifiSettingsFragment.OnWifiSettingsChangedListener {

    /**
     * {@link QrCodeFragment} to notify when the {@link #settings} change
     */
    private QrCodeFragment qrCodeFragment;

    /**
     * Model
     */
    private WifiSettings   settings;

    /**
     * Initialize {@link #settings} to <code>null</code>
     */
    public MainActivity() {

        settings = null;

    }

    /**
     * Inflate the options {link Menu}
     * 
     * @param menu
     *            options {@ink Menu}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;

    }

    /**
     * Handle notification that the {@link WifiSettings} model state has been
     * changed by the user
     * 
     * @param newSettings
     *            {@link WifiSettings}
     * 
     * @see us.rader.wyfy.WifiSettingsFragment.OnWifiSettingsChangedListener#onWifiSettingsChanged(us.rader.wyfy.model.WifiSettings)
     */
    @Override
    public void onWifiSettingsChanged(WifiSettings newSettings) {

        settings = newSettings;
        qrCodeFragment.updateQrCode(settings);

    }

    /**
     * Prepare this instance to be displayed
     * 
     * Initialize {@link #settings} and attach the {@link Fragment} instances
     * according to the dynamically loaded layout
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (settings == null) {

            Intent intent = getIntent();

            if (intent != null) {

                Uri uri = intent.getData();

                if (uri != null) {

                    try {

                        settings = WifiSettings.parse(uri);

                    } catch (Exception e) {

                        Log.e(getClass().getName(), "error parsing URI", e); //$NON-NLS-1$

                    }
                }
            }
        }

        if (savedInstanceState != null) {

            return;

        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (findViewById(R.id.single_fragment) != null) {

            transaction.add(R.id.single_fragment,
                    WifiSettingsFragment.newInstance(settings));

        } else {

            transaction.add(R.id.first_fragment,
                    WifiSettingsFragment.newInstance(settings));
            qrCodeFragment = QrCodeFragment.newInstance(settings);
            transaction.add(R.id.second_fragment, qrCodeFragment);

        }

        transaction.commit();

    }

}
