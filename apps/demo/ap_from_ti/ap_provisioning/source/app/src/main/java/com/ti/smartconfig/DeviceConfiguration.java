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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import com.ti.smartconfig.utils.DevicePickPopUpView_;
import com.ti.smartconfig.utils.Device_Type_Enum;
import com.ti.smartconfig.utils.ScanResultsPopUpView_;
import com.ti.smartconfig.utils.SharedPreferencesInterface_;
import com.ti.smartconfig.utils.AddProfileAsyncTask;
import com.ti.smartconfig.utils.WifiNetworkUtils;
import com.ti.smartconfig.utils.CFG_Result_Enum;
import com.ti.smartconfig.utils.Constants;
import com.ti.smartconfig.utils.Device;
import com.ti.smartconfig.utils.DevicePickPopUpView;
import com.ti.smartconfig.utils.DeviceVersion;

import com.ti.smartconfig.utils.NetworkUtil;

import com.ti.smartconfig.utils.ScanResultsPopUpView;
import com.ti.smartconfig.utils.SecurityType;
import com.ti.smartconfig.utils.SmartConfigConstants;

import com.ti.smartconfig.utils.AddProfileAsyncTask.AddProfileAsyncTaskCallback;
import com.ti.smartconfig.utils.WifiNetworkUtils.BitbiteNetworkUtilsCallback;
import com.ti.smartconfig.utils.WifiNetworkUtils.WifiConnectionFailure;
import com.ti.smartconfig.utils.Popup.PopupType;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.TextView;

@EFragment(R.layout.tab_device_configuration)
public class DeviceConfiguration extends Fragment {

	protected static final String TAG = "DeviceConfiguration";
	private ProgressDialog progressDialog;
	public Boolean mIsReady = false;
	private Boolean scanningnWifi = false;
	private Boolean scanningnMDNS = false;
	private Boolean scanFinishTimer = false;
	//private Boolean toastScanCheck = false;
	private Boolean deviceFound = true;
	public Vibrator v;

	public List<ScanResult> wifiList;
	public String startingSSID;
	private String chosenWifiSSID;

	public String ssidToAdd;
	public String ssidToAddSecurityKey;
	public String ssidToAddPriority;
	public SecurityType ssidToAddSecurityType;
	private Device mDevice;

	private Handler mHandler = new Handler();
	private DeviceVersion deviceVersion;
	private String mDeviceName;

	private DevicePickPopUpView devicePickPopUpView;
	private ScanResultsPopUpView scanResultsPopUpView;
	private AlertDialog alertDialog;
	public String savedSSID ;
	SharedPreferences sharedpreferences;
	private boolean inInitiateScan;
	public static final String mypreference = "iot";
	boolean isSearching = false;
    public static final String Name = "deviceIP";

    public MainActivity mainActivity;

    public TextWatcher nameFieldTextWatcher;
    public TextWatcher passFieldTextWatcher;





	@Pref
	SharedPreferencesInterface_ prefs;

	@ViewById
	public
	ImageView tab_device_configuration_start_button;
	@ViewById
	EditText tab_device_configuration_device_name_editText;
	@ViewById
	RelativeLayout tab_device_configuration_device_name_layout;
	@ViewById
	RelativeLayout textViewConnectionTextView;
	@ViewById
	RelativeLayout tab_device_configuration_device_to_configure_device_pick_layout;
	@ViewById
	TextView tab_device_configuration_device_to_configure_device_pick_label;
	@ViewById
	ImageView tab_device_configuration_device_to_configure_device_pick_image;
	@ViewById
	RelativeLayout tab_device_configuration_loader_layout;
	@ViewById
	TextView tab_device_configuration_loader_label;

	@ViewById
	RelativeLayout tab_device_configuration_router_layout;
	@ViewById
	public
	TextView tab_device_configuration_router_device_pick_label;
	@ViewById
	RelativeLayout tab_device_configuration_router_device_pick_layout;
	@ViewById
	ImageView tab_device_configuration_router_device_pick_image;

	@ViewById
	TextView tab_device_configuration_device_connection;

	@ViewById
	RelativeLayout tab_device_configuration_iot_uuid_layout;
	@ViewById
	EditText tab_device_configuration_iot_uuid_name_editText;
	@ViewById
	CheckBox tab_device_configuration_password_checkbox;
	@ViewById
	public
	RelativeLayout tab_device_configuration_password_check_layout;
	@ViewById
	EditText tab_device_configuration_password_check_editText;

	private Logger mLogger;
	private int cgfTryNumber = 0;

	private Runnable mScanningWifiTimeoutRunnable = new Runnable() {
		/**
		 * Removes visible loader, displays toast informing the user that the WiFi scan had timed out.
		 */
		@Override
		public void run() {
			if (scanningnWifi) {
				mLogger.error("*AP* *** Wifi scan timed out! ***");
				scanningnWifi = false;
				showLoaderWithText(false, null);
				showToastWithMessage("Wifi scan was timed out");
			}
		}
	};

	/**
	 * Upon receiving a broadcast (sticky - receives on registration) informing that a
	 * change in network connectivity has occurred, determines WiFi connectivity status,
	 * if connected to WiFi calls a method which sets the UI "device connection" bar's
	 * appearance accordingly.
	 *
	 * @see DeviceConfiguration#DisplayWifiState() DisplayWifiState()
	 */
	BroadcastReceiver myWifiReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent arg1) {

			ConnectivityManager cm = (ConnectivityManager) arg0.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = cm.getActiveNetworkInfo();

			if (!(networkInfo == null)) {

				if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
					DisplayWifiState();
				}
			}
			if(networkInfo == null){
				tab_device_configuration_device_connection.setText(getResources().getString(R.string.new_smart_config_fragment_no_wifi_connection));
				tab_device_configuration_device_connection.setTextColor(Color.WHITE);
				textViewConnectionTextView.setBackgroundColor(getResources().getColor(R.color.color_red));
			}
		}
	};

	/**
	 * Receives a broadcast when an access point scan had been completed, and results are available from the supplicant,
	 * unregisters the receiver, counts the amount of SL devices discovered in the scan,
	 * if there is information from a QR scan, searches for a matching device in the access point scan and connects to it if exists,
	 * if no QR information exists then if only one SL device was discovered and auto-connect is enabled, connects to that device,
	 * otherwise dismisses the loader from the UI without action.
	 *
	 * @see com.ti.smartconfig.utils.NetworkUtil#getWifiScanResults(Boolean, Context)
	 */
	BroadcastReceiver receiverWifi = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			inInitiateScan = false;
			try {
				getActivity().unregisterReceiver(receiverWifi);
			} catch (Exception e) {
				e.printStackTrace();
			}


			if (scanningnWifi) {
				mHandler.removeCallbacks(mScanningWifiTimeoutRunnable);
				scanningnWifi = false;

				int devicesNumber = 0;
				ScanResult result = null;

				wifiList = NetworkUtil.getWifiScanResults(true, getActivity());
				mLogger.info("*AP* getWifiScanResults from wifiManager to find SL device as AP - Provisioning tab opened in AP mode");
				for (ScanResult scanResult : wifiList) {
					if (scanResult.SSID.contains(Constants.DEVICE_PREFIX)) {
						devicesNumber++;
						result = scanResult;
					}
				}

				//If there is only one device, and auto connect to SL is on, then we will auto connect to SL, only if it is not password protected.
					if (devicesNumber == 1 && prefs.autoConnectToSL().get()) {

						SecurityType securityType = NetworkUtil.getScanResultSecurity(result);
						if (securityType == SecurityType.OPEN) {
							deviceWasChosen(result, SecurityType.OPEN, null);
						}
						else {
							showToastWithMessage("The device is password protected, auto connect is not possible");
							showLoaderWithText(false, "");
						}
					}
					else {

					    	showLoaderWithText(false, "");

							if( devicesNumber > 1 ) {

								showToastWithMessage("Too many simplelink devices around you , cannot auto connect to the simplelink device");
							}
							if( devicesNumber < 1 ){

								showToastWithMessage("There is no simplelink devices around you..");
							}
						//add connect to starting ssid / if we have one
						//ofir : need to test 24.1
						if(startingSSID != null){


								WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(getActivity(), startingSSID, SecurityType.OPEN, null);
								WifiNetworkUtils.getInstance(getActivity()).connectToWifi(configuration, getActivity(), new BitbiteNetworkUtilsCallback() {

									@Override
									public void successfullyConnectedToNetwork(String ssid) {
										//no need to add toast
									//	showToastWithMessage("Connected to starting ssid");
									}

									@Override
									public void failedToConnectToNetwork(WifiConnectionFailure failure) {
										showToastWithMessage("Failed to connect to initial network " + startingSSID);
									}
								}, true);

							} else {
								mLogger.info("Initial network is null - will not attempt to connect");
								Log.i(TAG,"Initial network is null - will not attempt to connect");

								showToastWithMessage("No initial network to connect to");
							}

					}
				}
			}
//		}
	};

	/**
	 * Receives a broadcast when an mDNS scan had been completed,
	 * if the requested SL device had been found, moves to confirmation stage via SL device as station,
	 * if the SL device had not been found moves to confirmation stage via SL device as access point.
	 *
	 * @see DeviceConfiguration#checkParams() checkParams()
	 * @see DeviceConfiguration#confirmResult(Boolean) confirmResult(Boolean)
	 */
	public BroadcastReceiver scanFinishedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "DeviceConfiguration scanFinishedReceiver, scanningnMDNS:" + scanningnMDNS);
