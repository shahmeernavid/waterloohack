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
import java.sql.Timestamp;
import java.util.Date;
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener,
		Runnable, DialogInterface.OnClickListener {
	public static final URL BASEURL;
	static {
		URL u = null;
		try {
			u = new URL("http://linux024.student.cs.uwaterloo.ca:40080/");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			assert false;
		}
		BASEURL = u;
	}
	ListView mSeedList;
	LocationManager mLocationManager;
	double mLat, mLon;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (BASEURL == null)
			finish();
		setContentView(R.layout.activity_main);
		mSeedList = (ListView) findViewById(R.id.seed_list);
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		ensureGps();
	}

	private void ensureGps() {
		ensureGps(this, mLocationManager, this);
	}

	public static void ensureGps(final Context ctx,
			LocationManager mLocationManager, LocationListener ll) {
		Location loc = null;
		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 5000, 10, ll);
			loc = mLocationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (loc != null)
				ll.onLocationChanged(loc);// XXX
		} else
			new AlertDialog.Builder(ctx)
					.setMessage("gps?")
					.setCancelable(false)
					.setPositiveButton("ok",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									ctx.startActivity(new Intent(
											android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
								}
							})
					.setNegativeButton("cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									dialog.cancel();
									Toast.makeText(ctx, "maybe next time",
											Toast.LENGTH_LONG).show();
								}
							}).create().show();
		if (loc == null
				&& mLocationManager
						.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 5000, 10, ll);
			loc = mLocationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (loc != null)
				ll.onLocationChanged(loc);
			else
				Log.v("SlingApp", "no gps fix");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (true)
			return false;// XXX
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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
			u = new URL(BASEURL, "list?latitude=" + mLat + "&longitude=" + mLon);
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

	protected static long timedelta(String date, Date now) {
		return Timestamp.valueOf(date.replace('T', ' ')).getTime()
				- now.getTime() - now.getTimezoneOffset() * 60 * 1000;
	}

	private void rebindList(final JSONArray arr) {
		mSeedList.setAdapter(new BaseAdapter() {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null)
					convertView = getLayoutInflater().inflate(
							R.layout.seed_view, null);// XXX parent
				final JSONObject obj = arr.optJSONObject(position);
				int icon = R.drawable.file;
				String type = obj.optString("content_type");
				if (type.startsWith("image/"))
					icon = R.drawable.image;
				else if (type.startsWith("text/"))
					icon = R.drawable.message;
				((ImageView) convertView.findViewById(R.id.content_type))
						.setImageResource(icon);
				((ImageView) convertView.findViewById(R.id.authentication)).setVisibility(obj
						.opt("password") instanceof String ? View.VISIBLE
						: View.GONE);
				((TextView) convertView.findViewById(R.id.title)).setText(obj
						.optString("title"));
				Date now = new Date();
				((TextView) convertView.findViewById(R.id.age))
						.setText(humanizeMillis(-timedelta(
								obj.optString("created"), now)));
				((TextView) convertView.findViewById(R.id.expiration))
						.setText(humanizeMillis(timedelta(
								obj.optString("expiration"), now)));
				((TextView) convertView.findViewById(R.id.distance))
				.setText(String.valueOf((int)Math.ceil(obj.optDouble("distance"))));
				((TextView) convertView.findViewById(R.id.hits))
				.setText(String.valueOf(obj.optInt("hits")));
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
		mSeedList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View arg0,
					int position, long id) {
				// arg0.setSelected(true);
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
				final TextView password = (TextView) prompt
						.findViewById(R.id.password);
				final AlertDialog dialog = new AlertDialog.Builder(
						MainActivity.this).setView(prompt)
						.setTitle(question == null ? "password" : "question")
						.setPositiveButton("ok", null)
						.setNegativeButton("cancel", MainActivity.this)
						.create();
				dialog.show();
				dialog.getButton(AlertDialog.BUTTON_POSITIVE)
						.setOnClickListener(new OnClickListener() {
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
								final byte[] digest = md.digest(passwordText
										.getBytes());
								for (int i = 0; i < digest.length; i++)
									if (Integer.parseInt(
											hash.substring(2 * i, 2 * (i + 1)),
											16) != (0xff & digest[i])) {
										Toast.makeText(MainActivity.this,
												"bad password",
												Toast.LENGTH_SHORT).show();
										return;
									}
								fetchSeed(type, key, passwordText);
								dialog.dismiss();
							}
						});
			}
		});
	}

	protected static CharSequence humanizeMillis(long l) {
		l /= 1000 * 60;
		if (l <= 0)
			return "< 1 min";
		int[] rates = { 60, 24, 7 };
		String[] names = { "hr", "day", "week" };
		String unit = "min";
		for (int i = 0; i < rates.length && l >= rates[i]; ++i) {
			l /= rates[i];
			unit = names[i];
		}
		return l + " " + unit + (l == 1 ? "" : "s");
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
		final String objPath = BASEURL
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
							fos = openFileOutput(basename, MODE_WORLD_READABLE);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
							return;
						}
						inputStream = entity.getContent();
						copyStream(inputStream, fos);
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
								final Uri uri = Uri.fromFile(outputFile);
								startActivity(new Intent(Intent.ACTION_VIEW)
										.setDataAndType(uri, type).putExtra(
												Intent.EXTRA_STREAM, uri));
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

	public static void copyStream(InputStream inputStream, OutputStream fos)
			throws IOException {
		byte[] buf = new byte[1024 * 8];
		int extent;
		while ((extent = inputStream.read(buf)) != -1)
			fos.write(buf, 0, extent);
	}

}
