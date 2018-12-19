package com.group.a4p61.quizvenge;

import android.app.Fragment;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class QuizMainActivity extends Fragment implements ListView.OnItemClickListener {

    public List<ContactTup> yourContactsList;
    public List<ContactTup> otherContactList;
    private Queue<String> testMessage;
    String selection = ContactsContract.Data.MIMETYPE + " in (?, ?)";


    private View view;
    private MessageHandler messageHandler;

    public ContactTup selectedContact;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = inflater.inflate(R.layout.activity_main, container, false);

        yourContactsList = new LinkedList<>();
        otherContactList = new LinkedList<>();


            // Permission has already been granted
            String[] projection = {
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };
            String selection = ContactsContract.Data.MIMETYPE + " in (?, ?)";
            String[] selectionArgs = {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
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
            while (!cursor.isLast()) {
                try {
                    count++;
                    ContactTup tup = new ContactTup();
                    tup.id = cursor.getString(cursor.getColumnIndex("CONTACT_ID"));
                    tup.name = cursor.getString(cursor.getColumnIndex("DISPLAY_NAME"));
                    tup.number = cursor.getString(2);
                    yourContactsList.add(tup);
                    if (count<10) {
                        JSONObject jObj = new JSONObject();
                        jObj.put("ID", tup.id);
                        jObj.put("NAME", tup.name);
                        jsonArray.put(jObj);
                    }
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
                JSONObject jObj = hostArray.getJSONObject(i);
                arrayAdapter.add(jObj.getString("NAME"));
                ContactTup cTup = new ContactTup();
                cTup.id=jObj.getString("ID");
                cTup.name=jObj.getString("NAME");
                otherContactList.add(cTup);
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
        selectedContact= otherContactList.get(position);
    }


    public interface MessageTarget {
        public Handler getHandler();
    }
    public void setMessageHandler(MessageHandler obj) {
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