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

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import com.ti.smartconfig.R;
import com.ti.smartconfig.utils.SharedPreferencesInterface_;

@EActivity(R.layout.activity_privacy_policy)
public class PrivacyPolicyActivity extends Activity  {

	private static final String TAG = "PrivacyPolicyActivity";

	@Pref
	SharedPreferencesInterface_ prefs;
	@ViewById
	TextView privacy_policy_ti_textView;
	@ViewById
	TextView privacy_policy_textView;
	@ViewById
	WebView privacy_policy_webView;
	@ViewById
	TextView privacy_policy_ti_textView_header;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


	}
	
	@AfterViews
	void afterViews() {

	}
	@Click
	//connecting to TI privacy policy page
		void privacy_policy_textView(){

			final ConnectivityManager conMgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
		//	if (activeNetwork == null && !activeNetwork.isConnected()) {
				//	if (!isAvailable())
				Toast.makeText(getApplicationContext(), "check your internet connection first", Toast.LENGTH_LONG).show();
		//	}
		//		else {
					gotoPrivacyPage();
					privacy_policy_ti_textView_header.setVisibility(View.INVISIBLE);
					privacy_policy_ti_textView.setVisibility(View.INVISIBLE);
					privacy_policy_textView.setVisibility(View.INVISIBLE);
				//}
		//	}
		}
		 void gotoPrivacyPage(){

		String url ="http://www.ti.com/privacy";

		WebSettings webSettings = privacy_policy_webView.getSettings();
		webSettings.setBuiltInZoomControls(true);
		privacy_policy_webView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});
		privacy_policy_webView.setWebViewClient(new Callback());
		privacy_policy_webView.loadUrl(url);

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


	private class Callback extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			return (false);
		}

	}

}