//			if (scanningnMDNS == false)
//				return;
//			isSearching = false;

			if (!deviceFound) {
				//check the list
				if (!scanFinishTimer) {
					scanFinishTimer = true;
					//print("No devices were found in the network: " + ssidToAdd + " Waiting 10 before moving forward to manual");
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (!deviceFound) {
								//print("Waiting is done, continue to manual check");
								scanningnMDNS = false;
								Log.i(TAG, "DeviceConfiguration - scanFinishedReceiver, scanningnMDNS:" + scanningnMDNS);
								mLogger.info("*AP* DeviceConfiguration - scanFinishedReceiver");
								mLogger.info("*AP* mDNS finished - requested SL device not found in local network as STA. Cfg verification retrieval will be executed by connecting to SL device as AP");
								checkParams();
							} else {
//								isSearching = false;
								if (progressDialog != null && progressDialog.isShowing()) {
									progressDialog.dismiss();
									Log.i(TAG, "scanFinishedReceiver, deviceFound:true");
									mLogger.info("*AP* scanFinishedReceiver,SL deviceFound:true");
									print("Found the requested device");
									confirmResult(true);
								}
							}
						}
					}, 25000);
				}
			}

		}
	};

	/**
	 * Receives a broadcast when an SL device had been detected in the local network,
	 * checks whether the discovered device is the requested SL device or not by comparing their names,
	 * if the discovered device is the requested one moves to confirmation stage.
	 *
	 * @see DeviceConfiguration#confirmResult(Boolean) confirmResult(Boolean)
	 */
	public BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "DeviceConfiguration receive device found, scanningnMDNS:" + scanningnMDNS);
			if (!scanningnMDNS)
				return;

//			isSearching = false;

			JSONObject deviceJSON = null;
			try {
				String jsonString = intent.getExtras().getString("newDevice");
				deviceJSON = new JSONObject(jsonString);
				Log.w(TAG, "Detected a new device (jsonString: " + jsonString + ")");
				mLogger.info("*AP* Detected a new SL device (jsonString: " + jsonString + ")");
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "Failed to create JSON of device");
				mLogger.info("*AP* Failed to create JSON of SL device");
			}
			Boolean nameIsValid = false;
			String name = null;
			String host = null;
			try {
				name = deviceJSON != null ? deviceJSON.getString("name") : null;
				host = deviceJSON != null ? deviceJSON.getString("host") : null;
				Log.w(TAG, "checking if " + name + " equals " + mDeviceName);
				mLogger.info("*AP* Checking if the SL device found is the correct SL device - does \"" + name + "\" equal \"" + mDeviceName + "\"");
				if (name != null && name.equals(mDeviceName)) {
					nameIsValid = true;
					Log.w(TAG, "name is valid");
				}
				else {
					nameIsValid = false;
					Log.w(TAG, "name is not valid");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (nameIsValid) {
				Log.w(TAG, "set SL device as mDevice");
				mDevice =  new Device(name, host);
				if ((progressDialog != null && progressDialog.isShowing()) || isSearching) {
					isSearching = false;
					if (progressDialog != null && progressDialog.isShowing())
					progressDialog.dismiss();


					Log.i(TAG, "deviceFoundReceiver device found");
					print("Found the requested device");
					((MainActivity) getActivity()).stopPing();
					deviceFound = true;
					scanningnMDNS = false;
					confirmResult(true);
					Log.i(TAG, "Found the device, stopping the MDNS discovery");
					Log.w(TAG, "deviceFoundReceiver stop scanning mdns");
					mLogger.info("Found the device, stopping M discovery");
					mLogger.info("*AP* Name is valid - found the correct SL device, stopping M discovery + Ping");
				}
			} else {
				mLogger.info("*AP* Name is not valid - the SL device found is not the correct SL device");
			}
		}
	};









	/**
	 * Checks the number of attempts made at retrieving configuration confirmation from the SL device,
	 * and either continues to another attempt, moves to verification retrieval from SL device as access point,
	 * or informs user of verification failure and finishes the process, depending on the number of attempts made.
	 *
	 * @param viaWifi Boolean. true if the configuration verification is to be retrieved from the SL device as station
	 *                         false if the configuration verification is to be retrieved from the SL device as access point.
	 *
	 * @see DeviceConfiguration#checkParams() checkParams()
	 * @see DeviceConfiguration#finish(Boolean) finish(Boolean)
	 * @see com.ti.smartconfig.DeviceConfiguration.GetCFGResult GetCFGResult
	 */
	private void getCFG(final Boolean viaWifi) {
		if (viaWifi) {
		    mLogger.info("*AP* Number of attempts previously made to retrieve cfg verification from SL device as STA: " + cgfTryNumber);
		} else{
			mLogger.info("*AP* Number of attempts previously made to retrieve cfg verification from SL device as AP: " + cgfTryNumber);
		}
		if (cgfTryNumber == Constants.ATTEMPTS_TO_GET_CGF_RESULTS) {
			deviceFound = false;
			if (viaWifi) {
				Log.i(TAG, "getCFG - viaWifi = true - going to checkParams");
				mLogger.info("*AP* Max attempts at cfg verification via SL device as STA reached, and passed parameter is true - starting attempts at verification via SL device as AP");
				checkParams();
			} else {
				mLogger.info("reached max attempts as AP, Fail");
				showLoaderWithText(false, "");
				((MainActivity) getActivity()).showSuccessDialog(Constants.DEVICE_LIST_CFG_CONFIRMATION_FAILED, getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
				finish(false);
			}

			return;
		}
		int utilInt = cgfTryNumber;
		mLogger.info("*AP* Executing cfg verification attempt no.: " + ++utilInt);

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						new GetCFGResult().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, viaWifi);
					} else {
						new GetCFGResult().execute(viaWifi);
					}


	}


	/**
	 * Starts the procedure of retrieving configuration verification from the SL device,
	 * and logs the state of that procedure.
	 *
	 * @param viaWifi Boolean. true if the configuration verification is to be retrieved from the SL device as station
	 *                         false if the configuration verification is to be retrieved from the SL device as access point.
	 */
	private void confirmResult(final Boolean viaWifi) {
	if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
			}
		if (!viaWifi) {
			if (cgfTryNumber == Constants.ATTEMPTS_TO_GET_CGF_RESULTS) {
				mLogger.info("*AP* Cfg verification via SL device as STA reached max attempts, starting attempts at verification via SL device as AP");
			} else {
			mLogger.info("*AP* SL device not found in local network/OR SSID added to SL device in profile to connect to not found in mobile device (wifiManager) scan - starting attempts at verification via SL device as AP");
			}
		} else {
		mLogger.info("*AP* Cfg verification via SL device as STA process begins");
		}
		isSearching = false;
		mLogger.info("confirmResult called");
		final Handler handler = new Handler();
		cgfTryNumber = 0;

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				getCFG(viaWifi);
			}
		}, Constants.DELAY_BEFORE_ASKING_CFG_RESULTS);
	}

	/**
	 * Receives results associated with the WIFI_FIRST_STEP_CONNECTION_FAILURE request code after failing to
	 * connect to the SL device for the first step of the provisioning process,
	 * if connected to the chosen SL device moves to retrieval of the device version,
	 * else resets the UI "Device to configure" view.
	 *
	 * @param resultCode int. The result code returned from the child activity, unused.
	 * @param returnIntent Intent. Containing the result data, unused.
	 *
	 * @see com.ti.smartconfig.DeviceConfiguration.GetDeviceVersion GetDeviceVersion
	 * @see DeviceConfiguration#resetDeviceToConfigureView() resetDeviceToConfigureView()
	 */
	@OnActivityResult(Constants.WIFI_FIRST_STEP_CONNECTION_FAILURE)
	void onFirstAttemptFailureResult(int resultCode, Intent returnIntent) {
		String connectedNetwork = NetworkUtil.getConnectedSSID(getActivity());

			if (connectedNetwork.equals(chosenWifiSSID)) {

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					new GetDeviceVersion().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "Sion");

				} else {
					new GetDeviceVersion().execute("Sion"); //Trying to get the simple link version, if fails, this is not valid simple link device so show warning
				}
			} else {
				showLoaderWithText(false, "");
				tab_device_configuration_router_layout.setVisibility(View.GONE);
				resetDeviceToConfigureView();
			}
	}

	/**
	 * Resets the UI "Device to configure" view.
	 */
	@UiThread
	void resetDeviceToConfigureView() {
		try {
			tab_device_configuration_device_to_configure_device_pick_label.setTextColor(getActivity().getResources().getColor(R.color.color_line));
			tab_device_configuration_device_to_configure_device_pick_label.setText(getString(R.string.tab_device_configuration_ap_search_device));
			tab_device_configuration_device_to_configure_device_pick_image.setImageResource(R.drawable.new_graphics_white_box_pick_red);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets the String passed as parameter as the text appearing in the UI "Name your device" field.
	 *
	 * @param newName    String. New value to appear in the field.
	 */
	@UiThread
	void changeDeviceName(String newName) {
		tab_device_configuration_device_name_editText.setText(newName);
		if (tab_device_configuration_iot_uuid_name_editText != null) {
			tab_device_configuration_iot_uuid_name_editText.setText("");
		}

	}

	/**
	 * Called after views binding has happened, sets the UI elements to their initial state and sets listeners to them,
	 * sets the startingSSID to the SSID the mobile device is currently connected to if it is null,
	 * or urges the user to connect to a Wi-Fi network if no connection exists,
	 * and initiates an access point scan by the mobile device.
	 *
	 * @see DeviceConfiguration#DisplayWifiState() DisplayWifiState()
	 * @see DeviceConfiguration#initiateScan() initiateScan()
	 */
	@AfterViews
	void afterViews() {

	//	((MainActivity) getActivity()).EnableOutOfTheBoxTabs(false);
		tab_device_configuration_password_check_layout.setVisibility(View.GONE);
		tab_device_configuration_password_check_editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		//if (tab_device_configuration_password_check_layout.getVisibility() == View.VISIBLE) {
			tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
			tab_device_configuration_start_button.setEnabled(false);


        //*********************************************************


       passFieldTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tab_device_configuration_password_check_editText.length() > 0) {

                    if (tab_device_configuration_device_name_layout.getVisibility() == View.VISIBLE) {
                        if (tab_device_configuration_password_check_editText.length() > 0) {
                            tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_on);
                            tab_device_configuration_start_button.setEnabled(true);
                            ssidToAddSecurityKey = tab_device_configuration_password_check_editText.getText().toString();
                            mIsReady = true;
                            setToReady(mIsReady);
                        } else {
                            tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
                            tab_device_configuration_start_button.setEnabled(false);
                            mIsReady = false;
                            setToReady(mIsReady);
                        }
                    } else {
                        tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_on);
                        tab_device_configuration_start_button.setEnabled(true);
                        ssidToAddSecurityKey = tab_device_configuration_password_check_editText.getText().toString();
                        mIsReady = true;
                        setToReady(mIsReady);
                    }



                } else {
                    tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
                    tab_device_configuration_start_button.setEnabled(false);
                    mIsReady = false;
                    setToReady(mIsReady);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };


       nameFieldTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (tab_device_configuration_device_name_editText.length() > 0) {

                    if (ssidToAddSecurityType != ssidToAddSecurityType.OPEN) {

                        tab_device_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);

                    } else {
                        tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_on);
                        tab_device_configuration_start_button.setEnabled(true);
                        ssidToAddSecurityKey = tab_device_configuration_password_check_editText.getText().toString();
                        mIsReady = true;
                        setToReady(mIsReady);
                    }

                    if (tab_device_configuration_password_check_layout.getVisibility() == View.VISIBLE) {

                        if (tab_device_configuration_password_check_editText.length() > 0) {

                            tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_on);
                            tab_device_configuration_start_button.setEnabled(true);
                            ssidToAddSecurityKey = tab_device_configuration_password_check_editText.getText().toString();
                            mIsReady = true;
                            setToReady(mIsReady);
                        }
                    }
                } else {
                    tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
                    tab_device_configuration_start_button.setEnabled(false);
                    mIsReady = false;
                    setToReady(mIsReady);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        startConfigButtonState();





























        //************************************************************************************

        /*

			tab_device_configuration_password_check_editText.addTextChangedListener(new TextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					if (tab_device_configuration_password_check_editText.length() > 0) {

						tab_device_configuration_start_button.setEnabled(true);
						ssidToAddSecurityKey = tab_device_configuration_password_check_editText.getText().toString();
						mIsReady = true;

						setToReady(mIsReady);


					} else {
						tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
						tab_device_configuration_start_button.setEnabled(false);
						mIsReady = false;

						setToReady(mIsReady);

					}
				}




				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
											  int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before,
										  int count) {

				}
			});

			*/


		DisplayWifiState();

		// Get instance of Vibrator from current Context
		v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

		// font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/fake.ttf");
		//tab_device_configuration_device_connection.setTypeface(font);

		mLogger = Logger.getLogger(DeviceConfiguration.class);
		setToReady(false);

		tab_device_configuration_router_layout.setVisibility(View.GONE);

//		changeDeviceName("");
		if (prefs.showDeviceName().get()) {
			tab_device_configuration_device_name_layout.setVisibility(View.VISIBLE);
		}
		else {
			tab_device_configuration_device_name_layout.setVisibility(View.GONE);
		}


		if(prefs.isIoTuuid().get()) {
			tab_device_configuration_iot_uuid_layout.setVisibility(View.VISIBLE);
		}
		else {
			tab_device_configuration_iot_uuid_layout.setVisibility(View.GONE);
		}

		//Check to see if the starting network is null
		startingSSID = ((MainActivity)getActivity()).mStartingWifiNetwork;
		String connectedWifi = NetworkUtil.getConnectedSSID(getActivity());
		Log.i(TAG,"StartingSSID: " + startingSSID);
		if (startingSSID ==  null && connectedWifi== null ) {
//
//			String connectedWifi = NetworkUtil.getConnectedSSID(getActivity());
//			final int wifiConnection=	NetworkUtil.getConnectionStatus(getActivity());
//			int sdkVersion= SdkVersionHelper.getSdkInt();
//			if(sdkVersion > 22) {
//				if(connectedWifi == null && wifiConnection == 1 ){
//
//					mLogger.info("*AP* Starting network was null, but now we are connected to " + connectedWifi + "so we will set the starting network to this wifi");
//					MainActivity activity = (MainActivity) getActivity();
//					activity.mStartingWifiNetwork = connectedWifi;
//					initiateScan();
//
//				}
//			}
//			else	if (sdkVersion < 23 && connectedWifi != null) {
//				mLogger.info("*AP* Starting network was null, but now we are connected to " + connectedWifi + "so we will set the starting network to this wifi");
//				MainActivity activity = (MainActivity) getActivity();
//				activity.mStartingWifiNetwork = connectedWifi;
//				initiateScan();
//			}
//			else {
				mLogger.info("*AP* Showing \"no wifi activity\" dialog, because the starting network is null and we are not connected to any network now");
		AlertDialog wifiDialog = new AlertDialog.Builder(getActivity()). //create a dialog
				setTitle("No WiFi Connection").
				setMessage("Please connect your Smart Phone to router").
				setPositiveButton("Connect to WiFi", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) { //the user clicked yes
						WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
						if (!wifiManager.isWifiEnabled())
							wifiManager.setWifiEnabled(true);
						startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), Constants.WIFI_SETTINGS_INTENT_RESULTS);
					}
				}).setNeutralButton("No router", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
				if (!wifiManager.isWifiEnabled())
					wifiManager.setWifiEnabled(true);

				initiateScan();
				Log.e(TAG,"Scan running from Afterviews #3");
			}
		}).create();
		wifiDialog.show();
		//	}
	}
	else {
		initiateScan();
		Log.e(TAG,"Scan running from Afterviews #2");
	}

		tab_device_configuration_loader_layout.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				return true;
			}
		});
		tab_device_configuration_password_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// checkbox status is changed from unchecked to checked.
				if (!isChecked) {
					// show password
					tab_device_configuration_password_check_editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
				} else {
					// hide password
					tab_device_configuration_password_check_editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
				}
			}
		});

		//listen to editText focus and hiding keyboard when focus is out
		tab_device_configuration_device_name_editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					hideKeyboard(v);
				}
			}
		});

		tab_device_configuration_password_check_editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					hideKeyboard(v);
				}
			}

		});
		int devicesNumber = 0;
		ScanResult result = null;

		wifiList = NetworkUtil.getWifiScanResults(true, getActivity());
		mLogger.info("*AP* getWifiScanResults from wifiManager to find SL device as AP - Provisioning tab opened in AP mode");
		for (ScanResult scanResult : wifiList) {
			if (scanResult.SSID.contains(Constants.DEVICE_PREFIX)) {
				devicesNumber++;
				result = scanResult;
			}
		}

