package com.example.attendance;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;

class BluetoothUtil {

    public BluetoothSocket mSocket;
    Context context = ApplicationClass.getAppContext();
    public ConnectedThread connectedThread;
    public ArrayList<ConnectedThread> mConnThreads = new ArrayList<ConnectedThread>();
    public ArrayList<ConnectedThread1> mConnThreads1 = new ArrayList<ConnectedThread1>();
    public static final int MESSAGE_READ = 0;
    public static final String MESSAGE_READ_TAG = "message_read";

    public ArrayList<BluetoothSocket> mSockets;
    public ArrayList<String> rollNos = new ArrayList<String>();

    public BluetoothUtil(){
        mSockets = new ArrayList<BluetoothSocket>();
    }

    public void writeMessage(String msg){
        //Toast.makeText(context, "writeMessage", Toast.LENGTH_LONG).show();
        connectedThread.write(msg.getBytes());
    }

    public void setmSocket(BluetoothSocket mSocket) {
        this.mSocket = mSocket;
        connectedThread = new ConnectedThread(mSocket);
    }

    public void AddSocket(BluetoothSocket socket){
        mSockets.add(socket);
    }

    public synchronized void connected() {

        Toast.makeText(context, String.valueOf(mSockets.size()), Toast.LENGTH_LONG).show();

        for (int i = 0; i < mSockets.size(); i++) {
            BluetoothSocket socket1 = mSockets.get(i);
            ConnectedThread mConnectedThread = new ConnectedThread(socket1);
            mConnectedThread.start();
            mConnThreads.add(mConnectedThread);
        }
    }

    public synchronized void connected1() {

        Toast.makeText(context, String.valueOf(mSockets.size()), Toast.LENGTH_LONG).show();

        for (int i = 0; i < mSockets.size(); i++) {
            BluetoothSocket socket1 = mSockets.get(i);
            ConnectedThread1 mConnectedThread1 = new ConnectedThread1(socket1);
            mConnectedThread1.start();
            mConnThreads1.add(mConnectedThread1);
        }
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    // save the connected device's name
                    String message = msg.getData().getString(MESSAGE_READ_TAG);
                    if(message.equals("")){
                       Toast.makeText(context, "no message", Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    };

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    Message readMsg = mHandler.obtainMessage(MESSAGE_READ);
                    Bundle bundle = new Bundle();
                    String str = new String(mmBuffer, 0, numBytes);
                    rollNos.add(str);
                    bundle.putString(MESSAGE_READ_TAG,str);
                    readMsg.setData(bundle);
                    mHandler.sendMessage(readMsg);
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }
        public void write(byte[] buffer) {
            try {

                mmOutStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectedThread1 extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread1(BluetoothSocket socket) {
            //Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                //Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            //Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            String s;
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    Message readMsg = mHandler.obtainMessage(MESSAGE_READ);
                    Bundle bundle = new Bundle();
                    String str = new String(buffer, 0, bytes);
                    bundle.putString(MESSAGE_READ_TAG,str);
                    readMsg.setData(bundle);
                    mHandler.sendMessage(readMsg);

                    connectedThread.write(str.getBytes());
                    // Send the obtained bytes to the UI Activity
                } catch (IOException e) {
                    e.printStackTrace();
                    //Log.e(TAG, "disconnected", e);
                    //connectionLost();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                //Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
