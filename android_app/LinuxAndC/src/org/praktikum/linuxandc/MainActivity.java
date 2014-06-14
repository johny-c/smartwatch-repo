package org.praktikum.linuxandc;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.sonyericsson.extras.liveware.aef.control.Control;

public class MainActivity extends ActionBarActivity implements Constants {

	private static final String TAG = "MainActivity";
	private static final boolean D = true;
	private static BluetoothAdapter mBluetoothAdapter;
	private MockServer mockServer;
	private ConnectionListener connectionListener;
	private BluetoothSocket btSocket;
	private DataShifter dataShifter;
	
	private static TextView statusTV, pairedDevicesTV;
	private static ToggleButton realServerButton, mockServerButton;
	private static ListView pairedDevicesLV;
	private ArrayList<String> devicesList;
	
	//private boolean mockServerRunning = false;
	//private boolean btServerRunning = false;
	//private static final int BT_REQUEST = 1; // must be > 0
	//private static final int DURATION_DISCOVERABLE = 300; // 0 means infinitely, max = 3600s
	//private static final String OUR_SMARTWATCH_MAC = "B4:52:7D:FB:AE:3C";
	
	// The Handler that gets information back from the DataForwarder
	private static final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			
				case DEVICE_UPDATE:
					if(D){
						Log.i(TAG, "NEW DEVICE CONNECTED, device name = " + msg.obj);
					}
					statusTV.setText(msg.obj + " is now connected!");
					break;
					
				case IMAGE_UPDATE:
					if (D)
						Log.i(TAG, "RECEIVED NEW IMAGE FROM PC, size = " + msg.arg1 + " bytes");
					statusTV.setText("NEW FRAME ("+msg.arg2+") RECEIVED FROM LINUX COMPUTER!");
					break;
					
				case SERVER_STOPPED:
					if(D){
						Log.i(TAG, "SERVER STOPPED");
					}
					statusTV.setText("Server is now stopped!");					
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
		
