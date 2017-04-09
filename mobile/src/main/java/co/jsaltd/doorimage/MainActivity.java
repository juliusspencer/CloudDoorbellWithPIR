package co.jsaltd.doorimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

	private ImageView mImageView;
	private TextView mTimestampTextView;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mImageView = (ImageView) findViewById(R.id.imageview);
		mTimestampTextView = (TextView) findViewById(R.id.timestamp);

		// Reference for doorbell events from embedded device
		DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference().child("logs");
		databaseRef.addChildEventListener(new ChildEventListener() {
			@Override public void onChildAdded(DataSnapshot dataSnapshot, String s) {
			}

			@Override public void onChildChanged(DataSnapshot dataSnapshot, String s) {
				if(BuildConfig.DEBUG) Log.i("DOOR", "New log");
				DoorbellEntry doorbellEntry = dataSnapshot.getValue(DoorbellEntry.class);
				if (doorbellEntry.getImage() != null) {
					if(BuildConfig.DEBUG) Log.i("DOOR", "Decoding image…");
					// Decode image data encoded by the Cloud Vision library
					byte[] imageBytes = Base64.decode(doorbellEntry.getImage(), Base64.NO_WRAP | Base64.URL_SAFE);
					Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
					if (bitmap != null) {
						mImageView.setImageBitmap(bitmap);
						mTimestampTextView.setText(
								new SimpleDateFormat("yyyy MM dd - HH:mm:ss", Locale.getDefault())
								.format(new Date(doorbellEntry
						.getTimestamp())));
					} else {
						Drawable placeholder = ContextCompat.getDrawable(MainActivity.this, R.mipmap.ic_launcher);
						mImageView.setImageDrawable(placeholder);
					}
				} else {
					if(BuildConfig.DEBUG) Log.i("DOOR", "No image!");
				}
			}

			@Override public void onChildRemoved(DataSnapshot dataSnapshot) {}

			@Override public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

			@Override public void onCancelled(DatabaseError databaseError) {}
		});
	}

	@Override protected void onResume() {
		super.onResume();
		hideSystemUI();
	}

	@Override protected void onPause() {
		super.onPause();
		showSystemUI();
	}

	// This snippet hides the system bars.
	private void hideSystemUI() {
		// Set the IMMERSIVE flag.
		// Set the content to appear under the system bars so that the content
		// doesn't resize when the system bars hide and show.
		mImageView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
						| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
						| View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	// This snippet shows the system bars. It does this by removing all the flags
	// except for the ones that make the content appear under the system bars.
	private void showSystemUI() {
		mImageView
		.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
	}
}
