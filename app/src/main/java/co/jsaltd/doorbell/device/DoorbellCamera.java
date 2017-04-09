package co.jsaltd.doorbell.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;

import java.util.Collections;

import co.jsaltd.doorbell.BuildConfig;

import static android.content.Context.CAMERA_SERVICE;

public class DoorbellCamera {

	private static final String TAG = "BELL";

	// Camera image parameters (device-specific)
	private static final int IMAGE_WIDTH = 640;
	private static final int IMAGE_HEIGHT = 480;
	private static final int MAX_IMAGES = 1;

	// Image result processor
	private ImageReader mImageReader;
	// Active camera device connection
	private CameraDevice mCameraDevice;
	// Active camera capture session
	private CameraCaptureSession mCaptureSession;

	// Lazy-loaded singleton, so only one instance of the camera is created.
	private DoorbellCamera() {
	}

	private static class InstanceHolder {
		private static DoorbellCamera mCamera = new DoorbellCamera();
	}

	public static DoorbellCamera getInstance() {
		return InstanceHolder.mCamera;
	}

	// Initialize a new camera device connection
	@SuppressLint("MissingPermission") public void initializeCamera(Context context, Handler backgroundHandler, ImageReader.OnImageAvailableListener imageListener) {

		// dump extra info for a debug build
		if(BuildConfig.DEBUG) dumpFormatInfo(context);

		// Discover the camera instance
		CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
		String[] camIds = {};
		try {
				camIds = manager.getCameraIdList();
		} catch (CameraAccessException e) {
			Log.d(TAG, "Cam access exception getting IDs", e);
		}
		if (camIds.length < 1) {
			Log.d(TAG, "No cameras found");
			return;
		}
		String id = camIds[0];

		// Initialize image processor
		mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, MAX_IMAGES);
		mImageReader.setOnImageAvailableListener(imageListener, backgroundHandler);

		// Open the camera resource
		try {
			manager.openCamera(id, mStateCallback, backgroundHandler);
		} catch (CameraAccessException cae) {
			Log.d(TAG, "Camera access exception", cae);
		}
	}

	// Callback handling devices state changes
	private final CameraDevice.StateCallback mStateCallback =
			new CameraDevice.StateCallback() {
				@Override public void onOpened(@NonNull CameraDevice cameraDevice) {
					mCameraDevice = cameraDevice;
				}

				@Override public void onDisconnected(@NonNull CameraDevice camera) {
					if(mCameraDevice != null) mCameraDevice.close();
				}

				@Override public void onError(@NonNull CameraDevice camera, int error) {
					mCameraDevice.close();
				}

				@Override public void onClosed(@NonNull CameraDevice camera) {
					super.onClosed(camera);
					mCameraDevice = null;
				}
			};

	// Close the camera resources
	public void shutDown() {
		if (mCameraDevice != null) {
			mCameraDevice.close();
		}
	}

	public void takePicture() {
		if (mCameraDevice == null) {
			Log.w(TAG, "Cannot capture image. Camera not initialized.");
			return;
		}

		// Here, we create a CameraCaptureSession for capturing still images.
		try {
			mCameraDevice.createCaptureSession(
					Collections.singletonList(mImageReader.getSurface()),
					mSessionCallback,
					null);
		} catch (CameraAccessException cae) {
			Log.d(TAG, "access exception while preparing pic", cae);
		}
	}

	// Callback handling session state changes
	private final CameraCaptureSession.StateCallback mSessionCallback =
			new CameraCaptureSession.StateCallback() {

				@Override public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
					// When the session is ready, we start capture.
					mCaptureSession = cameraCaptureSession;
					triggerImageCapture();
				}

				@Override public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
					Log.w(TAG, "Failed to configure camera");
				}
			};

	private void triggerImageCapture() {
		try {
			final CaptureRequest.Builder captureBuilder =
					mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			captureBuilder.addTarget(mImageReader.getSurface());
			captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

			mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null);
		} catch (CameraAccessException cae) {
			Log.d(TAG, "camera capture exception");
		}
	}

	// Callback handling capture progress events
	private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
				@Override public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
						@NonNull TotalCaptureResult result) {
					session.close();
					mCaptureSession = null;
					Log.d(TAG, "CaptureSession closed");
				}
			};

	/**
	 * Helpful debugging method:  Dump all supported camera formats to log.
	 * You don't need to run this for normal operation, but it's very
	 * helpful when porting this code to different hardware.
	 */
	private static void dumpFormatInfo(Context context) {
		CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
		String[] camIds = {};
		try {
			camIds = manager.getCameraIdList();
		} catch (CameraAccessException e) {
			Log.d(TAG, "Cam access exception getting IDs");
		}
		if (camIds.length < 1) Log.d(TAG, "No cameras found");

		String id = camIds[0];
		Log.d(TAG, "Using camera id " + id);
		try {
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
			StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			if(configs == null) return;
			for (int format : configs.getOutputFormats()) {
				Log.d(TAG, "Getting sizes for format: " + format);
				for (Size s : configs.getOutputSizes(format)) {
					Log.d(TAG, "\t" + s.toString());
				}
			}

			int[] effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
			if(effects == null) return;
			for (int effect : effects) {
				Log.d(TAG, "Effect available: " + effect);
			}
		} catch (CameraAccessException e) {
			Log.d(TAG, "Cam access exception getting characteristics.");
		}
	}

}
