//*
//* Copyright (C) 2019 Texas Instruments Incorporated - http://www.ti.com/
//*
//*
//*  Redistribution and use in source and binary forms, with or without
//*  modification, are permitted provided that the following conditions
//*  are met:
//*
//*    Redistributions of source code must retain the above copyright
//*    notice, this list of conditions and the following disclaimer.
//*
//*    Redistributions in binary form must reproduce the above copyright
//*    notice, this list of conditions and the following disclaimer in the
//*    documentation and/or other materials provided with the
//*    distribution.
//*
//*    Neither the name of Texas Instruments Incorporated nor the names of
//*    its contributors may be used to endorse or promote products derived
//*    from this software without specific prior written permission.
//*
//*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
//*  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
//*  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
//*  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
//*  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//*  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
//*  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
//*  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
//*  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//*  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//*  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//*
//*/
package com.ti.smartconfig;
import android.support.v4.app.Fragment;

import android.view.View;

import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;

import java.util.ArrayList;


import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.os.Bundle;
import com.loopj.android.http.*;
import android.widget.EditText;

import org.androidannotations.annotations.ViewById;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.text.TextUtils;
import java.util.concurrent.TimeUnit;


import cz.msebera.android.httpclient.Header;

@EFragment(R.layout.tab_schedule_pane)
public class SchedulePane extends Fragment implements View.OnClickListener{
    String URL="https://seniordesigndb.herokuapp.com/";
    String med_change;
    ArrayAdapter<String> adapter;
    ArrayAdapter<String> spin_adapter;
    ArrayList<ScheduleRecord>meds = new ArrayList<>();
    ArrayList<String>medsString = new ArrayList<>();
    ArrayList<String>medSpinnerOptions = new ArrayList<>();
    String med_id;

