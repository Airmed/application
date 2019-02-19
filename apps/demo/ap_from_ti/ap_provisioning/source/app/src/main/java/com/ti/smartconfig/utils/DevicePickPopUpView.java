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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import com.ti.smartconfig.R;
import com.ti.smartconfig.DeviceConfiguration;
import com.ti.smartconfig.NewSmartConfigFragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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

@EViewGroup(R.layout.device_pick_pop_up_view)
public class DevicePickPopUpView extends RelativeLayout {
	
	private Context mContext;
	private Boolean scanningnWifi = false;
	private Boolean showAll = false;
	public List<ScanResult> fullList;
	public List<ScanResult> wifiList;
	private int selectedItem = -1;
	private NewDeviceListAdapter adapter;
	private HashMap<String,Integer> mRSSIMap;
	
	public DeviceConfiguration deviceConfiguration;
	public NewSmartConfigFragment newSmartConfigFragment;
	
	@ViewById
	ListView pick_pop_up_list;
	@ViewById
	ImageView pick_pop_up_buttons_ok_button;
	@ViewById
	ImageView pick_pop_up_buttons_rescan_button;
	@ViewById
	ProgressBar pick_pop_up_loader;
	@ViewById
	RelativeLayout scan_results_pop_up_password_layout;
	@ViewById
	EditText scan_results_pop_up_pas_editText;
	@ViewById
	CheckBox scan_checkbox;
	public DevicePickPopUpView(Context context) {
		super(context);
		mContext = context;
	}
	
