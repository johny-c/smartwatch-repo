package org.praktikum.linuxandc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class MockServer extends AsyncTask<Void, Void, Void> implements
		Constants {
	
	private InputStream imgInputStream;
	private final Context mContext;
	private static final String TAG = "MockServer";
	
	MockServer(Context ctx) {
		mContext = ctx;
	}
	
	

	@Override
	protected Void doInBackground(Void... params) {
		byte[] buffer = null;  // buffer store for the stream

		
		int frame = 1;
		//int counter = 1;
		Intent intent = null;
		
		while(true){
			imgInputStream = mContext.getResources().openRawResource(
					mContext.getResources().getIdentifier("raw/img" + frame,
	   	    	            "raw", mContext.getPackageName()));
			
			try {
				buffer = convertStreamToByteArray(imgInputStream);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

            intent = new Intent(IMAGE_BYTES);
            intent.putExtra(BYTE_ARRAY_KEY, buffer);
            mContext.sendBroadcast(intent);
	    	Log.d(TAG, "Broadcasting image!~");
	    	
	    	
	    	frame++;
	    	if(frame > 4)
	    		frame = 1;
	    	
	    	if(this.isCancelled()){
	    		Log.d(TAG, "Mock Server is cancelled!");
	    		cancel();
	    		break;
	    	}
	    	
	    	
	    	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    	//counter++;
		}

		try {
			imgInputStream.close();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		return null;
	}
	
	
    public void cancel() {
        try {
        	imgInputStream.close();
        } catch (IOException e) { 
        	Log.e(TAG, e.toString());
        }
    }
	
	
	public static byte[] convertStreamToByteArray(InputStream is) throws IOException {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte[] buff = new byte[IMG_BUFFER_SIZE];
	    int i = Integer.MAX_VALUE;
	    while ((i = is.read(buff, 0, buff.length)) > 0) {
	        baos.write(buff, 0, i);
	    }

	    return baos.toByteArray(); // be sure to close InputStream in calling function
	}
	
	
	/*
	void makeDeviceDiscoverable(int duration){
		// Make device discoverable
		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration); 

		startActivityForResult(discoverableIntent, BT_REQUEST);
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		  if (requestCode == BT_REQUEST && resultCode != Activity.RESULT_CANCELED) {			 
			      // processing code goes here	  	
			  	String discoverableInfo = "Device is now discoverable ";
			  	if(resultCode == 0)
			  		discoverableInfo += "infinitely!";
			  	else
			  		discoverableInfo += "for " + resultCode + " seconds!";	
				Toast.makeText(this, discoverableInfo, Toast.LENGTH_LONG).show();
				BTServerTask btServer = new BTServerTask();
				btServer.execute();
		  }
		  else {//if (resultCode == Activity.RESULT_CANCELED) {
			  String s = "Unable to make device discoverable.";
			  if(statusTV != null)
				  statusTV.setText(s);
			  else
				  Toast.makeText(this, s, Toast.LENGTH_LONG).show();
		}

	}
	*/

}