    @ViewById
    EditText edtDispName;
    @ViewById
    EditText edtHour;
    @ViewById
    EditText edtMin;
    @ViewById
    EditText edtDispQty;
    @ViewById
    ListView disp_records_view;
    @ViewById
    Button BtnDispPost;
    @ViewById
    Spinner disp_spinner;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }


    @AfterViews
    public void afterViews() {
        BtnDispPost.setOnClickListener(this);
        adapter = new ArrayAdapter<>(getActivity().getApplicationContext(),android.R.layout.simple_list_item_1, medsString);
        disp_records_view.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        spin_adapter = new ArrayAdapter<>(getActivity().getApplicationContext(),android.R.layout.simple_spinner_item,medSpinnerOptions);
        spin_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        disp_spinner.setAdapter(spin_adapter);
        disp_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                int index = arg0.getSelectedItemPosition();
                edtDispName.setText(medSpinnerOptions.get(index));
                med_change = medSpinnerOptions.get(index);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        getSchedule();
    }

    @Override
    public void onClick(View v) {
        addSchedule();
    }

    public void getSchedule() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("query", "select schedule.id,med_id,name,hour,minute,num_disp from medications, schedule where med_id = medications.id;");
        client.post(URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                super.onSuccess(statusCode, headers, res);

                try {
                    medsString.clear();
                    disp_records_view.invalidateViews();
                    String padedMedTitle = String.format("%-28s", "Medication");
                    String padedMedTime = String.format("%-17s", "Dispense Time");
                    String padedMedQty = "Qty";
                    medsString.add(padedMedTitle + " " + padedMedTime + " " + padedMedQty);
                    JSONArray jsonArray = res.getJSONArray("data");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ScheduleRecord s = new ScheduleRecord();
                        s.medName = jsonArray.getJSONObject(i).getString("name");
                        s.medicationID = jsonArray.getJSONObject(i).getString("med_id");
                        s.medHour = jsonArray.getJSONObject(i).getString("hour");
                        s.medMinute = jsonArray.getJSONObject(i).getString("minute");
                        s.medDispenseQty = jsonArray.getJSONObject(i).getString("num_disp");
                        meds.add(s);
                        String padedMed = String.format("%-30s", s.medName);
                        if (Integer.parseInt(s.medMinute) < 10) {
                            s.medMinute = "0" + Integer.toString(Integer.parseInt(s.medMinute));
                        }
                        if (Integer.parseInt(s.medHour) < 10) {
                            s.medHour = "0" + Integer.toString(Integer.parseInt(s.medHour));
                        }
                        String padedTime = String.format("%-22s", s.medHour + ":" + s.medMinute);
                        String input = padedMed + padedTime + s.medDispenseQty;


                        medsString.add(input);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Toast.makeText(getActivity(), "Something Wrong :" + statusCode, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                super.onFinish();
            }
        });
        updateSpinner();
        spin_adapter.notifyDataSetChanged();
    }
    public void updateSpinner()
        {
            AsyncHttpClient client1=new AsyncHttpClient();
            RequestParams params1 = new RequestParams();
            params1.put("query", "select * from medications;");
            client1.post(URL,params1, new JsonHttpResponseHandler(){
                @Override
                public void onStart() {
                    super.onStart();
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                    super.onSuccess(statusCode, headers, res);

                    try{
                        medSpinnerOptions.clear();
                        JSONArray jsonArray = res.getJSONArray("data");
                        for(int i= 0; i < jsonArray.length(); i++) {
                            medSpinnerOptions.add(jsonArray.getJSONObject(i).getString("name"));
                        }
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    Toast.makeText(getActivity(), "Something Wrong :"+statusCode, Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFinish() {
                    super.onFinish();
                }
            });
        }

    public void addSchedule() {
        AsyncHttpClient client=new AsyncHttpClient();
        RequestParams params = new RequestParams();
        String med_name = edtDispName.getText().toString();
        String hour = edtHour.getText().toString();
        String min = edtMin.getText().toString();
        String qty = edtDispQty.getText().toString();

        if (TextUtils.isEmpty(med_name) && TextUtils.isEmpty(hour)&& TextUtils.isEmpty(min) && TextUtils.isEmpty(qty)){
            getSchedule();
            return;
        }
        if((Integer.parseInt(min)) < 0 || (Integer.parseInt(min)) > 59 || (Integer.parseInt(hour)) < 0 || (Integer.parseInt(hour) > 23)){
            Toast.makeText(getActivity(), "The Time entered was not valid. Please try again.", Toast.LENGTH_SHORT).show();
            getSchedule();
            return;
        }
        if((Integer.parseInt(qty)) < 1){
            Toast.makeText(getActivity(), "The Quantity to Dispense entered was not valid. Please try again.", Toast.LENGTH_SHORT).show();
            getSchedule();
            return;
        }
        for(int i =0; i < 5; i++)
        {
            getMedID(med_name);
        }

        //Toast.makeText(getActivity(),med_id, Toast.LENGTH_SHORT).show();

        if(med_id == "null")
        {
            getSchedule();
            //Toast.makeText(getActivity(), "The Medication entered was not valid. Please try again." , Toast.LENGTH_SHORT).show();
            //return;
        }

        String request = "INSERT INTO schedule (med_id, hour, minute, num_disp) VALUES ("+med_id+","+ hour+", "+ min +", "+ qty +");";
       // Toast.makeText(getActivity(), request , Toast.LENGTH_SHORT).show();

        params.put("query", request);
        client.post(URL,params, new JsonHttpResponseHandler(){
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                super.onSuccess(statusCode, headers, res);
                //Toast.makeText(getActivity(), "Added to Schedule Successfully", Toast.LENGTH_SHORT).show();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        edtDispName.setText("");
                        edtHour.setText("");
                        edtMin.setText("");
                        edtDispQty.setText("");
                    }
                });
                getSchedule();
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Toast.makeText(getActivity(), "Something Wrong :"+statusCode, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFinish() {
                super.onFinish();
            }
        });
    }

    public void getMedID(String name)
    {
        AsyncHttpClient client1=new AsyncHttpClient();
        RequestParams params1 = new RequestParams();
        String query = "select * from medications where name='" + name +"';";
        //Toast.makeText(getActivity(), name, Toast.LENGTH_SHORT).show();
        params1.put("query", query);
        client1.post(URL,params1, new JsonHttpResponseHandler(){
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                super.onSuccess(statusCode, headers, res);

                try{
                    JSONArray jsonArray = res.getJSONArray("data");
                    for(int i= 0; i < jsonArray.length(); i++) {
                        med_id = jsonArray.getJSONObject(i).getString("id");
                        Toast.makeText(getActivity(), med_id, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Toast.makeText(getActivity(), "Something Wrong :"+statusCode, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFinish() {
                super.onFinish();
            }
        });
    }
}