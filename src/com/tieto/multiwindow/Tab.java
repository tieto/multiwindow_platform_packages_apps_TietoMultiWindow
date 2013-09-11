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

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;
import android.view.ViewGroup;

import com.tieto.multiwindow.Window;

/**
 * Class gives Tab functionality and therefore is container for windows.
 *
 */
public class Tab extends ArrayList<Window> {

    private boolean mIsActive = true;
    private MultiWindow.State mState;
    private Rect mPosition;
    private static final int mMaxWindows = 4;
    private static final String TAG = "TMW";
    private static final Rect mInitialPosition = new Rect(4000, 4000, 4800, 4800);

    /**
     * Method sets state of the Tab
     */
    public Tab(MultiWindow.State state) {
        mState = state;
        mPosition = new Rect(mInitialPosition);
    }

    public void removeTab() throws RemoteException {
        try {
            for (Window w : this) {
                w.removeWindow();
            }
            clear();
        } catch (RemoteException e) {
            Log.e(TAG,"Error removing tab");
        }
    }

    protected void finalize() throws Throwable {
        Log.v(TAG,"finalize, Tab");
        removeTab();
    }

    /**
     * Method sets state of the Tab
     */
    public void setActive(boolean state) {
        // probably set Active should also make windows (in)visible
        mIsActive = state;
    }

    /**
     * Method gets state of the Tab
     */
    public boolean isActive() {
        return mIsActive;
    }

    /**
     * Method adds window to Tab
     */
    public boolean addWindow (Context context, ViewGroup ghostLayer) {
        if (size() < mMaxWindows && mIsActive) {
            add(new Window(new Rect(mInitialPosition), context, ghostLayer));
            resizeWindows(mPosition);
            return true;
        }
        return false;
    }

    void requestLayout() {
        resizeWindows(mPosition);
    }

    /**
     * Method removes window from Tab
     */
    public boolean removeWindow (int stackId) {
        // I look for specific window in current tab.
        // If found it gets removed and remaining windows get resized.
        for (Window w : this) {
            if (w.getStackId() == stackId) {
                w.windowRemovedExternally();
                remove(w);
                resizeWindows(mPosition);
                return true;
            }
        }
        return false;
    }

    void setVisibility(boolean vis) {
        for(Window w : this) {
            w.setVisibility(vis);
        }
    }

    /**
     * Method resizes windows contained in tab according to parameters given
     */
    public void resizeWindows (Rect r) {
        mPosition = r;
        Rect rw = new Rect();
        // handling window resizing in docked mode
        if (!mIsActive) {
            for (int i=0; i<size(); i++) {
                // introduced without having checked out
                get(i).setVisibility(false);
            }
        } else {
            if (mState.mDockedMode) {
                for (int i=0; i<size(); i++) {
                    rw.set(r.left, r.top + i*r.height()/size(), r.right, r.top + (i+1)*r.height()/size());
                    get(i).resize(rw);
                    get(i).setVisibility(true);
                }
            //floating mode here
            } else {
                int dWidth = (r.right - r.left)/10;
                int dHeight = (r.bottom - r.top)/10;
                for (int i=0; i<size(); i++) {
                    // introduced without having checked out
                    if (!get(i).restoreLastFloatingPosition()) {
                        rw.set(r.left + (4-i)*dWidth, r.top + (i+1)*dHeight, r.right - (i+1)*dWidth, r.bottom - (4-i)*dHeight);
                        get(i).resize(rw);
                        get(i).setVisibility(true);
                        Log.v(TAG,"firsttimeTAB");
                    }
                }
            }
        }
    }
}
