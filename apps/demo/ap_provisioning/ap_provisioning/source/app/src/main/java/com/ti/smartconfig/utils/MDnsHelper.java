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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import javax.jmdns.impl.JmDNSImpl;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;

@EBean
public class MDnsHelper implements ServiceListener, ServiceTypeListener {


	
	public static final String SERVICE_TYPE = "_http._tcp.local.";
	public static final String SMARTCONFIG_IDENTIFIER = "srcvers=1D90645";
	private static final String TAG = "MDnsHelper";

	Activity activity;
	MDnsCallbackInterface callback;
//	private JmDNS jmdns;
	private JmDNSImpl jmdns;
    private MulticastLock multicastLock;
    WifiManager wm;
    InetAddress bindingAddress;
    boolean isDiscovering;
    
    public void init(Activity activity, MDnsCallbackInterface callback) {
    	this.activity = activity;
    	this.callback = callback;
    	isDiscovering = false;
    	wm = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
    	multicastLock = wm.createMulticastLock(getClass().getName());
    	multicastLock.setReferenceCounted(true);
    }


	@Background
	public void startDiscovery() {
		synchronized(this) {
			if (isDiscovering)
				return;
			final InetAddress deviceIpAddress = getDeviceIpAddress(wm);
			if (!multicastLock.isHeld()) {
				multicastLock.acquire();
			} else {
				Log.i(TAG, "Muticast lock already held...");
			}
			try {
				if (jmdns == null) {
					jmdns = new JmDNSImpl(deviceIpAddress, "SmartConfig");
					//jmdns.registerServiceType(SERVICE_TYPE);
					jmdns.addServiceTypeListener(this);
				}

				Log.w(TAG, "Starting MDNS discovery");
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			} finally {
				if (jmdns != null) {
					isDiscovering = true;
					Log.i(TAG, "discovering services");
				}
			}
		}
	}





	@Background
	public void stopDiscovery() {
		synchronized(this) {

			//try
			//{
			if (!isDiscovering || jmdns == null)
				return;
			if (multicastLock.isHeld()) {
				multicastLock.release();
			} else {
				Log.i(TAG, "Multicast lock already released");
			}
			jmdns.closeState();
			jmdns.cancelState();
			jmdns.unregisterAllServices();
			jmdns.cancelTimer();
			jmdns.cancelState();
			jmdns.cancelStateTimer();
			jmdns.purgeStateTimer();
			jmdns.purgeTimer();
			jmdns.recover();
			jmdns.close();
			jmdns = null;
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
			}
			isDiscovering = false;
			Log.w(TAG, "MDNS discovery stopped");
			//} catch (IOException e) {
			// e.printStackTrace();
			//}
		}
	}





	@Background
	public void restartDiscovery() {
		synchronized(this) {

			Log.w(TAG, "restartDiscovery");
			try {
				if (jmdns != null) {
					jmdns.closeState();
					jmdns.cancelState();
					jmdns.unregisterAllServices();
					jmdns.cancelTimer();
					jmdns.cancelState();
					jmdns.cancelStateTimer();
					jmdns.purgeStateTimer();
					jmdns.purgeTimer();
					jmdns.recover();
					jmdns.close();
					Thread.sleep(6000);
					jmdns = null;
					final InetAddress deviceIpAddress = getDeviceIpAddress(wm);
					//jmdns = JmDNS.create(deviceIpAddress, "SmartConfig");
					jmdns = new JmDNSImpl(deviceIpAddress, "SmartConfig");
					jmdns.addServiceTypeListener(this);
					Log.w(TAG, "reStarting MDNS discovery");
				}

			} catch (Exception e) {
				Log.e(TAG, e.getMessage());

			}
		}
	}



	@Override
	public void serviceAdded(ServiceEvent service) {
		Log.i(TAG, "serviceAdded: " + service + " Nice String:" + service.getInfo().getNiceTextString());

	}

	@Override
	public void serviceRemoved(ServiceEvent service) {
		Log.i(TAG, "serviceRemoved: " + service);
	}

	@Override
	public void serviceResolved(ServiceEvent service) {
		Log.i(TAG, "serviceResolved " + service);

		if (service.getInfo().getNiceTextString().contains(SMARTCONFIG_IDENTIFIER)){
			JSONObject deviceJSON = new JSONObject();
			try {
				deviceJSON.put("name", service.getName());
				deviceJSON.put("host", service.getInfo().getHostAddresses()[0]);
				deviceJSON.put("age", 0);
				Log.i(TAG, "Publishing device found to application,  name: " + service.getName());

				callback.onDeviceResolved(deviceJSON);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private InetAddress getDeviceIpAddress(WifiManager wifi) {
		InetAddress result = null;
		try {
			// default to Android localhost
			result = InetAddress.getByName("10.0.0.2");

			// figure out our wifi address, otherwise bail
			WifiInfo wifiinfo = wifi.getConnectionInfo();
			int intaddr = wifiinfo.getIpAddress();
			byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
			result = InetAddress.getByAddress(byteaddr);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			Log.e(TAG, "getDeviceIpAddress Error: " + e.getMessage());
		}

		return result;
	}

	@Override
	public void serviceTypeAdded(ServiceEvent event) {
		Log.i(TAG, "serviceTypeAdded: " + event);
		if (event.getType().contains(SERVICE_TYPE)) {
			jmdns.addServiceListener(event.getType(), this);
		}
	}

	@Override
	public void subTypeForServiceTypeAdded(ServiceEvent event) {
		Log.i(TAG, "subTypeForServiceTypeAdded: " + event);
	}

}