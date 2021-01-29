package com.example.attendance;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    Button listen, create, sendButton, showList;
    EditText rollNo;
    private static final String NAME = "BluetoothChatMulti";
    private static final String TAG = "BluetoothChatService";
    int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothAdapter mAdapter;
    private ArrayList<UUID> mUuids;
    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final String DEVICE_NAME = "device_name";
    private String mConnectedDeviceName = null;
    public BluetoothDevice[] btArray;
    public ArrayList<String> rollNos;
    ListView listView;
    private ConnectThread mConnectThread;
    private AcceptThread mAcceptThread;
    public BluetoothUtil bluetoothUtil;
    Boolean flag = false;
    Integer check = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listen = (Button) findViewById(R.id.listen);
        create = (Button) findViewById(R.id.create);
        listView = (ListView) findViewById(R.id.listView);
        rollNo = (EditText) findViewById(R.id.rollNo);
        sendButton = (Button) findViewById(R.id.sendButton);
        showList = (Button) findViewById(R.id.showList);
        sendButton.setVisibility(View.GONE);
        rollNo.setVisibility(View.GONE);
        rollNos = new ArrayList<String>();

        bluetoothUtil = new BluetoothUtil();

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mUuids = new ArrayList<UUID>();
        mUuids.add(UUID.fromString("b7746a40-c758-4868-aa19-7ac6b3475dfc"));
        mUuids.add(UUID.fromString("2d64189d-5a2c-4511-a074-77f199fd0834"));
        mUuids.add(UUID.fromString("e442e09a-51f3-4a7b-91cb-f638491d1412"));
        mUuids.add(UUID.fromString("a81d6504-4536-49ee-a475-7d96d09439e4"));
        mUuids.add(UUID.fromString("aa91eab1-d8ad-448e-abdb-95ebba4a9b55"));
        mUuids.add(UUID.fromString("4d34da73-d0a4-4f40-ac38-917e0a9dee97"));
        mUuids.add(UUID.fromString("5e14d4df-9c8a-4db7-81e4-c937564c86e0"));

        if (!mAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        }


        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag.equals(false)) {
                    listen.setText("End Listening");
                    AcceptThread acceptThread = new AcceptThread();
                    acceptThread.start();
                    flag = true;
                } else {
                    bluetoothUtil.connected();
                }
            }
        });

        showList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rollNos = bluetoothUtil.rollNos;
                for(String string : rollNos){
                    Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
                }
                String[] strings = new String[rollNos.size()];
                for(int i = 0 ; i < rollNos.size(); i++){
                    strings[i] = rollNos.get(i);
                }
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
                listView.setAdapter(arrayAdapter);
                listView.setVisibility(View.VISIBLE);
            }
        });

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (check.equals(0)) {
                    Set<BluetoothDevice> bt = mAdapter.getBondedDevices();
                    String[] strings = new String[bt.size()];
                    btArray = new BluetoothDevice[bt.size()];
                    int index = 0;

                    if (bt.size() > 0) {
                        for (BluetoothDevice device : bt) {
                            btArray[index] = device;
                            strings[index] = device.getName();
                            index++;
                        }
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
                        listView.setAdapter(arrayAdapter);
                    }
                }
                else if(check.equals(1)){
                    check = 2;
                    mAcceptThread = new AcceptThread();
                    mAcceptThread.start();
                    create.setText("End Listening");
                }
                else{
                    //mAcceptThread.cancel();
                    create.setVisibility(View.GONE);
                    bluetoothUtil.connected1();
                }

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                listView.setVisibility(View.GONE);
                connect(btArray[i]);
                listen.setVisibility(View.GONE);
                sendButton.setVisibility(View.VISIBLE);
                rollNo.setVisibility(View.VISIBLE);
                create.setText("Start listening");
                check = 1;
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String roll = (String) rollNo.getText().toString();
                bluetoothUtil.writeMessage(roll);
            }
        });

    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    private class AcceptThread extends Thread {
        BluetoothServerSocket serverSocket = null;

        public AcceptThread() {
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket = null;
            try {
                // Listen for all 7 UUIDs
                for (int i = 0; i < mUuids.size(); i++) {
                    serverSocket = mAdapter.listenUsingRfcommWithServiceRecord(NAME, mUuids.get(i));
                    socket = serverSocket.accept();
                    if (socket != null) {
                        connected(socket, socket.getRemoteDevice(), i);
                        serverSocket.close();
                    }
                }
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private UUID tempUuid;
        int i, x;

        public ConnectThread(BluetoothDevice device, UUID uuidToTry, int a) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            tempUuid = uuidToTry;
            i = 0;
            x = a;
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuidToTry);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                if (tempUuid.toString().contentEquals(mUuids.get(6).toString())) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Cant connect to device", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), String.valueOf(x), Toast.LENGTH_SHORT).show();
                }
            });
            // Start the connected thread
            connected(mmSocket, mmDevice);
            // Reset the ConnectThread because we're done
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        bluetoothUtil.setmSocket(socket);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, int i) {
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName() + "  " + String.valueOf(i));
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        bluetoothUtil.AddSocket(socket);
    }

    public synchronized void connect(BluetoothDevice device) {

        // Create a new thread and attempt to connect to each UUID one-by-one.
        for (int i = 0; i < mUuids.size(); i++) {
            try {
                mConnectThread = new ConnectThread(device, mUuids.get(i), i);
                mConnectThread.start();
            } catch (Exception e) {
            }
        }
    }
}