	BroadcastReceiver receiverWifi = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (scanningnWifi) {
				scanningnWifi = false;
				
				fullList = NetworkUtil.getWifiScanResults(true, mContext);

				mRSSIMap = new HashMap<>();
				for (ScanResult result : fullList) {
					int RSSI = WifiManager.calculateSignalLevel(result.level,4);
					mRSSIMap.put(result.SSID,RSSI);
				}

				if (deviceConfiguration != null)
					deviceConfiguration.wifiList = fullList;
				
				if (showAll) {
					wifiList = fullList;
				}
				else {
					filterList();
				}
				adapter = new NewDeviceListAdapter();
				pick_pop_up_list.setAdapter(adapter);
				pick_pop_up_loader.setVisibility(View.INVISIBLE);
			}
			else{
				adapter = new NewDeviceListAdapter();
				pick_pop_up_list.setAdapter(adapter);
				pick_pop_up_loader.setVisibility(View.INVISIBLE);
			}
		}
	};
	
	private void filterList() {
		wifiList = new ArrayList<ScanResult>();
		for (ScanResult scanResult : fullList) {
			if (scanResult.SSID.contains(Constants.DEVICE_PREFIX))
				wifiList.add(scanResult);
		}
	}
	
	@UiThread
	void print(String msg) {
		System.out.println(msg);
	}
	
	@UiThread
	void checkForPasswordLayout(String ssid) {
		SecurityType securityType = checkSSIDSecurityType(ssid);
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
	
	private SecurityType checkSSIDSecurityType(String ssid) {
		SecurityType newSecurityType = SecurityType.UNKNOWN;
		
		for (ScanResult result : wifiList) {
			if (result.SSID.equalsIgnoreCase(ssid)) {
				newSecurityType = NetworkUtil.getScanResultSecurity(result);
				break;
			}
		}
		Log.i("DevicePickPopUpView","Security type: " + newSecurityType);
		return newSecurityType;
	}
	
	@AfterViews
	void afterViews() {
		startWifiScan();
		scan_results_pop_up_password_layout.setVisibility(View.GONE);
		scan_results_pop_up_pas_editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		pick_pop_up_loader.setVisibility(View.INVISIBLE);
		pick_pop_up_list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if (arg2 == wifiList.size()) {
					showAll = !showAll;
					if (showAll)
						wifiList = fullList;
					else 
						filterList();
					
					scan_results_pop_up_password_layout.setVisibility(View.GONE);
					
					pick_pop_up_buttons_ok_button.setImageResource(R.drawable.new_graphics_ok_off);
					selectedItem = -1;
				}
				else {
					pick_pop_up_buttons_ok_button.setImageResource(R.drawable.new_graphics_ok_on);
					selectedItem = arg2;
					
					ScanResult result = wifiList.get(arg2);
					checkForPasswordLayout(result.SSID);
				}
				
				adapter.notifyDataSetChanged();
			}
		});
		
		mContext.registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		/*if (wifiList != null) {
			fullList = wifiList;
			filterList();
		}
		else{
			startWifiScan();
		}*/
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
		if (wifiList != null) {
			fullList = wifiList;
			filterList();
			
			adapter = new NewDeviceListAdapter();
			pick_pop_up_list.setAdapter(adapter);
		}
		else{
			startWifiScan();
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
	    super.onDetachedFromWindow();
	    mContext.unregisterReceiver(receiverWifi);
	}
	
	@Click
	void pick_pop_up_buttons_ok_button() {
		if (selectedItem == -1)
			return;
		
		ScanResult result = wifiList.get(selectedItem);
		SecurityType securityType = checkSSIDSecurityType(result.SSID);
		String password = scan_results_pop_up_pas_editText.getText().toString();
		
		if (securityType == SecurityType.WPA1 || securityType == SecurityType.WPA2 || securityType == SecurityType.WEP) {
			if (password.equals("")) {
				showToastWithMessage(Constants.DEVICE_LIST_MUST_SUPPLY_PASSWORD);
				return;
			}
		}
		
		if (deviceConfiguration != null) {
			deviceConfiguration.deviceWasChosen(result, securityType, password);
			Log.i("DevicePickPopUpView", "device was chosen / AP Prov.: " + "\nSSID: " + result + "\nSecurity type: " + securityType + "\nPassword: " + password);
		}
		if (newSmartConfigFragment != null) {
			newSmartConfigFragment.deviceWasChosen(result, securityType, password);
			Log.i("DevicePickPopUpView", "device was chosen / SC Prov.: " + "\nSSID: " + result + "\nSecurity type: " + securityType + "\nPassword: " + password);
		}
	}
	
	@UiThread
	void showToastWithMessage(String msg) {
		Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
	}
	
	@Click
	void pick_pop_up_buttons_rescan_button() {
		startWifiScan();
	}
	
	@UiThread
	void startWifiScan() {
		pick_pop_up_buttons_ok_button.setImageResource(R.drawable.new_graphics_ok_off);
		pick_pop_up_loader.setVisibility(View.VISIBLE);
		selectedItem = -1;
		pick_pop_up_list.setAdapter(null);
		print("Searching for devices Do not abort!");
		scanningnWifi = true;
		NetworkUtil.startScan(mContext);
	}
	
	private class NewDeviceListAdapter extends BaseAdapter {
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			/*if(convertView == null){
				LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
				convertView = inflater.inflate(R.layout.list_new_device_item, parent, false);
			}
			
			TextView cellLabel = (TextView)convertView.findViewById(R.id.new_device_item_label);
			ImageView selectedImage = (ImageView)convertView.findViewById(R.id.new_device_item_image_view);
			
			if (position == wifiList.size()) {
				selectedImage.setVisibility(View.INVISIBLE);
				if (showAll)
					cellLabel.setText("Show only devices");
				else
					cellLabel.setText("Show all");
			}
			else {
				ScanResult scanResult = wifiList.get(position);
				cellLabel.setText(scanResult.SSID);
				if (selectedItem == position)
					selectedImage.setVisibility(View.VISIBLE);
				else 
					selectedImage.setVisibility(View.INVISIBLE);
			}*/
			
			if(convertView == null){
				LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
				convertView = inflater.inflate(R.layout.scan_result_list_item, parent, false);
			}
			
			TextView cellLabel = (TextView)convertView.findViewById(R.id.scan_result_item_label);
			ImageView selectedImage = (ImageView)convertView.findViewById(R.id.scan_result_item_image_view);
			ImageView wifiImage = (ImageView)convertView.findViewById(R.id.scan_result_wifi_imageView);
			EditText manualName = (EditText)convertView.findViewById(R.id.scan_result_wifi_editText);
			manualName.setVisibility(View.GONE);
			
			if (position == wifiList.size()) {
				selectedImage.setVisibility(View.INVISIBLE);
				wifiImage.setVisibility(View.INVISIBLE);
				if (showAll)
					cellLabel.setText(getResources().getString(R.string.device_pick_pop_up_view_show_only_devices));
				else
					cellLabel.setText(getResources().getString(R.string.device_pick_pop_up_view_show_all));
			}
			else {
				wifiImage.setVisibility(View.VISIBLE);
				ScanResult scanResult = wifiList.get(position);
				cellLabel.setText(scanResult.SSID);
				if (selectedItem == position)
					selectedImage.setVisibility(View.VISIBLE);
				else 
					selectedImage.setVisibility(View.INVISIBLE);
				

				SecurityType sec = checkSSIDSecurityType(scanResult.SSID);
				String ssid = scanResult.SSID;


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
			
			if (deviceConfiguration != null)
				return wifiList.size() + 1;
			
			return wifiList.size();
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
}
