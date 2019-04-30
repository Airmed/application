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

import java.io.File;

import android.os.Environment;

public interface Constants {
	String DEVICE_PREFIX = "mysimplelink";
	String BASE_URL = "http://mysimplelink.net";
	String BASE_URL_NO_HTTP = "://mysimplelink.net";
	int PERIODIC_SCAN_TIME = 25000;
	int ATTEMPTS_TO_GET_CGF_RESULTS = 5;
	
	int ATTEMPTS_TO_GET_CGF_RESULTS_DELAY = 1500;
	
	int DELAY_BETWEEN_CONNECTING_TO_DEVICE_TO_PULLING_NETWORKS = 5000;
	//ofir 15.1
	//changed from 3 seconds to 1
	int DELAY_BETWEEN_CFG_RESULTS_REQUESTS = 3000;
	int DELAY_BEFORE_ASKING_CFG_RESULTS = 3000;
	int DELAY_AFTER_RESCAN_NETWORKS_BEFORE_FETCHING_NETWORKS = 20000;
	int DELAY_BEFORE_FETCHING_SSIDS_FROM_DEVICE = 1000;
	
	int SMART_CONFIG_WAITING_BEFORE_MANUAL_CONFIRMATION = 15000;
	
	int WIFI_CONNECTION_TIMEOUT = 10000;
	int WIFI_SCAN_TIMEOUT = 10000;
	
	int SMART_CONFIG_TRANSMIT_TIME = 40000;
	
	int DEVICE_WAITING_TIME_BEFORE_STARTING_MDNS_SCAN = 3000;
	
	int TRY_NUMBER_FOR_GETTING_SSIDS_FROM_DEVICE = 3;
	
	String DEVICE_UNKNOWN_TOKEN_STRING = "Unknown Token";

	String MDNS_SCAN_MESSAGE = "Searching for devices";
	
	String DEVICE_LIST_FAILED_TO_GET_RESULTS = "Application cannot configure the selected device. Either you have selected a non-SimpleLink device or  you have selected a SimpleLink device which supports only legacy provisioning sequence. Please choose another SimpleLink device";
	String DEVICE_LIST_FAILED_TO_RESCAN = "Failed to perform rescan on device";
	String DEVICE_LIST_MUST_SUPPLY_PASSWORD = "You must supply password for this network";
	String DEVICE_LIST_MUST_SUPPLY_SSID = "You must choose a legit network";
	String DEVICE_LIST_FAILED_ADDING_PROFILE = "Failed adding the profile";
	String DEVICE_LIST_FAILED_CONFIRMATION_VIA_DEVICE = "Failed confirmation via device";
	String DEVICE_LIST_FAILED_CONFIRMATION_VIA_WIFI = "Failed confirmation via wifi";
	
	String DEVICE_LIST_CFG_CONFIRMATION_FAILED = "Confirmation failed";
	
	String SMART_CONFIG_MUST_CHOOSE_NETWORK_TO_BEGIN = "Please choose network first";
	
	String SMART_CONFIG_CFG_RESULT_NOT_STARTED = "Simplelink confirmation not started";
	


	String CAMERA_NOT_AVAILABLE = "Camera unavailable or scan canceled by user";
	
	String PROVISIONING_NOT_READY = "Not ready to start";
	
	String QUESTION_DEVICE_NAME = "Set the \"Device name\" to a friendly name, for example - Kitchen Temp Sensor. If not set, device name shall be factory default name.";

	String QUESTION_DEVICE_PASSWORD = "Set the device's password, used for connecting to devices working in secured AP mode";
	String QUESTION_CONFIGURATION_KEY = "Enter the password used for secure transmission of network information to the device";
	String QUESTION_NETWORK_NAME = "Set the name of the Network you would like to connect your device to. For example, your home router name. The device will connect to this network at the end of configuration sequence.";
	String QUESTION_NETWORK_PASSWORD = "In case network is secured, set its password";
	String QUESTION_CHOOSE_DEVICE = "More than one device was detected, select the device you want to configure";
	String QUESTION_CHOOSE_ROUTER = "Please choose the network that the device will connect to";
	String QUESTION_SET_IOT = "Please set your iotLink UUID (Unique User ID)";
	int WIFI_SETTINGS_INTENT_RESULTS = 4;
	int WIFI_FIRST_STEP_CONNECTION_FAILURE = 44;
	int WIFI_TIMEOUT_FAILURE = 47;
	int WIFI_3G_FAILURE = 57;
	String QUESTION_PASSWORD = "Please enter your WiFi password";
	String LOG_PATH = Environment.getExternalStorageDirectory() + File.separator + "SmartConfig" + File.separator + "logs" + File.separator + "log4j.txt";
	String DOWNLOAD_FILE_PATH_PREFIX = "http://software-dl.ti.com/ecs/cc31xx/OTA/readme";





}
