package org.praktikum.linuxandc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.registration.Registration;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends ActionBarActivity implements Constants {

	private static final String TAG = "MainActivity";
	private static final boolean D = true;
	private static BluetoothAdapter mBluetoothAdapter;

	// you can use one of the many random UUID generators on the web, then
	// initialize a UUID with fromString(String).
	private static final UUID MY_UUID_SECURE = UUID
			.fromString("d2e19fab-f367-43d9-9cc2-b6837e7eb915");
	
	private static final int BT_REQUEST = 1; // must be > 0
	private static final int DURATION_DISCOVERABLE = 300; // means infinitely, max = 3600s
	
	private static TextView statusTV, swConnStatusTV;
	private static Button btServerButton, listBTDevicesButton;
	private static ListView devicesListView;
	private ArrayList<String> devicesList;
	
	private static final String ourSmartWatchMAC = "B4:52:7D:FB:AE:3C";
	


	// The Handler that gets information back from the BTCommThread
	private static final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case IMAGE_UPDATE:
				if (D)
					Log.i(TAG, "RECEIVED NEW IMAGE FROM PC, size = " + msg.arg1 + " bytes");
				statusTV.setText("NEW FRAME ("+msg.arg2+") RECEIVED FROM LINUX COMPUTER!");
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			
			statusTV = (TextView) rootView.findViewById(R.id.statusTV);
			btServerButton = (Button) rootView.findViewById(R.id.startBTServerButton);
			listBTDevicesButton =  (Button) rootView.findViewById(R.id.listBTDevicesButton);	
			//swConnStatusTV = (TextView) rootView.findViewById(R.id.swConnStatusTV);
			devicesListView = (ListView) rootView.findViewById(R.id.devicesLV);
			
			// Check if the device supports bluetooth
			if (mBluetoothAdapter == null) {
				statusTV.setText("Device does not support bluetooth!");
				btServerButton.setEnabled(false);
				listBTDevicesButton.setEnabled(false);
			}

			return rootView;
		}
	}
	
	
	
	public void startBTServer(View button){
		btServerButton = (Button) button;
		btServerButton.setEnabled(false);
		
		// Make device discoverable
		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DURATION_DISCOVERABLE); 

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
	
	private class BTServerTask extends AsyncTask<Void, String, Void> {
		
		private final BluetoothServerSocket mmServerSocket;

		public BTServerTask() {
			BluetoothServerSocket tmp = null;
			try {
				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
						"Praktikum Smartwatch", MY_UUID_SECURE);

			} catch (IOException e) { }
			mmServerSocket = tmp;
			statusTV.setText("Now listening for BT connections...");
		}

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				try {
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					break;
				}
				// If a connection was accepted
				if (socket != null) {
					publishProgress(socket.getRemoteDevice().getName() + " connected!");
					// Do work to manage the connection (in a separate thread)
					////////////////////////////////////////////////////////////////////////////////////////////		
					BTCommThread commThread = new BTCommThread(socket, mHandler, MainActivity.this);
					commThread.start();
					///////////////////////////////////////////////////////////////////////////////////////////
					try {
						mmServerSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
			}
			
			return null;
		}
		
		
		@Override
	    protected void onProgressUpdate(String... values) {
			statusTV.setText(values[values.length-1]);
	    }
		
		
		
		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) {
			}
		}
		
	}
	
	
	
	
	
	public void listBTDevices(View button){
		listBTDevicesButton = (Button) button;
		listBTDevicesButton.setEnabled(false);		
		
	    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices(); 
	    devicesList = new ArrayList<String>();
	    boolean found = false;
	    for(BluetoothDevice btd : pairedDevices){
	    	devicesList.add(btd.getName() + " ("+btd.getAddress() +")");
	    	if(btd.getAddress().equals(ourSmartWatchMAC))
	    		found = true;
	    }
	    
         if(devicesList.size() == 0)
        	 statusTV.setText("No devices paired!");
         else {
         ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                 this, 
                 android.R.layout.simple_list_item_1,
                 devicesList );

         devicesListView.setAdapter(arrayAdapter);
         }
         
         if(found){

        	 Log.d(TAG, "Starting SWService with Control start intent");
        	 Intent serviceIntent = new Intent(this, SWExtensionService.class);
        	 serviceIntent.setAction(Control.Intents.CONTROL_START_INTENT);
        	 startService(serviceIntent);

        	 SendImageTask sit = new SendImageTask();
        	 sit.execute();
         }
	}
	
	
	
	
	
	public class SendImageTask extends AsyncTask<Void, Void, Intent> {
		private InputStream imgInputStream;

		@Override
		protected Intent doInBackground(Void... params) {
			byte[] buffer = null;  // buffer store for the stream

			
			int frame = 1;
			int counter = 1;
			Intent intent = null;
			
			while(counter < 12){
			
			imgInputStream = getResources().openRawResource(
	   	            getResources().getIdentifier("raw/img" + frame,
	   	    	            "raw", getPackageName()));
			
			try {
				buffer = convertStreamToByteArray(imgInputStream);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

            intent = new Intent(IMAGE_BYTES);
            intent.putExtra(BYTE_ARRAY_KEY, buffer);
	    	MainActivity.this.sendBroadcast(intent);
	    	Log.d(TAG, "Broadcasting image!~");
	    	
	    	
	    	frame++;
	    	if(frame > 4)
	    		frame = 1;
	    	
	    	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	counter++;
			}

			return intent;
		}
		
		
	     protected void onPostExecute(Intent intent) {
	    	 //MainActivity.this.sendBroadcast(intent);
	    	 //Log.d(TAG, "Broadcasting image!~");
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
    // Handler for received Intents for smartwatch connection status
    public class SWStatusReceiver extends BroadcastReceiver {
      @Override
      public void onReceive(Context context, Intent intent) {
    	  boolean connected = intent.getBooleanExtra("connected", false);
    	  if(connected){
    		  swConnStatusTV.setText("YES");
    		  swConnStatusTV.setTextColor(Color.GREEN);
    		  btServerButton.setEnabled(true);
    	  }
    	  else{
    		  swConnStatusTV.setText("NO");
    		  swConnStatusTV.setTextColor(Color.RED);
    		  btServerButton.setEnabled(false);
    	  }
      }
    };
    
    */





}
