package com.awo.mycameraxstudy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BoxView extends View {


    Rect drawRect = null;

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

    }

    public void setDrawRect(Rect rect) {
        drawRect = rect;
        invalidate();
    }
}
