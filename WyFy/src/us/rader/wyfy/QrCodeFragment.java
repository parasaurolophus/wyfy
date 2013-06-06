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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import us.rader.wyfy.model.WifiSettings;
import us.rader.wyfy.provider.FileProvider;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * {link Fragment} to display the QR code representation of the current WIFI:
 * URI
 * 
 * @author Kirk
 */
public final class QrCodeFragment extends Fragment {

    /**
     * Create the QR image {@link Bitmap} in a worker thread and update
     * {@link QrCodeFragment#qrCode} in the UI thread
     * 
     * @author Kirk
     */
    private class UpdateQrCodeTask extends AsyncTask<Void, Void, Bitmap> {

        /**
         * Return the {@link Bitmap} for the QR code image representing
         * <code>wifiSettings[0]</code>
         * 
         * @param params
         *            ignored
         * 
         * @return {@link Bitmap} for QR code image or <code>null</code>
         * 
         * @see android.os.AsyncTask#doInBackground(Void...)
         * @see WifiSettings#getQrCode(int)
         * @see #onPostExecute(Bitmap)
         */
        @Override
        protected Bitmap doInBackground(Void... params) {

            try {

                int size = getQrCodeSize();

                if (size > 0) {

                    return wifiSettings.getQrCode(size);

                }

            } catch (Exception e) {

                Log.e(getClass().getName(), "doInBackground", e); //$NON-NLS-1$

            }

            return null;

        }

        /**
         * Set the {@link Bitmap} for {@link QrCodeFragment#qrCode}
         * 
         * @param bitmap
         *            the {@link Bitmap}
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         * @see #doInBackground(Void...)
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (bitmap != null) {

                qrCode.setImageBitmap(bitmap);

            }
        }

    }

    /**
     * Cache the singleton instance of {@link WifiSettings}
     */
    private static WifiSettings wifiSettings;

    static {

        wifiSettings = WifiSettings.getInstance();

    }

    /**
     * QR code image
     */
    private ImageView           qrCode;

    /**
     * Inflate the {@Link View}
     * 
     * @param inflater
     *            {@link LayoutInflater}
     * 
     * @param container
     *            {@link ViewGroup}
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     * 
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     *      android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.qr_code_fragment, container,
                false);
        qrCode = (ImageView) view.findViewById(R.id.qr_code);
        Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        qrCode.setImageBitmap(bitmap);

        qrCode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                shareQrCode();

            }

        });

        return view;

    }

    /**
     * Update the QR code bitmap now that the wifiSettings have, presumably,
     * been restored and the view is, hopefully, ready
     * 
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {

        super.onResume();

        // TODO: figure out a better way to work around state-management bug in
        // Android UI classes than using a Timer to delay this initial refresh
        TimerTask task = new TimerTask() {

            @Override
            public void run() {

                updateQrCode();

            }

        };

        new Timer().schedule(task, 500);

    }

    /**
     * Save QR code to a file and then send a sharing {@link Intent} wrapped in
     * a chooser
     * 
     * @see Intent#ACTION_SEND
     * @see Intent#createChooser(Intent, CharSequence)
     * @see Activity#startActivity(Intent)
     */
    public void shareQrCode() {

        try {

            FragmentActivity activity = getActivity();
            Bitmap bitmap = wifiSettings.getQrCode(getQrCodeSize());
            File file = activity.getFileStreamPath("wyfy_qr.png"); //$NON-NLS-1$
            FileOutputStream stream = activity
                    .openFileOutput(file.getName(), 0);

            try {

                bitmap.compress(CompressFormat.PNG, 100, stream);

            } finally {

                stream.close();

            }

            String label = getString(R.string.share_label);
            Uri uri = FileProvider
                    .getContentUri(getString(R.string.provider_authority_file),
                            file.getName());
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setData(uri);
            intent.setType(FileProvider.getMimeType(uri));
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(intent, label);
            startActivity(chooser);

        } catch (Exception e) {

            // ignore errors here

        }
    }

    /**
     * Update the QR code to match the specified state
     */
    public void updateQrCode() {

        new UpdateQrCodeTask().execute();

    }

    /**
     * Get the size of the QR code image based on the current {@link View}
     * 
     * @return the size of the image
     */
    private int getQrCodeSize() {

        View view = getView();

        if (view == null) {

            return 0;

        }

        View qrView = view.findViewById(R.id.qr_code_layout);

        if (qrView == null) {

            return 0;

        }

        int size = Math.min(qrView.getWidth(), qrView.getHeight());
        return size;

    }

}
