package net.dnsalias.vbr.myremotecamera;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MyRemoteCamera extends ActionBarActivity {
    private static final String TAG = "MyCamera";

    private Camera camera;
    private Preview preview;
    private boolean mRecordLocation;
    private Button buttonClick;
    private TextView myServerIP;

    private prefmanager prefs;
    private String mIP;
    private int mPort = 8888;


    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera

            return true;
        } else {
            // no camera on this device
            return false;
        }

    };

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.d(TAG, "getCameraInstance: Error getting camera : " + e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_remote_camera);

        // Create an instance of Camera
        camera = getCameraInstance();
        // get Camera parameters
        //Camera.Parameters params = camera.getParameters();

        preview = new Preview(this,camera, (SurfaceView)findViewById(R.id.surfaceView));
        //preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.preview)).addView(preview);

        buttonClick = (Button) findViewById(R.id.buttonClick);
        buttonClick.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();

            }
        });

        prefs = new prefmanager(getApplicationContext());

           /*
        myServerIP = (TextView) findViewById(R.id.serverip);
        serverIP = getLocalIpAddress();
        myServerIP.setText(serverIP.toString());

        serverthr = new ServerThread(serverIP,8080);
        Thread fst = new Thread(serverthr);
        fst.start();
        //String msg = "addr" + serverIP.toString();
        */
        Log.d(TAG, "onCreate");
    }

    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            Log.d(TAG, "onShutter");
        }
    };

    /** Handles data for raw picture */
    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - raw");
        }
    };

    /** Create a File for saving an image or video */
    private File getOutputMediaFile() {
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;

        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        File mediaStorageDir;

        if (mExternalStorageWriteable) {
            mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM), "MyCamera");

            //File mediaStorageDir = new File("/sdcard/DCIM/106BLZTE");

            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d(TAG, "failed to create directory" + mediaStorageDir.toString());
                    return null;
                }
            }
        } else {
            // use internal storage
            ContextWrapper cw= new ContextWrapper(getApplicationContext());
            mediaStorageDir = cw.getDir("imageDir", Context.MODE_PRIVATE);
        }
        return mediaStorageDir;
    }

    /** Handles data for jpeg picture */
    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

            FileOutputStream outStream = null;
            File mediaDir = getOutputMediaFile();

            try {
                String timeStamp = new SimpleDateFormat("HHmmss").format(new Date());
                String fileName = String.format("%s/DSC_%s.JPG",mediaDir.toString(),timeStamp);

                outStream = new FileOutputStream(fileName);
                outStream.write(data);
                outStream.flush();
                outStream.close();
                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }

            // ?
            camera.startPreview();
            Log.d(TAG, "onPictureTaken - jpeg");

        }
    };

    private void releaseCamera(){
        if (camera != null){
            camera.stopPreview();
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"release camera onPause.");
        releaseCamera();              // release the camera immediately on pause event
    }

    protected void onResume() {
        super.onResume();

        Log.d(TAG,"activity camera onResume.");
        if (camera == null){
            Log.d(TAG,"acquireing camera onResume.");
            // Create an instance of Camera
            camera = getCameraInstance();
            //Setup the FrameLayout with the Camera Preview Screen
            /*
            preview = new Preview(this,camera,);

            ((FrameLayout) findViewById(R.id.preview)).addView(preview);
            */
            camera.startPreview();
        }

    }

    @Override
    public void onDestroy()
    {
        if (camera != null)
        {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
        super.onDestroy();
    }

    public void takePicture() {
        if (camera != null) {
            camera.stopPreview();
            Camera.Parameters mParameters = camera.getParameters();

            // get the best picture size ...
            List<Size> sizes = mParameters.getSupportedPictureSizes();
            int i = 0;
            int dim = 0;
            for (Camera.Size cs : sizes) {
                if(cs.width  >= 1024 && cs.height >= 768) dim = i;
                Log.d(TAG, "Camera - supports:(" + (i++) + ") " + cs.width + "x" + cs.height);

            }
            Size size = sizes.get(0);
            mParameters.setPictureSize(size.width, size.height);

            Log.d(TAG, "Camera - current focus : " + mParameters.getFocusMode());
            Log.d(TAG, "Camera - current expo  : " + mParameters.getExposureCompensation());
            Log.d(TAG, "Camera - current zoom  : " + mParameters.getZoom());
            //Log.d(TAG, "Camera - current metreing area  : " + mParameters.getMaxNumMeteringAreas());

            //p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_FLUORESCENT);

            mParameters.setJpegQuality(100);//a value between 1 and 100
            mParameters.setPictureFormat(PixelFormat.JPEG);
            camera.setParameters(mParameters);

            camera.takePicture(shutterCallback, /*rawCallback */ null,
                    jpegCallback);
            // maybe here ?
            //camera.startPreview();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my_remote_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                setting();
                break;
				/*
			case R.id.quitter:
			//Pour fermer l'application il suffit de faire finish()
               finish();
               return true;
			   */
        }

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }
*/
        return super.onOptionsItemSelected(item);
    }

    private void setting() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.server_setting, null);
        AlertDialog dialog =  new AlertDialog.Builder(MyRemoteCamera.this)
                //.setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.setting_title)
                .setView(textEntryView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        EditText ipEdit = (EditText)textEntryView.findViewById(R.id.ip_edit);
                        EditText portEdit = (EditText)textEntryView.findViewById(R.id.port_edit);
                        mIP = ipEdit.getText().toString();
                        mPort = Integer.parseInt(portEdit.getText().toString());

                        Toast.makeText(MyRemoteCamera.this, "New address: " + mIP + ":" + mPort, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked cancel so do some stuff */
                    }
                })
                .create();
        dialog.show();
    }
}






