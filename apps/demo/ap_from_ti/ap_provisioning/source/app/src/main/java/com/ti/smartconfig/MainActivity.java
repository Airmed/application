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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TabWidget;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ti.smartconfig.utils.Device_Type_Enum;
import com.ti.smartconfig.utils.Popup_;
import com.ti.smartconfig.utils.SharedPreferencesInterface_;
import com.ti.smartconfig.utils.WifiNetworkUtils;
import com.ti.smartconfig.utils.Constants;
import com.ti.smartconfig.utils.Device;
import com.ti.smartconfig.utils.MDnsCallbackInterface;
import com.ti.smartconfig.utils.MDnsHelper;
import com.ti.smartconfig.utils.Ping;
import com.ti.smartconfig.utils.Ping.PingCallback;
import com.ti.smartconfig.utils.NetworkUtil;
import com.ti.smartconfig.utils.Popup;
import com.ti.smartconfig.utils.SmartConfigConstants;
import com.ti.smartconfig.utils.Popup.PopUpCallback;
import com.ti.smartconfig.utils.Popup.PopupType;

import de.mindpipe.android.logging.log4j.LogConfigurator;

import com.ti.smartconfig.utils.NetInfo;
import com.ti.smartconfig.utils.UdpBcastServer;

@SuppressLint("NewApi")
@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.activity_main)
public class MainActivity extends FragmentActivity {

    private int popUpTries = 0;

    protected static final String TAG = "MainActivity";
    private Popup mPopup;
    private Dialog mDialog;
    private Device mMDNSDevice;
    private boolean firstRun = true;
    public WifiNetworkUtils mNetworkUtils;

    public boolean killMDNSBackgroundScan;
    public ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    public Lock readLock = readWriteLock.readLock();
    public Lock writeLock = readWriteLock.writeLock();

    int tabDimens; //the size of the tabs in pixels, initialized in afterViews

    String mStartingWifiNetwork;

    //the tabs
    DevicesFragment devicesFragment;
    SettingsFragment settingsFragment;
    NewSmartConfigFragment newSmartConfigFragment;
    DeviceConfiguration deviceConfiguration;
    LanFragment lanFragment;
    CloudFragment cloudFragment;
    OtaFragment otaFragment;
    public Logger mLogger;

    public Device_Type_Enum deviceTypeEnum;

    public boolean isWifiAlertEnabled = true; //flag for enabling the wifi connection dialog

    private boolean firstTime = true;

    public Thread udpBcastServerThread;

    public String slOriginalName;
    public String apName;
    public String apPass;
    public boolean isPassCheckChecked;
    public String iterationNumString;
    public String delayNumString;

    SharedPreferences sharedpreferences;
    public static final String mypreference = "iot";
    public static final String Name = "deviceIP";

    @Bean
    MDnsHelper mDnsHelper;

    MDnsCallbackInterface mDnsCallback;
    JSONArray devicesArray;

    protected NetInfo net = null;
    private long network_ip = 0;
    private long network_start = 0;
    private long network_end = 0;
    private Ping mPing;
    public UdpBcastServer udpBcastServer;


    @Pref
    SharedPreferencesInterface_ prefs;

    @ViewById
    FragmentTabHost tabhost;

    @ViewById
    FrameLayout real_tab_content;

    @ViewById
    TabWidget tabs;

    BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        /**
         * Upon receiving a broadcast (sticky - receives on registration) informing that a
         * change in network connectivity has occurred, determines WiFi connectivity status,
         * and starts/stops {@link com.ti.smartconfig.utils.MDnsHelper mDNS discovery} and
         * {@link com.ti.smartconfig.utils.Ping Ping} accordingly.
         * If not connected to wifi or connected to SL device, disables mDNS discovery and stops Ping.
         * If connected to wifi and not to an SL device, starts up mDNS discovery and Ping.
         *
         * @param context   Context. The Context in which the receiver is running
         * @param intent    Intent. The Intent being received
         *
         * @see MainActivity#stopPing() stopPing()
         * @see MainActivity#clear() clear()
         * @see MainActivity#scanForDevices() scanForDevices()
         * @see MainActivity#startPing() startPing()
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            int networkState = NetworkUtil.getConnectionStatus(context);
            if (networkState != NetworkUtil.WIFI && isWifiAlertEnabled && tabhost.getCurrentTab() == 0) {//no wifi connection and alert dialog allowed

                readLock.lock();
                try {
                    prefs.scanningDisable().put(true);
                } finally {
                    readLock.unlock();
                }
                stopPing();
            }
            if (networkState == NetworkUtil.WIFI && !isWifiAlertEnabled) // wifi connected and alert disabled
            {
                isWifiAlertEnabled = true; //re-enable alert
                clear();
                if (NetworkUtil.getConnectedSSID(context).contains(Constants.DEVICE_PREFIX)) {
                    readLock.lock();
                    try {

                        prefs.scanningDisable().put(true);
                    } finally {
                        readLock.unlock();
                    }
                    stopPing();
                } else {
                    readLock.lock();
                    try {

                        prefs.scanningDisable().put(false);
                    } finally {
                        readLock.unlock();
                    }
                    scanForDevices();
                    startPing();
                }
            }
        }
    };

    /**
     * Terminates UDP broadcast server asynchronous task if one exists, and starts a new one.
     * The {@link com.ti.smartconfig.utils.UdpBcastServer UDP broadcast server} is responsible for receiving UDP packets publishing the
     * configured SL device's newly acquired IP address to the network.
     */
    protected void restartUdp() {//i-udp are the bcast packets the SL device sends with it's acquired IP to publish it

        mLogger.info("UDPBcastServer - restarted");
        if (udpBcastServer != null) {//i-if the udpServer AST variable is not null, cancel and nullify its variable
//			udpBcastServer.cancel(true);
            udpBcastServerThread.interrupt();
            udpBcastServer = null;
        }

        udpBcastServer = new UdpBcastServer(mPingCallback);

        udpBcastServerThread = new Thread(udpBcastServer.udpBcastServerRunnable);
        udpBcastServerThread.start();

//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//			udpBcastServer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//			} else {
//				udpBcastServer.execute();
//			}
    }


