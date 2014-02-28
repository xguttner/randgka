package cz.muni.fi.randgka.provider.minentropy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import cz.muni.fi.androidrandextr.setrt.MinEntropyApproximationRT;
import cz.muni.fi.randgka.library.ByteSequence;
import cz.muni.fi.randgka.library.GainMode;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class CameraMES implements MinEntropySource, Callback, PreviewCallback {

	private Camera camera;
	private Parameters cameraSettings;
	private boolean surfaceReady, cameraReady;
	private Looper cameraLooper;
	private SurfaceView surfaceView;
	
	private byte[] imageBuffer;
	private static final int SQUARE_MATRIX_WIDTH = 20;
	private static final int PREVIEW_HEIGHT = 240, PREVIEW_WIDTH = 320, SQUARE_SIDE = 16, MAXIMUM_FPS = 16000;
	private long sampleNumber, currentSample;
	private FileOutputStream fos;
	private boolean store;
	private ByteSequence sourceData;
	private CountDownLatch countDownLatch;
	private boolean preprocessingFlag;
	private static final int PREPROCESSED_SAMPLE_LENGTH = 4;
	private int bytesPerSample;
	private int currentShift = 0;

	/**
	 * CameraThread enables to run actions on camera in separate thread
	 */
	private class CameraThread implements Runnable {
		@Override
		public void run() {
			Looper.prepare();
			cameraLooper = Looper.myLooper();
			camera = Camera.open();
			countDownLatch.countDown();
			Looper.loop();
		}
	}
	
	/**
	 * Non-parametric constructor
	 */
	public CameraMES() {}

	/**
	 * MinEntropySource implemented method
	 */
	public boolean initialize() {
		
		//wait for camera to open
		countDownLatch = new CountDownLatch(1);
		Thread cameraThread = new Thread(new CameraThread());
        cameraThread.start();
        try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        //set preview parameters
		cameraSettings = camera.getParameters();
		cameraSettings.setPreviewFormat(ImageFormat.NV21);
		cameraSettings.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
		List<int[]> rates = cameraSettings.getSupportedPreviewFpsRange();
		if (rates != null && rates.size() > 0) {
			int[] fps = rates.get(0);
			for (int[] rate : rates) {
				if (rate[1] <= MAXIMUM_FPS) fps = rate;
			}
			cameraSettings.setPreviewFpsRange(fps[0], fps[1]);
		}
		camera.setParameters(cameraSettings);
		
		// set callbackbuffer
		bytesPerSample = ImageFormat.getBitsPerPixel(cameraSettings.getPreviewFormat()) * PREVIEW_WIDTH * PREVIEW_HEIGHT / 8;
		imageBuffer = new byte[this.bytesPerSample];
		camera.addCallbackBuffer(imageBuffer);
		
//		if(rotation==Surface.ROTATION_0) camera.setDisplayOrientation(90);
//		if(rotation==Surface.ROTATION_90) camera.setDisplayOrientation(0);
//		if(rotation==Surface.ROTATION_270) camera.setDisplayOrientation(180);
		camera.setDisplayOrientation(0);

		// set initial values
		sampleNumber = -1;
		currentSample = 0;
		preprocessingFlag = true;
		
		// get surfaceView
		surfaceView = SurfaceViewProvider.getSurfaceView();
		this.setDisplaySurface(surfaceView);
		this.startPreview();
		
		return cameraReady && surfaceReady;
	}
	
	private void setDisplaySurface(SurfaceView surface) {
		this.surfaceView = surface;
		surface.getHolder().addCallback(this);
	}

	public void startPreview() {
		if (surfaceReady) {
			try {
				Log.d("CameraHandler", "camera from start Preview");
				camera.setPreviewDisplay(surfaceView.getHolder());
				Log.d("threadstartPreview", "id: "
						+ Thread.currentThread().getId());
				camera.setPreviewCallbackWithBuffer(this);
			} catch (IOException e) {
			}
			camera.startPreview();
		} else
			cameraReady = true;

	}

	public void stop() {
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
		if (cameraLooper != null) cameraLooper.quit();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			if (camera == null) initialize();
			camera.setPreviewDisplay(holder);
			surfaceReady = true;
			if (cameraReady) {
				camera.startPreview();
				camera.setPreviewCallbackWithBuffer(this);
			}
		} catch (IOException e) {
			Log.e("CameraHandler", "Something went wrong cannot set preview");
			e.printStackTrace();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if (camera!=null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
		if (cameraLooper != null) cameraLooper.quit();
	}

	public void onPreviewFrame(byte[] newData, Camera camera) {
		ByteSequence data = null;
		camera.addCallbackBuffer(imageBuffer);
		try {
			if (sampleNumber > currentSample) {
				if (this.preprocessingFlag) data = this.preprocess(new ByteSequence(newData, this.bytesPerSample*8));
				else data = new ByteSequence(newData, this.bytesPerSample*8);
				Log.d("data",data.toString());
				if (this.store) this.fos.write(data.getSequence());
				else this.sourceData.add(data);
				
				currentSample++;
				this.countDownLatch.countDown();
			}
			if (this.store && sampleNumber == currentSample) {
				this.fos.close();
				sampleNumber = -1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ByteSequence preprocess(ByteSequence randSequence) {
		currentShift = (currentShift + 5) % SQUARE_SIDE;
		byte[] inputData = randSequence.getSequence();
		byte[] preprocessedData = new byte[PREPROCESSED_SAMPLE_LENGTH];
		int offset = this.PREVIEW_WIDTH * this.PREVIEW_HEIGHT;
		int squareOffset = offset / (SQUARE_SIDE*SQUARE_SIDE);
		byte[] columnPixels = new byte[SQUARE_MATRIX_WIDTH];
		ByteSequence returnSequence = new ByteSequence();
		for (int i = 0; i < SQUARE_MATRIX_WIDTH; i++) {
			columnPixels[i] = 0x00;
		}
		int j = 0;
		for (int noOfSq = 0; noOfSq < squareOffset; noOfSq++) {
			j = (SQUARE_SIDE * SQUARE_MATRIX_WIDTH) * // pixels per row
					((currentShift+noOfSq)%SQUARE_SIDE + SQUARE_SIDE * (int)(noOfSq/SQUARE_MATRIX_WIDTH)) //line; shift verticaly possible here
					+ SQUARE_SIDE*(noOfSq%SQUARE_MATRIX_WIDTH) // pixels from row begin to current square
					+ (int)((noOfSq/SQUARE_SIDE)+currentShift)%SQUARE_SIDE; //pixels from row begin in the square; another possibility of shift (horizontaly) on this line 
			columnPixels[noOfSq%SQUARE_MATRIX_WIDTH] = (byte)(0xff & ((int)columnPixels[noOfSq%SQUARE_MATRIX_WIDTH] ^ ((int)inputData[j] ^ (int)inputData[offset+2*(int)(j/4)] ^ (int)inputData[offset+2*(int)(j/4)+1])));
		}
		for (int i = 0; i < 4; i++) {
			preprocessedData[i] = 0x00;
			for (int k = 0; k < 5; k++) { // xoring different columns into one of the four important
				preprocessedData[i] = (byte)(0xff & ((int)preprocessedData[i] ^ (int)columnPixels[i + k*4]));
			}
			for (int l = 1; l < 8; l++) { // xoring the bits of one byte to get the outcoming bit (stored as LSB of a byte)
				byte before = preprocessedData[i];
				preprocessedData[i] = (byte)(0xff & ((int)(0x01 & before) ^ (int)(0x7f & (preprocessedData[i] >> 1))));
			}
			returnSequence.addBit(preprocessedData[i]);
		}
		
		return returnSequence;
	}
	
	public void setCountDownLatch(int sampleNumber) {
		this.sourceData = new ByteSequence();
		this.countDownLatch = new CountDownLatch(sampleNumber);
		this.currentSample = 0;
		this.sampleNumber = sampleNumber; 
	}

	public CountDownLatch getCountDownLatch() {
		return this.countDownLatch;
	}
	
	private ByteSequence getSourceData(int minEntropyDataLength, File storage, boolean preprocessingFlag) {
		this.sampleNumber = (int)Math.ceil((double)minEntropyDataLength/getBytesPerSample(preprocessingFlag));
		this.preprocessingFlag = preprocessingFlag;
		try {
			this.store = false;
			
			if (storage != null) {
				this.store = true;
				this.fos = new FileOutputStream(storage);
			}
			
			this.setCountDownLatch((int) sampleNumber);
			this.countDownLatch.await();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		this.sourceData.setBitLength(minEntropyDataLength);
		
		return (!this.store)?this.sourceData:null;
	}
	
	public ByteSequence getRawSourceData(int minEntropyDataLength, File storage) {
		return this.getSourceData(minEntropyDataLength, storage, false);
	}

	public ByteSequence getPreprocessedSourceData(int minEntropyDataLength, File storage) {
		return this.getSourceData(minEntropyDataLength, storage, true);
	}

	@Override
	public int getBytesPerSample(boolean usingPreprocessing) {
		if (usingPreprocessing) return PREPROCESSED_SAMPLE_LENGTH;
		else return bytesPerSample;
	}

	@Override
	public ByteSequence getMinEntropyData(int minEntropyDataLength) {
		return this.getSourceData(minEntropyDataLength, null, true);
	}
}
