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

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import com.ti.smartconfig.R;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
//Searching for device
@EViewGroup(R.layout.popup_layout)
public class Popup extends RelativeLayout {

	private Context mContext;
	private PopUpCallback mPopUpCallback;
	private PopupType mPopupType;
	
	@ViewById
	TextView popup_leftButton_text;
	
	@ViewById
	TextView popup_rightButton_text;
	
	@ViewById
	RelativeLayout popup_right_button_layout;
	
	@ViewById
	TextView popup_text;

	@ViewById
	TextView popup_text_headline;
	
	@ViewById
	ImageView popup_status_image;
	
	@ViewById
	LinearLayout popup_buttons_layout;
	
	public Popup(Context context) {
		super(context);
		mContext = context;
	}
	
	@AfterViews
	void afterViews() {
		
	}
	
	public void start(String text, String leftButtonText, String rightButtonText, PopupType popupType, PopUpCallback callBack, String headline) {
		
		if (text != null) {
			popup_text.setVisibility(View.VISIBLE);
			popup_text.setText(text);
		}
		else {
			popup_text.setVisibility(View.GONE);
			RelativeLayout.LayoutParams params = (LayoutParams)popup_text_headline.getLayoutParams();
			params.addRule(RelativeLayout.ABOVE, popup_buttons_layout.getId());
		}
		
		popup_leftButton_text.setText(leftButtonText);
		
		if (rightButtonText == null) {
			popup_right_button_layout.setVisibility(View.GONE);
		}
		else {
			popup_rightButton_text.setText(rightButtonText);
		}
		
		if (headline != null) {
			popup_text_headline.setText(headline);
		}
		
		mPopUpCallback = callBack;
		mPopupType = popupType;
		
		switch (mPopupType) {
		case Failure:
			popup_status_image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.popup_fail));
			break;
		case Information:
			popup_status_image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.popup_info));
			break;
		case Success:
			popup_status_image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.popup_ok));
			break;
		}
	}
	
	@Click
	void popup_leftButton() {
		mPopUpCallback.popupLeftButtonTapped();
	}
	
	@Click
	void popup_rightButton_text() {
		mPopUpCallback.popupRightButtonTapped();
	}
	
	public interface PopUpCallback {
		void popupLeftButtonTapped();
		void popupRightButtonTapped();
	}
	
	public enum PopupType {
		Success,
		Failure,
		Information
	}
}
