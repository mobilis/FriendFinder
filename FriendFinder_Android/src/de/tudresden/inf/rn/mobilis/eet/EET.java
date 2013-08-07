package de.tudresden.inf.rn.mobilis.eet;

import java.io.File;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognitionClient;

import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientLocation;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.IXMPPCallback;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.IsTrackAvailableResponse;

/**
 * class for energy-efficient tracking implements a prediction-algorithm and an
 * activity-recognition
 * 
 */
public class EET implements ILocationProxy, LocationListener,
		ConnectionCallbacks, OnConnectionFailedListener {
	private static final String TAG = "EET";

	/**
	 * proxy, which has the method to send the isTrackAvailable-message to the
	 * service
	 */
	private IEETProxy mobilisProxy;
	/**
	 * jid of the service
	 */
	private String serviceJID;

	/**
	 * Android LocactionManager
	 */
	private LocationManager locMan;
	/**
	 * Context to get the LocationManager
	 */
	private Context ctx;
	/**
	 * last received location
	 */
	private Location lastLoc;
	/**
	 * extern listener, which will be called, if a new position detected
	 */
	private LocationListener externListener;

	/**
	 * PendingIntent for activity-recognition
	 */
	private PendingIntent mActivityRecognitionPendingIntent;
	/**
	 * the regognition-client
	 */
	private ActivityRecognitionClient mActivityRecognitionClient;
	/**
	 * is activity-recognition connected for enabling/disabling
	 */
	private boolean arInProgress = false;
	/**
	 * is activity-recognition enabled
	 */
	private boolean arEnabled = false;
	/**
	 * last received activity
	 */
	private String lastActivity = "unknown";
	/**
	 * convidence for the last activity
	 */
	private int lastActivityConvidence = 0;

	/**
	 * active intern locationlistener (gps or prediction)
	 */
	private LocationListener curGPSLocListener = this;
	/**
	 * is tracking enabled
	 */
	private Boolean isTracking = false;
	/**
	 * the gpx-track to save the received points (not in emulation-mode)
	 */
	private GPXTrack mGPX;
	/**
	 * gps-interval for disabled prediction
	 */
	private int gpsIntervall = 5000;

	public final int PRED_OFF = 0;
	public final int PRED_CHECK = 1;
	public final int PRED_ON = 2;

	/**
	 * gps-interval for enabled prediction
	 */
	private int predGpsIntervall = 30000;
	/**
	 * current database-id of the recorded or emulated track
	 */
	private int predCurrentTrackId;
	/**
	 * predicted track
	 */
	private GPXTrack predTempTrack;
	/**
	 * emulation-class for prediction
	 */
	private TrackEmulation predTrackEmulation;
	/**
	 * current position in prediction-track
	 */
	private int predCurrentPoint;
	/**
	 * status of prediction (off/check/on)
	 */
	private int predStatus;
	/**
	 * status-string for debug-outpug
	 */
	private String predLastServerStatus = "";
	/**
	 * status-string for debug-outpug
	 */
	private String predLastClientStatus = "";

	/**
	 * messured time in prediction-mode
	 */
	private long timeInPrediction = 0;
	private long tempTimeInPrediction = 0;
	/**
	 * messured time in gps-mode
	 */
	private long timeInGPS = 0;
	private long tempTimeInGPS = 0;
	/**
	 * count the total gps-updates
	 */
	private int countGPSUpdates = 0;

	/**
	 * real or emulation mode
	 */
	private Boolean emulationMode = false;
	/**
	 * emulation-class
	 */
	private TrackEmulation trackEmulation = null;

	/******************* Handler *************************/
	/**
	 * called, if the location has changed (gps or prediction) include the
	 * prediction-check algorithm, whether a received track is correct
	 */
	private Handler onLocationChangedHandler = new Handler(
			Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				lastLoc = (Location) msg.obj;
				mGPX.addTrackPoint(lastLoc.getLatitude(),
						lastLoc.getLongitude(), lastActivity,
						lastLoc.getSpeed(), lastLoc.getTime(),
						lastLoc.getAltitude(), lastLoc.getAccuracy());

				/* check prediction status */
				switch (predStatus) {
				case PRED_CHECK:
					// if(!emulationMode) countGPSUpdates++;
					int i = -1;
					if (lastLoc != null && predTempTrack != null)
						i = predTempTrack.getTrackPosition(
								lastLoc.getLatitude(), lastLoc.getLongitude());
					if (i != -1) {
						if (i == predCurrentPoint - 1
								|| i == predCurrentPoint - 2) {
							// lösche alle nachfolgenden punkte, drehe liste um,
							// starte emulation
							predTempTrack.cutTrack(i, false);
							predLastClientStatus = "PRED_CHECK: cut + reverse Track (size: "
									+ predTempTrack.getTrackPoints().size()
									+ ")";

							enablePrediction(predTempTrack);
							sendRateTrack();
						} else if (i == predCurrentPoint + 1
								|| i == predCurrentPoint + 2) {
							predTempTrack.cutTrack(i, true);
							predLastClientStatus = "PRED_CHECK: cut Track (size: "
									+ predTempTrack.getTrackPoints().size()
									+ ")";

							enablePrediction(predTempTrack);
							sendRateTrack();
						} else {
							predLastClientStatus = "PRED_CHECK: anderen Punkt gefunden: "
									+ i + " -> " + predCurrentPoint;
							predCurrentPoint = i;
						}
					} else {
						predStatus = PRED_OFF;
						predLastClientStatus = "PRED_CHECK: disable prediction (next point not in track)";
						Log.v(TAG, predLastClientStatus);
					}
					Log.v(TAG, predLastClientStatus);
					break;
				case PRED_OFF:
					// if(!emulationMode) countGPSUpdates++;
					sendIsTrackAvailable();
					break;
				case PRED_ON:
					// check prediction in secondLocListener
					break;
				}
			}
		}
	};

	/**
	 * called, if a activity was detected
	 */
	protected Handler onActivityResultHandler = new Handler(
			Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				Bundle result = (Bundle) msg.obj;
				lastActivity = result.getString("activityName");
				lastActivityConvidence = result.getInt("activityConfidence");
				Log.v(TAG, "new activity detected: " + lastActivity + " ("
						+ lastActivityConvidence + ")");
			}
		}
	};

	/**
	 * called, if the prediction was at the end of the track
	 */
	protected Handler onPredictionEndHandler = new Handler(
			Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			if (predStatus == PRED_ON)
				disablePrediction();
		}
	};

	/**
	 * called, when the emulation terminated
	 */
	protected Handler onEmulationEndHandler = new Handler(
			Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
		}
	};

	/**
	 * called, if a IsTrackAvailableResponse-message was received set the
	 * prediction-status to check
	 */
	protected IXMPPCallback<IsTrackAvailableResponse> isTrackAvailableResponse = new IXMPPCallback<IsTrackAvailableResponse>() {
		@Override
		public void invoke(IsTrackAvailableResponse xmppBean) {
			Log.v(TAG, "onIsTrackAvailable: " + xmppBean.getResult() + ", id "
					+ xmppBean.getTrackId() + ", "
					+ xmppBean.getGpxTrack().trkpt.size() + " points");

			GPXTrack gpx = xmppBean.getGpxTrack();

			if (xmppBean.getResult()) {
				if (gpx.getTrackPoints().isEmpty()) {
					predLastServerStatus = "Prediction: Point with this trackid is already in DB, Result empty";
					Log.i(TAG, predLastServerStatus);

					predCurrentTrackId = xmppBean.getTrackId();
				} else {
					predLastServerStatus = "Prediction: Track received, check prediction";
					Log.i(TAG, predLastServerStatus);

					predTempTrack = gpx;
					predCurrentTrackId = 0;
					predCurrentPoint = gpx.getTrackPosition(
							lastLoc.getLatitude(), lastLoc.getLongitude());
					predStatus = PRED_CHECK;
				}
			} else {
				predLastServerStatus = "Prediction: No Result";
				Log.i(TAG, predLastServerStatus);
				predCurrentTrackId = xmppBean.getTrackId();
			}
		}
	};

	/******************** class specific functions ******************************/

	public EET(Context ctx, String serviceJID, IEETProxy mobilisProxy) {
		this.ctx = ctx;
		this.mobilisProxy = mobilisProxy;
		this.serviceJID = serviceJID;
		this.mGPX = new GPXTrack();

		File gpxFile = GPXTrack.getGPXFile("emulation");
		emulationMode = gpxFile.exists();

		initGPS();
	}

	/**
	 * start tracking or emulation
	 */
	public void start() {
		if (emulationMode)
			Toast.makeText(
					ctx.getApplicationContext(),
					"Tracking start" + " (Emulation, "
							+ trackEmulation.countPoints() + " Points)",
					Toast.LENGTH_SHORT).show();
		else
			Toast.makeText(ctx.getApplicationContext(), "Tracking start",
					Toast.LENGTH_SHORT).show();
		Log.i(TAG, "start Tracking");

		enableGPS();
		initPrediction();

		isTracking = true;
	}

	/**
	 * stop tracking or emulation
	 */
	public void stop() {
		disablePrediction();
		disableGPS();

		if (!emulationMode)
			mGPX.saveFile();
		else

		if (isTracking)
			Toast.makeText(ctx.getApplicationContext(), "Tracking stop",
					Toast.LENGTH_SHORT).show();
		isTracking = false;

		Log.i(TAG, "stop Tracking (GPS: " + (timeInGPS / 1000.0) + "s, Pred: "
				+ (timeInPrediction / 1000.0) + "s)");
	}

	/**
	 * get last received location
	 */
	public Location getLastLocation() {
		return lastLoc;
	}

	/**
	 * register an extern location listener there can be only one listener
	 * registered
	 */
	public void registerLocationListener(LocationListener locationListener) {
		this.externListener = locationListener;
	}

	/************* gps *****************/

	/**
	 * the activity-recognition, gps or emulation was initialized
	 */
	protected void initGPS() {
		locMan = (LocationManager) ctx
				.getSystemService(Context.LOCATION_SERVICE);
		lastLoc = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (!emulationMode) {

			// set activity-result handler
			ActivityRecognitionIntentService.onActivitiyResultHandler = onActivityResultHandler;

			// init acitivity recognition
			mActivityRecognitionClient = new ActivityRecognitionClient(ctx,
					this, this);
			Intent intent = new Intent(ctx,
					ActivityRecognitionIntentService.class);
			mActivityRecognitionPendingIntent = PendingIntent.getService(ctx,
					0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		} else {
			lastLoc = new Location("");
			trackEmulation = new TrackEmulation(
					GPXTrack.getGPXFile("emulation"), curGPSLocListener,
					onActivityResultHandler, onEmulationEndHandler, 100);
			Log.i(TAG, "Emulation mode");
		}
	}

	/**
	 * set the current listener to gps, set normal gps-interval, activates
	 * activity-recognition
	 */
	protected void enableGPS() {
		Log.i(TAG, "enableGPS");
		tempTimeInGPS = System.currentTimeMillis();

		LocationListener oldLocLis = curGPSLocListener;
		curGPSLocListener = this;

		if (!emulationMode) {
			locMan.removeUpdates(oldLocLis);
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					gpsIntervall, 0, curGPSLocListener);
			if (!arInProgress) {
				arInProgress = true;
				mActivityRecognitionClient.connect();
			}
		} else {
			trackEmulation.setHandlers(curGPSLocListener,
					onActivityResultHandler, onEmulationEndHandler);
			trackEmulation.start();

			// energytest
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					gpsIntervall, 0, testLocListener);
		}
	}

	/**
	 * set the current listener to predition, set reduced
	 * prediction-gps-interval, deactivates activity-recognition
	 */
	protected void disableGPSForPrediction() {
		LocationListener oldLocLis = curGPSLocListener;
		curGPSLocListener = secondLocListener;
		if (tempTimeInGPS > 0)
			timeInGPS += (System.currentTimeMillis() - tempTimeInGPS);
		tempTimeInGPS = 0;
		Log.i(TAG, "disableGPSForPrediction (" + (timeInGPS / 1000.0) + "s)");

		if (!emulationMode) {
			locMan.removeUpdates(oldLocLis);
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					predGpsIntervall, 0, curGPSLocListener);
			if (!arInProgress) {
				arInProgress = true;
				mActivityRecognitionClient.connect();
			}
		} else {
			trackEmulation.setHandlers(curGPSLocListener, null,
					onEmulationEndHandler);

			// energytest
			locMan.removeUpdates(testLocListener);
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					predGpsIntervall, 0, testLocListener);
		}
	}

	/**
	 * remove LocationListener, deactivates activity-recognition
	 */
	protected void disableGPS() {
		if (tempTimeInGPS > 0)
			timeInGPS += (System.currentTimeMillis() - tempTimeInGPS);
		tempTimeInGPS = 0;
		Log.i(TAG, "disableGPS(" + (timeInGPS / 1000.0) + "s)");

		if (!emulationMode) {
			locMan.removeUpdates(curGPSLocListener);
			if (!arInProgress) {
				arInProgress = true;
				mActivityRecognitionClient.connect();
			}
		} else {
			if (trackEmulation != null)
				trackEmulation.stop();

			// energytest
			locMan.removeUpdates(testLocListener);
		}
	}

	/************ prediction ******************/
	/**
	 * initialize prediction-variables
	 */
	protected void initPrediction() {
		if (predStatus == PRED_ON)
			disablePrediction();

		predCurrentTrackId = 0;
		predTempTrack = null;
		predTrackEmulation = null;
		predCurrentPoint = -1;
	}

	/**
	 * enable prediction over the given track, start track-emulation
	 * 
	 * @param gpx
	 */
	protected void enablePrediction(GPXTrack gpx) {
		Log.i(TAG, "enablePrediction");
		tempTimeInPrediction = System.currentTimeMillis();

		predTrackEmulation = null;
		predTrackEmulation = new TrackEmulation(gpx, EET.this,
				EET.this.onActivityResultHandler,
				EET.this.onPredictionEndHandler);
		predStatus = PRED_ON;
		predTrackEmulation.start();

		disableGPSForPrediction();
	}

	/**
	 * stop track-emulation, enable gps
	 */
	protected void disablePrediction() {
		if (tempTimeInPrediction > 0)
			timeInPrediction += (System.currentTimeMillis() - tempTimeInPrediction);
		tempTimeInPrediction = 0;
		Log.i(TAG, "disablePrediction(" + (timeInPrediction / 1000.0) + "s)");

		if (predTrackEmulation != null) {
			predTrackEmulation.stop();
			predTrackEmulation = null;
		}
		predStatus = PRED_OFF;

		enableGPS();
	}

	/*************** communication *****************/

	/**
	 * send IsTrackAvailableRequest-Message to the service
	 */
	public void sendIsTrackAvailable() {
		Location loc;
		if (lastLoc != null)
			loc = lastLoc;
		else
			loc = new Location(LocationManager.GPS_PROVIDER);

		ClientLocation cl = new ClientLocation(loc.getLatitude(),
				loc.getLongitude(), loc.getAccuracy(), lastActivity,
				loc.getSpeed(), System.currentTimeMillis());

		mobilisProxy.IsTrackAvailable(serviceJID, cl, predCurrentTrackId,
				isTrackAvailableResponse);
	}

	/**
	 * send a special IsTrackAvailableRequest-Message to rate the current Track
	 * in the Database
	 */
	public void sendRateTrack() {
		ClientLocation cl = new ClientLocation(-Double.MAX_VALUE,
				-Double.MAX_VALUE, 0, "", 0, 0);

		try {
			mobilisProxy.IsTrackAvailable(serviceJID, cl,
					Integer.parseInt(predTempTrack.trkName),
					isTrackAvailableResponse);
		} catch (Exception e) {
			Log.e(TAG, "sendRateTrack", e);
		}
	}

	/**************** Listener **********************/

	/**
	 * LocationListener for GPS in Prediction-Mode check, whether the prediction
	 * is correct
	 */
	private LocationListener secondLocListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location arg0) {
			Log.i(TAG, "secondLocation received N" + arg0.getLatitude() + " E"
					+ arg0.getLongitude());

			if (predStatus == PRED_ON) {
				int pos = predTempTrack.getTrackPosition(arg0.getLatitude(),
						arg0.getLongitude());
				if (pos == -1) {
					predLastClientStatus = "secondLocListerner: Prediction failed, Position not in Track";
					Log.i(TAG, predLastClientStatus);
					disablePrediction();
				} else {
					int predPos = predTrackEmulation.getCurrentPosition();

					if (pos < predPos) {
						if (!emulationMode)
							predTrackEmulation.adaptSpeed(false);
						predLastClientStatus = "secondLocListerner: Prediction corrected (decrease)";
					} else if (pos > predPos) {
						if (!emulationMode)
							predTrackEmulation.adaptSpeed(true);
						predLastClientStatus = "secondLocListerner: Prediction corrected (increase)";
					}
					Log.i(TAG, predLastClientStatus);
				}
			} else {
				if(!emulationMode) countGPSUpdates++;
			}
		}

		@Override
		public void onProviderDisabled(String arg0) {
		}

		@Override
		public void onProviderEnabled(String arg0) {
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		}

	};

	/**
	 * this LocationListener get the gps-updates, when the app is in emulation-mode
	 * read the battery-level and count the countGPSUpdates-variable for energy-tests
	 */
	private LocationListener testLocListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location arg0) {
			String bat = EET.this.getBatteryLevel() + "%";

			Log.d(TAG, "testLocListener:onLocationChanged (" + bat + ")");
			Toast.makeText(ctx.getApplicationContext(), "tLL",
					Toast.LENGTH_SHORT).show();
			countGPSUpdates += 1;
		}

		@Override
		public void onProviderDisabled(String arg0) {
		}

		@Override
		public void onProviderEnabled(String arg0) {
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		}
	};

	/**
	 * intern, first LocationListener for prediction or GPS
	 * send the Position to the onLocationChangedHandler and the externListener
	 */
	@Override
	public void onLocationChanged(Location arg0) {
		if (externListener != null)
			externListener.onLocationChanged(arg0);

		Message msg = new Message();
		msg.what = 1;
		msg.obj = arg0;
		onLocationChangedHandler.sendMessage(msg);

		Log.v(TAG,
				"onLocationChanged: N" + arg0.getLatitude() + " E"
						+ arg0.getLongitude());

	}

	@Override
	public void onProviderDisabled(String arg0) {
		if (externListener != null)
			externListener.onProviderDisabled(arg0);

	}

	@Override
	public void onProviderEnabled(String arg0) {
		if (externListener != null)
			externListener.onProviderEnabled(arg0);

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		if (externListener != null)
			externListener.onStatusChanged(arg0, arg1, arg2);

	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.w(TAG, "OnConnectionFailedListener.onConnectionFailed(), Code: "
				+ connectionResult.getErrorCode());
	}

	/**
	 * called, if the connection to the ActivityRecognitionClient was established
	 * add or remove the activity-updates
	 */
	@Override
	public void onConnected(Bundle arg0) {
		Log.v(TAG, "ConnectionCallbacks.onConnected(), act-recog "
				+ (arEnabled ? "disabled" : "enabled"));

		if (arEnabled) {
			arEnabled = false;
			mActivityRecognitionClient
					.removeActivityUpdates(mActivityRecognitionPendingIntent);
		} else {
			arEnabled = true;
			mActivityRecognitionClient.requestActivityUpdates(this.gpsIntervall,
					mActivityRecognitionPendingIntent);
		}

		arInProgress = false;
		mActivityRecognitionClient.disconnect();

	}

	@Override
	public void onDisconnected() {
	}

	/******************* Getter/Setter *********************/

	public Boolean isTracking() {
		return isTracking;
	}

	public void setServiceJID(String serviceJID) {
		this.serviceJID = serviceJID;
	}

	public int getPredStatus() {
		return this.predStatus;
	}

	public GPXTrack getPredTempTrack() {
		return this.predTempTrack;
	}

	public String getEETStatus() {
		String status = "";
		if (lastLoc != null)
			status = "N" + lastLoc.getLatitude() + " E"
					+ lastLoc.getLongitude();
		status += " countGPSUpdates: " + countGPSUpdates + " (GPS: "
				+ (timeInGPS / 1000.0) + "s, Pred: "
				+ (timeInPrediction / 1000.0) + "s)" + "\nServer: "
				+ predLastServerStatus + "\nClient: " + predLastClientStatus;

		return status;
	}

	/**
	 * preparation for the dynamic gps-interval, based on the recognized activity
	 * @return the suggested gps-interval for the last activity
	 */
	private int getGPSIntervall() {
		if (this.lastActivity == "on_bicycle")
			return 5000;
		if (this.lastActivity == "in_vehicle")
			return 3000;
		if (this.lastActivity == "on_foot")
			return 10000;
		if (this.lastActivity == "still")
			return 30000;
		if (this.lastActivity == "unknown")
			return 5000;
		if (this.lastActivity == "tilting")
			return 5000;
		else
			return 5000;
	}

	/**
	 * get the current battery-level
	 * @return
	 */
	private float getBatteryLevel() {
		Intent batteryIntent = this.ctx.registerReceiver(null,
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		// Error checking that probably isn't needed but I added just in case.
		if (level == -1 || scale == -1) {
			return 50.0f;
		}

		return ((float) level / (float) scale) * 100.0f;
	}
}
