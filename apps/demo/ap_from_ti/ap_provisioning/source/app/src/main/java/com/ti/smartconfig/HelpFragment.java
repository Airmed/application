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

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import com.ti.smartconfig.utils.SharedPreferencesInterface_;
import com.ti.smartconfig.utils.HelpViewPagerAdapter;

import android.annotation.SuppressLint;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

@EFragment(R.layout.help_fragment)
public class HelpFragment extends Fragment {

	@ViewById
	RelativeLayout help_fragment_background_layout;
	
	@ViewById(R.id.pager)
	ViewPager mPager;
	
	@Pref
	SharedPreferencesInterface_ prefs;
	
	@ViewById
	ToggleButton toggleButton1;
	
	//@ViewById(R.id.indicator)
	//CirclePageIndicator mIndicator;
	
	@AfterViews
	void afterViews() {
		help_fragment_background_layout.setOnTouchListener(new OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				return true;
			}
		});
		
		mPager.setAdapter(new HelpViewPagerAdapter(getActivity().getSupportFragmentManager()));
		//mIndicator.setViewPager(mPager);
		
		if (prefs.doNotShowHelpAgain().get()) {
			toggleButton1.setChecked(true);
		}
		else {
			toggleButton1.setChecked(false);
		}
		FragmentTransaction mFragmentTransaction = getFragmentManager()
				.beginTransaction();
		mFragmentTransaction.addToBackStack(null);
	}
	@Override
	public  void onResume() {
		super.onResume();
		//we preventing from the user to use the back button on this fragment
		help_fragment_background_layout.setFocusableInTouchMode(true);
		help_fragment_background_layout.requestFocus();
		help_fragment_background_layout.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
					// handle back button's click listener
				//	Toast.makeText(getActivity(), "Back press", Toast.LENGTH_SHORT).show();

					return true;
				}
				return false;
			}
		});
	}

	@Click
	void toggleButton1() {
		prefs.doNotShowHelpAgain().put(toggleButton1.isChecked());
	}
	
	@Click
	void help_fragment_close_button() {
		mPager.setAdapter(null);
		
		FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
		transaction.setCustomAnimations(R.anim.fragment_fade_out, R.anim.fragment_fade_out);
		transaction.remove(this);
		transaction.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		transaction.commit();
	}
	//added 7.2
	public void onBackPressed() {
		// ignore the back button
	}

}
