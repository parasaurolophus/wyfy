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
package us.rader.wyfy.nfc;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import us.rader.wyfy.R;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.util.Log;

/**
 * {@link ForegroundDispatchActivity} that extracts a {@link NdefMessage} from a
 * {@link Tag}
 * 
 * @author Kirk
 */
public abstract class NdefReaderActivity extends
        ForegroundDispatchActivity<NdefMessage> implements WellKnowUriStrings {

    /**
     * Decode the payload of certain kinds of {@link NdefRecord}
     * 
     * This will return the URI from "U" records, the text from "T" records and
     * so on.
     * 
     * @param record
     *            the {@link NdefRecord}
     * 
     * @return the decoded payload or <code>null</code> if <code>record</code>
     *         isn't supported by this method
     */
    public static String decodePayload(NdefRecord record) {

        try {

            if (record == null) {

                return null;

            }

            switch (record.getTnf()) {

                case NdefRecord.TNF_ABSOLUTE_URI:

                    return new String(record.getType(), "US-ASCII"); //$NON-NLS-1$

                case NdefRecord.TNF_MIME_MEDIA:

                    return decodeMime(new String(record.getType(), "US-ASCII"), //$NON-NLS-1$
                            record.getPayload());

                case NdefRecord.TNF_WELL_KNOWN:

                    return decodeWellKnown(new String(record.getType(),
                            "US-ASCII"), record.getPayload()); //$NON-NLS-1$

                default:

                    return null;

            }

        } catch (Exception e) {

            Log.e(NdefReaderActivity.class.getName(), "decodePayload", e); //$NON-NLS-1$
            return null;

        }
    }

    /**
     * Return <code>payload</code> decoded as a UTF-8 string if
     * <code>type</code> starts with "text/"
     * 
     * <p>
     * Return <code>null</code> for any MIME type that doesn't start with
     * "text/"
     * </p>
     * 
     * @param type
     *            the MIME type
     * 
     * @param payload
     *            the bytes to decode
     * 
     * @return the decoded <code>payload</code> or <code>null</code>
     * 
     * @throws UnsupportedEncodingException
     *             if there is a bug in the Java virtual machine
     */
    private static String decodeMime(String type, byte[] payload)
            throws UnsupportedEncodingException {

        if (type.equalsIgnoreCase("application/xml")) { //$NON-NLS-1$

            return new String(payload, "UTF-8"); //$NON-NLS-1$
        }

        if (type.equalsIgnoreCase("application/json")) { //$NON-NLS-1$

            return new String(payload, "UTF-8"); //$NON-NLS-1$
        }

        if (type.toLowerCase(Locale.US).startsWith("text/")) { //$NON-NLS-1$

            return new String(payload, "UTF-8"); //$NON-NLS-1$

        }

        return null;

    }

    /**
     * Decode the <code>payload</code> of a "T" record
     * 
     * <p>
     * This parses and discards any language-code prefix in the
     * <code>payload</code>
     * </p>
     * 
     * @param payload
     *            the bytes to decode
     * 
     * @return the decoded <code>payload</code>
     * 
     * @throws UnsupportedEncodingException
     *             if there is a bug in the Java virtual machine
     */
    private static String decodeText(byte[] payload)
            throws UnsupportedEncodingException {

        int code = payload[0];
        int languageLength = code & 0x7F;
        int encodingFlag = code & 0x80;
        String encoding;

        if (encodingFlag == 0) {

            encoding = "UTF-8"; //$NON-NLS-1$

        } else {

            encoding = "UTF-16"; //$NON-NLS-1$

        }

        byte[] bytes = new byte[payload.length - languageLength - 1];
        System.arraycopy(payload, 1, bytes, 0, bytes.length);
        return new String(bytes, encoding);

    }

    /**
     * Decode the <code>payload</code> of a "U" record
     * 
     * @param payload
     *            the bytes to decode
     * 
     * @return the decoded URI or <code>null</code>
     * 
     * @throws UnsupportedEncodingException
     *             if there is a bug in the Java virtual machine
     */
    private static String decodeUri(byte[] payload)
            throws UnsupportedEncodingException {

        int code = payload[0];
        String suffix = new String(payload, 1, payload.length - 1, "US-ASCII"); //$NON-NLS-1$
        String prefix = ""; //$NON-NLS-1$

        if ((code > 0) && (code < WELL_KNOWN_URI_PREFIX.length)) {

            prefix = WELL_KNOWN_URI_PREFIX[code];

        }

        return prefix + suffix;
    }

    /**
     * Decode <code>payload</code> as indicated by the given "well known" RTD
     * <code>type</code>
     * 
     * Return <code>null</code> if <code>type</code> isn't supported
     * 
     * @param type
     *            the RTD
     * 
     * @param payload
     *            the bytes to decode
     * 
     * @return the decoded bytes or <code>null</code>
     * 
     * @throws UnsupportedEncodingException
     *             if there is a bug in the Java virtual machine
     */
    private static String decodeWellKnown(String type, byte[] payload)
            throws UnsupportedEncodingException {

        if ("T".equals(type)) { //$NON-NLS-1$

            return decodeText(payload);
        }

        if ("U".equals(type)) { //$NON-NLS-1$

            return decodeUri(payload);

        }

        return null;

    }

    /**
     * Pass <code>requestCode</code> to
     * {@link ForegroundDispatchActivity#ForegroundDispatchActivity(int)}
     * 
     * @param requestCode
     *            value to pass to
     *            {@link ForegroundDispatchActivity#ForegroundDispatchActivity(int)}
     * 
     * @see ForegroundDispatchActivity#ForegroundDispatchActivity(int)
     */
    protected NdefReaderActivity(int requestCode) {

        super(requestCode);

    }

    /**
     * Extract an {@link NdefMessage} from the given {@link Tag}
     * 
     * @param tag
     *            the {@link Tag}
     * 
     * @param task
     *            the {@link ForegroundDispatchActivity.ProcessTagTask} running
     *            this method
     * 
     * @return the {@link NdefMessage} or <code>null</code> if something goes
     *         wrong
     * 
     * @see ForegroundDispatchActivity#processTag(android.nfc.Tag,
     *      ForegroundDispatchActivity.ProcessTagTask)
     */
    @Override
    protected NdefMessage processTag(Tag tag, ProcessTagTask task) {

        Ndef ndef = Ndef.get(tag);

        if (ndef == null) {

            task.report(getString(R.string.incompatible_tag));
            return null;

        }

        NdefMessage ndefMessage = ndef.getCachedNdefMessage();

        if (null == ndefMessage) {

            task.report(getString(R.string.empty_tag));

        }

        return ndefMessage;

    }

}
