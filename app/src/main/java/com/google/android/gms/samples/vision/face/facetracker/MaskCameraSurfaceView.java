package com.google.android.gms.samples.vision.face.facetracker;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MaskCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final int LANDSCAPE_ROTATE = 0;
    private static final int PORTRAIT_ROTATE = 90;

    private final SurfaceHolder mHolder;

    /* Use android.hardware.Camera2 if you are going for a higher version number! */
    private Camera mCamera;

    private int mRotation = 0;

    public MaskCameraSurfaceView(final Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setWillNotDraw(false);
        mCamera = Camera.open();

        try {
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.stopPreview();

        final Camera.Parameters parameters = mCamera.getParameters();
        final Camera.Size previewSize = parameters.getSupportedPreviewSizes().get(0);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        if (width > height) {
            mCamera.setDisplayOrientation(LANDSCAPE_ROTATE);
            mRotation = LANDSCAPE_ROTATE;
        }
        else {
            mCamera.setDisplayOrientation(PORTRAIT_ROTATE);
            mRotation = PORTRAIT_ROTATE;
        }

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    public void capture(final Camera.PictureCallback imageHandler) {
        /* your code here: take picture with camera and call the passed imageHandler */

        mCamera.takePicture(null, null, imageHandler);

    }



    public void startPreview() {
        /* your code here: start preview of camera */
        mHolder.setType(mHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCamera.startPreview();
    }

    public int getCurrentRotation() {
        /* your code here: get the current rotation */


        return mRotation;
    }

    public void stopCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
