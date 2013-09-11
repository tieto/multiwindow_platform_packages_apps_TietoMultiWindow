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

import android.os.Bundle;
import android.os.RemoteException;
import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IMultiwindowManager;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import com.tieto.multiwindow.ResizeLayer.ResizeLayerListener;

import java.util.ArrayList;

public class MultiWindow extends Activity {

    private static final IActivityManager mService = ActivityManagerNative.getDefault();
    private static final String TAG = "TMW";
    private Rect mDisplaySize = new Rect();
    private ResizeLayer mSlidingPanel = null;
    State mState = new State();
    private int mMultiwindowAppStackId;
    private int mFormerPosition;
    private int mMinPos;
    private int mMaxPos;
    private TabContainer mTabContainer;
    private ViewGroup mGhostLayer;

    /**
     * Implementation of the onRemovedMethod
     */
    private final IMultiwindowManager.Stub mMultiwindowBinder = new IMultiwindowManager.Stub() {
        public void onWindowRemoved(final int stackId) {
            Log.v(TAG,"onWindowRemoved caught. StackId: " + stackId);
            mSlidingPanel.post(new Runnable() {
                @Override
                public void run() {
                    mTabContainer.removeWindow(stackId);
                    if (mState.mExpanded && mTabContainer.currentTabWindowCount() == 0) {
                        setPosition(mState.mRightSide ? mDisplaySize.right : mDisplaySize.left);
                    }
                }
            });
        }
        public void onWindowRelayout(final int stackId, Rect r) {
            Log.v(TAG,"onWindowRelayout caught. StackId: " + stackId + " " + r);
            final Rect rect = r;
            mSlidingPanel.post(new Runnable() {
                @Override
                public void run() {
                    Window w = mTabContainer.getWindowByStackID(stackId);
                    if(w != null)
                        w.setRect(rect);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mDisplaySize.left = 0;
        mDisplaySize.right = metrics.widthPixels;
        mDisplaySize.top = getStatusBarHeight();
        mDisplaySize.bottom = metrics.heightPixels;
        mMinPos = metrics.widthPixels/4;
        mMaxPos = metrics.widthPixels/2;
        mFormerPosition = mDisplaySize.right - mMinPos;
        GhostView.OFFSET_X = mDisplaySize.left;
        GhostView.OFFSET_Y = mDisplaySize.top;
        mSlidingPanel = (ResizeLayer) findViewById(R.id.resize_layer);
        mSlidingPanel.setResizingEnabled(false);

        if (mTabContainer == null) {
            mTabContainer = new TabContainer(mState, this, (ViewGroup) findViewById(R.id.tab_buttons));
        } else {
            Log.e(TAG,"mTabContainer was initialized before...");
        }

        try {
            mMultiwindowAppStackId = mService.getCornerstoneWindowStackId();
            setPosition(mDisplaySize.right);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

        mSlidingPanel.setResizeLayerListener(new ResizeLayerListener() {
            @Override
            public void onResizeEvent(int x_pos) {
                // If resizing is disable, onResizeEvent is not called
                resizeSlidingPanel(x_pos);
            }
        });
        mGhostLayer = (ViewGroup)findViewById(R.id.ghost_layer);

        // mMultiwindowBinder becomes our default MultiwindowManager. Required for window removal callback
        try{
            mService.setMultiwindowManager(mMultiwindowBinder);
            mService.setMultiwindowRelayoutRestriction(mState.mDockedMode);
        } catch (RemoteException e){
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        if (mTabContainer != null) {
            mTabContainer.clearAll();
        }
    }

    @Override
    public void onBackPressed() {
        // overload to prevent app finish 
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
           result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
     }

    public void addWindow(View v) {
        if (!mTabContainer.addWindow(mGhostLayer)) {
            CharSequence text = getText(R.string.maxNoOfWindows);
            showMessage(text);
        }
        if (mState.mExpanded && mTabContainer.currentTabWindowCount() == 1) {
            setPosition(mFormerPosition);
        }
    }

    /**
     * Method handles hiding/expanding the panel when in EXPANDED mode.
     * Otherwise, event is ignored.
     */
    public void onToggle(View v) {
        int position;
        if (!mState.mDockedMode) {
            mState.mFloatingEdit = !mState.mFloatingEdit;
            position = mState.mRightSide ? mDisplaySize.right : mDisplaySize.left;
        } else {
            mState.mExpanded = !mState.mExpanded;
            mSlidingPanel.setResizingEnabled(mState.mExpanded);
            if (mState.mExpanded && mTabContainer.currentTabWindowCount() != 0) {
                position = mFormerPosition;
            } else {
                if (mState.mExpanded) {
                    CharSequence text = getText(R.string.cant_expand_no_window);
                    showMessage(text);
                }
                position = mState.mRightSide ? mDisplaySize.right : mDisplaySize.left;
            }
        }
        setPosition(position);
    }

    /**
     * Method changes app operation modes between DOCKED_RIGHT and DOCKED_LEFT side
     */
    public void onModeChange(View v) {
        mState.mRightSide = !mState.mRightSide;
        int new_pos;
        new_pos = mDisplaySize.right - mFormerPosition;
        mFormerPosition = new_pos;
        LayoutParams ghostLp = (LayoutParams) mGhostLayer.getLayoutParams();
        LayoutParams slidingLp = (LayoutParams) mSlidingPanel.getLayoutParams();
        if (mState.mRightSide) {
            ghostLp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            ghostLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            ghostLp.removeRule(RelativeLayout.RIGHT_OF);
            ghostLp.addRule(RelativeLayout.LEFT_OF, mSlidingPanel.getId());
            slidingLp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
            slidingLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            GhostView.OFFSET_X = 0;
        } else {
            ghostLp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
            ghostLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            ghostLp.removeRule(RelativeLayout.LEFT_OF);
            ghostLp.addRule(RelativeLayout.RIGHT_OF, mSlidingPanel.getId());
            slidingLp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            slidingLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            GhostView.OFFSET_X = mSlidingPanel.getWidth();
        }
        mGhostLayer.setLayoutParams(ghostLp);
        mSlidingPanel.setLayoutParams(slidingLp);
        if (!mState.mExpanded || mTabContainer.currentTabWindowCount() == 0) {
            new_pos = mState.mRightSide ? mDisplaySize.right : mDisplaySize.left;
        }
        Log.v(TAG,"setting position: " + new_pos);
        setPosition(new_pos);
    }

    /**
     * Method changes app operation modes between FLOATING and DOCKED
     */
    public void onEditModeChange(View v) {
        // onEditModeChange
        int new_pos;
        if (mState.mDockedMode) {
            //got into floating mode
            mSlidingPanel.setResizingEnabled(false);
            mState.mDockedMode = false;
            mState.mExpanded = false;
            mState.mFloatingEdit = false;
            new_pos = mState.mRightSide ? mDisplaySize.right : mDisplaySize.left;
            Log.v(TAG,"Switching from DOCKED to FLOATING");
        } else {
            //got into expanded, docked mode
            mSlidingPanel.setResizingEnabled(true);
            mState.mDockedMode = true;
            mState.mExpanded = true;
            mState.mFloatingEdit = false;
            new_pos = mState.mRightSide ? mDisplaySize.right - mMinPos : mDisplaySize.left + mMinPos;
            mFormerPosition = new_pos;
            Log.v(TAG,"Switching from FLOATING to DOCKED");
        }
        try {
            mService.setMultiwindowRelayoutRestriction(mState.mDockedMode);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        setPosition(new_pos);
    }

    /**
     * Method adds new tab (if possible)
     */
    public void onAddTab(View v) {
        if(!mTabContainer.addTab()) {
            CharSequence text = getText(R.string.tooManyTabs);
            showMessage(text);
        } else {
            if (mState.mDockedMode) {
                mTabContainer.addWindow(mGhostLayer);
            }
        }
    }

    public void onGhostLayerClick(View v) {
        if (!mState.mFloatingEdit) {
            return;
        }
        mState.mFloatingEdit = false;
        setPosition(mState.mRightSide ? mDisplaySize.right : mDisplaySize.left);
    }

    /**
     * Method handles touch-resize and sets sliding bar as well as windows into new positions
     */
    private void resizeSlidingPanel(int x_pos) {
        if (!mState.mDockedMode || !mState.mExpanded) {
            return;
        }
        int new_pos = mFormerPosition;
        if (mState.mRightSide) {
            if (x_pos > mDisplaySize.right - mMinPos) {
                new_pos = mDisplaySize.right - mMinPos;
            } else if (x_pos < mDisplaySize.right - mMaxPos) {
                new_pos = mDisplaySize.right -(mMaxPos);
            } else {
                new_pos = x_pos;
            }
        } else {
            if (x_pos < mMinPos) {
                new_pos = mMinPos;
            } else if (x_pos > mMaxPos) {
                new_pos = mMaxPos;
            } else {
                new_pos = x_pos;
            }
        }
        setPosition(new_pos);
        mFormerPosition = new_pos;
    }

    /**
     * Sets position of the sliding bar depending on WorkingMode
     */
    private void setPosition(int x_pos) {
        try {
            Rect rmw, rlaun, rcont;
            rmw = new Rect(mDisplaySize);
            rlaun = new Rect(mDisplaySize);
            rcont = new Rect(mDisplaySize);
            int width = mSlidingPanel.getLayoutParams().width;
            if (mTabContainer.currentTabWindowCount() == 0) {
                x_pos = mState.mRightSide ? mDisplaySize.right : mDisplaySize.left;
            }
            if (mState.mRightSide) {
                rlaun.right = x_pos - width;
                if (!mState.mFloatingEdit) {
                    rmw.left = x_pos - width;
                }
                rmw.right = x_pos;
                if (!mState.mDockedMode) {
                    rcont.right = x_pos - width;
                } else {
                    if (!mState.mExpanded) {
                        rcont.right = x_pos + rcont.width();
                    }
                    rcont.left = x_pos;
                }
            } else { //MultiWindow on left
                rlaun.left = x_pos + width;
                rmw.left = x_pos;
                if (!mState.mFloatingEdit) {
                    rmw.right = x_pos + width;
                }
                if (!mState.mDockedMode) {
                    rcont.left = x_pos + width;
                } else {
                    if (!mState.mExpanded) {
                        rcont.left = x_pos - rcont.width();
                    }
                    rcont.right = x_pos;
                }
            }
            mState.stateChanged();
            mService.relayoutWindow(mMultiwindowAppStackId, rmw);
            mService.relayoutWindow(mService.getMainWindowStackId(), rlaun);
            mTabContainer.relayoutWindows(rcont);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Purpose of this class is to hold application state
     */
    class State {
        /**
         * List contains information about who wants to get notification
         * that state has changed
         */
        private ArrayList<StateChangedListener> mStateChangedListenerList = new ArrayList<StateChangedListener>();
        boolean mDockedMode = true;
        boolean mRightSide = true;
        boolean mExpanded = false;
        boolean mFloatingEdit = false;

        void stateChanged() {
            for (StateChangedListener sc : mStateChangedListenerList ) {
                sc.onStateChanged();
            }
        }

        void addStateChangedListener(StateChangedListener sc) {
            mStateChangedListenerList.add(sc);
        }

        void removeStateChangedListener(StateChangedListener sc) {
            mStateChangedListenerList.remove(sc);
        }
    }

    interface StateChangedListener {
        public void onStateChanged();
    }

    private void showMessage(CharSequence message) {
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, message, duration);
        toast.show();
    }
}
