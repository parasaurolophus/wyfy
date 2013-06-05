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

import android.nfc.NdefRecord;

/**
 * Constants used in formatting and parsing NDEF "U" records
 * 
 * @author Kirk
 */
interface WellKnowUriStrings {

    /**
     * URI prefix for "U" records with code byte 0
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_00         = "";                          //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 1
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_01         = "http://www.";               //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 2
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_02         = "https://www.";              //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 3
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_03         = "http://";                   //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 4
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_04         = "https://";                  //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 5
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_05         = "tel:";                      //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 6
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_06         = "mailto:";                   //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 7
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_07         = "ftp://anonymous:anonymous@"; //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 8
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_08         = "ftp://ftp.";                //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 9
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_09         = "ftps://";                   //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 10
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_10         = "sftp://";                   //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 11
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_11         = "smb://";                    //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 12
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_12         = "nfs://";                    //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 13
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_13         = "ftp://";                    //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 14
     */
    static final String   URI_PREFIX_14         = "dav://";                    //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 15
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_15         = "news:";                     //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 16
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_16         = "telnet://";                 //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 17
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_17         = "imap:";                     //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 18
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_18         = "rtsp://";                   //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 19
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_19         = "urn:";                      //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 20
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_20         = "pop:";                      //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 21
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_21         = "sip:";                      //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 22
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_22         = "sips:";                     //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 23
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_23         = "tftp:";                     //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 24
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_24         = "btspp://";                  //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 25
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_25         = "btl2cap://";                //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 26
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_26         = "btgoep://";                 //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 27
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_27         = "tcpobex://";                //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 28
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_28         = "irdaobex://";               //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 29
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_29         = "file://";                   //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 30
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_30         = "urn:epc:id:";               //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 31
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_31         = "urn:epc:tag:";              //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 32
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_32         = "urn:epc:pat:";              //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 33
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_33         = "urn:epc:raw:";              //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 34
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_34         = "urn:epc:";                  //$NON-NLS-1$
    /**
     * URI prefix for "U" records with code byte 35
     * 
     * @see #WELL_KNOWN_URI_PREFIX
     */
    static final String   URI_PREFIX_35         = "urn:nfc:";                  //$NON-NLS-1$
    /**
     * Strings to prepend to the URI in a NDEF "well known URI" record.
     * 
     * <p>
     * The format of a NDEF "Well known URI" record, is as follows:
     * </p>
     * 
     * <ul>
     * 
     * <li>{@link NdefRecord#getTnf()} returns {@link NdefRecord#TNF_WELL_KNOWN}
     * 
     * <li>{@link NdefRecord#getType()} returns an array with the same contents
     * as {@link NdefRecord#RTD_URI} (i.e. the ASCII representation of the
     * single-character string "U")
     * 
     * <li>The first byte of the array returned by
     * {@link NdefRecord#getPayload()} contains a code denoting a standard URI
     * prefix as defined in the NDEF forum specification for such records
     * 
     * <li>The remaining bytes of the array returned by
     * {@link NdefRecord#getPayload()} contain the ASCII characters to append to
     * the prefix denoted by the first byte
     * 
     * </ul>
     * 
     * <p>
     * The number and order of the entries in this array must match the values
     * that appear as the first byte in the payload of such records as laid out
     * in the NDEF format specifcation documents.
     * </p>
     */
    static final String[] WELL_KNOWN_URI_PREFIX = { URI_PREFIX_00,
            URI_PREFIX_01, URI_PREFIX_02, URI_PREFIX_03, URI_PREFIX_04,
            URI_PREFIX_05, URI_PREFIX_06, URI_PREFIX_07, URI_PREFIX_08,
            URI_PREFIX_09, URI_PREFIX_10, URI_PREFIX_11, URI_PREFIX_12,
            URI_PREFIX_13, URI_PREFIX_14, URI_PREFIX_15, URI_PREFIX_16,
            URI_PREFIX_17, URI_PREFIX_18, URI_PREFIX_19, URI_PREFIX_20,
            URI_PREFIX_21, URI_PREFIX_22, URI_PREFIX_23, URI_PREFIX_24,
            URI_PREFIX_25, URI_PREFIX_26, URI_PREFIX_27, URI_PREFIX_28,
            URI_PREFIX_29, URI_PREFIX_30, URI_PREFIX_31, URI_PREFIX_32,
            URI_PREFIX_33, URI_PREFIX_34, URI_PREFIX_35 };

}