    /**
     * Called when the activity is first created, adds flag to window to keep the mobile device's screen on as long as window is visible,
     * gets a populated instance of {@link com.ti.smartconfig.utils.WifiNetworkUtils#getInstance(Context) WifiNetworkUtils},
     * sets the isScanning and scanningDisable shared preferences to false
     * and creates a new {@link com.ti.smartconfig.utils.NetInfo#NetInfo(Context) NetInfo} object.
     *
     * @param savedInstanceState Bundle. Dynamic instance state of the Activity saved in
     *                           {@link android.app.Activity#onSaveInstanceState(Bundle) onSaveInstanceState(Bundle)},
     *                           or null if none was saved.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //restated the keep screen on
        //6.2
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mNetworkUtils = WifiNetworkUtils.getInstance(this);
        prefs.isScanning().put(false);
        prefs.scanningDisable().put(false);
        //ctxt = getApplicationContext();
        net = new NetInfo(this);
//        prefs.enableSmartConfig().put(true);

        sharedpreferences = this.getSharedPreferences(mypreference,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Name,"");
        editor.commit();
    }

    /**
     * Called after views binding has happened, if on application's first run shows Toast indicating current WiFi connectivity status,
     * {@link #setupLogger() sets up Logger}, {@link android.support.v4.app.FragmentTabHost#setup(Context, FragmentManager, int) sets up FragmentTabHost}
     * and adds tabs to it, displays appropriate tab according to user's settings, clears the shared preferences devicesArray and recentDevicesArray,
     * {@link MainActivity#initMDns() initiates mDNS} and {@link MainActivity#restartUdp() restarts UDP}, gets startingWifiNetwork
     * (which the mobile device is currently connected to) for later use, enables scanning and starts mDNS scan,
     * and shows help screen if necessary according to user's settings.
     *
     * @see MainActivity#initTabs(int) initTabs(int)
     * @see MainActivity#scanForDevices() scanForDevices()
     * @see MainActivity#showHelpScreen() showHelpScreen()
     */
    @AfterViews
    void afterViews() {
//		nsdHelper = new NsdHelper(this);
//		nsdHelper.initializeNsd();
//		nsdHelper.registerService();
//     	nsdHelper.discoverServices();

        killMDNSBackgroundScan = false;
        //throwing a toast on app first run and let the user know if he is connected to wifi or not, and which wifi he is connected to
        if (firstRun) {

            ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiConnectionInfo = wifiManager.getConnectionInfo();
                String ssid = wifiConnectionInfo.getSSID();
                if (ssid != null && !ssid.equals("")) {
                    Toast.makeText(getApplicationContext(), "Connected to Wi-fi: " + ssid, Toast.LENGTH_LONG).show();
                }
            }
//			String ssid= mNetworkUtils.getConnectedSSID();
            else {
                Toast.makeText(getApplicationContext(), "No Wi-Fi Connection", Toast.LENGTH_LONG).show();
            }
            firstRun = false;
        }

        setupLogger();


