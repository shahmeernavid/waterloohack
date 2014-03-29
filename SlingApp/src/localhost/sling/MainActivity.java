package localhost.sling;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener,
		Runnable, OnClickListener, DialogInterface.OnClickListener {
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
		// Log.v("SlingApp", "got json " + arr);
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
					convertView = getLayoutInflater().inflate(
							R.layout.seed_view, null);// XXX parent
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
		final JSONObject obj = (JSONObject) arg0.getTag();
		final String key = obj.optString("key"), type = obj
				.optString("content_type"), hash = obj.opt("password") instanceof String ? obj
				.optString("password") : null;
		if (hash == null) {
			fetchSeed(type, key, null);
			return;
		}
		String question = obj.opt("question") instanceof String ? obj
				.optString("question") : null;
		final View prompt = getLayoutInflater().inflate(
				R.layout.password_prompt, null);
		((TextView) prompt.findViewById(R.id.prompt))
				.setText(question == null ? "password" : question);
		final TextView password = (TextView) prompt.findViewById(R.id.password);
		final AlertDialog dialog = new AlertDialog.Builder(this)
				.setView(prompt)
				.setTitle(question == null ? "password" : "question")
				.setPositiveButton("ok", null)
				.setNegativeButton("cancel", this).create();
		dialog.show();
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						final String passwordText = password.getText()
								.toString();
						final MessageDigest md;
						try {
							md = MessageDigest.getInstance("SHA-1");
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
							finish();
							return;
						}
						md.update("NSGbUvrdqwqHYFAQ".getBytes());
						final byte[] digest = md.digest(passwordText.getBytes());
						for (int i = 0; i < digest.length; i++)
							if (Integer.parseInt(
									hash.substring(2 * i, 2 * (i + 1)), 16) != (0xff & digest[i])) {
								Toast.makeText(MainActivity.this,
										"bad password", Toast.LENGTH_SHORT)
										.show();
								return;
							}
						fetchSeed(type, key, passwordText);
						dialog.dismiss();
					}
				});
	}

	String encodeURIComponent(String component) {
		try {
			return URLEncoder.encode(component, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			finish();
			return "";
		}
	}

	private void fetchSeed(final String type, String key, String password) {
		final String objPath = mBaseurl
				+ "get?key="
				+ encodeURIComponent(key)
				+ (password == null ? "" : "&password="
						+ encodeURIComponent(password));
		Log.v("SlingApp", "objPath = " + objPath);

		String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
		final String basename = ext == null ? key : key + "." + ext;
		new Thread() {
			public void run() {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet(objPath);
				final HttpResponse response;
				try {
					response = httpclient.execute(httpget);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					final File outputFile = new File(getFilesDir(), basename);
					InputStream inputStream = null;
					FileOutputStream fos = null;
					try {
						try {
							fos = new FileOutputStream(outputFile);// openFileOutput(basename,
																	// MODE_WORLD_READABLE);//XXX
						} catch (FileNotFoundException e) {
							e.printStackTrace();
							return;
						}
						inputStream = entity.getContent();
						byte[] buf = new byte[1024 * 8];
						int extent;
						while ((extent = inputStream.read(buf)) != -1)
							fos.write(buf, 0, extent);
					} catch (IllegalStateException e) {
						e.printStackTrace();
						return;
					} catch (IOException e) {
						e.printStackTrace();
						return;
					} finally {
						try {
							if (inputStream != null)
								inputStream.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						try {
							if (fos != null)
								fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					Log.v("SlingApp", "saved to " + outputFile);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							try {
								startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(outputFile), type));
							} catch (Exception e) {
								e.printStackTrace();
								Toast.makeText(MainActivity.this,
										"cannot open", Toast.LENGTH_LONG)
										.show();
							}
						}
					});
				}
			};
		}.start();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		dialog.dismiss();
	}

}