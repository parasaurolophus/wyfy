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
package us.rader.wyfy.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;

import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Model class for Android WIFI settings
 * 
 * @author Kirk
 */
public final class WifiSettings implements Serializable {

    /**
     * Enumeratio of supported wifi security protocols
     * 
     * @author Kirk
     */
    public enum Security {

        /**
         * No security
         */
        NONE,

        /**
         * WEP security
         */
        WEP,

        /**
         * WPA / WPA2 security
         */
        WPA;

    }

    /**
     * Serialization version number
     */
    private static final long serialVersionUID = 1L;

    /**
     * Return a {@link WifiSettings} that reflects the state of the active WIFI
     * connection, if any
     * 
     * Returns <code>null</code> if there is no active WIFI connection
     * 
     * @param manager
     *            {@link WifiManager}
     * 
     * @return {@link WifiSettings} or <code>null</code>
     */
    public static WifiSettings getActive(WifiManager manager) {

        WifiInfo info = manager.getConnectionInfo();

        if (info == null) {

            return null;

        }

        int activeId = info.getNetworkId();

        if (activeId == -1) {

            return null;

        }

        for (WifiConfiguration configuration : manager.getConfiguredNetworks()) {

            if (configuration.networkId == activeId) {

                return new WifiSettings(configuration);

            }
        }

        return null;

    }

    /**
     * Parse WIFI: URI from the given {@link InputStream}
     * 
     * @param stream
     *            {@link InputStream}
     * 
     * @return {@link WifiSettings}
     * 
     * @throws IOException
     *             if an I/O error occurs
     * 
     * @throws IllegalArgumentException
     *             if the URI is ill-formed
     */
    public static WifiSettings parse(InputStream stream) throws IOException {

        return parse(new InputStreamReader(stream));

    }

    /**
     * Parse WIFI: URI from the given {@link Reader}
     * 
     * @param reader
     *            {@link Reader}
     * 
     * @return {@link WifiSettings}
     * 
     * @throws IOException
     *             if an I/O error occurs
     * 
     * @throws IllegalArgumentException
     *             if the URI is ill-formed
     */
    public static WifiSettings parse(Reader reader) throws IOException {

        char[] chars = new char[5];
        int actual = reader.read(chars);

        if (actual != chars.length) {

            throw new IllegalArgumentException("URI must begin with 'WIFI:'");

        }

        String prefix = new String(chars);

        if (!"WIFI:".equalsIgnoreCase(prefix)) {

            throw new IllegalArgumentException("URI must begin with 'WIFI:'");

        }

        int c;
        WifiSettings settings = new WifiSettings();

        while ((c = reader.read()) != ';') {

            if (c == -1) {

                throw new IllegalArgumentException("unexpected end of input");

            }

            char parameter = Character.toUpperCase((char) c);
            String token = parseToken(reader);

            switch (parameter) {

                case 'S':

                    settings.setSsid(token);
                    break;

                case 'P':

                    settings.setPassword(token);
                    break;

                case 'H':

                    settings.setHidden(Boolean.parseBoolean(token));
                    break;

                case 'T':

                    settings.setSecurity(toSecurity(token));
                    break;

                default:

                    throw new IllegalArgumentException(
                            "unrecognized parameter " + parameter);

            }
        }

        return settings;

    }

    /**
     * Parse the given WIFI: URI string
     * 
     * @param uri
     *            URI string
     * 
     * @return {@link WifiSettings}
     * 
     * @throws IOException
     *             if an I/O error occurs
     * 
     * @throws IllegalArgumentException
     *             if the URI is ill-formed
     */
    public static WifiSettings parse(String uri) throws IOException {

        return parse(new ByteArrayInputStream(uri.getBytes("US-ASCII")));

    }

    /**
     * Parse the given WIFI: {@link Uri}
     * 
     * @param uri
     *            {@link Uri}
     * 
     * @return {@link WifiSettings}
     * 
     * @throws IOException
     *             if an I/O error occurs
     * 
     * @throws IllegalArgumentException
     *             if the URI is ill-formed
     */
    public static WifiSettings parse(Uri uri) throws IOException {

        return parse(uri.toString());

    }

    /**
     * Add the specific item to the list of cofigured WiFi networks
     * 
     * TODO: this makes a lot of assumptions based on a small statistical
     * sampling of WIFI configurations. need more research!
     * 
     * @param manager
     *            the {@link WifiManager}
     * 
     * @param denormalizedSsid
     *            the denormalized SSID string
     * 
     * @param password
     *            the password string
     * 
     * @param securityProtcol
     *            the {@link Security} value
     * 
     * @return the network id
     */
    private static int addNetwork(WifiManager manager, String denormalizedSsid,
            String password, Security securityProtcol) {

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = denormalizedSsid;
        config.allowedKeyManagement.clear();
        config.allowedAuthAlgorithms.set(WifiConfiguration.KeyMgmt.NONE);
        config.allowedGroupCiphers.clear();

        switch (securityProtcol) {

            case NONE:

                break;

            case WEP:

                config.wepKeys = new String[] { denormalize(password) };

                if (password.length() == 10) {

                    config.allowedGroupCiphers
                            .set(WifiConfiguration.GroupCipher.WEP40);

                } else {

                    config.allowedAuthAlgorithms
                            .set(WifiConfiguration.GroupCipher.WEP104);

                }

                break;

            case WPA:

                config.preSharedKey = denormalize(password);
                config.allowedGroupCiphers
                        .set(WifiConfiguration.GroupCipher.CCMP);

                break;

        }

        return manager.addNetwork(config);

    }

