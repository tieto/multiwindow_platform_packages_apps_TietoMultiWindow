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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tieto.multiwindow.Tab;
import com.tieto.multiwindow.TabButton;

public class TabContainer extends ArrayList<Tab> {

    private static final String TAG = "TMW";
    private Rect mPosition;
    private MultiWindow.State mState;
    private ViewGroup mTabsButtonsLayout;
    private Context mContext;
    private Tab mCurrentTab = null;

    // HAS TO TAKE THE VALUES FROM THE SETTINGS
    private int mMinTabNumber = 1;
    private int mMaxTabNumber = 4;

    /**
     * Method initializes tab container
     *
     */
    public TabContainer (MultiWindow.State state, Context context, ViewGroup ll) {
        mContext = context;
        mState = state;
        int i = 0;
        mPosition = new Rect(-3000,0,0,1000);
        mTabsButtonsLayout = ll;
        addTab();
    }

    /**
     * Method adds tab to TabContainer
     *
     */
    public boolean addTab () {
        if (size() < mMaxTabNumber) {
            //deactivating all the other tabs...
            for (int i = 0; i < size(); i++) {
                get(i).setActive(false);
                ((TabButton) mTabsButtonsLayout.getChildAt(i)).setActive(false);
            }
            //... so that the new one could get active
            Tab t = new Tab(mState);
            add(t);

            TabButton button = new TabButton(mContext, mState);
            button.updateGraphics();
            button.setTab(t);

            // Implementing method for tab switch on - OnClick
            button.setOnSwitchButtonClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleSwitchTab(v);
                }
            });

            // Implementing method for tab removal - OnClick
            button.setOnRemoveButtonClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleRemoveTab(v);
                }
            });

            // Add button to the visible layout
            mTabsButtonsLayout.addView(button);
            switchTab(t);
            button.setActive(true);
            Log.v(TAG,"Added " + size() + "th tab");
            return true;
        }
        return false;
    }

    public void handleRemoveTab(View v) {
        if (size() <= mMinTabNumber) {
            Log.v(TAG,"size > mintabnumber");
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(mContext, mContext.getText(R.string.lastTab), duration);
            toast.show();
            return;
        }
        TabButton tabButton = (TabButton)v;
        if (tabButton.isActive()) {
            View target;
            if (mTabsButtonsLayout.getChildAt(mTabsButtonsLayout.getChildCount() - 1).equals(v)) {
                target = mTabsButtonsLayout.getChildAt(mTabsButtonsLayout.getChildCount() - 2);
            } else {
                target = mTabsButtonsLayout.getChildAt(mTabsButtonsLayout.indexOfChild(v) + 1);
            }
            handleSwitchTab(target);
        }
        try {
            tabButton.getTab().removeTab();
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        remove(tabButton.getTab());
        mTabsButtonsLayout.removeView(v);
    }

    public void handleSwitchTab(View v) {
        switchTab(((TabButton) v).getTab());
        for (int i = 0; i < mTabsButtonsLayout.getChildCount(); i++) {
            TabButton tab = ((TabButton) mTabsButtonsLayout.getChildAt(i));
            tab.setActive(v.equals(mTabsButtonsLayout.getChildAt(i)));
        }
    }

    public void clearAll() {
        for (int i=0; i<size(); i++) {
            try {
                get(i).removeTab();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
        clear();
    }

    public void switchTab (Tab index) {
        Log.v(TAG,"Switch tab for number " + index);
        for (Tab i : this) {
            i.setActive(i.equals(index));
        }
        mCurrentTab = index;
        relayoutWindows(mPosition);
    }

    /**
     * Method adds tab to TabContainer
     */
    public boolean addWindow (ViewGroup ghostLayer) {
        for (Tab t : this) {
            if (t.isActive()) {
                return t.addWindow(mContext, ghostLayer);
            }
        }
        return false;
    }

    /**
     * Method removes window with given StackId
     */
    public boolean removeWindow (int stackId) {
        // I look for the window in all available tabs.
        // If found, it gets removed and the search stops.
        for (Tab t : this) {
            if (t.removeWindow(stackId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method relayouts positions of tabs and windows
     */
    public void relayoutWindows(Rect pos) {
        mPosition = pos;
        Log.v(TAG,"relayout");
        for (int i=0; i<size(); i++) {
            get(i).resizeWindows(pos);
            ((TabButton) mTabsButtonsLayout.getChildAt(i)).updateGraphics();
        }
    }

    public Window getWindowByStackID(int stackId){
        for (int i=0; i<size(); i++) {
            Tab tab = get(i);
            for(int j=0;j<tab.size(); j++){
                if(tab.get(j).getStackId() == stackId)
                    return tab.get(j);
            }
        }
        return null;
    }

    public int currentTabWindowCount() {
        if (mCurrentTab == null) {
            return 0;
        }
        return mCurrentTab.size();
    }

}