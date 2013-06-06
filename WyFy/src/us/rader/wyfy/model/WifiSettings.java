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
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Model class for Android WIFI settings
 * 
 * @author Kirk
 */
public final class WifiSettings implements Serializable {

    /**
     * Enumerated type returned by {@link WifiSettings#connect(WifiManager)}
     * 
     * @author Kirk
     */
    public enum ConnectionOutcome {

        /**
         * Added and successfully connected
         */
        ADDED,

        /**
         * Already existed, so just enabled
         */
        ENABLED,

        /**
         * Failed
         */
        FAILED;

    }

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

        /**
         * Serialization version number
         */
        private static final long serialVersionUID = 1L;

    }

    /**
     * Serialization version number
     */
    private static final long   serialVersionUID = 1L;

    /**
     * The singleton instance
     */
    private static WifiSettings singleton;

    static {

        singleton = new WifiSettings();

    }

    /**
     * Return {@link #singleton}
     * 
     * @return {@link #singleton}
     */
    public static WifiSettings getInstance() {

        return singleton;

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

            default:

                throw new IllegalStateException(
                        "Illegal security constant " + securityProtcol); //$NON-NLS-1$

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

            return ""; //$NON-NLS-1$

        }

        return "\"" + string + "\""; //$NON-NLS-1$//$NON-NLS-2$

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

            return ""; //$NON-NLS-1$

        }

        if (string.startsWith("\"") && string.endsWith("\"")) { //$NON-NLS-1$//$NON-NLS-2$

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

            throw new IllegalArgumentException("':' expected"); //$NON-NLS-1$

        }

        if (c != ':') {

            throw new IllegalArgumentException("':' expected"); //$NON-NLS-1$

        }

        StringBuffer buffer = new StringBuffer();

        while ((c = reader.read()) != ';') {

            if (c == -1) {

                throw new IllegalArgumentException("';' expected"); //$NON-NLS-1$

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

        if ("WEP".equalsIgnoreCase(token)) { //$NON-NLS-1$

            return Security.WEP;

        } else if ("WPA".equalsIgnoreCase(token)) { //$NON-NLS-1$

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
    protected WifiSettings() {

        ssid = ""; //$NON-NLS-1$
        password = ""; //$NON-NLS-1$
        security = Security.NONE;
        hidden = false;

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
    public ConnectionOutcome connect(WifiManager manager) {

        String denormalizedSsid = denormalize(ssid);

        for (WifiConfiguration configuration : manager.getConfiguredNetworks()) {

            if (configuration.SSID.equals(denormalizedSsid)) {

                if (manager.enableNetwork(configuration.networkId, false)) {

                    return ConnectionOutcome.ENABLED;

                }

                return ConnectionOutcome.FAILED;

            }
        }

        int networkId = addNetwork(manager, denormalizedSsid, password,
                security);

        if (networkId == -1) {

            return ConnectionOutcome.FAILED;

        }

        if (manager.enableNetwork(networkId, false)) {

            return ConnectionOutcome.ADDED;

        }

        return ConnectionOutcome.FAILED;

    }

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
    public boolean getActiveConnection(WifiManager manager) {

        try {

            WifiInfo info = manager.getConnectionInfo();

            if (info != null) {

                int activeId = info.getNetworkId();

                if (activeId != -1) {

                    for (WifiConfiguration configuration : manager
                            .getConfiguredNetworks()) {

                        if (configuration.networkId == activeId) {

                            return initialize(configuration);

                        }
                    }
                }
            }

            return false;

        } finally {

            // TODO: putting in a little delay here to allow the UI to settle
            // down at
            // launch. otherwise, QrCodeFragment.updateQrCode() is invoked when
            // it is not yet ready to display its image -- investigate better
            // ways to deal with this
            try {

                Thread.sleep(500);

            } catch (InterruptedException e) {

                // ignore this exception

            }

        }
    }

    /**
     * @return current value of password
     */
    public String getPassword() {

        return password;

    }

    /**
     * Create the QR code {@link Bitmap}
     * 
     * @param size
     *            size of the QR code bitmap to create
     * 
     * @return QR code {@link Bitmap}
     * 
     * @throws WriterException
     *             if a zxing error occurs
     */
    public Bitmap getQrCode(int size) throws WriterException {

        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        BitMatrix bitMatrix = writer.encode(toString(), BarcodeFormat.QR_CODE,
                size, size, hints);
        Bitmap bitmap = Bitmap
                .createBitmap(size, size, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < bitMatrix.getHeight(); ++y) {

            for (int x = 0; x < bitMatrix.getWidth(); ++x) {

                bitmap.setPixel(x, y, (bitMatrix.get(x, y) ? Color.BLACK
                        : Color.WHITE));

            }
        }

        return bitmap;

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
     * Initialize this instance from the given {@link WifiConfiguration}
     * 
     * TODO: crude implementation; research how this is supposed to work
     * 
     * @param configuration
     *            {@link WifiConfiguration}
     * 
     * @return <code>true</code> if successful
     */
    public boolean initialize(WifiConfiguration configuration) {

        setSsid(normalize(configuration.SSID));
        setHidden(configuration.hiddenSSID);

        if ((configuration.preSharedKey != null)
                && !"".equals(configuration.preSharedKey)) { //$NON-NLS-1$

            setSecurity(Security.WPA);
            setPassword(normalize(configuration.preSharedKey));
            return true;

        }

        if ((configuration.wepKeys != null)
                && (configuration.wepKeys.length > 0)) {

            setSecurity(Security.WEP);
            setPassword(normalize(configuration.wepKeys[0]));
            return true;

        }

        setSecurity(Security.NONE);
        setPassword(""); //$NON-NLS-1$
        return true;

    }

    /**
     * @return current value of hidden
     */
    public boolean isHidden() {

        return hidden;

    }

    /**
     * Parse WIFI: URI from the given {@link InputStream}
     * 
     * @param stream
     *            {@link InputStream}
     * 
     * @return <code>true</code> if and only if URI was successfully parsed
     * 
     * @throws IOException
     *             if an I/O error occurs
     * 
     * @throws IllegalArgumentException
     *             if the URI is ill-formed
     */
    public boolean parse(InputStream stream) throws IOException {

        return parse(new InputStreamReader(stream));

    }

    /**
     * Parse WIFI: URI from the given {@link Reader}
     * 
     * @param reader
     *            {@link Reader}
     * 
     * @return <code>true</code> if and only if URI was successfully parsed
     * 
     * @throws IOException
     *             if an I/O error occurs
     * 
     * @throws IllegalArgumentException
     *             if the URI is ill-formed
     */
    public boolean parse(Reader reader) throws IOException {

        char[] chars = new char[5];
        int actual = reader.read(chars);

        if (actual != chars.length) {

            return false;

        }

        String prefix = new String(chars);

        if (!"WIFI:".equalsIgnoreCase(prefix)) { //$NON-NLS-1$

            return false;

        }

        int c;

        while ((c = reader.read()) != ';') {

            if (c == -1) {

                return false;

            }

            char parameter = Character.toUpperCase((char) c);
            String token = parseToken(reader);

            switch (parameter) {

                case 'S':

                    setSsid(token);
                    break;

                case 'P':

                    setPassword(token);
                    break;

                case 'H':

                    setHidden(Boolean.parseBoolean(token));
                    break;

                case 'T':

                    setSecurity(toSecurity(token));
                    break;

                default:

                    return false;

            }
        }

        return true;

    }

    /**
     * Parse the given WIFI: URI string
     * 
     * @param uri
     *            URI string
     * 
     * @throws IOException
     *             if an I/O error occurs
     * 
     * @return <code>true</code> if and only if URI was successfully parsed
     * 
     * @throws IllegalArgumentException
     *             if the URI is ill-formed
     */
    public boolean parse(String uri) throws IOException {

        return parse(new ByteArrayInputStream(uri.getBytes("US-ASCII"))); //$NON-NLS-1$

    }

    /**
     * Parse the given WIFI: {@link Uri}
     * 
     * @param uri
     *            {@link Uri}
     * 
     * @return <code>true</code> if and only if URI was successfully parsed
     * 
     * @throws IOException
     *             if an I/O error occurs
     * 
     * @throws IllegalArgumentException
     *             if the URI is ill-formed
     */
    public boolean parse(Uri uri) throws IOException {

        return parse(uri.toString());

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

        StringBuffer buffer = new StringBuffer("WIFI:S:"); //$NON-NLS-1$
        buffer.append(ssid);
        buffer.append(';');

        switch (security) {

            case WEP:

                buffer.append("P:"); //$NON-NLS-1$
                buffer.append(password);
                buffer.append(";T:WEP;"); //$NON-NLS-1$
                break;

            case WPA:

                buffer.append("P:"); //$NON-NLS-1$
                buffer.append(password);
                buffer.append(";T:WPA;"); //$NON-NLS-1$
                break;

            default:

                buffer.append("T:nopass;"); //$NON-NLS-1$
                break;

        }

        if (hidden) {

            buffer.append("H:true;"); //$NON-NLS-1$

        }

        buffer.append(';');
        return buffer.toString();

    }

}
