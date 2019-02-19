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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.log4j.Logger;

import com.ti.smartconfig.MainActivity;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

public class WifiNetworkUtils {

	private static final String TAG = "WifiNetworkUtils";
	private static final WifiNetworkUtils instance = new WifiNetworkUtils();
	
	private static final long CONNECTION_TIMEOUT = 40000;
	private static final long LONG_CONNECTION_TIMEOUT = 60000;
	
	private WifiConfiguration mConfigurationToConnectAfterDisconnecting = null;
	private Context mContext = null;
	private BitbiteNetworkUtilsCallback mBitbiteNetworkUtilsCallback;
	private Handler mWifiHandler = new Handler();
	private Boolean mConnectAfterDisconnected = false;
	private WifiManager wifiManager;
	private BitbiteNetworkUtilsCallback mTempBitbiteNetworkUtilsCallback;
	private Logger mLogger;
	private ConnectivityManager mConnectivityManager;
	private Boolean isInitial3GEnabled;


	public static WifiNetworkUtils getInstance(Context context) {
		if (instance.mContext == null) {
			instance.mContext = context;
			instance.wifiManager = (WifiManager) instance.mContext.getSystemService(Context.WIFI_SERVICE);
			instance.mLogger = Logger.getLogger(WifiNetworkUtils.class);
			instance.mConnectivityManager = (ConnectivityManager)instance.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			instance.isInitial3GEnabled = isLollipopAndUp() ? instance.isMobileDataEnabledLollipop() : instance.isMobileDataEnabled();
		}
		
		return instance; 
	}

