package de.voelker.diplom.EET;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class EET implements LocationListener{
	private static final String TAG = "EET";
	
	private LocationManager locMan;
	private Context ctx;
	private Location lastLoc;
	private LocationListener externListener;
	
	private Boolean isTracking = false;
	
	/******************* Handler *************************/
	private Handler onLocationChangedHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == 1) lastLoc = (Location) msg.obj;
		}
	};
	
	public EET(Context ctx){
		this.ctx = ctx;
		locMan = (LocationManager) ctx.getSystemService(ctx.LOCATION_SERVICE);
		lastLoc = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}
	
	public void start(){
		locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, this);
		isTracking = true;
	}
	
	public void stop(){
		locMan.removeUpdates(this);
		isTracking = false;
	}
	
	public Location getLastLocation() {
		return lastLoc;
	}
	
	public void registerLocationListener(LocationListener locationListener){
		this.externListener = locationListener;
	}
	
	/**************** LocationListener **********************/

	@Override
	public void onLocationChanged(Location arg0) {
		if(externListener != null) externListener.onLocationChanged(arg0);
		
		Message msg = new Message();
		msg.what = 1;
		msg.obj = arg0;
		onLocationChangedHandler.sendMessage(msg);
		
		Log.v(TAG, "onLocationChanged: N" + arg0.getLatitude() + " E" + arg0.getLongitude());
		
	}

	@Override
	public void onProviderDisabled(String arg0) {
		if(externListener != null) externListener.onProviderDisabled(arg0);
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		if(externListener != null) externListener.onProviderEnabled(arg0);
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		if(externListener != null) externListener.onStatusChanged(arg0, arg1, arg2);
		
	}

	/******************* Getter/Setter *********************/
	
	public Boolean isTracking() {
		return isTracking;
	}

}
