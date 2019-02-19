/*
* Copyright (C) 2016 Texas Instruments Incorporated - http://www.ti.com/
*
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions
*  are met:
*
*    Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
*    Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in the
*    documentation and/or other materials provided with the
*    distribution.
*
*    Neither the name of Texas Instruments Incorporated nor the names of
*    its contributors may be used to endorse or promote products derived
*    from this software without specific prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
*  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
*  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
*  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
*  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
*  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
*  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
*  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
*  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
*  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
*  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
*/

package com.ti.smartconfig;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ti.smartconfig.utils.Constants;
import com.ti.smartconfig.utils.Device_Type_Enum;
import com.ti.smartconfig.utils.NetworkUtil;
import com.ti.smartconfig.utils.Popup;
import com.ti.smartconfig.utils.SharedPreferencesInterface_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@EFragment(R.layout.activity_ota_view)
public class OtaFragment extends Fragment implements View.OnClickListener {
    //     String download_file_path = "https://docs.google.com/uc?authuser=0&id=0B_79IOgREGJ4VjZnT0NmWUJ3Y00&export=download";//  - for custom download check
//    String download_file_path = "https://docs.google.com/uc?authuser=0&id=0B_79IOgREGJ4SFRqQ1FTX2NuQUU&export=download";
//    String download_file_path = "https://docs.google.com/uc?authuser=0&id=0B_79IOgREGJ4SFRqQ1FTX2NuQUU&export=download";//no
//    String download_file_path = "https://docs.google.com/uc?id=0B_whXeuPlba2QXJFTTh1dTRNcUU&export=download";//appid api
//    String download_file_path = "https://docs.google.com/uc?authuser=0&id=0B_whXeuPlba2ZHg5c21nX1BTWTg&export=download";//this is appname api - but only manually!
//    String download_file_path = "https://docs.google.com/uc?authuser=0&id=0B_79IOgREGJ4bHpvb3M2T2x0Zjg&export=download";//no

    private static final String TAG = "OtaFragment";
    public SharedPreferences sharedpreferences;
    public static final String mypreference = "iot";
    public static final String Name = "deviceIP";
    Boolean statusFlagTrigger = false;
    Boolean cancelAsyncTaskProcess;
    byte[] downloadedFile;
    UploadStatusAsyncTask uploadStatusAsyncTask;
    DeviceVersionAsyncTask deviceVersionAsyncTask;
    UploadNewVersionAsyncTask uploadNewVersionAsyncTask;
    FileDownloadAsyncTask fileDownloadAsyncTask;
    String deviceIp;
    String currentVersion;
    String deviceVersionUrl;
    String deviceUploadUrl;
    String uploadStatusUrl;
    String versionNumber;
    String REGEX = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    @ViewById
    RelativeLayout tab_ota_configuration_laoder_layout;
    @ViewById
    TextView tab_ota_configuration_laoder_label;
    @Pref
    SharedPreferencesInterface_ prefs;
    @ViewById
    ImageButton check_version_button;
    @ViewById
    Button update_version_button;
    @ViewById
    ProgressBar software_update_progressBar;
    @ViewById
    TextView update_software_append_textview;
    @ViewById
    TextView newest_software_version_title_textview_update;
    @ViewById
    TextView tab_ota_progressbar_textview;
    @ViewById
    TextView current_sw_version_textview_update;
    @ViewById
    Button software_status_button;
    @ViewById
    RelativeLayout upload_status_box;
    @ViewById
    ScrollView scroller;

    /**
     * Called after fragment is initialized
     */
    @AfterViews
    void afterViews() {

        cancelAsyncTaskProcess = false;
        sharedpreferences = getActivity().getSharedPreferences(mypreference, Context.MODE_PRIVATE);
        if (sharedpreferences.contains(Name)) {
            deviceIp = (sharedpreferences.getString(Name, ""));
        }
        if (NetworkUtil.getConnectedSSID(getActivity()) == null) {
            Toast.makeText(getActivity(), "Check WiFi connection first", Toast.LENGTH_SHORT).show();
        }
        deviceVersionUrl = "http://" + deviceIp + "/ota?version";
        deviceUploadUrl = "http://" + deviceIp + "/ota?filename=";
        uploadStatusUrl = "http://" + deviceIp + ":5432/?x";
        DeviceVersionStatus();
        check_version_button.setOnClickListener(this);
        update_version_button.setOnClickListener(this);
        software_status_button.setOnClickListener(this);
        //Developer mode long click
        check_version_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                DeveloperModeDialog();
                return false;
            }
        });
    }
