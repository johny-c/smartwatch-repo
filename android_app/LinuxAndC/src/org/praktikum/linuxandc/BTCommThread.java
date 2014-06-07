package org.praktikum.linuxandc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;



public class BTCommThread extends Thread implements Constants {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final Handler mHandler;
    private final Context mContext;
    
    
    //private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;
    private static final int IMG_BUFFER_SIZE = 128*128*3;
    private static int frameCounter;

 
    public BTCommThread(BluetoothSocket socket, Handler handler, Context context) {
        mmSocket = socket;
        mHandler = handler;
        mContext = context;
        
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
 
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }
 
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }
    
    
 
    public void run() {
        byte[] buffer = new byte[IMG_BUFFER_SIZE];  // buffer store for the stream
        int bytes; // number of bytes read returned from read()
 
        frameCounter = 1;
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                                  
                Intent intent = new Intent(IMAGE_BYTES);
                intent.putExtra(BYTE_ARRAY_KEY, buffer);
                mContext.sendBroadcast(intent);
                
                // mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer)
                // .sendToTarget();
                
                // Send the obtained bytes to the UI MainActivity
                // obtainMessage(int what, int arg1, int arg2, Object obj)
                mHandler.obtainMessage(IMAGE_UPDATE, bytes, frameCounter, "")
                        .sendToTarget();
                
                frameCounter++;
                
            } catch (IOException e) {
                break;
            }
        }
    }
    
    
 
    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }
 
    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}