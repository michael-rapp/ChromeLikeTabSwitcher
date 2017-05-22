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
package de.mrapp.android.tabswitcher.layout.phone;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;

import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.iterator.AbstractTabItemIterator;
import de.mrapp.android.tabswitcher.iterator.TabItemIterator;
import de.mrapp.android.tabswitcher.layout.AbstractDragHandler;
import de.mrapp.android.tabswitcher.layout.Arithmetics;
import de.mrapp.android.tabswitcher.layout.Arithmetics.Axis;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.gesture.DragHelper;
import de.mrapp.android.util.view.AttachedViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A drag handler, which allows to calculate the position and state of tabs on touch events, when
 * using the smartphone layout.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class PhoneDragHandler extends AbstractDragHandler<PhoneDragHandler.Callback> {

    /**
     * Defines the interface, a class, which should be notified about the events of a drag handler,
     * must implement.
     */
    public interface Callback extends AbstractDragHandler.Callback {

        /**
         * The method, which is invoked, when tabs are overshooting at the start.
         *
         * @param position
         *         The position of the first tab in pixels as a {@link Float} value
         */
        void onStartOvershoot(float position);

        /**
         * The method, which is invoked, when the tabs should be tilted when overshooting at the
         * start.
         *
         * @param angle
         *         The angle, the tabs should be tilted by, in degrees as a {@link Float} value
         */
        void onTiltOnStartOvershoot(float angle);

        /**
         * The method, which is invoked, when the tabs should be tilted when overshooting at the
         * end.
         *
         * @param angle
         *         The angle, the tabs should be tilted by, in degrees as a {@link Float} value
         */
        void onTiltOnEndOvershoot(float angle);

    }

    /**
     * The view recycler, which allows to inflate the views, which are used to visualize the tabs,
     * whose positions and states are calculated by the drag handler.
     */
    private final AttachedViewRecycler<TabItem, ?> viewRecycler;

    /**
     * The drag helper, which is used to recognize drag gestures when overshooting.
     */
    private final DragHelper overshootDragHelper;

    /**
     * The maximum overshoot distance in pixels.
     */
    private final int maxOvershootDistance;

    /**
     * The maximum angle, tabs can be rotated by, when overshooting at the start, in degrees.
     */
    private final float maxStartOvershootAngle;

    /**
     * The maximum angle, tabs can be rotated by, when overshooting at the end, in degrees.
     */
    private final float maxEndOvershootAngle;

    /**
     * The number of tabs, which are contained by a stack.
     */
    private final int stackedTabCount;

    /**
     * The inset of tabs in pixels.
     */
    private final int tabInset;

    /**
     * Notifies the callback, that tabs are overshooting at the start.
     *
     * @param position
     *         The position of the first tab in pixels as a {@link Float} value
     */
    private void notifyOnStartOvershoot(final float position) {
        if (getCallback() != null) {
            getCallback().onStartOvershoot(position);
        }
    }

    /**
     * Notifies the callback, that the tabs should be tilted when overshooting at the start.
     *
     * @param angle
     *         The angle, the tabs should be tilted by, in degrees as a {@link Float} value
     */
    private void notifyOnTiltOnStartOvershoot(final float angle) {
        if (getCallback() != null) {
            getCallback().onTiltOnStartOvershoot(angle);
        }
    }

    /**
     * Notifies the callback, that the tabs should be titled when overshooting at the end.
     *
     * @param angle
     *         The angle, the tabs should be tilted by, in degrees as a {@link Float} value
     */
    private void notifyOnTiltOnEndOvershoot(final float angle) {
        if (getCallback() != null) {
            getCallback().onTiltOnEndOvershoot(angle);
        }
    }

    /**
     * Creates a new drag handler, which allows to calculate the position and state of tabs on touch
     * events, when using the smartphone layout.
     *
     * @param tabSwitcher
     *         The tab switcher, whose tabs' positions and states should be calculated by the drag
     *         handler, as an instance of the class {@link TabSwitcher}. The tab switcher may not be
     *         null
     * @param arithmetics
     *         The arithmetics, which should be used to calculate the position, size and rotation of
     *         tabs, as an instance of the type {@link Arithmetics}. The arithmetics may not be
     *         null
     * @param viewRecycler
     *         The view recycler, which allows to inflate the views, which are used to visualize the
     *         tabs, whose positions and states should be calculated by the tab switcher, as an
     *         instance of the class AttachedViewRecycler. The view recycler may not be null
     */
    public PhoneDragHandler(@NonNull final TabSwitcher tabSwitcher,
                            @NonNull final Arithmetics arithmetics,
                            @NonNull final AttachedViewRecycler<TabItem, ?> viewRecycler) {
        super(tabSwitcher, arithmetics, true);
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        this.viewRecycler = viewRecycler;
        this.overshootDragHelper = new DragHelper(0);
        Resources resources = tabSwitcher.getResources();
        this.tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        this.stackedTabCount = resources.getInteger(R.integer.stacked_tab_count);
        this.maxOvershootDistance = resources.getDimensionPixelSize(R.dimen.max_overshoot_distance);
        this.maxStartOvershootAngle = resources.getInteger(R.integer.max_start_overshoot_angle);
        this.maxEndOvershootAngle = resources.getInteger(R.integer.max_end_overshoot_angle);
    }

    @Override
    @Nullable
    protected final TabItem getFocusedTab(final float position) {
        AbstractTabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.getTag().getState() == State.FLOATING ||
                    tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                View view = tabItem.getView();
                Toolbar[] toolbars = getTabSwitcher().getToolbars();
                float toolbarHeight = getTabSwitcher().getLayout() != Layout.PHONE_LANDSCAPE &&
                        getTabSwitcher().areToolbarsShown() && toolbars != null ?
                        toolbars[0].getHeight() - tabInset : 0;
                float viewPosition =
                        getArithmetics().getPosition(Axis.DRAGGING_AXIS, view) + toolbarHeight +
                                getArithmetics().getPadding(Axis.DRAGGING_AXIS, Gravity.START,
                                        getTabSwitcher());

                if (viewPosition <= position) {
                    return tabItem;
                }
            }
        }

        return null;
    }

    @Override
    protected final float onOvershootStart(final float dragPosition,
                                           final float overshootThreshold) {
        float result = overshootThreshold;
        overshootDragHelper.update(dragPosition);
        float overshootDistance = overshootDragHelper.getDragDistance();

        if (overshootDistance < 0) {
            float absOvershootDistance = Math.abs(overshootDistance);
            float startOvershootDistance =
                    getTabSwitcher().getCount() >= stackedTabCount ? maxOvershootDistance :
                            (getTabSwitcher().getCount() > 1 ? (float) maxOvershootDistance /
                                    (float) getTabSwitcher().getCount() : 0);

            if (absOvershootDistance <= startOvershootDistance) {
                float ratio =
                        Math.max(0, Math.min(1, absOvershootDistance / startOvershootDistance));
                AbstractTabItemIterator iterator =
                        new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).create();
                TabItem tabItem = iterator.getItem(0);
                float currentPosition = tabItem.getTag().getPosition();
                float position = currentPosition - (currentPosition * ratio);
                notifyOnStartOvershoot(position);
            } else {
                float ratio =
                        (absOvershootDistance - startOvershootDistance) / maxOvershootDistance;

                if (ratio >= 1) {
                    overshootDragHelper.setMinDragDistance(overshootDistance);
                    result = dragPosition + maxOvershootDistance + startOvershootDistance;
                }

                notifyOnTiltOnStartOvershoot(
                        Math.max(0, Math.min(1, ratio)) * maxStartOvershootAngle);
            }
        }

        return result;
    }

    @Override
    protected final float onOvershootEnd(final float dragPosition, final float overshootThreshold) {
        float result = overshootThreshold;
        overshootDragHelper.update(dragPosition);
        float overshootDistance = overshootDragHelper.getDragDistance();
        float ratio = overshootDistance / maxOvershootDistance;

        if (ratio >= 1) {
            overshootDragHelper.setMaxDragDistance(overshootDistance);
            result = dragPosition - maxOvershootDistance;
        }

        notifyOnTiltOnEndOvershoot(Math.max(0, Math.min(1, ratio)) *
                -(getTabSwitcher().getCount() > 1 ? maxEndOvershootAngle : maxStartOvershootAngle));
        return result;
    }

    @Override
    protected final void onOvershootReverted() {
        overshootDragHelper.reset();
    }

    @Override
    protected final void onReset() {
        overshootDragHelper.reset();
    }

    @Override
    protected final boolean isSwipeThresholdReached(@NonNull final TabItem swipedTabItem) {
        View view = swipedTabItem.getView();
        return Math.abs(getArithmetics().getPosition(Axis.ORTHOGONAL_AXIS, view)) >
                getArithmetics().getTabContainerSize(Axis.ORTHOGONAL_AXIS) / 6f;
    }

}