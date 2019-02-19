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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EBean
public class RecentDeviceListAdapter extends BaseAdapter {
	
	private static final String TAG = "RecentDeviceListAdapter";
	private static final String HTML_PATTERN = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>";
	private Pattern pattern = Pattern.compile(HTML_PATTERN);
	List<Device> recentDevices;
	
	@Pref
	SharedPreferencesInterface_ prefs;
	
	@RootContext
	Context context;

	@AfterInject
	void initAdapter() {
		recentDevices = new ArrayList<Device>(); // initialize new list of devices
		try {
			JSONArray recentDevicesArray = new JSONArray(prefs.recentDevicesArray().get()); // get the JSON array of devices from the shared preferences
			JSONArray devicesArray = new JSONArray(prefs.devicesArray().get());

			for (int i = 0; i < recentDevicesArray.length(); i++) { // populate the list
				
				JSONObject recent = recentDevicesArray.getJSONObject(i);
				//31.1 ofir
				Boolean shouldAdd = true;
				for (int x = 0; x < devicesArray.length(); x++) {
					JSONObject device = devicesArray.getJSONObject(x);
					
					if (recent.getString("name").equals(device.getString("name")))
						shouldAdd = false;
				}
				
				if (shouldAdd && !hasHTMLTags(HTML_PATTERN,recent.getString("name"))) {
					recentDevices.add(new Device(recentDevicesArray.getJSONObject(i).getString("name"), recentDevicesArray.getJSONObject(i).getString("host")));
				}
				else {
					Log.e(TAG, "Wont add the recent device cause it is already in devices data");
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
//	public boolean hasHTMLTags(String text){
//		Matcher matcher = pattern.matcher(text);
//		return matcher.matches();
//	}
	private static boolean hasHTMLTags(String s, String pattern) {
		try {
			Pattern pat = Pattern.compile(pattern);
			Matcher matcher = pat.matcher(s);
			return matcher.matches();
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		initAdapter();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		DeviceItemView deviceItemView;
		if (convertView == null) {
			deviceItemView = DeviceItemView_.build(context);
		} else {
			deviceItemView = (DeviceItemView) convertView;
		}
		deviceItemView.bind(getItem(position));
		return deviceItemView;
	}
	
	@Override
	public int getCount() {
		return recentDevices.size();
	}

	@Override
	public Device getItem(int position) {
		return recentDevices.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

}
