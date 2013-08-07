package de.tudresden.inf.rn.mobilis.friendfinder;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import de.tudresden.inf.rn.mobilis.eet.GPXTrack;
import de.tudresden.inf.rn.mobilis.eet.ILocationProxy;
import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientData;
import de.tudresden.inf.rn.mobilis.friendfinder.service.ServiceConnector;

/**
 * MapActivity with GoogleMaps-Fragment
 * 
 */
public class MapActivity extends FragmentActivity implements
		OnMapClickListener, OnMapLongClickListener, OnCameraChangeListener,
		OnMarkerClickListener, OnInfoWindowClickListener {

	public static final String TAG = "MapActivity";

	/**
	 * show the status of the tracking-class in a gray-box at the bottom of the
	 * map
	 */
	private final boolean showEETStatus = true;

	private GoogleMap mMap;
	private Toast toast;

	private TextView eet_status;

	/**
	 * save the markers for the chat-members
	 */
	private HashMap<String, Marker> markers = new HashMap<String, Marker>();

	private ServiceConnector mServiceConnector;

	/********************* Handler **********************/

	/**
	 * called, if the BackgroundService bound successfully
	 */
	private Handler onServiceBoundHandler = new Handler() {
		public void handleMessage(Message msg) {
			mServiceConnector.getService().registerMUCListener(
					MapActivity.this, onNewMUCMessageHandler);

			setUpMapIfNeeded();
			updateMarkerList();
		}

	};

	/**
	 * called, if a new muc-message was received
	 * update the marker-list
	 */
	private Handler onNewMUCMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			// Display a toast on top of the screen
			String text = mServiceConnector.getService().parseXMLMUCMessage(
					msg.obj.toString(), true);
			updateMarkerList();
			if (text.length() > 0) {
				toast = Toast.makeText(MapActivity.this, text,
						Toast.LENGTH_LONG);
				toast.show();
			}
		}
	};

	/****************** activity-methods **********************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		eet_status = (TextView) findViewById(R.id.map_eet_status);

		toast = Toast.makeText(this, "null", Toast.LENGTH_SHORT);

		mServiceConnector = new ServiceConnector(this);
		mServiceConnector.bindService(onServiceBoundHandler);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	/*************** class-specific functions *******************/

	/**
	 * update the markers and the prediction-status
	 */
	public void updateMarkerList() {

		if (mMap != null) {
			mMap.clear();
			markers.clear();

			Map<String, ClientData> cdl = mServiceConnector.getService()
					.getClientDataList();
			for (ClientData cd : cdl.values()) {
				String color = cd.getColor();
				int red = (int) (Integer.parseInt(color.subSequence(1, 3)
						.toString(), 16) / 255.0f) * 100;
				int green = (int) (Integer.parseInt(color.subSequence(3, 5)
						.toString(), 16) / 255.0f) * 100;
				int blue = (int) (Integer.parseInt(color.subSequence(5, 7)
						.toString(), 16) / 255.0f) * 100;

				float hsv[] = new float[3];
				Color.RGBToHSV(red, green, blue, hsv);

				Marker m = mMap.addMarker(new MarkerOptions()
						.position(
								new LatLng(cd.getClientLocation().getLat(), cd
										.getClientLocation().getLng()))
						.title(cd.getName())
						.icon(BitmapDescriptorFactory.defaultMarker(hsv[0])));
				markers.put(cd.getJid(), m);
			}
		}

		if (this.showEETStatus)
			updateEETStatus();
	}

	/**
	 * update prediction-status and the polyline for the prediction-track
	 */
	private void updateEETStatus() {
		ILocationProxy eet = mServiceConnector.getService().getLocationProxy();
		eet_status.setText(eet.getEETStatus());

		if (eet.getPredStatus() == ILocationProxy.PRED_ON) {
			PolylineOptions options = new PolylineOptions().width(5).color(
					Color.RED);
			for (GPXTrack.Trkpt pt : eet.getPredTempTrack().getTrackPoints()) {
				options.add(new LatLng(pt.lat, pt.lon));
			}
			mMap.addPolyline(options);
		}
	}

	/***************** map functions *********************/

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			// Try to obtain the map from the MapFragment.
			// mMap = ((MapFragment)
			// getFragmentManager().findFragmentById(R.id.map)).getMap();
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				setUpMap();
			}
		}
	}

	/** set up map */
	private void setUpMap() {
		mMap.setMyLocationEnabled(true);
		mMap.setOnMapClickListener(this);
		mMap.setOnMapLongClickListener(this);
		mMap.setOnCameraChangeListener(this);

		mMap.setOnMarkerClickListener(this);
		mMap.setOnInfoWindowClickListener(this);

		Location lastLoc = mServiceConnector.getService().getLocationProxy()
				.getLastLocation();
		if (lastLoc != null)
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
					lastLoc.getLatitude(), lastLoc.getLongitude()), 15));
		else
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
					51.0456, 13.7366), 12));
	}

	@Override
	public void onMapClick(LatLng point) {
	}

	@Override
	public void onMapLongClick(LatLng point) {
		toast.setText("long pressed, point=" + point);
		toast.show();
		mMap.addMarker(new MarkerOptions().position(point).draggable(false)
				.visible(true).title("new marker"));
	}

	@Override
	public void onCameraChange(final CameraPosition position) {
	}

	@Override
	public boolean onMarkerClick(final Marker marker) {
		return false;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		toast.setText(marker.getTitle());
		toast.show();
	}
}
