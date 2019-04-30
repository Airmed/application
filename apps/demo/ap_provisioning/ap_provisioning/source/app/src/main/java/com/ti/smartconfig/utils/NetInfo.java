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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by admin on 7/20/15.
 */
public class NetInfo {
    private final String TAG = "NetInfo";
    private static final int BUF = 8 * 1024;
    private static final String CMD_IP = " -f inet addr show %s";
    private static final String PTN_IP1 = "\\s*inet [0-9\\.]+\\/([0-9]+) brd [0-9\\.]+ scope global %s$";
    private static final String PTN_IP2 = "\\s*inet [0-9\\.]+ peer [0-9\\.]+\\/([0-9]+) scope global %s$"; // FIXME: Merge with PTN_IP1
    private static final String PTN_IF = "^%s: ip [0-9\\.]+ mask ([0-9\\.]+) flags.*";
    private static final String NO_IF = "0";
    public static final String NO_IP = "0.0.0.0";
    public static final String NO_MASK = "255.255.255.255";
    public static final String NO_MAC = "00:00:00:00:00:00";
    private Context ctxt;
    private WifiInfo info;
    private SharedPreferences prefs;

    public String intf = "eth0";
    public String ip = NO_IP;
    public int cidr = 24;

    public int speed = 0;
    public String ssid = null;
    public String bssid = null;
    public String carrier = null;
    public String macAddress = NO_MAC;
    public String netMaskIp = NO_MASK;
    public String gatewayIp = NO_IP;

    public NetInfo(final Context ctxt) {
        this.ctxt = ctxt;
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        getIp();
        getWifiInfo();

        // Set ARP enabled
        // try {
        // Runtime.getRuntime().exec("su -C ip link set dev " + intf +
        // " arp on");
        // } catch (Exception e) {
        // Log.e(TAG, e.getMessage());
        // }
        // Runtime.getRuntime().exec("echo 1 > /proc/sys/net/ipv4/conf/" + intf
        // + "/proxy_arp");
        // Runtime.getRuntime().exec("echo 1 > /proc/sys/net/ipv4/conf/tun0/proxy_arp");
    }

//    @Override
//    public int hashCode() {
//        int ip_custom = prefs.getBoolean(Prefs.KEY_IP_CUSTOM, Prefs.DEFAULT_IP_CUSTOM) ? 1:0;
//        int ip_start = prefs.getString(Prefs.KEY_IP_START, Prefs.DEFAULT_IP_START).hashCode();
//        int ip_end = prefs.getString(Prefs.KEY_IP_END, Prefs.DEFAULT_IP_END).hashCode();
//        int cidr_custom = prefs.getBoolean(Prefs.KEY_CIDR_CUSTOM, Prefs.DEFAULT_CIDR_CUSTOM) ? 1:0;
//        int cidr = prefs.getString(Prefs.KEY_CIDR, Prefs.DEFAULT_CIDR).hashCode();
//        return 42 + intf.hashCode() + ip.hashCode() + cidr + ip_custom + ip_start + ip_end + cidr_custom + cidr;
//    }

    public void getIp() {
        try {
            // Automatic interface selection
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements();) {
                NetworkInterface ni = en.nextElement();
                intf = ni.getName();
                ip = getInterfaceFirstIp(ni);
                if (!ip.equals(NO_IP)) {
//                if (ip != NO_IP) {
                    break;
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, e.getMessage());
        }
        getCidr();
    }

    private String getInterfaceFirstIp(NetworkInterface ni) {
        if (ni != null) {
            for (Enumeration<InetAddress> nis = ni.getInetAddresses(); nis.hasMoreElements();) {
                InetAddress ia = nis.nextElement();
                if (!ia.isLoopbackAddress()) {
                    if (ia instanceof Inet6Address) {
                        Log.i(TAG, "IPv6 detected and not supported yet!");
                        continue;
                    }
                    return ia.getHostAddress();
                }
            }
        }
        return NO_IP;
    }

