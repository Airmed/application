package com.example.isaia.databaseconnection;



import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.loopj.android.http.*;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.text.TextUtils;


import java.util.ArrayList;


import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    String URL="https://seniordesigndb.herokuapp.com/";
    JSONObject object=new JSONObject();
    ListView listView;
    ArrayAdapter<String> adapter;
    ArrayList<UserRecord>users = new ArrayList<>();
    ArrayList<String>usersString = new ArrayList<>();
    private EditText edtFirst, edtLast, edtAge, edtGender;
    private Button btnPost;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edtFirst = findViewById(R.id.edtFirst);
        edtLast = findViewById(R.id.edtLast);
        edtAge = findViewById(R.id.edtAge);
        edtGender = findViewById(R.id.edtGender);

        listView = findViewById(R.id.records_view);

        btnPost = findViewById(R.id.BtnPost);
        btnPost.setOnClickListener(this);
        getUsers();
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, usersString);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
    @Override
    public void onClick(View v) {
        addUser();
    }

    public void getUsers() {
        AsyncHttpClient client=new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("query", "select * from users;");
        client.post(URL,params, new JsonHttpResponseHandler(){
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                super.onSuccess(statusCode, headers, res);
                //Toast.makeText(MainActivity.this, "Retrieving Users from DB", Toast.LENGTH_SHORT).show();
                try{
                    usersString.clear();
                    listView.invalidateViews();
                    JSONArray jsonArray = res.getJSONArray("data");
                    for(int i= 0; i < jsonArray.length(); i++) {
                        UserRecord u = new UserRecord();
                        u.lastName = jsonArray.getJSONObject(i).getString("lastname");
                        u.firstName = jsonArray.getJSONObject(i).getString("firstname");
                        u.userAge = jsonArray.getJSONObject(i).getString("age");
                        u.gender = jsonArray.getJSONObject(i).getString("gender");
                        users.add(u);
                        String input = u.lastName + "   " + u.firstName +"   "+ u.userAge + "   " + u.gender;


                        usersString.add(input);
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Toast.makeText(MainActivity.this, "Something Wrong :"+statusCode, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFinish() {
                super.onFinish();
            }
        });
    }

    public void addUser() {
        AsyncHttpClient client=new AsyncHttpClient();
        RequestParams params = new RequestParams();
        String last = edtLast.getText().toString();
        String first = edtFirst.getText().toString();
        String age = edtAge.getText().toString();
        String gender = edtGender.getText().toString();

        if (TextUtils.isEmpty(last) && TextUtils.isEmpty(first) &&
                TextUtils.isEmpty(age)&&
                TextUtils.isEmpty(gender)) {

                getUsers();
            return;
        }
        String request = "insert into users (LastName, FirstName, Age, Gender) values ('"+last+"', '"+ first+"',"+age+", '"+gender+"');";
        params.put("query", request);
        client.post(URL,params, new JsonHttpResponseHandler(){
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                super.onSuccess(statusCode, headers, res);
                Toast.makeText(MainActivity.this
                        , "Posted to DB Successfully!", Toast.LENGTH_SHORT).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        edtFirst.setText("");
                        edtLast.setText("");
                        edtAge.setText("");
                        edtGender.setText("");
                    }
                });
                getUsers();
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Toast.makeText(MainActivity.this, "Something Wrong :"+statusCode, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFinish() {
                super.onFinish();
            }
        });
    }
}

