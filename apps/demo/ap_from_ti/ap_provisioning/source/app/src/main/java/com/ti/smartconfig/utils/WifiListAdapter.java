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

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import com.ti.smartconfig.R;

@EBean
public class WifiListAdapter extends BaseAdapter {
	public List<String> wifis;
	public Boolean useWifiIcon = false;
	public int mSelectedItem = -1;
	
	@RootContext
	Context context;
	
	@AfterInject
	void initAdapter() {
		wifis = new ArrayList<String>();
	}
	
	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		WifiItemView wifiItemView;
		if (convertView == null) {
			wifiItemView = WifiItemView_.build(context);
		} 
		else {
			wifiItemView = (WifiItemView)convertView;
		}
		
		wifiItemView.bind(getItem(position));
		
		if (useWifiIcon) {
			wifiItemView.wifi_list_item_image.setImageDrawable(context.getResources().getDrawable(R.drawable.wifi_icon));
		}
		else {
			wifiItemView.wifi_list_item_image.setImageDrawable(context.getResources().getDrawable(R.drawable.device_icon));
		}
		
		if (mSelectedItem == position) {
			wifiItemView.setBackgroundColor(Color.GRAY);
		}
		else {
			wifiItemView.setBackgroundColor(Color.WHITE);
		}
		
		return wifiItemView;
	}
	
	@Override
	public int getCount() {
		return wifis.size();
	}

	@Override
	public String getItem(int position) {
		return wifis.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}