    /**
     * Wrap the given string in double-quoes
     * 
     * TODO: this should probably try to detect whether such wrapping is
     * actually necessary for a given string. Need more research
     * 
     * @param string
     *            the string
     * 
     * @return the wrapped string
     */
    private static String denormalize(String string) {

        if (string == null) {

            return "";

        }

        return "\"" + string + "\"";

    }

    /**
     * Remove the first and last character from the given string if they are
     * double quotes
     * 
     * @param string
     *            the string to unwrap
     * 
     * @return the unwrapped string
     */
    private static String normalize(String string) {

        if (string == null) {

            return "";

        }

        if (string.startsWith("\"") && string.endsWith("\"")) {

            return string.substring(1, string.length() - 1);

        }

        return string;

    }

    /**
     * Parse a token that begins with ':' and ends with ';'
     * 
     * @param reader
     *            {@link Reader}
     * 
     * @return the parsed token
     * 
     * @throws IOException
     *             if an I/O error occurs
     * 
     * @throws IllegalArgumentException
     *             if the URI is ill-formed
     */
    private static String parseToken(Reader reader) throws IOException {

        int c = reader.read();

        if (c == -1) {

            throw new IllegalArgumentException("':' expected");

        }

        if (c != ':') {

            throw new IllegalArgumentException("':' expected");

        }

        StringBuffer buffer = new StringBuffer();

        while ((c = reader.read()) != ';') {

            if (c == -1) {

                throw new IllegalArgumentException("';' expected");

            }

            buffer.append((char) c);

        }

        return buffer.toString();

    }

    /**
     * Convert a T: URI token to the corresponding {@link Security} constant
     * 
     * @param token
     *            the token
     * 
     * @return {@link Security}
     */
    private static Security toSecurity(String token) {

        if ("WEP".equalsIgnoreCase(token)) {

            return Security.WEP;

        } else if ("WPA".equalsIgnoreCase(token)) {

            return Security.WPA;

        } else {

            return Security.NONE;

        }
    }

    /**
     * Hidden SSID
     */
    private boolean  hidden;

    /**
     * Password
     */
    private String   password;

    /**
     * Security protocol
     */
    private Security security;

    /**
     * SSID
     */
    private String   ssid;

    /**
     * Initialize to default state
     */
    public WifiSettings() {

        ssid = "";
        password = "";
        security = Security.NONE;
        hidden = false;

    }

    /**
     * Initialize this instance from the given {@link WifiConfiguration}
     * 
     * TODO: crude implementation; research how this is supposed to work
     * 
     * @param configuration
     *            {@link WifiConfiguration}
     */
    public WifiSettings(WifiConfiguration configuration) {

        setSsid(normalize(configuration.SSID));
        setHidden(configuration.hiddenSSID);

        if ((configuration.preSharedKey != null)
                && !"".equals(configuration.preSharedKey)) {

            setSecurity(Security.WPA);
            setPassword(normalize(configuration.preSharedKey));

        } else if ((configuration.wepKeys != null)
                && (configuration.wepKeys.length > 0)) {

            setSecurity(Security.WEP);
            setPassword(normalize(configuration.wepKeys[0]));

        } else {

            setSecurity(Security.NONE);
            setPassword("");

        }
    }

    /**
     * Activate the WIFI connection represented by this instance
     * 
     * <p>
     * This should always be invoked on a worker thread
     * </p>
     * 
     * <p>
     * TODO: crude implementation; research how this is supposed to work
     * </p>
     * 
     * @param manager
     *            {@link WifiManager}
     * 
     * @return <code>true</code> if and only if the connection was successfully
     *         activated
     */
    public boolean connect(WifiManager manager) {

        String denormalizedSsid = denormalize(ssid);

        for (WifiConfiguration configuration : manager.getConfiguredNetworks()) {

            if (configuration.SSID.equals(denormalizedSsid)) {

                return manager.enableNetwork(configuration.networkId, false);

            }
        }

        int networkId = addNetwork(manager, denormalizedSsid, password,
                security);
        return manager.enableNetwork(networkId, false);

    }

    /**
     * @return current value of password
     */
    public String getPassword() {

        return password;

    }

    /**
     * @return current value of security
     */
    public Security getSecurity() {

        return security;

    }

    /**
     * @return current value of ssid
     */
    public String getSsid() {

        return ssid;

    }

    /**
     * @return current value of hidden
     */
    public boolean isHidden() {

        return hidden;

    }

    /**
     * @param hidden
     *            new value for hidden
     */
    public void setHidden(boolean hidden) {

        this.hidden = hidden;

    }

    /**
     * @param password
     *            new value for password
     */
    public void setPassword(String password) {

        this.password = password;

    }

    /**
     * @param security
     *            new value for security
     */
    public void setSecurity(Security security) {

        this.security = security;

    }

    /**
     * @param ssid
     *            new value for ssid
     */
    public void setSsid(String ssid) {

        this.ssid = ssid;

    }

    /**
     * Return the WIFI: URI for this instance
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        StringBuffer buffer = new StringBuffer("WIFI:S:");
        buffer.append(ssid);
        buffer.append(';');

        switch (security) {

            case WEP:

                buffer.append("P:");
                buffer.append(password);
                buffer.append(";T:WEP;");
                break;

            case WPA:

                buffer.append("P:");
                buffer.append(password);
                buffer.append(";T:WPA;");
                break;

            default:

                buffer.append("T:nopass;");
                break;

        }

        if (hidden) {

            buffer.append("H:true;");

        }

        buffer.append(';');
        return buffer.toString();

    }

}