//
//            Handler handler = new Handler();
//            int numberOfTaps = 0;
//            long lastTapTimeMs = 0;
//            long touchDownMs = 0;
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        touchDownMs = System.currentTimeMillis();
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        handler.removeCallbacksAndMessages(null);
//                        if ((System.currentTimeMillis() - touchDownMs) > ViewConfiguration.getTapTimeout()) {
//                            //it was not a tap
//                            numberOfTaps = 0;
//                            lastTapTimeMs = 0;
//                            break;
//                        }
//
//                        if (numberOfTaps > 0
//                                && (System.currentTimeMillis() - lastTapTimeMs) < ViewConfiguration.getDoubleTapTimeout()) {
//                            numberOfTaps += 1;
//                        } else {
//                            numberOfTaps = 1;
//                        }
//
//                        lastTapTimeMs = System.currentTimeMillis();
//
//                        if (numberOfTaps == 2) {
//                            //handle double tap
//                            //     showDeveloperOption();
//                            DeveloperModeDialog();
//                        }
//                }
//                return true;
//            }
//        });


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.update_version_button:
                if (downloadedFile == null || downloadedFile.length < 1) {
                    Toast.makeText(getActivity(), "Please download the latest version first", Toast.LENGTH_SHORT).show();
                } else {
                    if (current_sw_version_textview_update.getText().equals("No information")) {
                        Toast.makeText(getActivity(), "Please connect to the right WiFi and try again", Toast.LENGTH_SHORT).show();
                        DeviceVersionStatus();
                        break;
                    }
                    if (newest_software_version_title_textview_update.getText().equals(current_sw_version_textview_update.getText())) {
                        //dialog triggered
                        SameVersionDialog();
                        break;
                    } else {
                        statusFlagTrigger = true;
                        uploadStatusAsyncTask = new UploadStatusAsyncTask();
                        uploadStatusAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uploadStatusUrl);
                        upload_status_box.setVisibility(View.VISIBLE);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //fired after 200ms
                                versionNumber = newest_software_version_title_textview_update.getText().toString();
                                uploadNewVersionAsyncTask = new UploadNewVersionAsyncTask();
                                uploadNewVersionAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, deviceUploadUrl);
                            }
                        }, 200);
                    }
                }
                break;
            case R.id.software_status_button:
                update_software_append_textview.setText("");
                Device_Type_Enum deviceTypeEnum = ((MainActivity)getActivity()).deviceTypeEnum;
                Log.i(TAG,"deviceTypeEnum: " + deviceTypeEnum);
                if (deviceTypeEnum!= null) {
                    String fileDownloadPath = Constants.DOWNLOAD_FILE_PATH_PREFIX;
                    Log.i(TAG,"fileDownloadPath: " + fileDownloadPath);

                    switch (deviceTypeEnum){
                        case F_Device:
                            fileDownloadPath += "_fs.txt";
                            Log.i(TAG,"fileDownloadPath: " + fileDownloadPath);

                            break;
                        case S_Device:
                            fileDownloadPath += "_rs.txt";
                            Log.i(TAG,"fileDownloadPath: " + fileDownloadPath);

                            break;
                        case R_Device:

                            break;
                    }
                fileDownloadAsyncTask = new FileDownloadAsyncTask();
                fileDownloadAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fileDownloadPath);
                }
                break;

        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onResume() {
        super.onResume();
        upload_status_box.setVisibility(View.INVISIBLE);
        if (downloadedFile != null) {
            if (downloadedFile.length > 1) {
                String string = new String(downloadedFile, StandardCharsets.UTF_8);
                String[] separated = string.split("_");
                String versionName = separated[0];
                newest_software_version_title_textview_update.setText(versionName); //  the version name acquired from the downloaded file
            }
        }
        cancelAsyncTaskProcess = false;
        DeviceVersionStatus();
        statusFlagTrigger = false;
        Log.e(TAG, newest_software_version_title_textview_update.getText().toString());
        Log.e(TAG, current_sw_version_textview_update.getText().toString());
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (!newest_software_version_title_textview_update.getText().toString().equals("No information") && !current_sw_version_textview_update.getText().toString().equals("No information")) {
                    update_version_button.setBackground(getResources().getDrawable(R.drawable.rounded_corners));
                } else {

                    update_version_button.setBackground(getResources().getDrawable(R.drawable.rounded_corners_disabled));
                }
            }
        }, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        upload_status_box.setVisibility(View.INVISIBLE);
        cancelAsyncTaskProcess = true;
        statusFlagTrigger = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAsyncTaskProcess = true;
        statusFlagTrigger = false;
    }

    /**
     * OTA ASYNCTASKS
     **/

    class UploadNewVersionAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            showLoaderWithText(true, "Uploading newest version");
            HttpResponse response;
            HttpClient httpClient = new DefaultHttpClient();
            URL urlUpload = null;
            try {
                urlUpload = new URL(url[0] + versionNumber + ".tar");
                HttpPut request = new HttpPut(String.valueOf(urlUpload));
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                ByteArrayEntity params = new ByteArrayEntity(downloadedFile);
                nameValuePairs.add(new BasicNameValuePair("filename", versionNumber + ".tar"));
                params.setContentType("application/x-tar");
                request.setHeader("Content-type", "  application/x-tar");
                request.addHeader("Accept", "*/*");
                request.addHeader("Connection", "Keep-Alive");
                request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
                request.addHeader("Accept-Language", "he-IL,he;q=0.8,en-US;q=0.6,en;q=0.4");
                request.setEntity((new UrlEncodedFormEntity(nameValuePairs)));
                request.setEntity(params);
                response = httpClient.execute(request);
                if (response != null) {
                    if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 204) {
                        BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                        String output;
                        System.out.println("Output from Server ...." + response.getStatusLine().getStatusCode() + "\n");
                        while ((output = br.readLine()) != null) {
                            System.out.println(output);
                        }
                    } else {
                        Log.e(TAG, String.valueOf(response.getStatusLine().getStatusCode()));
                        throw new RuntimeException("Failed : HTTP error code : "
                                + response.getStatusLine().getStatusCode());
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "ex Code UploadError: " + ex);
                Log.e(TAG, "url:" + urlUpload);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            showLoaderWithText(false, null);
            Log.e(TAG, tab_ota_progressbar_textview.getText().toString());
            if (tab_ota_progressbar_textview.getText().toString().contains("100")) {

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //fired after 12 seconds
                        DeviceVersionStatus();
                    }
                }, 8000);
                //  statusFlagTrigger=false;
            } else {
                tab_ota_progressbar_textview.setText("");
                software_update_progressBar.setVisibility(View.INVISIBLE);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //fired after 12 seconds
                        DeviceVersionStatus();
                    }
                }, 8000);
                //  statusFlagTrigger=false;
            }
        }
    }


