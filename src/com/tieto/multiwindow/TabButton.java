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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class TabButton extends LinearLayout {

    public static final String TAG = "TMW";
    private ImageButton mRemoveButton;
    private ImageButton mSwitchButton;
    private Tab mTab;
    private boolean mIsActive;
    private OnClickListener mOnRemoveButtonClickListener = null;
    private OnClickListener mOnSwitchButtonClickListener = null;
    private MultiWindow.State mState;

    public TabButton(Context context, MultiWindow.State state) {
        super(context);
        mState = state;
        setOrientation(LinearLayout.VERTICAL);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        setLayoutParams(lp);

        mSwitchButton = new ImageButton(context);
        mSwitchButton.setBackgroundResource(R.drawable.tab);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mSwitchButton.setLayoutParams(lp);
        mSwitchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSwitchButton();
            }
        });

        // adding switchpart of the tabbutton
        addView(mSwitchButton);
        mRemoveButton = new ImageButton(context);
        mRemoveButton.setBackgroundResource(R.drawable.remove_tab);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mRemoveButton.setLayoutParams(lp);
        mRemoveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRemoveButton();
            }
        });

        // adding removepart of the tabbutton
        addView(mRemoveButton);
    }

    public void updateGraphics() {
        if (mIsActive) {
            if (mState.mRightSide) {
                mRemoveButton.setBackgroundResource(R.drawable.remove_tab_click);
                mSwitchButton.setBackgroundResource(R.drawable.tab_click);
            } else {
                mRemoveButton.setBackgroundResource(R.drawable.remove_tab_click_left);
                mSwitchButton.setBackgroundResource(R.drawable.tab_click_left);
            }
        } else {
            if (mState.mRightSide) {
                mRemoveButton.setBackgroundResource(R.drawable.remove_tab);
                mSwitchButton.setBackgroundResource(R.drawable.tab);
            } else {
                mRemoveButton.setBackgroundResource(R.drawable.remove_tab_left);
                mSwitchButton.setBackgroundResource(R.drawable.tab_left);
            }
        }
    }

    public boolean isActive() {
        return mIsActive;
    }
    public void setTab(Tab v) {
        mTab = v;
    }

    public Tab getTab() {
        return mTab;
    }

    public void setActive(boolean active) {
        mIsActive = active;
        updateGraphics();
    }

    private void handleRemoveButton() {
        if (mOnRemoveButtonClickListener != null) {
            mOnRemoveButtonClickListener.onClick(this);
        }
    }

    public void setOnRemoveButtonClickListener(OnClickListener listener) {
        mOnRemoveButtonClickListener = listener;
    }

    private void handleSwitchButton() {
        if (mOnSwitchButtonClickListener != null) {
            mOnSwitchButtonClickListener.onClick(this);
        }
    }

    public void setOnSwitchButtonClickListener(OnClickListener listener) {
        mOnSwitchButtonClickListener = listener;
    }
}
