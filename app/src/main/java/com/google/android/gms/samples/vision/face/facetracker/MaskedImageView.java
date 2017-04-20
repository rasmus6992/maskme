package com.google.android.gms.samples.vision.face.facetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.util.Log;
import android.widget.ImageView;

public class MaskedImageView extends ImageView {
    FaceDetector.Face[] faces = null;
    int imageWidth;
    int imageHeight;
    public int mode;

    public MaskedImageView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float scaleX = (float) canvas.getWidth() / (float) imageWidth;
        final float scaleY = (float) canvas.getHeight() / (float) imageHeight;
        final float transX = ((float) canvas.getWidth() - scaleX * imageWidth) / 2f;
        final float transY = ((float) canvas.getHeight() - scaleY * imageHeight) / 2f;

        if (faces != null) {

            for(int i = 0; i < faces.length; i++) {
                Log.i("length", String.valueOf(faces.length));
                Log.i("The ith", String.valueOf(i));
                final PointF midpoint = new PointF();
                faces[i].getMidPoint(midpoint);
                final float x = scaleX * midpoint.x + transX;
                final float y = scaleY * midpoint.y + transY;
                final float faceSize = scaleX * faces[i].eyesDistance();

                if(mode == 1)
                    drawMask(x, y, faceSize, canvas);
                else if(mode == 2)
                    changeMask(x, y, faceSize, canvas);
                else
                    blue(x, y, faceSize, canvas);
            }
        } else {
            Log.i("ImageFaces", "no faces");
        }
    }

    private void blue(final float x, final float y, final float faceSize, final Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        canvas.drawCircle(x, y, faceSize, paint);
    }

    private void changeMask(final float x, final float y, final float faceSize, final Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        canvas.drawCircle(x, y, faceSize, paint);
    }

    private void drawMask(final float x, final float y, final float faceSize, final Canvas canvas) {
        /* your code here: draw a mask over the image */
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        canvas.drawCircle(x, y, faceSize, paint);

    }

    public void maskFaces(FaceDetector.Face[] faces, int count, int width, int height) {
        imageWidth = width;
        imageHeight = height;

        this.faces = new FaceDetector.Face[count];

        System.arraycopy(faces, 0, this.faces, 0, count);
    }

    public void noFaces() {
        faces = null;
    }

    public void reset() {
        faces = null;
        setImageBitmap(null);
    }
}
