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
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import com.ti.smartconfig.utils.SharedPreferencesInterface_;
import com.ti.smartconfig.utils.Constants;
import com.ti.smartconfig.utils.Popup.PopupType;
import com.integrity_project.smartconfiglib.SmartConfigVersion;

@EFragment(R.layout.tab_settings_view)
public class SettingsFragment extends Fragment implements OnCheckedChangeListener {

	private static final String TAG = "SettingsFragment";

	public Vibrator v;
	boolean pageCheck = false;
	@Pref
	SharedPreferencesInterface_ prefs;

	@ViewById
	ToggleButton settings_show_name_toggle;
	//Switch settings_show_name_toggle;

	@ViewById
	ToggleButton settings_open_devices_tab_toggle;
	//Switch settings_open_devices_tab_toggle;

	@ViewById
	ToggleButton settings_iot_uuid_toggle;

	@ViewById
	ToggleButton settings_show_password_toggle;
	//Switch settings_show_password_toggle;

	@ViewById
	TextView settings_privacy_button;

	@ViewById
	ToggleButton settings_auto_connect_toggle;
	//Switch settings_auto_connect_toggle;

	@ViewById
	ToggleButton settings_ap_sc_toggle;
	//Switch settings_ap_sc_toggle;

	@ViewById
	TextView settings_build_text;

	@ViewById
	EditText settings_sc_transmit_timing_edit_text;
	@ViewById
	TextView settings_terms_button;

	@AfterViews
	void afterViews() {
		// Get instance of Vibrator from current Context

		v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

		// initialize switches

		if (prefs.showDeviceName().get())
			settings_show_name_toggle.setChecked(true);
		else
			settings_show_name_toggle.setChecked(false);
		if (prefs.openInDevicesList().get())
			settings_open_devices_tab_toggle.setChecked(true);
		else
			settings_open_devices_tab_toggle.setChecked(false);
		if (prefs.showSmartConfigPass().get())
			settings_show_password_toggle.setChecked(true);
		else
			settings_show_password_toggle.setChecked(false);

		if (prefs.autoConnectToSL().get())
			settings_auto_connect_toggle.setChecked(true);
		else
			settings_auto_connect_toggle.setChecked(false);

		if (prefs.enableSmartConfig().get())
			settings_ap_sc_toggle.setChecked(true);
		else
			settings_ap_sc_toggle.setChecked(false);

		if (prefs.isIoTuuid().get())
			settings_iot_uuid_toggle.setChecked(true);
		else
			settings_iot_uuid_toggle.setChecked(false);

		settings_sc_transmit_timing_edit_text.setText(String.valueOf(prefs.smartConfigTransmitTime().get()));
		settings_sc_transmit_timing_edit_text.addTextChangedListener(mTextWatcher);

		// initialize listeners
		settings_show_name_toggle.setOnCheckedChangeListener(this);
		settings_open_devices_tab_toggle.setOnCheckedChangeListener(this);
		settings_show_password_toggle.setOnCheckedChangeListener(this);
		settings_auto_connect_toggle.setOnCheckedChangeListener(this);
		settings_ap_sc_toggle.setOnCheckedChangeListener(this);
        settings_iot_uuid_toggle.setOnCheckedChangeListener(this);
		SmartConfigVersion  smartConfigVer = new SmartConfigVersion();
		try {
			PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
			settings_build_text.setText(String.format(getString(R.string.settings_build_text),pInfo.versionName,smartConfigVer.ver));
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
			case R.id.settings_show_name_toggle:
				v.vibrate(50);
				prefs.showDeviceName().put(settings_show_name_toggle.isChecked());
				break;
			case R.id.settings_open_devices_tab_toggle:
				v.vibrate(50);
				if (settings_open_devices_tab_toggle.isChecked())
					prefs.startTab().put(1);
				else
					prefs.startTab().put(0);
				prefs.openInDevicesList().put(settings_open_devices_tab_toggle.isChecked());
				break;
			case R.id.settings_show_password_toggle:
				v.vibrate(50);
				prefs.showSmartConfigPass().put(settings_show_password_toggle.isChecked());
				break;
			case R.id.settings_auto_connect_toggle:
				v.vibrate(50);
				prefs.autoConnectToSL().put(settings_auto_connect_toggle.isChecked());
				break;
			case R.id.settings_ap_sc_toggle:
				v.vibrate(50);
				prefs.enableSmartConfig().put(settings_ap_sc_toggle.isChecked());
//					((MainActivity) getActivity()).changeToAP(true, 0);
				//((MainActivity) getActivity()).changeToAP(true, 2);
                ((MainActivity) getActivity()).deviceTypeEnum = null;
                ((MainActivity) getActivity()).clearAllTabs();
                ((MainActivity) getActivity()).initTabs(0);

				break;
			case R.id.settings_iot_uuid_toggle:
				v.vibrate(50);
				prefs.isIoTuuid().put(settings_iot_uuid_toggle.isChecked());
				break;
		}
	}

