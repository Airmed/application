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
import android.graphics.Typeface;
import android.support.v4.app.Fragment;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

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

@EFragment(R.layout.tab_medication_pane)
public class MedicationPane extends Fragment implements View.OnClickListener{
    String URL="https://seniordesigndb.herokuapp.com/";
    String med_change;
    ArrayAdapter<String> adapter;
    ArrayAdapter<String> spin_adapter;
    ArrayList<MedRecord>meds = new ArrayList<>();
    ArrayList<String>medsString = new ArrayList<>();
    ArrayList<String>medSpinnerOptions = new ArrayList<>();

    @ViewById
    EditText edtQty;
    @ViewById
    EditText edtMedName;
    @ViewById
    ListView med_records_view;
    @ViewById
    Button BtnMedPost;
    @ViewById
    Spinner med_spinner;
    @ViewById
    TableLayout med_records_table;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

    }

    @AfterViews
    public void afterViews() {
        BtnMedPost.setOnClickListener(this);

        adapter = new ArrayAdapter<>(getActivity().getApplicationContext(),android.R.layout.simple_list_item_1, medsString);
        med_records_view.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        spin_adapter = new ArrayAdapter<>(getActivity().getApplicationContext(),android.R.layout.simple_spinner_item,medSpinnerOptions);
        spin_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        med_spinner.setAdapter(spin_adapter);
        med_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                int index = arg0.getSelectedItemPosition();
                edtMedName.setText(medSpinnerOptions.get(index));
                med_change = medSpinnerOptions.get(index);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        getMedications();
        for(int i = 0; i < 60000; i++);
    }

    @Override
    public void onClick(View v) {
        updateMedications();
    }

    public void getMedications() {
        AsyncHttpClient client=new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("query", "select * from medications;");
        client.post(URL,params, new JsonHttpResponseHandler(){
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                super.onSuccess(statusCode, headers, res);

                try{
                    medsString.clear();
                    medSpinnerOptions.clear();
                    med_records_view.invalidateViews();
                    String padedMedTitle = String.format("%-40s", "Medication Name");
                    String padedMedQty = String.format("%-40s", "Quantity");
                    medsString.add(padedMedTitle + " " +padedMedQty);
                    JSONArray jsonArray = res.getJSONArray("data");
                    for(int i= 0; i < jsonArray.length(); i++) {
                        MedRecord m = new MedRecord();
                        m.medName = jsonArray.getJSONObject(i).getString("name");
                        m.medQty = jsonArray.getJSONObject(i).getString("qty");
                        medSpinnerOptions.add(jsonArray.getJSONObject(i).getString("name"));
                        meds.add(m);
                        String padedname = String.format("%-50s", m.medName);
                        String padedQty = String.format("%-50s", m.medQty);
                        String input = padedname + " " + m.medQty;


                        medsString.add(input);
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
                adapter.notifyDataSetChanged();
                spin_adapter.notifyDataSetChanged();

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

    public void updateMedications() {
        AsyncHttpClient client=new AsyncHttpClient();
        RequestParams params = new RequestParams();
        String med_name = edtMedName.getText().toString();
        String med_qty = edtQty.getText().toString();

        if (TextUtils.isEmpty(med_name) && TextUtils.isEmpty(med_qty)){

            getMedications();
            return;
        }

        String request = "UPDATE medications SET name='"+ med_name+"', qty=qty+"+med_qty+" WHERE name='"+ med_change+"';";


        params.put("query", request);
        client.post(URL,params, new JsonHttpResponseHandler(){
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                super.onSuccess(statusCode, headers, res);
                Toast.makeText(getActivity()
                        , "Updated Medications Successfully", Toast.LENGTH_SHORT).show();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        edtMedName.setText("");
                        edtQty.setText("");
                    }
                });
                getMedications();
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