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

public enum CFG_Result_Enum {
	Success,
	Failure,
	Unknown_Token,
	Not_Started,
	Wrong_Password,
	Ap_Not_Found,
	Ip_Add_Fail,
	Time_Out;
	
	public static String SUCCESS_STRING = "Provisioning Successful";
	public static String FAILURE_STRING = "Please try to restart the device and the configuration application and try again";
	public static String UNKNOWN_STRING = "CFG_Result_Enum: Unknown_Token";
	public static String NOT_STARTED_STRING = "The provisioning sequence has not started yet. Device is waiting for configuration to be sent";
	public static String WRONG_PASSWORD_STRING = "Connection to selected SimpleLink device has failed. Please restart the SimpleLink device, and try one of the following:\nCheck your password and/or key are entered correctly and try again\n,Check your SimpleLink device is working";
	public static String AP_NOT_FOUND_STRING = "Could not find the selected WiFi network; it is either turned off or out of range. When the WiFi network is available please restart the device in order to connect.";
	public static String IP_ADD_FAIL_STRING = "The SimpleLink device failed to acquire an IP address from the selected AP. Please try one of the following:\nConnect a new device to the WiFi AP to see if it is OK,\nRestart the WiFi AP";
	public static String TIME_OUT_STRING = "CFG_Result_Enum: Time_Out";
	
}
