package de.tudresden.inf.rn.mobilis.eet;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import de.tudresden.inf.rn.mobilis.eet.GPXTrack.Trkpt;

public class TrackEmulation {
	protected static final String TAG = "TrackEmulation";

	protected TrackEmulationThread tet = null;

	public TrackEmulation(File gpxFile, LocationListener mLocationListener,
			Handler mActivityResultHandler, Handler mEmulationEndHandler, int iterations) {
		GPXTrack gpx = new GPXTrack(true);

		try {
			FileInputStream fis = new FileInputStream(gpxFile);
			XmlPullParser xpp;
			xpp = XmlPullParserFactory.newInstance().newPullParser();
			xpp.setInput(fis, null);
			gpx.fromXML(xpp);
			// Log.i(TAG, gpx.toXML(false));
		} catch (Exception e) {
			Log.e(TAG, "emulateFile", e);
		}

		tet = new TrackEmulationThread(gpx, mLocationListener,
				mActivityResultHandler, mEmulationEndHandler, iterations);
	}
	
	public TrackEmulation(GPXTrack gpx, LocationListener mLocationListener,
			Handler mActivityResultHandler, Handler mEmulationEndHandler) {
		tet = new TrackEmulationThread(gpx, mLocationListener,
				mActivityResultHandler, mEmulationEndHandler, 1);
	}
	
	public void setHandlers(LocationListener mLocationListener,
			Handler onActivityResultHandler, 
			Handler mEmulationEndHandler){
		tet.setHandlers(mLocationListener,
				onActivityResultHandler,
				mEmulationEndHandler);
	}

	public void start() {
		tet.start();
	}

	public void stop() {
		tet.cancel();
	}
	
	public int countPoints(){
		return tet.gpx.trkpt.size();
	}
	
	public int getCurrentPosition(){
		return tet.position;
	}
	
	public void adaptSpeed(boolean increase){
		tet.adaptSpeed(increase);
	}

	public class TrackEmulationThread extends Thread {
		protected static final String TAG = "TrackEmulationThread";
		protected Thread thread;

		protected Boolean cancel = false;
		public int position = 0;
		public float speed = 1;
		public int iterations;

		protected GPXTrack gpx;
		protected LocationListener mLocationListener;
		protected Handler onActivityResultHandler;
		protected Handler mEmulationEndHandler;

		public TrackEmulationThread(GPXTrack gpx,
				LocationListener mLocationListener,
				Handler onActivityResultHandler, 
				Handler mEmulationEndHandler, int iterations) {
			super("TrackEmulation");
			this.gpx = gpx;
			this.mLocationListener = mLocationListener;
			this.onActivityResultHandler = onActivityResultHandler;
			this.mEmulationEndHandler = mEmulationEndHandler;
			this.iterations = iterations;
		}

		@Override
		public void start() {
			if(super.getState() == Thread.State.NEW){
				super.start();
				Log.i(TAG, "Track-emulation startet");
				cancel = false;
			}
		}

		public void cancel() {
			cancel = true;
			this.interrupt();
			if(mEmulationEndHandler != null) mEmulationEndHandler.sendEmptyMessage(0);
		}
		
		public void adaptSpeed(boolean increase){
			if(increase) if(speed < 10) speed += 0.1;
			else if(speed > 0) speed -= 0.1;
		}
		
		public void setHandlers(LocationListener mLocationListener,
				Handler onActivityResultHandler, 
				Handler mEmulationEndHandler){
			this.mLocationListener = mLocationListener;
			this.onActivityResultHandler = onActivityResultHandler;
			this.mEmulationEndHandler = mEmulationEndHandler;
		}

		@Override
		public void run() {
			ArrayList<GPXTrack.Trkpt> trkpts = (ArrayList<Trkpt>) gpx.getTrackPoints().clone();
			Trkpt lastTrkpt = null;
			for(int i = 0; i < this.iterations; i++){
				for (Trkpt pt : trkpts) {
					if (cancel) {
						Log.i(TAG, "Emulation aborted");
						return;
					}
	
					if (lastTrkpt != null) {
						long time = Math.abs(pt.time - lastTrkpt.time);
						if(time > 60000){ time = 5000;  Log.d("EET", "emulation: long waiting, cut... (" + time + "ms -> 5s)"); }
						try {
							long sleep = (long)(time/speed);
							if(sleep > 0 ) Thread.sleep(sleep);
						} catch (InterruptedException e) {
							Log.i("EET", "Emulation aborted (interrupt)");
							return;
						} catch (Exception e){
							Log.e("EET", "Emulation exception", e);
						}
					}
	
					/* update location listener */
					if (mLocationListener != null){
						Location loc = new Location(LocationManager.GPS_PROVIDER);
						loc.setLatitude(pt.lat);
						loc.setLongitude(pt.lon);
						loc.setSpeed(pt.speed);
						loc.setTime(pt.time);
						mLocationListener.onLocationChanged(loc);
					}
					
					/* send activity message */
					if (onActivityResultHandler != null) {
						Bundle resultBundle = new Bundle();
						resultBundle.putString("activityName", pt.activity);
						resultBundle.putInt("activityConfidence", 50);
	
						Message msg = new Message();
						msg.what = 1;
						msg.obj = resultBundle;
	
						onActivityResultHandler.sendMessage(msg);
					}
	
					lastTrkpt = pt;
					position++;
				}
				Log.i("EET", "Emulation terminated (" + i + " of " + this.iterations + ")");
				lastTrkpt = null;
			}
			if(mEmulationEndHandler != null) mEmulationEndHandler.sendEmptyMessage(1);
		}
	};
}
