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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class NetworkUtil {

    private static final String TAG = "NetworkUtil";

    public static int NOT_CONNECTED = 0;
    public static int WIFI = 1;
    public static int MOBILE = 2;
    public static boolean didRedirect = false;
    public static final String HTTP_ = "http";
    public static final String HTTPS_ = "https";

    private static Logger mLogger = Logger.getLogger(NetworkUtil.class);

    public static int getConnectionStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return WIFI;
            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return MOBILE;
        }
        return NOT_CONNECTED;
    }

    public static String getConnectedSSID(Context context) {

        if (context == null)
            return null;
        String networkName = null;
        int networkState = getConnectionStatus(context);
        Log.i(TAG, "Network State:" + networkState);
        mLogger.info("Network state: " + networkState);
        if (networkState == NetworkUtil.WIFI) { //no wifi connection and alert dialog allowed //i-why no wifi connection?
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    networkName = wifiInfo.getSSID().replaceAll("\"", "");
                }
            }
        }
        if (networkName == null || networkName.equals("<unknown ssid>") || networkName.equals("0x") || networkName.equals("")) {
            networkName = null;
        }

        return networkName;
    }

    public static String getConnectionStatusString(Context context) {
        int connectionStatus = NetworkUtil.getConnectionStatus(context);
        if (connectionStatus == NetworkUtil.WIFI)
            return "Connected to Wifi";
        if (connectionStatus == NetworkUtil.MOBILE)
            return "Connected to Mobile Data";
        return "No internet connection";
    }

    public static List<ScanResult> getWifiScanResults(Boolean sorted, Context context) {
        WifiManager wifiManager = NetworkUtil.getWifiManager(context);
        List<ScanResult> wifiList = wifiManager.getScanResults();

        //Remove results with empty ssid
        List<ScanResult> wifiListNew = new ArrayList<>();
        for (ScanResult scanResult : wifiList) {
            if (!scanResult.SSID.equals(""))
                wifiListNew.add(scanResult);
        }
        wifiList.clear();
        wifiList.addAll(wifiListNew);

        if (!sorted)
            return wifiList;

        ArrayList<ScanResult> wifiWithAPPrefix = new ArrayList<>();
        ArrayList<ScanResult> rest = new ArrayList<>();
        for (ScanResult scanResult : wifiList) {
            if (scanResult.SSID.contains(Constants.DEVICE_PREFIX))
                wifiWithAPPrefix.add(scanResult);
            else
                rest.add(scanResult);
        }


        wifiWithAPPrefix = removeMultipleSSIDsWithRSSI(wifiWithAPPrefix);
        rest = removeMultipleSSIDsWithRSSI(rest);


        wifiWithAPPrefix.addAll(rest);
        wifiList = wifiWithAPPrefix;

        return wifiList;
    }


    /**
     * The removeMultipleSSIDsWithRSSI method is used to remove multiple appearances of identical SSIDs from
     * the list of APs obtained from the wifiManager, and displayed on the smartConfig mode configuration page as WiFi networks.
     * This is due to the possible presence of several APs possessing the same SSID but different BSSIDs, and thus causing
     * the same AP to appear several times on the list.
     */
    public static ArrayList<ScanResult> removeMultipleSSIDsWithRSSI(ArrayList<ScanResult> list) {

        ArrayList<ScanResult> newList = new ArrayList<>();
        boolean contains;
        for (ScanResult ap : list) {
            contains = false;
            for (ScanResult mp : newList) {
                if ((mp.SSID).equals(ap.SSID)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                newList.add(ap);
            }
        }

        Collections.sort(newList, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                return (lhs.level < rhs.level ? 1 : (lhs.level == rhs.level ? 0 : -1));
            }

        });

        return newList;

    }

    public static String getWifiName(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String wifiName = wifiManager.getConnectionInfo().getSSID();
        if (wifiName != null) {
            if (!wifiName.contains("unknown ssid") && wifiName.length() > 2) {
                if (wifiName.startsWith("\"") && wifiName.endsWith("\""))
                    wifiName = wifiName.subSequence(1, wifiName.length() - 1).toString();
                return wifiName;
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    public static String getGateway(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return NetworkUtil.intToIp(wm.getDhcpInfo().gateway);
    }

    public static String intToIp(int i) {
        return ((i >> 24) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                (i & 0xFF);
    }

    public static void startScan(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
    }

    public static WifiManager getWifiManager(Context context) {
        WifiManager wifiManager = null;

        try {

            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return wifiManager;
    }

    public static void connectToKnownWifi(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
            }
        }
    }

    public static Boolean connectToWifiAfterDisconnecting(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wc;
        wc = new WifiConfiguration();
        wc.SSID = "\"" + ssid + "\"";
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wifiManager.addNetwork(wc);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.enableNetwork(i.networkId, true);
                Boolean flag = wifiManager.reconnect();
                return flag;
            }
        }

        return false;
    }

    public static void removeSSIDFromConfiguredNetwork(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        List<WifiConfiguration> configuredWifiList = wifiManager.getConfiguredNetworks();
        for (int x = 0; x < configuredWifiList.size(); x++) {
            WifiConfiguration i = configuredWifiList.get(x);
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                Log.w(TAG, "Removing network: " + i.SSID);
                wifiManager.removeNetwork(i.networkId);
                return;
            }
        }
    }

    public static WifiConfiguration getWifiConfigurationWithInfo(Context context, String ssid, SecurityType securityType, String password) {

        List<WifiConfiguration> configuredWifiList = null;

        WifiManager wifiManager = getWifiManager(context);
        if (wifiManager != null) {
            configuredWifiList = wifiManager.getConfiguredNetworks();
        }

        if (configuredWifiList == null) {
            return null;

        } else {

            for (WifiConfiguration i : configuredWifiList) {
                if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    Log.i(TAG, "Wifi configuration for " + ssid + " already exist, so we will use it");
                    return i;
                }
            }

            Log.i(TAG, "Wifi configuration for " + ssid + " doesn't exist, so we will create new one");
            Log.i(TAG, "SSID: " + ssid);
            Log.i(TAG, "Security: " + securityType);
            WifiConfiguration wc = new WifiConfiguration();

            wc.SSID = "\"" + ssid + "\"";
            wc.status = WifiConfiguration.Status.ENABLED;
            wc.hiddenSSID = false;

            switch (securityType) {
                case OPEN:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    break;
                case WEP:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                    wc.preSharedKey = "\"" + password + "\"";
                    break;
                case WPA1:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    wc.preSharedKey = "\"" + password + "\"";
                    break;
                case WPA2:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    wc.preSharedKey = "\"" + password + "\"";
                    break;
                case UNKNOWN:
                    if (password == null) {
                        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    } else {
                        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                        wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                        wc.preSharedKey = "\"" + password + "\"";
                    }
                    break;
                default:
                    break;
            }

            Log.i(TAG, "New wifi configuration with id " + wifiManager.addNetwork(wc));
            Log.i(TAG, "Saving configuration " + wifiManager.saveConfiguration());
            Log.i(TAG, "wc.networkId " + wc.networkId);

            configuredWifiList = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : configuredWifiList) {
                if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    Log.i(TAG, "Returning wifiConfiguration with id " + i.networkId);
                    return i;
                }
            }
        }
        return null;
    }

    public static void connectToWifiWithInfo(Context context, String ssid, SecurityType securityType, String password) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        int numberOfOcc = 0;

        List<WifiConfiguration> configuredWifiList = wifiManager.getConfiguredNetworks();
        for (int x = 0; x < configuredWifiList.size(); x++) {
            WifiConfiguration i = configuredWifiList.get(x);
            //System.out.println(i.SSID);
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                numberOfOcc++;
            }
        }

        System.out.println("Done checking doubles: " + numberOfOcc);

        for (WifiConfiguration i : configuredWifiList) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                Log.i(TAG, "Trying to disconnect (success = " + wifiManager.disconnect() + ")");
                Log.i(TAG, "Trying to connect to " + i.SSID + " (success = " + wifiManager.enableNetwork(i.networkId, true) + ")");
                return;
            }
        }

        WifiConfiguration wc = new WifiConfiguration();

        wc.SSID = "\"" + ssid + "\"";
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.hiddenSSID = false;

        switch (securityType) {
            case OPEN:
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case WEP:
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                wc.preSharedKey = "\"" + password + "\"";
                break;
            case WPA1:
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wc.preSharedKey = "\"" + password + "\"";
                break;
            case WPA2:
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wc.preSharedKey = "\"" + password + "\"";
                break;
            case UNKNOWN:
                if (password == null) {
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                } else {
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    wc.preSharedKey = "\"" + password + "\"";
                }
                break;
            default:
                break;
        }

        int res = wifiManager.addNetwork(wc);
        Log.i(TAG, "addNetwork: " + res);
        wifiManager.disconnect();
        wifiManager.enableNetwork(res, true);
        wifiManager.saveConfiguration();

		/*
        if (isLollipopAndUp()) {
			enableNetwork(ssid, context);
		}
		else {
			wifiManager.enableNetwork(res, true);
		}
		 */

		/*
		if (wifiManager.saveConfiguration() == false) {
			Log.w(TAG, "Failed to save wifi configuration");
		}
		 */
    }

    public static Boolean isLollipopAndUp() {
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        return currentApiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }

    public static SecurityType getScanResultSecurity(ScanResult scanResult) {
        String cap = scanResult != null ? scanResult.capabilities : "";
        SecurityType newState = scanResult != null ? SecurityType.OPEN : SecurityType.UNKNOWN;

        if (cap.contains("WEP"))
            newState = SecurityType.WEP;
        else if (cap.contains("WPA2"))
            newState = SecurityType.WPA2;
        else if (cap.contains("WPA"))
            newState = SecurityType.WPA1;

        return newState;
    }

    public static Boolean addProfile(String baseUrl, SecurityType securityType, String ssid, String password, String priorityString, DeviceVersion version) {

        String url = baseUrl;
        Log.i(TAG,"REDIRECT- addProfile / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/profiles_add.html";
                Log.i(TAG,"REDIRECT- addProfile / url: " + url);
                break;
            case R2:
                Log.i(TAG,"REDIRECT- addProfile / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/api/1/wlan/profile_add";
                Log.i(TAG,"REDIRECT- addProfile / url: " + url);
                break;
            case UNKNOWN:
                break;
        }

        Boolean flag;
        if (securityType == SecurityType.UNKNOWN) {
            if (password.matches("")) {
                flag = NetworkUtil.addProfile(baseUrl, SecurityType.OPEN, ssid, password, priorityString, version);
            } else {

                flag = NetworkUtil.addProfile(baseUrl, SecurityType.WEP, ssid, password, priorityString, version);
                flag = flag && NetworkUtil.addProfile(baseUrl, SecurityType.WPA1, ssid, password, priorityString, version);
            }
        } else {
            try {
//                HttpClient client = new DefaultHttpClient();

                HttpClient client = getNewHttpClient();
                String addProfileUrl = url;
                HttpPost addProfilePost = new HttpPost(addProfileUrl);
                List<NameValuePair> nameValuePairs = new ArrayList<>(4);
                ssid = new String(ssid.getBytes("UTF-8"), "ISO-8859-1");
                nameValuePairs.add(new BasicNameValuePair("__SL_P_P.A", ssid));
                nameValuePairs.add(new BasicNameValuePair("__SL_P_P.B", String.valueOf(SecurityType.getIntValue(securityType))));
                nameValuePairs.add(new BasicNameValuePair("__SL_P_P.C", password));

                try {
                    int priority = Integer.parseInt(priorityString);
                    nameValuePairs.add(new BasicNameValuePair("__SL_P_P.D", String.valueOf(priority)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    nameValuePairs.add(new BasicNameValuePair("__SL_P_P.D", String.valueOf(0)));
                }

                addProfilePost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                client.execute(addProfilePost);
                flag = true;
            } catch (Exception e) {
                e.printStackTrace();
                flag = false;
            }
        }

        return flag;
    }

    public static Boolean moveStateMachineAfterProfileAddition(String baseUrl, String ssid, DeviceVersion version) throws CertificateException {

        String url = baseUrl;
        Log.i(TAG,"REDIRECT- moveStateMachineAfterProfileAddition / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/add_profile.html";
                Log.i(TAG,"REDIRECT- moveStateMachineAfterProfileAddition / url: " + url);
                break;
            case R2:
                Log.i(TAG,"REDIRECT- moveStateMachineAfterProfileAddition / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/api/1/wlan/confirm_req";
                Log.i(TAG,"REDIRECT- moveStateMachineAfterProfileAddition / url: " + url);
                break;
            case UNKNOWN:
                break;
        }

        Boolean flag;
//        HttpClient client = new DefaultHttpClient();

        HttpClient client = getNewHttpClient();
        try {
            String stateMachineUrl = url;
            HttpPost stateMachinePost = new HttpPost(stateMachineUrl);

            switch (version) {
                case R1:
                    List<NameValuePair> stateParam = new ArrayList<>(1);
                    stateParam.add(new BasicNameValuePair("__SL_P_UAN", ssid));
                    stateMachinePost.setEntity(new UrlEncodedFormEntity(stateParam));
                    break;
                case R2:
                    break;
                case UNKNOWN:
                    break;
            }

            client.execute(stateMachinePost);
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        }

        return flag;
    }


    public static DeviceVersion getSLVersion(String baseUrl) throws IOException, URISyntaxException {

        didRedirect = false;

        Log.i(TAG,"REDIRECT- getSLVersion / did redirect: " + didRedirect);
        Log.i(TAG,"REDIRECT- getSLVersion / base url: " + baseUrl);

        String url = baseUrl + "/param_product_version.txt";
//        String url ="https://mysimplelink.net"+ "/param_product_version.txt";
        Log.i(TAG,"REDIRECT- getSLVersion / url: " + url);
        if (!baseUrl.startsWith("http")) {
            url = HTTP_ + baseUrl + "/param_product_version.txt";
            Log.i(TAG,"REDIRECT- getSLVersion / url: " + url);
        }

        DeviceVersion version = DeviceVersion.UNKNOWN;

        try {
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
//            url ="https://mysimplelink.net"+ "/param_product_version.txt";
//            Log.i(TAG,"REDIRECT- getSLVersion / url: " + url);
//            didRedirect = true;
            URL urlObj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlObj.openConnection();
            int urlConResponseCode = httpURLConnection.getResponseCode();
            Log.i(TAG,"REDIRECT- getSLVersion / response code: " + urlConResponseCode);
            if (urlConResponseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                //wasRedirected
                didRedirect = true;
                Log.i(TAG,"REDIRECT- getSLVersion / did redirect: " + didRedirect);
                if (baseUrl.startsWith("http")) {
                    baseUrl = baseUrl.substring(baseUrl.indexOf(":"),baseUrl.length());
                }

                url = HTTPS_ + baseUrl + "/param_product_version.txt";
//                url = httpURLConnection.getHeaderField("Location");
                Log.i(TAG,"REDIRECT- getSLVersion / redirected url: " + url);

            }

//            HttpClient client = new DefaultHttpClient(httpParameters);
            HttpClient client = getNewHttpClient();
//			HttpClient client = new DefaultHttpClient();
            HttpGet slResult = new HttpGet(url);
//            HttpGet slResult = new HttpGet();
//            slResult.setURI(new URI(url));


            HttpResponse response = client.execute(slResult);


//            int responseStatusCode = response.getStatusLine().getStatusCode();
//            Log.i(TAG,"REDIRECT- getSLVersion / response status code: " + responseStatusCode);

//            if (responseStatusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
//                //wasRedirected
//                didRedirect = true;
//                Log.i(TAG,"REDIRECT- getSLVersion / did redirect: " + didRedirect);
//
//                url = HTTPS_ + Constants.BASE_URL_NO_HTTP + "/param_product_version.txt";
//                Log.i(TAG,"REDIRECT- getSLVersion / redirected url: " + url);
//
//                slResult = new HttpGet(url);
//
//                response = client.execute(slResult);
//
//            }

            String html = EntityUtils.toString(response.getEntity());
            Log.i(TAG,"REDIRECT- getSLVersion / html: " + html);

            if (html.equals("R1.0") || html.contains("1.0")) {
                version = DeviceVersion.R1;
            } else if (html.equals("R2.0") || html.equals("2.0") || html.contains("2.0")) {
                version = DeviceVersion.R2;
            }
        } catch (Exception e) {

            e.printStackTrace();
            System.out.println(e);
            //add here the retry check redirect or not?
            if(didRedirect) {
                HttpClient client = getNewHttpClient();
                HttpGet slResult = new HttpGet(url);
                HttpResponse response = client.execute(slResult);
                String html = EntityUtils.toString(response.getEntity());
                Log.i(TAG, "REDIRECT 1- getSLVersion / html: " + html);

                if (html.equals("R1.0") || html.contains("1.0")) {
                    version = DeviceVersion.R1;
                } else if (html.equals("R2.0") || html.equals("2.0") || html.contains("2.0")) {
                    version = DeviceVersion.R2;
                }
            }else{

                //  HttpClient client = new DefaultHttpClient(httpParameters);
                //    HttpClient client = getNewHttpClient();
                HttpClient client = new DefaultHttpClient();
                // HttpGet slResult = new HttpGet(url);
                HttpGet slResult = new HttpGet();
                slResult.setURI(new URI(url));


                HttpResponse response = client.execute(slResult);
                String html = EntityUtils.toString(response.getEntity());
                Log.i(TAG,"REDIRECT 2- getSLVersion / html: " + html);

                if (html.equals("R1.0") || html.contains("1.0")) {
                    version = DeviceVersion.R1;
                } else if (html.equals("R2.0") || html.equals("2.0") || html.contains("2.0")) {
                    version = DeviceVersion.R2;
                }

            }
        }

        return version;
    }

    public static String getDeviceName(String baseUrl, DeviceVersion version) {

        String deviceName = "";
        String url = baseUrl;
        Log.i(TAG,"REDIRECT- getDeviceName / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/param_device_name.txt";
                Log.i(TAG,"REDIRECT- getDeviceName / url: " + url);
                break;
            case R2:
                Log.i(TAG,"REDIRECT- getDeviceName / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/__SL_G_DNP";
                Log.i(TAG,"REDIRECT- getDeviceName / url: " + url);
                break;
            case UNKNOWN:
                break;
        }

        try {
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

//            HttpClient client = new DefaultHttpClient(httpParameters);

            HttpClient client = getNewHttpClient();
//			HttpClient client = new DefaultHttpClient();
            HttpGet slResult = new HttpGet(url);

            HttpResponse response = client.execute(slResult);
            String name = EntityUtils.toString(response.getEntity());
            deviceName = name;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to fetch device name from board");
        }

        return deviceName;
    }

    public static ArrayList<String> getSSIDListFromDevice(String baseUrl, DeviceVersion version) throws IOException {

        String url = baseUrl;
        Log.i(TAG,"REDIRECT- getSSIDListFromDevice / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                Log.i(TAG,"REDIRECT- getSSIDListFromDevice / url: " + url);
                break;
            case R2:
                Log.i(TAG,"REDIRECT- getSSIDListFromDevice / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                Log.i(TAG,"REDIRECT- getSSIDListFromDevice / url: " + url);
                break;
            case UNKNOWN:
                break;
        }

        url = url + "/netlist.txt";
        Log.i(TAG,"REDIRECT- getSSIDListFromDevice / url: " + url);

        ArrayList<String> list = new ArrayList<>();

        mLogger.info("*AP* Getting list of available access points from SL device, from url: " + url);

        try {
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

//            HttpClient client = new DefaultHttpClient(httpParameters);

            HttpClient client = getNewHttpClient();
//			HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            String responseString = EntityUtils.toString(response.getEntity());
            mLogger.info("*AP* Got netlist with results: " + responseString);

            String[] names = responseString.split(";");
            for (String name : names) {
                if (!name.equals("X"))
                    list.add(name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mLogger.error("Failed to get netlist: " + e.getMessage());
            return null;
        }

        return list;
    }

    public static Boolean rescanNetworksOnDevice(String url, DeviceVersion version) throws CertificateException {

        Boolean flag = false;
//        HttpClient client = new DefaultHttpClient();

        HttpClient client = getNewHttpClient();
        List<NameValuePair> stateParam;
        String rescanUrl = url;
        Log.i(TAG,"REDIRECT- rescanNetworksOnDevice / rescanUrl: " + rescanUrl);

        mLogger.info("*AP* Rescanning for list of available access points from SL device with url: " + url);

        switch (version) {

            case R1:
                rescanUrl = HTTP_ + rescanUrl;
                Log.i(TAG,"REDIRECT- rescanNetworksOnDevice / rescanUrl: " + rescanUrl);
                stateParam = new ArrayList<NameValuePair>(1);

                try {
                    HttpPost rescanPost = new HttpPost(rescanUrl);
                    stateParam.add(new BasicNameValuePair("__SL_P_UFS", "just empty information"));
                    rescanPost.setEntity(new UrlEncodedFormEntity(stateParam));
                    client.execute(rescanPost);
                    flag = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    mLogger.error("*AP* Failed to perform rescan");
                    flag = false;
                }

                break;

            case R2:
                stateParam = new ArrayList<NameValuePair>(2);

                Log.i(TAG,"REDIRECT- rescanNetworksOnDevice / didRedirect: " + didRedirect);

                if (didRedirect) {
                    rescanUrl = HTTPS_ + rescanUrl;
                } else {
                    rescanUrl = HTTP_ + rescanUrl;
                }
                rescanUrl += "/api/1/wlan/en_ap_scan";
                Log.i(TAG,"REDIRECT- rescanNetworksOnDevice / rescanUrl: " + rescanUrl);

                try {
                    HttpPost rescanPost = new HttpPost(rescanUrl);
                    stateParam.add(new BasicNameValuePair("__SL_P_SC1", "10"));
                    stateParam.add(new BasicNameValuePair("__SL_P_SC2", "1"));
                    rescanPost.setEntity(new UrlEncodedFormEntity(stateParam));
                    client.execute(rescanPost);
                    flag = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    mLogger.error("*AP* Failed to perform rescan (R2)");
                    flag = false;
                }

                break;
            case UNKNOWN:
                break;

        }

        return flag;
    }

    public static CFG_Result_Enum cfgEnumForResponse(String string) {

        CFG_Result_Enum resultEnum;

        if (string.contains("5") || string.contains("4")) {
            resultEnum = CFG_Result_Enum.Success;
        } else if (string.contains("Unknown Token")) {
            resultEnum = CFG_Result_Enum.Unknown_Token;
        } else if (string.contains("Timeout")) {
            resultEnum = CFG_Result_Enum.Time_Out;
        } else if (string.contains("0")) {
            resultEnum = CFG_Result_Enum.Not_Started;
        } else if (string.contains("1")) {
            resultEnum = CFG_Result_Enum.Ap_Not_Found;
        } else if (string.contains("2")) {
            resultEnum = CFG_Result_Enum.Wrong_Password;
        } else if (string.contains("3")) {
            resultEnum = CFG_Result_Enum.Ip_Add_Fail;
        } else {
            resultEnum = CFG_Result_Enum.Failure;
        }

        return resultEnum;
    }

    public static Boolean setNewDeviceName(String newName, String baseUrl, DeviceVersion version) throws CertificateException {

        String url = baseUrl;
        Log.i(TAG,"REDIRECT- setNewDeviceName / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/mode_config";
                Log.i(TAG,"REDIRECT- setNewDeviceName / url: " + url);
                break;
            case R2:
                Log.i(TAG,"REDIRECT- setNewDeviceName / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/api/1/netapp/set_urn";
                Log.i(TAG,"REDIRECT- setNewDeviceName / url: " + url);
                break;
            case UNKNOWN:
                break;
        }

        Boolean flag;
//        HttpClient client = new DefaultHttpClient();

        HttpClient client = getNewHttpClient();
        try {
            String stateMachineUrl = url;
            HttpPost rescanPost = new HttpPost(stateMachineUrl);
            List<NameValuePair> stateParam = new ArrayList<>(1);

            newName = new String(newName.getBytes("UTF-8"), "ISO-8859-1");
            stateParam.add(new BasicNameValuePair("__SL_P_S.B", newName));

            rescanPost.setEntity(new UrlEncodedFormEntity(stateParam));
            client.execute(rescanPost);
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        }

        return flag;
    }


    public static Boolean setIotUuid(String newName, String baseUrl) throws CertificateException {

        String url = baseUrl;
        Log.i(TAG,"REDIRECT- setIotUuid / url: " + url);

        Log.i(TAG,"REDIRECT- setIotUuid / didRedirect: " + didRedirect);
        if(didRedirect) {
            url = HTTPS_ + url;
        } else {
            url = HTTP_ + url;
        }
        url += "/api/1/iotlink/uuid";
        Log.i(TAG,"REDIRECT- setIotUuid / url: " + url);

        if (newName.equals(""))
            return false;

        Boolean flag = false;
        HttpParams httpParameters = new BasicHttpParams();
        // Set the timeout in milliseconds until a connection is established.
        // The default value is zero, that means the timeout is not used.
        int timeoutConnection = 1000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        int timeoutSocket = 1000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

//        HttpClient client = new DefaultHttpClient(httpParameters);

        HttpClient client = getNewHttpClient();

        try {
            String stateMachineUrl = url;
            HttpPost rescanPost = new HttpPost(stateMachineUrl);
            List<NameValuePair> stateParam = new ArrayList<>(1);

            newName = new String(newName.getBytes("UTF-8"), "ISO-8859-1");
            stateParam.add(new BasicNameValuePair("uuid", newName));

            rescanPost.setEntity(new UrlEncodedFormEntity(stateParam));
            HttpResponse response = client.execute(rescanPost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                flag = true;
            }
            client.getConnectionManager().shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            client.getConnectionManager().shutdown();
            flag = false;
        }

        return flag;
    }


    public static String getCGFResultFromDevice(String baseUrl, DeviceVersion version) {

        String url = baseUrl;
        Log.i(TAG,"REDIRECT- getCGFResultFromDevice / url: " + url);

        if (url.startsWith("http")) {
            url = url.substring(url.indexOf(":"),url.length());
        }
        Log.i(TAG,"REDIRECT- getCGFResultFromDevice / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/param_cfg_result.txt";
                Log.i(TAG,"REDIRECT- getCGFResultFromDevice / R1 - url: " + url);
                break;
            case R2:
                Log.i(TAG,"REDIRECT- getCGFResultFromDevice / R2 - didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/__SL_G_MCR";
                Log.i(TAG,"REDIRECT- getCGFResultFromDevice / R2 - url: " + url);
                break;
            case UNKNOWN:
                break;
        }

        String result;

        try {
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);


//            HttpClient client = new DefaultHttpClient(httpParameters);

            HttpClient client = getNewHttpClient();

            HttpGet cfgResult = new HttpGet(url);
            HttpResponse response = client.execute(cfgResult);
            result = EntityUtils.toString(response.getEntity());

            if (result.equals("")) {
                Log.w(TAG, "CFG result returned empty!");
                mLogger.info("CFG result returned empty!");
                result = "Timeout";
            } else {
                Log.i(TAG, "CFG result returned: " + result);
                mLogger.info("CFG result returned: " + result);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to get CFG result");
            mLogger.info("Failed to get CFG result");
            result = "Timeout";
        }

        return result;
    }

    public static String getErrorMsgForCFGResult(CFG_Result_Enum result) {
        String resultString = null;

        switch (result) {
            case Failure:
                resultString = CFG_Result_Enum.FAILURE_STRING;
                break;
            case Success:
                resultString = CFG_Result_Enum.SUCCESS_STRING;
                break;
            case Time_Out:
                System.out.println("CFG_Result_Enum: Time_Out");
                break;
            case Unknown_Token:
                resultString = CFG_Result_Enum.UNKNOWN_STRING;
                break;
            case Not_Started:
                resultString = CFG_Result_Enum.NOT_STARTED_STRING;
                break;
            case Ap_Not_Found:
                resultString = CFG_Result_Enum.AP_NOT_FOUND_STRING;
                break;
            case Ip_Add_Fail:
                resultString = CFG_Result_Enum.IP_ADD_FAIL_STRING;
                break;
            case Wrong_Password:
                resultString = CFG_Result_Enum.WRONG_PASSWORD_STRING;
                break;
        }

        return resultString;
    }

    public static CFG_Result_Enum getResultTypeCFGString(String resultString) {

        CFG_Result_Enum result = CFG_Result_Enum.Unknown_Token;

        if (resultString.equals(CFG_Result_Enum.WRONG_PASSWORD_STRING)) {
            result = CFG_Result_Enum.Wrong_Password;
        } else if (resultString.equals(CFG_Result_Enum.IP_ADD_FAIL_STRING)) {
            result = CFG_Result_Enum.Ip_Add_Fail;
        } else if (resultString.equals(CFG_Result_Enum.AP_NOT_FOUND_STRING)) {
            result = CFG_Result_Enum.Ap_Not_Found;
        } else if (resultString.equals(CFG_Result_Enum.NOT_STARTED_STRING)) {
            result = CFG_Result_Enum.Not_Started;
        } else if (resultString.equals(CFG_Result_Enum.SUCCESS_STRING)) {
            result = CFG_Result_Enum.Success;
        } else if (resultString.equals(CFG_Result_Enum.FAILURE_STRING)) {
            result = CFG_Result_Enum.Failure;
        }

        return result;
    }


//    public static boolean isOTA(String baseUrl) {
//
//        Log.i(TAG, "isOTA started");
//
////		String url = baseUrl + "/device?appid";
//        String url = baseUrl + "/device?appname";
//        Log.i(TAG, "isOTA url: " + url);
//
//        boolean result = false;
//
//        try {
//            HttpParams httpParameters = new BasicHttpParams();
//            int connectionTimeout = 3000;
//            HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeout);
//
//            int socketTimeout = 5000;
//            HttpConnectionParams.setSoTimeout(httpParameters, socketTimeout);
//
//            HttpClient httpClient = new DefaultHttpClient(httpParameters);
//
//            HttpGet httpGet = new HttpGet(url);
//
//            HttpResponse httpResponse = httpClient.execute(httpGet);
//
//            String stringResponse = EntityUtils.toString(httpResponse.getEntity());
//            Log.i(TAG, "isOTA response: " + stringResponse);
//
//            if (stringResponse.contains("out_of_box")) {
//                result = true;
//            }
//
//        } catch (ClientProtocolException e) {
//            e.printStackTrace();
//            Log.e(TAG, e.getMessage());
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG, e.getMessage());
//        }
//
//        Log.i(TAG, "isOTA result: " + result);
//        return result;
//
//
//    }

    public static Device_Type_Enum slDeviceOTAAndType(String baseUrl) {

        Log.i(TAG, "slDeviceOTAAndType started");

//		String url = baseUrl + "/device?appid";
        String url = baseUrl + "/device?appname";
        Log.i(TAG, "slDeviceOTAAndType url: " + url);
        Log.i(TAG, "REDIRECT- slDeviceOTAAndType / url: " + url);

        Log.i(TAG, "REDIRECT- slDeviceOTAAndType / didRedirect: " + didRedirect);
        if (didRedirect) {
            url = HTTPS_ + url;
        } else {
            url = HTTP_ + url;
        }
        Log.i(TAG, "REDIRECT- slDeviceOTAAndType / url: " + url);

        Device_Type_Enum deviceTypeEnum = null;

        try {
            HttpParams httpParameters = new BasicHttpParams();
            int connectionTimeout = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeout);

            int socketTimeout = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, socketTimeout);

//            HttpClient httpClient = new DefaultHttpClient(httpParameters);

            HttpClient httpClient = getNewHttpClient();

            HttpGet httpGet = new HttpGet(url);

            HttpResponse httpResponse = httpClient.execute(httpGet);

            String stringResponse = EntityUtils.toString(httpResponse.getEntity());
            Log.i(TAG, "slDeviceOTAAndType response: " + stringResponse);

            if (stringResponse.equals("appname=out_of_box_fs") ) {
                deviceTypeEnum = Device_Type_Enum.F_Device;

            } else if (stringResponse.contains("appname=out_of_box_rs")||(stringResponse.equals("appname=out_of_box"))) {
                deviceTypeEnum = Device_Type_Enum.S_Device;

            } else if (stringResponse.equals("appname=out_of_box_r")) {
                deviceTypeEnum = Device_Type_Enum.R_Device;

            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        Log.i(TAG, "slDeviceOTAAndType result: " + deviceTypeEnum);
        return deviceTypeEnum;


    }

    public static String getDeviceIp(String baseUrl) {

        String url = baseUrl;
        url += "/__SL_G_PIP";
//        String stringResponse = "";
        Log.i(TAG, "REDIRECT- getDeviceIp / url: " + url);
        Log.i(TAG, "REDIRECT- getDeviceIp / didRedirect: " + didRedirect);
        if (didRedirect) {
            url = HTTPS_ + url;
        } else {
            url = HTTP_ + url;
        }

        Log.i(TAG, "REDIRECT- getDeviceIp / url: " + url);

        String stringResponse = "";


        try {
            HttpParams httpParams = new BasicHttpParams();
            int connectionTimeout = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout);
            int socketTimeout = 5000;
            HttpConnectionParams.setSoTimeout(httpParams, socketTimeout);

//            HttpClient httpClient = new DefaultHttpClient(httpParams);

            HttpClient httpClient = getNewHttpClient();

            HttpGet httpGet = new HttpGet(url);

            HttpResponse httpResponse = httpClient.execute(httpGet);

            stringResponse = EntityUtils.toString(httpResponse.getEntity());

            Log.i(TAG, "getDeviceIp result: " + stringResponse);


        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringResponse;
    }


//    public static HttpClient getNewHttpClient() {
//        try {
//            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            trustStore.load(null, null);
//            MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
//            sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//
//            HttpParams params = new BasicHttpParams();
//            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
//            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
//
//            SchemeRegistry registry = new SchemeRegistry();
//            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
//            registry.register(new Scheme("https", sf, 443));
//
//            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
//            return new DefaultHttpClient(ccm, params);
//
//        } catch (Exception e) {
//            return new DefaultHttpClient();
//        }
//    }
private static HttpClient getNewHttpClient()  {
    try {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        HttpParams params = new BasicHttpParams();

        HttpConnectionParams.setConnectionTimeout(params, 5000);
        HttpConnectionParams.setSoTimeout(params, 25000);

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", sf, 443));

        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

        HttpConnectionParams.setConnectionTimeout(params, 5000);
        HttpConnectionParams.setSoTimeout(params, 25000);
        HttpClient client = new DefaultHttpClient(ccm, params);
//			if (NetState.Mobile == NetStateManager.CUR_NETSTATE) {
//				// ?????????APN???
//				HttpHost proxy = NetStateManager.getAPN();
//				if (null != proxy) {
//					client.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, proxy);
//				}
//			}
        return client;
    } catch (Exception e) {
        return new DefaultHttpClient();
    }
}

}