//		//If there is only one device, and auto connect to SL is on, then we will auto connect to SL, only if it is not password protected.
//		if (devicesNumber == 1 && prefs.autoConnectToSL().get()) {
//			SecurityType securityType = NetworkUtil.getScanResultSecurity(result);
//			if (securityType == SecurityType.OPEN) {
//				deviceWasChosen(result, SecurityType.OPEN, null);
//			}
//			else {
//				showToastWithMessage("The device is password protected, auto connect is not possible");
//				showLoaderWithText(false, "");
//			}
//		}
//		else {
//			showLoaderWithText(false, "");
//
//			showToastWithMessage("Too many simplelink devices around you , cannot auto connect to the simplelink device");		}
//


	}

    public void startConfigButtonState() {

        if (tab_device_configuration_device_name_layout.getVisibility() == View.VISIBLE) {


            tab_device_configuration_device_name_editText.addTextChangedListener(nameFieldTextWatcher);


            Random random = new Random();
//            String ran = String.valueOf(random.nextInt());
            int ran = (random.nextInt(8)+1)*100 + (random.nextInt(8)+1)*10 + (random.nextInt(8)+1);
            if (ran < 0) {
                ran = (ran * -1);
            }

            tab_device_configuration_device_name_editText.setTextColor(Color.LTGRAY);
            tab_device_configuration_device_name_editText.setText(String.format("Dev-%d",ran));



            tab_device_configuration_device_name_editText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tab_device_configuration_device_name_editText.setText("");
                    tab_device_configuration_device_name_editText.setTextColor(Color.BLACK);
                }
            });

            if (tab_device_configuration_device_name_editText.length() > 0) {

                if (ssidToAddSecurityType != ssidToAddSecurityType.OPEN) {
                    tab_device_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);

                } else {
                    tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_on);
                    tab_device_configuration_start_button.setEnabled(true);
                    ssidToAddSecurityKey = tab_device_configuration_password_check_editText.getText().toString();
                    mIsReady = true;
                    setToReady(mIsReady);
                }

