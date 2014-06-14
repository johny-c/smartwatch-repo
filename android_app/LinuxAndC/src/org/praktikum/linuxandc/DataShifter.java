package org.praktikum.linuxandc;

import java.io.IOException;
import java.io.InputStream;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class DataShifter implements Runnable, Constants {
	
	private static final String TAG = "DataShifter";
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    //private final OutputStream mmOutStream;
    private final Handler mHandler;
    private final Context mContext;
	private boolean isCancelled = false;
    private int frameCounter;
	
	
	DataShifter(BluetoothSocket socket, Handler handler, Context context) {
		mmSocket = socket;
        mHandler = handler;
        mContext = context;
        
        InputStream tmpIn = null;
        //OutputStream tmpOut = null;
 
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            //tmpOut = socket.getOutputStream();
        } catch (IOException e) { }
 
        mmInStream = tmpIn;
        //mmOutStream = tmpOut;
	}

	@Override
	public void run() {

        byte[] buffer = new byte[IMG_BUFFER_SIZE];  // buffer store for the stream
        int bytes; // number of bytes read returned from read()
        int byteOffset;
        int totalBytesRead;

        frameCounter = 1;
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                //bytes = mmInStream.read(buffer);
            	
            	byteOffset = 0;            
                while(byteOffset < IMG_BUFFER_SIZE) {
                	bytes = mmInStream.read(buffer, byteOffset, IMG_BUFFER_SIZE - byteOffset);
                	if(bytes == -1){
                		break;
                	}
                	else {
                		byteOffset += bytes;
                	}
                }
                
                
                if(byteOffset == IMG_BUFFER_SIZE) {
                                  
                	// Broadcast the buffer
	                Intent intent = new Intent(IMAGE_BYTES);
	                intent.putExtra(BYTE_ARRAY_KEY, buffer);
	                mContext.sendBroadcast(intent);
	                
	                // Send the number of obtained bytes to the UI MainActivity
	                // obtainMessage(int what, int arg1, int arg2, Object obj)
	                mHandler.obtainMessage(IMAGE_UPDATE, byteOffset, frameCounter, null).sendToTarget();
	            
	                frameCounter++;
                }
                else {
                    // Send the number of obtained bytes to the UI MainActivity
                    // obtainMessage(int what, int arg1, int arg2, Object obj)
                    mHandler.obtainMessage(DATA_UPDATE, byteOffset, frameCounter, buffer).sendToTarget();
                }
     
                
                if(isCancelled ){
                	closeStreams();
                	break;
                }
                
            } catch (IOException e) {
            	Log.e(TAG, "IOException caught while shifting data");
            	closeStreams();
                break;
            }
        }	
	}
	
	
    public void cancel() {
    	isCancelled = true;
    }
    
    
    private void closeStreams(){
    	
    	if(mmInStream != null){
	    	try {
				mmInStream.close();
			} catch (IOException e1) {
				Log.e(TAG, "IOException caught while closing mmInStream");
			}
    	}
    	
    	if(mmSocket != null){
    		try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "IOException caught while closing mmSocket");
			}
    	}
    }

}
