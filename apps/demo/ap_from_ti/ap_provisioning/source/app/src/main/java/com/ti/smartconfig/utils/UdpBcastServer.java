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

import android.util.Log;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UdpBcastServer {

    private final String TAG = "UdpBcastServer";
    public boolean working = false;
    DatagramSocket serverSocket1;
    int portNumber1 = 1501;
    byte[] receiveData = new byte[1024];
    private Ping.PingCallback mPingCallback; // use the same callback as ping
    private Logger mLogger = Logger.getLogger(UdpBcastServer.class);

    public UdpBcastServer(Ping.PingCallback callBack) {
        mPingCallback = callBack;
    }

    protected void before() {
        try {
            if( serverSocket1 == null ) {
                serverSocket1 = new DatagramSocket(portNumber1);
            }
        } catch (SocketException e) {
            e.printStackTrace();
            Log.i(TAG, "socket error" + e.getMessage());
        }

        working = true;
    }

    protected void after() {
        working = false;
        serverSocket1.disconnect();
        serverSocket1.close();
    }

    public Runnable udpBcastServerRunnable = new Runnable() {
        @Override
        public void run() {

            if (Thread.interrupted()) {
                return;
            }

            //onPre
            before();

            if (Thread.interrupted()) {
                return;
            }

            //do in background
            working = true;
            Log.i(TAG, "udpBcastRunnable started");
            UdpReceive();

            if (Thread.interrupted()) {
                return;
            }

            //onPost
            after();
        }
    };


    public void UdpReceive() {
        int packetLength = 0;
        while(working) {

            if(serverSocket1 != null) {
                DatagramPacket recPacket = new DatagramPacket(receiveData, receiveData.length);
                try {
                    serverSocket1.receive(recPacket);
                }
                catch(IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "IO error");
                    working = false;
                }
                String dataAsString = new String(recPacket.getData(),0,recPacket.getLength());
                if ( dataAsString.length() == 0 ) {
                    continue;
                }


                String deviceName = dataAsString;
                String deviceAddress = dataAsString;
                if(deviceName != null && !deviceName.equalsIgnoreCase("")) {
                    deviceName = deviceName.split(",")[1];
                }
                if(deviceAddress!=null && !deviceAddress.equalsIgnoreCase("") ) {
                    deviceAddress = deviceAddress.split(",")[0];
                }
                Log.i(TAG, "Received name: " + deviceName + " Received address: " + deviceAddress );
                InetAddress IPAddress = recPacket.getAddress();
                Log.i(TAG, "Received from " + IPAddress.toString().split("/")[1]);

                if (!deviceName.equalsIgnoreCase("") && !deviceAddress.equalsIgnoreCase("") ) {
                    JSONObject deviceJSON = new JSONObject();
                    try {
                        deviceJSON.put("name", deviceName);
                        deviceJSON.put("host", deviceAddress);
                        deviceJSON.put("age", 0);
                        Log.i(TAG, "Bcast publishing device found to application,  name: " + deviceName);
                        mLogger.info("Bcast SL dev found to app, name: " + deviceName);
                        mPingCallback.pingDeviceFetched(deviceJSON);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }

        }
    }


}































