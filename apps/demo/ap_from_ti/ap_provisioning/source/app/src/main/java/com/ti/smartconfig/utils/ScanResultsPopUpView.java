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
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import com.ti.smartconfig.R;
import com.ti.smartconfig.DeviceConfiguration;
import com.ti.smartconfig.NewSmartConfigFragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

@EViewGroup(R.layout.scan_results_pop_up_view)
public class ScanResultsPopUpView extends RelativeLayout {

	private String startingSSID;
	private static final String TAG = "ScanResultsPopUpView";

	private ArrayList<ScanResult> wifiList;
	private Boolean scanningnWifi = false;
	public List<String> mFullList;
	public List<String> scanResultList;
	private Context mContext;
	private ScanResultListAdapter adapter;
	private SecurityType securityType = SecurityType.UNKNOWN;
	public DeviceVersion deviceVersion = DeviceVersion.UNKNOWN;
	private int selectedItem = -1;
	public Boolean	buttonClickBool=false;
	HashMap<String, SecurityType> mSecurityMapping;
	
	private Boolean showAll = false;

	public DeviceConfiguration deviceConfiguration;
	public NewSmartConfigFragment newSmartConfigFragment;

	private String manualSsid = null;
	private TextWatcher mTextWatcher;
	private HashMap<String,Integer> mRSSIMap;



	final private Logger mLogger = Logger.getLogger(ScanResultsPopUpView.class);
	
	@Pref
	SharedPreferencesInterface_ prefs;

	@ViewById
	ListView scan_results_pop_up_list;
	@ViewById
	ImageView scan_results_pop_up_buttons_ok_button;
	@ViewById
	ImageView scan_results_pop_up_buttons_rescan_button;
	@ViewById
	ProgressBar scan_results_pop_up_loader;
	@ViewById
	RelativeLayout scan_results_pop_up_password_layout;
	@ViewById
	EditText scan_results_pop_up_pas_editText;
	@ViewById
	CheckBox scan_checkbox;

	public ScanResultsPopUpView(Context context) {
		super(context);
		mContext = context;
	}

