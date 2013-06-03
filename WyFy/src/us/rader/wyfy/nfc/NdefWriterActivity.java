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

import us.rader.wyfy.R;
import android.nfc.NdefMessage;
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
     * If <code>true</code>, write-protect a {@link Tag} after writing to it.
     * Otherwise, leave the {@link Tag} writable
     */
    private boolean writeProtectRequested;

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
