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
import android.provider.BaseColumns;

/**
 * SQLlite contract for persisting {@link WifiSettings}
 * 
 * @author Kirk
 */
public final class WiFiSettingsContract {

    /**
     * Table for storing instances of {@link WifiSettings}
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
         * Prevent casual instantiation of contract member class
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
