package edu.ucsb.ece.ece150.maskme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

public class MaskedImageView extends android.support.v7.widget.AppCompatImageView{

    private enum MaskType {
        NOMASK, FIRST, SECOND
    }

    private SparseArray<Face> faces = null;
    private MaskType maskType = MaskType.NOMASK;
    Paint mPaint = new Paint();
    private Bitmap mBitmap;
    private Context mContext;

    public MaskedImageView(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mBitmap = ((BitmapDrawable) getDrawable()).getBitmap();
        if(mBitmap == null){
            return;
        }
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

        drawBitmap(canvas, scale);

        switch (maskType){
            case FIRST:
                drawFirstMaskOnCanvas(canvas, scale);
                break;
            case SECOND:
                drawSecondMaskOnCanvas(canvas, scale);
                break;
        }
    }

    protected void drawFirstMask(SparseArray<Face> faces){
        this.faces = faces;
        this.maskType = MaskType.FIRST;
        this.invalidate();
    }

    protected void drawSecondMask(SparseArray<Face> faces){
        this.faces = faces;
        this.maskType = MaskType.SECOND;
        this.invalidate();
    }

    private void drawBitmap(Canvas canvas, double scale) {
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();

        Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale), (int)(imageHeight * scale));
        canvas.drawBitmap(mBitmap, null, destBounds, null);
    }

    private void drawFirstMaskOnCanvas(Canvas canvas, double scale) {

        // TODO: Draw first type of mask on the static photo
        // 1. set properties of mPaint
        // 2. get positions of faces and draw masks on faces.

        // facing me
        // landmark 0 is left eye
        // landmark 1 is right eye
        // landmark 2 is nose
        // landmark 3 is right cheek
        // landmark 4 is left cheek

        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            // draw nose
            drawEmoji(face, canvas, scale, 2, R.drawable.pig_nose);
            // draw eyes
            drawEmoji(face, canvas, scale, 0, R.drawable.emoji_eye);
            drawEmoji(face, canvas, scale, 1, R.drawable.emoji_eye);
        }
    }

    private void drawSecondMaskOnCanvas( Canvas canvas, double scale ) {
        // TODO: Draw second type of mask on the static photo
        // 1. set properties of mPaint
        // 2. get positions of faces and draw masks on faces.

        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            // draw nose
            drawEmoji(face, canvas, scale, 2, R.drawable.shit);
        }

    }

    private void drawEmoji(Face face, Canvas canvas, double scale, int index, int drawable){
        Landmark landmark = face.getLandmarks().get(index);
        int cx = (int) (landmark.getPosition().x * scale);
        int cy = (int) (landmark.getPosition().y * scale);
        Bitmap mask= BitmapFactory.decodeResource(mContext.getResources(), drawable);
        float newX = cx - mask.getWidth()/2;
        float newY = cy - mask.getHeight()/2;
        canvas.drawBitmap(mask, newX,newY, mPaint);
    }

    public void noFaces() {
        faces = null;
    }

    public void reset() {
        faces = null;
        setImageBitmap(null);
    }
}
