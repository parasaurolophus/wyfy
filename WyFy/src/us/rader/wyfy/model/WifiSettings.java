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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import us.rader.wyfy.db.WiFiSettingsContract.WifiSettingsEntry;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

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
     */
    public enum ConnectionOutcome {

        /**
         * Added and enabled
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

        /**
         * Serialization version number
         */
        private static final long serialVersionUID = 1L;

    }

    /**
     * Enumeration of supported wifi security protocols
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
     * The empty string
     */
    public static final String  EMPTY_STRING        = "";     //$NON-NLS-1$

    /**
     * Double-quote
     */
    private static final String DOUBLE_QUOTE        = "\"";   //$NON-NLS-1$

    /**
     * Serialization version number
     */
    private static final long   serialVersionUID    = 1L;

    /**
     * The singleton instance
     */
    private static WifiSettings singleton;

    /**
     * Scheme for WIFI: URI's
     */
    private static final String URI_SCHEME          = "WIFI:"; //$NON-NLS-1$

    /**
     * T: parameter value for WEP security protocol
     */
    private static final String WEP_PARAMETER_VALUE = "WEP";  //$NON-NLS-1$

    /**
     * T: parameter value for WPA security protocol
     */
    private static final String WPA_PARAMETER_VALUE = "WPA";  //$NON-NLS-1$

    static {

        singleton = new WifiSettings();

    }

    /**
     * @return {@link #singleton}
     */
    public static WifiSettings getInstance() {

        return singleton;

    }

    /**
     * Add the specified entry to the list of configured wi fi networks
     * 
     * TODO: this makes a lot of assumptions based on a small sampling of actual
     * wi fi configurations -- need more research and testing!
     * 
     * @param manager
     *            the {@link WifiManager}
     * 
     * @param wrappedSsid
     *            the SSID string wrapped in double-quotes
     * 
     * @param password
     *            the password string
     * 
     * @param securityProtcol
     *            the {@link Security} value
     * 
     * @return the network id or -1 on failure
     * 
     * @see #initialize(WifiConfiguration)
     */
    private static int addNetwork(WifiManager manager, String wrappedSsid,
            String password, Security securityProtcol) {

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = wrappedSsid;
        config.allowedKeyManagement.clear();
        config.allowedAuthAlgorithms.set(WifiConfiguration.KeyMgmt.NONE);
        config.allowedGroupCiphers.clear();

        switch (securityProtcol) {

            case NONE:

                break;

            case WEP:

                config.wepKeys = new String[] { addQuotes(password) };

                if (password.length() == 10) {

                    config.allowedGroupCiphers
                            .set(WifiConfiguration.GroupCipher.WEP40);

                } else {

                    config.allowedAuthAlgorithms
                            .set(WifiConfiguration.GroupCipher.WEP104);

                }

                break;

            case WPA:

                config.preSharedKey = addQuotes(password);
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
     * Wrap the given <code>string</code> in double-quotes
     * 
     * @param string
     *            the string
     * 
     * @return the wrapped string
     */
    private static String addQuotes(String string) {

        return DOUBLE_QUOTE + string + DOUBLE_QUOTE;

    }

    /**
     * Parse a token that begins with ':' and ends with ';'
     * 
     * <p>
     * This is used to parse the parameter values from a WIFI: URI, e.g.
     * S:ssid;P:password; etc.
     * </p>
     * 
     * <p>
     * Throws {@link IllegalArgumentException} if the next character in the
     * stream isn't ':' or if end of input is reached without finding ';'.
     * Otherwise, it returns the (possibly empty) string containing all of the
     * characters (if any) after the initial ':' and before the next ';'
     * </p>
     * 
     * <p>
     * TODO: this will not work correctly if the parameter value itself contains
     * a literal ';' or white-space characters at the beginning or end -- need
     * to research the "official" syntax (hard to do for such an ad hoc format)
     * </p>
     * 
     * @param reader
     *            {@link BufferedReader}
     * 
     * @return the parsed token, i.e. everything after the initial ':' and
     *         before the next ';'
     * 
     * @throws IOException
     *             if an I/O error occurs
     * 
     * @throws IllegalArgumentException
     *             if the URI is ill-formed, i.e. the next character read from
     *             the stream isn't ':' or end of input is reached without
     *             finding ';'
     */
    private static String parseParameterValue(BufferedReader reader)
            throws IOException {

        int c = skipWhitespace(reader);

        if (c == -1) {

            throw new IllegalArgumentException("end of input when ':' expected"); //$NON-NLS-1$

        }

        if (c != ':') {

            throw new IllegalArgumentException("':' expected"); //$NON-NLS-1$

        }

        StringBuffer buffer = new StringBuffer();

        while ((c = reader.read()) != ';') {

            if (c == -1) {

                throw new IllegalArgumentException(
                        "end of input when ';' expected"); //$NON-NLS-1$

            }

            buffer.append((char) c);

        }

        return buffer.toString().trim();

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
    private static String removeQuotes(String string) {

        if (string == null) {

            return EMPTY_STRING;

        }

        if (string.startsWith(DOUBLE_QUOTE) && string.endsWith(DOUBLE_QUOTE)) {

            return string.substring(1, string.length() - 1);

        }

        return string;

    }

    /**
     * Return the next non-white-space character found in the given stream
     * 
     * Discards any white-space characters found and marks the position of the
     * returned character for use by {@link BufferedReader#reset()}
     * 
     * @param reader
     *            {@link BufferedReader}
     * 
     * @return the next non-white-space character or -1 if end of input is
     *         reached
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    private static int skipWhitespace(BufferedReader reader) throws IOException {

        int c;

        do {

            reader.mark(1);
            c = reader.read();

            if (c == -1) {

                return -1;

            }

        } while (Character.isWhitespace(c));

        return c;

    }

    /**
     * Convert a T: URI parameter's value to the corresponding {@link Security}
     * constant
     * 
     * @param parameterValue
     *            the token
     * 
     * @return {@link Security}
     */
    private static Security toSecurity(String parameterValue) {

        if (WEP_PARAMETER_VALUE.equalsIgnoreCase(parameterValue)) {

            return Security.WEP;

        } else if (WPA_PARAMETER_VALUE.equalsIgnoreCase(parameterValue)) {

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
    private WifiSettings() {

        ssid = EMPTY_STRING;
        password = EMPTY_STRING;
        security = Security.NONE;
        hidden = false;

    }

    /**
     * Activate the WIFI connection represented by this instance
     * 
     * This should <code>always</code> be invoked on a worker thread
     * 
     * @param manager
     *            {@link WifiManager}
     * 
     * @return <code>true</code> if and only if the connection was successfully
     *         enabled
     */
    public ConnectionOutcome connect(WifiManager manager) {

        String wrappedSsid = addQuotes(ssid);

        for (WifiConfiguration configuration : manager.getConfiguredNetworks()) {

            if (configuration.SSID.equals(wrappedSsid)) {

                if (manager.enableNetwork(configuration.networkId, false)) {

                    return ConnectionOutcome.ENABLED;

                }

                return ConnectionOutcome.FAILED;

            }
        }

        int networkId = addNetwork(manager, wrappedSsid, password, security);

        if (networkId == -1) {

            return ConnectionOutcome.FAILED;

        }

        if (manager.enableNetwork(networkId, false)) {

            return ConnectionOutcome.ADDED;

        }

        return ConnectionOutcome.FAILED;

    }

    /**
     * Update this instance to match the state of the active WIFI connection, if
     * any
     * 
     * <p>
     * This should always be called in a worker thread
     * </p>
     * 
     * <p>
     * Doesn't change the state of this instance if there is no active wi fi
     * connection, in which case this method will return <code>false</code>
     * </p>
     * 
     * @param manager
     *            {@link WifiManager}
     * 
     * @return <code>true</code> if and only if this instance was updated to
     *         match the current wi fi connection
     */
    public boolean getActiveConnection(WifiManager manager) {

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

    }

    /**
     * Get a {@link ContentValues} to use to persist the state of this instance
     * 
     * @return {@link ContentValues}
     * 
     * @see WifiSettings#update(ContentValues)
     */
    public ContentValues getContentValues() {

        ContentValues values = new ContentValues();
        values.put(WifiSettingsEntry.COLUMN_NAME_HIDDEN, isHidden());
        values.put(WifiSettingsEntry.COLUMN_NAME_PASSWORD, getPassword());
        values.put(WifiSettingsEntry.COLUMN_NAME_SECURITY, getSecurity()
                .toString());
        values.put(WifiSettingsEntry.COLUMN_NAME_SSID, getSsid());
        return values;

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
     * @param foregroundColor
     *            foreground color
     * 
     * @param backgroundColor
     *            background color
     * 
     * @param size
     *            size of the QR code bitmap to create
     * 
     * @return QR code {@link Bitmap}
     * 
     * @throws WriterException
     *             if a zxing error occurs
     */
    public Bitmap getQrCode(int foregroundColor, int backgroundColor, int size)
            throws WriterException {

        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        String uri = toString();
        BitMatrix bitMatrix = writer.encode(uri, BarcodeFormat.QR_CODE, size,
                size, hints);
        Bitmap bitmap = Bitmap
                .createBitmap(size, size, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < bitMatrix.getHeight(); ++y) {

            for (int x = 0; x < bitMatrix.getWidth(); ++x) {

                bitmap.setPixel(x, y, (bitMatrix.get(x, y) ? foregroundColor
                        : backgroundColor));

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
     * @return current value of hidden
     */
    public boolean isHidden() {

        return hidden;

    }

    /**
     * Parse WIFI: URI from the given {@link Reader}
     * 
     * Note that this doesn't enforce any required parameters (i.e. S:) nor
     * consistency rules (e.g. missing P: when T: isn't "nopass"), check for
     * duplicate parameters etc.
     * 
     * @param reader
     *            {@link BufferedReader}
     * 
     * @return <code>true</code> if and only if URI was successfully parsed
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public boolean parse(BufferedReader reader) throws IOException {

        try {

            int c = skipWhitespace(reader);

            if (c == -1) {

                Log.w(getClass().getName(),
                        "end of input reached while expecting WIFI: URI"); //$NON-NLS-1$
                return false;

            }

            reader.reset();
            char[] chars = new char[URI_SCHEME.length()];
            int actual = reader.read(chars);
            String prefix = new String(chars, 0, actual);

            if (!URI_SCHEME.equalsIgnoreCase(prefix)) {

                Log.w(getClass().getName(), "URI must start with '" //$NON-NLS-1$
                        + URI_SCHEME + "' but found '" //$NON-NLS-1$
                        + prefix + "'"); //$NON-NLS-1$
                return false;

            }

            while ((c = skipWhitespace(reader)) != ';') {

                if (c == -1) {

                    return false;

                }

                char parameter = Character.toUpperCase((char) c);
                String token = parseParameterValue(reader);

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

                        Log.w(getClass().getName(),
                                "Unrecognized URI parameter name '" //$NON-NLS-1$
                                        + parameter + "'"); //$NON-NLS-1$
                        return false;

                }
            }

            return true;

        } catch (IllegalArgumentException e) {

            Log.w(getClass().getName(), "parse(BufferedReader)", e); //$NON-NLS-1$
            return false;

        }

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
     * @see #parse(Reader)
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
     * @see #parse(BufferedReader)
     */
    public boolean parse(Reader reader) throws IOException {

        return parse(new BufferedReader(reader));

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
     * @see #parse(InputStream)
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
     * @see #parse(String)
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

        StringBuffer buffer = new StringBuffer(URI_SCHEME + "S:"); //$NON-NLS-1$
        buffer.append(ssid);
        buffer.append(';');

        switch (security) {

            case WEP:

                buffer.append("T:WEP;P:"); //$NON-NLS-1$
                buffer.append(password);
                buffer.append(';');
                break;

            case WPA:

                buffer.append("T:WPA;P:"); //$NON-NLS-1$
                buffer.append(password);
                buffer.append(';');
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

    /**
     * Update the state of this instance from the given {@link ContentValues}
     * 
     * @param values
     *            {@link ContentValues}
     * 
     * @see #getContentValues()
     */
    public void update(ContentValues values) {

        String ssid = values.getAsString(WifiSettingsEntry.COLUMN_NAME_SSID);
        String password = values
                .getAsString(WifiSettingsEntry.COLUMN_NAME_PASSWORD);
        Security security = Enum.valueOf(Security.class,
                values.getAsString(WifiSettingsEntry.COLUMN_NAME_SECURITY));
        boolean hidden = values
                .getAsBoolean(WifiSettingsEntry.COLUMN_NAME_HIDDEN);
        setSsid(ssid);
        setPassword(password);
        setSecurity(security);
        setHidden(hidden);

    }

    /**
     * Initialize this instance from the given {@link WifiConfiguration}
     * 
     * TODO: crude implementation; research how this is supposed to work
     * 
     * @param configuration
     *            {@link WifiConfiguration}
     * 
     * @return <code>true</code> if and only if successful
     * 
     * @see #addNetwork(WifiManager, String, String, Security)
     */
    private boolean initialize(WifiConfiguration configuration) {

        setSsid(removeQuotes(configuration.SSID));
        setHidden(configuration.hiddenSSID);

        if ((configuration.preSharedKey != null)
                && !EMPTY_STRING.equals(configuration.preSharedKey)) {

            setSecurity(Security.WPA);
            setPassword(removeQuotes(configuration.preSharedKey));
            return true;

        }

        if ((configuration.wepKeys != null)
                && (configuration.wepKeys.length > 0)) {

            setSecurity(Security.WEP);
            setPassword(removeQuotes(configuration.wepKeys[0]));
            return true;

        }

        setSecurity(Security.NONE);
        setPassword(EMPTY_STRING);
        return true;

    }

}
