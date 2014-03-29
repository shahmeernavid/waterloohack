package localhost.sling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener,
		Runnable, OnClickListener {
	ListView mSeedList;
	LocationManager mLocationManager;
	URL mBaseurl;
	double mLat, mLon;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSeedList = (ListView) findViewById(R.id.seed_list);
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		try {
			mBaseurl = new URL("http://linux024.student.cs.uwaterloo.ca:40080/");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			finish();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		ensureGps();
	}

	private void ensureGps() {
		Location loc = null;
		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 5000, 10, this);
			loc = mLocationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (loc != null)
				onLocationChanged(loc);// XXX
		} else
			new AlertDialog.Builder(this)
					.setMessage("gps?")
					.setCancelable(false)
					.setPositiveButton("ok",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									startActivity(new Intent(
											android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
								}
							})
					.setNegativeButton("cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									dialog.cancel();
									Toast.makeText(MainActivity.this,
											"maybe next time",
											Toast.LENGTH_LONG).show();
								}
							}).create().show();
		if (loc == null
				&& mLocationManager
						.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 5000, 10, this);
			loc = mLocationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (loc != null)
				onLocationChanged(loc);
			else
				Log.v("SlingApp", "no gps fix");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu); // XXX default
		return true;
	}

	@Override
	public void onLocationChanged(Location location) {
		mLat = location.getLatitude();
		mLon = location.getLongitude();
		Log.d("SlingApp", "location changed to " + mLat + "," + mLon);
		new Thread(this).start();
	}

	@Override
	public void run() {
		URL u;
		try {
			u = new URL(mBaseurl, "list?latitude=" + mLat + "&longitude="
					+ mLon);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			finish();
			return;
		}
		Log.v("SlingApp", "requesting " + u);
		Scanner sc;
		try {
			sc = new Scanner(u.openStream());
		} catch (IOException e) {
			e.printStackTrace(); // XXX
			return;
		}
		String data;
		try {
			data = sc.useDelimiter("\\A").next();
		} finally {
			sc.close();
		}
		final JSONArray arr;
		try {
			arr = new JSONArray(data);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		Log.v("SlingApp", "got json " + arr);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				rebindList(arr);
			}
		});
	}

	private void rebindList(final JSONArray arr) {
		mSeedList.setAdapter(new BaseAdapter() {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null)
					convertView = LayoutInflater.from(MainActivity.this)
							.inflate(R.layout.seed_view, null);// XXX parent
																// doesn't work
				((ImageView) convertView.findViewById(R.id.content_type))
						.setImageResource(R.drawable.ic_launcher);
				final JSONObject obj = arr.optJSONObject(position);
				((TextView) convertView.findViewById(R.id.title)).setText(obj
						.optString("title"));
				((TextView) convertView.findViewById(R.id.json)).setText(obj
						.toString());
				convertView.setOnClickListener(MainActivity.this);
				convertView.setTag(obj);
				return convertView;
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

			@Override
			public Object getItem(int position) {
				return arr.opt(position);
			}

			@Override
			public int getCount() {
				return arr.length();
			}
		});
	}

	@Override
	public void onProviderDisabled(String provider) {
		ensureGps();
	}

	@Override
	public void onProviderEnabled(String provider) {
		ensureGps();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View arg0) {
		final JSONObject obj=(JSONObject) arg0.getTag();
		
	}

}
