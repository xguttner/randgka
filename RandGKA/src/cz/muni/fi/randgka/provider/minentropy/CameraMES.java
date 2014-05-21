package cz.muni.fi.randgka.provider.minentropy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import cz.muni.fi.randgka.tools.ByteSequence;
import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

/**
 * Class utilizing the camera as a min-entropy source. It needs CameraMESSupport class to gain
 * a SurfaceView and Camera objects.
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class CameraMES implements MinEntropySource, Callback, PreviewCallback, Serializable {

	private static final long serialVersionUID = 3507582501311379646L;
	
	// camera-related parameters 
	private Camera camera;
	private Parameters cameraSettings;
	private boolean surfaceReady, cameraReady;
	private Looper cameraLooper;
	private SurfaceView surfaceView;
	private byte[] imageBuffer;
	private static final int //PREVIEW_HEIGHT = 240, 
			//PREVIEW_WIDTH = 320, 
			//YBLOCK_SIZE = PREVIEW_HEIGHT*PREVIEW_WIDTH,
			MAXIMUM_FPS = 16000;
	
	// processing-related parameters
	private static final int SQUARE_MATRIX_WIDTH = 20, // number of squares in the row the preview frame is divided into
			SQUARE_SIDE = 16, 
			//SQUARE_OFFSET = YBLOCK_SIZE / (SQUARE_SIDE*SQUARE_SIDE),
			FRAME_SKIPPER = 1, // we take every FRAME_SKIPPER-th frame
			PREPROCESSED_SAMPLE_LENGTH = 4;
	private ByteSequence sourceData,
			byteHolder;
	private CountDownLatch countDownLatch;
	private boolean preprocessingFlag;
	private int bytesPerSample,
			currentShift = 0,
			frameNo = 0,
			sampleNumber,
			currentSample;
	
	// storing-related parameters
	private FileOutputStream fos;
	private boolean store;
	
	//new preprocessing
	private static final int P_WIDTH = 320,
			P_HEIGHT = 240,
			YB_SIZE = P_WIDTH*P_HEIGHT,
			SQ_SIDE = 10,
			SQ_IN_ROW = P_WIDTH/SQ_SIDE,
			SQ_IN_COLUMN = P_HEIGHT/SQ_SIDE,
			NO_OF_ROW_MERGE = 8,
			PREPROCESSED_LENGTH = SQ_IN_ROW/NO_OF_ROW_MERGE,
			PER_BYTE_ROUNDS = (int)Math.ceil(8.0/PREPROCESSED_LENGTH);
	
	/**
	 * Non-parametric constructor
	 */
	public CameraMES() {}

	/**
	 * MinEntropySource implemented method
	 */
	public boolean initialize() {return true;}
	
	public boolean initialize(SurfaceView surfaceView) {
		// if camera isn't opened, try to open it
		if (camera == null) {
			countDownLatch = new CountDownLatch(1);
			Thread cameraThread = new Thread(new CameraThread());
	        cameraThread.start();
	        try {
				countDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
        
        //set preview parameters
		cameraSettings = camera.getParameters();
		cameraSettings.setPreviewFormat(ImageFormat.NV21);
		cameraSettings.setPreviewSize(P_WIDTH, P_HEIGHT);
		
		try {
			List<int[]> rates = cameraSettings.getSupportedPreviewFpsRange();
			if (rates != null && rates.size() > 0) {
				int[] fps = rates.get(0);
				for (int[] rate : rates) {
					if (rate[1] <= MAXIMUM_FPS) fps = rate;
				}
				cameraSettings.setPreviewFpsRange(fps[0], fps[1]);
			}
		} catch (NoSuchMethodError e) {
			// for API level < 9
			try {
				List<Integer> rates = cameraSettings.getSupportedPreviewFrameRates();
			
				if (rates != null && rates.size() > 0) {
					Integer fps = rates.get(0);
					for (Integer rate : rates) {
						if (rate <= MAXIMUM_FPS) fps = rate;
					}
					cameraSettings.setPreviewFrameRate(fps);
				}
			} finally {}
		}
		
		camera.setParameters(cameraSettings);
		camera.setDisplayOrientation(0);
		
		// set callback buffer for image preview
		bytesPerSample = ImageFormat.getBitsPerPixel(cameraSettings.getPreviewFormat()) * P_WIDTH * P_HEIGHT / 8;
		imageBuffer = new byte[this.bytesPerSample];
		camera.addCallbackBuffer(imageBuffer);

		// set initial values
		sampleNumber = -1;
		currentSample = 0;
		preprocessingFlag = true;
		
		// get surfaceView
		this.surfaceView = surfaceView;
		this.setDisplaySurface(surfaceView);
		this.startPreview();
		return cameraReady;
	}
	
	/**
	 * Initialization of one attempt to gain min-entropy sequence.
	 * 
	 * @param sampleNumber - number of frames we want to use
	 */
	public void initializeRun(int sampleNumber) {
		this.sourceData = new ByteSequence();
		this.countDownLatch = new CountDownLatch(sampleNumber);
		this.currentSample = 0;
		this.frameNo = 0;
		this.sampleNumber = sampleNumber;
		this.byteHolder = new ByteSequence();
	}
	
	/**
	 * Process the received image according to pre-set parameters
	 * 
	 * @param newData received from the preview
	 * @param camera
	 */
	public void onPreviewFrame(byte[] newData, Camera camera) {
		ByteSequence data = null;
		camera.addCallbackBuffer(imageBuffer);
		Log.d("new", "frame");
		try {
			frameNo++;
			
			// test if we skipped given amount of frames (for sufficient independence) and if we need some more frames
			if (frameNo%FRAME_SKIPPER == 0 && sampleNumber > currentSample) {
				
				// decision about preprocessing
				if (preprocessingFlag) data = preprocess(new ByteSequence(newData, bytesPerSample*8));
				else data = new ByteSequence(newData, bytesPerSample*8);
				//Log.d("oneseq", data.toString());
				// decision about storing the data
				if (store) {
					byteHolder.add(data);
					if ((currentSample+1)%PER_BYTE_ROUNDS == 0){
						//Log.d("bh", byteHolder.toString());
						fos.write(byteHolder.getSequence());
						byteHolder = new ByteSequence();
					}
				}
				else sourceData.add(data);
				
				currentSample++;
				countDownLatch.countDown();

				if (store && sampleNumber == currentSample) {
					fos.close();
					sampleNumber = -1;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Preprocessing mechanism. Mechanism is set for the NV21 format that should be available in all
	 * Android devices. 
	 * 
	 * @param randSequence to preprocess
	 * @return ByteSequence of the preprocessed data
	 */
	private ByteSequence preprocess(ByteSequence randSequence) {
		
		byte[] inputData = randSequence.getSequence();
		byte[] preprocessedData = new byte[PREPROCESSED_LENGTH];
		byte[] columnPixels = new byte[SQ_IN_ROW];
		ByteSequence returnSequence = new ByteSequence();
		
		// process frame into columnPixels
		for (int j = 0, i = 0; i < SQ_IN_ROW*SQ_IN_COLUMN; i++) {
			j = (i/SQ_IN_ROW)*(P_WIDTH*SQ_SIDE) + // move to current square row
					(i%SQ_IN_ROW)*SQ_SIDE + // move to current square
					((i+((3*currentSample)/SQ_SIDE))%SQ_IN_ROW%SQ_SIDE)*P_WIDTH + // move to current row in square
					((i/SQ_IN_ROW)+(3*currentSample))%SQ_SIDE; // move from left border of square
			columnPixels[i%SQ_IN_ROW] = (byte)(0xff & ((int)columnPixels[i%SQ_IN_ROW] ^ ((int)inputData[j] ^ (int)inputData[YB_SIZE+2*(int)(j/4)] ^ (int)inputData[YB_SIZE+2*(int)(j/4)+1])));
		}
		
		// process columnPixels into PREPROCESSED_SAMPLE_LENGTH bits
		for (int i = 0; i < PREPROCESSED_LENGTH; i++) {
			for (int k = 0; k < NO_OF_ROW_MERGE; k++) { // xoring different columns into one of the PREPROCESSED_SAMPLE_LENGTH important
				preprocessedData[i] = (byte)(0xff & ((int)preprocessedData[i] ^ (int)columnPixels[i + k*PREPROCESSED_LENGTH]));
			}
			for (int l = 1; l < 8; l++) { // xoring the bits of one byte to get the outcoming bit (stored as LSB of a byte)
				byte before = preprocessedData[i];
				preprocessedData[i] = (byte)(0xff & ((int)(0x01 & before) ^ (int)(0x7f & (preprocessedData[i] >> 1))));
			}
			returnSequence.addBit(preprocessedData[i]);
		}
		
		return returnSequence;
	}
	
	/**
	 * Preprocessing mechanism. Mechanism is set for the NV21 format that should be available in all
	 * Android devices. 
	 * 
	 * @param randSequence to preprocess
	 * @return ByteSequence of the preprocessed data
	 */
	/*private ByteSequence preprocess2(ByteSequence randSequence) {
		// shift the pixels used as a source between frames
		currentShift = (currentShift + 5) % SQUARE_SIDE;
		
		byte[] inputData = randSequence.getSequence();
		byte[] preprocessedData = new byte[PREPROCESSED_SAMPLE_LENGTH];
		byte[] columnPixels = new byte[SQUARE_MATRIX_WIDTH];
		
		ByteSequence returnSequence = new ByteSequence();
		
		// process frame into columnPixels
		for (int j = 0, noOfSq = 0; noOfSq < SQUARE_OFFSET; noOfSq++) {
			j = (SQUARE_SIDE * SQUARE_MATRIX_WIDTH) * // pixels per row
					((currentShift+noOfSq)%SQUARE_SIDE + SQUARE_SIDE * (int)(noOfSq/SQUARE_MATRIX_WIDTH)) //line; shift verticaly possible here
					+ SQUARE_SIDE*(noOfSq%SQUARE_MATRIX_WIDTH) // pixels from row begin to current square
					+ (int)((noOfSq/SQUARE_SIDE)+currentShift)%SQUARE_SIDE; //pixels from row begin in the square; another possibility of shift (horizontaly) on this line 
			columnPixels[noOfSq%SQUARE_MATRIX_WIDTH] = (byte)(0xff & ((int)columnPixels[noOfSq%SQUARE_MATRIX_WIDTH] ^ ((int)inputData[j] ^ (int)inputData[YBLOCK_SIZE+2*(int)(j/4)] ^ (int)inputData[YBLOCK_SIZE+2*(int)(j/4)+1])));
		}
		
		// process columnPixels into PREPROCESSED_SAMPLE_LENGTH bits
		for (int i = 0; i < PREPROCESSED_SAMPLE_LENGTH; i++) {
			for (int k = 0; k < 5; k++) { // xoring different columns into one of the PREPROCESSED_SAMPLE_LENGTH important
				preprocessedData[i] = (byte)(0xff & ((int)preprocessedData[i] ^ (int)columnPixels[i + k*PREPROCESSED_SAMPLE_LENGTH]));
			}
			for (int l = 1; l < 8; l++) { // xoring the bits of one byte to get the outcoming bit (stored as LSB of a byte)
				byte before = preprocessedData[i];
				preprocessedData[i] = (byte)(0xff & ((int)(0x01 & before) ^ (int)(0x7f & (preprocessedData[i] >> 1))));
			}
			returnSequence.addBit(preprocessedData[i]);
		}
		
		return returnSequence;
	}*/
	
	@Override
	public ByteSequence getMinEntropyData(int minEntropyDataLength, File storage) {
		return this.getSourceData(minEntropyDataLength, storage, true);
	}
	
	public ByteSequence getRawSourceData(int minEntropyDataLength, File storage) {
		return this.getSourceData(minEntropyDataLength, storage, false);
	}

	public ByteSequence getPreprocessedSourceData(int minEntropyDataLength, File storage) {
		return this.getSourceData(minEntropyDataLength, storage, true);
	}

	@Override
	public int getBitsPerSample(boolean usingPreprocessing) {
		if (usingPreprocessing) return PREPROCESSED_LENGTH;
		else return bytesPerSample*8;
	}
	
	/**
	 * 
	 * @param minEntropyDataLength
	 * @param storage
	 * @param preprocessingFlag
	 * @return data from camera-source according to given parameters
	 */
	private ByteSequence getSourceData(int minEntropyDataLength, File storage, boolean preprocessingFlag) {
		
		// set values to be seen from onPreviewFrame method
		this.sampleNumber = (int)Math.ceil((double)minEntropyDataLength/getBitsPerSample(preprocessingFlag));
		this.preprocessingFlag = preprocessingFlag;
		
		try {
			// decide about storing
			this.store = false;
			if (storage != null) {
				this.store = true;
				this.fos = new FileOutputStream(storage);
			}
			
			this.initializeRun((int) sampleNumber);
			
			//wait for the result
			this.countDownLatch.await();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// cut to the given length
		sourceData.setBitLength(minEntropyDataLength);
		
		return (!this.store)?this.sourceData:null;
	}
	
	private void setDisplaySurface(SurfaceView surface) {
		this.surfaceView = surface;
		surface.getHolder().addCallback(this);
	}

	private void startPreview() {
		if (surfaceReady) {
			try {
				camera.setPreviewDisplay(surfaceView.getHolder());
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
		if (fos != null) {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			if (camera == null) initialize(surfaceView);
			camera.setPreviewDisplay(holder);
			surfaceReady = true;
			if (cameraReady) {
				camera.startPreview();
				camera.setPreviewCallbackWithBuffer(this);
			}
		} catch (IOException e) {
			Log.e("CameraHandler", "Cannot set preview");
			e.printStackTrace();
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void surfaceDestroyed(SurfaceHolder holder) {
		holder.getSurface().release();
		stop();
	}

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

	@Override
	public boolean ready() {
		return cameraReady;
	}
}