	public void onResume() {
		setMobileDataEnabled(false);
		mContext.registerReceiver(instance.mWifiConnectionReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
		mContext.registerReceiver(instance.mSupplicantStateReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
	}
	
	public void onPaused() {
		setMobileDataEnabled(isInitial3GEnabled);
		try {
			mContext.unregisterReceiver(mWifiConnectionReceiver);
			mContext.unregisterReceiver(mSupplicantStateReceiver);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}


	public String getConnectedSSID() {

		String networkName = null;
		if(wifiManager !=null) {
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			if (wifiInfo != null) {
				networkName = wifiInfo.getSSID().replaceAll("\"", "");
			}

			if (networkName == null || networkName.equals("<unknown ssid>") || networkName.equals("0x") || networkName.equals("")) {
				networkName = null;
			}
		}
		return networkName;
	}
	
	public List<WifiConfiguration> getSavedNetworks(Context context) {
		return wifiManager.getConfiguredNetworks();
	}
	
	public void connectToWifi(WifiConfiguration configuration, Context context, BitbiteNetworkUtilsCallback callback, Boolean withTimer) {

		try {
			mBitbiteNetworkUtilsCallback = callback;
			mConfigurationToConnectAfterDisconnecting = configuration;

			if (configuration == null) {
				mLogger.error("WifiConfiguration was null.. can't connect to null silly....");
				mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Unknown);
				return;
			}

			if (isLollipopAndUp()) {
				mLogger.info("Connecting (Lollipop and up) to " + configuration.SSID + " With Timer: " + withTimer);
				mConnectAfterDisconnected = true;
//				Log.i(TAG, "Disconnect: " + wifiManager.disconnect());
//				mLogger.info("Disconnect: " + wifiManager.disconnect());
			}
//			else {
//				wifiManager.disconnect();
//				Log.i(TAG, "Disconnect: " + wifiManager.disconnect());
				mLogger.info("Disconnect: " + wifiManager.disconnect());
				boolean connectFromList = false;
			//check for existing configuration
				List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
				for (WifiConfiguration i : list) {
					if (!connectFromList) {
					if (i.SSID != null && i.SSID.equals(configuration.SSID)) {
						mLogger.info("found config in list - connect from list");
						mLogger.info("Connecting to \"" + configuration.SSID + "\" from list");
						wifiManager.enableNetwork(i.networkId, true);
						wifiManager.reconnect();
						connectFromList = true;
					}
					}
				}

				if (!(connectFromList)) {
					mLogger.info("Did not find config in list - add");
					mLogger.info("Connecting to \"" + configuration.SSID + "\" With Timer: " + withTimer);
					wifiManager.addNetwork(configuration);
					wifiManager.saveConfiguration();
					List<WifiConfiguration> configsList = wifiManager.getConfiguredNetworks();
					boolean found = false;
					for (WifiConfiguration i : configsList) {
						if (!found) {
							if (i.SSID != null && i.SSID.equals(configuration.SSID)) {
								mLogger.info("found new config in list - connect from list");
								mLogger.info("Connecting to \"" + configuration.SSID + "\" from list");
								wifiManager.enableNetwork(i.networkId, true);
								wifiManager.reconnect();
								found = true;
							}
						}
					}
//					Log.i(TAG, "Enabling network: " + wifiManager.enableNetwork(configuration.networkId, true));
				}
//			}
			
			if (withTimer) {
				mLogger.info("Starting wifi connection short timer (" + CONNECTION_TIMEOUT + "ms)");
				Log.w(TAG, "Starting wifi connection short timer (" + CONNECTION_TIMEOUT + "ms)");
				mWifiHandler.postDelayed(mWifiConnectionTimeout, CONNECTION_TIMEOUT);
			}
			else {
				mLogger.info("Starting wifi connection long timer (" + LONG_CONNECTION_TIMEOUT + "ms)");
				Log.w(TAG, "Starting wifi connection long timer (" + LONG_CONNECTION_TIMEOUT + "ms)");
				mWifiHandler.postDelayed(mWifiConnectionTimeout, LONG_CONNECTION_TIMEOUT);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			mLogger.error("Exception with input: " + configuration + " " + context + " " + callback + " " + withTimer);
			if (mBitbiteNetworkUtilsCallback != null)
				mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Unknown);
		}
	}

	public static Boolean isLollipopAndUp() {
		int currentApiVersion = android.os.Build.VERSION.SDK_INT;
		return currentApiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP;
	}
	
	public List<ScanResult> getWifiScanResults(Context context) {
		return wifiManager.getScanResults();
	}
	
	public WifiConfiguration getConfigurationForScanResult(ScanResult result, Context context) {
		for (WifiConfiguration configuration : getSavedNetworks(context)) {
			if (configuration.SSID.replaceAll("\"", "").equals(result.SSID))
				return configuration;
		}
		
		return null;
	}
	
	private BroadcastReceiver mWifiConnectionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			NetworkInfo info = (NetworkInfo) intent.getExtras().get("networkInfo");
			State state = info != null ? info.getState() : null;
			String network = info != null ? info.getExtraInfo() : null;
			String networkType = info != null ? info.getTypeName() : null;

			if(networkType != null && networkType.contains("mobile"))
			{
				return;
			}

			if (mBitbiteNetworkUtilsCallback == null)
				return;

			mLogger.info("State: " + state + " Network:\"" + network +"\"");

			if (state != null) {

				switch (state) {
					case CONNECTED:

						if (network == null) {
							network = getConnectedSSID();
						}

						if (network.contains("sphone")) {
							return;
						}

						NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
						//	&& networkInfo.getType() == ConnectivityManager.TYPE_WIFI
						if (network.equals(mConfigurationToConnectAfterDisconnecting.SSID) && !mConfigurationToConnectAfterDisconnecting.SSID.equals("")) {
							mLogger.info("Connected to desired network: \"" + network + "\"");
							successfullyConnectToWifi(network, mBitbiteNetworkUtilsCallback);
							disconnectCallback();
						} else if (mConfigurationToConnectAfterDisconnecting.SSID.replaceAll("\"", "").equals(network)) {
							mLogger.info("Connected to desired network: \"" + network + "\"");
							successfullyConnectToWifi(network, mBitbiteNetworkUtilsCallback);
							disconnectCallback();
						}
						break;
					case CONNECTING:
						break;
					case DISCONNECTED:

						if (mConnectAfterDisconnected) {
							mConnectAfterDisconnected = false;
							Log.i(TAG, "Connecting after disconnecting: " + wifiManager.enableNetwork(mConfigurationToConnectAfterDisconnecting.networkId, true));
						}

						break;
					case DISCONNECTING:
						break;
					case SUSPENDED:
						break;
					case UNKNOWN:
						break;
					default:
						break;
				}
			}
		}
	};

	private void disconnectCallback() {
		mWifiHandler.removeCallbacks(mWifiConnectionTimeout);
		mConfigurationToConnectAfterDisconnecting = null;
		mBitbiteNetworkUtilsCallback = null;
	}
	
