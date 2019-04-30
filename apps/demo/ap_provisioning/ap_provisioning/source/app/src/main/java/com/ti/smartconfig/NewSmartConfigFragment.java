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

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import com.integrity_project.smartconfiglib.SmartConfig;
import com.integrity_project.smartconfiglib.SmartConfigListener;
import com.ti.smartconfig.utils.Device;
import com.ti.smartconfig.utils.DevicePickPopUpView_;
import com.ti.smartconfig.utils.Device_Type_Enum;
import com.ti.smartconfig.utils.ScanResultsPopUpView_;
import com.ti.smartconfig.utils.SharedPreferencesInterface_;
import com.ti.smartconfig.utils.AddProfileAsyncTask;
import com.ti.smartconfig.utils.WifiNetworkUtils;
import com.ti.smartconfig.utils.CFG_Result_Enum;
import com.ti.smartconfig.utils.Constants;
import com.ti.smartconfig.utils.DevicePickPopUpView;
import com.ti.smartconfig.utils.DeviceVersion;
import com.ti.smartconfig.utils.NetworkUtil;
import com.ti.smartconfig.utils.ScanResultsPopUpView;
import com.ti.smartconfig.utils.SecurityType;
import com.ti.smartconfig.utils.SmartConfigConstants;
import com.ti.smartconfig.utils.AddProfileAsyncTask.AddProfileAsyncTaskCallback;
import com.ti.smartconfig.utils.WifiNetworkUtils.BitbiteNetworkUtilsCallback;
import com.ti.smartconfig.utils.WifiNetworkUtils.WifiConnectionFailure;
import com.ti.smartconfig.utils.Popup.PopupType;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

@EFragment(R.layout.tab_new_smart_config)
/** NewSmartConfigFragment class is responsible for  handling and activating SmartConfig algorithm
 *  and handling application states related to SmartConfig actions and configurations
 */
public class NewSmartConfigFragment extends Fragment {

    protected static final String TAG = "NewSmartConfigFragment";
    private ProgressDialog progressDialog;
    public Boolean mIsReady = false;
    private Boolean mSmartConfigInProgress = false;
    public String ssidToAdd;
    public Boolean stopScLoader= false;
    public String ssidToAdd_WrongPassword;
    public String ssidToAddSecurityKey;
    public String ssidToAddSecurityKey_WrongPassword;
    public String ssidToAddPriority;
    public SecurityType ssidToAddSecurityType;
    public SecurityType ssidToAddSecurityType_WrongPassword;
    public Vibrator v;
    private AlertDialog alertDialog;
    private ScanResultsPopUpView scanResultsPopUpView;
    private DevicePickPopUpView devicePickPopUpView;
    public List<ScanResult> wifiList;
    public List<ScanResult> deviceList;
    final Handler handlerForTransmit = new Handler();
    Runnable smartConfigRunner = null;
    private ScanResult chosenDevice;
    private int cgfTryNumber = 0;
    SharedPreferences sharedpreferences;
    public static final String mypreference = "iot";
    public static final String Name = "deviceIP";
    byte[] freeData;
    SmartConfig smartConfig;
    SmartConfigListener smartConfigListener;
    JSONArray devicesArray;
    JSONArray recentDevicesArray;
    boolean waitForScanFinishForRestart = false;
    boolean waitForScanFinish = false;
    boolean wrongPassword = false;
    boolean foundNewDevice = false;

    private Boolean mGetCFGFromMDNS = false;

    private Boolean mGetCFGFromSC = false;

    String startingSSID = null;

    private Device mDevice;

    private boolean isDeviceAsAp = false;
    private int utilInt;
    MainActivity mainActivity;
    public String uiDeviceName;
    public boolean secondAPAAttemptCommitted = false;
    public Thread progThread;

    public TextWatcher passFieldTextWatcher;
    public TextWatcher nameFieldTextWatcher;


    @Pref
    SharedPreferencesInterface_ prefs;
    @ViewById
    EditText tab_device_configuration_sc_device_name_editText;
    @ViewById
    EditText tab_device_configuration_configuration_key_editText;
    @ViewById
    RelativeLayout tab_device_configuration_sc_device_to_configure_layout;
    @ViewById
    public ImageView tab_device_configuration_sc_start_button;

    @ViewById
    RelativeLayout tab_device_configuration_sc_device_name_layout;
    @ViewById
    RelativeLayout tab_device_configuration_configuration_key_layout;
    @ViewById
    RelativeLayout tab_device_configuration_iot_uuid_layout;
    @ViewById
    EditText tab_device_configuration_iot_uuid_name_editText;
    @ViewById
    RelativeLayout textViewConnectionTextView;
    @ViewById
    ImageView tab_device_configuration_network_to_configure_network_pick_image;
    @ViewById
    public TextView tab_device_configuration_network_to_configure_network_pick_label;
    @ViewById
    ImageView tab_device_configuration_device_to_configure_device_pick_image;
    @ViewById
    TextView tab_device_configuration_device_to_configure_device_pick_label;
    @ViewById
    RelativeLayout tab_sc_loader_layout;
    @ViewById
    TextView tab_sc_loader_label;
    @ViewById
    TextView tab_device_configuration_device_connection;
    @ViewById
    CheckBox tab_device_sc_configuration_password_checkbox;
    @ViewById
    public
    RelativeLayout tab_device_sc_configuration_password_check_layout;
    @ViewById
    EditText tab_device_sc_configuration_password_check_editText;
    @ViewById
    RelativeLayout tab_device_configuration_sc_layout;
    /**
     * Upon receiving a broadcast (sticky - receives on registration) informing that a
     * change in network connectivity has occurred, checks whether the mobile phone is connected to Wi-Fi or not,
     * if it is calls a method which analyzes the security type of the SSID the mobile phone is currently connected to
     * and sets it as the SSID to send in profile to SL device, and sets the UI accordingly.
     *
     * @see NewSmartConfigFragment#DisplayWifiState()
     */
    BroadcastReceiver myWifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {

            ConnectivityManager cm = (ConnectivityManager) arg0.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();

            if (networkInfo != null) {

                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {

                    DisplayWifiState();
                }
            }
            if(networkInfo == null){

                tab_device_configuration_network_to_configure_network_pick_label.setTextColor(getResources().getColor(R.color.color_connection_text_sc_holo_grey));
                tab_device_configuration_network_to_configure_network_pick_label.setText(getString(R.string.new_smart_config_fragment_search_for_your_network));
                tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
                tab_device_configuration_device_connection.setText(getResources().getString(R.string.new_smart_config_fragment_no_wifi_connection));//replace text
                tab_device_configuration_device_connection.setTextColor(Color.WHITE);
                textViewConnectionTextView.setBackgroundColor(getResources().getColor(R.color.color_red));
                mIsReady = false;
            }


        }
    };

    /**
     * Receives a broadcast when an mDNS scan had been completed,
     * if requested SL device was not found in the scan informs the user and allows
     * for configuration verification to be retrieved from SL device as access point.
     *
     * @see NewSmartConfigFragment#lookForNewDevice()
     * @see NewSmartConfigFragment#finish(Boolean)
     * @see NewSmartConfigFragment#makeManualVisible()
     */
    BroadcastReceiver scanFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if ((!mSmartConfigInProgress)) {
                return;
            }
            if (waitForScanFinishForRestart) {
                Log.i(TAG, "mDNS scan completed, restarting scan");
                mLogger.info("*SC* M scan completed, restarting scan");
                waitForScanFinishForRestart = false;
                lookForNewDevice();
            } else if (waitForScanFinish && !foundNewDevice && (ssidToAdd != null && !ssidToAdd.equals(""))) {
                Log.i(TAG, "mDNS scan completed, device wasn't found");
                mLogger.info("*SC* M scan completed, SL device was not found");
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                waitForScanFinish = false;
                if (mGetCFGFromSC) {
                    finish(false);
                    isDeviceAsAp = true;
                    mLogger.info("*SC* No SL devices were found in \"" + ssidToAdd + "\", You can...");
                    showToastWithMessage("No devices were found in \"" + ssidToAdd + "\", You can try to setup the device manually, or choose another network");
                    print("Device is not connected to the network");
                    print("Configuration shall continue after connecting to the device. Please choose your device.");
                    mLogger.info("*SC* Since SL device was not found in the requested network - Allow user to" +
                            " manually choose SL device to connect to in order to obtain cfg verification from as AP");
                    makeManualVisible();
                    mSmartConfigInProgress = false;
                    prefs.isSmartConfigActive().put(false);
                } else {
                    finish(false);
                    mLogger.info("*SC* SL Dev is not connected to local network sent in profile to connect to");
                    print("Device is not connected to the network");
                    print("Configuration shall continue after connecting to the device. Please choose your device.");
                    mSmartConfigInProgress = false;
                    prefs.isSmartConfigActive().put(false);
                }
            }
        }
    };
    /**
     * Receives a broadcast when an SL device was discovered via mDNS discovery, Ping broadcast or UDP server,
     * Checks whether the discovered SL device is the requested SL device by comparing their names,
     * if it is the device stops smartConfig transmission and moves to the configuration verification retrieval stage from that SL device.
     *
     * @see SmartConfig#stopTransmitting()
     * @see NewSmartConfigFragment#getCFG()
     */
    public BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (foundNewDevice) {
                return;
            }
            String currentSSID = NetworkUtil.getConnectedSSID(mainActivity);
            if (currentSSID != null) {
                if (!currentSSID.equals(ssidToAdd)) {
                    Log.i(TAG, "not in config network - can not be the configured SL device");
                    return;
                }
            }
            JSONObject deviceJSON = null;
            try {
                String jsonString = intent.getExtras().getString("newDevice");
                deviceJSON = new JSONObject(jsonString);
                Log.w(TAG, "Detected a new device (jsonString: " + jsonString );
                mLogger.info("*SC* Detected a new SL device (jsonString: " + jsonString + ")");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to create JSON of device");
                mLogger.info("*SC* Failed to create JSON of SL device");
            }

            Boolean nameIsValid = false;
            String name = null;
