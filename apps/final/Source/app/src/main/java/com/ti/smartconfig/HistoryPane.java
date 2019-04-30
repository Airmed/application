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

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;

@EFragment(R.layout.tab_history_pane)
public class HistoryPane extends Fragment{
    String URL="https://seniordesigndb.herokuapp.com/";
    ArrayAdapter<String> adapter;
    ArrayList<HisRecord>history = new ArrayList<>();
    ArrayList<String>hisString = new ArrayList<>();

    @ViewById
    ListView his_record_view;



    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }


    @AfterViews
    public void afterViews() {
        adapter = new ArrayAdapter<>(getActivity().getApplicationContext(),android.R.layout.simple_list_item_1, hisString);
        his_record_view.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        getHistory();
        for(int i = 0; i < 600000; i++);

    }


    public void getHistory() {
        AsyncHttpClient client=new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("query", "select * from history;");
        client.post(URL,params, new JsonHttpResponseHandler(){
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                super.onSuccess(statusCode, headers, res);

                try{
                    
                    hisString.clear();
                    his_record_view.invalidateViews();
                    String paddedTS = String.format("%-41s", "Time Stamp");
                    String paddedEvent = String.format("%-39s", "Event");
                    hisString.add(paddedTS + " " +paddedEvent);
                    JSONArray jsonArray = res.getJSONArray("data");
                    for(int i= 0; i < jsonArray.length(); i++) {
                        HisRecord h = new HisRecord();
                        h.timeStamp = jsonArray.getJSONObject(i).getString("cur_time");
                        h.errorMessage = jsonArray.getJSONObject(i).getString("error");

                        history.add(h);
                        String pTime = String.format("%-30s", h.timeStamp);
                        String pEvent = String.format("%-50s", h.errorMessage);
                        String input = pTime +  pEvent;


                        hisString.add(input);
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
                adapter.notifyDataSetChanged();

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