        newSmartConfigFragment = new NewSmartConfigFragment_();
        devicesFragment = new DevicesFragment_();
        settingsFragment = new SettingsFragment_();
        deviceConfiguration = new DeviceConfiguration_();
        lanFragment = new LanFragment_();
        cloudFragment = new CloudFragment_();
        otaFragment = new OtaFragment_();
        tabDimens = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SmartConfigConstants.TAB_DIMENS, getResources().getDisplayMetrics());

        tabhost.setup(this, getSupportFragmentManager(), real_tab_content.getId());

        tabhost.setOnTabChangedListener(new OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                mNetworkUtils.clearCallback();
            }
        });

        initTabs(prefs.startTab().get());
        devicesArray = new JSONArray();
        if (!prefs.isScanning().get()) {
            prefs.devicesArray().put("[]");
        }
        prefs.recentDevicesArray().put("[]");
        initMDns();

        restartUdp();

        int networkState = NetworkUtil.getConnectionStatus(this);
        if (networkState == NetworkUtil.WIFI) { //no wifi connection and alert dialog allowed
            mStartingWifiNetwork = NetworkUtil.getConnectedSSID(this);
            readLock.lock();
            try {

                prefs.scanningDisable().put(false);
            } finally {
                readLock.unlock();
            }
            scanForDevices();
            //initPing();
            //startPing();
        }

        if (!prefs.doNotShowHelpAgain().get()) {
            showHelpScreen();
        }
    }

    /**
     * Stops mDNS scan (and restarts it), and clears devicesArray in shared preferences and in Main.
     *
     * @see MainActivity#stopScanning() stopScanning()
     */
    public void clear() {//i-method that stops the mDNS discovery scan, and if the scan is indeed not in progress clears the devices array and saves the fact the devices array is empty to shared prefs
        mLogger.info("Stop mDNS discovery and clear devices and recent devices arrays and prefs");
        Log.w(TAG, "scanForDevices stop scanning mDNS");
        stopScanning();
        if (!prefs.isScanning().get()) {
            prefs.devicesArray().put("[]");
        }
//		prefs.recentDevicesArray().put("[]");
        devicesArray = new JSONArray();
    }

    /**
     * Called before the Activity is destroyed, stops Ping.
     *
     * @see MainActivity#stopPing() stopPing()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();


        stopPing();
    }

    /**
     * Called when the activity starts interacting with the user, disables mobile data (3G),
     * registers {@link com.ti.smartconfig.utils.WifiNetworkUtils#mWifiConnectionReceiver mWifiConnectionReceiver} and
     * {@link com.ti.smartconfig.utils.WifiNetworkUtils#mSupplicantStateReceiver mSupplicantStateReceiver} BroadcastReceivers,
     * sets killMDNSBackgroundScan to false and registers {@link MainActivity#networkChangeReceiver networkChangeReceiver} BroadcastReceiver.
     *
     * @see WifiNetworkUtils#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        mNetworkUtils.onResume();

        killMDNSBackgroundScan = false;
        registerReceiver(networkChangeReceiver, new IntentFilter(SmartConfigConstants.NETWORK_CHANGE_BROADCAST_ACTION));

		/*
		ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		if (NetworkUtil.isLollipopAndUp()) {
			//Deals with network callback for preferred WIFI network
			NetworkUtil.registerNetworkCallback(cm);
		}
		*/
    }

    /**
     * Called when the activity is going into the background, but has not (yet) been killed, sets killMDNSBackgroundScan to true,
     * sets mobile data (3G) to it's original state - enabled/disabled, and unregisters
     * {@link com.ti.smartconfig.utils.WifiNetworkUtils#mWifiConnectionReceiver mWifiConnectionReceiver},
     * {@link com.ti.smartconfig.utils.WifiNetworkUtils#mSupplicantStateReceiver mSupplicantStateReceiver} and
     * {@link MainActivity#networkChangeReceiver networkChangeReceiver} BroadcastReceivers.
     *
     * @see WifiNetworkUtils#onPaused()
     */
    @Override
    public void onPause() {
        super.onPause();
        killMDNSBackgroundScan = true;
        mNetworkUtils.onPaused();
        unregisterReceiver(networkChangeReceiver);
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancel();
            mDialog = null;
        }
    }

    /**
     * Called upon a "Settings" menu item selection event,
     * if SmartConfig is active shows Toast asking to stop SC process before leaving tab,
     * otherwise sets the "Settings" tab as the tab displayed.
     */
    @OptionsItem(R.id.menu_settings)
    void settings() {
        if (prefs.isSmartConfigActive().get()) {
            Toast.makeText(this, "Please stop SmartConfig process before leaving this tab", Toast.LENGTH_SHORT).show();
        } else {
            tabhost.setCurrentTab(2);
        }
    }

    /**
     * Adds tabs to {@link android.support.v4.app.FragmentTabHost FragmentTabHost} and sets the tab to be displayed, according to user's settings.
     *
     * @param currentTab int. The tab to be displayed
     * @see android.support.v4.app.FragmentTabHost#addTab(TabHost.TabSpec, Class, Bundle)
     * @see android.support.v4.app.FragmentTabHost#newTabSpec(String)
     * @see android.widget.TabHost.TabSpec#setIndicator(View)
     * @see MainActivity#makeTabIndicator(Drawable)
     * @see android.support.v4.app.FragmentTabHost#setCurrentTab(int)
     */
    public void initTabs(int currentTab) {
        try {

            if (!prefs.enableSmartConfig().get()) {
                tabhost.addTab(tabhost.newTabSpec("deviceConfiguration").setIndicator(makeTabIndicator(getResources().getDrawable(R.drawable.tab_approvision_selector))),
                        deviceConfiguration.getClass(), null);
            } else {
                tabhost.addTab(tabhost.newTabSpec("smartconfig").setIndicator(makeTabIndicator(getResources().getDrawable(R.drawable.tab_approvision_selector))),
                        newSmartConfigFragment.getClass(), null);
            }

            tabhost.addTab(tabhost.newTabSpec("devices").setIndicator(makeTabIndicator(getResources().getDrawable(R.drawable.tab_devices_selector))),
                    devicesFragment.getClass(), null);

            tabhost.addTab(tabhost.newTabSpec("lan").setIndicator(makeTabIndicator(getResources().getDrawable(R.drawable.lan_selector))),
                    lanFragment.getClass(), null);

            tabhost.addTab(tabhost.newTabSpec("ota").setIndicator(makeTabIndicator(getResources().getDrawable(R.drawable.ota_selector))),
                    otaFragment.getClass(), null);

            tabhost.addTab(tabhost.newTabSpec("settings").setIndicator(makeTabIndicator(getResources().getDrawable(R.drawable.tab_settings_selector))),
                    settingsFragment.getClass(), null);

            tabhost.getTabWidget().getChildAt(2).setVisibility(View.GONE);

            tabhost.getTabWidget().getChildAt(3).setVisibility(View.GONE);


            if (deviceTypeEnum != null) {

                switch (deviceTypeEnum) {

                    case F_Device:
                    case S_Device:
                    case R_Device:

                        tabhost.getTabWidget().getChildAt(2).setVisibility(View.VISIBLE);

                        tabhost.getTabWidget().getChildAt(3).setVisibility(View.VISIBLE);

                        break;

                }

            }

            tabhost.setCurrentTab(currentTab);
        }catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes all tabs from the tab widget associated with this tab host,
     * re-adds tabs to it, and sets the tab to be displayed, according to the tabNumber passed as parameter.
     *
     * @param change    Boolean. Unused
     * @param tabNumber int. Representing the tab to be displayed
     * @see FragmentTabHost#clearAllTabs()
     * @see MainActivity#initTabs(int) initTabs(int)
     */
    public void changeToAP(Boolean change, int tabNumber) {
        tabhost.clearAllTabs();
        initTabs(tabNumber);
    }

    public void clearAllTabs() {
        tabhost.clearAllTabs();
    }

    /**
     * Sets the Device passed as parameter as the mMDNSDevice, removes all tabs from the tab widget
     * associated with the tab host, re-adds tabs to it, and sets the "Devices" tab as the tab to be displayed.
     *
     * @param device Device. the SimpleLink device being provisioned, found in the added profile network
     * @see MainActivity#setMDNSDevice(Device) setMDNSDevice(Device)
     * @see FragmentTabHost#clearAllTabs()
     * @see MainActivity#initTabs(int) initTabs(int)
     */
    public void changeToDevices(Device device) {
        setMDNSDevice(device);
        tabhost.clearAllTabs();
        initTabs(1);
    }

    public void EnableOutOfTheBoxTabs(Boolean bool) {
        //tabhost.clearAllTabs();
//		tabhost.getTabWidget().getChildTabViewAt(3).setEnabled(bool);
//		tabhost.getTabWidget().getChildTabViewAt(2).setEnabled(bool);
        tabhost.getTabWidget().getChildAt(2).setVisibility(View.VISIBLE);
        tabhost.getTabWidget().getChildAt(3).setVisibility(View.VISIBLE);
    }

    /**
     * Creates a view to be used as an indicator for a tab.
     *
     * @param drawable Drawable. The image representing the tab, to be displayed in the view
     * @return View. The view to be used as the tab indicator
     */
    public View makeTabIndicator(Drawable drawable) {
        ImageView tabImage = new ImageView(this);
        LayoutParams LP;
        LP = new LayoutParams(tabDimens, tabDimens, 1);
        LP.setMargins(1, 0, 1, 0);
        tabImage.setLayoutParams(LP);
        tabImage.setImageDrawable(drawable);
        tabImage.setBackgroundColor(Color.TRANSPARENT);
        return tabImage;
    }


    /**
     * Enables Wi-Fi and starts the mobile device's Wi-Fi settings activity.
     */
    public void activateWifi() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    /**
     * Starts the activity displaying the Texas Instruments terms of use web page.
     */
    public void startTermsOfUse() {
        Intent intent = new Intent(this, TermsOfUseActivity.class);
        startActivity(intent);

    }

    /**
     * Starts the activity displaying the exas Instruments privacy policy web page.
     */
    public void startPrivacyPolicy() {
        Intent intent = new Intent(this, PrivacyPolicyActivity_.class);
        startActivity(intent);
    }

    /**
     * Displays the fragment containing the Help screen, with transition animation
     */
    public void showHelpScreen() {
        HelpFragment fragment = new HelpFragment_();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_in);
        transaction.add(android.R.id.content, fragment);
        transaction.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();
    }

    /**
     * Initiates the Ping broadcast process, creates a Ping AsyncTask and sets the IP address to ping.
     *
     * @return boolean. False if not connected to Wi-Fi, true otherwise .
     */
    public boolean initPing() {
        stopPing();
        int networkState = NetworkUtil.getConnectionStatus(this);
        if (networkState == NetworkUtil.WIFI) {
            //network_ip = NetInfo.getUnsignedLongFromIp(net.ip);
            net.getWifiInfo();
            while (net.gatewayIp.equalsIgnoreCase("0.0.0.0")) {
                Log.i(TAG, "in while - network ip: " + net.gatewayIp);
                try {
                    //changed sleep time to 500 instead of 200
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                net.getWifiInfo();
            }
            Log.i(TAG, "after while network ip: " + net.gatewayIp);
            String ipToPing = "";
            String[] parts = net.gatewayIp.split("\\.");
            for (int i = 0; i < 3; i++) {
                ipToPing += parts[i] + ".";
            }
            mPing = new Ping(mPingCallback, net.gatewayIp);
            mPing.ipToPing = ipToPing + "255";
            Log.i(TAG, "Will ping ip: " + mPing.ipToPing);
            return true;
            //mPing.working=true;
            //		new Ping(mPingCallback).execute();
            //		mPing.execute();
            //		mPing.getAvgResponseTime("20.2.2.255");
        }
        return false;
    }

    /**
     * Starts the Ping broadcast process, if Ping is initiated successfully and not already working.
     *
     * @see MainActivity#initPing() initPing()
     */
    public void startPing() {

        if (mPing != null && mPing.working) {
            return;
        }
        if (initPing()) {

            try {

                Thread pingThread = new Thread(mPing.pingRunnable);
                pingThread.start();

            } catch (NullPointerException e) {
                e.printStackTrace();
            }

//			mPing.pingInBackground();

//				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                mPing.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//				} else {
//					mPing.execute();
//				}
//			}


        }
    }

    /**
     * Stops the Ping broadcast process, if it is working.
     * Destroys the process that was running the Ping and sets the Ping's boolean variable tracking it's working state to false.
     *
     * @see Ping#stopPing()
     */
    public void stopPing() {
        if (mPing != null && mPing.working) {
            mPing.stopPing();
            mLogger.info("Ping - stopped");
        }
        mPing = null;
    }

    /**
     * Implementation of the PingCallback Interface, includes pingCompleted, pingDeviceFetched(JSONObject) and pingFailed(String).
     *
     * @see PingCallback#pingCompleted()
     * @see PingCallback#pingDeviceFetched(JSONObject)
     * @see PingCallback#pingFailed(String)
     */
    private PingCallback mPingCallback = new PingCallback() {
        /**
         * Logs the completion of the Ping broadcast process.
         */
        @Override
        public void pingCompleted() {
            Log.i(TAG, "PingOrBcastCallback - Completed");
            mLogger.info("PB - Completed");
        }

        /**
         * Called upon a discovery of a simpleLink device, logs the discovery, re-populates the array containing
         * the discovered devices to reflect this discovery, updates the shared preferences with this array,
         * and broadcasts the discovered device to registered broadcast receivers.
         *
         * @param deviceJSON    JSONObject. Representing the simpleLink device found.
         */
        @Override
        public void pingDeviceFetched(JSONObject deviceJSON) {
            Log.i(TAG, "SL Device was found via PING or Bcast : " + deviceJSON);
            mLogger.info("SL Dev found PB: " + deviceJSON);
            // omer: check if device already exist
            boolean addToDeviceList = true;
            JSONArray updateDevicesArray = new JSONArray();
            updateDevicesArray.put(deviceJSON);

            for (int i = 0; i < devicesArray.length(); i++) { // populate the list
                try {
                    JSONObject deviceJSON1 = devicesArray.getJSONObject(i);
                    if (!(deviceJSON1.getString("host").equalsIgnoreCase(deviceJSON.getString("host")))) {
                        for (int j = 0; j < updateDevicesArray.length(); j++) { // populate the list
                            try {
                                JSONObject updateDeviceJSON = updateDevicesArray.getJSONObject(j);
                                if (updateDeviceJSON.getString("host").equalsIgnoreCase(deviceJSON1.getString("host"))) {
                                    addToDeviceList = false;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (addToDeviceList) {
                            Log.i(TAG, "Add Device from list: " + deviceJSON1);
                            updateDevicesArray.put(deviceJSON1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "Broadcasting d found by PB to app : " + deviceJSON);
            devicesArray = updateDevicesArray;
            prefs.devicesArray().put(devicesArray.toString());
            Intent intent = new Intent();
            intent.putExtra("newDevice", deviceJSON.toString());
            intent.setAction(SmartConfigConstants.DEVICE_FOUND_BROADCAST_ACTION);
            sendBroadcast(intent);
        }

        /**
         * Logs the failure of the Ping broadcast process.
         *
         * @param errorMessage    String. The message connected to the error that caused the Ping to fail.
         */
        @Override
        public void pingFailed(String errorMessage) {
            Log.i(TAG, "PingCallback - pingFailed");
            mLogger.info("PB - failed");
        }
    };

    /**
     * Initiates the mDNS process, populating it's fields, and implements
     * the MDnsCallbackInterface Interface including onDeviceResolved(JSONObject).
     *
     * @see com.ti.smartconfig.utils.MDnsCallbackInterface#onDeviceResolved(JSONObject)
     * @see com.ti.smartconfig.utils.MDnsHelper#init(Activity, MDnsCallbackInterface)
     */
    public void initMDns() {
        mDnsCallback = new MDnsCallbackInterface() {
            /**
             * Called upon a discovery of a simpleLink device, logs the discovery, and if scanning is not disabled
             * re-populates the array containing the discovered devices to reflect this discovery,
             * updates the shared preferences with this array, and broadcasts the discovered device to registered broadcast receivers.
             *
             * @param deviceJSON    JSONObject. Representing the simpleLink device found.
             */
            @Override
            public void onDeviceResolved(JSONObject deviceJSON) {
                Log.i(TAG, "Device was found via mDNS : " + deviceJSON);
                mLogger.info("SL Dev found by M: " + deviceJSON);



                if ((prefs.scanningDisable().get()) && (!prefs.isSmartConfigActive().get())) {
                    Log.i(TAG, "Device was found via mDNS : " + deviceJSON + " but scanning is disabled");
                    return;
                }
                // omer: check if device already exist
                boolean addToDeviceList = true;
                JSONArray updateDevicesArray = new JSONArray();
                updateDevicesArray.put(deviceJSON);

                for (int i = 0; i < devicesArray.length(); i++) { // populate the list
                    try {
                        JSONObject deviceJSON1 = devicesArray.getJSONObject(i);
                        if (!(deviceJSON1.getString("host").equalsIgnoreCase(deviceJSON.getString("host")))) {
                            for (int j = 0; j < updateDevicesArray.length(); j++) { // populate the list
                                try {
                                    JSONObject updateDeviceJSON = updateDevicesArray.getJSONObject(j);
                                    if (updateDeviceJSON.getString("host").equalsIgnoreCase(deviceJSON1.getString("host"))) {
                                        addToDeviceList = false;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (addToDeviceList) {
                                ///////Toast.makeText(getApplicationContext(),"***************",Toast.LENGTH_LONG).show();
                                Log.i(TAG, "Add Device from list: " + deviceJSON1);
                                updateDevicesArray.put(deviceJSON1);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                devicesArray = updateDevicesArray;
                prefs.devicesArray().put(devicesArray.toString());
                Intent intent = new Intent();
                intent.putExtra("newDevice", deviceJSON.toString());
                intent.setAction(SmartConfigConstants.DEVICE_FOUND_BROADCAST_ACTION);
                sendBroadcast(intent);
            }
        };
        //	nsdHelper(this,mDnsCallback);
        mDnsHelper.init(this, mDnsCallback);
        //	nsdHelper = new NsdHelper(this,mDnsCallback)
    }

    /**
     * Handles the mDNS discovery cycle, runs on a background thread, separate from the UI thread.
     * Starts the Ping process. If the application is not in the background, and scanning is not
     * already in progress or disabled (if it is in progress only logs that fact),
     * sets shared preferences isScanning to true, refreshes the array containing the found
     * simpleLink devices by removing devices that have been in the array for a certain amount
     * of calls of this method, sends a broadcast to registered broadcast receivers in case
     * the array has been changed, and unless smartConfig is active starts mDNS discovery.
     * Finally if scanning is in progress and not disabled, logs the end of the mDNS scan,
     * broadcasts the scan's termination to registered broadcast receivers,
     * sets the shared preferences isScanning to false,
     * and calls this method recursively.
     *
     * @see MainActivity#startPing() startPing()
     * @see MDnsHelper#startDiscovery()
     * @see MDnsHelper#restartDiscovery()
     */
    @Background

    public void scanForDevices() {

        startPing();

        //killMDNSBackgroundScan responsible to disable mdns scans when the app is in background (solving cpu problem)
        if (!killMDNSBackgroundScan) {
            if (!prefs.isScanning().get() && !prefs.scanningDisable().get()) {
                readLock.lock();
                try {
                    prefs.isScanning().put(true);
                } finally {
                    readLock.unlock();
                }
                JSONArray updateDevicesArray = new JSONArray();
                boolean refreshList = false;
                for (int i = 0; i < devicesArray.length(); i++) { // populate the list
                    try {
                        JSONObject deviceJSON = devicesArray.getJSONObject(i);
                        int age = deviceJSON.getInt("age");
                        if (age < 5) {
                            deviceJSON.remove("age");
                            deviceJSON.put("age", age + 1);
                            Log.i(TAG, "Add item:" + deviceJSON);
                            updateDevicesArray.put(deviceJSON);
                        } else {
                            Log.i(TAG, "Should remove item:" + i);
                            // should refresh list
                            refreshList = true;//i-flag list as eligible for refreshing
                            //devicesArray.remove(i);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Log.i(TAG, "update Devices Array length:" + updateDevicesArray.length());
                devicesArray = updateDevicesArray;
                writeLock.lock();
                try {
                    prefs.devicesArray().put(devicesArray.toString());
                } catch (ConcurrentModificationException e) {
                    e.printStackTrace();
                } finally {
                    writeLock.unlock();
                }
                if (refreshList) {
                    Intent intent = new Intent();
                    intent.setAction(SmartConfigConstants.DEVICE_FOUND_BROADCAST_ACTION);
                    sendBroadcast(intent);
                }
                try {
                    //////////////mLogger.info("*M START *");
                    Log.w(TAG, "Start mDNS discovery");
                    if (prefs.isSmartConfigActive().get()) {
                        Log.w(TAG, "SmartConfig in action -  no mDNS discovery");
//					Thread.sleep(SmartConfigConsMtants.MAIN_SCAN_TIME*2);
                    } else {
                        Log.w(TAG, "SmartConfig not in action -  start mDNS discovery");
                        if (firstTime) {
                            firstTime = false;
                            mDnsHelper.startDiscovery();
                        } else {
                            //	mDnsHelper.startDiscovery();
                            mDnsHelper.restartDiscovery();
                        }
                        Thread.sleep(SmartConfigConstants.MAIN_SCAN_TIME);
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Failed to sleep during mDNS discovery");
                    e.printStackTrace();
                } finally {
                    if (prefs.isScanning().get() && !prefs.scanningDisable().get()) {
                        Log.i(TAG, "Stopping mDNS discovery from scanForDevices");
                        Log.w(TAG, "scanForDevices stop scanning mDNS");
                        Log.i(TAG, "Stop mDNS scan notification");
                        Intent intent = new Intent();
                        intent.setAction(SmartConfigConstants.SCAN_FINISHED_BROADCAST_ACTION);
                        sendBroadcast(intent);
                        readLock.lock();
                        try {
                            prefs.isScanning().put(false);
                        } finally {
                            readLock.unlock();
                        }
                        scanForDevices();
//					if (prefs.isSmartConfigActive().get()==false) {
//						stopScanning();
//					}
                    }
                }
            } else {
                Log.i(TAG, "already mDNS discovery");
//			try {
//				Thread.sleep(SmartConfigConstants.SPLASH_SCREEN_TIME);
//			}catch (InterruptedException e) {
//				Log.e(TAG, "Failed to sleep during mDNS discovery");
//				e.printStackTrace();
//			}
//			scanForDevices();
            }
        }
    }

    /**
     * Handles the termination of the mDNS discovery, runs on a background thread, separate from the UI thread.
     * Logs the termination of the mDNS discovery and if smartConfig is not active terminates mDNS discovery.
     * Finally broadcasts the scan's termination to registered broadcast receivers,
     * sets shared preferences isScanning to false, and calls scanForDevices to handle the mDNS discovery cycle.
     *
     * @see MDnsHelper#stopDiscovery()
     * @see MainActivity#scanForDevices() scanForDevices()
     */
    @Background
    public void stopScanning() {
        if (true) {////prefs.isScanning().get()==true && prefs.scanningDisable().get() == false) {
            try {
                mLogger.info("*M STOP *");
                if (!prefs.isSmartConfigActive().get()) {
                    firstTime = true;
                    mDnsHelper.stopDiscovery();
                    Thread.sleep(SmartConfigConstants.JMDNS_CLOSE_TIME);
                } else {
                    mLogger.info("Smartconf in action - on rx only");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to sleep during mDNS stop");
                e.printStackTrace();
            } finally {
                Log.i(TAG, "Stop mDNS scan notification");
                Intent intent = new Intent();
                intent.setAction(SmartConfigConstants.SCAN_FINISHED_BROADCAST_ACTION);
                sendBroadcast(intent);
                readLock.lock();
                try {
                    prefs.isScanning().put(false);
                } finally {
                    readLock.unlock();
                }
                scanForDevices();
            }
        } else {
            readLock.lock();
            try {
                prefs.isScanning().put(false);
            } finally {
                readLock.unlock();
            }
        }
    }

    /**
     * Creates and shows a custom Dialog.
     * If a dialog already exists and is showing, cancels and nullifies that dialog.
     * Implements the PopUpCallback interface (cancelling the dialog upon any dialog button click),
     * starts the Popup which sets the dialog's text, image, callback and type.
     * creates a Dialog object and sets the Popup as it's content view,
     * makes the dialog Object cancellable and sets a window fade-in animation to it,
     * and displays the dialog.
     * If the dialog failed to be displayed logs this fact and calls a method which determines whether
     * the mobile device is connected to whichever network it is meant to be connected to according to the
     * specific stage of the process it is in (whether it be the Wi-Fi network the mobile device was initially
     * connected to, the Wi-Fi network sent to the simpleLink device as the profile to connect to or the
     * simlpleLink device itself in AP mode), and calls the appropriate callback method -
     * failedToConnectToNetwork(WifiConnectionFailure)
     * or successfullyConnectedToNetwork(String).
     *
     * @param mainText        String. The text displayed in the body of the dialog.
     * @param leftButtonText  String. The text displayed on the dialog's left button.
     * @param rightButtonText String. The text displayed on the dialog's right button.
     * @param popupType       PopupType. The type of the dialog (failure/information/success)
     *                        determining the image displayed in the dialog.
     * @param callback        PopupCallback. The interface outlining the methods that must be
     *                        implemented in order to react to the dialog's buttons being tapped.
     * @param headline        String.The text displayed in the headline of the dialog.
     * @see PopUpCallback#popupRightButtonTapped()
     * @see PopUpCallback#popupLeftButtonTapped()
     * @see com.ti.smartconfig.utils.Popup#start(String, String, String, PopupType, PopUpCallback, String)
     * @see WifiNetworkUtils#timeoutDialogFinished()
     * @see com.ti.smartconfig.utils.WifiNetworkUtils.BitbiteNetworkUtilsCallback#failedToConnectToNetwork(WifiNetworkUtils.WifiConnectionFailure)
     * @see com.ti.smartconfig.utils.WifiNetworkUtils.BitbiteNetworkUtilsCallback#successfullyConnectedToNetwork(String)
     */
    @UiThread
    public void showSuccessDialog(String mainText, String leftButtonText, String rightButtonText, PopupType popupType, PopUpCallback callback, String headline) {

        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancel();
            mDialog = null;
        }

        mPopup = Popup_.build(this);

        if (callback == null) {
            callback = new PopUpCallback() {
                @Override
                public void popupRightButtonTapped() {
                    mDialog.cancel();
                }

                @Override
                public void popupLeftButtonTapped() {
                    mDialog.cancel();
                }
            };
        }

        mPopup.start(mainText, leftButtonText, rightButtonText, popupType, callback, headline);
        mDialog = new Dialog(this, R.style.ThemeDialogCustom);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(mPopup);
        mDialog.setCancelable(true);
        mDialog.getWindow().getAttributes().windowAnimations = R.anim.fragment_fade_in;

        try {
            mDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            mLogger.error("Failed to show success popup: " + e.getMessage());
            mNetworkUtils.timeoutDialogFinished();
        }
    }

    /**
     * Creates and shows an AlertDialog informing the user of a connection failure and enabling an option to connect manually.
     * In case the user chooses the manual option (dialog's positive button) the mobile device's Wi-Fi
     * capabilities are enabled and the Wi-Fi settings activity is started.
     * If the user chooses to cancel the dialog (dialog's neutral button) or if the dialog fails to be displayed
     * a method is called which determines whether the mobile device is connected to whichever network
     * it is meant to be connected to according to the specific stage of the process it is in
     * (whether it be the Wi-Fi network the mobile device was initially connected to, the Wi-Fi network sent
     * to the simpleLink device as the profile to connect to or the simlpleLink device itself in AP mode),
     * and calls the appropriate callback method -
     * failedToConnectToNetwork(WifiConnectionFailure)
     * or successfullyConnectedToNetwork(String).
     *
     * @param configuration WifiConfiguration. The configuration the mobile device was meant to connect to.
     * @param wifiManager   WifiManger. Used to enable the mobile device's Wi-Fi capabilities, if necessary.
     * @see WifiNetworkUtils#timeoutDialogFinished()
     * @see com.ti.smartconfig.utils.WifiNetworkUtils.BitbiteNetworkUtilsCallback#failedToConnectToNetwork(WifiNetworkUtils.WifiConnectionFailure)
     * @see com.ti.smartconfig.utils.WifiNetworkUtils.BitbiteNetworkUtilsCallback#successfullyConnectedToNetwork(String)
     */
    @UiThread
    public void showTimeoutPopup(final WifiConfiguration configuration, final WifiManager wifiManager) {

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {

                if (configuration.SSID == null) {
                    configuration.SSID = "";
                }

                AlertDialog wifiDialog = new AlertDialog.Builder(MainActivity.this). //create a dialog
                        setCancelable(false).
                        setTitle("Connection Failure").
                        setMessage("Failed to connect to " + configuration.SSID + ".").
                        setPositiveButton("Connect Manually", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) { //the user clicked yes
                                if (!wifiManager.isWifiEnabled())
                                    wifiManager.setWifiEnabled(true);

                                startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), Constants.WIFI_TIMEOUT_FAILURE);
                            }
                        }).setNeutralButton("Cancel", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mNetworkUtils.timeoutDialogFinished();
                    }
                }).create();

                try {
                    mLogger.info("Showing the timeout popup");
                    wifiDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                    mLogger.error("Failed to show timeout popup: " + e.getMessage());

                    popUpTries++;
                    if (popUpTries < 3) {
                        showTimeoutPopup(configuration, wifiManager);
                    } else {
                        popUpTries = 0;
                        mNetworkUtils.timeoutDialogFinished();
                    }
                }
            }
        }, 1500);
    }

    /**
     * Receives results associated with the WIFI_TIMEOUT_FAILURE request code from the Wi-Fi settings activity,
     * calls a method which determines whether the mobile device is connected to whichever network
     * it is meant to be connected to according to the specific stage of the process it is in
     * (whether it be the Wi-Fi network the mobile device was initially connected to, the Wi-Fi network sent
     * to the simpleLink device as the profile to connect to or the simlpleLink device itself in AP mode),
     * and calls the appropriate callback method -
     * failedToConnectToNetwork(WifiConnectionFailure)
     * or successfullyConnectedToNetwork(String).
     *
     * @param resultCode   int. The result code returned from the child activity.
     * @param returnIntent Intent. Containing the result data.
     * @see WifiNetworkUtils#timeoutDialogFinished()
     * @see com.ti.smartconfig.utils.WifiNetworkUtils.BitbiteNetworkUtilsCallback#failedToConnectToNetwork(WifiNetworkUtils.WifiConnectionFailure)
     * @see com.ti.smartconfig.utils.WifiNetworkUtils.BitbiteNetworkUtilsCallback#successfullyConnectedToNetwork(String)
     */
    @OnActivityResult(Constants.WIFI_TIMEOUT_FAILURE)
    void onSettingsResult(int resultCode, Intent returnIntent) {
        mNetworkUtils.timeoutDialogFinished();
    }

    /**
     * Sets up a Logger. Sets the path to the file which will contain the logs, the root level, the level,
     * the file pattern, the maximum file size (two megabytes in order to allow for it to be portable via e-mail),
     * creates a Logger Object, and logs the application's creation.
     */
    private void setupLogger() {
        LogConfigurator logConfigurator = new LogConfigurator();
        logConfigurator.setFileName(Constants.LOG_PATH);
        logConfigurator.setRootLevel(Level.DEBUG);
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.setFilePattern("%d %-5p [%c{2}]-[%L] %m%n");
        logConfigurator.setMaxFileSize(1024 * 1024 * 2); // limit 2M for emails
        logConfigurator.setMaxBackupSize(5);
        logConfigurator.setImmediateFlush(true);
        logConfigurator.configure();

        mLogger = Logger.getLogger(MainActivity.class);
        mLogger.info("***** Application Created *****");
    }

    /**
     * Creates and shows an AlertDialog informing the user of data flowing via the mobile network that should be turned off,
     * and allows the user quick access to the relevant settings.
     */
    @UiThread
    public void show3GDialog() {

        String msg;
        String buttonText;
        final Intent intent;
        if (WifiNetworkUtils.isLollipopAndUp()) {
            msg = "You are connected to the desired network, but the data is flowing via your mobile network. Please turn off your mobile data and try again.\n\n(Settings -> Data Usage -> Mobile Data, Turn it off)";
            buttonText = "Settings";
            intent = new Intent(Settings.ACTION_SETTINGS);
        } else {
            msg = "You are connected to the desired network, but the data is flowing via your mobile network. Please turn off your mobile data and try again.";
            buttonText = "Mobile Settings";
            intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
        }

        AlertDialog dialog = new AlertDialog.Builder(this).
                setCancelable(false).
                setTitle("Wifi Connection Issue").
                setMessage(msg).
                setPositiveButton(buttonText, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(intent, Constants.WIFI_3G_FAILURE);
                    }
                }).
                setNeutralButton("Close", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();
        dialog.show();
    }

    /**
     * Sets the Device passed as a parameter as the mMDNSDevice.
     *
     * @param device Device. The device to be set as the mMDNSDevice.
     */
    public void setMDNSDevice(Device device) {
        mMDNSDevice = device;
    }

    /**
     * Returns the current mMDNSDevice.
     *
     * @return mMDNSDevice.
     */
    public Device getMDNSDevice() {
        return mMDNSDevice;
    }


    @UiThread
    void showToastWithMessage(final String msg) {

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        }, 2000);
    }



@UiThread
    public void testHelperMethod() {
//         custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.automation_data_dialog_layout);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // set the custom dialog components - text, image and button
        ImageView okButton = (ImageView) dialog.findViewById(R.id.automation_data_dialog_ok_button);

        final EditText slNameEditText = (EditText) dialog.findViewById(R.id.sl_name_edittext);
        final EditText apNameEditText = (EditText) dialog.findViewById(R.id.ap_name_edittext);
        final EditText apPassEditText = (EditText) dialog.findViewById(R.id.ap_pass_edittext);
        final CheckBox apPassCheckBox = (CheckBox) dialog.findViewById(R.id.ap_pass_checkbox);
        final EditText delayEditText = (EditText)dialog.findViewById(R.id.delay_edittext);
        final View apPassLayout = dialog.findViewById(R.id.automation_data_dialog_ap_pass_layout);
        final EditText iterationNumEditText = (EditText) dialog.findViewById(R.id.iteration_num_edittext);
        final CheckBox delayCheckBox = (CheckBox) dialog.findViewById(R.id.delay_checkbox);
        final View delayLayout = dialog.findViewById(R.id.automation_data_dialog_delay_layout);



        apPassCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                apPassEditText.requestFocus();
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(apPassEditText, InputMethodManager.SHOW_IMPLICIT);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                if (isChecked) {
                    apPassLayout.setVisibility(View.VISIBLE);
                } else {
                    apPassLayout.setVisibility(View.GONE);
                }
            }
        });

       delayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
           @Override
           public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

               delayEditText.requestFocus();
               try {
                   InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                   imm.showSoftInput(delayEditText,InputMethodManager.SHOW_IMPLICIT);
               } catch (NullPointerException e) {
                    e.printStackTrace();
               }

               if (isChecked) {
                   delayLayout.setVisibility(View.VISIBLE);
               } else {
                   delayLayout.setVisibility(View.GONE);
               }
           }
       });

//
        //OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slOriginalName = slNameEditText.getText().toString().trim();
                apName = apNameEditText.getText().toString().trim();
                apPass = apPassEditText.getText().toString().trim();
                isPassCheckChecked = apPassCheckBox.isChecked();
                iterationNumString = iterationNumEditText.getText().toString().trim();
                delayNumString = delayEditText.getText().toString().trim();

                if (slOriginalName == "" || apName == "") {
                    showToastWithMessage("Missing data");
//                    Toast.makeText(getActivity() "Missing data", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    showToastWithMessage("Automation data successfully entered");
//                    Toast.makeText(this, "Automation data successfully entered", Toast.LENGTH_SHORT).show();

                }
                dialog.dismiss();
            }
        });


        //CANCEL button
        ImageView cancelButton = (ImageView) dialog.findViewById(R.id.automation_data_dialog_cancel__button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }


}