	private Runnable mWifiConnectionTimeout = new Runnable() {
		@Override
		public void run() {
			mLogger.error("******************************************");
			mLogger.error("**** Connection to wifi was timed out ****");
			mLogger.error("******************************************");
			
			String connectedNetwork = getConnectedSSID();
			mLogger.info("Connected to \"" + connectedNetwork + "\" now, was supposed to connect to: \"" + mConfigurationToConnectAfterDisconnecting.SSID +"\"");
			
			mTempBitbiteNetworkUtilsCallback = mBitbiteNetworkUtilsCallback;
			mBitbiteNetworkUtilsCallback = null;

			if ((mConfigurationToConnectAfterDisconnecting.SSID).equals(connectedNetwork)) {
				successfullyConnectToWifi(mConfigurationToConnectAfterDisconnecting.SSID, mTempBitbiteNetworkUtilsCallback);
			}
			else if ((mConfigurationToConnectAfterDisconnecting.SSID).replaceAll("\"", "").equals(connectedNetwork)) {
				successfullyConnectToWifi(mConfigurationToConnectAfterDisconnecting.SSID, mTempBitbiteNetworkUtilsCallback);
			}
			else
				((MainActivity)mContext).showTimeoutPopup(mConfigurationToConnectAfterDisconnecting, wifiManager);
		}
	};
	
	private void successfullyConnectToWifi(String ssid, BitbiteNetworkUtilsCallback callback) {
		
		if (callback == null)
			return;
		
		try {
			if (isActiveNetworkWifi()) {
				callback.successfullyConnectedToNetwork(ssid);
			}
			else {
				callback.failedToConnectToNetwork(WifiConnectionFailure.Connected_To_3G);
			}
		}
		catch (NullPointerException e){
			e.printStackTrace();
			mLogger.error("Failed to activate the callback");
		}
	}
	
	private BroadcastReceiver mSupplicantStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			if (mBitbiteNetworkUtilsCallback == null) {
				return;
			}
			
