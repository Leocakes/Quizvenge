package com.group.a4p61.quizvenge;

import android.Manifest;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class QuizMainActivity extends AppCompatActivity {

    public List<ContactTup> contactsList;
    private Queue<String> testMessage;
    String selection = ContactsContract.Data.MIMETYPE + " in (?, ?)";

    private final static int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 12345;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        testMessage = new LinkedList<>();
        contactsList = new LinkedList<>();

        if (ContextCompat.checkSelfPermission(QuizMainActivity.this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(QuizMainActivity.this,
                    Manifest.permission.READ_CONTACTS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(QuizMainActivity.this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            String[] projection = {
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };
            String selection = ContactsContract.Data.MIMETYPE + " in (?, ?)";
            String[] selectionArgs = {
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            };
            String sortOrder = ContactsContract.Contacts.SORT_KEY_ALTERNATIVE;

            Uri uri = ContactsContract.CommonDataKinds.Contactables.CONTENT_URI;
// we could also use Uri uri = ContactsContract.Data.CONTENT_URI;

// ok, let's work...
            Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
            cursor.moveToFirst();
            //Send contact list
            JSONArray jsonArray = new JSONArray();
            while (!cursor.isLast()) {
                try {
                    ContactTup tup = new ContactTup();
                    tup.id = cursor.getString(cursor.getColumnIndex("CONTACT_ID"));
                    tup.name = cursor.getString(cursor.getColumnIndex("DISPLAY_NAME"));
                    tup.number = cursor.getString(2);
                    contactsList.add(tup);
                    JSONObject jObj = new JSONObject();
                    jObj.put("ID", tup.id);
                    jObj.put("NAME", tup.name);
                    jsonArray.put(jObj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                cursor.moveToNext();
            }
            testMessage.add(jsonArray.toString());
            //Get Contact List
            JSONArray hostArray = null;

            try {
                hostArray = new JSONArray(testMessage.remove());
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter(QuizMainActivity.this, android.R.layout.simple_list_item_1);
                for (int i = 0; i < hostArray.length(); i++) {
                    arrayAdapter.add(hostArray.getJSONObject(i).getString("NAME"));
                }
                ListView listView = findViewById(R.id.listView);
                listView.setAdapter(arrayAdapter);
            } catch (Exception e) {
                //I don't really know
            }

        }

    }

    public void startQuiz(View v) {
        Intent intent = new Intent(QuizMainActivity.this, Quiz.class);
        startActivity(intent);
    }
}