	   	 Log.d(TAG, "Starting SWService with Control start intent");
	   	 Intent serviceIntent = new Intent(this, SWExtensionService.class);
	   	 serviceIntent.setAction(Control.Intents.CONTROL_START_INTENT);
	   	 startService(serviceIntent);
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
			realServerButton = (ToggleButton) rootView.findViewById(R.id.realServerButton);
			mockServerButton = (ToggleButton) rootView.findViewById(R.id.mockServerButton);
			pairedDevicesTV = (TextView) rootView.findViewById(R.id.pairedDevicesTV);
			//listBTDevicesButton =  (Button) rootView.findViewById(R.id.listBTDevicesButton);	
			//swConnStatusTV = (TextView) rootView.findViewById(R.id.swConnStatusTV);
			pairedDevicesLV = (ListView) rootView.findViewById(R.id.pairedDevicesLV);
			
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();	
			// Check if the device supports bluetooth
			if (mBluetoothAdapter == null) {
				statusTV.setText("Device does not support bluetooth!");
				realServerButton.setEnabled(false);
				mockServerButton.setEnabled(false);
			}
			return rootView;
		}
	}
	
	
	protected void onDestroy(){
		super.onDestroy();
		stopMockServer();
		stopRealServer();
		
		 Log.d(TAG, "Stoping SWService with Control stop intent");
		 Intent serviceIntent = new Intent(this, SWExtensionService.class);
		 serviceIntent.setAction(Control.Intents.CONTROL_STOP_INTENT);
		 stopService(serviceIntent);
	}


	public void toggleMockServer(View button){
		mockServerButton.setEnabled(false);
		
		String s = mockServerButton.isChecked() ? "RUNNING" : "STOPPED";
		Toast.makeText(this, "Mock Server is "+s, Toast.LENGTH_SHORT).show();
		
		if(mockServerButton.isChecked()){
			realServerButton.setEnabled(false);
			startMockServer();	
			mockServerButton.setChecked(true);
		}
		else {
			stopMockServer();
			realServerButton.setEnabled(true);
			mockServerButton.setChecked(false);
		}
		
		mockServerButton.setEnabled(true);
	}
	
	
	private void startMockServer() {
		listPairedDevices();
		mockServer = new MockServer(this);
		mockServer.execute();
	}	
	
	
	private void stopMockServer() {
		if(mockServer != null){
			mockServer.cancel(true);	
			//while(mockServer.getStatus() != AsyncTask.Status.FINISHED);
		}	
	}
	
	
	
	public void toggleRealServer(View button){
		realServerButton.setEnabled(false);

		String s = realServerButton.isChecked() ? "RUNNING" : "STOPPED";
		Toast.makeText(this, "Real Server is "+s, Toast.LENGTH_SHORT).show();
		
		if(realServerButton.isChecked()){
			boolean paired = listPairedDevices();
			if(!paired){
				statusTV.setText("Pair phone with computer and smartwatch!");
				realServerButton.setChecked(false);
				realServerButton.setEnabled(true);
				return;
			}
			mockServerButton.setEnabled(false);
			
			startRealServer();
			realServerButton.setChecked(true);
		}
		else {
			stopRealServer();
			realServerButton.setChecked(false);
			mockServerButton.setEnabled(true);	
		}
		
		realServerButton.setEnabled(true);
		
	}
	
	
	private void startRealServer(){
			
		connectionListener = new ConnectionListener(mHandler, this);
		connectionListener.execute();
		statusTV.setText("Now listening for BT connections...");
		
		/*
		try {
			dataShifter = connectionListener.get();
		} catch (InterruptedException e) {			
			Log.e(TAG, e.toString());
			statusTV.setText("Real Server was stopped..!");
		} catch (ExecutionException e) {
			Log.e(TAG, e.toString());
			statusTV.setText("Connection Listener was stopped..!");
		}
		*/
	}
	
	
	private void stopRealServer(){
		if(connectionListener != null){
			connectionListener.cancel(true);
			//while(connectionListener.getStatus() != AsyncTask.Status.FINISHED);
		}
		
		if(dataShifter != null){
			dataShifter.cancel();
			//while(dataShifter.getStatus() != AsyncTask.Status.FINISHED);
		}
	}
	 
	
	
	
	public boolean listPairedDevices(){		
		
		if(!mBluetoothAdapter.isEnabled()){
			statusTV.setText("Bluetooth is not turned on!");
			return false;
		}
		
		 devicesList = new ArrayList<String>();
		 Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		 boolean computerFound = false; 
		 boolean watchFound = false;
		 int deviceClass;
		 String deviceClassName;
		 for(BluetoothDevice btd : pairedDevices){
			 deviceClass = btd.getBluetoothClass().getMajorDeviceClass();
			 deviceClassName = getDeviceClassName(deviceClass);
			 
			 devicesList.add(btd.getName() 
					+ "\n"+btd.getAddress() 
					+ "\n("+deviceClassName+")");
			 
			if(deviceClass == BluetoothClass.Device.Major.COMPUTER){
				computerFound = true;
			}
			else if(deviceClass == BluetoothClass.Device.Major.WEARABLE){
				watchFound = true;
			}
		 }
		 
		 
		 if(devicesList.size() == 0) {
			 pairedDevicesTV.setText("No devices paired!");
		 }
		 else {
			 pairedDevicesTV.setText("Paired Devices");
			         ArrayAdapter<String> arrayAdapter = 
			        		 new ArrayAdapter<String>(this, 
			                 android.R.layout.simple_list_item_1,
			                 devicesList );
			
			 pairedDevicesLV.setAdapter(arrayAdapter);
		 }
		         
         if(computerFound && watchFound)
        	 return true;
 
         return false;
	}
	
	
	
	private String getDeviceClassName(int deviceClass){
		String deviceClassName;	
		
    	switch(deviceClass){
	    	case BluetoothClass.Device.Major.AUDIO_VIDEO:
	    		deviceClassName = "Audio_Video"; break;
	    		
	    	case BluetoothClass.Device.Major.COMPUTER:
	    		deviceClassName = "Computer"; break;
	    		
	    	case BluetoothClass.Device.Major.HEALTH:
	    		deviceClassName = "Health"; break;
	    		
	    	case BluetoothClass.Device.Major.IMAGING:
	    		deviceClassName = "Imaging"; break;
	    		
	    	case BluetoothClass.Device.Major.MISC:
	    		deviceClassName = "Misc"; break;
	    		
	    	case BluetoothClass.Device.Major.NETWORKING:
	    		deviceClassName = "Networking"; break;
	    	
	    	case BluetoothClass.Device.Major.PERIPHERAL:
	    		deviceClassName = "Peripheral"; break;
	    		
	    	case BluetoothClass.Device.Major.PHONE:
	    		deviceClassName = "Phone"; break;
	    		
	    	case BluetoothClass.Device.Major.TOY:
	    		deviceClassName = "Toy"; break;
	    		
	    	case BluetoothClass.Device.Major.WEARABLE:
	    		deviceClassName = "Wearable"; break;
	    		
	    	case BluetoothClass.Device.Major.UNCATEGORIZED:
	    		deviceClassName = "Unknown"; break;
	    		
	    	default: deviceClassName = ""; break;
		}
    	
    	return deviceClassName;
	}


}
