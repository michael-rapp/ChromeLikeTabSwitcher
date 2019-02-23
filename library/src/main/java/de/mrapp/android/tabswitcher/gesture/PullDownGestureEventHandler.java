/*
 * Copyright 2016 - 2019 Michael Rapp
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
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * An event handler, which allows to handle pull down gestures, which can be used to show the tab
 * switcher by pulling down the currently selected tab, when using the smartphone layout.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class PullDownGestureEventHandler extends AbstractDragGestureEventHandler {

    /**
     * Defines the interface, a class, which should be notified about the events of a {@link
     * PullDownGestureEventHandler} , must implement.
     */
    public interface Callback {

        /**
         * The method, which is notified, when the currently selected tab has been pulled down.
         */
        void onPulledDown();

    }

    /**
     * The previous drag position.
     */
    private float previousDragPosition;

    /**
     * The callback, which is notified about the event handler's events.
     */
    private Callback callback;

    /**
     * Notifies the callback, that the currently selected tab has been pulled down.
     */
    private void notifyOnPulledDown() {
        if (callback != null) {
            callback.onPulledDown();
        }
    }

    /**
     * Creates a new event handler, which allows to handle pull down gestures, which can be used to
     * show the tab switcher by pulling down the currently selected tab, when using the smartphone
     * layout.
     *
     * @param tabSwitcher
     *         The tab switcher, the event handler belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param dragThreshold
     *         The threshold of the drag helper, which is used to recognize drag gestures, in pixels
     *         as an {@link Integer} value The threshold must be at least 0
     * @param touchableArea
     *         The bounds of the onscreen area, the handler should take into consideration for
     *         handling touch events, as an instance of the class {@link RectF} or null, if the are
     */
    public PullDownGestureEventHandler(@NonNull final TabSwitcher tabSwitcher,
                                       final int dragThreshold,
                                       @Nullable final RectF touchableArea) {
        super(tabSwitcher, dragThreshold, touchableArea);
        this.previousDragPosition = -1;
        this.callback = null;
    }

    /**
     * Sets the callback, which should be notified about the event handler's events.
     *
     * @param callback
     *         The callback, which should be set, as an instance of the type {@link Callback} or
     *         null, if no callback should be notified
     */
    public final void setCallback(@Nullable final Callback callback) {
        this.callback = callback;
    }

    @Override
    protected final boolean isDraggingAllowed() {
        return getTabSwitcher().getLayout() != Layout.TABLET &&
                !getTabSwitcher().isSwitcherShown() && getTabSwitcher().getSelectedTab() != null;
    }

    @Override
    protected final void onTouchEvent() {

    }

    @Override
    protected final void onDown(@NonNull final MotionEvent event) {

    }

    @Override
    protected final void onDrag(@NonNull final MotionEvent event) {
        float dragPosition = event.getY();

        if (dragPosition > previousDragPosition) {
            previousDragPosition = dragPosition;
            getDragHelper().update(dragPosition);

            if (getDragHelper().hasThresholdBeenReached()) {
                onUp(null);
                notifyOnPulledDown();
            }
        } else {
            getDragHelper().reset();
            previousDragPosition = -1;
        }
    }

    @Override
    protected final void onUp(@Nullable final MotionEvent event) {
        previousDragPosition = -1;
        reset();
    }

}