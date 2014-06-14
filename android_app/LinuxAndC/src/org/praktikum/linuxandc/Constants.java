package org.praktikum.linuxandc;

public interface Constants {
	 // Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;	
	public static final int IMAGE_UPDATE = 6;
	public static final int DEVICE_UPDATE = 7;
	public static final int SERVER_STOPPED = 8;
	//public static final int SW_CONNECTED = 7;
	//public static final int SW_DISCONNECTED = 8;
	
	 // Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	public static final String IMAGE_BYTES = "imgUpdate";
	
    public static final String BYTE_ARRAY_KEY = "byteArray";
    
    public static final int IMG_BUFFER_SIZE = 128*128*4; // WIDTH * HEIGHT * RGBA
}
