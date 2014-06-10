Contents of the android_app folder
==================================

A. LinuxAndC folder contains our Android project

B. appcombat_v7 is a library needed for compatibility with other Android versions 

C. SmartExtensionUtils is a library by Sony that all Smart Extensions 
	(Android apps that use accessories like the Smartwatch) have to use
	
D. SampleControlExtension is a sample app by Sony that uses the Control API
	(see E)
	
E. Praktikum Android App Documentation is a summarized Documentation of everything 		that has to do with the Android part of the project

F. SmartExtension_SampleExtensionTutorial.pdf, SmartExtension API Specification.pdf 	  and SmartWatch_WP_1.pdf are documentations by Sony.


Notes
=====

In our project (A) I used and modified all classes from the sample app (D), 
except for:

1. SampleControlSmartWirelessHeadsetPro.java, 
which is meant for another accessory, and 

2. SamplePreferenceActivity.java, 
which is meant for the user handling the settings of the app. 
(We have no settings to be handled by the user)



Otherwise, new classes are: 

1. MainActivity.java, 
which implements the UI of the Android app

2. BTCommThread.java, 
which implements a Bluetooth communication thread, 
that receives data (images) from the Linux PC
	
3. Constants.java, 
which contains some constants to be used for 
communication between the different app components

