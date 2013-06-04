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

import us.rader.wyfy.R;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.util.Log;

/**
 * 
 * @author Kirk
 */
public abstract class NdefWriterActivity extends NdefReaderActivity {

    /**
     * Create a AAR {@link NdefRecord} for the given {@link Package}
     * 
     * @param pkg
     *            the {@link Package}
     * 
     * @return AAR {@link NdefRecord}
     */
    public static NdefRecord createAar(Package pkg) {

        return createAar(pkg.getName());

    }

    /**
     * Create a AAR {@link NdefRecord} for the given {@link Package} name
     * 
     * <p>
     * Even though AAR records will only be used by devices running ice cream
     * sandwich or later, this method uses API's available since gingerbread mr1
     * to create them. They will be benignly ignored by older devices when
     * reading tags that include them.
     * </p>
     * 
     * <p>
     * TODO: inferred this format by inspecting some actual AAR records created
     * using {@link NdefRecord#createApplicationRecord(String)} (that only
     * became available in ice cream sandwich). Should investigate if there is,
     * somewhere, an official public specification.
     * </p>
     * 
     * @param pkg
     *            the {@link Package} name
     * 
     * @return AAR {@link NdefRecord}
     */
    public static NdefRecord createAar(String pkg) {

        try {

            byte[] type = "android.com:pkg".getBytes("US-ASCII"); //$NON-NLS-1$//$NON-NLS-2$
            byte[] payload = pkg.getBytes("US-ASCII"); //$NON-NLS-1$
            return new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, type, null,
                    payload);

        } catch (UnsupportedEncodingException e) {

            Log.e(NdefWriterActivity.class.getName(), "createAar", e); //$NON-NLS-1$
            throw new IllegalArgumentException(e);

        }
    }

    /**
     * Return MIME {@link NdefRecord}
     * 
     * @param type
     *            the MIME type string
     * 
     * @param payload
     *            the payload bytes
     * 
     * @return MIME {@link NdefRecord}
     * 
     * @see #createMime(String, String, String)
     */
    public static NdefRecord createMime(String type, byte[] payload) {

        try {

            byte[] bytes = type.getBytes("US-ASCII"); //$NON-NLS-1$
            return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, bytes, null,
                    payload);

        } catch (UnsupportedEncodingException e) {

            Log.e(NdefWriterActivity.class.getName(), "createMime", e); //$NON-NLS-1$
            throw new IllegalArgumentException(e);

        }
    }

    /**
     * Return MIME {@link NdefRecord}
     * 
     * @param type
     *            the MIME type string
     * 
     * @param payload
     *            the payload string
     * 
     * @param encoding
     *            the encoding for the payload (e.g. "US-ASCII", "UTF-8" etc.)
     * 
     * @return MIME {@link NdefRecord}
     * 
     * @see #createMime(String, byte[])
     */
    public static NdefRecord createMime(String type, String payload,
            String encoding) {

        try {

            byte[] bytes = payload.getBytes(encoding);
            return createMime(type, bytes);

        } catch (UnsupportedEncodingException e) {

            Log.e(NdefWriterActivity.class.getName(), "createMime", e); //$NON-NLS-1$
            throw new IllegalArgumentException(e);

        }
    }

    /**
     * Return a "T" {@link NdefRecord}
     * 
     * @param language
     *            the language code
     * 
     * @param text
     *            the text string
     * 
     * @return "T" {@link NdefRecord}
     */
    public static NdefRecord createText(String language, String text) {

        try {

            byte[] languageBytes = language.getBytes("UTF-8"); //$NON-NLS-1$
            byte[] textBytes = text.getBytes("UTF-8"); //$NON-NLS-1$
            byte[] payload = new byte[languageBytes.length + textBytes.length
                    + 1];
            payload[0] = (byte) languageBytes.length;
            System.arraycopy(languageBytes, 0, payload, 1, languageBytes.length);
            System.arraycopy(textBytes, 0, payload, languageBytes.length + 1,
                    textBytes.length);
            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_TEXT, null, payload);

        } catch (UnsupportedEncodingException e) {

            Log.e(NdefWriterActivity.class.getName(), "createText", e); //$NON-NLS-1$
            throw new IllegalArgumentException(e);

        }
    }

    /**
     * Create a "U" {@link NdefRecord}
     * 
     * @param uri
     *            the URI string
     * 
     * @return "U" {@link NdefRecord}
     * 
     * @see #createUri(Uri)
     */
    public static NdefRecord createUri(String uri) {

        try {

            String prefix = ""; //$NON-NLS-1$
            String suffix = uri;
            int code = 0;

            for (int index = 1; index < URI_PREFIXES.length; ++index) {

                if (uri.startsWith(URI_PREFIXES[index])) {

                    prefix = URI_PREFIXES[index];
                    suffix = uri.substring(prefix.length());
                    code = index;
                    break;

                }

            }

            byte[] bytes = suffix.getBytes("US-ASCII"); //$NON-NLS-1$
            byte[] payload = new byte[bytes.length + 1];
            payload[0] = (byte) code;
            System.arraycopy(bytes, 0, payload, 1, bytes.length);
            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_URI, null, payload);

        } catch (UnsupportedEncodingException e) {

            Log.e(NdefWriterActivity.class.getName(), "createUri", e); //$NON-NLS-1$
            throw new IllegalArgumentException(e);

        }
    }

    /**
     * Create a "U" {@link NdefRecord}
     * 
     * @param uri
     *            the {@link Uri}
     * 
     * @return "U" {@link NdefRecord}
     * 
     * @see #createUri(String)
     */
    public static NdefRecord createUri(Uri uri) {

        return createUri(uri.toString());

    }

    /**
     * If <code>true</code>, write-protect a {@link Tag} after writing to it.
     * Otherwise, leave the {@link Tag} writable
     */
    private boolean writeProtectRequested;

    /**
     * Pass required parameter to super class constructor
     * 
     * @param requestCode
     *            the foreground dispatch request code to pass to
     *            {@link NdefReaderActivity#NdefReaderActivity(int)}
     */
    protected NdefWriterActivity(int requestCode) {

        super(requestCode);
        writeProtectRequested = false;

    }

    /**
     * Return the current value of {@link #writeProtectRequested}
     * 
     * @return {@link #writeProtectRequested}
     */
    public final boolean isWriteProtectRequested() {

        return writeProtectRequested;

    }

    /**
     * Update the value of {@link #writeProtectRequested}
     * 
     * @param writeProtectRequested
     *            new value for {@link #writeProtectRequested}
     */
    public final void setWriteProtectRequested(boolean writeProtectRequested) {

        this.writeProtectRequested = writeProtectRequested;

    }

    /**
     * Return the {@link NdefMessage} to write to the tag
     * 
     * @param currentMessage
     *            the current contents of the tag, or <code>null</code> if the
     *            tag is empty
     * 
     * @return the {@link NdefMessage} to write or <code>null</code> to indicate
     *         that the tag should be left as-is
     */
    protected abstract NdefMessage createNdefMessage(NdefMessage currentMessage);

    /**
     * Write the value returned by {@link #createNdefMessage(NdefMessage)} to
     * the given {@link Tag}
     * 
     * @param tag
     *            the {@link Tag}
     * 
     * @param task
     *            the {@link ForegroundDispatchActivity.ProcessTagTask} running
     *            this method
     * 
     * @return the {@link NdefMessage} that was written to the {@link Tag} or
     *         <code>null</code>
     * 
     * @see NdefReaderActivity#processTag(Tag,
     *      ForegroundDispatchActivity.ProcessTagTask)
     */
    @Override
    protected final NdefMessage processTag(Tag tag, ProcessTagTask task) {

        try {

            Ndef ndef = Ndef.get(tag);

            if (ndef != null) {

                return writeNdef(ndef, task);

            }

            NdefFormatable formatable = NdefFormatable.get(tag);

            if (formatable != null) {

                return writeFormatable(formatable, task);

            }

            task.report(getString(R.string.incompatible_tag));
            return null;

        } catch (Exception e) {

            Log.e(NdefWriterActivity.class.getName(), "processTag", e); //$NON-NLS-1$
            return null;

        }
    }

    /**
     * Write the result of calling {@link #createNdefMessage(NdefMessage)} to
     * the given {@link NdefFormatable} tag
     * 
     * @param formatable
     *            the {@link NdefFormatable} tag
     * 
     * @param task
     *            the {@link ForegroundDispatchActivity.ProcessTagTask} running
     *            this method
     * 
     * @return the {@link NdefMessage} written to the tag
     */
    private NdefMessage writeFormatable(NdefFormatable formatable,
            ProcessTagTask task) {

        try {

            NdefMessage ndefMessage = createNdefMessage(null);

            if (ndefMessage == null) {

                task.cancel(false);
                return null;

            }

            formatable.connect();

            try {

                if (writeProtectRequested) {

                    formatable.formatReadOnly(ndefMessage);

                } else {

                    formatable.format(ndefMessage);

                }

                return ndefMessage;

            } finally {

                formatable.close();

            }

        } catch (Exception e) {

            Log.e(NdefWriterActivity.class.getName(), "writeFormatable", e); //$NON-NLS-1$
            task.report(getString(R.string.error_formatting_tag));
            return null;

        }
    }

    /**
     * Write the result of calling {@link #createNdefMessage(NdefMessage)} to
     * the given {@link Ndef} formatted tag
     * 
     * @param ndef
     *            the {@link Ndef} tag
     * 
     * @param task
     *            the {@link ForegroundDispatchActivity.ProcessTagTask} running
     *            this method
     * 
     * @return the {@link NdefMessage} written to the tag
     */
    private NdefMessage writeNdef(Ndef ndef, ProcessTagTask task) {

        try {

            if (!ndef.isWritable()) {

                task.report(getString(R.string.read_only_tag));
                return null;

            }

            NdefMessage ndefMessage = createNdefMessage(ndef
                    .getCachedNdefMessage());

            if (ndefMessage == null) {

                task.cancel(false);
                return null;

            }

            byte[] bytes = ndefMessage.toByteArray();
            int max = ndef.getMaxSize();

            if (bytes.length > max) {

                task.report(getString(R.string.tag_size_exceeded, bytes.length,
                        max));
                return null;

            }

            ndef.connect();

            try {

                ndef.writeNdefMessage(ndefMessage);

                if (writeProtectRequested) {

                    if (!ndef.makeReadOnly()) {

                        task.report(getString(R.string.failed_to_write_protect_tag));
                        return null;

                    }
                }

                return ndefMessage;

            } finally {

                ndef.close();

            }

        } catch (Exception e) {

            Log.e(NdefWriterActivity.class.getName(), "writeNdef", e); //$NON-NLS-1$
            task.report(getString(R.string.error_writing_tag));
            return null;

        }
    }

}
