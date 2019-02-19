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

public interface SmartConfigConstants {
	
	String NETWORK_CHANGE_BROADCAST_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
	String SCAN_FINISHED_BROADCAST_ACTION = "com.ti.smartconfig.utils.SCAN_FINISHED";
	String DEVICE_FOUND_BROADCAST_ACTION = "com.ti.smartconfig.utils.DEVICE_FOUND";
	int SPLASH_SCREEN_TIME = 2000; //2 seconds
	int MAIN_SCAN_TIME = 15000; //15 seconds
	int MDNS_SCAN_TIME = 30000; //15 seconds
	int JMDNS_CLOSE_TIME = 6000; //6 seconds
	int TAB_DIMENS = 80; // the size of the tabs in dp
	public static final int JMDNS_CLOSE_DELAY = 5000; // 5 seconds
	int SC_RUNTIME = 60000; // one minute + 5 seconds
	int SC_PROGRESSBAR_INTERVAL = 1000; // one second
	int SC_MDNS_INTERVAL = 10000; //10 seconds
	String ZERO_PADDING_16="0000000000000000";

	int RSSI_LEVEL_HIGH = 3;
	int RSSI_LEVEL_MID_HIGH = 2;
	int RSSI_LEVEL_MID_LOW = 1;
	int RSSI_LEVEL_LOW = 0;
	
}
