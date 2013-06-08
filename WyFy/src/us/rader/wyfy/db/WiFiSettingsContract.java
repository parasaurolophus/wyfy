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
package us.rader.wyfy.db;

import us.rader.wyfy.model.WifiSettings;
import android.content.ContentValues;
import android.provider.BaseColumns;

/**
 * SQLlite contract for persisting instances of {@link WifiSettings}
 * 
 * @author Kirk
 */
public final class WiFiSettingsContract {

    /**
     * Table for storing instances of {@link WifiSettings}
     * 
     * @author Kirk
     */
    public static final class WifiSettingsEntry implements BaseColumns {

        /**
         * "Hidden" state column name
         */
        public static final String COLUMN_NAME_HIDDEN   = "hidden";       //$NON-NLS-1$

        /**
         * Password column name
         */
        public static final String COLUMN_NAME_PASSWORD = "password";     //$NON-NLS-1$

        /**
         * Security column name
         */
        public static final String COLUMN_NAME_SECURITY = "security";     //$NON-NLS-1$

        /**
         * SSID column name
         */
        public static final String COLUMN_NAME_SSID     = "ssid";         //$NON-NLS-1$

        /**
         * {@link WifiSettings} table name
         */
        public static final String TABLE_NAME           = "wifi_settings"; //$NON-NLS-1$

        /**
         * Get a {@link ContentValues} to use to persist the current state of
         * the {@link WifiSettings} singleton
         * 
         * @return {@link ContentValues}
         * 
         * @see #updateWifiSettings(ContentValues)
         */
        public static ContentValues getContentValues() {

            WifiSettings settings = WifiSettings.getInstance();
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_HIDDEN, settings.isHidden());
            values.put(COLUMN_NAME_PASSWORD, settings.getPassword());
            values.put(COLUMN_NAME_SECURITY, settings.getSecurity().toString());
            values.put(COLUMN_NAME_SSID, settings.getSsid());
            return values;

        }

        /**
         * Update the state of the singleton {@link WifiSettings} from the given
         * {@link ContentValues}
         * 
         * @param values
         *            {@link ContentValues}
         * 
         * @see #getContentValues()
         */
        public static void updateWifiSettings(ContentValues values) {

            String ssid = values.getAsString(COLUMN_NAME_SSID);
            String password = values.getAsString(COLUMN_NAME_PASSWORD);
            WifiSettings.Security security = Enum.valueOf(
                    WifiSettings.Security.class,
                    values.getAsString(COLUMN_NAME_SECURITY));
            boolean hidden = values.getAsBoolean(COLUMN_NAME_HIDDEN);
            WifiSettings settings = WifiSettings.getInstance();
            settings.setSsid(ssid);
            settings.setPassword(password);
            settings.setSecurity(security);
            settings.setHidden(hidden);

        }

        /**
         * Prevent casual instantiation of contract member
         */
        private WifiSettingsEntry() {

            // nothing to do here

        }

    }

    /**
     * Prevent casual instantiation of contract class
     */
    private WiFiSettingsContract() {

        // nothing to do here

    }

}