			if (intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR)) {
				int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);
				
				mLogger.error("Supplicant State Receiver, error: " + error);
				
				Log.e(TAG, "Supplicant error (" + error + ")");
				if (error == WifiManager.ERROR_AUTHENTICATING)
					mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Wrong_Password);
				else
					mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Unknown);
				
				disconnectCallback();
			}
			else if (intent.hasExtra(WifiManager.EXTRA_NEW_STATE)) {
				//Log.e(TAG, "Supplicant State Receiver with extra new state: " + intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
			}
			else {
				mLogger.info("**** Got Supplicant state with unknown extra ****");
				for (String key : intent.getExtras().keySet()) {
				    Object value = intent.getExtras().get(key);
				    mLogger.info(String.format("%s %s (%s)", key, value != null ? value.toString() : null, value != null ? value.getClass().getName() : null));
				}
				mLogger.info("************************************************");
			}
		}
	};
	
	public enum WifiConnectionFailure {
		Connected_To_3G,
		Wrong_Password,
		Timeout,
		Unknown
	}
	
	public void clearCallback() {
		Log.e(TAG, "Callback was cleared");
		mBitbiteNetworkUtilsCallback = null;
	}

	public void timeoutDialogFinished() {
	if(mConfigurationToConnectAfterDisconnecting.SSID.equals("")) {
		if (mTempBitbiteNetworkUtilsCallback == null || mConfigurationToConnectAfterDisconnecting == null)
			mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Timeout);
		}
		String connectedNetwork = getConnectedSSID();
		mLogger.info("Timeout dialog was finished, Callback:" + mTempBitbiteNetworkUtilsCallback + " Needed WiFi: \"" + mConfigurationToConnectAfterDisconnecting.SSID + "\" Connected to: \"" + connectedNetwork + "\"");
		
		mBitbiteNetworkUtilsCallback = mTempBitbiteNetworkUtilsCallback;
		mTempBitbiteNetworkUtilsCallback = null;
		
		if (mBitbiteNetworkUtilsCallback == null || mConfigurationToConnectAfterDisconnecting == null)
			return;
		
		if (mConfigurationToConnectAfterDisconnecting.SSID.replaceAll("\"", "").equals(connectedNetwork)) {
			successfullyConnectToWifi(mConfigurationToConnectAfterDisconnecting.SSID, mBitbiteNetworkUtilsCallback);
		}
		else
			mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Timeout);
	}
	
	public interface BitbiteNetworkUtilsCallback {
		void successfullyConnectedToNetwork(String ssid);
		void failedToConnectToNetwork(WifiConnectionFailure failure);
	}
	
	/*
	@SuppressWarnings("deprecation")
	private void checkActiveNetwork() {
		
		ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if (!NetworkUtil.isActiveNetworkWifi(mContext)) {
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(mContext, "We are not in WIFI. Please send log", Toast.LENGTH_LONG).show();;
				}
			});
		
			if (isLollipopAndUp()) {
				mLogger.info("Changing default network");
				lollipopChangeDefaultNetwork(cm, mContext);
			}
			else {
				final android.net.NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			    final android.net.NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				mLogger.info("WIFI: " + wifi.getExtraInfo());
				mLogger.info("MOBILE: " + mobile.getExtraInfo());
				
				mLogger.info("Changing active network..");
				cm.setNetworkPreference(ConnectivityManager.TYPE_WIFI);
				
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				mLogger.info("Checking again for active network..");
			}
			
			NetworkUtil.isActiveNetworkWifi(mContext);
		}
		else {
			final android.net.NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		    final android.net.NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			mLogger.info("WIFI: " + wifi.getExtraInfo());
			mLogger.info("MOBILE: " + mobile.getExtraInfo());
		}
	}
	*/
	
	private Boolean isActiveNetworkWifi() {


//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//
//        if (wifiInfo != null) {
//            mLogger.info("We are in WIFI");
//            return true;
//        } else {
//            mLogger.info("We are not in WIFI");
//            return false;
//        }


		NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();

		if (networkInfo == null)
			return true;

		if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_WIMAX) {
			mLogger.error("We are in WIFI");
			return true;
		}
		else {
			mLogger.error("We are not in WIFI");
			return false;
		}


	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void lollipopChangeDefaultNetwork(final ConnectivityManager cm, Context context) {
		Network[] array = cm.getAllNetworks();
		for (Network network : array) {
			NetworkInfo info = cm.getNetworkInfo(network);
			mLogger.info("Network: \"" + network + "\"\nInfo: " + info);
			
			if (info.getType() == ConnectivityManager.TYPE_WIFI) {
				mLogger.info("Setting the network: \"" + network + "\" as default (" + ConnectivityManager.setProcessDefaultNetwork(network) + ")");
			}
		}
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void setMobileDataEnabled(boolean enabled) {
		//mLogger.info("Setting 3G data to " + enabled);
		try {
			final Class conmanClass = Class.forName(mConnectivityManager.getClass().getName());
		    final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
		    iConnectivityManagerField.setAccessible(true);
		    final Object iConnectivityManager = iConnectivityManagerField.get(mConnectivityManager);
		    final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
			final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
		    setMobileDataEnabledMethod.setAccessible(true);
		    setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
		    ///mLogger.info("3G " + enabled + " success");
		} catch (Exception e) {
			e.printStackTrace();
			///mLogger.error("3G " + enabled + " FAILURE");
		}
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public Boolean isMobileDataEnabledLollipop(){
		TelephonyManager telephonyService = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        
        switch (telephonyService.getDataState()) {
		case TelephonyManager.DATA_DISCONNECTED:
			Log.i(TAG, "DATA_DISCONNECTED");
			break;
		case TelephonyManager.DATA_CONNECTING:
			Log.i(TAG, "DATA_CONNECTING");
			break;
		case TelephonyManager.DATA_SUSPENDED:
			Log.i(TAG, "DATA_SUSPENDED");
			break;
		case TelephonyManager.DATA_CONNECTED:
			Log.i(TAG, "DATA_CONNECTED");
			return true;
		}

		return false;
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Boolean isMobileDataEnabled(){ 
	    try {
	    	final Class conmanClass = Class.forName(mConnectivityManager.getClass().getName());
		    final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
		    iConnectivityManagerField.setAccessible(true);
		    final Object iConnectivityManager = iConnectivityManagerField.get(mConnectivityManager);
		    final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
		    
		    final Method getMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("getMobileDataEnabled");
		    getMobileDataEnabledMethod.setAccessible(true);
		    Boolean flag = (Boolean) getMobileDataEnabledMethod.invoke(iConnectivityManager);
		    Log.i(TAG, "3G data was initialised " + flag);
		    return flag;
	    } catch (Exception e) {
			e.printStackTrace();
	    	Log.e(TAG, "Failed to know if 3G was enabled");
	    }
	    
	    return false;
	}
}
