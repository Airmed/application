package com.ti.smartconfig.utils;

public enum DeviceVersion {
	UNKNOWN,
	R1,
	R2;
	
	public static String getStringFor(DeviceVersion version) {
		String string = "";
		
		switch (version) {
		case R1:
			string = "R1.0";
			break;
		case R2:
			string = "R2.0";
			break;
		case UNKNOWN:
			string = "UNKNOWN";
		}
		
		return string;
	}
}
