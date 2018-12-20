package com.group.a4p61.quizvenge;

import android.Manifest;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements WiFiDirectServicesList.DeviceClickListener, Handler.Callback, WifiP2pManager.ConnectionInfoListener, Runnable {

    public static final String TAG = "quizvenge";

    /*
    Networking Stuff
     */
    static final int SERVER_PORT = 4545;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;
    private WifiP2pDnsSdServiceRequest serviceRequest;

    private WifiP2pManager manager;

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_quizvenge";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;

    private final static int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 12345;

    private static final int SEND_SMS=6565;

    private QuizMainActivity quizMainFragment;
    private Quiz quizFragment;

    private Handler handler = new Handler(this);
    private WiFiDirectServicesList servicesList;

    //When you need to send the message
    private String message;
    private ContactTup contactToMsg;

    Queue<String> contacts = new LinkedList<>();
    Boolean receivingContacts = false;

    private Integer theirScore;
    private Integer yourScore;

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }
    /*
    Networking Stuff End
     */
    private Boolean start;
    /*
    Chat Fragment Stuff
     */
    public static MessageHandler messageHandler;

    /*
    Chat Fragment Stuff End
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        networkingSetup();

        requestPermissions(new String[]{Manifest.permission.SEND_SMS,Manifest.permission.READ_CONTACTS},SEND_SMS);

        if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this,"Needs sms permission",Toast.LENGTH_SHORT).show();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

    }

    /**
     * This is networking methods below
     */
    @Override
    protected void onRestart() {
        Fragment frag = getFragmentManager().findFragmentByTag("services");
        if (frag != null) {
            getFragmentManager().beginTransaction().remove(frag).commit();
        }
        super.onRestart();
    }

    @Override
    protected void onStop() {
        if (manager != null && channel != null) {
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });
        }
        super.onStop();
    }

    public void gameEnd() {
        if(theirScore>yourScore) {
            Toast.makeText(this,"You lose",Toast.LENGTH_SHORT).show();
            SmsManager manager = SmsManager.getDefault();
            manager.sendTextMessage(contactToMsg.number, null, message, null, null);
            final LoosingEnd le = new LoosingEnd();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container_root, le).commit();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    le.testtext("Text \"" + message + "\" has been send to " + contactToMsg.name + " through your phone.");
                }
            },1000);
            //textView.setText("Text \"" + message + "\" has been send to " + contactToMsg.name + ".");
        } else {
            Toast.makeText(this,"You win",Toast.LENGTH_SHORT).show();
            final WinningEnd we = new WinningEnd();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container_root, we).commit();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    we.testtext("Your message has been sent through your opponents phone");
                }
            },1000);
            //TextView v =findViewById(R.id.winning_text);
            //v.setText("Text \"" + message + "\" has been send to " + contactToMsg.name + " through your opponents phone.");
        }
    }

    public void networkingSetup(){
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        startRegistrationAndDiscovery();

        servicesList = new WiFiDirectServicesList();
        getFragmentManager().beginTransaction()
                .add(R.id.container_root, servicesList, "services").commit();
    }

    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                appendStatus("Failed to add a service");
            }
        });

        discoverService();

    }

    public void appendStatus(String status) {
        System.out.println(status);
    }

    private void discoverService() {
        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
        manager.setDnsSdResponseListeners(channel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?

                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                            // update the UI and add the item the discovered
                            // device.
                            WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                                    .findFragmentByTag("services");
                            if (fragment != null) {
                                WiFiDirectServicesList.WiFiDevicesAdapter adapter = ((WiFiDirectServicesList.WiFiDevicesAdapter) fragment
                                        .getListAdapter());
                                WiFiP2pService service = new WiFiP2pService();
                                service.device = srcDevice;
                                service.instanceName = instanceName;
                                service.serviceRegistrationType = registrationType;
                                adapter.add(service);
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "onBonjourServiceAvailable "
                                        + instanceName);
                            }
                        }

                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,
                                device.deviceName + " is "
                                        + record.get(TXTRECORD_PROP_AVAILABLE));
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, serviceRequest,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        appendStatus("Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        appendStatus("Failed adding service discovery request");
                    }
                });
        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                appendStatus("Service discovery failed");

            }
        });
    }



    @Override
    public void connectP2p(WiFiP2pService service) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if (serviceRequest != null)
            manager.removeServiceRequest(channel, serviceRequest,
                    new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(int arg0) {
                        }
                    });

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Connecting to service");
            }

            @Override
            public void onFailure(int errorCode) {
                appendStatus("Failed connecting to service");
            }
        });
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, readMessage);
                if(readMessage.contains("ID")&&readMessage.contains("NAME")){
                    (quizMainFragment).fillContacts(readMessage);
                }

                if(readMessage.contains("SCORE")) {
                    this.theirScore=Integer.parseInt(readMessage.substring(6));
                    if(yourScore!=null ) {
                        gameEnd();
                    }
                }

                if(readMessage.contains("MESSAGE")) {
                    try {
                        JSONObject jObj = new JSONObject(readMessage);
                        this.message = jObj.getString("MESSAGE");
                        String id = jObj.getString("CONTACT_ID");
                        for(ContactTup contactTup:quizMainFragment.yourContactsList) {
                            if (contactTup.id.equals(id)) {
                                this.contactToMsg = contactTup;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                break;

            case MY_HANDLE:
                Object obj = msg.obj;
                setMessageHandler((MessageHandler) obj);
                (quizMainFragment).setMessageHandler((MessageHandler) obj);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */

        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                handler = new GroupOwnerSocketHandler(
                        getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            handler = new ClientSocketHandler(
                    getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();
        }

                        quizMainFragment = new QuizMainActivity();
                        getFragmentManager().beginTransaction()
                                .replace(R.id.container_root, quizMainFragment).commit();


        /**
         * Connection started now do thing, replaced
         *         chatFragment = new WiFiChatFragment();
         *         getFragmentManager().beginTransaction()
         *                 .replace(R.id.container_root, chatFragment).commit();
         *         statusTxtView.setVisibility(View.GONE);
         */
    }
    //For when the quiz is over
    @Override
    public void run() {
        this.yourScore=quizFragment.score;
        if(this.theirScore!=null) {
            gameEnd();
        }
    }

    /**
     * MESSAGE FRAGMENT TIME
     */

    public interface MessageTarget {
        public Handler getHandler();
    }

    public void setMessageHandler(MessageHandler obj) {
        messageHandler = obj;
    }

    public void pushMessage(String readMessage) {
//        adapter.add(readMessage);
//        adapter.notifyDataSetChanged();
    }

    public void buttonTest(View view){
        if (messageHandler!=null) {
            messageHandler.write("test message".getBytes());
        }
    }

    public void startQuiz(View v) {
        EditText msgBox = findViewById(R.id.messageBox);
        if (quizMainFragment.selectedContact==null) {
            Toast.makeText(this,"Select a contact",Toast.LENGTH_SHORT).show();
        } else if(msgBox.getText().length()==0) {
           Toast.makeText(this,"Enter message",Toast.LENGTH_SHORT).show();
        } else {
            JSONObject jObj = new JSONObject();
            try {
                jObj.put("CONTACT_ID", quizMainFragment.selectedContact.id);
                jObj.put("MESSAGE",msgBox.getText());
                messageHandler.write(jObj.toString().getBytes() );
                quizFragment = new Quiz();
                quizFragment.seconds=60;
                new Handler().postDelayed(this,quizFragment.seconds*1000+500);
                getFragmentManager().beginTransaction()
                        .replace(R.id.container_root, quizFragment).commit();
                (quizFragment).setMessageHandler(messageHandler);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
