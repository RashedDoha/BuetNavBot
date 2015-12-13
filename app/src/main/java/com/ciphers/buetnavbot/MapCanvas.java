package com.ciphers.buetnavbot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.lang.Math;

/**
 * Created by Rashed on 11/12/2015.
 */
public class MapCanvas extends View {

    boolean initialized = true;
    boolean body = false;

    Bitmap myBitmap;


    //Context
    private Context mapContext;
    char command;

    // Map coordinates
    int dx = 0;
    int dy = 0;
    int currentX;
    int currentY;
    int lastX, lastY;
    Path myPath;
    int angle = 0;

    //Bluetooth Refs
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;

    //Thread Refs
    ConnectThread mConnectThread;
    ConnectedThread mConnectedThread;

    //Handler Refs
    Handler mmHandler;

    //Draw variables
    Paint myPaint, circlePaint;

    public MapCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        mapContext = context;
        myPaint = new Paint();
        circlePaint = new Paint();
        myPaint.setStrokeWidth(20);
        myPaint.setPathEffect(null);
        myPaint.setColor(Color.BLUE);
        myPaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(Color.RED);
        circlePaint.setPathEffect(null);
        setOnTouchListener(new TouchPoint());

        myPath = new Path();
        setupBT();

    }




    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int startX = 100;
        int startY = canvas.getHeight() - 50;
        if(initialized)
        {
            myPath.moveTo(startX, startY);
            myPath.lineTo(startX, startY - 20);
            lastX = startX;
            lastY = startY-20;
            currentX = lastX;
            currentY = lastY;
            canvas.drawPath(myPath, myPaint);
            initialized = false;
        }
        else
        {
            myPath.rLineTo(dx, dy);
            canvas.drawPath(myPath, myPaint);
            lastX = currentX;
            lastY = currentY;
            currentX = lastX + dx;
            currentY = lastY + dy;

        }
        if(body)
        {
            myPath.addCircle(currentX, currentY, 30, Path.Direction.CW);
            canvas.drawPath(myPath, circlePaint);
            body = false;
        }

        // canvas.drawLine(startX, startY, startX + dx, startY + dy, myPaint);


    }

    void setupHandler() {
        mmHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                byte[] writebuf = (byte[]) msg.obj;
                int begin = (int) msg.arg1;
                int end = (int) msg.arg2;
                switch (msg.what) {
                    case 1:
                        String writeMessage = new String(writebuf);
                        writeMessage = writeMessage.substring(begin, end);
                        if(writeMessage.charAt(0) != '#') Toast.makeText(mapContext, writeMessage.charAt(0), Toast.LENGTH_LONG).show();
                        else Toast.makeText(mapContext, 'a', Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };
    }

    private void setupBT() {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                bluetoothDevice = device;
            }
        }
        mConnectThread = new ConnectThread(bluetoothDevice);
        mConnectThread.start();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-" +
                "00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {

            }
            mmSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    return;
                }
            }

            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();


        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            Log.d("Maps log", "This works!");
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    final String strReceived = new String(buffer, 0, bytes);
                    final String strByteCnt = String.valueOf(bytes) + " bytes received.\n";

                    ((Activity)mapContext).runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            command = strReceived.charAt(0);

                            if(command >= '0' && command <= '9')
                            {
                                angle = Integer.parseInt(command + "") * 10;
                                Toast.makeText(mapContext, angle + "", Toast.LENGTH_SHORT).show();

                            }
                            switch (command)
                            {
                                case 'n':
                                    dx = 0;
                                    dy = -7;
                                    invalidate();
                                    break;
                                case 'e':
                                    dx = 7;
                                    dy = 0;
                                    invalidate();
                                    break;
                                case 's':
                                    dx = 0;
                                    dy = 7;
                                    invalidate();
                                    break;
                                case 'w':
                                    dx = -7;
                                    dy = 0;
                                    invalidate();
                                    break;
                                case 't':
                                    body = true;
                                    Toast.makeText(mapContext, "Human Body Detected!", Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    Math.tan(angle);
                                    break;
                            }
                        }
                    });

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    ((Activity)mapContext).runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            command = '#';
                        }
                    });
                }
            }
//                try {
//                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
//                    for (int i = begin; i < bytes; i++) {
//                        if (buffer[i] == "#".getBytes()[0]) {
//                            mmHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
//                            begin = i + 1;
//                            if (i == bytes - 1) {
//                                bytes = 0;
//                                begin = 0;
//                            }
//                        }
//                    }
//                } catch (IOException e) {
//                    break;
//                }
            }


        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }

    }

    private class TouchPoint implements OnTouchListener
    {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Toast.makeText(mapContext, "Human Body Detected!", Toast.LENGTH_SHORT).show();
            body = true;
            return false;
        }
    }

}
