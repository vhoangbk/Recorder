package com.fiptin.recorder;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener,
		OnTouchListener, SurfaceHolder.Callback {

	private ImageView imgGrid, imgClose, imgNext;

	private Thread tProgress;
	private ProgressBar progressBar;

	private boolean isGridShow = false;
	private int currentCamera = Camera.CameraInfo.CAMERA_FACING_BACK;
	private boolean isFlashOn = false;

	private ImageView imgFrontCamera, imgFlash, imgShowGrid, imgGhost,
			imgRecord;

	private Camera mCamera;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;

	public String TAG = "MainActivity";

	private File videoFile;
	private MediaRecorder videoRecorder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		imgClose = (ImageView) findViewById(R.id.img_close);
		imgNext = (ImageView) findViewById(R.id.img_next);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
		imgFrontCamera = (ImageView) findViewById(R.id.img_front_camera);
		imgShowGrid = (ImageView) findViewById(R.id.img_show_grid);
		imgRecord = (ImageView) findViewById(R.id.img_record);
		imgGhost = (ImageView) findViewById(R.id.img_ghost);
		imgFlash = (ImageView) findViewById(R.id.img_flash);
		imgGrid = (ImageView) findViewById(R.id.img_grid);
		progressBar = (ProgressBar) findViewById(R.id.progress_time);

		if (Utils.checkCameraHardware(MainActivity.this)) {
			mCamera = Utils.getCameraInstance(MainActivity.this);

		} else {
			Toast.makeText(MainActivity.this, "No camera on this device",
					Toast.LENGTH_SHORT).show();
		}

		if (mCamera != null) {
			imgNext.setOnClickListener(this);
			imgFrontCamera.setOnClickListener(this);
			imgShowGrid.setOnClickListener(this);
			imgGhost.setOnClickListener(this);
			imgFlash.setOnClickListener(this);
			imgRecord.setOnTouchListener(this);
		}

		imgClose.setOnClickListener(this);

		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			imgRecord.setImageResource(R.drawable.record_active);
			startRecord();
			break;
		case MotionEvent.ACTION_UP:
			imgRecord.setImageResource(R.drawable.record);
			stopRecord();
			break;
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.img_show_grid:
			isGridShow = !isGridShow;
			if (isGridShow) {
				imgGrid.setVisibility(View.VISIBLE);
				imgShowGrid.setImageResource(R.drawable.ico_grid_active);
			} else {
				imgGrid.setVisibility(View.GONE);
				imgShowGrid.setImageResource(R.drawable.ico_grid);
			}
			break;
		case R.id.img_close:
			finish();
			break;
		case R.id.img_flash:
			if (currentCamera != Camera.CameraInfo.CAMERA_FACING_FRONT) {
				isFlashOn = !isFlashOn;
				Utils.turnOnFlash(MainActivity.this, mCamera, isFlashOn,
						currentCamera);
				if (isFlashOn) {
					imgFlash.setImageResource(R.drawable.ico_flash_active);
				} else {
					imgFlash.setImageResource(R.drawable.ico_flash);
				}
			}

			break;

		case R.id.img_front_camera:
			switchCamera();
			break;

		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int w, int h) {
		if (mHolder.getSurface() == null) {
			return;
		}

		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// --------------------

		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();

		} catch (Exception e) {
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}

		Parameters params = mCamera.getParameters();
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		if (sizes != null) {
			Camera.Size size = Utils.getOptimalPreviewSize(MainActivity.this,
					sizes, 1);
			params.setPreviewSize(size.width, size.height);
			mCamera.setParameters(params);
		}

		for (Camera.Size s : sizes) {
			Log.d("camera_size: ", "support " + s.width + "x" + s.height);
		}
		Log.d("camera_size: ",
				params.getPreviewSize().width + "X"
						+ params.getPreviewSize().height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		try {
			mCamera.setPreviewDisplay(mHolder);
			Utils.setCameraDisplayOrientation(MainActivity.this, currentCamera,
					mCamera);
			mCamera.startPreview();
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
			mHolder = null;
		}
	}

	@SuppressLint("NewApi")
	public void switchCamera() {
		mCamera.stopPreview();
		mCamera.release();
		if (currentCamera == Camera.CameraInfo.CAMERA_FACING_BACK) {
			isFlashOn = false;
			currentCamera = Camera.CameraInfo.CAMERA_FACING_FRONT;
			imgFrontCamera.setImageResource(R.drawable.ico_switch_cam_active);
			imgFlash.setImageResource(R.drawable.ico_flash);
		} else {
			currentCamera = Camera.CameraInfo.CAMERA_FACING_BACK;
			imgFrontCamera.setImageResource(R.drawable.ico_switch_cam);
		}

		mCamera = Camera.open(currentCamera);

		Utils.setCameraDisplayOrientation(MainActivity.this, currentCamera,
				mCamera);
		try {
			mCamera.setPreviewDisplay(mHolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mCamera.startPreview();
	}

	public void startRecord() {
		if (videoRecorder == null) {
			videoRecorder = new MediaRecorder();
		}

		initRecorder();
	}

	/**
	 * Initialize video recorder to record video
	 */
	@SuppressLint("NewApi")
	private void initRecorder() {
		try {
			String path = Environment.getExternalStorageDirectory() + "/test";
			File dir = new File(path);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			mCamera.stopPreview();
			mCamera.unlock();
			Calendar calendar = Calendar.getInstance();
//			filePath = path + "/" + calendar.getTime().toString() + ".mp4";
			videoFile = new File(dir, calendar.getTime().toString() + ".mp4");
			videoRecorder.setCamera(mCamera);

			// Step 2: Set sources
			videoRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			videoRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

			// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
			videoRecorder.setProfile(CamcorderProfile
					.get(CamcorderProfile.QUALITY_HIGH));

			// Step 4: Set output file
			videoRecorder.setOutputFile(videoFile.getAbsolutePath());
			// Step 5: Set the preview output
			videoRecorder.setPreviewDisplay(mSurfaceView.getHolder()
					.getSurface());

			// Utils.setCameraDisplayOrientation(MainActivity.this,
			// currentCamera, mCamera);

			// Step 6: Prepare configured MediaRecorder
			videoRecorder.setMaxDuration(15 * 1000);
			videoRecorder.setOnInfoListener(new OnInfoListener() {

				@Override
				public void onInfo(MediaRecorder mr, int what, int extra) {
					if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
						mCamera.stopPreview();
						releaseMediaRecorder();
						Toast.makeText(MainActivity.this,
								"Stop video record for too duration",
								Toast.LENGTH_SHORT).show();
						imgRecord.setImageResource(R.drawable.record);
					}
				}
			});
			videoRecorder.prepare();
			videoRecorder.start();
			Toast.makeText(MainActivity.this, "Starting record...",
					Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Log.e("Error Stating CuXtom Camera", e.getMessage());
		}
	}

	public void releaseMediaRecorder() {
		if (videoRecorder != null) {
			videoRecorder.reset(); // clear recorder configuration
			videoRecorder.release(); // release the recorder object
			videoRecorder = null;
		}
	}

	public void stopRecord() {

		if(videoRecorder!=null){
			Toast.makeText(MainActivity.this, "Stop video record",
					Toast.LENGTH_SHORT).show();
			releaseMediaRecorder();
		}
		
		

		// String pathForAppFiles = getFilesDir().getAbsolutePath();
		// pathForAppFiles += RECORDED_FILE;
		/*
		 * String pathForAppFiles = ExternalStorage.getSdCardPath();
		 * pathForAppFiles += RECORDED_FILE; Log.d("Audio filename:",
		 * pathForAppFiles);
		 * 
		 * ContentValues values = new ContentValues(10);
		 * 
		 * values.put(MediaStore.MediaColumns.TITLE, "RecordedVideo");
		 * values.put(VideoColumns.ALBUM, "Your Groundbreaking Movie");
		 * values.put(VideoColumns.ARTIST, "Your Name");
		 * values.put(MediaColumns.DISPLAY_NAME,
		 * "The Video File You Recorded In Media App");
		 * 
		 * values.put(MediaStore.MediaColumns.TITLE, "RecordedVideo");
		 * values.put(MediaStore.MediaColumns.DATE_ADDED,
		 * System.currentTimeMillis() / 1000);
		 * values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
		 * values.put(MediaColumns.DATA, pathForAppFiles);
		 * 
		 * Uri videoUri =
		 * getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI
		 * , values); if (videoUri == null) { Log.d("Video",
		 * "Content resolver failed"); return; }
		 * 
		 * // Force Media scanner to refresh now. Technically, this is //
		 * unnecessary, as the media scanner will run periodically but //
		 * helpful for testing. Log.d("Video URI", "Path = " +
		 * videoUri.getPath()); sendBroadcast(new
		 * Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, videoUri));
		 */
	}
	
	Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			progressBar.setProgress(msg.what);
		}
	};
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseMediaRecorder();
	}
}
