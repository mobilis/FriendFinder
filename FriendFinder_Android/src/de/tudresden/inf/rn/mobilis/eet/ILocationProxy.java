package de.tudresden.inf.rn.mobilis.eet;

import android.location.Location;
import android.location.LocationListener;

/**
 * Interface define the public methods of the tracing-classes
 */
public interface ILocationProxy {
	
	public final int PRED_OFF = 0;
	public final int PRED_CHECK = 1;
	public final int PRED_ON = 2;
	
	public void start();

	public void stop();

	public Location getLastLocation();

	public void registerLocationListener(LocationListener locationListener);

	/******************* Getter/Setter *********************/

	public Boolean isTracking();

	public void setServiceJID(String serviceJID);
	public int getPredStatus();
	
	public GPXTrack getPredTempTrack();
	
	public String getEETStatus();
}
