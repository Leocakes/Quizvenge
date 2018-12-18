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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class QuizMainActivity extends Fragment implements ListView.OnItemClickListener {

    public List<ContactTup> contactsList;
    private Queue<String> testMessage;
    String selection = ContactsContract.Data.MIMETYPE + " in (?, ?)";

    private View view;
    private MessageHandler messageHandler;

    public ContactTup selectedContact;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = inflater.inflate(R.layout.activity_main, container, false);

        contactsList = new LinkedList<>();


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
            Cursor cursor = getActivity().getApplication().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
            cursor.moveToFirst();
            //Send contact list
            JSONArray jsonArray = new JSONArray();
            int count = 0;
            while (!cursor.isLast()&&count<10) {
                try {
                    count++;
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
            sendContacts(jsonArray.toString());
        return view;
    }

    public void fillContacts(String contacts){
        JSONArray hostArray = null;
        try {
            hostArray = new JSONArray(contacts);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.select_dialog_singlechoice);
            for (int i = 0; i < hostArray.length(); i++) {
                arrayAdapter.add(hostArray.getJSONObject(i).getString("NAME"));
            }
            ListView listView = view.findViewById(R.id.listView);
            listView.setAdapter(arrayAdapter);
            listView.setOnItemClickListener(this);
        } catch (Exception e) {
            //I don't really know
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectedContact=contactsList.get(position);
    }


    public interface MessageTarget {
        public Handler getHandler();
    }
    public void setMessgaeHandler(MessageHandler obj) {
        messageHandler = obj;
    }


    public void sendContacts(final String contactString) {
        final String contacts = contactString;
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    if (messageHandler != null) {
                            System.out.println("SENT MESSAGE");
                            System.out.println("SENT:"+contacts);
                            messageHandler.write(contacts.getBytes());
                        break;
                    } else {
                        System.out.println("Did not work");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        }).start();
    }
}