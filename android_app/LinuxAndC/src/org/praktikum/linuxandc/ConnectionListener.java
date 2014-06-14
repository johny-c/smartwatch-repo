package org.praktikum.linuxandc;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class ConnectionListener extends AsyncTask<Void, Void, Void>
		implements Constants {
	
	private static final String TAG = "ConnectionListener";
	private final BluetoothServerSocket mmServerSocket;

	private final BluetoothAdapter mBluetoothAdapter;
	// you can use one of the many random UUID generators on the web, then
	// initialize a UUID with fromString(String).
	private static final UUID SECURE_UUID = UUID
			.fromString("d2e19fab-f367-43d9-9cc2-b6837e7eb915");
	
	private DataShifter dataShifter;
    private final Handler mHandler;
    private final Context mContext;
    private static int frameCounter;
    private InputStream mmInStream;
    private BluetoothSocket socket;

	public ConnectionListener(Handler handler, Context context) {
        mHandler = handler;
        mContext = context;

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothServerSocket tmp = null;
		try {
			tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
					"Smartphone BT Server", SECURE_UUID);

		} catch (IOException e) { }
		mmServerSocket = tmp;
	}

	@Override
	protected Void doInBackground(Void... params) {
		
		socket = null;
		// Keep listening until exception occurs or a socket is returned
		while (true) {
			try {
				socket = mmServerSocket.accept();
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				cancel();
				break;
			}
			
			break;
		}
		
		
		if(socket != null){
			//dataShifter = new DataShifter(socket, mHandler, mContext);
			//new Thread(dataShifter).start();
			
	        mHandler.obtainMessage(DEVICE_UPDATE, 0 , 0, socket.getRemoteDevice().getName())
	        .sendToTarget();

	        byte[] buffer = new byte[IMG_BUFFER_SIZE + 1];  // buffer store for the stream
	        int bytes; // number of bytes read returned from read()

	        frameCounter = 1;
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                
	                if(bytes == IMG_BUFFER_SIZE) {
	                                  
		                Intent intent = new Intent(IMAGE_BYTES);
		                intent.putExtra(BYTE_ARRAY_KEY, buffer);
		                mContext.sendBroadcast(intent);
		                
		                // Send the obtained bytes to the UI MainActivity
		                // obtainMessage(int what, int arg1, int arg2, Object obj)
		                mHandler.obtainMessage(IMAGE_UPDATE, bytes, frameCounter, "")
		                        .sendToTarget();
		                
		                frameCounter++;
	                }
	     
	                
	                if(isCancelled() ){
	                	cancel();
	                	break;
	                }
	                
	            } catch (IOException e) {
	            	Log.d(TAG, "IOException caught, terminating");
	            	cancel();
	                break;
	            }
	        }	
		}
		
		cancel();
		
		return null;
	}
	
	
	
	/** Will cancel the listening socket, and cause the thread to finish */
	private void cancel() {
		try {
			if(mmServerSocket != null)
				mmServerSocket.close();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		
		if(socket != null){
			try {
				socket.close();
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
		}
        mHandler.obtainMessage(SERVER_STOPPED, 0 , 0, "")
        .sendToTarget();
	}

}
