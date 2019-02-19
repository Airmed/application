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

package com.ti.smartconfig.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ti.smartconfig.DeviceConfiguration_;
import com.ti.smartconfig.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by admin on 7/20/15.
 */
public class Ping  {

    private final String TAG = "Ping";
    private PingCallback mPingCallback;
    private static final int BUF = 512;
    private final int REACH_TIMEOUT = 5000;
    //    private final String CMD = "/system/bin/ping -A -q -n -w 3 -W 2 -c 3 -b ";
    private final String CMD = "/system/bin/ping -n -b ";
    //    private final String PTN = "^rtt min\\/avg\\/max\\/mdev = [0-9\\.]+\\/[0-9\\.]+\\/([0-9\\.]+)\\/[0-9\\.]+ ms.*";
    private final String PTN = "\\b[0-9]+.[0-9]+.[0-9]+.[0-9]\\b";

    private final String IP_ADDRESS_PATTERN = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

    private Process proc = null;

    private Pattern mPattern;
    private String line;
    public String indicator = null;
    public int rate = 800; // Slow start
    public boolean working = false;
    public String ipToPing = "";
    private String mGatewayIp = "";
    private Handler handler_ = new Handler(Looper.getMainLooper());


    public Ping(PingCallback callBack, String gatewayIp) {
        mPingCallback = callBack;
        mGatewayIp = gatewayIp;
        mPattern = Pattern.compile(IP_ADDRESS_PATTERN);
    }

    protected void before() {
        working = true;
    }

    protected void after() {
        working = false;
        mPingCallback.pingCompleted();
    }

    public Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {

            if (Thread.interrupted()) {
                return;
            }

            Log.d("pingRunnable","started");
            //onPre
            before();

            if (Thread.interrupted()) {
                return;
            }


            //doInBackground
            working = true;
            Log.i(TAG, "Ping Runnable started");
            ping(ipToPing);

            if (Thread.interrupted()) {
                return;
            }


            //onPost
            after();
            Log.d("pingRunnable", "finished");
        }
    };




    public void adaptRate() {
        int response_time = 0;
        if ((response_time = getAvgResponseTime(indicator)) > 0) {
            if (response_time > 100) { // Most distanced hosts
                rate = response_time * 5; // Minimum 500ms
            } else {
                rate = response_time * 10; // Maximum 1000ms
            }
            if (rate > REACH_TIMEOUT) {
                rate = REACH_TIMEOUT;
            }
        }
    }


    private String getDeviceName(String ip) {


        String url = "http://"+ip + "/param_device_name.txt";
        String name = "";
        String response = "";

        HttpParams httpParameters = new BasicHttpParams();
        // Set the timeout in milliseconds until a connection is established.
        // The default value is zero, that means the timeout is not used.
        int timeoutConnection = 3000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        int timeoutSocket = 5000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

        HttpClient client = new DefaultHttpClient(httpParameters);

        //DefaultHttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);

        try {
            HttpResponse execute = client.execute(httpGet);
           if (execute.getStatusLine().getStatusCode() == 404 || execute.getStatusLine().getStatusCode() == 503) {
                name = "";
            } else {
                InputStream content = execute.getEntity().getContent();

                BufferedReader buffer = new BufferedReader(
                        new InputStreamReader(content));
                String s;
                while ((s = buffer.readLine()) != null) {
                    response += s;
                }
                Log.i(TAG, "name from file:" + response);
                name = response;
            }
        }
        catch (HttpHostConnectException e) {
            Log.e(TAG, "Connection exception: " + e.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "exception: " + e.toString());
            e.printStackTrace();
        }
        return name;
    }


    public void ping(String host){
        BufferedReader reader = null;
        Matcher matcher;
        try {
            Log.i(TAG, "ping " + host);
            if (proc == null) {
                proc = Runtime.getRuntime().exec(CMD + host);
                //proc.waitFor();
                //int exit = proc.exitValue();
                //Log.i(TAG, "exit value = " + exit);
                reader = new BufferedReader(new InputStreamReader(proc.getInputStream()), BUF);
                while ((line = reader.readLine()) != null) {
                    if (!working){
                        if (proc !=null){
                            proc.destroy();
                        }
                        return;
                    }
                    //Log.i(TAG,"Answer from ping"+line);
                    matcher = mPattern.matcher(line);
                    if (matcher.find()) {
                        if (mGatewayIp.equalsIgnoreCase(matcher.group())) {
                            Log.i(TAG, "gateway answer - skip");
                        } else {
                            String name = "";

                            try {
                                name = getDeviceName(matcher.group());
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                            if (!name.equalsIgnoreCase("")) {
                                //                        mPingCallback.pingDeviceFetched("host:" + matcher.group() + ", name:" + name);
                                JSONObject deviceJSON = new JSONObject();
                                try {
                                    deviceJSON.put("name", name);
                                    deviceJSON.put("host", matcher.group());
                                    deviceJSON.put("age", 0);
                                    Log.i(TAG, "Publishing device found to application,  name: " + name);
                                    mPingCallback.pingDeviceFetched(deviceJSON);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
//                    reader.close();
//                    Log.i(TAG, "found device at:" + matcher.group());
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Can't use native ping");
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }


    public int getAvgResponseTime(String host) {
        BufferedReader reader = null;
        Matcher matcher;
        try {
            Log.i(TAG, "getAvgResponseTime");
            final Process proc = Runtime.getRuntime().exec(CMD + host);
            proc.waitFor();
            int exit = proc.exitValue();
            Log.i(TAG, "exit value = " + exit);
            reader = new BufferedReader(new InputStreamReader(proc.getInputStream()), BUF);
            while ((line = reader.readLine()) != null) {
                Log.i(TAG,"Answer from ping"+line);
                matcher = mPattern.matcher(line);
                if (matcher.matches()) {
                    reader.close();
                    Log.i(TAG, "found device at:" + matcher.group(1));
                    return (int) Float.parseFloat(matcher.group(1));
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Can't use native ping");
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
        return rate;
    }


    public void stopPing() {
        Log.i(TAG, "stopPing");

//        BackgroundExecutor.cancelAll("ping_in_background",true);
//        working = false;



        if (proc!=null) {
            proc.destroy();
        }
        working = false;
    }

    public interface PingCallback {
        void pingCompleted();
        void pingDeviceFetched(JSONObject deviceJSON);
        void pingFailed(String errorMessage);
    }



}






