	@Click
	void settings_privacy_button() {
		((MainActivity) getActivity()).startPrivacyPolicy();

	}


	@Click
	void settings_help_button() {
		((MainActivity) getActivity()).showHelpScreen();
	}

	private TextWatcher mTextWatcher = new TextWatcher() {
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
									  int after) {
		}

		@Override
		public void afterTextChanged(Editable ssidString) {
			String value = ssidString.toString();

			try {
				int intValue = Integer.parseInt(value);
				prefs.smartConfigTransmitTime().put(intValue);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}


		}
	};

	@Click
	void settings_auto_connect_help() {
		//showToastWithMessage("In case a single device is detected or when QR code exists, the application autonomously connects to a SimpleLink device.");
		((MainActivity) getActivity()).showSuccessDialog("In case a single device is detected the application autonomously connects to a SimpleLink device.", getString(R.string.pop_up_close), null, PopupType.Information, null, null);
	}

	@Click
	void settings_ap_sc_layout_help() {


		//showToastWithMessage("No SmartConfig algorithm is activated. Provisioning is performed directly over WiFi connection to the device, acting as a WiFi router.");
		((MainActivity) getActivity()).showSuccessDialog("Enable to activate SmartConfig algorithm.\nProvisioning is performed directly over WiFi connection to the device.", getString(R.string.pop_up_close), null, PopupType.Information, null, null);
	}

	@Click
	void settings_show_name_layout_help() {
		//showToastWithMessage("Device name entry is available on provisioning screen.");
		((MainActivity) getActivity()).showSuccessDialog("Device name entry is available on provisioning screen.", getString(R.string.pop_up_close), null, PopupType.Information, null, null);
	}

	@Click
	void settings_show_password_layout_help() {
		//showToastWithMessage("SmartConfig security Key entry is available on provisioning screen.");
		((MainActivity) getActivity()).showSuccessDialog("SmartConfig security Key entry is available on provisioning screen.", getString(R.string.pop_up_close), null, PopupType.Information, null, null);
	}

	@Click
	void settings_open_devices_tab_layout_help() {
		//showToastWithMessage("Application starts on Devices tab. Otherwise application starts on provisioning tab.");
		((MainActivity) getActivity()).showSuccessDialog("Application starts on Devices tab. Otherwise application starts on provisioning tab.", getString(R.string.pop_up_close), null, PopupType.Information, null, null);
	}


    @Click
    void settings_iot_uuid_layout_help(){
        ((MainActivity)getActivity()).showSuccessDialog("iotLink UUID entry is available on provisioning screen", getString(R.string.pop_up_close), null, PopupType.Information, null, null);
    }

    @Click
	void settings_terms_button() {
		//settings_terms_button.setEnabled(true);
		final ConnectivityManager conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.isConnected()) {
		//	if (!isAvailable())
			//	Toast.makeText(getActivity(), "check your internet connection first", Toast.LENGTH_LONG).show();
		//	else {
				((MainActivity) getActivity()).startTermsOfUse();
			}
			else{
			Toast.makeText(getActivity(), "check your internet connection first", Toast.LENGTH_LONG).show();
			}

	}
	@Click
	void settings_logs_button() {
		final Intent ei = new Intent(Intent.ACTION_SEND);
		ei.setType("vnd.android.cursor.dir/email");
		ei.putExtra(Intent.EXTRA_EMAIL, new String[]{"ecs-bugreport@list.ti.com"});
		ei.putExtra(Intent.EXTRA_SUBJECT, "Log for SmartConfig, version: " + settings_build_text.getText().toString());
		ei.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + Constants.LOG_PATH));
		startActivity(Intent.createChooser(ei, "Send email..."));
	}

	@UiThread
	void showToastWithMessage(final String msg) {
		Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();

		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
			}
		}, 2000);
	}
	//check if the connected wifi also got internet
	public Boolean isAvailable() {
		try {
			Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1    www.google.com");
			int returnVal = p1.waitFor();
			boolean reachable = (returnVal==0);
			if(reachable){
				System.out.println("Internet access");
				return reachable;
			}
			else{
				System.out.println("No Internet access");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();
	}


}

