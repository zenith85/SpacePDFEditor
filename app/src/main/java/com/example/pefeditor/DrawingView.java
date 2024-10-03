package com.example.pefeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawingView extends View {
    private Paint paint;
    private Bitmap bitmap;
    private Canvas canvas;
    private float startX, startY;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);
    }

    // Set the bitmap for the PDF page and clear the canvas for drawing
    public void setBitmap(Bitmap pdfBitmap) {
        if (bitmap != null) {
            bitmap.recycle(); // Recycle the old bitmap to free memory
        }
        bitmap = Bitmap.createBitmap(pdfBitmap.getWidth(), pdfBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        canvas.drawBitmap(pdfBitmap, 0, 0, null);

        // Scale the bitmap to fit the view
        bitmap = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(), true);
        // Set the canvas for drawing
        this.canvas = new Canvas(bitmap); // Set the drawing canvas
        invalidate(); // Force a redraw to show the new bitmap
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (bitmap != null) {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
            canvas.setBitmap(scaledBitmap);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float endX = event.getX();
                float endY = event.getY();
                canvas.drawLine(startX, startY, endX, endY, paint);
                startX = endX;
                startY = endY;
                invalidate(); // Redraw the canvas
                return true;
        }
        return super.onTouchEvent(event);
    }

    // Get the bitmap with the drawings
    public Bitmap getBitmap() {
        return bitmap;
    }

    // Method to restore the drawing from the saved bitmap for the specified page index
    public void restoreDrawing(Bitmap savedBitmap) {
        if (savedBitmap != null) {
            bitmap = Bitmap.createBitmap(savedBitmap);
            canvas = new Canvas(bitmap);
            invalidate();
        }
    }
}