//                tab_device_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);

            } else {
                tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
                tab_device_configuration_start_button.setEnabled(false);
                mIsReady = false;
                setToReady(mIsReady);
            }

        } else {

            if (ssidToAddSecurityType != ssidToAddSecurityType.OPEN) {
                tab_device_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);

            } else {
                tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_on);
                tab_device_configuration_start_button.setEnabled(true);
                ssidToAddSecurityKey = tab_device_configuration_password_check_editText.getText().toString();
                mIsReady = true;
                setToReady(mIsReady);
            }

//            tab_device_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);
        }
    }


	public void hideKeyboard(View view) {
		InputMethodManager inputMethodManager =(InputMethodManager)getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}


	/**
	 * If auto-connect is enabled starts a scan to find available SL devices.
	 *
	 * @see DeviceConfiguration#startWifiScan() startWifiScan()
	 */
	private void initiateScan() {
		if (!prefs.autoConnectToSL().get())
			return;

		inInitiateScan = true;

		showLoaderWithText(true, "Looking for devices near you");
		startWifiScan();
	}

	/**
	 * Receives results associated with the WIFI_SETTINGS_INTENT_RESULTS request code from the Wi-Fi settings activity,
	 * sets the SSID the mobile device is currently connected to as the starting Wi-Fi network,
	 * and initiates a scan for available SL devices.
	 *
	 * @param resultCode int. The result code returned from the child activity, unused.
	 * @param returnIntent Intent.Containing the result data, unused.
	 *
	 * @see DeviceConfiguration#initiateScan() initiateScan()
	 */
	@OnActivityResult(Constants.WIFI_SETTINGS_INTENT_RESULTS)
	void onSettingsResult(int resultCode, Intent returnIntent) {
		String ssid = NetworkUtil.getConnectedSSID(getActivity());
		mLogger.info("*AP* Connected now to: " + "\"" + ssid + "\"" + ", and setting it as new starting SSID network");
		((MainActivity)getActivity()).mStartingWifiNetwork = ssid;
		initiateScan();
		Log.e(TAG,"scanning running inside result #1");
	}

	/**
	 * Sets the state of the UI "Start Configuration" button according to the Boolean passed as parameter.
	 *
	 * @param isReady    Boolean. True to enable "Start Configuration" button.
	 *                            False to disable "Start Configuration" button.
	 */
	@UiThread
	public void setToReady(Boolean isReady) {
		mIsReady = isReady;
		if (mIsReady) {
			tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_on);

		}
		else {
			tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
		}
	}

	/**
	 * Closes the Scan results pop up view, after setting the relevant parameters to the UI fields.
	 */
	@UiThread
	public void closeDialog() {
		tab_device_configuration_router_device_pick_label.setTextColor(Color.BLACK);
		tab_device_configuration_router_device_pick_label.setText(ssidToAdd);
		tab_device_configuration_router_device_pick_image.setImageResource(R.drawable.new_graphics_scrool_choose);
        if (ssidToAddSecurityType != SecurityType.OPEN) {
            tab_device_configuration_password_check_layout.setVisibility(View.VISIBLE);
            tab_device_configuration_password_check_editText.setText(ssidToAddSecurityKey);
        }
		alertDialog.cancel();
		alertDialog = null;
		scanResultsPopUpView = null;
		setToReady(true);
	}

	/**
	 * Tries to connect to SL device, if connection successful continues the provisioning process by
	 * attempting to retrieve the SL device's version, if connection unsuccessful resets the UI "Device to configure" field
	 * and informs the user of the cause of this failure.
	 *
	 * @param ssid          String. SL device SSID.
	 * @param securityType  SecurityType. SL device security type (open, wep, wpa1/2).
	 * @param password      String. Password if SL device is secured.
	 */
	@UiThread
	void connectToSSIDAndGetScanResults(String ssid, SecurityType securityType, String password) {
		showLoaderWithText(true, "Connecting to " + ssid);
		WifiNetworkUtils.getInstance(getActivity()).clearCallback();
		final WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(getActivity(), ssid, securityType, password);
		WifiNetworkUtils.getInstance(getActivity()).connectToWifi(configuration, getActivity(), new BitbiteNetworkUtilsCallback() {
            @Override
            public void successfullyConnectedToNetwork(String ssid) {


                print("Checking if the network is simplelink device");
			//10.1 ofir add new condition to check if we actually have normal RSSI - (TODO)

			WifiManager	wifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
				int info = wifi.getConnectionInfo().getRssi();
			//	textStatus.setText("WiFi Rssi: " + info);
				 mLogger.info("RSSI is : "+info);

				mLogger.info("True RSSI is : "+wifi.calculateSignalLevel(info,5));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    new GetDeviceVersion().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "Sion");

                } else {
                    new GetDeviceVersion().execute("Sion");
                }
                if (ssid.contains(Constants.DEVICE_PREFIX)) {
                    prefs.scanningDisable().put(true);
                    ((MainActivity) getActivity()).stopPing();
                } else {
                    prefs.scanningDisable().put(false);
                    prefs.isScanning().put(false);
                    ((MainActivity) getActivity()).scanForDevices();
                }

            }

            @Override
            public void failedToConnectToNetwork(WifiConnectionFailure failure) {
                showLoaderWithText(false, "");
                tab_device_configuration_router_layout.setVisibility(View.GONE);
                resetDeviceToConfigureView();

                switch (failure) {
                    case Connected_To_3G:
                        show3GDialog();
                        break;
                    case Timeout:
                    case Unknown:
                        //	getActivity().startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

                        showToastWithMessage("You must connect to a router first");
                        break;

                    case Wrong_Password:
                        showToastWithMessage("The password you entered for the simplelink is wrong, please try again");
                        break;
                }
            }
        }, true);
	}

	/**
	 * If requested SL device was found moves to configuration verification retrieval from SL device as station,
	 * if requested SL device was not found tries to connect to SL device as access point,
	 * if connection successful continues to retrieve configuration verification from it,
	 * if not informs user of this failure.
	 *
	 * @see DeviceConfiguration#confirmResult(Boolean) confirmResult(Boolean)
	 *
	 */
	@UiThread
	void checkParams() {
		if (!deviceFound) {
			prefs.scanningDisable().put(true);
			((MainActivity) getActivity()).stopPing();
			print("Verify via AP, Connecting to " + chosenWifiSSID + ".");
			mLogger.info("*AP* Connecting to SL device as AP in order to obtain cfg verification");
			WifiNetworkUtils.getInstance(getActivity()).clearCallback();
			WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(getActivity(), chosenWifiSSID, SecurityType.OPEN, null);
			WifiNetworkUtils.getInstance(getActivity()).connectToWifi(configuration, getActivity(), new BitbiteNetworkUtilsCallback() {
                @Override
                public void successfullyConnectedToNetwork(String ssid) {
                    mLogger.info("*AP* Successfully connected to SL device \"" + ssid + "\" as AP for cfg verification");
                    if (mDevice == null) {
                        //SL device was not discovered as STA in network but connected to as AP for configuration
                        // verification - so no mDevice was obtained, obtain via HTTP Server
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            new GetDeviceIP().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
                        } else {
                            new GetDeviceIP().execute("");
                        }
                    }
                    confirmResult(false);
                    WifiNetworkUtils.getInstance(getActivity()).clearCallback();
                }

                @Override
                public void failedToConnectToNetwork(WifiConnectionFailure failure) {
                    showLoaderWithText(false, null);
                    ((MainActivity) getActivity()).showSuccessDialog("Failed to connect to simple link for cfg verification", getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
                    WifiNetworkUtils.getInstance(getActivity()).clearCallback();
                }
            }, false);
		}
		else
		{
			confirmResult(true);
			//finish(false);
		}

	}

	/**
	 * Called after access point mode provisioning process is complete,
	 * resets the UI fields to their initial state, connects to initial network and
	 * moves the UI to "Devices" tab.
	 *
	 * @param moveToDevice    Boolean. True in order to connect to starting SSID and move to device tab.
	 *
	 * @see DeviceConfiguration#setToReady(Boolean) setToReady(Boolean)
	 * @see DeviceConfiguration#resetDeviceToConfigureView() resetDeviceToConfigureView()
	 * @see MainActivity#changeToDevices(Device)
	 */
	@UiThread
	void finish(final Boolean moveToDevice) {

		mLogger.info("*AP* *** SL device AP mode provisioning process complete. Success: " + moveToDevice + " - SL device is connected to the network: " + moveToDevice + " ***");
		if (wifiList != null) {
			wifiList.clear();
			wifiList = null;
		}

		mDeviceName = "";
		scanningnMDNS = false;


		print("Connecting to the initial network: " + startingSSID);
		mLogger.info("*AP* Connecting to initial network: \"" + startingSSID + "\"");
		setToReady(false);
		tab_device_configuration_router_layout.setVisibility(View.GONE);
		resetDeviceToConfigureView();
		tab_device_configuration_router_device_pick_image.setImageResource(R.drawable.new_graphics_white_box_pick_red);

		WifiNetworkUtils.getInstance(getActivity()).clearCallback();

		if (startingSSID != null && startingSSID.equalsIgnoreCase(WifiNetworkUtils.getInstance(getActivity()).getConnectedSSID()))
		{

			if (moveToDevice) {
				mLogger.info("*AP* Already connected to initial network, moving to device tab with mDNS SL device: " + mDevice + " to display provisioned SL device");
				//6.2
				//fix for LG G4 when going into sleep mode - losing tabs
//				PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
//				boolean isScreenOn = pm.isScreenOn();
//
//				if(isScreenOn) {
//
					MainActivity activity = (MainActivity) getActivity();
					activity.changeToDevices(mDevice);
			//	}
//				MainActivity activity = (MainActivity) getActivity();
//				activity.changeToDevices(mDevice);
			}
		}
		else if (startingSSID != null)
		{

			WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(getActivity(), startingSSID, SecurityType.OPEN, null);
			WifiNetworkUtils.getInstance(getActivity()).connectToWifi(configuration, getActivity(), new BitbiteNetworkUtilsCallback() {
				@Override
				public void successfullyConnectedToNetwork(String ssid) {
					if (moveToDevice) {
						mLogger.info("*AP* connected to initial network, moving to device tab with mDNS SL device: " + mDevice);
						MainActivity activity = (MainActivity) getActivity();
						activity.changeToDevices(mDevice);
					}
				}

				@Override
				public void failedToConnectToNetwork(WifiConnectionFailure failure) {
					showToastWithMessage("Failed to connect to initial network " + startingSSID);
				}
			}, true);

		} else {
			mLogger.info("Initial network is null - will not attempt to connect");
			Log.i(TAG,"Initial network is null - will not attempt to connect");

			showToastWithMessage("No initial network to connect to");
		}

        if (moveToDevice) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new OTAAndType().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            } else {
                new OTAAndType().execute("");
            }
        }
	}

	/**
	 * Called when the activity starts interacting with the user,
	 * Registers broadcast receivers.
	 *
	 * @see DeviceConfiguration#myWifiReceiver myWifiReceiver
	 * @see DeviceConfiguration#scanFinishedReceiver scanFinishedReceiver
	 * @see DeviceConfiguration#deviceFoundReceiver deviceFoundReceiver
	 */
	@Override
	public void onResume() {
		super.onResume();

        startConfigButtonState();

		getActivity().registerReceiver(myWifiReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		getActivity().registerReceiver(scanFinishedReceiver, new IntentFilter(SmartConfigConstants.SCAN_FINISHED_BROADCAST_ACTION));
		getActivity().registerReceiver(deviceFoundReceiver, new IntentFilter(SmartConfigConstants.DEVICE_FOUND_BROADCAST_ACTION));
//		if (inInitiateScan) {
//			initiateScan();
//			Log.e(TAG,"Scan running from onResume");
//		}
//
//			inInitiateScan = false;
//			try {
//				getActivity().unregisterReceiver(receiverWifi);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
	}

	/**
	 * Called when the activity is going into the background, but has not (yet) been killed,
	 * unregisters broadcast receivers.
	 *
	 * @see DeviceConfiguration#receiverWifi receiverWifi
	 * @see DeviceConfiguration#scanFinishedReceiver scanFinishedReceiver
	 * @see DeviceConfiguration#deviceFoundReceiver deviceFoundReceiver
	 */
	@Override
	public void onPause() {
		super.onPause();
	//unregisters the receivers
		try {
			scanningnWifi = false;
			getActivity().unregisterReceiver(receiverWifi);
			mHandler.removeCallbacks(mScanningWifiTimeoutRunnable);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			getActivity().unregisterReceiver(scanFinishedReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			getActivity().unregisterReceiver(deviceFoundReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			getActivity().unregisterReceiver(myWifiReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
		showLoaderWithText(false,"");

	}


	/**
	 * Logs and displays String passed as parameter to user.
	 *
	 * @param string    String. The String to log and display to user.
	 */
	@UiThread
	public void print(String string) {
		Log.i(TAG, string);
		tab_device_configuration_loader_label.setText(string);
	}

	/**
	 * Starts a scan for available access points by the WifiManager.
	 *
	 * @see com.ti.smartconfig.utils.NetworkUtil#startScan(Context)
	 */
	private void startWifiScan() {
		if (!scanningnWifi) {

			print("Searching for devices Do not abort!");
			scanningnWifi = true;

			showLoaderWithText(true, "Scanning for wifi networks");
			mHandler.postDelayed(mScanningWifiTimeoutRunnable, Constants.WIFI_SCAN_TIMEOUT);
			getActivity().registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			NetworkUtil.startScan(getActivity());
		}
		else {
			mLogger.info("*AP* Already wifi scanning");
			mHandler.removeCallbacks(mScanningWifiTimeoutRunnable);
			mHandler.postDelayed(mScanningWifiTimeoutRunnable, Constants.WIFI_SCAN_TIMEOUT);
		}
	}

	/**
	 * Displays an explanatory dialog related to the adjacent field, upon pressing the question mark button.
	 */
	@Click
	void tab_device_configuration_router_question_button() {
		((MainActivity)getActivity()).showSuccessDialog(Constants.QUESTION_CHOOSE_ROUTER, getString(R.string.pop_up_close), null, PopupType.Information, null, null);
	}

	/**
	 * Displays an explanatory dialog related to the adjacent field, upon pressing the question mark button.
	 */
	@Click
	void tab_device_configuration_device_to_configure_question_button() {
		((MainActivity)getActivity()).showSuccessDialog(Constants.QUESTION_CHOOSE_DEVICE, getString(R.string.pop_up_close), null, PopupType.Information, null, null);
	}

	/**
	 * Displays an explanatory dialog related to the adjacent field, upon pressing the question mark button.
	 */
	@Click
	void tab_device_configuration_device_name_question_button() {
		((MainActivity) getActivity()).showSuccessDialog(Constants.QUESTION_DEVICE_NAME, getString(R.string.pop_up_close), null, PopupType.Information, null, null);
	}

	/**
	 * Displays a toast containing the String passed as parameter.
	 *
	 * @param msg    String. The text to be displayed in the toast.
	 */
	@UiThread
	void showToastWithMessage(final String msg) {

		try {

			Toast.makeText(mainActivity, msg, Toast.LENGTH_LONG).show();

			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					try {
						//Toast.makeText(mainActivity, msg, Toast.LENGTH_LONG).show();
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
				}
			}, 2000);

		}catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Displays the scan results pop up view, which allows the user to choose a Wi-Fi network to
	 * send as profile to connect to to SL device, out of a list of available access points.
	 *
	 * @see com.ti.smartconfig.utils.ScanResultsPopUpView ScanResultsPopUpView
	 */
	@UiThread
	void showDeviceConfigurationDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		scanResultsPopUpView = ScanResultsPopUpView_.build(getActivity());
		scanResultsPopUpView.deviceConfiguration = this;
		scanResultsPopUpView.start();
		alertDialog = builder.create();
		alertDialog.setView(scanResultsPopUpView, 0, 0, 0, 0);
		alertDialog.show();

	}

	/**
	 * Calls a method which starts mDNS discovery and Ping, and displays relevant progress dialog.
	 *
	 * @see MainActivity#scanForDevices()
	 */
	@UiThread
	void scanForDevices() {
		deviceFound = false;
		scanningnMDNS = true;
		Log.i(TAG, "DeviceConfiguration - scanForDevices, scanningnMDNS:" + scanningnMDNS);

			final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {

				mLogger.info("*AP* Going to show progress dialog for mDNS scan");
				showProgressDialog(SmartConfigConstants.MDNS_SCAN_TIME, Constants.MDNS_SCAN_MESSAGE);
				prefs.scanningDisable().put(false);
				prefs.isScanning().put(false);
				((MainActivity) getActivity()).scanForDevices();

			}
		}, Constants.DEVICE_WAITING_TIME_BEFORE_STARTING_MDNS_SCAN);
	}

	/**
	 * Called when the UI "Start Configuration" button is pressed,
	 * starts the configuration stage of the provisioning process by executing an asynchronous task
	 * of adding a Wi-Fi profile to the SL device based on the user's input.
	 *
	 * @see com.ti.smartconfig.utils.AddProfileAsyncTask
	 */
	@SuppressWarnings("unchecked")
	@Click
	void tab_device_configuration_start_button() {
		mLogger.info("*AP* *** Starting SL device configuration in AP mode - \"start configuration\" button pressed ***");

		// Vibrate for 100 milliseconds
		v.vibrate(100);
		if (!mIsReady) {
			return;
		}

		if (ssidToAdd == null || ssidToAdd.equals("") || tab_device_configuration_router_device_pick_label.getText().toString().contains("mysimplelink")) {
			showToastWithMessage(Constants.DEVICE_LIST_MUST_SUPPLY_SSID);
			return;
		} else {
			//change button color fix added
			tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
			scanFinishTimer = true;
			deviceFound = false;
			ssidToAdd = tab_device_configuration_router_device_pick_label.getText().toString();
			//12.1 ofir :added string to the loader - giving the wrong impression if it's blank / user may think the application stuck
			showLoaderWithText(true, "Starting Configuration");

			ArrayList<Object> passing = new ArrayList<>();
			passing.add(tab_device_configuration_device_name_editText.getText().toString());
			passing.add(ssidToAddSecurityType);
			passing.add(ssidToAdd);
			passing.add(ssidToAddSecurityKey);
			passing.add(ssidToAddPriority);
			passing.add(tab_device_configuration_iot_uuid_name_editText.getText().toString());

			mLogger.info("*AP* Executing AddProfileAST to set a new wifi profile to SL device" +
					"\n***\nSend\nSL device name: " + tab_device_configuration_device_name_editText.getText().toString() +
					"\nSSID to add: " + ssidToAdd + "\nPass len: " + ssidToAddSecurityKey.length() + "\n***");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

				new AddProfileAsyncTask(mAddProfileAsyncTaskCallback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, passing);
				//print for testing
				Log.e(TAG,"profile added #1");
			} else {
				new AddProfileAsyncTask(mAddProfileAsyncTaskCallback).execute(passing);
				//print for testing
				Log.e(TAG,"profile added #2");
			}

			if (tab_device_configuration_device_name_editText.hasFocus() || tab_device_configuration_password_check_editText.hasFocus() || tab_device_configuration_iot_uuid_name_editText.hasFocus()) {
				InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
				if (getView() != null) {
					inputMethodManager.hideSoftInputFromWindow(getView().getWindowToken(), 0);
				}

			}
			tab_device_configuration_password_check_editText.setText("");
		}
	}



	/**
	 * Sets the UI "Device connection" bar's state (background color and text)
	 * to reflect the current Wi-Fi connection status of the mobile phone.
	 */
	private void DisplayWifiState(){
		ConnectivityManager myConnManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//		WifiManager myWifiManager = (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
	//	WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
		String ssid = NetworkUtil.getConnectedSSID(getActivity());
		if (myNetworkInfo.isConnected()) {
			tab_device_configuration_device_connection.setText("Connected to : " +ssid);
	//		tab_device_configuration_device_connection.setTextColor(getResources().getColorStateList(R.color.color_connection_text_sc));
			tab_device_configuration_device_connection.setTextColor(Color.WHITE);
			textViewConnectionTextView.setBackgroundColor(getResources().getColor(R.color.color_connection_text_sc_holo_grey));

			//	Toast.makeText(getActivity(),""+myWifiInfo.getSSID(),Toast.LENGTH_LONG).show();
		} else {
			tab_device_configuration_device_connection.setText("No Wifi Connection");
			tab_device_configuration_device_connection.setTextColor(Color.WHITE);
			textViewConnectionTextView.setBackgroundColor(getResources().getColor(R.color.color_red));
			//	Toast.makeText(getActivity(),""+myWifiInfo.getSSID(),Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * An asynchronous task which attempts to retrieve the SL device's version,
	 * if successful sets up the UI router device pick layout for further action,
	 * if unsuccessful resets the UI device to configure view.
	 *
	 * @see com.ti.smartconfig.utils.NetworkUtil#getSLVersion(String)
	 * @see DeviceConfiguration#resetDeviceToConfigureView()
	 */
	class GetDeviceVersion extends AsyncTask<String, Void, DeviceVersion> {

		@Override
		protected void onPostExecute(DeviceVersion result) {

			showLoaderWithText(false, "");
			if (result != DeviceVersion.UNKNOWN) {
				try {
					tab_device_configuration_router_device_pick_label.setTextColor(getActivity().getResources().getColor(R.color.color_line));
					tab_device_configuration_router_device_pick_label.setText(getString(R.string.tab_device_configuration_ap_search_router));
					tab_device_configuration_router_layout.setVisibility(View.VISIBLE);
					//savedSSID= NetworkUtil.getConnectedSSID(getActivity());
					//tab_device_configuration_router_device_pick_label.setText(chosenWifiSSID);

					if(tab_device_configuration_device_to_configure_device_pick_label.getText().toString().contains("mysimplelink")) {

						tab_device_configuration_router_device_pick_label.setText(savedSSID);
						tab_device_configuration_router_device_pick_label.setTextColor(Color.BLACK);

//						if (chosenWifiSSID != null && !(savedSSID.equals(""))) {
						if (!(savedSSID.equals(""))) {

							if (ssidToAddSecurityType == ssidToAddSecurityType.WPA2) {
								tab_device_configuration_password_check_layout.setVisibility(View.VISIBLE);
                                tab_device_configuration_password_check_editText.setText("");
							} else if (ssidToAddSecurityType == ssidToAddSecurityType.WPA1) {
								tab_device_configuration_password_check_layout.setVisibility(View.VISIBLE);
                                tab_device_configuration_password_check_editText.setText("");
							} else if (ssidToAddSecurityType == ssidToAddSecurityType.WEP) {
								tab_device_configuration_password_check_layout.setVisibility(View.VISIBLE);
                                tab_device_configuration_password_check_editText.setText("");
							} else if (ssidToAddSecurityType == ssidToAddSecurityType.OPEN) {
								tab_device_configuration_password_check_layout.setVisibility(View.GONE);
                                startConfigButtonStateOpenNetwork();



                            }

						}
					}
				} catch (NullPointerException e) {
					Log.e(TAG, "GetDeviceVersion, onPostExecute, ui was null");
				}
			}
			else {
				try {
					tab_device_configuration_router_layout.setVisibility(View.GONE);
					resetDeviceToConfigureView();
				} catch (NullPointerException e) {
					Log.e(TAG, "GetDeviceVersion, onPostExecute, ui was null");
				}

				((MainActivity)getActivity()).showSuccessDialog(Constants.DEVICE_LIST_FAILED_TO_GET_RESULTS, getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
			}

			super.onPostExecute(result);
		}


		@Override
		protected DeviceVersion doInBackground(String... params) {
			deviceVersion = DeviceVersion.UNKNOWN;
			try {
				deviceVersion = NetworkUtil.getSLVersion(Constants.BASE_URL);
			} catch (Exception e) {
				e.printStackTrace();
				//15.1 ofir - removing loader if we have an exception...
				showLoaderWithText(false,"");
			}

			return deviceVersion;
		}
	}

	/**
	 * Callbacks for AddProfileAsyncTask.
	 */
	private AddProfileAsyncTaskCallback mAddProfileAsyncTaskCallback = new AddProfileAsyncTaskCallback() {
		/**
		 * Logs and displays the String passed as parameter, used to report profile addition progress.
		 *
		 * @param errorMessage    String. the text to be logged and displayed.
		 */
		@Override
		public void addProfileMsg(String errorMessage) {
			print(errorMessage);
			mLogger.info("*AP* Wifi profile addition to SL device in progress, msg: " + errorMessage);
		}

		/**
		 * Logs failure of profile addition, dismisses loader from UI and displays toast informing user of the failure.
		 *
		 * @param errorMessage    String. the text to be logged and displayed.
		 */
		@Override
		public void addProfileFailed(String errorMessage) {
//			showToastWithMessage(errorMessage);
			showLoaderWithText(false, "");
			mLogger.info("*AP* Wifi profile addition to SL device completed unsuccessfully, msg: " + errorMessage);
		}

		/**
		 * Called when profile addition to SL device is complete,
		 * moves to the configuration verification stage of the provisioning process,
		 * either via SL device as station or via SL device as access point,
		 * according to the availability of the SSID sent in the profile, in regards to the mobile phone.
		 *
		 * @see DeviceConfiguration#checkParams()
		 * @see MainActivity#startPing()
		 * @see MainActivity#restartUdp()
		 * @see DeviceConfiguration#scanForDevices()
		 * @see DeviceConfiguration#resetDeviceToConfigureView()
		 */
		@Override
		public void addProfileCompleted() {
			mLogger.info("*AP* Wifi profile addition to SL device completed successfully");
			mLogger.info("*AP* Requested SL device restart and move state to STA for cfg verification");
			ScanResult result = null;
			for (ScanResult scanResult : wifiList) {
				if (scanResult.SSID.equals(ssidToAdd)) {
					result = scanResult;
					break;
				}
			}

			if (result == null) {
				print("Network " + ssidToAdd + " is not in range. Your mobile needs to connect to the device in order to confirm configuration has succeeded.");
				mLogger.info("*AP* SSID: \"" + ssidToAdd + "\" sent to SL device in added profile to connect to is not in range of mobile device." +
						" Cfg verification retrieval will be executed by connecting to SL device as AP ");
				checkParams();
			}

			else {
				print("Connecting to " + ssidToAdd + " in order to confirm device configuration has succeeded.\n DO NOT ABORT!");
				mLogger.info("*AP* Connecting to \"" + ssidToAdd + "\" in order to obtain cfg verification");
				WifiNetworkUtils.getInstance(getActivity()).clearCallback();
				WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(getActivity(), ssidToAdd, ssidToAddSecurityType, ssidToAddSecurityKey);
				WifiNetworkUtils.getInstance(getActivity()).connectToWifi(configuration, getActivity(), new BitbiteNetworkUtilsCallback() {
					@Override
					public void successfullyConnectedToNetwork(String ssid) {
						isSearching = true;


						print("Connected to " + ssidToAdd + ". Searching for new devices\n DO NOT ABORT!");
						mLogger.info("*AP* Connected to SSID in profile added to SL device: \"" + ssidToAdd + "\"." +
								" Searching for new SL devices in the local network for cfg verification: Activating Ping Bcast + UDPBcastServer + mDNS discovery");
						((MainActivity) getActivity()).startPing();
						((MainActivity) getActivity()).restartUdp();




						if (!deviceFound) {
							//if (startingSSID == ssid) {
							scanForDevices();
							//}
						}
					}

					@Override
					public void failedToConnectToNetwork(WifiConnectionFailure failure) {
						showLoaderWithText(false, null);
						resetDeviceToConfigureView();
						setToReady(false);

						switch (failure) {
						case Connected_To_3G:
							show3GDialog();
							break;
						case Timeout:
						case Unknown:
							showToastWithMessage("There was an unknown error connecting to the network");
							break;
						case Wrong_Password:
							showToastWithMessage("The password you entered for the network is wrong please try again");
							break;
						}
					}
				}, true);
			}
		}

		/**
		 * Sets the String passed as parameter as the mDeviceName.
		 *
		 * @param deviceName    String. The text to be the mDeviceName.
		 */
		@Override
		public void addProfileDeviceNameFetched(String deviceName) {
			mDeviceName = deviceName;
		}
	};

	/**
	 * Creates and populates a List of Devices, based on the devicesArray from shared preferences
	 * containing devices discovered via mDNS discovery, Ping broadcast and UDP server.
	 *
	 * @return list of Devices    List<Device>. The populated List.
	 */
	private List<Device> getDevices() {
		List<Device> devices = new ArrayList<>();

		JSONArray devicesArray;
		try {
			devicesArray = new JSONArray(prefs.devicesArray().get());
			for (int i=0; i<devicesArray.length(); i++) { // populate the list
				JSONObject jsonObject = devicesArray.getJSONObject(i);
				Device device = new Device(jsonObject.getString("name"), jsonObject.getString("host"));
				devices.add(device);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return devices;
	}

	/**
	 * An asynchronous task which retrieves the configuration verification from the SL device,
	 * and either finishes the provisioning process successfully/unsuccessfully or calls for another
	 * configuration verification retrieval attempt according to the result obtained.
	 *
	 * @see com.ti.smartconfig.utils.NetworkUtil#getCGFResultFromDevice(String, DeviceVersion)
	 * @see com.ti.smartconfig.utils.NetworkUtil#cfgEnumForResponse(String)
	 * @see com.ti.smartconfig.utils.NetworkUtil#getErrorMsgForCFGResult(CFG_Result_Enum)
	 * @see DeviceConfiguration#finish(Boolean)
	 * @see DeviceConfiguration#getCFG(Boolean)
	 */
	class GetCFGResult extends AsyncTask<Boolean, Void, String> {

		private Boolean mIsViaWifi;

		@Override
		protected void onPostExecute(String result) {

			Log.i(TAG, "Cfg result from SL: " + result);
			mLogger.info("*AP* Cfg result text: " + result);

			if (result != null) {
				showLoaderWithText(false, "");

				if (NetworkUtil.getResultTypeCFGString(result) == CFG_Result_Enum.Success) {
					deviceFound = true;
					((MainActivity)getActivity()).showSuccessDialog(null, getString(R.string.pop_up_close), null, PopupType.Success, null, result);
					//fetching device IP -lan page 8.2
					try {
						sharedpreferences = getActivity().getSharedPreferences(mypreference, Context.MODE_PRIVATE);
						String simplelinkDeviceIp = mDevice.host;
						SharedPreferences.Editor editor = sharedpreferences.edit();
						editor.putString(Name, simplelinkDeviceIp);
						editor.commit();
						Log.i(TAG, "Entered IP into SP: " + mDevice.host);
						mLogger.info("*AP* Entered IP into SP: " + mDevice.host);


					} catch (NullPointerException e) {
						e.printStackTrace();
					}
			//		((MainActivity) getActivity()).EnableOutOfTheBoxTabs(true);
					finish(true);
				}
				else {
					((MainActivity)getActivity()).showSuccessDialog(result, getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
					finish(false);
				}
			}
			else {
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						cgfTryNumber++;
						getCFG(mIsViaWifi);
					}
				}, Constants.DELAY_BETWEEN_CFG_RESULTS_REQUESTS);
			}

			super.onPostExecute(result);
		}

		@Override
		protected String doInBackground(Boolean... params) {
			mLogger.info("GetCFGResult doInBackground called");

			mIsViaWifi = params[0];
			String result;

			String resultString;
//			String baseUrl = Constants.BASE_URL;
			String baseUrl = Constants.BASE_URL_NO_HTTP;
			if (mIsViaWifi && mDevice != null) {
//				baseUrl = "http://" + mDevice.host;
				baseUrl = "://" + mDevice.host;
			}

			resultString = NetworkUtil.getCGFResultFromDevice(baseUrl, deviceVersion);
			Log.i(TAG, "Getting cfg result from SL (" + resultString + ")");
			mLogger.info("*AP* Got cfg result from SL device: " + resultString);

			CFG_Result_Enum result_Enum = NetworkUtil.cfgEnumForResponse(resultString);
			result = NetworkUtil.getErrorMsgForCFGResult(result_Enum);

			return result;
		}
	}





	class OTAAndType extends AsyncTask<String,Void,Device_Type_Enum> {

		@Override
		protected Device_Type_Enum doInBackground(String... params) {

			Device_Type_Enum deviceTypeEnum = null;

			try {
				Log.i(TAG, "OTAAndType doInBackground");

//				String baseUrl = "http://" + mDevice.host;
				String baseUrl = "://" + mDevice.host;
				Log.i(TAG, "OTAAndType baseUrl: " + baseUrl);

				try {
					deviceTypeEnum = NetworkUtil.slDeviceOTAAndType(baseUrl);
				} catch (Exception e) {
					e.printStackTrace();
				}
				Log.i(TAG, "OTAAndType: " + deviceTypeEnum);
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			return deviceTypeEnum;

		}

		@Override
		protected void onPostExecute(Device_Type_Enum deviceTypeEnum) {
			super.onPostExecute(deviceTypeEnum);

			Log.i(TAG, "OTAAndType onPost, result: " + deviceTypeEnum);

			mainActivity.deviceTypeEnum = deviceTypeEnum;
			Log.i(TAG,"OTAAndType set result to main: " + mainActivity.deviceTypeEnum);

				mainActivity.clearAllTabs();

			if (startingSSID != null && startingSSID.equalsIgnoreCase(WifiNetworkUtils.getInstance(mainActivity).getConnectedSSID())) {
				//refresh tabs in order to display extra tabs - and move to Devices tab
				mainActivity.initTabs(1);
			} else {
				//refresh tabs in order to display extra tabs - and do not move to Devices tab
				mainActivity.initTabs(0);
			}



		}


	}





	/**
	 * Displays the device pick pop up view enabling the user to pick an SL device to configure,
	 * and calls a method which analyzes the current mobile phone's Wi-Fi connection's SSID security status,
	 * and sets it as the SSID to send to the SL device in the Wi-Fi profile to connect to.
	 *
	 * @see DeviceConfiguration#ssidSecurityChecker()
	 * @see com.ti.smartconfig.utils.DevicePickPopUpView
	 */
	@Click
	void tab_device_configuration_device_to_configure_device_pick_layout() {
		ConnectivityManager myConnManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//		WifiManager myWifiManager = (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
		//	WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
		String ssid = NetworkUtil.getConnectedSSID(getActivity());

		if (alertDialog != null && alertDialog.isShowing()) {
			alertDialog.cancel();
			alertDialog = null;
		} else {
			alertDialog = null;
		}
//device configuration alert dialog
		savedSSID= NetworkUtil.getConnectedSSID(getActivity());
		//tab_device_configuration_router_device_pick_label.setText(chosenWifiSSID);
		ssidSecurityChecker();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		devicePickPopUpView = DevicePickPopUpView_.build(getActivity());

		alertDialog = builder.create();
		alertDialog.setView(devicePickPopUpView, 0, 0, 0, 0);
		devicePickPopUpView.deviceConfiguration = this;

		if (wifiList != null)
			devicePickPopUpView.wifiList = wifiList;
//		mLogger.info("*AP* Starting wifiManager scan to find SL device as AP");
		devicePickPopUpView.start();

		alertDialog.show();
	}

	/**
	 * Called when an SL device has been chosen for configuration,
	 * sets the UI to reflect the choice made, and moves to the next provisioning stage of
	 * connecting to the chosen SL device and retrieving its version.
	 *
	 * @param result        ScanResult. The SL device chosen.
	 * @param securityType  SecurityType. Security type of the chosen SL device.
	 * @param password      String.The SL device's password.
	 */
	public void deviceWasChosen(ScanResult result, SecurityType securityType, String password) {

		if (alertDialog != null && alertDialog.isShowing()) {
			alertDialog.cancel();
			alertDialog = null;
		} else {
			alertDialog = null;
		}

		chosenWifiSSID = result.SSID;
		tab_device_configuration_device_to_configure_device_pick_image.setImageResource(R.drawable.new_graphics_scrool_choose);
		tab_device_configuration_device_to_configure_device_pick_label.setTextColor(Color.BLACK);
		tab_device_configuration_device_to_configure_device_pick_label.setText(chosenWifiSSID);
		connectToSSIDAndGetScanResults(chosenWifiSSID, securityType, password);
	}

	/**
	 * Displays the scan results pop up view enabling the user to pick an access point to send
	 * to SL device as Wi-Fi profile to connect to.
	 *
	 * @see DeviceConfiguration#showDeviceConfigurationDialog()
	 */
	//Wifi network dialog
	@Click
	void tab_device_configuration_router_device_pick_layout() {

		showDeviceConfigurationDialog();
	}

	/**
	 * Displays an explanatory dialog related to the adjacent field, upon pressing the question mark button.
	 */
	@Click
	void tab_device_configuration_iot_uuid_button() {
		((MainActivity) getActivity()).showSuccessDialog(Constants.QUESTION_SET_IOT, getString(R.string.pop_up_close), null, PopupType.Information, null, null);
	}


	/**
	 * Displays or dismisses the loader, depending on the Boolean value passed as parameter.
	 *
	 * @param show    Boolean. True to display loader, false to dismiss.
	 * @param msg     String. The text to be displayed if the loader is to be shown.
	 */
	@UiThread
	void showLoaderWithText(Boolean show, String msg) {
		if (!show) {

			tab_device_configuration_loader_layout.setVisibility(View.GONE);
			tab_device_configuration_loader_label.setText("");
		}
		else {
			tab_device_configuration_loader_layout.setVisibility(View.VISIBLE);
			tab_device_configuration_loader_label.setText(msg);
		}
	}

	/**
	 * Displays progress dialog relevant to mDNS scan,
	 * when scan's designated duration has passed dismisses the progress dialog and moves to the
	 * configuration verification stage via SL device as station or access point depending on
	 * whether the SL device had been found in the scan or not.
	 *
	 * @param duration    int. The duration the progress dialog should be displayed
	 * @param title       String. The progress dialog's title.
	 */
	@UiThread
	void showProgressDialog(int duration, String title) {
		if (!isSearching) {
			return;
		}

		mLogger.info("*AP* Showing progress dialog for mDNS scan");

		if (progressDialog != null && progressDialog.isShowing()) {
			mLogger.error("*AP* Going to show progress dialog, but it is already up!!");
			return;
		}

		print(title);
		progressDialog = new ProgressDialog(getActivity());
		progressDialog.setCancelable(false);
		progressDialog.setMessage(title);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setProgress(0);
		progressDialog.setMax(duration / 1000);
		progressDialog.setProgressDrawable(getActivity().getResources().getDrawable(R.drawable.progress_bar_shape));
		progressDialog.show();
                tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

		@Override
		public void onCancel(DialogInterface dialog) {

		}
	});

		//BackButtonPressed -to cancel progress dialog
		progressDialog.setOnKeyListener(new Dialog.OnKeyListener() {
		private long lastPressedTime;
		private static final int PERIOD = 2000;

		@Override
		public boolean onKey(DialogInterface arg0, int keyCode,
		KeyEvent event) {

			switch (event.getAction()) {
				case KeyEvent.ACTION_DOWN:
					if (event.getDownTime() - lastPressedTime < PERIOD) {
						finish(false);

						prefs.scanningDisable().put(true);
						((MainActivity) getActivity()).stopScanning();
						progressDialog.dismiss();

					} else {
						Toast.makeText(getActivity(), "Press again to cancel action",
								Toast.LENGTH_SHORT).show();
						lastPressedTime = event.getEventTime();
					}
					return true;
			}
			return false;
		}
	});


		new Thread(new Runnable() {
			int progress = 0;
			public void run() {
				long timerEnd = System.currentTimeMillis() + progressDialog.getMax() * 1000;

				while (timerEnd >  System.currentTimeMillis() ) {

					if (progressDialog == null || !progressDialog.isShowing())
						break;

					progress = (int) (progressDialog.getMax() - (timerEnd - System.currentTimeMillis()) / 1000);
					updateProgressDialog(progress);
					try { Thread.sleep(1000); } catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();

					if (!deviceFound) {
						scanningnMDNS = false;
						Log.i(TAG, "showProgressDialog - dismiss, scanningnMDNS:" + scanningnMDNS);
						mLogger.info("*AP* Requested SL device has not been found as a STA in the local network." +
								" Cfg verification retrieval will be executed by connecting to SL device as AP");
						checkParams();
					} else {
						Log.i(TAG, "showProgressDialog - dismiss, deviceFound:true");
						print("Found the requested device");
						confirmResult(true);
					}
				}

			}
		}).start();
	}



    class GetDeviceIP extends AsyncTask<String,Void,String> {

        @Override
        protected void onPostExecute(String ip) {
            super.onPostExecute(ip);

            Log.i(TAG, "GetDeviceIP - OnPostExecute, ip: " + ip);
            mLogger.info("*SC* GetDeviceIP - onPostExecute, ip: " + ip);

            if (!ip.equals("")) {
                if (chosenWifiSSID != null) {
                    mDevice = new Device(chosenWifiSSID, ip);
                } else {
                    mDevice = new Device("",ip);
                }
            }
        }

        @Override
        protected String doInBackground(String... params) {

            Log.i(TAG,"GetDeviceIP - DoInBackground");
            mLogger.info("*SC* GetDeviceIP - doInBackground");
            String deviceIp = "";
            deviceIp = NetworkUtil.getDeviceIp(Constants.BASE_URL_NO_HTTP);

            return deviceIp;
        }
    }




    /**
	 * Updates the progress of the progress dialog.
	 *
	 * @param progress   int. New progress value.
	 */
	@UiThread
	void updateProgressDialog(int progress) {
		if (progressDialog != null && progressDialog.isShowing())
			progressDialog.setProgress(progress);
	}

	/**
	 * Calls a method which shows an AlertDialog informing the user of data flowing
	 * via the mobile network that should be turned off,
	 * and allows the user quick access to the relevant settings.
	 *
	 * @see MainActivity#show3GDialog()
	 */
	private void show3GDialog() {
		MainActivity main = (MainActivity) getActivity();
		main.show3GDialog();
	}

	/**
	 * Called upon a change in mobile phone's Wi-Fi connectivity,
	 * analyzes the security type of the SSID the mobile phone is currently connected to,
	 * sets the UI password field and "Start Configuration" button state accordingly,
	 * and sets the current SSID as the SSID to send to SL device as profile to connect to.
	 *
	 * @see com.ti.smartconfig.utils.NetworkUtil#getConnectedSSID(Context)
	 */
	void ssidSecurityChecker() {
        //this method connected to the broadcast receiver who listen to network changes

        ConnectivityManager myConnManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        chosenWifiSSID = NetworkUtil.getConnectedSSID(getActivity());

        WifiManager wifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> networkList = wifi.getScanResults();
        if (chosenWifiSSID == null || savedSSID.equals("")) {
            tab_device_configuration_password_check_layout.setVisibility(View.GONE);
        }
        if (chosenWifiSSID != null) {
            if (networkList != null) {
                for (ScanResult network : networkList) {

                    if (chosenWifiSSID.equalsIgnoreCase(network.SSID)) {
                        //get capabilities of current connection
                        String Capabilities = network.capabilities;
                        Log.d(TAG, network.SSID + " capabilities : " + Capabilities);

                        if (Capabilities.contains("WPA2")) {
//							tab_device_configuration_password_check_layout.setVisibility(View.VISIBLE);
                            tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
                            ssidToAddSecurityType = ssidToAddSecurityType.WPA2;
                            ssidToAdd = chosenWifiSSID;
                            ssidToAddPriority = "0";
                            mIsReady = false;
                            setToReady(mIsReady);
                            Log.v(TAG, "+++++++++++++++++++++++++");
                            Log.v(TAG, "++++++++++WPA2+++++++++++");
                            Log.v(TAG, "+++++++++++++++++++++++++");

                        } else if (Capabilities.contains("WPA")) {
//							tab_device_configuration_password_check_layout.setVisibility(View.VISIBLE);
                            tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
                            ssidToAddSecurityType = ssidToAddSecurityType.WPA1;
                            ssidToAdd = chosenWifiSSID;
                            ssidToAddPriority = "0";
                            mIsReady = false;
                            setToReady(mIsReady);
                            Log.v(TAG, "+++++++++++++++++++++++++");
                            Log.v(TAG, "++++++++++WPA1+++++++++++");
                            Log.v(TAG, "+++++++++++++++++++++++++");

                            Toast.makeText(getActivity(), "WPA", Toast.LENGTH_LONG).show();
                        } else if (Capabilities.contains("WEP")) {
//							tab_device_configuration_password_check_layout.setVisibility(View.VISIBLE);
                            tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
                            ssidToAddSecurityType = ssidToAddSecurityType.WEP;
                            ssidToAdd = chosenWifiSSID;
                            ssidToAddPriority = "0";
                            mIsReady = false;
                            setToReady(mIsReady);
                            Log.v(TAG, "+++++++++++++++++++++++++");
                            Log.v(TAG, "+++++++++++WEP+++++++++++");
                            Log.v(TAG, "+++++++++++++++++++++++++");

                        } else if (Capabilities.contains("ESS")) {
//							tab_device_configuration_password_check_layout.setVisibility(View.GONE);
                            Log.v(TAG, "+++++++++++++++++++++++++");
                            Log.v(TAG, "+++++++++++OPEN+++++++++++");
                            Log.v(TAG, "+++++++++++++++++++++++++");

                            ssidToAdd = chosenWifiSSID;
                            ssidToAddPriority = "0";
                            ssidToAddSecurityKey = "";
                            ssidToAddSecurityType = ssidToAddSecurityType.OPEN;
                            startConfigButtonStateOpenNetwork();
                        }
                    }
                }
            }
        }
    }


    public void startConfigButtonStateOpenNetwork() {

        if (tab_device_configuration_device_name_layout.getVisibility() == View.VISIBLE) {
            if (tab_device_configuration_device_name_editText.length() > 0) {
                tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_on);
                tab_device_configuration_start_button.setEnabled(true);
                mIsReady = true;
                setToReady(mIsReady);
            } else {
                tab_device_configuration_device_name_editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (tab_device_configuration_device_name_editText.length() > 0) {
                            tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_on);
                            tab_device_configuration_start_button.setEnabled(true);
                            mIsReady = true;
                            setToReady(mIsReady);
                        } else {
                            tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_off);
                            tab_device_configuration_start_button.setEnabled(false);
                            mIsReady = false;
                            setToReady(mIsReady);
                        }

                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
            }
        } else {
            tab_device_configuration_start_button.setImageResource(R.drawable.start_configuration_button_on);
            tab_device_configuration_start_button.setEnabled(true);
            mIsReady = true;
            setToReady(mIsReady);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mainActivity = (MainActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }
}
