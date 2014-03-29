package localhost.sling;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.HttpClient;

import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.CloseableHttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.entity.ContentType;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntityBuilder;
import ch.boye.httpclientandroidlib.entity.mime.content.ContentBody;
import ch.boye.httpclientandroidlib.entity.mime.content.FileBody;
import ch.boye.httpclientandroidlib.entity.mime.content.InputStreamBody;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class PlantActivity extends Activity implements OnEditorActionListener,
		OnClickListener, LocationListener, Runnable {
	TextView mTitle, mPassword, mQuestion;
	Spinner mExpiration;
	LocationManager mLocationManager;
	double mLat, mLon;
	Button mPlant;
	CloseableHttpClient mHttpClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		if (!Intent.ACTION_SEND.equals(intent.getAction()))
			finish();
		setContentView(R.layout.activity_plant);
		mTitle = (TextView) findViewById(R.id.title);
		mPassword = (TextView) findViewById(R.id.password);
		mQuestion = (TextView) findViewById(R.id.question);
		mExpiration = (Spinner) findViewById(R.id.expiration);
		mPassword.setOnEditorActionListener(this);
		(mPlant = (Button) findViewById(R.id.plant)).setOnClickListener(this);
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Log.v("SlingApp", "handling intent " + getIntent());
		mHttpClient = HttpClientBuilder.create().build();
	}

	@Override
	protected void onResume() {
		super.onResume();
		ensureGps();
	}

	private void ensureGps() {
		MainActivity.ensureGps(this, mLocationManager, this);
	}

	@Override
	public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
		mQuestion.setVisibility(arg0.getText().length() == 0 ? View.GONE
				: View.VISIBLE);
		return true;
	}

	@Override
	public void onClick(View arg0) {
		final InputStream is;
		final String type;
		final Intent intent = getIntent();
		Uri uri = intent.getData();
		if (uri == null)
			uri = (Uri) intent.getExtras().getParcelable(Intent.EXTRA_STREAM);
		if (uri == null) {
			Log.v("SlingApp", "could not open");
			return;
		}
		try {
			final ContentResolver contentResolver = getContentResolver();
			is = contentResolver.openInputStream(uri);
			type = contentResolver.getType(uri);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			finish();
			return;
		}
		final HttpPost httppost;
		try {
			httppost = new HttpPost(new URI(MainActivity.BASEURL + "plant"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
			finish();
			return;
		}
		final MultipartEntityBuilder builder = MultipartEntityBuilder
				.create()
				.addTextBody("latitude", String.valueOf(mLat))
				.addTextBody("longitude", String.valueOf(mLon))
				.addTextBody("title",
						String.valueOf(mTitle.getText().toString()))
				.addTextBody(
						"expiration",
						String.valueOf(getResources().getIntArray(
								R.array.expiration_secs_array)[mExpiration
								.getSelectedItemPosition()]));
		if (mPassword.getText().length() != 0) {
			builder.addTextBody("password", mPassword.getText().toString());
			if (mQuestion.getText().length() != 0)
				builder.addTextBody("question", mQuestion.getText().toString());
		}
		final String name = uri
				.getLastPathSegment();
		httppost.setEntity(builder.addPart(
				"seed",
				new InputStreamBody(is, ContentType.create(type!=null?type:"application/octet-stream"), name==null?uri.toString():name)).build());
		Log.v("SlingApp", "created post" + httppost);
		new Thread() {
			@Override
			public void run() {
				CloseableHttpResponse chr = null;
				try {
					chr = mHttpClient.execute(httppost);
					if (chr.getStatusLine().getStatusCode() / 100 == 2)
						runOnUiThread(PlantActivity.this);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				} finally {
					if (chr != null)
						try {
							chr.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
				}
			}
		}.start();
	}

	@Override
	public void onLocationChanged(Location location) {
		mLat = location.getLatitude();
		mLon = location.getLongitude();
		mPlant.setEnabled(true);
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
	public void run() {
		Toast.makeText(this, "success", Toast.LENGTH_LONG).show();
		finish();
	}
}
