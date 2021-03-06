Linux & C (Practical Course) Project
====================================
====================================

Motivation
========== 
Future drivers could wear a device like a smartwatch that monitors their driving ability but also the car's state. For example, the tires air pressure could be transmitted. In this project, we implement a prototype similar to Nissan's Nismo Smartwatch that monitors the system (car) and sends the collected data wirelessly to the smartwatch. For simulation purposes instead of actual car data, we send system information from a Linux-PC.


Goals
=====
1. Develop an open GUI for the smartwatch
2. Implement a display driver for controlling wireless displays (extension of LCD4Linux)
3. Implement a protocol for reading and displaying data on the smartwatch.


Contents
========

A. The LinuxAndC folder contains our Android project.

B. appcombat_v7 is a library needed for compatibility with previous Android versions.

C. SmartExtensionUtils is a library by Sony that all Sony Smart Extensions (Android apps that use accessories like the Smartwatch) must use.
	
D. SampleControlExtension is a sample app by Sony that uses the Control API (see E).
	
E. docs/Praktikum Android App Documentation.pdf is a summarized Documentation of everything that has to do with the Android part of the project.

F. docs/SmartExtension_SampleExtensionTutorial.pdf, 
   docs/SmartExtension API Specification.pdf and 
   docs/SmartWatch_WP_1.pdf are documentations by Sony.


Notes
=====

In our project (A) I used and modified all classes from the sample app (D), except for:

1. SampleControlSmartWirelessHeadsetPro.java, 
which is meant for the headset accessory. 

2. SamplePreferenceActivity.java, 
which is meant for editing the settings of the app by the user. 
(We have no settings to be handled by the user.)



Otherwise, new classes are: 

1. MainActivity.java, 
   implements the UI of the Android app.

2. BluetoothServer.java,
which starts listening for connections of bluetooth devices. 
Once a device is connected (the linux-pc), the socket is passed to the a Runnable DataShifter.
(running in the same background thread)

3. DataShifter.java, 
which implements a Bluetooth communication that receives data (images) from the Linux PC and sends them to the SmartExtension
	
4. Constants.java, 
which contains some constants to be used for communication between the different app components

5. MockServer.java, 
sends images from the phone to the watch
(used for testing purposes)

