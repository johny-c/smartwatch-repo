package org.praktikum.linuxandc;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class BluetoothServer implements Callable<DataShifter>, Constants {
	
	private static final String TAG = "BluetoothServer";
	private final BluetoothServerSocket mmServerSocket;
	private final BluetoothAdapter mBluetoothAdapter;
	private DataShifter dataShifter;
    private final Handler mHandler;
    private final Context mContext;
    private BluetoothSocket socket;
	// you can use one of the many random UUID generators on the web, then
	// initialize a UUID with fromString(String).
	private static final UUID SECURE_UUID = UUID
			.fromString("d2e19fab-f367-43d9-9cc2-b6837e7eb915");

	public BluetoothServer(Handler handler, Context context) {
        mHandler = handler;
        mContext = context;

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothServerSocket tmp = null;
		try {
			tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
					"Smartphone BT Server", SECURE_UUID);

		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		mmServerSocket = tmp;
	}
	
	@Override
	public DataShifter call() throws Exception {
		
		dataShifter = null;
		
		try {
			socket = mmServerSocket.accept();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			closeStreams();
			return null;
		}
		
		try {
			mmServerSocket.close();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}

		
		if(socket != null){			
	        mHandler.obtainMessage(DEVICE_UPDATE, 0 , 0, socket.getRemoteDevice().getName()).sendToTarget();
			dataShifter = new DataShifter(socket, mHandler, mContext);
			new Thread(dataShifter).start();   
		}
		
		return dataShifter;
	}
	
	
	
	
	
	/** Will cancel the listening socket, and cause the thread to finish */
	private void closeStreams() {
		
		if(dataShifter != null){
			dataShifter.stop();
		}
		
    	if(socket != null){
    		try {
    			socket.close();
			} catch (IOException e) {
				Log.e(TAG, "IOException caught while closing mmComputerSocket");
			}
    	}
    	  	
		if(mmServerSocket != null){
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}			
	}







}
