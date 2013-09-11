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
import android.widget.Button;

import com.tieto.multiwindow.MultiWindow.State;
import com.tieto.multiwindow.MultiWindow.StateChangedListener;

public class CustomButton extends Button implements StateChangedListener {

    private static final int[] STATE_RIGHT = { R.attr.state_right };
    private static final int[] STATE_DOCKED = { R.attr.state_docked };
    private static final int[] STATE_EXPANDED = { R.attr.state_expanded };
    private State mState = null;

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mState != null) {
            return;
        }
        Context context = getContext();
        if (context instanceof MultiWindow) {
            MultiWindow mw = (MultiWindow) context;
            mState = mw.mState;
            mState.addStateChangedListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mState != null) {
            mState.removeStateChangedListener(this);
            mState = null;
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if (mState == null) {
            return super.onCreateDrawableState(extraSpace);
        }
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 3);
        if (mState.mRightSide) {
            mergeDrawableStates(drawableState, STATE_RIGHT);
        }
        if (mState.mDockedMode) {
            mergeDrawableStates(drawableState, STATE_DOCKED);
        }
        if (mState.mExpanded) {
            mergeDrawableStates(drawableState, STATE_EXPANDED);
        }
        return drawableState;
    }

    public void onStateChanged() {
        this.post(new Runnable() {

            @Override
            public void run() {
                refreshDrawableState();
            }
        });
    }
}
