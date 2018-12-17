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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements WiFiDirectServicesList.DeviceClickListener, Handler.Callback, WifiP2pManager.ConnectionInfoListener {

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

    private Handler handler = new Handler(this);
    private WiFiDirectServicesList servicesList;

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
    private MessageHandler messageHandler;

    /*
    Chat Fragment Stuff End
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        networkingSetup();
        this.start=false;
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
                Toast.makeText(this, "I have received a message", Toast.LENGTH_SHORT).show();
                if(this.start==false) {
                    this.start=true;
                    messageHandler.write("test message".getBytes());
                    Intent intent = new Intent(MainActivity.this, QuizMainActivity.class);
                    startActivity(intent);
                }
                break;

            case MY_HANDLE:
                Object obj = msg.obj;
                setMessageHandler((MessageHandler) obj);

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
        System.out.println("HI I DID IT");
        /**
         * Connection started now do thing, replaced
         *         chatFragment = new WiFiChatFragment();
         *         getFragmentManager().beginTransaction()
         *                 .replace(R.id.container_root, chatFragment).commit();
         *         statusTxtView.setVisibility(View.GONE);
         */
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
}