	@AfterViews
	void afterViews() {
		scan_results_pop_up_pas_editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		scan_results_pop_up_password_layout.setVisibility(View.GONE);

		scan_results_pop_up_loader.setVisibility(View.INVISIBLE);
		scan_results_pop_up_list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
									long arg3) {

				int neededPosition = showAll ? scanResultList.size() + 1 : scanResultList.size();
				if (arg2 == neededPosition) {
					showAll = !showAll;
					if (showAll) {
						scanResultList.clear();
						scanResultList.addAll(mFullList);
					} else {
						scanResultList.clear();
//need to add condition
						if (deviceConfiguration != null) {
							if (deviceConfiguration.savedSSID != null && !deviceConfiguration.savedSSID.contains(Constants.DEVICE_PREFIX)) {
								if (mFullList.size() > 0)
									scanResultList.add(mFullList.get(0));
							}
						} else if (newSmartConfigFragment != null) {
							if (mFullList.size() > 0 && startingSSID != null)
								scanResultList.add(mFullList.get(0));
						}
					}

					scan_results_pop_up_password_layout.setVisibility(View.GONE);

					scan_results_pop_up_buttons_ok_button.setImageResource(R.drawable.new_graphics_ok_off);
					selectedItem = -1;
				} else {
					manualSsid = "";
					selectedItem = arg2;
					scan_results_pop_up_buttons_ok_button.setImageResource(R.drawable.new_graphics_ok_on);
					checkForPasswordLayout(scanResultList.get(arg2));
				}

				adapter.notifyDataSetChanged();
			}
		});


		mTextWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable ssidString) {
				manualSsid = ssidString.toString();

				if (!manualSsid.equals("")) {
					scan_results_pop_up_buttons_ok_button.setImageResource(R.drawable.new_graphics_ok_on);
					checkForPasswordLayout(manualSsid);
				}
			}
		};

		scan_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// checkbox status is changed from unchecked to checked.
				if (isChecked) {
					// show password
					scan_results_pop_up_pas_editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
				} else {
					// hide password
					scan_results_pop_up_pas_editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
				}
			}
		});
	}

	public void start() {
		if (deviceConfiguration != null) {
			scan();
		}
		else {
			mContext.registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			startWifiScan();
		}
	}
	
	@UiThread
	void checkForPasswordLayout(String ssid) {
		securityType = checkSSIDSecurityType(ssid);
		switch (securityType) {
		case OPEN:
			scan_results_pop_up_password_layout.setVisibility(View.GONE);
			break;
		case WEP:
		case UNKNOWN:
		case WPA1:
		case WPA2:
			scan_results_pop_up_password_layout.setVisibility(View.VISIBLE);
		default:
			break;
		}
	}

	@UiThread
	void scan() {
		scan_results_pop_up_loader.setVisibility(View.VISIBLE);
		mLogger.info("*AP* Fetching scan results from SL device, for a list of available access points to choose from to send to SL device as profile to connect to");
		new RetrieveSSIDsFromDevice().execute("Sion");
	}

	@UiThread
	void showToastWithMessage(final String msg) {
		Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();

		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
			}
		}, 2000);
	}

	@Click
	void scan_results_pop_up_buttons_rescan_button() {

		if (deviceConfiguration != null) {
			scan_results_pop_up_list.setAdapter(null);

			if (scanResultList == null)
				scanResultList = new ArrayList<String>();

			scanResultList.clear();
			scan_results_pop_up_loader.setVisibility(View.VISIBLE);
			mLogger.info("*AP* Starting a rescan process for available access points from SL device (for sending to SL device in profile)");
			new RescanNetworksOnDevice().execute("Sion");
			/*
			try {
				Boolean success = new RescanNetworksOnDevice().execute("Sion").get();
				if (success == false) {
					showToastWithMessage(Constants.DEVICE_LIST_FAILED_TO_RESCAN);
					scan_results_pop_up_loader.setVisibility(View.GONE);
				}
				else {
					if (prefs.enableDebugMode().get())
						print("Rescan was ok, waiting before fetching networks..");

					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							scan();
						}
					}, Constants.DELAY_AFTER_RESCAN_NETWORKS_BEFORE_FETCHING_NETWORKS);
				}
			} catch (Exception e) {
				scan_results_pop_up_loader.setVisibility(View.GONE);
				e.printStackTrace();
			}
			*/
		}
		else {
			mLogger.info("*SC* Starting a rescan process for available access points from wifiManager (for sending to SL device in profile)");
			mContext.registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			startWifiScan();
		}
	}

	@Click
	void scan_results_pop_up_buttons_ok_button() {

		if (selectedItem == -1 && (manualSsid == null || manualSsid.equals(""))) {
			print("Can't continue, data is missing");
			mLogger.info("Can not continue with SSID chosen by user from list of available access points - Data is missing");
			scan_results_pop_up_buttons_ok_button.setImageResource(R.drawable.new_graphics_ok_off);
			return;
		}

		String ssid;
		if (selectedItem != -1) {
			ssid = scanResultList.get(selectedItem);
		}
		else {
			ssid = manualSsid;
		}

		String password = scan_results_pop_up_pas_editText.getText().toString();
		String priorityString = "0";

		if (ssid.equals("")) {
			showToastWithMessage(Constants.DEVICE_LIST_MUST_SUPPLY_SSID);
			return;
		}

		if (securityType == SecurityType.WPA1 || securityType == SecurityType.WPA2 || securityType == SecurityType.WEP) {
			if (password.equals("")) {
				showToastWithMessage(Constants.DEVICE_LIST_MUST_SUPPLY_PASSWORD);
				return;
			}
		}

		print("SSID: " + ssid + "\nPASSWORD: " + password + "\nSECURITY: " + securityType);
		mLogger.info("Chosen SSID from list to send to SL device in profile to connect to: " + "\nSSID: " + ssid + "\nPASS LEN: " + password.length() + "\nSECURITY: " + securityType);
		Log.i(TAG,"Chosen SSID from list to send to SL device in profile to connect to: " + "\nSSID: " + ssid + "\nPASS LEN: " + password.length() + "\nSECURITY: " + securityType);

		if (deviceConfiguration != null) {
			buttonClickBool=true;
			deviceConfiguration.ssidToAdd = ssid;
			deviceConfiguration.ssidToAddPriority = priorityString;
			deviceConfiguration.ssidToAddSecurityKey = password;
			deviceConfiguration.ssidToAddSecurityType = securityType;
			deviceConfiguration.closeDialog();
		}
		else {
			buttonClickBool=true;
			newSmartConfigFragment.ssidToAdd = ssid;
			newSmartConfigFragment.ssidToAddPriority = priorityString;
			newSmartConfigFragment.ssidToAddSecurityKey = password;
			newSmartConfigFragment.ssidToAddSecurityType = securityType;
			newSmartConfigFragment.closeDialog();
		}
	}

	private class ScanResultListAdapter extends BaseAdapter {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null){
				LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
				convertView = inflater.inflate(R.layout.scan_result_list_item, parent, false);
			}

			TextView cellLabel = (TextView)convertView.findViewById(R.id.scan_result_item_label);
			ImageView selectedImage = (ImageView)convertView.findViewById(R.id.scan_result_item_image_view);
			ImageView wifiImage = (ImageView)convertView.findViewById(R.id.scan_result_wifi_imageView);
			EditText manualName = (EditText)convertView.findViewById(R.id.scan_result_wifi_editText);

			if (selectedItem == position)
				selectedImage.setVisibility(View.VISIBLE);
			else 
				selectedImage.setVisibility(View.INVISIBLE);

			int neededPosition = showAll ? scanResultList.size() + 1 : scanResultList.size(); 
			if (position == neededPosition) {
				selectedImage.setVisibility(View.INVISIBLE);
				wifiImage.setVisibility(View.INVISIBLE);
				manualName.setVisibility(View.INVISIBLE);

				if (showAll)
					cellLabel.setText(getResources().getString(R.string.scan_results_pop_up_view_minimize_wifi_networks_list));
				else
					cellLabel.setText(getResources().getString(R.string.scan_results_pop_up_view_show_all));
			}

			if (showAll) {
				if (position == scanResultList.size()) {

					wifiImage.setVisibility(View.VISIBLE);
					wifiImage.setImageResource(R.drawable.new_graphics_wifi_0);
					manualName.setVisibility(View.VISIBLE);
					manualName.addTextChangedListener(mTextWatcher);
					manualName.setText(manualSsid);
					manualName.setSelection(manualName.getText().length());
					
					cellLabel.setText("");

					manualName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						@Override
						public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
							if (actionId == EditorInfo.IME_ACTION_DONE) {
					        	checkForPasswordLayout(manualSsid);
					            return false;
					        }
							return false;
						}
					});
					
					manualName.setOnFocusChangeListener(new OnFocusChangeListener() {
						@Override
						public void onFocusChange(View arg0, boolean arg1) {

//							if (arg1) {
//								EditText e = (EditText) arg0;
//								e.setSelection(e.getText().length());
//							}
							
							if (arg1 && selectedItem != -1) {
								selectedItem = -1;
								notifyDataSetChanged();
							}
							else if (selectedItem == -1) {
								if (manualSsid == null || manualSsid.equals("")) {
									scan_results_pop_up_buttons_ok_button.setImageResource(R.drawable.new_graphics_ok_off);
								}
								else {
									scan_results_pop_up_buttons_ok_button.setImageResource(R.drawable.new_graphics_ok_on);
									checkForPasswordLayout(manualSsid);
								}
							}
						}
					});
				}
				else if (position < scanResultList.size()){
					String ssid = scanResultList.get(position);

					wifiImage.setVisibility(View.VISIBLE);
					manualName.setVisibility(View.INVISIBLE);
					cellLabel.setText(ssid);


					SecurityType sec = SecurityType.UNKNOWN;
					if (mSecurityMapping != null) {
						sec = mSecurityMapping.get(ssid);
						
						if (sec == null)
							sec = checkSSIDSecurityType(ssid);
					} else {
						sec = checkSSIDSecurityType(ssid);
					}


					if (mRSSIMap != null && mRSSIMap.containsKey(ssid)) {
							wifiImage.setImageResource(determineImageForScanResult(sec, mRSSIMap.get(ssid)));
					} else {

						switch (sec) {
							case OPEN:
								wifiImage.setImageResource(R.drawable.new_graphics_wifi_4);
								break;
							case WEP:
							case WPA1:
							case WPA2:
								wifiImage.setImageResource(R.drawable.new_graphics_secure_wifi_4);
								break;
							case UNKNOWN:
								wifiImage.setImageResource(R.drawable.new_graphics_wifi_q_red);
								break;
						}
				    }
				}
			}
			else {
				if (position < scanResultList.size()) {
					String ssid = scanResultList.get(position);

					manualName.setVisibility(View.INVISIBLE);
					cellLabel.setText(ssid);

					SecurityType sec = checkSSIDSecurityType(ssid);

					if (mRSSIMap != null && mRSSIMap.containsKey(ssid)) {
						wifiImage.setImageResource(determineImageForScanResult(sec, mRSSIMap.get(ssid)));
					} else {

						switch (sec) {
							case OPEN:
								wifiImage.setImageResource(R.drawable.new_graphics_wifi_4);
								break;
							case WEP:
							case WPA1:
							case WPA2:
								wifiImage.setImageResource(R.drawable.new_graphics_secure_wifi_4);
								break;
							case UNKNOWN:
								wifiImage.setImageResource(R.drawable.new_graphics_wifi_q_red);
								break;
						}
					}
				}
			}

			return convertView;
		}

        /**
         * The determineImageForScanResult method matches the correct image to reflect the RSSI level and security type of an AP
         * displayed in the list of available APs obtained from the WifiManager
		 */
		protected int determineImageForScanResult(SecurityType sec,int rssi) {

			int imageInt = R.drawable.new_graphics_wifi_4;
			if (sec.equals(SecurityType.UNKNOWN)) {
				imageInt = R.drawable.new_graphics_wifi_q_red;
			} else {
				int secNum = 0;
				if (!sec.equals(SecurityType.OPEN)) {
					secNum = 1;
				}
				switch (rssi) {
					case SmartConfigConstants.RSSI_LEVEL_HIGH:
						if (secNum == 0) {
							imageInt = R.drawable.new_graphics_wifi_4;
						} else {
							imageInt = R.drawable.new_graphics_secure_wifi_4;
						}
						break;
					case SmartConfigConstants.RSSI_LEVEL_MID_HIGH:
						if (secNum == 0) {
							imageInt = R.drawable.new_graphics_wifi_3;
						} else {
							imageInt = R.drawable.new_graphics_secure_wifi_3;
						}
						break;
					case SmartConfigConstants.RSSI_LEVEL_MID_LOW:
						if (secNum == 0) {
							imageInt = R.drawable.new_graphics_wifi_2;
						} else {
							imageInt = R.drawable.new_graphics_secure_wifi_2;
						}
						break;
					case SmartConfigConstants.RSSI_LEVEL_LOW:
						if (secNum == 0) {
							imageInt = R.drawable.new_graphics_wifi_1;
						} else {
							imageInt = R.drawable.new_graphics_secure_wifi_1;
						}
						break;
				}

			}

			return imageInt;

		}



		@Override
		public int getCount() {

			if (showAll) {
				return scanResultList.size() + 2;
			}

			return scanResultList.size() + 1;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}
	}

	@UiThread
	void print(String msg) {
		System.out.println(msg);
	}

	/**
	 * Getting scan results from the simple link 
	 */
	class RetrieveSSIDsFromDevice extends AsyncTask<String, Void, Boolean> {
		//Wifi scan results
		@Override
		protected void onPostExecute(Boolean result) {
			
			if (result) {
				adapter = new ScanResultListAdapter();
				scan_results_pop_up_list.setAdapter(adapter);
				scan_results_pop_up_loader.setVisibility(View.GONE);
			}
			else {
				showToastWithMessage(Constants.DEVICE_LIST_FAILED_TO_GET_RESULTS);
				scan_results_pop_up_loader.setVisibility(View.GONE);
			}
			
			super.onPostExecute(result);
		}
		
		@Override
		protected Boolean doInBackground(String... params) {

			Boolean success = false;

			try {
				
				if (deviceVersion == DeviceVersion.UNKNOWN) {
					mLogger.info("*AP* Starting to retrieve list of access points SSIDs from SL device, for a list of available access points to choose from to send to SL device as profile to connect to");
					deviceVersion = NetworkUtil.getSLVersion(Constants.BASE_URL_NO_HTTP);
					mLogger.info("*AP* Got SL device version with value: " + deviceVersion);
				}
				
				ArrayList<String> listWithSecurityAndRssi = NetworkUtil.getSSIDListFromDevice(Constants.BASE_URL_NO_HTTP, deviceVersion);
				mLogger.info("*AP* Got list of networks: " + listWithSecurityAndRssi);
				
				if (listWithSecurityAndRssi == null) {
					mLogger.error("*AP* List was null!");
					return false;
				}
				
				listWithSecurityAndRssi = removeMultipleSSIDs(listWithSecurityAndRssi);

				mLogger.info("*AP* duplicate SSIDs removed list of networks: " + listWithSecurityAndRssi);

				mSecurityMapping = new HashMap<String, SecurityType>();
				mFullList = new ArrayList<String>();
				
				for (String network : listWithSecurityAndRssi) {
					String securityTypeString = network.substring(0, 1);
					String ssid = network.substring(1);
					
					SecurityType securityType = SecurityType.parseString(securityTypeString);
					Log.e(TAG,"*** SSID : "+ssid +"Security type : "+securityTypeString + "***");

					if (securityType != SecurityType.UNKNOWN)
						mFullList.add(ssid);
					
					mSecurityMapping.put(ssid, securityType);
				}
				
//need to test
				if (deviceConfiguration.savedSSID != null && !deviceConfiguration.savedSSID.contains(Constants.DEVICE_PREFIX) ) {
					if (mFullList.contains(deviceConfiguration.savedSSID)) {
						int index = mFullList.indexOf(deviceConfiguration.savedSSID);
						if (index != -1) {
							String ssidName = mFullList.get(index);
							mFullList.remove(index);
							mFullList.add(0, ssidName);
						}
					}
					else {
						mFullList.add(0, deviceConfiguration.savedSSID);
					}
				}



				if (deviceConfiguration.savedSSID == null || deviceConfiguration.savedSSID.contains(Constants.DEVICE_PREFIX)) {
					showAll = true;
					scanResultList = new ArrayList<String>(mFullList);
				}
				else {
					scanResultList = new ArrayList<String>();
					if (!showAll) {
						if (mFullList.size() > 0)
							scanResultList.add(mFullList.get(0));
					}
					else {
						scanResultList.addAll(mFullList);
					}
				}

				mLogger.info("scan result list: " + scanResultList);

				success = true;
				
			} catch (Exception e) {
				e.printStackTrace();
				mLogger.error("*AP* Failed to create the list");
			}
			
			return success;
		}

		/**
		 * The removeMultipleSSIDs method is used to remove multiple appearances of identical SSIDs from
		 * the list of APs obtained from the device, and displayed on the AP mode configuration page as WiFi networks.
		 * This is due to the possible presence of several APs possessing the same SSID but different BSSIDs, and thus causing
		 * the same AP to appear several times on the list.
		 */
		public ArrayList<String> removeMultipleSSIDs(ArrayList<String> multiList) {

			ArrayList<String> newList = new ArrayList<>();
			for (String ap : multiList) {
				if (!newList.contains(ap)) {
					newList.add(ap);
				}
			}
			return newList;

		}

	}

	private SecurityType checkSSIDSecurityType(String ssid) {
		SecurityType newSecurityType = SecurityType.UNKNOWN;

		if (deviceConfiguration != null) {
			for (ScanResult result : deviceConfiguration.wifiList) {
				if (result.SSID.equalsIgnoreCase(ssid)) {
					newSecurityType = NetworkUtil.getScanResultSecurity(result);
					break;
				}
			}
		}
		else {
			for (ScanResult result : wifiList) {
				if (result.SSID.equalsIgnoreCase(ssid)) {
					newSecurityType = NetworkUtil.getScanResultSecurity(result);
					break;
				}
			}
		}

		return newSecurityType;
	}

	class RescanNetworksOnDevice extends AsyncTask<String, Void, Boolean> {
		
		@Override
		protected void onPostExecute(Boolean result) {
			
			if (!result) {
				showToastWithMessage(Constants.DEVICE_LIST_FAILED_TO_RESCAN);
				scan_results_pop_up_loader.setVisibility(View.GONE);
			}
			else {
				final Handler handler = new Handler();

				mLogger.info("*AP* Rescan completed successfully, now we wait " + Constants.DELAY_AFTER_RESCAN_NETWORKS_BEFORE_FETCHING_NETWORKS + " mSec before fetching results from SL device");
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						scan();
					}
				}, Constants.DELAY_AFTER_RESCAN_NETWORKS_BEFORE_FETCHING_NETWORKS);
			}
			
			super.onPostExecute(result);
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
//			String url = Constants.BASE_URL;
			String url = Constants.BASE_URL_NO_HTTP;
			Boolean success = false;

			if (deviceVersion == DeviceVersion.UNKNOWN) {
				mLogger.info("*AP* Starting to retrieve list of SSIDs from SL device - Rescan");
				try {
					deviceVersion = NetworkUtil.getSLVersion(Constants.BASE_URL);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				mLogger.info("*AP* Got SL device version with value: " + deviceVersion);
			}
			
			mLogger.info("*AP* Rescanning for list of available access points from SL device with url: " + url + " SL device version: " + deviceVersion);
			try {
				if (NetworkUtil.rescanNetworksOnDevice(url, deviceVersion)) {
                    success = true;
                }
			} catch (CertificateException e) {
				e.printStackTrace();
			}

			return success;
		}
	}

	BroadcastReceiver receiverWifi = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (scanningnWifi) {
				scanningnWifi = false;
				List<ScanResult> fullList = NetworkUtil.getWifiScanResults(true, mContext);

				wifiList = new ArrayList<ScanResult>();
				for (ScanResult scanResult : fullList) {
					if (!scanResult.SSID.contains(Constants.DEVICE_PREFIX) && scanResult.frequency < 2500) {

						if ( deviceConfiguration == null && scanResult.SSID.equals(startingSSID))
//							ConnectivityManager myConnManager = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
//							WifiInfo wifiInfo = wifiManger.getConnectionInfo();
//							int mbps = wifiInfo.getLinkSpeed();
							wifiList.add(0, scanResult);
						else
							wifiList.add(scanResult);
					}

				}

				ArrayList<String> temp = new ArrayList<>();
				for (ScanResult result : wifiList) {
					temp.add(result.SSID + " + " + result.level);
				}
				mLogger.info("*SC* wifiList scanResults from wifiManager (SSID + RSSI level): " + temp);


				mRSSIMap = new HashMap<>();
				for (ScanResult result : wifiList) {
					int RSSI = WifiManager.calculateSignalLevel(result.level,4);
					mRSSIMap.put(result.SSID,RSSI);
				}

				mFullList = new ArrayList<String>();
				for (ScanResult result : wifiList) {
					mFullList.add(result.SSID);
				}

				scanResultList = new ArrayList<String>();
				if (!showAll) {
					if (mFullList.size() > 0 && startingSSID != null)
						scanResultList.add(mFullList.get(0));
				}
				else {
					scanResultList.addAll(mFullList);
				}

				adapter = new ScanResultListAdapter();
				scan_results_pop_up_list.setAdapter(adapter);
				scan_results_pop_up_loader.setVisibility(View.GONE);
			}
		}
	};

	@UiThread
	void startWifiScan() {
		scan_results_pop_up_buttons_ok_button.setImageResource(R.drawable.new_graphics_ok_off);
		scan_results_pop_up_loader.setVisibility(View.VISIBLE);
		selectedItem = -1;
		scan_results_pop_up_list.setAdapter(null);

		startingSSID = NetworkUtil.getConnectedSSID(mContext);
		print("Starting ssid: " + startingSSID);
		mLogger.info("*SC* Starting ssid: \"" + startingSSID + "\"");

		print("Searching for networks");
		mLogger.info("*SC* WifiManager Searching for networks to choose from, for sending to SL device as profile to connect to");
		scanningnWifi = true;
		NetworkUtil.startScan(mContext);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if(deviceConfiguration !=null){
			{
				//mContext.unregisterReceiver(receiverWifi);
				ConnectivityManager myConnManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				String ssid = NetworkUtil.getConnectedSSID(getContext());
				if (myNetworkInfo.isConnected() && ssid != null && !(ssid.equals(""))) {

					if(!buttonClickBool) {
//						deviceConfiguration.tab_device_configuration_router_device_pick_label.setText(ssid);
//						deviceConfiguration.tab_device_configuration_router_device_pick_label.setTextColor(Color.BLACK);
					}
				}
				if (scan_results_pop_up_pas_editText.getText() != null && !scan_results_pop_up_pas_editText.getText().toString().equals("")) {
//					deviceConfiguration.tab_device_configuration_password_check_layout.setVisibility(GONE);
					deviceConfiguration.mIsReady = true;
					deviceConfiguration.setToReady(deviceConfiguration.mIsReady);
					deviceConfiguration.tab_device_configuration_start_button.setEnabled(true);

				}


				if(deviceConfiguration.ssidToAddSecurityType==SecurityType.OPEN){
					deviceConfiguration.mIsReady = true;
					deviceConfiguration.tab_device_configuration_password_check_layout.setVisibility(GONE);
					deviceConfiguration.setToReady(deviceConfiguration.mIsReady);
					deviceConfiguration.tab_device_configuration_start_button.setEnabled(true);
				}
				if(deviceConfiguration.ssidToAddSecurityType==SecurityType.WPA2 ||deviceConfiguration.ssidToAddSecurityType==SecurityType.WPA1 ||deviceConfiguration.ssidToAddSecurityType==SecurityType.WEP){
					if (scan_results_pop_up_pas_editText.getText() != null) {
						if (!scan_results_pop_up_pas_editText.getText().toString().equals("")) {
							deviceConfiguration.mIsReady = true;
							deviceConfiguration.setToReady(deviceConfiguration.mIsReady);
							deviceConfiguration.tab_device_configuration_start_button.setEnabled(true);
						}
					}

				}
			}
		}

		if (newSmartConfigFragment != null) {
			mContext.unregisterReceiver(receiverWifi);
			ConnectivityManager myConnManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			String ssid = NetworkUtil.getConnectedSSID(getContext());
			
        	if (myNetworkInfo.isConnected() && ssid != null && !(ssid.equals(""))) {

				if(!buttonClickBool) {
					newSmartConfigFragment.tab_device_configuration_network_to_configure_network_pick_label.setText(ssid);
					newSmartConfigFragment.tab_device_configuration_network_to_configure_network_pick_label.setTextColor(Color.BLACK);
				}
			}
			if (scan_results_pop_up_pas_editText.getText() != null && !scan_results_pop_up_pas_editText.getText().toString().equals("")) {
				newSmartConfigFragment.tab_device_sc_configuration_password_check_layout.setVisibility(GONE);
				newSmartConfigFragment.mIsReady = true;
				newSmartConfigFragment.setToReady(newSmartConfigFragment.mIsReady);
				newSmartConfigFragment.tab_device_configuration_sc_start_button.setEnabled(true);

			}


			if(newSmartConfigFragment.ssidToAddSecurityType==SecurityType.OPEN){
				newSmartConfigFragment.mIsReady = true;
				newSmartConfigFragment.tab_device_sc_configuration_password_check_layout.setVisibility(GONE);
				newSmartConfigFragment.setToReady(newSmartConfigFragment.mIsReady);
				newSmartConfigFragment.tab_device_configuration_sc_start_button.setEnabled(true);
			}
			if(newSmartConfigFragment.ssidToAddSecurityType==SecurityType.WPA2 ||newSmartConfigFragment.ssidToAddSecurityType==SecurityType.WPA1 ||newSmartConfigFragment.ssidToAddSecurityType==SecurityType.WEP){
				if(!scan_results_pop_up_pas_editText.getText().toString().equals("")){
					newSmartConfigFragment.mIsReady = true;
					newSmartConfigFragment.setToReady(newSmartConfigFragment.mIsReady);
					newSmartConfigFragment.tab_device_configuration_sc_start_button.setEnabled(true);
				}

			}
		}
	}





}
