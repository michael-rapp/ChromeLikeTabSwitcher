/*
 * Copyright 2016 - 2017 Michael Rapp
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package de.mrapp.android.tabswitcher.gesture;

import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * An event handler, which allows to handle swipe gestures, which can be used to switch between
 * tabs.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class SwipeEventHandler extends AbstractTouchEventHandler {

    /**
     * Creates a new event handler, which allows to handle swipe gestures, which can be used to
     * switch between tabs.
     *
     * @param tabSwitcher
     *         The tab switcher, whose tabs should be switched, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param dragThreshold
     *         The drag threshold in pixels as an {@link Integer} value. The drag threshold must be
     *         at least 0
     * @param touchableArea
     *         The bounds of the onscreen area, the handler should take into consideration for
     *         handling touch events, as an instance of the class {@link RectF} or null, if the are
     *         should not be restricted
     */
    public SwipeEventHandler(@NonNull final TabSwitcher tabSwitcher, final int dragThreshold,
                             @Nullable final RectF touchableArea) {
        super(MAX_PRIORITY, touchableArea, tabSwitcher, dragThreshold);
    }

    @Override
    protected final boolean isDraggingAllowed() {
        return !getTabSwitcher().isSwitcherShown() && getTabSwitcher().getCount() > 1;
    }

    @Override
    protected final void onTouchEvent() {
        // notifyOnCancelFling();
    }

    @Override
    protected final void onDown(@NonNull final MotionEvent event) {

    }

    @Override
    protected final void onDrag(@NonNull final MotionEvent event) {

    }

    @Override
    protected final void onUp(@Nullable final MotionEvent event, final int dragThreshold) {

    }

}