package com.awo.mycameraxstudy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.awo.mycameraxstudy.mtcnn.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class BoxView extends View {


    Rect drawRect = null;

    Vector<Box> drawBoxs;

    List<Rect> drawRects;

    public BoxView(Context context) {
        super(context);
    }

    public BoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
//        this.mContext = context;
    }


    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (drawRect != null) {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(10);
            canvas.drawRect(drawRect, paint);
        }

        if (drawBoxs != null) {
            for (Box box : drawBoxs) {
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setAntiAlias(true);
                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(10);
                canvas.drawRect(scaleRect(box.transform2Rect(), 480, 640), paint);
            }
        }

        if (drawRects != null) {
            for (Rect rect : drawRects) {
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setAntiAlias(true);
                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(10);
                canvas.drawRect(scaleRect(rect, 480, 640), paint);
            }
        }

    }

    public void setDrawRect(Rect rect) {
        drawRect = rect;
        invalidate();
    }

    public void setBox(Vector<Box> boxes) {
        drawBoxs = boxes;
        invalidate();
    }

    public void setRects(List<Rect> rects) {
        drawRects = rects;
        invalidate();
    }

    private Rect scaleRect (Rect rect, int width, int height) {
        Log.d("MAIN", "image size " + width + " " + height);
        Log.d("MAIN", "boxview size " + getWidth() + " " + getHeight());
        float scale = (float)getHeight() /  (float)height;
        float showWidth = width * scale;
        float startx = (getWidth() - showWidth)/2;

        Rect newRect = new Rect();
        newRect.top = (int) (scale * rect.top);
        newRect.left = (int) (startx + (int) (scale * rect.left));
        newRect.bottom = (int) (scale * rect.bottom);
        newRect.right = (int) ((int) (scale * rect.right) + startx);
        return newRect;

    }
}
