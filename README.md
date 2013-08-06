FriendFinder
============

Map-based demo application which shows current positions of friends on a map with the ability to chat.

Running the app on your device
-------------------------------------------

* Install the Service on your mobilis-server *(FriendFinder_Service/dist/FriendFinder_Service_v1.jar)*
* Open the Android-project *FriendFinder_Android*
* Enter your coordinator-JID in the class *IQProxy.java*
* Run the Android-App
* At the first start, you will be asked for your XMPP-login. Go to the settings and enter username and password.
* Create a service-instance, join them and have fun!

Every time a service-instance is created there was created a copy of the sqlite-database in the folder *friendfinder_db* in the maindirectory of the mobilis-server. If there is a file called *eet.db* the service create a copy of this, which is used by the service-instance. Otherwise an empty database will be created.

If you wish to disable the hole energy-efficient-stuff and use normal tracking, change the constructor of *EET* in the *BackgroundService*-Method *onCreate* to *EET_simple* with the same arguments.
