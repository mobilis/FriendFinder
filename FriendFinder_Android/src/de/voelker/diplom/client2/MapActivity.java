package de.voelker.diplom.client2;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.tudresden.inf.rn.mobilis.friendfinder.clientstub.ClientData;
import de.voelker.diplom.client2.service.ServiceConnector;

public class MapActivity extends FragmentActivity implements
		OnMapClickListener, OnMapLongClickListener, OnCameraChangeListener,
		OnMarkerClickListener, OnInfoWindowClickListener {

	public static final String TAG = "MapActivity";

	private GoogleMap mMap;
	private MapView mMapView;
	private Toast toast;

	private HashMap<String, Marker> markers = new HashMap<String, Marker>();

	private ServiceConnector mServiceConnector;

	/********************* Handler **********************/

	private Handler onServiceBoundHandler = new Handler() {
		public void handleMessage(Message msg) {
			mServiceConnector.getService().registerMUCListener(
					MapActivity.this, onNewMUCMessageHandler);

			updateMarkerList();
		}

	};

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
		
		Log.i(TAG, "Play Status: " + GooglePlayServicesUtil.isGooglePlayServicesAvailable(this));

		toast = Toast.makeText(this, "null", Toast.LENGTH_SHORT);

		mServiceConnector = new ServiceConnector(this);
		mServiceConnector.bindService(onServiceBoundHandler);

		setUpMapIfNeeded();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		}
		return super.onOptionsItemSelected(item);
	}

	/*************** class-specific functions *******************/

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

	private void setUpMap() {
		mMap.setMyLocationEnabled(true);
		mMap.setOnMapClickListener(this);
		mMap.setOnMapLongClickListener(this);
		mMap.setOnCameraChangeListener(this);

		mMap.setOnMarkerClickListener(this);
		mMap.setOnInfoWindowClickListener(this);
	}

	@Override
	public void onMapClick(LatLng point) {
		toast.setText("tapped, point=" + point);
		toast.show();
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
		toast.setText("camera, " + position.toString());
		toast.show();
	}

	@Override
	public boolean onMarkerClick(final Marker marker) {
		// registerForContextMenu(mMapView);

		// We return false to indicate that we have not consumed the event and
		// that we wish
		// for the default behavior to occur (which is for the camera to move
		// such that the
		// marker is centered and for the marker's info window to open, if it
		// has one).
		return false;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		toast.setText(marker.getTitle());
		toast.show();
	}
}
