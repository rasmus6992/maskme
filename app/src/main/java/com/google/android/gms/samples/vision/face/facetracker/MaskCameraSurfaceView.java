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
}
