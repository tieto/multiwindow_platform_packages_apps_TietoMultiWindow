/**
 * TietoMultiWindow
 * Copyright (C) 2013 Tieto Poland Sp. z o.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tieto.multiwindow;

import com.tieto.multiwindow.MultiWindow;

import com.tieto.multiwindow.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Button;

public class GhostView extends RelativeLayout {

    public static int OFFSET_X = 0;
    public static int OFFSET_Y = 0;

    private int mLastRawX = 0;
    private int mLastRawY = 0;

    private int mBitmapDim = 0;
    private Paint mPaint = new Paint();
    private Bitmap mBitmap;
    private int mBallId = -1;

    private double mDist = 0;
    private double mScale = 1;

    private WindowRelayoutListener mWindowRelayoutListener;

    public GhostView(Context context, WindowRelayoutListener wrl) {
        super(context);
        mWindowRelayoutListener = wrl;
        setFocusable(true);
        mBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.corner);
        mBitmapDim = mBitmap.getWidth();
        setBackgroundResource(R.color.ghost_color);
    }

    public void updatePosition(Rect position){
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
        lp.leftMargin = position.left - OFFSET_X;
        lp.topMargin = position.top - OFFSET_Y;
        lp.width = position.width();
        lp.height = position.height();
        setLayoutParams(lp);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.ghost_frame_color));
        mPaint.setStrokeWidth(8);

        canvas.drawRect(0, 0, lp.width, lp.height, mPaint);

        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
        canvas.drawBitmap(mBitmap, 0, lp.height-mBitmapDim, mPaint);
        canvas.drawBitmap(mBitmap, lp.width-mBitmapDim, lp.height-mBitmapDim, mPaint);
        canvas.drawBitmap(mBitmap, lp.width-mBitmapDim, 0, mPaint);
    }

    public boolean onTouchEvent(MotionEvent event) {

        final int rawX = (int) event.getRawX();
        final int rawY = (int) event.getRawY();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();

        switch (event.getAction()) {

        case MotionEvent.ACTION_DOWN:
            mLastRawX = rawX;
            mLastRawY = rawY;

            final int X = (int) event.getX();
            final int Y = (int) event.getY();
            mBallId = -1;

            if ((mBitmapDim - X) > 0) { // left edge
                mBallId = 0x1;
            } else if (layoutParams.width - X < mBitmapDim) {
                mBallId = 0x02;
            }
            if (mBallId != -1) {
                if ((mBitmapDim - Y) > 0) { // top
                    mBallId |= 0x4;
                } else if (layoutParams.height - Y < mBitmapDim) {
                    mBallId |= 0x8;
                } else {
                    mBallId = -1;
                }
            }
            break;

        case MotionEvent.ACTION_MOVE:
            if (mBallId > -1) {
                if(mBallId == 0x5){
                    layoutParams.leftMargin += rawX - mLastRawX;
                    layoutParams.topMargin += rawY - mLastRawY;
                    layoutParams.width -= rawX - mLastRawX;
                    layoutParams.height -= rawY - mLastRawY;
                } else if(mBallId == 0x9){
                    layoutParams.leftMargin += rawX - mLastRawX;
                    layoutParams.width -= rawX - mLastRawX;
                    layoutParams.height += rawY - mLastRawY;
                } else if(mBallId == 0x6){
                    layoutParams.topMargin += rawY - mLastRawY;
                    layoutParams.width += rawX - mLastRawX;
                    layoutParams.height -= rawY - mLastRawY;
                } else{
                    layoutParams.width += rawX - mLastRawX;
                    layoutParams.height += rawY - mLastRawY;
                }
                setLayoutParams(layoutParams);
            } else {
                if (event.getPointerCount() >= 2) {

                     float x0 = event.getX(0);
                     float y0 = event.getY(0);
                     float x1 = event.getX(1);
                     float y1 = event.getY(1);

                     double dist = Math.sqrt(Math.pow(x0 - x1, 2) + Math.pow(y0 - y1, 2));

                     switch (event.getAction() & MotionEvent.ACTION_MASK) {
                         case MotionEvent.ACTION_MOVE:
                             if (mDist == 0) {
                                 mDist = dist;
                                 break;
                             }
                             mScale = dist / mDist;
                             mDist = dist;

                             layoutParams.width = (int)(layoutParams.width * mScale);
                             layoutParams.height = (int)(layoutParams.height * mScale);

                             setLayoutParams(layoutParams);
                             break;
                     }
                     return true;
                 } else{
                    layoutParams.leftMargin += rawX - mLastRawX;
                    layoutParams.topMargin += rawY - mLastRawY;
                    setLayoutParams(layoutParams);
                }
            }

            break;

        case MotionEvent.ACTION_UP:
            Rect pos = new Rect(layoutParams.leftMargin + OFFSET_X, layoutParams.topMargin + OFFSET_Y,
                    layoutParams.leftMargin+layoutParams.width + OFFSET_X, layoutParams.topMargin+layoutParams.height + OFFSET_Y);
            if (mWindowRelayoutListener != null) {
                mWindowRelayoutListener.onWindowRelayout(pos);
            }
            mDist = 0;
            break;
        }

        mLastRawX = rawX;
        mLastRawY = rawY;

        invalidate();
        return true;

    }

    public interface WindowRelayoutListener {
        public void onWindowRelayout(Rect newPos);
    }
}