//            String nameToLookFor = tab_device_configuration_sc_device_name_editText.getText().toString();
            String nameToLookFor = uiDeviceName;
            Log.i(TAG, "SL device name to look for: " + nameToLookFor);
            if (nameToLookFor != null) {
                if (nameToLookFor.length() > 0) {
                    try {
                        name = deviceJSON != null ? deviceJSON.getString("name") : null;
                        mLogger.info("*SC* Checking if the SL device found is the correct SL device - does \"" + name + "\" equal \"" + nameToLookFor + "\"");
                        if (name != null && name.equals(nameToLookFor)) {
                            mLogger.info("*SC* Name is valid - Requested SL device has been found");
                            nameIsValid = true;
                            prefs.isSmartConfigActive().put(false);

                            try {
                                mLogger.info("*SC* Requested SL device found - Stop SmartConfig transmission");
                                smartConfig.stopTransmitting();
                                Log.i(TAG, "Broadcasting information to network finished");
                                mLogger.info("*SC* Requested SL device found - SmartConfig Broadcasting configuration information to network finished");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            mLogger.info("*SC* Name is not Valid - SL device found is not requested SL device");
                            nameIsValid = false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    mLogger.info("*SC* No user given device name to compare found SL device to - assume name is valid and requested SL device found");
                    // This is the case where device is not defined by the user and there is no device
                    // to compare to. We assume the device name is valid since we check JSON...
                    try {
                        name = deviceJSON != null ? deviceJSON.getString("name") : null;
                        nameIsValid = true;
                        prefs.isSmartConfigActive().put(false);
                        try {
                            smartConfig.stopTransmitting();
                            Log.i(TAG, "Broadcasting information to network finished");
                            mLogger.info("*SC* Requested SL device found - SmartConfig Broadcasting configuration information to network finished");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }


            if (!nameIsValid) {
                Log.w(TAG, "The device that was found (" + name + ") is not the one we just set, the name does not match");
                mLogger.info("*SC* The SL device found is not the one that was just set, \"" + name + "\" does not match \"" + nameToLookFor + "\"");
                return;
            }

            if (deviceJSON != null && !foundNewDevice) {
                ((MainActivity) getActivity()).stopPing();
                waitForScanFinish = false;
                Log.i(TAG, "The device that was found was new");
                mLogger.info("*SC* The SL device that was found was new - and is the requested SL device that was just set");
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                foundNewDevice = true;
                try {
                    String host = deviceJSON.getString("host");
                    String hostUrl = "http://" + host;
                    mDevice = new Device(name, host);
                    Log.i(TAG, "Set mDevice name: " + name + " host: " + host);
                    mLogger.info("*SC* Set the SL device found as the device to obtain cfg verification from -   mDevice name: " + name + ", host: " + host);
                    mLogger.info("*SC* Cfg verification process via SL device as STA begins");
                    cgfTryNumber = 0;
                    getCFG();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                mLogger.info("*SC* The detected SL device is not new");
            }
        }
    };

    private Logger mLogger;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mainActivity = (MainActivity) activity;
    }

    /**
     * Moves to the configuration verification stage of the provisioning process,
     * retrieving the verification from SL device as station or as access point
     * depending on whether the requested SL device was discovered in the
     * relevant network or not.
     *
     * @see NewSmartConfigFragment#confirmResult()
     */
    @UiThread
    void checkParams() {
        if (!foundNewDevice) {
            prefs.scanningDisable().put(true);
            mainActivity.stopPing();
            print("Verify via AP, Connecting to " + chosenDevice.SSID + "..");
            WifiNetworkUtils.getInstance(mainActivity).clearCallback();
            WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(mainActivity, chosenDevice.SSID, SecurityType.OPEN, null);
            WifiNetworkUtils.getInstance(mainActivity).connectToWifi(configuration, mainActivity, new BitbiteNetworkUtilsCallback() {
                @Override
                public void successfullyConnectedToNetwork(String ssid) {
                    WifiNetworkUtils.getInstance(mainActivity).clearCallback();
                    confirmResult();
                }

                @Override
                public void failedToConnectToNetwork(WifiConnectionFailure failure) {
                    WifiNetworkUtils.getInstance(mainActivity).clearCallback();
                    showLoaderWithText(false, null);
                    mainActivity.showSuccessDialog("Failed to connect to simple link for confirmation", getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
                }
            }, true);
        } else {
            confirmResult();
            //finish(false);
        }

    }

    /**
     * Connects to Wi-Fi network mobile phone was connected to before provisioning process began,
     * and moves UI to "Devices" tab to display the provisioned SL device.
     *
     * @param moveToDevices Boolean. True to move to "Devices" tab.
     *                      False to stay in "Provisioning" tab.
     */
    private void finish(final Boolean moveToDevices) {
        uiDeviceName = null;
        isDeviceAsAp = false;
        showLoaderWithText(false, "");
        secondAPAAttemptCommitted = false;


        print("Connecting to starting network " + startingSSID);
        mLogger.info("*SC* Connecting to starting network \"" + startingSSID + "\"");
        if (startingSSID != null && startingSSID.equalsIgnoreCase(WifiNetworkUtils.getInstance(mainActivity).getConnectedSSID())) {
            if (moveToDevices) {
                mLogger.info("*SC* Provisioning process of SL device in SmartConfig mode ended successfully and we are connected to starting network: \""
                        + startingSSID + "\" - moving to devices tab to display provisioned SL device ");
                //6.2
                //fix for LG G4 when going into sleep mode - losing tabs
//                PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
//                boolean isScreenOn = pm.isScreenOn();
//
//                if(isScreenOn) {
      //          }
            }
        } else if (startingSSID != null){
            WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(mainActivity, startingSSID, SecurityType.OPEN, null);
            WifiNetworkUtils.getInstance(mainActivity).connectToWifi(configuration, mainActivity, new BitbiteNetworkUtilsCallback() {
                @Override
                public void successfullyConnectedToNetwork(String ssid) {
                    mLogger.info("*SC* successfully connected to starting network: \"" + startingSSID + "\"");
                    if (moveToDevices) {
                        mLogger.info("*SC* Moving to devices tab to display provisioned SL device");
                    }
                }

                @Override
                public void failedToConnectToNetwork(WifiConnectionFailure failure) {
                    mLogger.error("*SC* Failed to connect to starting network: \"" + startingSSID + "\"");
                    showToastWithMessage("Failed to connect to starting network " + startingSSID);
                }
            }, true);

        }else {
            mLogger.info("Initial network is null - will not attempt to connect");
            Log.i(TAG,"Initial network is null - will not attempt to connect");

            showToastWithMessage("No initial network to connect to");
        }


        if (moveToDevices) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new OTAAndType().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            } else {
                new OTAAndType().execute("");
            }
        }

    }

    /**
     * Called after views binding has happened,
     * sets UI elements to their initial state and sets listeners to them.
     */
    @AfterViews
    void afterViews() {

        tab_device_sc_configuration_password_check_editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);


        //watchers

        //text watcher
        passFieldTextWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


                if (tab_device_sc_configuration_password_check_editText.length() > 0) {
                    //pass required - got pass
                    //enable configuration and pull pass
                    tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                    tab_device_configuration_sc_start_button.setEnabled(true);

                    ssidToAddSecurityKey = tab_device_sc_configuration_password_check_editText.getText().toString();

                    mIsReady = true;
                    setToReady(mIsReady);


                } else {
                    //pass required - but missing
                    //disable configuration
                    tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
                    tab_device_configuration_sc_start_button.setEnabled(false);

                    mIsReady = false;
                    setToReady(mIsReady);

                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        //name watcher
        nameFieldTextWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (tab_device_configuration_sc_device_name_editText.length() > 0) {
                    //name required - got name
                    //check pass field visibility
                    if (tab_device_sc_configuration_password_check_layout.getVisibility() == View.VISIBLE) {
                        //pass required
                        if (tab_device_sc_configuration_password_check_editText.length() > 0) {
                            // got pass - enable configuration and pull pass
                            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                            tab_device_configuration_sc_start_button.setEnabled(true);

                            ssidToAddSecurityKey = tab_device_sc_configuration_password_check_editText.getText().toString();

                            mIsReady = true;
                            setToReady(mIsReady);

                        } else {
                            tab_device_sc_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);
//                            //missing pass - disable configuration right now - if pass will be entered at some point pass watcher will enable configuration
//                            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
//                            tab_device_configuration_sc_start_button.setEnabled(false);
//
//                            mIsReady = false;
//                            setToReady(mIsReady);
                        }


                    } else {
                        //no pass required -  enable configuration - but no pass
                        tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                        tab_device_configuration_sc_start_button.setEnabled(true);

                        mIsReady = true;
                        setToReady(mIsReady);
                    }

                } else {
                    // name required but missing - disable configuration
                    tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
                    tab_device_configuration_sc_start_button.setEnabled(false);

                    mIsReady = false;
                    setToReady(mIsReady);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };


        startConfigButtonState();


        mLogger = Logger.getLogger(NewSmartConfigFragment.class);


        // Get instance of Vibrator from current Context
        v = (Vibrator) mainActivity.getSystemService(Context.VIBRATOR_SERVICE);

        ssidToAdd = null;
        prefs.isSmartConfigActive().put(false);
        mSmartConfigInProgress = false;
        foundNewDevice = true;
        waitForScanFinish = false;
        //Typeface font = Typeface.createFromAsset(mainActivity.getAssets(), "fonts/fake.ttf");
        //   tab_device_configuration_device_connection.setTypeface(font);

        makeManualGone();

//        tab_device_configuration_sc_device_name_editText.setText("");
        if (prefs.showDeviceName().get()) {
            tab_device_configuration_sc_device_name_layout.setVisibility(View.VISIBLE);
        } else {
            tab_device_configuration_sc_device_name_layout.setVisibility(View.GONE);
        }

        if (prefs.showSmartConfigPass().get()) {
            tab_device_configuration_configuration_key_layout.setVisibility(View.VISIBLE);
        } else {
            tab_device_configuration_configuration_key_layout.setVisibility(View.GONE);
        }

        if (prefs.isIoTuuid().get()) {
            tab_device_configuration_iot_uuid_layout.setVisibility(View.VISIBLE);
        } else {
            tab_device_configuration_iot_uuid_layout.setVisibility(View.GONE);
        }


        tab_sc_loader_layout.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                return true;
            }
        });

        String ssid = NetworkUtil.getConnectedSSID(mainActivity);

        if (ssid == null) {
            tab_device_configuration_device_connection.setText(getString(R.string.new_smart_config_fragment_no_wifi_connection));
            tab_device_configuration_device_connection.setTextColor(Color.WHITE);
            textViewConnectionTextView.setBackgroundColor(getResources().getColor(R.color.color_red));
        } else {
            tab_device_configuration_device_connection.setText(String.format(getString(R.string.tab_device_configuration_connected_to_ssid), ssid));
            tab_device_configuration_device_connection.setTextColor(Color.WHITE);
            textViewConnectionTextView.setBackgroundColor(getResources().getColor(R.color.color_connection_text_sc_holo_grey));
            //29.2 touched
            startScan();
        }


        tab_device_sc_configuration_password_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                mainActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);


                tab_device_sc_configuration_password_check_editText.requestFocus();
                try {
                    InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(tab_device_sc_configuration_password_check_editText, InputMethodManager.SHOW_IMPLICIT);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }


//                    tab_device_sc_configuration_password_check_editText.setShowSoftInputOnFocus(true);

                // checkbox status is changed from unchecked to checked.
                if (!isChecked) {
                    // show password
                    tab_device_sc_configuration_password_check_editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    // hide password
                    tab_device_sc_configuration_password_check_editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
        });

        //listen to editText focus and hiding keyboard when focus is out
        tab_device_configuration_sc_device_name_editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        tab_device_sc_configuration_password_check_editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }

        });


//        tab_device_configuration_configuration_key_editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (!hasFocus) {
//                    validateInput(v);
//                }
//
//
//
////                @Override
////                public void onFocusChange(View v, boolean hasFocus) {
////    /* When focus is lost check that the text field
////    * has valid values.
////    */
////                    if (!hasFocus) {
////                        validateInput(v);
////                    }
////                }
////            });
//
//
//
//
//
//
//            }
//        });
        //Check to see if the starting network is null
        startingSSID = ((MainActivity) getActivity()).mStartingWifiNetwork;
        String connectedWifi = NetworkUtil.getConnectedSSID(getActivity());
        Log.i(TAG, "StartingSSID: " + startingSSID);
        if (startingSSID == null && connectedWifi == null) {
            mLogger.info("*AP* Showing \"no wifi activity\" dialog, because the starting network is null and we are not connected to any network now");
            AlertDialog wifiDialog = new AlertDialog.Builder(getActivity()). //create a dialog
                    setTitle("No WiFi Connection").
                    setMessage("Please connect your Smart Phone to router").
                    setPositiveButton("Connect to WiFi", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) { //the user clicked yes
                            WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                            if (!wifiManager.isWifiEnabled())
                                wifiManager.setWifiEnabled(true);
                            startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), Constants.WIFI_SETTINGS_INTENT_RESULTS);
                        }

                    }).setNeutralButton("No router", new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                    if (!wifiManager.isWifiEnabled())
                        wifiManager.setWifiEnabled(true);

                    //   initiateScan();
                    Log.e(TAG, "Scan running from Afterviews #3");
                }
            }).create();
            wifiDialog.show();
            //	}
        }
    }





    public void startConfigButtonState() {

        if (tab_device_configuration_sc_device_name_layout.getVisibility() == View.VISIBLE) {

            tab_device_configuration_sc_device_name_editText.addTextChangedListener(nameFieldTextWatcher);


            Random random = new Random();
//            String ran = String.valueOf(random.nextInt());
            int ran = (random.nextInt(8)+1)*100 + (random.nextInt(8)+1)*10 + (random.nextInt(8)+1);
            if (ran < 0) {
                ran = (ran * -1);
            }

            tab_device_configuration_sc_device_name_editText.setTextColor(Color.LTGRAY);
            tab_device_configuration_sc_device_name_editText.setText(String.format("Dev-%d",ran));




            tab_device_configuration_sc_device_name_editText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tab_device_configuration_sc_device_name_editText.setText("");
                    tab_device_configuration_sc_device_name_editText.setTextColor(Color.BLACK);
                }
            });



            if (tab_device_configuration_sc_device_name_editText.length() > 0) {

                if (tab_device_sc_configuration_password_check_layout.getVisibility() == View.VISIBLE) {

                    tab_device_sc_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);

                    if (tab_device_sc_configuration_password_check_editText.length() > 0) {

                        tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                        tab_device_configuration_sc_start_button.setEnabled(true);

                        ssidToAddSecurityKey = tab_device_sc_configuration_password_check_editText.getText().toString();

                        mIsReady = true;
                        setToReady(mIsReady);
                    }
//                    else {
//                        tab_device_sc_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);
//                    }

                } else {
                    tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                    tab_device_configuration_sc_start_button.setEnabled(true);

                    mIsReady = true;
                    setToReady(mIsReady);
                }

            }