    private void getCidr() {
        if (!(netMaskIp.equals(NO_MASK))) {
            cidr = IpToCidr(netMaskIp);
        } else {
            String match;
            // Running ip tools
            try {
                if ((match = runCommand("/system/xbin/ip", String.format(CMD_IP, intf), String.format(PTN_IP1, intf))) != null) {
                    cidr = Integer.parseInt(match);
                    return;
                } else if ((match = runCommand("/system/xbin/ip", String.format(CMD_IP, intf), String.format(PTN_IP2, intf))) != null) {
                    cidr = Integer.parseInt(match);
                    return;
                } else if ((match = runCommand("/system/bin/ifconfig", " " + intf, String.format(PTN_IF, intf))) != null) {
                    cidr = IpToCidr(match);
                    return;
                } else {
                    Log.i(TAG, "cannot find cidr, using default /24");
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Log.i(TAG, e.getMessage()+ " -> cannot find cidr, using default /24");
            }
        }
    }

    // FIXME: Factorize, this isn't a generic runCommand()
    private String runCommand(String path, String cmd, String ptn) {
        try {
            if (new File(path).exists()) {
                String line;
                Matcher matcher;
                Pattern ptrn = Pattern.compile(ptn);
                Process p = Runtime.getRuntime().exec(path + cmd);
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()), BUF);
                while ((line = r.readLine()) != null) {
                    matcher = ptrn.matcher(line);
                    if (matcher.matches()) {
                        return matcher.group(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Can't use native command: " + e.getMessage());
            return null;
        }
        return null;
    }

    public boolean getMobileInfo() {
        TelephonyManager tm = (TelephonyManager) ctxt.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            carrier = tm.getNetworkOperatorName();
        }
        return false;
    }

    public boolean getWifiInfo() {
        WifiManager wifi = (WifiManager) ctxt.getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            info = wifi.getConnectionInfo();
            // Set wifi variables
            speed = info.getLinkSpeed();
            ssid = info.getSSID();
            bssid = info.getBSSID();
            macAddress = info.getMacAddress();
            gatewayIp = getIpFromIntSigned(wifi.getDhcpInfo().gateway);
            // broadcastIp = getIpFromIntSigned((dhcp.ipAddress & dhcp.netmask)
            // | ~dhcp.netmask);
            netMaskIp = getIpFromIntSigned(wifi.getDhcpInfo().netmask);
            return true;
        }
        return false;
    }

    public String getNetIp() {
        int shift = (32 - cidr);
        int start = ((int) getUnsignedLongFromIp(ip) >> shift << shift);
        return getIpFromLongUnsigned((long) start);
    }


    public SupplicantState getSupplicantState() {
        return info.getSupplicantState();
    }

    public static boolean isConnected(Context ctxt) {
        NetworkInfo nfo = ((ConnectivityManager) ctxt
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (nfo != null) {
            return nfo.isConnected();
        }
        return false;
    }

    public static long getUnsignedLongFromIp(String ip_addr) {
        String[] a = ip_addr.split("\\.");
        return (Integer.parseInt(a[0]) * 16777216 + Integer.parseInt(a[1]) * 65536
                + Integer.parseInt(a[2]) * 256 + Integer.parseInt(a[3]));
    }

    public static String getIpFromIntSigned(int ip_int) {
        String ip = "";
        for (int k = 0; k < 4; k++) {
            ip = ip + ((ip_int >> k * 8) & 0xFF) + ".";
        }
        return ip.substring(0, ip.length() - 1);
    }

    public static String getIpFromLongUnsigned(long ip_long) {
        String ip = "";
        for (int k = 3; k > -1; k--) {
            ip = ip + ((ip_long >> k * 8) & 0xFF) + ".";
        }
        return ip.substring(0, ip.length() - 1);
    }

    private int IpToCidr(String ip) {
        double sum = -2;
        String[] part = ip.split("\\.");
        for (String p : part) {
            sum += 256D - Double.parseDouble(p);
        }
        return 32 - (int) (Math.log(sum) / Math.log(2d));
    }


}


