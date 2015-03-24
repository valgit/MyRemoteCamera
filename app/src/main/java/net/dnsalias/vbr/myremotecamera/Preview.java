package net.dnsalias.vbr.myremotecamera;


import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


class Preview extends SurfaceView implements SurfaceHolder.Callback  {
    private static final String TAG = "Preview";

    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Boolean isPreview;
    private Size previewSize;
    private final LinkedList<byte[]> mQueue = new LinkedList<byte[]>();

    Preview(Context context, Camera camera, SurfaceView sv) {
        super(context);
        if(!isInEditMode()) {
            if (camera != null)
                mCamera = camera;
            else {
                Log.d(TAG, "surfaceCreated: Error setting camera => NULL");
                return;
            }

            mSurfaceView = sv;
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    Preview(Context context, Camera camera, AttributeSet attrs) {
        super(context, attrs);
        if(!isInEditMode()) {
            Log.d(TAG, "surfaceCreated: Error setting camera with attrs");
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        //camera = Camera.open();
        //try {
            mHolder = holder;

            //mCamera.setPreviewDisplay(holder);

            isPreview = false;
			/*
			camera.setPreviewCallback(new PreviewCallback() {

				public void onPreviewFrame(byte[] data, Camera arg1) {
					FileOutputStream outStream = null;
					try {
						outStream = new FileOutputStream(String.format(
								"/sdcard/%d.jpg", System.currentTimeMillis()));
						outStream.write(data);
						outStream.close();
						Log.d(TAG, "onPreviewFrame - wrote bytes: "
								+ data.length);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
					}
					Preview.this.invalidate();
				}
			});
			 */
/*
        } catch (IOException e) {
            //e.printStackTrace();
            Log.d(TAG, "surfaceCreated: Error setting camera preview: " + e.getMessage());
        }
*/
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        // empty. Take care of releasing the Camera preview in your activity.
        Log.d(TAG,"Stopping preview in SurfaceDestroyed().");
		/* done in activity ...
		mCamera.stopPreview();
		mCamera.setPreviewCallback(null);
		*/
        //camera = null;
        isPreview = false;

    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Log.d(TAG, "INFO: getOptimalPreviewSize fitting size : " + h + "," + w );
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            Log.d(TAG, "INFO: getOptimalPreviewSize test size : " + size.height + "," + size.width);
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            Log.d(TAG, "INFO: getOptimalPreviewSize optimal not found");
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                Log.d(TAG, "INFO: getOptimalPreviewSize try size : " + size.height + "," + size.width);
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();

        List<Size> sizes = parameters.getSupportedPreviewSizes();
        int i = 0;
        for (Camera.Size cs : sizes) {
            Log.d(TAG, "Camera - preview supports:(" + (i++) + ") " + cs.width + "x" + cs.height);
        }
        Size optimalSize = getOptimalPreviewSize(sizes, h, w);
        Log.d(TAG, "INFO: optimal size : " + optimalSize.height + "," + optimalSize.width);
        previewSize = optimalSize;

        Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        //Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 90; break;
            case Surface.ROTATION_90: degrees = 180; break;
            case Surface.ROTATION_180: degrees = 270; break;
            case Surface.ROTATION_270: degrees = 0; break;
        }

        mCamera.setDisplayOrientation(degrees);
        Log.d(TAG, "INFO: orient " + degrees);

        //parameters.setJpegQuality(100);//a value between 1 and 100

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        //parameters.set("jpeg-quality", 100);
        //parameters.setPreviewSize(w, h);

        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        mCamera.setParameters(parameters);
        //camera.startPreview();

        //-- Must add the following callback to allow the camera to autofocus.
        mCamera.autoFocus(new Camera.AutoFocusCallback(){
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Log.d(TAG, "onAutoFocus: isAutofocus " + Boolean.toString(success));
            }
        } );

        // start preview with new settings
        try {
            mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            isPreview = true;

        } catch (Exception e){
            Log.d(TAG, "surfaceChanged: Error starting camera preview: " + e.getMessage());
        }
    }

    public void onPause() {
        if (mCamera != null) {
            Log.d(TAG, "onPause");
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
        resetBuff();
    }

/*
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		Paint p = new Paint(Color.RED);
		Log.d(TAG, "draw");
		canvas.drawText("PREVIEW", canvas.getWidth() / 2,
				canvas.getHeight() / 2, p);
	}
*/
    private void resetBuff() {

        synchronized (mQueue) {
            mQueue.clear();
            //mLastFrame = null;
        }
    }

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // TODO Auto-generated method stub

            Log.d(TAG, "onPreviewFrame");
            /*
            synchronized (mQueue) {
                if (mQueue.size() == MAX_BUFFER) {
                    mQueue.poll();
                }
                mQueue.add(data);
            }
            */
            Preview.this.invalidate();
        }
    };

}
