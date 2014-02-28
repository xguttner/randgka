package cz.muni.fi.randgka.provider.minentropy;

import android.view.SurfaceView;

/**
 * Class holding a current surfaceView to provide it to CameraMES
 */
public class SurfaceViewProvider {
	
	private static SurfaceView surfaceView;
	
	public static void setSurfaceView(SurfaceView surfaceView) {
		SurfaceViewProvider.surfaceView = surfaceView;
	}
	
	public static SurfaceView getSurfaceView() {
		return surfaceView;
	}
	
}
