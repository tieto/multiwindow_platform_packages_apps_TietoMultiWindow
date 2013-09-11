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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * Purpose of this class is catch all move events and resize TMW.
 */
public class ResizeLayer extends RelativeLayout {

    /**
     * Implement this interface to get resize events
     */
    public interface ResizeLayerListener {
        void onResizeEvent(int x_pos);
    }

    private final static int mMinimalMove = 25;
    private int mFirstXTouch = -1;
    private ResizeLayerListener mResizeLayerListener = null;
    private boolean mResizingEnabled = false;

    public ResizeLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        //on default resize bar lands onto the right side
    }

    public void setResizeLayerListener(ResizeLayerListener rll) {
        mResizeLayerListener = rll;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            return true;
        }
        int x = (int)event.getRawX();
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (mResizeLayerListener != null) {
                mResizeLayerListener.onResizeEvent(x);
            }
        }
        if ((event.getActionMasked() == MotionEvent.ACTION_UP)) {
            boolean ret = Math.abs(mFirstXTouch - x) > mMinimalMove;
            mFirstXTouch = -1;
            return ret;
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mResizeLayerListener == null) {
            return false;
        }
        if (mResizingEnabled == false) {
            return false;
        }
        if ((event.getActionMasked() == MotionEvent.ACTION_DOWN)) {
            mFirstXTouch = (int)event.getRawX();
            return false;
        }
        int x = (int)event.getRawX();
        if (((event.getActionMasked() == MotionEvent.ACTION_MOVE))
            && (Math.abs(mFirstXTouch - x) > mMinimalMove)) {
            return true;
        }
        if ((event.getActionMasked() == MotionEvent.ACTION_UP)) {
            mFirstXTouch = -1;
        }
        return false;
    }

    /**
     * Method sets resizing enabled/disabled depending on the value given
     */
    public void setResizingEnabled(boolean val) {
        mResizingEnabled = val;
    }

    /**
     * Method returns if resizing of the window is enabled
     */
    public boolean isResizingEnabled() {
        return mResizingEnabled;
    }
}
