package edu.ucsb.ece.ece150.maskme;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import edu.ucsb.ece.ece150.maskme.camera.CameraSourcePreview;
import edu.ucsb.ece.ece150.maskme.camera.GraphicOverlay;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends AppCompatActivity {
    private static final String TAG = "FaceTracker";

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private enum ButtonsMode {
        PREVIEW_TAKEPICTURE_INVISIBLE, BACK_MASK_LUCKY
    }

    SparseArray<Face> mFaces = new SparseArray<>();

    private ButtonsMode buttonsMode = ButtonsMode.PREVIEW_TAKEPICTURE_INVISIBLE;
    private MaskedImageView mImageView;
    private Button mCameraButton;
    private Bitmap mCapturedImage;
    private Button mLeftButton;
    private Button mRightButton;

    private FaceDetector mStaticFaceDetector;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.facetracker_main);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        mCameraButton = (Button) findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(buttonsMode == ButtonsMode.PREVIEW_TAKEPICTURE_INVISIBLE) {
                    mLeftButton.setVisibility(View.VISIBLE);
                    if(mCameraSource != null){
                        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data) {
                                mCapturedImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                                int degree = 0;
                                switch (getWindowManager().getDefaultDisplay().getRotation()) {
                                    case Surface.ROTATION_0:
                                        degree = 90;
                                        break;
                                    case Surface.ROTATION_90:
                                        degree = 0;
                                        break;
                                    case Surface.ROTATION_180:
                                        degree = 270;
                                        break;
                                    case Surface.ROTATION_270:
                                        degree = 180;
                                        break;
                                }
                                // rotate the image by appropriate degree
                                rotateImage(degree);
                                // resize the image to fit the layout
                                resizeImage();
                                mImageView.setImageBitmap(mCapturedImage);
                                Toast.makeText(getApplicationContext(), "Photo captured", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                else{
                    if(mFaces.size() == 0) {
                        detectStaticFaces(mCapturedImage);
                    }
                    mImageView.drawFirstMask(mFaces);
                }
            }
        });

        mLeftButton = (Button) findViewById(R.id.left_button);
        mRightButton = (Button) findViewById(R.id.right_button);

        mLeftButton.setVisibility(View.GONE);
        mRightButton.setVisibility(View.GONE);

        mLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(buttonsMode == ButtonsMode.PREVIEW_TAKEPICTURE_INVISIBLE) {
                    mImageView.setImageBitmap(mCapturedImage);
                    mPreview.addView(mImageView);
                    mPreview.bringChildToFront(mImageView);
                    mLeftButton.setText("Back");
                    mCameraButton.setText("Mask!");
                    mRightButton.setVisibility(View.VISIBLE);
                    buttonsMode = ButtonsMode.BACK_MASK_LUCKY;
                }
                else{
                    mPreview.removeView(mImageView);
                    mLeftButton.setText("Preview");
                    mCameraButton.setText("Take Picture!");
                    mRightButton.setVisibility(View.GONE);
                    mFaces.clear();
                    buttonsMode = ButtonsMode.PREVIEW_TAKEPICTURE_INVISIBLE;
                }
            }
        });

        mRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mFaces.size() == 0) {
                    detectStaticFaces(mCapturedImage);
                }
                mImageView.drawSecondMask(mFaces);
            }
        });

        mImageView = new MaskedImageView(getApplicationContext());
        mImageView.setScaleType(ImageView.ScaleType.FIT_START);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionGranted == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // restart the camera
        mPreview.stop();
        createCameraSource();
        startCameraSource();

        if (buttonsMode == ButtonsMode.BACK_MASK_LUCKY){
            mImageView.setBackgroundColor(Color.BLACK);
        }

    }

    // resize the image to fit the layout
    private void resizeImage(){
        int width = mCapturedImage.getWidth();
        int height = mCapturedImage.getHeight();
        float scaleWidth = ((float) mPreview.mLayoutWidth) / width;
        float scaleHeight = ((float) mPreview.mlayoutHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        mCapturedImage = Bitmap.createBitmap(mCapturedImage, 0,0, width, height, matrix, false);
    }

    // rotate the image by given degree
    private void rotateImage(int degree){
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        mCapturedImage = Bitmap.createBitmap(mCapturedImage, 0, 0, mCapturedImage.getWidth(), mCapturedImage.getHeight(),
                matrix, true);

    }

    private void detectStaticFaces(Bitmap inputImage){
        if(inputImage == null){
            return;
        }

        Frame frame = new Frame.Builder().setBitmap(inputImage).build();
        mFaces = mStaticFaceDetector.detect(frame);

        Log.i("NumberOfFaces", String.valueOf(mFaces.size()));
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        // TODO: Create a face detector for real time face detection
        // 1. Get the application's context
        Context context = getApplicationContext();
        //
        // 2. Create a FaceDetector object for real time detection
        //    Ref: https://developers.google.com/vision/android/face-tracker-tutorial
        FaceDetector realTimeDetector = new FaceDetector.Builder(context)
                .setTrackingEnabled(true)
                .build();
        //
        // 3. Create a FaceDetector object for detecting faces on a static photo
        mStaticFaceDetector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();
        //
        // 4. Create a GraphicFaceTrackerFactory
        GraphicFaceTrackerFactory graphicFaceTrackerFactory = new GraphicFaceTrackerFactory();
        //
        // 5. Pass the GraphicFaceTrackerFactory to
        //    a MultiProcessor.Builder to create a MultiProcessor
        MultiProcessor<Face> multiProcessor = new MultiProcessor.Builder<>(graphicFaceTrackerFactory).build();
        //
        // 6. Associate the MultiProcessor with the real time detector
        realTimeDetector.setProcessor(multiProcessor);
        //
        // 7. Check if the real time detector is operational
        if (!realTimeDetector.isOperational()) {
            Log.w(TAG, "still downloading face api");
        }
        //
        // 8. Create a camera source to capture video images from the camera,
        //    and continuously stream those images into the detector and
        //    its associated MultiProcessor
        mCameraSource = new CameraSource.Builder(context, realTimeDetector)
                .setRequestedPreviewSize(850, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        finish();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay, getApplicationContext());
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }
}
