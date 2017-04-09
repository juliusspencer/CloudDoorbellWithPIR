package co.jsaltd.doorbell;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import co.jsaltd.doorbell.device.DoorbellCamera;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "BELL";
	private static final String PIR_INPUT = "BCM26";

	/**A Handler for running tasks in the background.*/
	private Handler mBackgroundHandler;

	/**An additional thread for running tasks that shouldn't block the UI.*/
	private HandlerThread mBackgroundThread;

	/** Camera capture device wrapper */
	private DoorbellCamera mCamera;

	private FirebaseDatabase mDatabase;

	private ImageView mImageView;
	private TextView mTextView;

	private Gpio mPirGpio;


	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// configure views
		mImageView = (ImageView) findViewById(R.id.imageview);
		/* Views */
		final Button button = (Button) findViewById(R.id.button);
		mTextView = (TextView) findViewById(R.id.status_textview);

		button.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				Log.d(TAG, "button pressed");
				mCamera.takePicture();
				try {
					Log.d(TAG, "Value " + mPirGpio.getValue());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		// database
		mDatabase = FirebaseDatabase.getInstance();

		// camera
		mCamera = DoorbellCamera.getInstance();
		mCamera.initializeCamera(this, mBackgroundHandler, mOnImageAvailableListener);

		startBackgroundThread();

		// IO
		PeripheralManagerService service = new PeripheralManagerService();

		try {
			mPirGpio = service.openGpio(PIR_INPUT);
			mPirGpio.setDirection(Gpio.DIRECTION_IN);
			mPirGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
			mPirGpio.registerGpioCallback(new GpioCallback() {
				@Override public boolean onGpioEdge(Gpio gpio) {
					try {
						Log.i(TAG, "GPIO changed, PIR movement: " + gpio.getValue());
						onMovementChange(gpio.getValue());
					} catch (IOException e) {
						e.printStackTrace();
					}
					// Return true to continue listening to events
					return true;
				}
			});
		} catch (IOException e) {
			Log.e(TAG, "Error on PeripheralIO API", e);
		}

	}

	@Override protected void onDestroy() {
		super.onDestroy();
		mBackgroundThread.quitSafely();
		mCamera.shutDown();

		if (mPirGpio != null) {
			// Close the Gpio pin
			Log.i(TAG, "Closing PIR GPIO pin");
			try {
				mPirGpio.close();
			} catch (IOException e) {
				Log.e(TAG, "Error on PeripheralIO API", e);
			} finally {
				mPirGpio = null;
			}
		}
	}

	/**
	 * Starts a background thread and its Handler.
	 */
	private void startBackgroundThread() {
		mBackgroundThread = new HandlerThread("InputThread");
		mBackgroundThread.start();
		mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
	}

	// Callback to receive captured camera image data
	private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
			new ImageReader.OnImageAvailableListener() {
				@Override public void onImageAvailable(ImageReader reader) {
					// Get the raw image bytes
					Image image = reader.acquireLatestImage();
					ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
					final byte[] imageBytes = new byte[imageBuf.remaining()];
					imageBuf.get(imageBytes);
					image.close();

					onPictureTaken(imageBytes);
				}
			};

	private void onPictureTaken(final byte[] imageBytes) {
		if (imageBytes != null) {
			// ...process the captured image...

			// remove all but last images
			mDatabase.getReference().child("logs").addListenerForSingleValueEvent(new ValueEventListener() {
				@Override public void onDataChange(DataSnapshot dataSnapshot) {
					long childrenCount = dataSnapshot.getChildrenCount();
					if (childrenCount == 0) return;

					mDatabase.getReference().child("logs").removeValue();

					// show the image in an ImageView
					Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

					// send it to the server to be dispatched
					final DatabaseReference log = mDatabase.getReference("logs").push();
					log.removeValue();
					Map<String, String> timestamp = ServerValue.TIMESTAMP;
					log.child("timestamp").setValue(timestamp);

					mImageView.setImageBitmap(bitmap);
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					byte[] byteArray = stream.toByteArray();

					// Save image data Base64 encoded
					String encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP | Base64.URL_SAFE);
					Task<Void> image = log.child("image").setValue(encoded);
					image.addOnCompleteListener(MainActivity.this, new OnCompleteListener<Void>() {
						@Override public void onComplete(@NonNull Task<Void> task) {
							Void result = task.getResult();
							Exception exception = task.getException();
							if (result != null) Log.e(TAG, "Log Result: " + result.toString());
							if (exception != null) Log.e(TAG, "Log Exception: " + exception.getMessage());
						}

					});

				}

				@Override public void onCancelled(DatabaseError databaseError) {}
			});
		}
	}

	/**
	 * Handles changes based on PIR detected movement
	 * @param moving the current state
	 */
	private void onMovementChange(boolean moving) {
		mTextView.setText(moving ? R.string.moving : R.string.still);
		if(moving) mCamera.takePicture();
	}

}

