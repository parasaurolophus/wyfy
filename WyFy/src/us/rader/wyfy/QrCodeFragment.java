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
     * Invoke {@link QrCodeFragment#updateQrCode(WifiSettings)} in the UI thread
     * after a short delay in a worker thread.
     * 
     * @author Kirk
     */
    private class UpdateQrCodeTask extends
            AsyncTask<WifiSettings, Void, Bitmap> {

        /**
         * Sleep briefly
         * 
         * @param settings
         *            Get QR code image from <code>settings[0]</code>
         * 
         * @return {@link Bitmap} for QR code image
         * 
         * @see android.os.AsyncTask#doInBackground(WifiSettings...)
         */
        @Override
        protected Bitmap doInBackground(WifiSettings... settings) {

            try {

                int size = getQrCodeSize();

                if (size > 0) {

                    if (settings[0] == null) {

                        return createBlankBitmap(size);

                    }

                    return settings[0].getQrCode(size);

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
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {

            qrCode.setImageBitmap(bitmap);

        }

    }

    /**
     * {@link Bundle} key for {@link WifiSettings} argument
     */
    private static final String WIFI_SETTINGS = "WIFI_SETTINGS"; //$NON-NLS-1$

    /**
     * Factory method for instances of this class
     * 
     * @param settings
     *            {@link WifiSettings}
     * 
     * @return {@link QrCodeFragment}
     */
    public static QrCodeFragment newInstance(WifiSettings settings) {

        QrCodeFragment fragment = new QrCodeFragment();
        Bundle arguments = new Bundle();

        if (settings != null) {

            arguments.putSerializable(WIFI_SETTINGS, settings);

        }

        fragment.setArguments(arguments);
        return fragment;

    }

    /**
     * QR code image
     */
    private ImageView    qrCode;

    /**
     * Model
     */
    private WifiSettings settings;

    /**
     * Initialize {@link #settings} to <code>null</code>
     */
    public QrCodeFragment() {

        settings = null;

    }

    /**
     * Process arguments
     * 
     * @param savedInstanceState
     *            saved state or <code>null</code>
     * 
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();

        if (arguments != null) {

            settings = (WifiSettings) arguments.getSerializable(WIFI_SETTINGS);

        }
    }

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
     * Update the QR code when resuming
     * 
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {

        super.onResume();
        new UpdateQrCodeTask().execute(settings);

    }

    /**
     * Save {@link #settings} QR code to a file and then send a sharing
     * {@link Intent} wrapped in a chooser
     * 
     * @see Intent#ACTION_SEND
     * @see Intent#createChooser(Intent, CharSequence)
     * @see Activity#startActivity(Intent)
     */
    public void shareQrCode() {

        try {

            FragmentActivity activity = getActivity();
            Bitmap bitmap = settings.getQrCode(getQrCodeSize());
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
     * 
     * @param settings
     *            {@link WifiSettings}
     */
    public void updateQrCode(WifiSettings settings) {

        this.settings = settings;
        new UpdateQrCodeTask().execute(settings);

    }

    /**
     * Return a square {@link Bitmap} of the given size filled with
     * {@link Color#WHITE}
     * 
     * @param size
     *            the width and height of the {@link Bitmap}
     * 
     * @return the {@link Bitmap}
     */
    private Bitmap createBlankBitmap(int size) {

        Bitmap bitmap;
        bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        return bitmap;

    }

    /**
     * Get the size of the QR code image based on the current {@link View}
     * 
     * @return the size of the image
     */
    private int getQrCodeSize() {

        View view = getView().findViewById(R.id.qr_code_layout);
        int size = Math.min(view.getWidth(), view.getHeight());
        return size;

    }

}