//            else {
//                tab_device_configuration_sc_device_name_editText.addTextChangedListener(nameFieldTextWatcher);
//            }




        } else {

            if (tab_device_sc_configuration_password_check_layout.getVisibility() == View.VISIBLE) {

                tab_device_sc_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);

                if (tab_device_sc_configuration_password_check_editText.length() > 0) {
                    tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                    tab_device_configuration_sc_start_button.setEnabled(true);

                    ssidToAddSecurityKey = tab_device_sc_configuration_password_check_editText.getText().toString();

                    mIsReady = true;
                    setToReady(mIsReady);

                }
//                else {
//                    tab_device_sc_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);
//
//                }

            } else {
                tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                tab_device_configuration_sc_start_button.setEnabled(true);

                mIsReady = true;
                setToReady(mIsReady);
            }
        }
    }


    public void startConfigButtonStateNoNaming() {
        if (tab_device_configuration_sc_device_name_layout.getVisibility() == View.VISIBLE) {

            tab_device_configuration_sc_device_name_editText.addTextChangedListener(nameFieldTextWatcher);


//            Random random = new Random();
////            String ran = String.valueOf(random.nextInt());
//            int ran = (random.nextInt(9)*100) + (random.nextInt(9)*10) + (random.nextInt(9));
//            if (ran < 0) {
//                ran = (ran * -1);
//            }
//
//            tab_device_configuration_sc_device_name_editText.setTextColor(Color.LTGRAY);
//            tab_device_configuration_sc_device_name_editText.setText(String.format("Dev_%d",ran));
//
//
//
//
//            tab_device_configuration_sc_device_name_editText.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    tab_device_configuration_sc_device_name_editText.setText("");
//                    tab_device_configuration_sc_device_name_editText.setTextColor(Color.BLACK);
//                }
//            });



            if (tab_device_configuration_sc_device_name_editText.length() > 0) {

                if (tab_device_sc_configuration_password_check_layout.getVisibility() == View.VISIBLE) {

                    tab_device_sc_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);

                    if (tab_device_sc_configuration_password_check_editText.length() > 0) {

                        tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                        tab_device_configuration_sc_start_button.setEnabled(true);

                        ssidToAddSecurityKey = tab_device_sc_configuration_password_check_editText.getText().toString();

                        mIsReady = true;
                        setToReady(mIsReady);
                    }
//                    else {
//                        tab_device_sc_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);
//                    }

                } else {
                    tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                    tab_device_configuration_sc_start_button.setEnabled(true);

                    mIsReady = true;
                    setToReady(mIsReady);
                }

            }
//            else {
//                tab_device_configuration_sc_device_name_editText.addTextChangedListener(nameFieldTextWatcher);
//            }




        } else {

            if (tab_device_sc_configuration_password_check_layout.getVisibility() == View.VISIBLE) {

                tab_device_sc_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);

                if (tab_device_sc_configuration_password_check_editText.length() > 0) {
                    tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                    tab_device_configuration_sc_start_button.setEnabled(true);

                    ssidToAddSecurityKey = tab_device_sc_configuration_password_check_editText.getText().toString();

                    mIsReady = true;
                    setToReady(mIsReady);

                }
//                else {
//                    tab_device_sc_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);
//
//                }

            } else {
                tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
                tab_device_configuration_sc_start_button.setEnabled(true);

                mIsReady = true;
                setToReady(mIsReady);
            }
        }
    }


    private boolean validateInput(View v) {
        String txt = ((EditText) v).getText().toString();
        byte[] txtByte = txt.getBytes();
        int txtByteLength = txtByte.length;
        if (txtByteLength != 16) {
            showToastWithMessage("Configuration key must be exactly 16 bytes, current size is: " + txtByteLength + " bytes, \nPlease correct your input");
            v.requestFocus();
            return false;
        } else {
            return true;
        }
    }

    /**
     * Enables/Disables the UI "Start Configuration" button according to the security type of the
     * SSID the mobile phone is currently connected to, and sets it as the chosen access point to
     * send to SL device in Wi-Fi profile, starts mDNS discovery and Ping broadcast,
     * and registers broadcast receivers.
     *
     * @see NewSmartConfigFragment#setToReady(Boolean)
     * @see NewSmartConfigFragment#startScan()
     * @see NewSmartConfigFragment#myWifiReceiver
     * @see NewSmartConfigFragment#scanFinishedReceiver
     * @see NewSmartConfigFragment#deviceFoundReceiver
     */
    @Override
    public void onResume() {
        super.onResume();
        tab_device_configuration_sc_device_name_editText.requestFocus();

        isDeviceAsAp = false;
        secondAPAAttemptCommitted = false;


//        Random random = new Random();
////        String ran = String.valueOf(random.nextInt());
//        int ran = random.nextInt();
//        if (ran < 0) {
//            ran = (ran * -1);
//        }
//        tab_device_configuration_sc_device_name_editText.setText(String.format("Dev_%d",ran));
//        tab_device_sc_configuration_password_check_editText.setText("");

        startConfigButtonState();



        ConnectivityManager myConnManager = (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        String ssid = NetworkUtil.getConnectedSSID(mainActivity);


        if (ssidToAddSecurityType == SecurityType.WEP || ssidToAddSecurityType == SecurityType.WPA1 || ssidToAddSecurityType == SecurityType.WPA2) {
            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
            mIsReady = false;
            setToReady(mIsReady);
        }
        if (ssidToAddSecurityType == SecurityType.OPEN) {
//            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
//            mIsReady = true;
//            setToReady(mIsReady);
        }

        if (myNetworkInfo.isConnected()) {
            if (ssid != null && !(ssid.equals(""))) {
                tab_device_configuration_network_to_configure_network_pick_label.setText(ssid);
                tab_device_configuration_network_to_configure_network_pick_label.setTextColor(Color.BLACK);

                //mIsReady = true;
                //	setToReady(mIsReady);
                //startSmartConfig();


            }
        }
        //29.2 touched
        startScan();

        mainActivity.registerReceiver(myWifiReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mainActivity.registerReceiver(scanFinishedReceiver, new IntentFilter(SmartConfigConstants.SCAN_FINISHED_BROADCAST_ACTION));
        mainActivity.registerReceiver(deviceFoundReceiver, new IntentFilter(SmartConfigConstants.DEVICE_FOUND_BROADCAST_ACTION));

    }

    /**
     * Called when the activity is going into the background, but has not (yet) been killed,
     * unregisters broadcast receivers and empties device name and password UI fields.
     *
     * @see NewSmartConfigFragment#myWifiReceiver
     * @see NewSmartConfigFragment#deviceFoundReceiver
     * @see NewSmartConfigFragment#scanFinishedReceiver
     */
    @Override
    public void onPause() {
        super.onPause();

        if(mSmartConfigInProgress) {

            finish(false);
            mainActivity.stopScanning();
          //  mainActivity.clear();
            prefs.scanningDisable().put(true);
            prefs.isSmartConfigActive().put(false);
            progressDialog.dismiss();
            try {
                smartConfig.stopTransmitting();
            } catch (Exception e) {
                e.printStackTrace();
            }
            stopScan();
        }
        tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);




        handlerForTransmit.removeCallbacks(smartConfigRunner);
        Log.d(TAG, "smartConfigRunner callbacks removed");
        if (progThread != null) {
            progThread.interrupt();
        }
        //9.2 password issue
        wrongPassword = false;
        stopScan();
        //unregisters the receivers
        try {
            mainActivity.unregisterReceiver(myWifiReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        try {
//            getActivity().unregisterReceiver(deviceFoundReceiver);
//        } catch (Exception e) {
//        }
//
//        try {
//            getActivity().unregisterReceiver(scanFinishedReceiver);
//        } catch (Exception e) {
//        }
        getActivity().unregisterReceiver(scanFinishedReceiver);
        getActivity().unregisterReceiver(deviceFoundReceiver);

//        tab_device_configuration_sc_device_name_editText.setText("");


//        startConfigButtonState();

//        tab_device_configuration_sc_device_name_editText.addTextChangedListener(nameFieldTextWatcher);
//        tab_device_sc_configuration_password_check_editText.addTextChangedListener(passFieldTextWatcher);
/*
        Random random = new Random();
//        String ran = String.valueOf(random.nextInt());
        int ran = random.nextInt();
        if (ran < 0) {
            ran = (ran * -1);
        }
        tab_device_configuration_sc_device_name_editText.setText(String.format("Dev_%d",ran));
        tab_device_sc_configuration_password_check_editText.setText("");
        */
    }


    /**
     * Prints String passed as parameter to system with System.out.println
     * and displays it to user as loader label.
     *
     * @param string String. The text to print and display.
     */
    @UiThread
    public void print(String string) {
        System.out.println(string);
        tab_sc_loader_label.setText(string);
    }

    /**
     * Displays an explanatory dialog related to the adjacent field, upon pressing the question mark button.
     */
    @Click
    void tab_device_configuration_sc_device_name_question_button() {
        mainActivity.showSuccessDialog(Constants.QUESTION_DEVICE_NAME, getString(R.string.pop_up_close), null, PopupType.Information, null, null);
    }

    /**
     * Displays an explanatory dialog related to the adjacent field, upon pressing the question mark button.
     */
    @Click
    void tab_device_configuration_configuration_key_question_button() {
        mainActivity.showSuccessDialog(Constants.QUESTION_CONFIGURATION_KEY, getString(R.string.pop_up_close), null, PopupType.Information, null, null);
    }

    /**
     * Displays an explanatory dialog related to the adjacent field, upon pressing the question mark button.
     */
    @Click
    void tab_device_configuration_sc_network_question_button() {
        mainActivity.showSuccessDialog(Constants.QUESTION_NETWORK_NAME, getString(R.string.pop_up_close), null, PopupType.Information, null, null);
    }

    /**
     * Displays an explanatory dialog related to the adjacent field, upon pressing the question mark button.
     */
    @Click
    void tab_device_configuration_sc_device_question_button() {
        mainActivity.showSuccessDialog(Constants.QUESTION_CHOOSE_DEVICE, getString(R.string.pop_up_close), null, PopupType.Information, null, null);
    }

    /**
     * Displays an explanatory dialog related to the adjacent field, upon pressing the question mark button.
     */
    //
    @Click
    void tab_device_sc_configuration_password_check_button() {
        mainActivity.showSuccessDialog(Constants.QUESTION_PASSWORD, getString(R.string.pop_up_close), null, PopupType.Information, null, null);
    }

    /**
     * Starts smartConfig transmission based on the user's input.
     *
     * @see NewSmartConfigFragment#startSmartConfig()
     */
    @Click
    void tab_device_configuration_sc_start_button() {

//        if (tab_device_configuration_configuration_key_editText.getVisibility() == View.VISIBLE) {
        if (prefs.showSmartConfigPass().get()) {

            boolean configKeyValidation = validateInput(tab_device_configuration_configuration_key_editText);
            if (!configKeyValidation) {
                return;
            }
        }

        mLogger.info("*SC* *** Starting SL device configuration in SC mode - \"start configuration\" button pressed ***");
        if (ssidToAdd == null || ssidToAddPriority == null) {
            tab_device_configuration_sc_start_button.setEnabled(false);
            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);


            return;
        }

        String ssid = NetworkUtil.getConnectedSSID(mainActivity);


        if (!mIsReady) {
            Toast.makeText(mainActivity, "Please connect to your router first", Toast.LENGTH_SHORT).show();
            return;
        }
        //wrong password fix
        if (wrongPassword == true || isDeviceAsAp) {

            tab_device_configuration_sc_device_to_configure_layout.setVisibility(View.VISIBLE);
            mGetCFGFromSC = false;
            showLoaderWithText(true, "");
            print("Connecting to " + chosenDevice.SSID);
            mLogger.info("Connecting to " + chosenDevice.SSID);
            mDevice = null;

            WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(mainActivity, chosenDevice.SSID, SecurityType.OPEN, "");
            WifiNetworkUtils.getInstance(mainActivity).connectToWifi(configuration, mainActivity, new BitbiteNetworkUtilsCallback() {
                @Override
                public void successfullyConnectedToNetwork(String ssid) {

                    prefs.isSmartConfigActive().put(true);
                    ArrayList<Object> passing = new ArrayList<Object>();
                    passing.add(tab_device_configuration_sc_device_name_editText.getText().toString());
                    passing.add(ssidToAddSecurityType);
                    passing.add(ssidToAdd);
                    passing.add(ssidToAddSecurityKey);
                    passing.add(ssidToAddPriority);
                    passing.add(tab_device_configuration_iot_uuid_name_editText.getText().toString());
                    ssidToAddSecurityKey_WrongPassword = ssidToAddSecurityKey;
                    ssidToAdd_WrongPassword = ssidToAdd;
                    ssidToAddSecurityType_WrongPassword = ssidToAddSecurityType;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        new AddProfileAsyncTask(mAddProfileAsyncTaskCallback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, passing);
                    } else {
                        new AddProfileAsyncTask(mAddProfileAsyncTaskCallback).execute(passing);
                    }
                    print("Your mobile is connected to " + ssid);

                    mLogger.info("Your mobile is connected to " + ssid);
                }


                @Override
                public void failedToConnectToNetwork(WifiConnectionFailure failure) {
                    failedToConnectToSLForConfirmation(failure);
                    wrongPassword = true;
                }
            }, false);

        } else {
            v.vibrate(100);
            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
            startSmartConfig();
            Log.v(TAG, "+++++++++++++++++++++++++++++++++++++++++");
            Log.v(TAG, "SSID to add sec key: " + ssidToAddSecurityKey);
            Log.v(TAG, "SSID to add: " + ssidToAdd);
            Log.v(TAG, "SSID to add priority: " + ssidToAddPriority);
            Log.v(TAG, "SSID to add sec type: " + ssidToAddSecurityType);
            Log.v(TAG, "+++++++++++++++++++++++++++++++++++++++++");
        }
        // keyboard fixed
        if (tab_device_configuration_sc_device_name_editText.hasFocus() || tab_device_sc_configuration_password_check_editText.hasFocus() || tab_device_configuration_iot_uuid_name_editText.hasFocus()) {
            InputMethodManager inputMethodManager = (InputMethodManager) mainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (getView() != null) {
                inputMethodManager.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }


    @UiThread
    void reloadDeviceSpinner() {
        ArrayList<String> wifiListStrings = new ArrayList<>();
        wifiListStrings.add(getString(R.string.ap_provisioning_spinner_headline));
        for (ScanResult scanResult : deviceList) {
            wifiListStrings.add(scanResult.SSID);
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(mainActivity, android.R.layout.simple_spinner_item, wifiListStrings);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    /**
     * Displays a toast containing the String passed as parameter.
     *
     * @param msg String. The text to be displayed in the toast.
     */
    @UiThread
    void showToastWithMessage(final String msg) {

        try {

            Toast.makeText(mainActivity, msg, Toast.LENGTH_LONG).show();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mainActivity, msg, Toast.LENGTH_LONG).show();
                }
            }, 2000);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the procedure of retrieving configuration verification from the SL device.
     */
    private void confirmResult() {
        final Handler handler = new Handler();
        cgfTryNumber = 0;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getCFG();
            }
        }, Constants.DELAY_BEFORE_ASKING_CFG_RESULTS);
    }

    /**
     * Checks the number of attempts made at retrieving configuration confirmation from SL device,
     * and either continues to another attempt or moves to verification retrieval from SL device as access point,
     * depending on the number of attempts made.
     *
     * @see NewSmartConfigFragment#finish(Boolean)
     * @see NewSmartConfigFragment#makeManualVisible()
     * @see com.ti.smartconfig.NewSmartConfigFragment.GetCFGResult
     */
    private void getCFG() {
        Log.i(TAG, "getCFG Started");
        mLogger.info("*SC* getCFG Started");
        mLogger.info("*SC* Number of attempts previously made to retrieve cfg verification from SL device: " + cgfTryNumber);
        if (cgfTryNumber >= Constants.ATTEMPTS_TO_GET_CGF_RESULTS) {
            mLogger.error("*SC* max attempts at retrieving cfg verification via SL device reached");

            //remove loader after max attempts
            showLoaderWithText(false,"");

//            mainActivity.showSuccessDialog(Constants.DEVICE_LIST_CFG_CONFIRMATION_FAILED, getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
            showToastWithMessage("Configuration verification failed, choose the SimpleLink device manually for a direct verification attempt");
            mLogger.info("*SC* Cfg verification retrieval from SL device failed");
            finish(false);
            mLogger.info("*SC* Allow user to manually choose SL device to connect to in order to obtain cfg verification from as AP (attempts made as STA failed)");
            makeManualVisible();
            return;


        }
        prefs.isSmartConfigActive().put(false);
        mSmartConfigInProgress = false;
        Log.i(TAG, "Confirming your configuration");
        showLoaderWithText(true,"Confirming your configuration");
        mLogger.info("*SC* Retrieving cfg verification from SL device");

        String uuidStr = "";
        if (prefs.isIoTuuid().get()) {
            uuidStr = tab_device_configuration_iot_uuid_name_editText.getText().toString();
        }

        utilInt = cgfTryNumber;
        mLogger.info("*SC* Executing cfg verification attempt no.: " + ++utilInt);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new GetCFGResult().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, Constants.BASE_URL_NO_HTTP, uuidStr);
        } else {
            new GetCFGResult().execute(Constants.BASE_URL_NO_HTTP, uuidStr);
        }
    }

    /**
     * Asynchronous task which retrieves the configuration verification from the SL device,
     * and either makes another retrieval attempt, finishes process successfully/unsuccessfully,
     * or moves to access point mode retrieval, depending on obtained result.
     * Stops Ping broadcast.
     *
     * @see NewSmartConfigFragment#getCFG()
     * @see NewSmartConfigFragment#finish(Boolean)
     * @see NewSmartConfigFragment#makeManualVisible()
     * @see com.ti.smartconfig.utils.AddProfileAsyncTask
     * @see MainActivity#stopPing()
     */
    class GetCFGResult extends AsyncTask<String, Void, String> {

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(String result) {

            Log.e(TAG,"INSIDE onPostExecute***");
            if (result != null && NetworkUtil.getResultTypeCFGString(result) == CFG_Result_Enum.Not_Started) {
                mGetCFGFromSC = false;
            }
            Log.i(TAG, "onPostExecute mGetCFGFromSC = " + mGetCFGFromSC);
            mLogger.info("*SC* Cfg result text: " + result);
            mLogger.info("*SC* onPostExecute mGetCFGFromSC = " + mGetCFGFromSC);
            if (mGetCFGFromSC) {
                //5 attempts to connect the host - if failed we will throw a failed window - confirmation failed
                if (result == null && cgfTryNumber < Constants.ATTEMPTS_TO_GET_CGF_RESULTS) {
                    mLogger.info("*SC* Cfg verification retrieval from SL device attempt no. " + utilInt + " was unsuccessful");

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mLogger.info("*SC* Running another cfg verification retrieval attempt - no. " + ++utilInt);
                            cgfTryNumber++;
                            getCFG();
                        }
                    }, Constants.DELAY_BEFORE_ASKING_CFG_RESULTS); //1 sec delay before trying again
                } else {

                    if (result == null) {
                        mainActivity.showSuccessDialog(null, getString(R.string.pop_up_close), null, PopupType.Success, null, "Configuration successful");
                        mLogger.info("*SC* *** SL device provisioning in SC mode ended successfully - SL device is connected to the network ***");
                        finish(true);
                    } else if (NetworkUtil.getResultTypeCFGString(result) == CFG_Result_Enum.Wrong_Password ||
                            NetworkUtil.getResultTypeCFGString(result) == CFG_Result_Enum.Ip_Add_Fail) {
                        if (result != null && !result.equals("")) {
                            mainActivity.showSuccessDialog(result, getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
                        } else {
                            mainActivity.showSuccessDialog("Provisioning Unsuccessful", getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
                        }
                        mLogger.info("*SC* *** SL device provisioning in SC mode ended unsuccessfully - either wrong password was entered or IP address was not successfully obtained ***");
                        finish(false);


                    } else if (NetworkUtil.getResultTypeCFGString(result) == CFG_Result_Enum.Success) {
                        mainActivity.showSuccessDialog(null, getString(R.string.pop_up_close), null, PopupType.Success, null, result);
                        mLogger.info("*SC* *** SL device provisioning in SC mode ended successfully - SL device is connected to the network ***");
                        //9.2 fixed password issue

                    if (wrongPassword == true) {
                        wrongPassword = false;
                    }
                    //fetching device IP -lan tab
                    sharedpreferences = mainActivity.getSharedPreferences(mypreference, Context.MODE_PRIVATE);
                    if (mDevice != null) {
                        String simplelinkDeviceIp = mDevice.host;
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(Name, simplelinkDeviceIp);
                        editor.commit();
                        Log.i(TAG, "Entered IP into SP: " + mDevice.host);
                        mLogger.info("*SC* Entered IP into SP: " + mDevice.host);
                        //          ((MainActivity) getActivity()).EnableOutOfTheBoxTabs(true);
                    }



                        finish(true);
                    } else {
                        if (result != null && !result.equals("")) {
                            mainActivity.showSuccessDialog(result, getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
                        } else {
                            mainActivity.showSuccessDialog("Provisioning Unsuccessful", getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
                        }
                        mLogger.info("*SC* *** SL device provisioning in SC mode ended unsuccessfully - SL device is not connected to the network ***");
                        makeManualVisible();
                    }
                }
            } else {
                if (result != null) {

                    if (NetworkUtil.getResultTypeCFGString(result) == CFG_Result_Enum.Not_Started) {
                        if (ssidToAdd == null || ssidToAdd.equals("")) {
                            showToastWithMessage(Constants.SMART_CONFIG_MUST_CHOOSE_NETWORK_TO_BEGIN);
                            mLogger.info("*SC* *** SL device provisioning in SC mode ended unsuccessfully - no network to send to SL device in profile to connect to was chosen ***");
//                        } else if (chosenDevice != null) {
                        } else if (ssidToAdd != null) {
                            if (secondAPAAttemptCommitted) {
                                Log.i(TAG, "second attempt at APA already committed - so finished unsuccessfully");
                                mainActivity.showSuccessDialog("Provisioning Unsuccessful", getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
                                mLogger.info("*SC* *** SL device provisioning in SC mode ended unsuccessfully - SL device is  not connected to the network ***");
                                finish(false);
                            } else {
                                showLoaderWithText(true, "Starting configuration");
                                Log.i(TAG, "Provisioning process has not started yet - so running AddProfileAsyncTask");
                                mLogger.info("*SC* *** SL device provisioning in SC mode ended unsuccessfully - network was chosen for profile, but profile addition was unsuccessful" +
                                        " - another attempt at AddProfileAsyncTask will commence now via SL device as AP ***");
                                secondAPAAttemptCommitted = true;
                                ArrayList<Object> passing = new ArrayList<>();
                                passing.add(tab_device_configuration_sc_device_name_editText.getText().toString());
                                passing.add(ssidToAddSecurityType);
                                passing.add(ssidToAdd);
                                passing.add(ssidToAddSecurityKey);
                                passing.add(ssidToAddPriority);
                                passing.add(tab_device_configuration_iot_uuid_name_editText.getText().toString());

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                    new AddProfileAsyncTask(mAddProfileAsyncTaskCallback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, passing);
                                } else {
                                    new AddProfileAsyncTask(mAddProfileAsyncTaskCallback).execute(passing);
                                }
                            }
                        } else {
                            Log.i(TAG, "Provisioning process has not started yet, and no access point was chosen for Wi-Fi profile");
                            mLogger.info("*SC* *** SL device provisioning in SC mode ended unsuccessfully - no network to send to SL device in profile to connect to was chosen ***");
//                            showToastWithMessage(Constants.SMART_CONFIG_CFG_RESULT_NOT_STARTED);
                        }
                    } else if (NetworkUtil.getResultTypeCFGString(result) == CFG_Result_Enum.Success) {
                        mainActivity.showSuccessDialog(null, getString(R.string.pop_up_close), null, PopupType.Success, null, result);
                        mLogger.info("*SC* *** SL device provisioning in SC mode ended successfully - SL device is connected to the network ***");
                        finish(true);
                    } else {
                        if (result != null && !result.equals("")) {
                            mainActivity.showSuccessDialog(result, getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
                        } else {
                            mainActivity.showSuccessDialog("Provisioning Unsuccessful", getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
                        }
                        mLogger.info("*SC* *** SL device provisioning in SC mode ended unsuccessfully - SL device is  not connected to the network ***");
                        finish(false);
                        //wrong password bool triggered
                        wrongPassword = true;

                    }
                } else {
                    mLogger.info("*SC* Cfg verification retrieval from SL device attempt no. " + utilInt + " was unsuccessful");
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mLogger.info("*SC* Running another cfg verification retrieval attempt, no. " + ++utilInt);
                            cgfTryNumber++;
                            getCFG();
                        }
                    }, Constants.DELAY_BEFORE_FETCHING_SSIDS_FROM_DEVICE); //1 sec delay before trying again
                }
            }

            super.onPostExecute(result);
            if (mainActivity != null) {
                mainActivity.stopPing();
            }
        }


        @Override
        protected String doInBackground(String... params) {
            Log.e(TAG,"INSIDE doInBackground***");
            Log.i(TAG, "GetCFGResult doInBackground Started");
            mLogger.info("*SC* GetCFGResult doInBackground Started");
            String result = null;
            String baseUrl = params[0];
            String uuidStr = params[1];

            if (mDevice != null) {
                Log.d(TAG, "mDevice name: " + mDevice.name + " mDevice host: " + mDevice.host);
            } else {
                Log.d(TAG, "mDevicve null");
            }
            Log.d(TAG, "isDeviceAsAP: " + isDeviceAsAp);
            Log.d(TAG, "secondAPAAttemptCommitted: " + secondAPAAttemptCommitted);


            if (((mDevice != null) && !isDeviceAsAp) || ((mDevice != null) && secondAPAAttemptCommitted)) {
                Log.d(TAG, "isOTA - getting host");
//                baseUrl = mDevice.host;
                baseUrl = "://" + mDevice.host;
            }

            try {
                String resultString;

                print("Getting confirmation from SimpleLink device url: " + baseUrl);
                mLogger.info("*SC* Getting cfg verification from SimpleLink device url: " + baseUrl);

                DeviceVersion deviceVersion = NetworkUtil.getSLVersion(baseUrl);
                Log.i(TAG, "Device version:" + deviceVersion);
                mLogger.info("*SC* SL Device version: " + deviceVersion);

                if (!uuidStr.equals("")) {
                    Log.i(TAG, "Set uuid: " + uuidStr);
                    mLogger.info("*SC* Set uuid: " + uuidStr);
                    if (!NetworkUtil.setIotUuid(uuidStr, baseUrl)) {
                        Log.i(TAG, "Set uuid failed " + uuidStr);
                        mLogger.info("*SC* Set uuid failed " + uuidStr);
                    }
                }

                resultString = NetworkUtil.getCGFResultFromDevice(baseUrl, deviceVersion);
                Log.i(TAG, "Device resultString:" + resultString);
                mLogger.info("*SC* Device resultString: " + resultString);
                //print("Got response: " + resultString);
                CFG_Result_Enum result_Enum = NetworkUtil.cfgEnumForResponse(resultString);
                Log.i(TAG, "result_Enum: " + result_Enum);
                mLogger.info("result_Enum: " + result_Enum);
                result = NetworkUtil.getErrorMsgForCFGResult(result_Enum);
                Log.i(TAG, "result: " + result);
                mLogger.info("result: " + result);

            } catch (Exception e) {
                e.printStackTrace();
                mLogger.info("12.1 Failed to get confirmation , removing loader");
                //added 12.1
                showLoaderWithText(false,"");

            }

            return result;
        }
    }

    /**
     * callbacks for AddProfileAsyncTask.
     */
    private AddProfileAsyncTaskCallback mAddProfileAsyncTaskCallback = new AddProfileAsyncTaskCallback() {
        /**
         * Prints String passed as parameter to system with System.out.println
         * and displays it to user as loader label.
         *
         * @param errorMessage    String. The text to print and display.
         */
        @Override
        public void addProfileMsg(String errorMessage) {
            print(errorMessage);
        }

        /**
         * Displays the String passed as parameter in a toast and dismisses loader.
         *
         * @param errorMessage    String. The text to be displayed.
         */
        @Override
        public void addProfileFailed(String errorMessage) {
//            showToastWithMessage(errorMessage);
//            showLoaderWithText(false, "");
        }


        /**
         * Checks whether the SSID sent to SL device in Wi-Fi profile is within the mobile phone's range,
         * if it is - connects to that SSID and starts Ping and mDNS discovery in order to find the
         * SL device in that network, for configuration verification from it as station,
         * if it is not - moves to configuration verification from SL device as access point.
         * If connection to SSID fails, informs user of the cause for that failure.
         *
         * @see NewSmartConfigFragment#checkParams()
         * @see MainActivity#startPing()
         * @see MainActivity#scanForDevices()
         */
        @Override
        public void addProfileCompleted() {
            Log.d(TAG, "AddProfileAsyncTask addProfileComplete callback started");
            ScanResult result = null;
            wifiList = NetworkUtil.getWifiScanResults(true, mainActivity);
            for (ScanResult scanResult : wifiList) {
                if (scanResult.SSID.equals(ssidToAdd)) {
                    result = scanResult;
                    break;
                }
            }

            if (result == null) {
                print("Network " + ssidToAdd + " is not in range. Your mobile needs to connect to the device in order to confirm configuration has succeeded.");
                mLogger.info("*SC* Network " + ssidToAdd + " is not in range. Your mobile needs to connect to the device in order to confirm configuration has succeeded.");
                checkParams();
            } else {
                //wrong password
                if (ssidToAdd_WrongPassword == null) {
                    print("Connecting to configuration network in order to confirm device configuration has succeeded.");
                } else {
                    print("Connecting to " + ssidToAdd_WrongPassword + " in order to confirm device configuration has succeeded.");
                }
                if (wrongPassword == true) {
                    WifiNetworkUtils.getInstance(mainActivity).clearCallback();
                    WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(mainActivity, ssidToAdd_WrongPassword, ssidToAddSecurityType_WrongPassword, ssidToAddSecurityKey_WrongPassword);
                    WifiNetworkUtils.getInstance(mainActivity).connectToWifi(configuration, mainActivity, new BitbiteNetworkUtilsCallback() {

                        @Override
                        public void successfullyConnectedToNetwork(String ssid) {
                            WifiNetworkUtils.getInstance(mainActivity).clearCallback();
                            print("Connected to " + ssidToAdd_WrongPassword + ". Searching for new devices\n DO NOT ABORT!");
                            mLogger.info("Connected to " + ssidToAdd_WrongPassword + ". Searching for new devices");

                            foundNewDevice = false;
                            mSmartConfigInProgress = true;
                            waitForScanFinish = true;
                            waitForScanFinishForRestart = true;
                            mainActivity.startPing();
                            prefs.scanningDisable().put(false);
                            prefs.isScanning().put(false);
                            mainActivity.scanForDevices();
                        }

                        @Override
                        public void failedToConnectToNetwork(WifiConnectionFailure failure) {
                            WifiNetworkUtils.getInstance(mainActivity).clearCallback();
                            showLoaderWithText(false, null);
                            // show error
                            finish(false);
                            setToReady(false);

                            switch (failure) {
                                case Connected_To_3G:
                                    show3GDialog();
                                    break;
                                case Timeout:
                                case Unknown:
                                    showToastWithMessage("There was an unknown error connecting to the network");
                                    break;
                                case Wrong_Password:
                                    showToastWithMessage("The password you entered for the network is wrong please try again");
                                    break;
                            }
                        }
                    }, true);
                } else {
                    WifiNetworkUtils.getInstance(mainActivity).clearCallback();
                    WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(mainActivity, ssidToAdd, ssidToAddSecurityType, ssidToAddSecurityKey);
                    WifiNetworkUtils.getInstance(mainActivity).connectToWifi(configuration, mainActivity, new BitbiteNetworkUtilsCallback() {

                        @Override
                        public void successfullyConnectedToNetwork(String ssid) {
                            WifiNetworkUtils.getInstance(mainActivity).clearCallback();
                            print("Connected to " + ssidToAdd + ". Searching for new devices");
                            mLogger.info("*SC* Connected to " + ssidToAdd + ". Searching for new devices");
                            foundNewDevice = false;
                            mSmartConfigInProgress = true;
                            waitForScanFinish = true;
                            waitForScanFinishForRestart = true;
                            mainActivity.startPing();
                            mainActivity.restartUdp();
                            prefs.scanningDisable().put(false);
                            prefs.isScanning().put(false);
                            mainActivity.scanForDevices();
                        }

                        @Override
                        public void failedToConnectToNetwork(WifiConnectionFailure failure) {
                            WifiNetworkUtils.getInstance(mainActivity).clearCallback();
                            showLoaderWithText(false, null);
                            // show error
                            finish(false);
                            setToReady(false);

                            switch (failure) {
                                case Connected_To_3G:
                                    show3GDialog();
                                    break;
                                case Timeout:
                                case Unknown:
                                    showToastWithMessage("There was an unknown error connecting to the network");
                                    break;
                                case Wrong_Password:
                                    showToastWithMessage("The password you entered for the network is wrong please try again");
                                    break;
                            }
                        }
                    }, true);
                }
            }
        }

        /**
         * Empty.
         *
         * @param deviceName    String.
         */
        @Override
        public void addProfileDeviceNameFetched(String deviceName) {
        }
    };


    class OTAAndType extends AsyncTask<String, Void, Device_Type_Enum> {

        @Override
        protected Device_Type_Enum doInBackground(String... params) {

            Log.i(TAG, "OTAAndType doInBackground");

            Device_Type_Enum deviceTypeEnum = null;

            try {
//                String baseUrl = "http://" + mDevice.host;
                String baseUrl = "://" + mDevice.host;
                Log.i(TAG, "OTAAndType baseUrl: " + baseUrl);

                try {
                    deviceTypeEnum = NetworkUtil.slDeviceOTAAndType(baseUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "OTAAndTypeA: " + deviceTypeEnum);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            return deviceTypeEnum;

        }

        @Override
        protected void onPostExecute(Device_Type_Enum deviceTypeEnum) {
            super.onPostExecute(deviceTypeEnum);

            Log.i(TAG, "OTAAndType onPost, result: " + deviceTypeEnum);

            mainActivity.deviceTypeEnum = deviceTypeEnum;
            Log.i(TAG, "OTAAndType set result to main: " + mainActivity.deviceTypeEnum);

                mainActivity.clearAllTabs();

            if (startingSSID != null && startingSSID.equalsIgnoreCase(WifiNetworkUtils.getInstance(mainActivity).getConnectedSSID())) {
                //refresh tabs in order to display extra tabs - and move to Devices tab
                mainActivity.initTabs(1);
            } else {
                //refresh tabs in order to display extra tabs - and do not move to Devices tab
                mainActivity.initTabs(0);
            }
        }


    }





    /**
     * Resets the UI device to configure field, and informs user of the cause of the failure to
     * connect to SL device for configuration confirmation.
     *
     * @param failure WifiConnectionFailure. Cause of the wifi connection failure.
     */
    private void failedToConnectToSLForConfirmation(WifiConnectionFailure failure) {
        showLoaderWithText(false, "");
        tab_device_configuration_device_to_configure_device_pick_image.setImageResource(R.drawable.new_graphics_white_box_pick_red);
        tab_device_configuration_device_to_configure_device_pick_label.setTextColor(getResources().getColor(R.color.color_line));
        tab_device_configuration_device_to_configure_device_pick_label.setText(getString(R.string.tab_device_configuration_ap_search_device));

        switch (failure) {
            case Connected_To_3G:
                show3GDialog();
                break;
            case Timeout:
            case Unknown:
                showToastWithMessage("There was an unknown error connecting to the simplelink");
                break;
            case Wrong_Password:
                showToastWithMessage("The password you entered for the simplelink is wrong please try again");
                break;
        }
    }

    /**
     * Hides the virtual keyboard from the window.
     *
     * @param view View. The view which lost focus (which caused this method to be called)
     */
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) mainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Called upon a change in mobile phone's Wi-Fi connectivity,
     * analyzes the security type of the SSID the mobile phone is currently connected to,
     * sets the UI password field and "Start Configuration" button state accordingly,
     * and sets the current SSID as the SSID to send to SL device as profile to connect to,
     * sets the UI "Device connection" bar's state (background color and text)
     * to reflect the current Wi-Fi connection status of the mobile phone.
     */
    private void DisplayWifiState() {
        Log.e(TAG,"DisplayWifiState**");
        ConnectivityManager myConnManager = (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        String ssid = NetworkUtil.getConnectedSSID(mainActivity);

        WifiManager wifi = (WifiManager) mainActivity.getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> networkList = wifi.getScanResults();
        if (ssid == null || ssid.equals("")) {
            tab_device_sc_configuration_password_check_layout.setVisibility(View.GONE);
            tab_device_configuration_network_to_configure_network_pick_label.setTextColor(getResources().getColor(R.color.color_connection_text_sc_holo_grey));
            tab_device_configuration_network_to_configure_network_pick_label.setText(getString(R.string.new_smart_config_fragment_search_for_your_network));
            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
            tab_device_configuration_device_connection.setText(getResources().getString(R.string.new_smart_config_fragment_no_wifi_connection));//replace text
            tab_device_configuration_device_connection.setTextColor(Color.WHITE);
            textViewConnectionTextView.setBackgroundColor(getResources().getColor(R.color.color_red));
            mIsReady = false;
            setToReady(mIsReady);
        }
        if (ssid != null) {
            if (networkList != null) {
                if (!isDeviceAsAp) {
                    for (ScanResult network : networkList) {

                        if (ssid.equalsIgnoreCase(network.SSID)) {

                            //get capabilities of current connection
                            String Capabilities = network.capabilities;
                            Log.d(TAG, network.SSID + " capabilities : " + Capabilities);
                            if (mLogger != null) {
                                mLogger.info("*SC* Currently connected via wifi to: \"" + network.SSID + "\" capabilities: " + Capabilities);
                            }

                            if (Capabilities.contains("WPA2")) {
                                tab_device_sc_configuration_password_check_layout.setVisibility(View.VISIBLE);
                                tab_device_sc_configuration_password_check_editText.setText("");
                                tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
                                ssidToAddSecurityType = ssidToAddSecurityType.WPA2;
                                ssidToAdd = ssid;
                                ssidToAddPriority = "0";
                                mIsReady = false;
                                setToReady(mIsReady);
                                Log.v(TAG, "+++++++++++++++++++++++++");
                                Log.v(TAG, "++++++++++WPA2+++++++++++");
                                Log.v(TAG, "+++++++++++++++++++++++++");

                            } else if (Capabilities.contains("WPA")) {
                                tab_device_sc_configuration_password_check_layout.setVisibility(View.VISIBLE);
                                tab_device_sc_configuration_password_check_editText.setText("");
                                tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
                                ssidToAddSecurityType = ssidToAddSecurityType.WPA1;
                                ssidToAdd = ssid;
                                ssidToAddPriority = "0";
                                mIsReady = false;
                                setToReady(mIsReady);
                                Log.v(TAG, "+++++++++++++++++++++++++");
                                Log.v(TAG, "++++++++++WPA1+++++++++++");
                                Log.v(TAG, "+++++++++++++++++++++++++");

                                Toast.makeText(mainActivity, "WPA", Toast.LENGTH_LONG).show();
                            } else if (Capabilities.contains("WEP")) {
                                tab_device_sc_configuration_password_check_layout.setVisibility(View.VISIBLE);
                                tab_device_sc_configuration_password_check_editText.setText("");
                                tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
                                ssidToAddSecurityType = ssidToAddSecurityType.WEP;
                                ssidToAdd = ssid;
                                ssidToAddPriority = "0";
                                mIsReady = false;
                                setToReady(mIsReady);
                                Log.v(TAG, "+++++++++++++++++++++++++");
                                Log.v(TAG, "+++++++++++WEP+++++++++++");
                                Log.v(TAG, "+++++++++++++++++++++++++");

                            } else if (Capabilities.contains("ESS")) {
                                tab_device_sc_configuration_password_check_layout.setVisibility(View.GONE);
                                Log.v(TAG, "+++++++++++++++++++++++++");
                                Log.v(TAG, "+++++++++++OPEN+++++++++++");
                                Log.v(TAG, "+++++++++++++++++++++++++");

                                ssidToAdd = ssid;
                                ssidToAddPriority = "0";
                                ssidToAddSecurityKey = "";
                                ssidToAddSecurityType = ssidToAddSecurityType.OPEN;
//                                tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
//                                tab_device_configuration_sc_start_button.setEnabled(true);
//                                mIsReady = true;
//                                setToReady(mIsReady);


                            }
//                            startConfigButtonState();
                            startConfigButtonStateNoNaming();
                        }
                    }
                }
            } else {
                tab_device_sc_configuration_password_check_layout.setVisibility(View.GONE);
            }
        }
        if (myNetworkInfo.isConnected()) {
            tab_device_configuration_device_connection.setText(String.format(getString(R.string.tab_device_configuration_connected_to_ssid), ssid));
            tab_device_configuration_device_connection.setTextColor(Color.WHITE);
            textViewConnectionTextView.setBackgroundColor(getResources().getColor(R.color.color_connection_text_sc_holo_grey));
            if (ssid != null && !(ssid.equals(""))) {

                tab_device_configuration_network_to_configure_network_pick_label.setText(ssid);
                tab_device_configuration_network_to_configure_network_pick_label.setTextColor(Color.BLACK);

                mGetCFGFromSC = true;
                //for handling wrong password situation with
                if (wrongPassword == true) {

                    tab_device_configuration_sc_device_to_configure_layout.setVisibility(View.VISIBLE);
                    tab_device_sc_configuration_password_check_editText.setVisibility(View.VISIBLE);

                } else if (!isDeviceAsAp) {
                    makeManualGone();
                }
            }
        } else {
            tab_device_configuration_network_to_configure_network_pick_label.setTextColor(getResources().getColor(R.color.color_connection_text_sc_holo_grey));
            tab_device_configuration_network_to_configure_network_pick_label.setText(getString(R.string.new_smart_config_fragment_search_for_your_network));
            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
            tab_device_configuration_device_connection.setText(getResources().getString(R.string.new_smart_config_fragment_no_wifi_connection));//replace text
            tab_device_configuration_device_connection.setTextColor(Color.WHITE);
            textViewConnectionTextView.setBackgroundColor(getResources().getColor(R.color.color_red));
            mIsReady = false;
            setToReady(mIsReady);
            //causing multiple windows -every connection change the method is fired .
            //	showNoWifiPopUp();
        }
    }

    /**
     * Starts smartConfig transmission of Wi-Fi profile to SL device,
     * once the transmission is complete connects to the access point
     * sent in the profile, and searches for the configured SL device.
     *
     * @see MainActivity#scanForDevices()
     * @see MainActivity#clear()
     * @see com.integrity_project.smartconfiglib.SmartConfig
     * @see com.integrity_project.smartconfiglib.SmartConfig#SmartConfig(SmartConfigListener, byte[], String, byte[], String, String, byte, String)
     * @see SmartConfig#transmitSettings()
     * @see NewSmartConfigFragment#lookForNewDevice()
     */
    public void startSmartConfig() {
        mDevice = null;
        mGetCFGFromSC = true;
        foundNewDevice = false;
        waitForScanFinish = false;
        waitForScanFinishForRestart = false;

        stopScan();

        handlerForTransmit.removeCallbacks(smartConfigRunner);

        showProgressDialog(prefs.smartConfigTransmitTime().get() * 1000, "Sending network configuration");
        if (!prefs.isScanning().get()) {
            Log.i(TAG, "Start smart config - start scan");
            mLogger.info("*SC* Start smart config - start mDNS discovery + PingBcast to find new SL devices");
            mainActivity.scanForDevices();
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Start smart config - already scan");
            mLogger.info("*SC* Start smart config - already scanning for new SL devices");
        }
        prefs.isSmartConfigActive().put(true);
        mSmartConfigInProgress = true;
        mainActivity.clear();
        prefs.scanningDisable().put(true);
        setToReady(false);
        startingSSID = mainActivity.mStartingWifiNetwork;
        Log.i(TAG, "Starting network is " + startingSSID + ".. Will return to it when it all ends");

        if ((ssidToAdd == null || ssidToAdd.equals(""))) {
            print("Please choose network first");
            return;
        }


        String passwordKey = ssidToAddSecurityKey.trim();
        String SSID = ssidToAdd.trim();
        byte[] paddedEncryptionKey;
        String gateway = NetworkUtil.getGateway(mainActivity);
        paddedEncryptionKey = null;

        //For Ofer
        paddedEncryptionKey = tab_device_configuration_configuration_key_editText.getText().toString().trim().getBytes();

        ///
        uiDeviceName = tab_device_configuration_sc_device_name_editText.getText().toString();


        int uiDeviceNameLength = uiDeviceName.length();
        mLogger.info("*SC* \n*** Send \nName: " + uiDeviceName + "\nLen: " + uiDeviceNameLength + ", Pass len: " + passwordKey.length() + "\n***");

        if (uiDeviceName.length() > 0) { // device name isn't empty
            byte[] freeDataChars = new byte[uiDeviceNameLength + 2];
            freeDataChars[0] = 0x03;
            freeDataChars[1] = (byte) uiDeviceNameLength;
            for (int i = 0; i < uiDeviceNameLength; i++) {
                freeDataChars[i + 2] = (byte) uiDeviceName.charAt(i);
            }
            freeData = freeDataChars;
        } else {
            freeData = null;
        }

        smartConfig = null;


        smartConfigListener = new SmartConfigListener() {

            @Override
            public void onSmartConfigEvent(SmtCfgEvent event, Exception e) {
            }
        };

        try {
            try {
              //  SSID = new String(SSID.getBytes("UTF-8"),("UTF-16"));
                smartConfig = new SmartConfig(smartConfigListener, freeData, passwordKey, paddedEncryptionKey, gateway, SSID, (byte) 0, "");
                Log.e(TAG, "** SSID ** :"+SSID);
            } catch (SocketException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to create instance of smart config");
                mLogger.info("*SC* Failed to create instance of smart config object");
                return;
            }

            //int transmitTime = prefs.smartConfigTransmitTime().get();
            int transmitTime = 40;

            Log.i(TAG, "Broadcasting information to network");
            mLogger.info("*SC* Broadcasting configuration information to network via SmartConfig");
            smartConfig.transmitSettings();
//			showProgressDialog(prefs.smartConfigTransmitTime().get() * 1000, "Sending network configuration");
            print("Broadcasting");
            showLoaderWithText(true, "");


            smartConfigRunner = new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "running smartConfigRunner");
                    mLogger.info("*SC* running smartConfigRunner");
                    prefs.isSmartConfigActive().put(false);

                    try {
                        smartConfig.stopTransmitting();
                        Log.i(TAG, "Broadcasting information to network finished");
                        mLogger.info("*SC* SmartConfig Broadcasting configuration information to network finished");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String SSID = null;
                    if (mainActivity != null) {
                        if (mainActivity.mStartingWifiNetwork != null) {
                            SSID = mainActivity.mStartingWifiNetwork;
                        }
                    }
                    try {
                        mainActivity.startPing();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    if (ssidToAdd != null) {
                        SSID = ssidToAdd.trim();
                    }
                    String currentWifi = NetworkUtil.getConnectedSSID(mainActivity);
                    if (currentWifi != null && currentWifi.equals(SSID)) {
                        mLogger.info("*SC* connected to network sent to SL device in profile to connect to - looking for new SL devices in the local network");
                        //print("Starting mdns scan");
                        waitForScanFinishForRestart = false;
                        lookForNewDevice();
                    } else {
                        print("Connecting to " + SSID);
                        mLogger.info("*SC* Connecting to network sent to SL device in profile to connect to, in order to find the configured SL device");
                        WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(mainActivity, SSID, ssidToAddSecurityType, ssidToAddSecurityKey);
                        WifiNetworkUtils.getInstance(mainActivity).connectToWifi(configuration, mainActivity, new BitbiteNetworkUtilsCallback() {
                            @Override
                            public void successfullyConnectedToNetwork(String ssid) {
                                //print("Starting mdns scan");
                                mLogger.info("*SC* connected to network sent to SL device in profile to connect to - looking for new SL devices in the local network");
                                waitForScanFinishForRestart = false;
                                lookForNewDevice();
                            }

                            @Override
                            public void failedToConnectToNetwork(WifiConnectionFailure failure) {
                                mLogger.info("*SC* Failed to connect to the network sent to SL device as profile to connect to -" +
                                        " can not look for new SL devices in the local network");
                                showLoaderWithText(false, "");
                                showToastWithMessage("Failed to connect to the network");
                            }
                        }, true);
                    }

                }

            };
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to start smart config " + e.getMessage());
            mLogger.info("*SC* Failed to start smart config");
        }
        handlerForTransmit.postDelayed(smartConfigRunner, 40 * 1000);
        Log.i(TAG, "smartConfigRunner ended");
        mLogger.info("*SC* smartConfigRunner ended");
    }

    /**
     * Displays and updates a progress dialog in relation to the smartConfig transmission,
     * after a predetermined duration dismisses the progress dialog and
     * starts mDNS discovery and Ping broadcast to discover the configured SL device.
     *
     * @param duration int.The duration of time for the progress dialog to be displayed.
     * @param title    String. The title for the progress dialog.
     */

    @UiThread
    void showProgressDialog(int duration, String title) {

        if (progressDialog != null && progressDialog.isShowing()) {
            Log.w(TAG, "Progress dialog is already showing!");
            return;
        }

        print(title);
        progressDialog = new ProgressDialog(mainActivity);

        progressDialog.setCancelable(false);
        progressDialog.setMessage(title);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(duration / 1000);
        progressDialog.setProgressDrawable(mainActivity.getResources().getDrawable(R.drawable.progress_bar_shape));
        progressDialog.show();
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {

            }
        });
        //double tap cancellation when progress dialog is up
        progressDialog.setOnKeyListener(new Dialog.OnKeyListener() {
            private long lastPressedTime;
            private static final int PERIOD = 2000;

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {

                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        if (event.getDownTime() - lastPressedTime < PERIOD) {
                            mLogger.info("*SC* Cancel action activated");
                            finish(false);
                            mainActivity.stopScanning();

                            prefs.scanningDisable().put(true);
                            prefs.isSmartConfigActive().put(false);
                            progressDialog.dismiss();
                            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);

                            showLoaderWithText(true, "Cancelling transmission");

                            Thread cancellationThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    //current time
                                    showLoaderWithText(true, "Cancelling transmission");
                                    stopScLoader= true;
                                    finish(false);
                                    mainActivity.stopScanning();
                                  //  mainActivity.clear();
                                    prefs.scanningDisable().put(true);
                                    prefs.isSmartConfigActive().put(false);
                                    progressDialog.dismiss();
                                    try {
                                        smartConfig.stopTransmitting();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    stopScan();
//                                    showLoaderWithText(true,"Cancelling transmission");

                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }


                                    showLoaderWithText(false, "");


                                }
                            });

                            cancellationThread.start();


                        } else {
                            Toast.makeText(mainActivity, "Press again to cancel action",
                                    Toast.LENGTH_SHORT).show();
                            lastPressedTime = event.getEventTime();
                        }
                        return true;
                }
                return false;
            }
        });


        progThread = new Thread(new Runnable() {

            int progress = 0;

            public void run() {
                if (Thread.interrupted()) {
                    return;
                }
                long timerEnd = System.currentTimeMillis() + progressDialog.getMax() * 1000;

                while (timerEnd > System.currentTimeMillis()) {

                    if (progressDialog == null || !progressDialog.isShowing())
                        break;

                    progress = (int) (progressDialog.getMax() - (timerEnd - System.currentTimeMillis()) / 1000);
                    updateProgressDialog(progress);

                    if (Thread.interrupted()) {
                        return;
                    }
//					if (progress*2>progressDialog.getMax())
//					{
//						prefs.scanningDisable().put(false);
//						startScan();
//					}
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (Thread.interrupted()) {
                    return;
                }

                try {
                    mainActivity.scanForDevices();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
            }
        });

        progThread.start();

    }

    /**
     * Updates the smartConfig transmission progress dialog.
     *
     * @param progress int. New progress value.
     */
    @UiThread
    void updateProgressDialog(int progress) {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.setProgress(progress);
    }

    /**
     * Calls a method which starts mDNS discovery and Ping broadcast.
     *
     * @see NewSmartConfigFragment#startScan()
     */
    @Background
    void lookForNewDevice() {
        prefs.scanningDisable().put(false);
        prefs.isScanning().put(false);
        if (!waitForScanFinishForRestart) {
            waitForScanFinish = true;
        }
        startScan();
//		if (mSmartConfigInProgress) {
//			try {
//				devicesArray = new JSONArray(prefs.devicesArray().get()); //save whatever devices we found so far
//				//recentDevicesArray = new JSONArray(prefs.recentDevicesArray().get());
//				if (prefs.isScanning().get()) { // if main activity is still scanning
//					Log.i(TAG, "Already scanning, stopping to restart");
//					waitForScanFinishForRestart = true; // flag to indicate we are waiting for the main activity's scan to finish
//					Log.w(TAG, "lookForNewDevice stop scanning mDNS");
//					((MainActivity) getActivity()).stopScanning();
//				} else if (!waitForScanFinishForRestart) { // main activity is done scanning and we're not waiting for scan finish
//					showProgressDialog(SmartConfigConstants.MAIN_SCAN_TIME, Constants.MDNS_SCAN_MESSAGE);
//					waitForScanFinish = true;
//					prefs.scanningDisable().put(false);
//					startScan();
//				}
//			} catch (JSONException e) {
//				Log.e(TAG, "Failed to pare json");
//			}
//		}
    }

    /**
     * Calls a method which starts mDNS discovery and Ping broadcast.
     *
     * @see MainActivity#scanForDevices()
     */
    private void startScan() {
        Log.i(TAG, "Starting mDNS scan");
        mLogger.info("*SC* Start M scan");
        mainActivity.scanForDevices();
    }

    /**
     * Calls a method which terminates mDNS discovery,
     * and calls another method which handles the mDNS discovery cycle.
     *
     * @see MainActivity#stopScanning()
     */
    private void stopScan() {
        Log.i(TAG, "Stopping mDNS scan");
        mLogger.info("*SC* Stop M scan");
        mainActivity.stopScanning();
    }

    /**
     * Checks whether the SL device represented by the JSONObject passed
     * as parameter is a new device or had already been discovered.
     *
     * @param deviceJSON JSONObject. Representing the SL device in question.
     * @return boolean. True - if the SL device is new.
     * False - if the SL device had already been discovered.
     */
    public boolean isNewDevice(JSONObject deviceJSON) {
        try {
            for (int i = 0; i < devicesArray.length(); i++) {
                if (devicesArray.getJSONObject(i).getString("host").equals(deviceJSON.getString("host"))) {
                    if (devicesArray.getJSONObject(i).getString("name").equals(deviceJSON.getString("name"))) {
                        return false;
                    } else {
                        devicesArray = removeFromJSONArray(devicesArray, i);
                        prefs.devicesArray().put(devicesArray.toString());
                        return true;
                    }
                }
            }
//			for (int i = 0; i < recentDevicesArray.length(); i++) {
//				if (recentDevicesArray.getJSONObject(i).getString("host").equals(deviceJSON.getString("host"))) {
//					if (recentDevicesArray.getJSONObject(i).getString("name").equals(deviceJSON.getString("name"))){
//						return false;
//					} else {
//						recentDevicesArray = removeFromJSONArray(recentDevicesArray, i);
//						prefs.recentDevicesArray().put(recentDevicesArray.toString());
//						return true;
//					}
//				}
//			}
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return true;
    }

    /**
     * Returns a JSONArray containing the contents of the JSONArray passed as parameter,
     * minus the JSONObject whose index was passed as parameter.
     *
     * @param array JSONArray. The array in need of item removal.
     * @param index int. The index in the JSONArray of the JSONObject to be removed.
     * @return JSONArray. A JSONArray containing the contents of the JSONArray
     * passed as parameter, minus the removed item.
     */
    public JSONArray removeFromJSONArray(JSONArray array, int index) {
        JSONArray result = new JSONArray();
        try {
            for (int i = 0; i < array.length(); i++) {
                if (i != index) {
                    result.put(array.getJSONObject(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Displays an AlertDialog informing the user no Wi-Fi connection currently exists,
     * and allows to either connect to Wi-Fi via mobile device's Wi-Fi settings
     * or connect to SL device as access point if no router is available.
     *
     * @see NewSmartConfigFragment#makeManualVisible()
     */
    @UiThread
    void showNoWifiPopUp() {

//        AlertDialog wifiDialog = new AlertDialog.Builder(getActivity()). //create a dialog
//                setTitle("No " +
//                " Connection").
//                setMessage("Please connect your Smart Phone to router").
        AlertDialog wifiDialog = new AlertDialog.Builder(mainActivity). //create a dialog
                setTitle("No " +
                " Connection").
                setMessage("Please connect your Smart Phone to router").


                setPositiveButton("Connect to WiFi", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) { //the user clicked yes
                        WifiManager wifiManager = (WifiManager) mainActivity.getSystemService(Context.WIFI_SERVICE);
                        if (!wifiManager.isWifiEnabled())
                            wifiManager.setWifiEnabled(true);
                        tab_device_sc_configuration_password_check_layout.setVisibility(View.INVISIBLE);
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                }).setNeutralButton("No router", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                makeManualVisible();
            }
        }).create();
        wifiDialog.show();

    }

    /**
     * Enables the user to pick SL device from list of available devices,
     * and connect to it as an access point.
     */
    private void makeManualVisible() {
        cgfTryNumber = 0;
        mGetCFGFromSC = false;
        tab_device_configuration_sc_device_to_configure_layout.setVisibility(View.VISIBLE);
        isDeviceAsAp = true;
        switch (ssidToAddSecurityType) {
            case WEP:
            case WPA1:
            case WPA2:
                tab_device_sc_configuration_password_check_layout.setVisibility(View.VISIBLE);
                tab_device_sc_configuration_password_check_editText.setText(ssidToAddSecurityKey);
                break;
            case OPEN:
                tab_device_sc_configuration_password_check_layout.setVisibility(View.GONE);
                break;
        }

    }

    /**
     * Displays the Device pick pop up view, which enables the user to pick an SL device to connect to as access point.
     *
     * @see com.ti.smartconfig.utils.DevicePickPopUpView
     */
    @Click
    void tab_device_configuration_device_to_configure_device_pick_image() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.cancel();
            alertDialog = null;
        } else {
            alertDialog = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        devicePickPopUpView = DevicePickPopUpView_.build(mainActivity);
        alertDialog = builder.create();
        alertDialog.setView(devicePickPopUpView, 0, 0, 0, 0);
        devicePickPopUpView.newSmartConfigFragment = this;
        devicePickPopUpView.start();
        alertDialog.show();
    }

    /**
     * Displays the Scan results pop up view, which enables the user to pick an access point to send to SL device in Wi-Fi profile.
     *
     * @see com.ti.smartconfig.utils.ScanResultsPopUpView
     */
    @Click
    void tab_device_configuration_network_to_configure_network_pick_image() {

//        setToReady(false);


        DisplayWifiState();
//        startConfigButtonState();
        startConfigButtonStateNoNaming();

        if (NetworkUtil.getConnectedSSID(mainActivity) != null) {
            tab_device_configuration_network_to_configure_network_pick_label.setTextColor(mainActivity.getResources().getColor(R.color.color_line));
            tab_device_configuration_network_to_configure_network_pick_label.setText(getString(R.string.tab_device_configuration_ap_search_network));
            makeManualGone();
        }

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.cancel();
            alertDialog = null;
        } else {
            alertDialog = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        scanResultsPopUpView = ScanResultsPopUpView_.build(mainActivity);
        scanResultsPopUpView.newSmartConfigFragment = this;
        scanResultsPopUpView.start();
        alertDialog = builder.create();
        alertDialog.setView(scanResultsPopUpView, 0, 0, 0, 0);
        alertDialog.show();
    }

    /**
     * Called when an SL device to configure had been chosen in the Device pick pop up view,
     * sets the UI "Choose device to configure" field accordingly, connects to that SL device and
     * moves to the version retrieval stage of the provisioning process.
     *
     * @param result       ScanResult. The chosen SL device.
     * @param securityType SecurityType. The chosen SL device's security type.
     * @param password     String. The chosen SL device's password.
     */
    public void deviceWasChosen(ScanResult result, SecurityType securityType, String password) {

        uiDeviceName = tab_device_configuration_sc_device_name_editText.getText().toString();

        if (alertDialog != null && alertDialog.isShowing()) {

            alertDialog.cancel();
            alertDialog = null;
        } else {
            alertDialog = null;
        }

        chosenDevice = result;
        tab_device_configuration_device_to_configure_device_pick_image.setImageResource(R.drawable.new_graphics_scrool_choose);
        tab_device_configuration_device_to_configure_device_pick_label.setTextColor(Color.BLACK);
        tab_device_configuration_device_to_configure_device_pick_label.setText(chosenDevice.SSID);

        showLoaderWithText(true, "");
        print("Connecting to " + chosenDevice.SSID);
        mLogger.info("*SC* Connecting to \"" + chosenDevice.SSID + "\"");
        mDevice = null;

        WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(mainActivity, chosenDevice.SSID, securityType, password);
        WifiNetworkUtils.getInstance(mainActivity).connectToWifi(configuration, mainActivity, new BitbiteNetworkUtilsCallback() {
            @Override
            public void successfullyConnectedToNetwork(String ssid) {
                WifiNetworkUtils.getInstance(mainActivity).clearCallback();
                print("Your mobile is connected to " + ssid);
                mLogger.info("*SC* Your mobile is connected to " + ssid);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    new GetDeviceVersion().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "Sion");
                } else {
                    new GetDeviceVersion().execute("Sion");
                }
            }

            @Override
            public void failedToConnectToNetwork(WifiConnectionFailure failure) {
                WifiNetworkUtils.getInstance(mainActivity).clearCallback();
                failedToConnectToSLForConfirmation(failure);
            }
        }, false);
    }

    /**
     * Called when an access point to send to SL device in Wi-Fi profile has been chosen in the Scan results pop up view,
     * Sets the UI "WiFi network" field and "Start Configuration" button accordingly.
     *
     * @see NewSmartConfigFragment#setToReady(Boolean)
     */
    @UiThread
    public void closeDialog() {

        ConnectivityManager myConnManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        String ssid = NetworkUtil.getConnectedSSID(getActivity());
        tab_device_configuration_network_to_configure_network_pick_label.setText(ssidToAdd);
        tab_device_configuration_network_to_configure_network_pick_image.setImageResource(R.drawable.new_graphics_scrool_choose);
        tab_device_configuration_network_to_configure_network_pick_label.setTextColor(Color.BLACK);


        alertDialog.cancel();
        alertDialog = null;
        scanResultsPopUpView = null;

        if (NetworkUtil.getConnectedSSID(mainActivity) == null) {
            setToReady(false);
        } else {
            setToReady(true);
        }

        if (ssid != ssidToAdd) {
            //29.11 add auto connection to wifi tomorrow


            print("Connecting to " + ssidToAdd);
            mLogger.info("*SC* Connecting to network sent to SL device in profile to connect to, in order to find the configured SL device");
            WifiConfiguration configuration = NetworkUtil.getWifiConfigurationWithInfo(mainActivity, ssidToAdd, ssidToAddSecurityType, ssidToAddSecurityKey);
            showLoaderWithText(true,"Connecting to the desired access point");
            WifiNetworkUtils.getInstance(mainActivity).connectToWifi(configuration, mainActivity, new BitbiteNetworkUtilsCallback() {
                @Override
                public void successfullyConnectedToNetwork(String ssid) {
                    //print("Starting mdns scan");
                    showLoaderWithText(false, "");
                    if (ssidToAddSecurityKey != null) {
                        new Handler().postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if(ssidToAddSecurityType!=SecurityType.OPEN) {
                                    tab_device_sc_configuration_password_check_layout.setVisibility(View.VISIBLE);
                                    tab_device_sc_configuration_password_check_editText.setText(ssidToAddSecurityKey);
                                }
                            }
                        }, 1000);

                    }
                }

                @Override
                public void failedToConnectToNetwork(WifiConnectionFailure failure) {

                    showToastWithMessage("Failed to connect to the network");
                    showLoaderWithText(false, "");
                }
            }, true);
       }
    }

    /**
     * Enables/Disables UI "Start Configuration" button according to Boolean passed as parameter.
     *
     * @param isReady Boolean. True - to enable UI "Start Configuration" button.
     *                False - to disable UI "Start Configuration" button.
     */
    @UiThread
    public void setToReady(Boolean isReady) {
        mIsReady = isReady;
        if (mIsReady) {
            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_on);
        } else {
            tab_device_configuration_sc_start_button.setImageResource(R.drawable.start_configuration_button_off);
        }
    }

    /**
     * Displays or dismisses the loader, depending on the Boolean value passed as parameter.
     *
     * @param show Boolean. True to display loader, false to dismiss.
     * @param msg  String. The text to be displayed if the loader is to be shown.
     */
    @UiThread
    void showLoaderWithText(Boolean show, String msg) {
        if (!show) {
            tab_sc_loader_layout.setVisibility(View.GONE);
            tab_sc_loader_label.setText("");
            if(stopScLoader){
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        } else {
            tab_sc_loader_layout.setVisibility(View.VISIBLE);
            tab_sc_loader_label.setText(msg);
        }
    }

    /**
     * Retrieves device version from SL device,
     * moves to configuration verification stage if version successfully retrieved
     * or resets the UI "Choose device to configure" field and informs user of failure,
     * if version not successfully retrieved.
     *
     * @see NetworkUtil#getSLVersion(String)
     * @see NewSmartConfigFragment#confirmResult()
     */
    class GetDeviceVersion extends AsyncTask<String, Void, DeviceVersion> {

        @Override
        protected void onPostExecute(DeviceVersion result) {

            Log.i(TAG, "GetDeviceVersion - onPostExecute, result=" + result);
            mLogger.info("*SC* GetDeviceVersion result=" + result);
            if (result != DeviceVersion.UNKNOWN) {
                mGetCFGFromSC = false;
                if (mDevice == null) {
                    //SL device was not discovered as STA in network but connected to as AP for configuration
                    // verification - so no mDevice was obtained, obtain via HTTP Server

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        new GetDeviceIP().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
                    } else {
                        new GetDeviceIP().execute("");
                    }

                }
                confirmResult();
            } else {
                showLoaderWithText(false, "");
                tab_device_configuration_device_to_configure_device_pick_image.setImageResource(R.drawable.new_graphics_white_box_pick_red);
                tab_device_configuration_device_to_configure_device_pick_label.setTextColor(getResources().getColor(R.color.color_line));
                tab_device_configuration_device_to_configure_device_pick_label.setText(getString(R.string.tab_device_configuration_ap_search_device));
                tab_device_sc_configuration_password_check_layout.setVisibility(View.INVISIBLE);
                mainActivity.showSuccessDialog(Constants.DEVICE_LIST_FAILED_TO_GET_RESULTS, getString(R.string.pop_up_close), null, PopupType.Failure, null, null);
            }

            super.onPostExecute(result);
        }

        @Override
        protected DeviceVersion doInBackground(String... params) {
            Log.i(TAG, "GetDeviceVersion - doInBackground");
            mLogger.info("*SC* GetDeviceVersion - doInBackground");
            DeviceVersion deviceVersion = DeviceVersion.UNKNOWN;
            try {
                deviceVersion = NetworkUtil.getSLVersion(Constants.BASE_URL);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return deviceVersion;
        }
    }


    class GetDeviceIP extends AsyncTask<String, Void, String> {

        @Override
        protected void onPostExecute(String ip) {
            super.onPostExecute(ip);

            Log.i(TAG, "GetDeviceIP - OnPostExecute, ip: " + ip);
            mLogger.info("*SC* GetDeviceIP - onPostExecute, ip: " + ip);

            if (!ip.equals("")) {
                if (chosenDevice != null) {
                    mDevice = new Device(chosenDevice.SSID, ip);
                } else {
                    mDevice = new Device("", ip);
                }
            }
        }

        @Override
        protected String doInBackground(String... params) {

            Log.i(TAG, "GetDeviceIP - DoInBackground");
            mLogger.info("*SC* GetDeviceIP - doInBackground");
            String deviceIp = "";
            deviceIp = NetworkUtil.getDeviceIp(Constants.BASE_URL_NO_HTTP);

            return deviceIp;
        }
    }

    /**
     * Calls a method which shows an AlertDialog informing the user of data flowing
     * via the mobile network that should be turned off,
     * and allows the user quick access to the relevant settings.
     *
     * @see MainActivity#show3GDialog()
     */
    private void show3GDialog() {
        MainActivity main = mainActivity;
        main.show3GDialog();
    }

    public void tabEnabler(Boolean bool) {
        ((MainActivity) getActivity()).tabhost.getTabWidget().getChildTabViewAt(4).setEnabled(bool);
        ((MainActivity) getActivity()).tabhost.getTabWidget().getChildTabViewAt(5).setEnabled(bool);
        ((MainActivity) getActivity()).tabhost.getTabWidget().getChildTabViewAt(3).setEnabled(bool);
    }

    /**
     * Removes "Choose device to configure" field from UI
     * and sets boolean representing access point mode connection to SL device to false.
     */
    private void makeManualGone() {
        tab_device_configuration_sc_device_to_configure_layout.setVisibility(View.GONE);
        isDeviceAsAp = false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }



}
