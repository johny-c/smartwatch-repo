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
    private final BluetoothSocket mmComputerSocket;
    private InputStream mmComputerInStream;
    //private final OutputStream mmOutStream;
    private final Handler mHandler;
    private final Context mContext;
	private boolean isPaused = false;
    private int frameCounter;
	private Thread self;
	
	DataShifter(BluetoothSocket socket, Handler handler, Context context) {
		mmComputerSocket = socket;
        mHandler = handler;
        mContext = context;
	}

	@Override
	public void run() {
		self = Thread.currentThread();
		
        InputStream tmpIn = null;
        //OutputStream tmpOut = null;
 
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = mmComputerSocket.getInputStream();
            //tmpOut = socket.getOutputStream();
        } catch (IOException e) { }
 
        mmComputerInStream = tmpIn;
        //mmOutStream = tmpOut;

        byte[] buffer = new byte[IMG_BUFFER_SIZE];  // buffer store for the stream
        int bytes; // number of bytes read returned from read()
        int byteOffset;
        //int totalBytesRead;

        frameCounter = 1;
        

        while(!Thread.currentThread().isInterrupted()){
        
        
        // Keep listening to the InputStream until an exception occurs
	        while (!isPaused) {
	            try {
	                // Read from the InputStream
	                //bytes = mmComputerInStream.read(buffer);
	            	
	            	byteOffset = 0;            
	                while(byteOffset < IMG_BUFFER_SIZE) {
	                	bytes = mmComputerInStream.read(buffer, byteOffset, IMG_BUFFER_SIZE - byteOffset);
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
	                
	            } catch (IOException e) {
	            	Log.e(TAG, "IOException caught while shifting data");
	            	closeStreams();
	                return;
	            }
	        }
	        	     
	        if(isPaused){
	        	mHandler.obtainMessage(SHIFTER_PAUSED, 0, 0, null).sendToTarget();
	        }
	        
	        while(isPaused){
	        	try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
	            	closeStreams();
	                return;				
				}
	        }
	        mHandler.obtainMessage(SHIFTER_RESUMED, 0, 0, null).sendToTarget();
        }
        
        closeStreams();
	}
	
	public void pause() {
		isPaused = true;
	}
	
	public void resume() {
		isPaused = false;
	}
    
    public void stop(){
    	self.interrupt();
    	closeStreams();
    }
    
    
    private void closeStreams(){
    	
    	if(mmComputerInStream != null){
	    	try {
				mmComputerInStream.close();
			} catch (IOException e1) {
				Log.e(TAG, "IOException caught while closing mmComputerInStream");
			}
    	}
    	
    }





}