//    class FileDownloadAsyncTask extends AsyncTask<Device_Type_Enum, Void, byte[]> {
//
//
//        @Override
//        protected byte[] doInBackground(Device_Type_Enum... deviceTypeEnum) {
//
//            try {
//
//
//                if (deviceTypeEnum[0] != null) {
//                    switch (deviceTypeEnum[0]) {
//                        case Device_Type_Enum.F_Device:
//
//                            break;
//                    }
//                }
//
//
//
//
//
//
//                showLoaderWithText(true, "Scanning for latest version");
//                DefaultHttpClient client = new DefaultHttpClient();
//                HttpGet request = new HttpGet(uri[0]);
//                HttpResponse response = null;
//                try {
//                    response = client.execute(request);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                HttpEntity entity = response != null ? response.getEntity() : null;
//                int fileLength = (int) (entity != null ? entity.getContentLength() : 0);
//                InputStream is = null;
//                try {
//                    is = entity != null ? entity.getContent() : null;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                byte[] file = new byte[fileLength];
//
//                int bytesRead = 0;
//                while (bytesRead < fileLength) {
//                    int n = 0;
//                    try {
//                        n = is != null ? is.read(file, bytesRead, fileLength - bytesRead) : 0;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    if (n <= 0)
//                        ; // do some error handling
//                    bytesRead += n;
//                }
//                byte[] file1 = new byte[0];
//                if (file.length > 0) {
//                    StringBuilder text = new StringBuilder();
//                    String str = null;
//                    try {
//                        str = new String(file, "UTF-8");
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                    }
//                    showLoaderWithText(true, "Downloading...");
//                    DefaultHttpClient client1 = new DefaultHttpClient();
////            if(str!=null) {
//                    HttpGet request1 = new HttpGet(str);
//                    HttpResponse response1 = null;
//                    try {
//                        response1 = client1.execute(request1);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    HttpEntity entity1 = response1 != null ? response1.getEntity() : null;
//                    int fileLength1 = (int) (entity1 != null ? entity1.getContentLength() : 0);
//                    InputStream is1 = null;
//
//                    try {
//                        is1 = entity1 != null ? entity1.getContent() : null;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    file1 = new byte[fileLength1];
//
//                    int bytesRead1 = 0;
//                    while (bytesRead1 < fileLength1) {
//                        int n = 0;
//                        try {
//                            n = is1 != null ? is1.read(file1, bytesRead1, fileLength1 - bytesRead1) : 0;
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        if (n <= 0)
//                            ; // do some error handling
//                        bytesRead1 += n;
//                    }
//                }
//                Log.e(TAG, "" + file1);
//
//
//                return file1;
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            return  new byte[0];
//
//
//        }
//
//        @TargetApi(Build.VERSION_CODES.KITKAT)
//        @Override
//        protected void onPostExecute(byte[] file) {
//        }
//
//
////        @Override
//        protected byte[] doInBackground(String... uri) {
//            try {
//                showLoaderWithText(true, "Scanning for latest version");
//                DefaultHttpClient client = new DefaultHttpClient();
//                HttpGet request = new HttpGet(uri[0]);
//                HttpResponse response = null;
//                try {
//                    response = client.execute(request);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                HttpEntity entity = response != null ? response.getEntity() : null;
//                int fileLength = (int) (entity != null ? entity.getContentLength() : 0);
//                InputStream is = null;
//                try {
//                    is = entity != null ? entity.getContent() : null;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                byte[] file = new byte[fileLength];
//
//                int bytesRead = 0;
//                while (bytesRead < fileLength) {
//                    int n = 0;
//                    try {
//                        n = is != null ? is.read(file, bytesRead, fileLength - bytesRead) : 0;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    if (n <= 0)
//                        ; // do some error handling
//                    bytesRead += n;
//                }
//                byte[] file1 = new byte[0];
//                if (file.length > 0) {
//                    StringBuilder text = new StringBuilder();
//                    String str = null;
//                    try {
//                        str = new String(file, "UTF-8");
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                    }
//                    showLoaderWithText(true, "Downloading...");
//                    DefaultHttpClient client1 = new DefaultHttpClient();
////            if(str!=null) {
//                    HttpGet request1 = new HttpGet(str);
//                    HttpResponse response1 = null;
//                    try {
//                        response1 = client1.execute(request1);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    HttpEntity entity1 = response1 != null ? response1.getEntity() : null;
//                    int fileLength1 = (int) (entity1 != null ? entity1.getContentLength() : 0);
//                    InputStream is1 = null;
//
//                    try {
//                        is1 = entity1 != null ? entity1.getContent() : null;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    file1 = new byte[fileLength1];
//
//                    int bytesRead1 = 0;
//                    while (bytesRead1 < fileLength1) {
//                        int n = 0;
//                        try {
//                            n = is1 != null ? is1.read(file1, bytesRead1, fileLength1 - bytesRead1) : 0;
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        if (n <= 0)
//                            ; // do some error handling
//                        bytesRead1 += n;
//                    }
//                }
//                Log.e(TAG, "" + file1);
//
//
//                return file1;
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            return  new byte[0];
//
//        }
//
//        @TargetApi(Build.VERSION_CODES.KITKAT)
//        @Override
//        protected void onPostExecute(byte[] file) {
//            downloadedFile = file;
//            showLoaderWithText(false, null);
//            if (downloadedFile == null || downloadedFile.length < 1) {
//                Toast.makeText(getActivity(), "Download failed\nCheck your internet connection and try again", Toast.LENGTH_LONG).show();
//            } else {
//                String string = new String(file, StandardCharsets.UTF_8);
//                String[] separated = string.split("_");
//                String versionName = separated[0];
//                newest_software_version_title_textview_update.setText(versionName); // newest version name
//                if(!newest_software_version_title_textview_update.getText().toString().equals("No information") && !current_sw_version_textview_update.getText().toString().equals("No information")){
//                    update_version_button.setBackground(getResources().getDrawable(R.drawable.rounded_corners));
//                }
//                else{
//                    update_version_button.setBackground(getResources().getDrawable(R.drawable.rounded_corners_disabled));
//                }
//                Toast.makeText(getActivity(), "Download completed", Toast.LENGTH_SHORT).show();
//            }
//            super.onPostExecute(file);
//        }
//    }



    class FileDownloadAsyncTask extends AsyncTask<String, Void, byte[]> {
        @Override
        protected byte[] doInBackground(String... uri) {
            try {
                showLoaderWithText(true, "Scanning for latest version");
                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(uri[0]);
                HttpResponse response = null;
                try {
                    response = client.execute(request);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                HttpEntity entity = response != null ? response.getEntity() : null;
                int fileLength = (int) (entity != null ? entity.getContentLength() : 0);
                InputStream is = null;
                try {
                    is = entity != null ? entity.getContent() : null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] file = new byte[fileLength];

                int bytesRead = 0;
                while (bytesRead < fileLength) {
                    int n = 0;
                    try {
                        n = is != null ? is.read(file, bytesRead, fileLength - bytesRead) : 0;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (n <= 0)
                        ; // do some error handling
                    bytesRead += n;
                }
                byte[] file1 = new byte[0];
                if (file.length > 0) {
                    StringBuilder text = new StringBuilder();
                    String str = null;
                    try {
                        str = new String(file, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    showLoaderWithText(true, "Downloading...");
                    DefaultHttpClient client1 = new DefaultHttpClient();
//            if(str!=null) {
                    HttpGet request1 = new HttpGet(str);
                    HttpResponse response1 = null;
                    try {
                        response1 = client1.execute(request1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    HttpEntity entity1 = response1 != null ? response1.getEntity() : null;
                    int fileLength1 = (int) (entity1 != null ? entity1.getContentLength() : 0);
                    InputStream is1 = null;

                    try {
                        is1 = entity1 != null ? entity1.getContent() : null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    file1 = new byte[fileLength1];

                    int bytesRead1 = 0;
                    while (bytesRead1 < fileLength1) {
                        int n = 0;
                        try {
                            n = is1 != null ? is1.read(file1, bytesRead1, fileLength1 - bytesRead1) : 0;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (n <= 0)
                            ; // do some error handling
                        bytesRead1 += n;
                    }
                }
                Log.e(TAG, "" + file1);


                return file1;

            } catch (Exception e) {
                e.printStackTrace();
            }

            return  new byte[0];

        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected void onPostExecute(byte[] file) {
            downloadedFile = file;
            showLoaderWithText(false, null);
            if (downloadedFile == null || downloadedFile.length < 1) {
                Toast.makeText(getActivity(), "Download failed\nCheck your internet connection and try again", Toast.LENGTH_LONG).show();
            } else {
                String string = new String(file, StandardCharsets.UTF_8);
                String[] separated = string.split("_");
                String versionName = separated[0];
                if (versionName.contains("404 Not Found")) {
                    versionName = "No Information";
                }
                newest_software_version_title_textview_update.setText(versionName); // newest version name
                if(!newest_software_version_title_textview_update.getText().toString().equals("No information") && !current_sw_version_textview_update.getText().toString().equals("No information")){
                    update_version_button.setBackground(getResources().getDrawable(R.drawable.rounded_corners));
                }
                else{
                    update_version_button.setBackground(getResources().getDrawable(R.drawable.rounded_corners_disabled));
                }
                Toast.makeText(getActivity(), "Download completed", Toast.LENGTH_SHORT).show();
            }

            super.onPostExecute(file);
        }
    }

    //GET device version
    class DeviceVersionAsyncTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = null;
            String responseString = null;
            try {
                response = httpclient.execute(new HttpGet(deviceVersionUrl));
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK && response.getStatusLine().toString() != null) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    responseString = out.toString();
                    publishProgress(responseString);
                    out.close();
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate();
            final String[] split;
            split = progress[0].split("&");
            for (final String tempParameterString : split) {
                final String[] arrTempParameter = tempParameterString.split("=");
                if (arrTempParameter.length >= 2) {
                    final String parameterKey = arrTempParameter[0];
                    final String parameterValue = arrTempParameter[1];

                    System.out.println("Key" + parameterKey);
                    System.out.println("Value" + parameterValue);
                    if (parameterKey.contains("version")) {
                        currentVersion = parameterValue;
                    }
                }
            }
            current_sw_version_textview_update.setText(currentVersion);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    //Upload progress status AsyncTask
    class UploadStatusAsyncTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... uri) {
            HttpParams httpParameters = new BasicHttpParams();
            int timeoutConnection = 1000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            int timeoutSocket = 1000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            //endless while loop until we flip the flag
            while (statusFlagTrigger == true) {
                try {
                    Log.d(TAG, "OtaProgress AsyncTask is running!!");
                    response = httpclient.execute(new HttpGet(uri[0]));
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK || statusLine.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        responseString = out.toString();
                        publishProgress(responseString);
                        out.close();
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //Closes the connection.
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }

                } catch (ClientProtocolException e) {
                    e.printStackTrace();

                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
            return responseString;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected void onProgressUpdate(String... progress) {

            super.onProgressUpdate();
            if (progress[0].equals("fail") || progress[0].equals("null")) {
                showLoaderWithText(true, "Something went wrong... \nTrying again in few seconds");
                software_update_progressBar.setVisibility(View.INVISIBLE);
                update_software_append_textview.setText("OTA update failed...");
                tab_ota_progressbar_textview.setText("");
            } else {
                if (!(Objects.equals(progress[0], "fail")) && !(Objects.equals(progress[0], null))) {
                    software_update_progressBar.setProgress(Integer.parseInt(progress[0]));
                    tab_ota_progressbar_textview.setText(progress[0] + "%");
                    if ((progress[0].equals("100"))) {
                        software_update_progressBar.setProgress(Integer.parseInt(progress[0]));
                        tab_ota_progressbar_textview.setText(progress[0] + "%");
                        update_software_append_textview.setText("Uploading new SW version.\nWriting to serial flash.\nExtracting package.\nRebooting.\nTesting new SW version\nDone.\n");
//                        SuccessDialog();
                        ((MainActivity) getActivity()).showSuccessDialog("Latest version \nsuccessfully updated","Close",null, Popup.PopupType.Success,null,null);
                        Toast.makeText(getActivity(), "Upload finished!.", Toast.LENGTH_SHORT).show();
                        tab_ota_progressbar_textview.setText("");
                        software_update_progressBar.setVisibility(View.INVISIBLE);
                        statusFlagTrigger = false;
                    }
                }
                if (!progress[0].equals("null") || !progress[0].equals("fail")) {
                    scrollToBottom();
                    if ((Integer.parseInt(progress[0])) < 25) {
                        update_software_append_textview.setText("Uploading new SW version.\nExtracting Package.\nWriting to serial flash.");
                        showLoaderWithText(true, "Uploading...");
                        software_update_progressBar.setVisibility(View.VISIBLE);
                    }
                    if ((Integer.parseInt(progress[0])) > 90 && (Integer.parseInt(progress[0])) < 100) {
                        update_software_append_textview.setText("Uploading new SW version.\nWriting to serial flash.\nExtracting package.\nRebooting.\nTesting new SW version.");

                    }
                }
            }
        }

        private void scrollToBottom()
        {
            scroller.post(new Runnable()
            {
                public void run()
                {
                    scroller.smoothScrollTo(0, update_software_append_textview.getBottom());
                }
            });
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    class CustomFileDownload extends AsyncTask<String, Void, byte[]> {
        @Override
        protected byte[] doInBackground(String... uri) {
            showLoaderWithText(true, "Downloading...");
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(uri[0]);
            HttpResponse response = null;
            byte[] file;
            try {
                response = client.execute(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
            HttpEntity entity = response != null ? response.getEntity() : null;
            int fileLength = (int) (entity != null ? entity.getContentLength() : 0);
            InputStream is = null;
            try {
                is = entity != null ? entity.getContent() : null;
            } catch (IOException e) {
                e.printStackTrace();
            }

            file = new byte[fileLength];
            int bytesRead = 0;
            while (bytesRead < fileLength) {
                int n = 0;
                try {
                    n = is != null ? is.read(file, bytesRead, fileLength - bytesRead) : 0;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (n <= 0)
                    ; // do some error handling
                bytesRead += n;
            }

            return file;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected void onPostExecute(byte[] file) {
            downloadedFile = file;
            showLoaderWithText(false, null);
            if (downloadedFile == null || downloadedFile.length < 1) {
                Toast.makeText(getActivity(), "Download failed\nCheck your internet connection and try again", Toast.LENGTH_SHORT).show();
            } else {
                String string = new String(file, StandardCharsets.UTF_8);
                String[] separated = string.split("_");
                String versionName = separated[0];
                if (versionName.contains("404 Not Found")) {
                    versionName = "No Information";
                }
                newest_software_version_title_textview_update.setText(versionName); // newest version name
                Toast.makeText(getActivity(), "Custom download completed", Toast.LENGTH_SHORT).show();
                statusFlagTrigger = true;
                if (!newest_software_version_title_textview_update.getText().toString().equals("No information") && !current_sw_version_textview_update.getText().toString().equals("No information")) {
                    update_version_button.setBackground(getResources().getDrawable(R.drawable.rounded_corners));
                } else {

                    update_version_button.setBackground(getResources().getDrawable(R.drawable.rounded_corners_disabled));
                }
            }
            super.onPostExecute(file);
        }
    }
    /********************************/
    /*************METHODS************/
    /*******************************/

    /**
     * triggering the deviceVersion asyncTask
     */
    void DeviceVersionStatus() {
        deviceVersionAsyncTask = new DeviceVersionAsyncTask();
        deviceVersionAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, deviceVersionUrl);
    }

    /**
     * @param show- enable/disable loader
     * @param msg   -
     */
    @UiThread
    void showLoaderWithText(Boolean show, String msg) {
        if (!show) {

            tab_ota_configuration_laoder_layout.setVisibility(View.GONE);
            tab_ota_configuration_laoder_label.setText("");
        } else {
            tab_ota_configuration_laoder_layout.setVisibility(View.VISIBLE);
            tab_ota_configuration_laoder_label.setText(msg);
        }
    }

    private static boolean IsMatch(String s, String pattern) {
        try {
            Pattern pat = Pattern.compile(pattern);
            Matcher matcher = pat.matcher(s);
            return matcher.matches();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void DeveloperModeDialog() {
        // custom dialog
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.developer_mode_window);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // set the custom dialog components - text, image and button
        ImageView okButton = (ImageView) dialog.findViewById(R.id.developer_mode_window_ok_button);
        final EditText editText = (EditText) dialog.findViewById(R.id.developer_mode_edittext);
        //OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String typedUrl = editText.getText().toString().trim();
                if (!IsMatch(typedUrl, REGEX)) {
                    Toast.makeText(getActivity(), "Not a valid url!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    CustomFileDownload customFileDownload = new CustomFileDownload();
                    customFileDownload.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, typedUrl);
                    Toast.makeText(getActivity(), typedUrl, Toast.LENGTH_SHORT).show();

                }
                dialog.dismiss();
            }
        });

        //CANCEL button
        ImageView cancelButton = (ImageView) dialog.findViewById(R.id.developer_mode_window_cancel__button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    public void SameVersionDialog() {
        // custom dialog
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.same_version_window);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // set the custom dialog components - text, image and button
        ImageView okButton = (ImageView) dialog.findViewById(R.id.same_version_window_ok_button);
        //OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                statusFlagTrigger = true;
                uploadStatusAsyncTask = new UploadStatusAsyncTask();
                uploadStatusAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uploadStatusUrl);
                upload_status_box.setVisibility(View.VISIBLE);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //fired after 200ms
                        versionNumber = newest_software_version_title_textview_update.getText().toString();
                        uploadNewVersionAsyncTask = new UploadNewVersionAsyncTask();
                        uploadNewVersionAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, deviceUploadUrl);
                    }
                }, 200);
                dialog.dismiss();

            }
        });
        //CANCEL button
        ImageView cancelButton = (ImageView) dialog.findViewById(R.id.same_version_window_cancel__button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void SuccessDialog() {
        // custom dialog
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.popup_layout);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        // set the custom dialog components - text, image and button
        ImageButton hideButton = (ImageButton) dialog.findViewById(R.id.popup_rightButton);
        hideButton.setVisibility(View.GONE);
        ImageButton okButton = (ImageButton) dialog.findViewById(R.id.popup_leftButton);
        TextView okButtonText = (TextView) dialog.findViewById(R.id.popup_leftButton_text);
        okButtonText.setText("Close");
        okButtonText.setTypeface(Typeface.DEFAULT_BOLD);
        TextView uploadResult = (TextView) dialog.findViewById(R.id.popup_text);
        uploadResult.setText("Latest version successfully updated");
        uploadResult.setTypeface(Typeface.DEFAULT_BOLD);
        //OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

            }
        });

        dialog.show();
    }
}

