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

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

/**
 * Purpose of this class is to hold all information related to Window
 *
 */
public class Window implements GhostView.WindowRelayoutListener{

    private static final IActivityManager mService = ActivityManagerNative.getDefault();
    private static final String TAG = "TMW";

    /**
     * This point is used for placing window in invisibile place. Window goes
     * there if its mVisibility is set to false;
     */
    private static final Point mInvisiblePoint = new Point(10000, 10000);

    /**
     * Contains minimal window size
     */
    private static final Rect mMinWinSize = new Rect(0, 0, 200, 200);
    private Context mContext;
    private int mStackId;
    private Rect mLastFloatingPosition = null;
    private Rect mPosition;
    private boolean mVisibility = true;
    private ViewGroup mGhostViewLayer;
    private GhostView mGhostView;

    /**
     * Implements constructor, which creates window and starts application in it.
     */
    public Window(Rect position, Context context, ViewGroup ghostViewLayer) {
        String cls = context.getString(R.string.default_app);
        String pkg = cls.substring(0, cls.lastIndexOf('.'));
        ComponentName cn = new ComponentName(pkg, cls);
        Intent intent = new Intent();
        intent.setComponent(cn);
        initWindow(position, context, intent, ghostViewLayer);
    }

    public Window(Rect position, Context context, Intent intent, ViewGroup ghostViewLayer) {
        initWindow(position, context, intent, ghostViewLayer);
    }

    private void initWindow(Rect position, Context context, Intent intent, ViewGroup ghostViewLayer) {
        try {
            mContext = context;
            mGhostViewLayer = ghostViewLayer;
            mPosition = position;
            mStackId = mService.initWindow(position);

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(mPosition.width(), mPosition.height());
            mGhostView = new GhostView(context, this);
            mGhostView.setLayoutParams(layoutParams);
            mGhostViewLayer.addView(mGhostView);

            Log.v(TAG,"window stackid: " + mStackId);
            new AppLauncher(intent).start();
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
    }

    public void windowRemovedExternally() {
        mGhostViewLayer.removeView(mGhostView);
        mGhostView = null;
        mStackId = -1;
    }

    public void removeWindow() throws RemoteException {
        Log.v(TAG,"finalize");
        if (mStackId != -1) {
            try {
                setVisibility(false);
                mService.removeWindow(mStackId);
                windowRemovedExternally();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    /**
     * Method implements quasi-destructor of the Window instance
     * It removes Window having mStackId from MultiWindow stack
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        removeWindow();
    }

    /**
     * Method resizes window to the position and size given
     */
    public void resize(Rect rw) {
        if (rw.width() < mMinWinSize.width()) {
            rw.right = rw.left + mMinWinSize.width();
        }
        if (rw.height() < mMinWinSize.height()) {
            rw.bottom = rw.top + mMinWinSize.height();
        }
        mPosition = rw;
        relayoutInternal();
    }

    public void setRect(Rect r){
        Log.v("onWindowRelayout","setRect:"+r);
        mPosition = r;
        mLastFloatingPosition = new Rect(r);
    }

    /**
     * Method relayout window with respect to visibility.
     */
    private void relayoutInternal() {
        try {
            Rect r;
            if (mVisibility) {
                r = mPosition;
            } else {
                r = new Rect(mInvisiblePoint.x,
                        mInvisiblePoint.y,
                        mInvisiblePoint.x + mPosition.width(),
                        mInvisiblePoint.y + mPosition.height());
            }
            mGhostView.updatePosition(r);
            mService.relayoutWindow(mStackId, r);
            Log.v(TAG, "Relayout window: " + mStackId + " " + r);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Method sets window's visibility to the state given
     */
    public void setVisibility(boolean vis) {
        mVisibility = vis;
        relayoutInternal();
    }

    /**
     * Returns visibility state of the window
     */
    public boolean isVisible () {
        return mVisibility;
    }

    /**
     * Returns Id window stack. Required for window removal
     */
    public int getStackId () {
        return mStackId;
    }

    public boolean restoreLastFloatingPosition() {
        if (mLastFloatingPosition == null) {
            return false;
        } else {
            mPosition = mLastFloatingPosition;
            mVisibility = true;
            relayoutInternal();
        }
        return true;
    }

    @Override
    public void onWindowRelayout(Rect newPos) {
        mLastFloatingPosition = new Rect(newPos);
        resize(newPos);
    }

    /**
     * Adds private class AppLauncher to launch apps in separate threads.
     */
    private class AppLauncher extends Thread {
        private Intent intent;

        AppLauncher(Intent launchIntent) {
            intent = launchIntent;
        }

        @Override
        public void run() {
            try {
                mService.startCornerstoneApp(intent, mStackId);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
