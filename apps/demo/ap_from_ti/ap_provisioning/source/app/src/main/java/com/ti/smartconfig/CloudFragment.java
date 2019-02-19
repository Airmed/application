

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
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ti.smartconfig.utils.NetworkUtil;
import com.ti.smartconfig.utils.SharedPreferencesInterface_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


    @EFragment(R.layout.cloud_view)
public class CloudFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {
    private static final String TAG = "CloudFragment";
    public SharedPreferences sharedpreferences;
    public static final String mypreference = "iot";
    public static final String Name = "deviceIP";
    TriggerModeAsyncHttpPost asyncHttpPost;
    HashMap<String, String> data;
    String deviceIp;
    CloudAsyncTask cloudAsyncTask;
    Boolean cancelAsyncTaskProcess;
    @ViewById
    TextView cloud_connection_textview;
    @ViewById
    EditText freetext_edittext;
    @ViewById
    EditText send_to_edittext;
    @ViewById
    RadioButton push_button_radio_button;
    @ViewById
    RadioButton shake_board_radio_button;
    @ViewById
    Button cloud_configuration_button;
    @Pref
    SharedPreferencesInterface_ prefs;
    @ViewById
    RadioGroup trigger_radio_group;


    /**
     * Called after fragment is initialized
     */
    @AfterViews
    void afterViews() {
        cancelAsyncTaskProcess = false;
        sharedpreferences = getActivity().getSharedPreferences(mypreference,
                Context.MODE_PRIVATE);

        if (sharedpreferences.contains(Name)) {
            deviceIp = (sharedpreferences.getString(Name, ""));
        }
        if (NetworkUtil.getConnectedSSID(getActivity()) == null) {
            Toast.makeText(getActivity(), "Check WiFi connection first", Toast.LENGTH_SHORT).show();
            cloud_connection_textview.setText("No WiFi connection");
        } else {

            cloud_connection_textview.setText("Checking connection");
            String cloudUrl = "http://" + deviceIp + "/cloud?state";
            cloudAsyncTask = new CloudAsyncTask();
            cloudAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, cloudUrl);
        }
        trigger_radio_group.setOnCheckedChangeListener(this);
    }


    @Click
    void push_button_radio_button() {
        if (shake_board_radio_button.isChecked()) {
            shake_board_radio_button.setChecked(false);
            push_button_radio_button.setChecked(true);
        }
    }

    @Click
    void shake_board_radio_button() {
        if (push_button_radio_button.isChecked()) {
            shake_board_radio_button.setChecked(true);
            push_button_radio_button.setChecked(false);
        }
    }

    @Click
    void cloud_configuration_button() {
        //  String email=send_to_edittext.getText().toString();
        Boolean emailCheck = isValidEmail(send_to_edittext.getText().toString());
        if (!emailCheck) {
            Toast.makeText(getActivity(), "Invalid Email.Please check the email address and try again", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Valid Email !", Toast.LENGTH_SHORT).show();
            data = new HashMap<String, String>();
            data.put("to", send_to_edittext.getText().toString());
            data.put("freetext", freetext_edittext.getText().toString());
            TriggerModeAsyncHttpPost asyncHttpPost = new TriggerModeAsyncHttpPost(data);
            String triggerUrl = "http://" + deviceIp + "/email";
            asyncHttpPost.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, triggerUrl);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        cancelAsyncTaskProcess = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelAsyncTaskProcess = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAsyncTaskProcess = true;

    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.shake_board_radio_button:
                data.put("triggermode", "shake");

                break;
            case R.id.push_button_radio_button:
                data.put("triggermode", "button");
                break;

        }
    }

    /**
     * ASYNCTASKS
     */

    //Cloud AsyncTask
    class CloudAsyncTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            //this loop going to work forever until the user will move to other tab/close the application
            while (!cancelAsyncTaskProcess) {
                try {
                    Log.d(TAG, "Cloud AsyncTask is running!!");
                    response = httpclient.execute(new HttpGet(uri[0]));
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK || statusLine.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        responseString = out.toString();
                        publishProgress(responseString);
                        out.close();
                        try {
                            Thread.sleep(4000);
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
            //we will never get here.will keep iterating in while loop or closing the AsyncTask
            return responseString;
        }


        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate();
            //updating results every 1 sec
            final String[] split;
            split = progress[0].split("&");
            for (final String tempParameterString : split) {
                final String[] arrTempParameter = tempParameterString.split("=");
                if (arrTempParameter.length >= 2) {
                    final String parameterKey = arrTempParameter[0];
                    final String parameterValue = arrTempParameter[1];
                    //do something with the parameters
                    System.out.println("Key" + parameterKey);
                    System.out.println("Value" + parameterValue);
                    if (parameterKey.contains("connected")) {
                        cloud_connection_textview.setText("Connected");
                        cloud_connection_textview.setTextColor(Color.GREEN);

                    } else {
                        cloud_connection_textview.setText("Disconnected");
                        cloud_connection_textview.setTextColor(Color.RED);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    //Info - send AsyncTask
    public class TriggerModeAsyncHttpPost extends AsyncTask<String, String, String> {
        private HashMap<String, String> mData = null;// post data

        /**
         * constructor
         */
        public TriggerModeAsyncHttpPost(HashMap<String, String> data) {
            mData = data;
        }

        /**
         * background
         */
        @Override
        protected String doInBackground(String... params) {
            byte[] result = null;
            String str = "";
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(params[0]);// in this case, params[0] is URL
            try {
                // set up post data
                ArrayList<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();
                Iterator<String> it = mData.keySet().iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    nameValuePair.add(new BasicNameValuePair(key, mData.get(key)));
                }

                post.setEntity(new UrlEncodedFormEntity(nameValuePair, "UTF-8"));
                HttpResponse response = client.execute(post);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpURLConnection.HTTP_OK) {
                    result = EntityUtils.toByteArray(response.getEntity());
                    str = new String(result, "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return str;
        }


        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getActivity(), "the result is:" + result, Toast.LENGTH_SHORT).show();
        }
    }

    public final static boolean isValidEmail(CharSequence target) {
        if (target == null)
            return false;

        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

}

