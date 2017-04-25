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
package de.mrapp.android.tabswitcher.layout;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.CallSuper;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.iterator.AbstractTabItemIterator;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.util.ViewUtil;
import de.mrapp.android.util.logging.Logger;
import de.mrapp.android.util.view.AttachedViewRecycler;

import static de.mrapp.android.util.Condition.ensureEqual;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An abstract base class for all layouts, which implement the functionality of a {@link
 * TabSwitcher}.
 *
 * @param <ViewRecyclerParamType>
 *         The type of the parameters, which can optionally be passed when inflating the views,
 *         which are used to visualize tabs
 * @author Michael Rapp
 * @since 0.1.0
 */
public abstract class AbstractTabSwitcherLayout<ViewRecyclerParamType>
        implements TabSwitcherLayout, OnGlobalLayoutListener, Model.Listener,
        AbstractDragHandler.Callback {

    /**
     * Defines the interface, a class, which should be notified about the events of a tab switcher
     * layout, must implement.
     */
    public interface Callback {

        /*
         * The method, which is invoked, when all animations have been ended.
         */
        void onAnimationsEnded();

    }

    /**
     * A layout listener, which unregisters itself from the observed view, when invoked. The
     * listener allows to encapsulate another listener, which is notified, when the listener is
     * invoked.
     */
    public static class LayoutListenerWrapper implements OnGlobalLayoutListener {

        /**
         * The observed view.
         */
        private final View view;

        /**
         * The encapsulated listener.
         */
        private final OnGlobalLayoutListener listener;

        /**
         * Creates a new layout listener, which unregisters itself from the observed view, when
         * invoked.
         *
         * @param view
         *         The observed view as an instance of the class {@link View}. The view may not be
         *         null
         * @param listener
         *         The listener, which should be encapsulated, as an instance of the type {@link
         *         OnGlobalLayoutListener} or null, if no listener should be encapsulated
         */
        public LayoutListenerWrapper(@NonNull final View view,
                                     @Nullable final OnGlobalLayoutListener listener) {
            ensureNotNull(view, "The view may not be null");
            this.view = view;
            this.listener = listener;
        }

        @Override
        public void onGlobalLayout() {
            ViewUtil.removeOnGlobalLayoutListener(view.getViewTreeObserver(), this);

            if (listener != null) {
                listener.onGlobalLayout();
            }
        }

    }

    /**
     * A animation listener, which increases the number of running animations, when the observed
     * animation is started, and decreases the number of accordingly, when the animation is
     * finished. The listener allows to encapsulate another animation listener, which is notified
     * when the animation has been started, canceled or ended.
     */
    protected class AnimationListenerWrapper extends AnimatorListenerAdapter {

        /**
         * The encapsulated listener.
         */
        private final AnimatorListener listener;

        /**
         * Decreases the number of running animations and executes the next pending action, if no
         * running animations remain.
         */
        private void endAnimation() {
            if (--runningAnimations == 0) {
                notifyOnAnimationsEnded();
            }
        }

        /**
         * Creates a new animation listener, which increases the number of running animations, when
         * the observed animation is started, and decreases the number of accordingly, when the
         * animation is finished.
         *
         * @param listener
         *         The listener, which should be encapsulated, as an instance of the type {@link
         *         AnimatorListener} or null, if no listener should be encapsulated
         */
        public AnimationListenerWrapper(@Nullable final AnimatorListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAnimationStart(final Animator animation) {
            super.onAnimationStart(animation);
            runningAnimations++;

            if (listener != null) {
                listener.onAnimationStart(animation);
            }
        }

        @Override
        public void onAnimationEnd(final Animator animation) {
            super.onAnimationEnd(animation);

            if (listener != null) {
                listener.onAnimationEnd(animation);
            }

            endAnimation();
        }

        @Override
        public void onAnimationCancel(final Animator animation) {
            super.onAnimationCancel(animation);

            if (listener != null) {
                listener.onAnimationCancel(animation);
            }

            endAnimation();
        }

    }

    /**
     * An iterator, which allows to iterate the tab items, which correspond to the tabs of a {@link
     * TabSwitcher}. When a tab item is referenced for the first time, its initial position and
     * state is calculated and the tab item is stored in a backing array. When the tab item is
     * iterated again, it is retrieved from the backing array.
     */
    protected class InitialTabItemIterator extends AbstractTabItemIterator {

        /**
         * The backing array, which is used to store tab items, once their initial position and
         * state has been calculated.
         */
        private final TabItem[] backingArray;

        /**
         * Calculates the initial position and state of a specific tab item.
         *
         * @param tabItem
         *         The tab item, whose position and state should be calculated, as an instance of
         *         the class {@link TabItem}. The tab item may not be null
         * @param predecessor
         *         The predecessor of the given tab item as an instance of the class {@link TabItem}
         *         or null, if the tab item does not have a predecessor
         */
        private void calculateAndClipStartPosition(@NonNull final TabItem tabItem,
                                                   @Nullable final TabItem predecessor) {
            float position = calculateStartPosition(tabItem);
            Pair<Float, State> pair =
                    clipTabPosition(getModel().getCount(), tabItem.getIndex(), position,
                            predecessor);
            tabItem.getTag().setPosition(pair.first);
            tabItem.getTag().setState(pair.second);
        }

        /**
         * Calculates and returns the initial position of a specific tab item.
         *
         * @param tabItem
         *         The tab item, whose position should be calculated, as an instance of the class
         *         {@link TabItem}. The tab item may not be null
         * @return The position, which has been calculated, as a {@link Float} value
         */
        private float calculateStartPosition(@NonNull final TabItem tabItem) {
            if (tabItem.getIndex() == 0) {
                return getCount() > stackedTabCount ? stackedTabCount * stackedTabSpacing :
                        (getCount() - 1) * stackedTabSpacing;

            } else {
                return -1;
            }
        }

        /**
         * Creates a new iterator, which allows to iterate the tab items, which corresponds to the
         * tabs of a {@link TabSwitcher}. When a tab item is referenced for the first time, its
         * initial position and state is calculated and the tab item is stored in a backing array.
         * When the tab item is iterated again, it is retrieved from the backing array.
         *
         * @param backingArray
         *         The backing array, which should be used to store tab items, once their initial
         *         position and state has been calculated, as an array of the type {@link TabItem}.
         *         The array may not be null and the array's length must be equal to the number of
         *         tabs, which are contained by the given tab switcher
         * @param reverse
         *         True, if the tabs should be iterated in reverse order, false otherwise
         * @param start
         *         The index of the first tab, which should be iterated, as an {@link Integer} value
         *         or -1, if all tabs should be iterated
         */
        public InitialTabItemIterator(@NonNull final TabItem[] backingArray, final boolean reverse,
                                      final int start) {
            ensureNotNull(backingArray, "The backing array may not be null");
            ensureEqual(backingArray.length, getModel().getCount(),
                    "The length of the backing array must be " + getModel().getCount());
            this.backingArray = backingArray;
            initialize(reverse, start);
        }

        @Override
        public final int getCount() {
            return backingArray.length;
        }

        @NonNull
        @Override
        public final TabItem getItem(final int index) {
            TabItem tabItem = backingArray[index];

            if (tabItem == null) {
                tabItem = TabItem.create(getModel(), getViewRecycler(), index);
                calculateAndClipStartPosition(tabItem, index > 0 ? getItem(index - 1) : null);
                backingArray[index] = tabItem;
            }

            return tabItem;
        }

    }

    /**
     * An animation, which allows to fling the tabs.
     */
    private class FlingAnimation extends android.view.animation.Animation {

        /**
         * The distance, the tabs should be moved.
         */
        private final float distance;

        /**
         * Creates a new fling animation.
         *
         * @param distance
         *         The distance, the tabs should be moved, in pixels as a {@link Float} value
         */
        FlingAnimation(final float distance) {
            this.distance = distance;
        }

        @Override
        protected void applyTransformation(final float interpolatedTime, final Transformation t) {
            if (flingAnimation != null) {
                getDragHandler().handleDrag(distance * interpolatedTime, 0);
            }
        }

    }

    /**
     * The tab switcher, the layout belongs to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The model of the tab switcher, the layout belongs to.
     */
    private final TabSwitcherModel model;

    /**
     * The arithmetics, which are used by the layout.
     */
    private final Arithmetics arithmetics;

    /**
     * The threshold, which must be reached until tabs are dragged, in pixels.
     */
    private final int dragThreshold;

    /**
     * The number of tabs, which are contained by a stack.
     */
    private final int stackedTabCount;

    /**
     * The space between tabs, which are part of a stack, in pixels.
     */
    private final int stackedTabSpacing;

    /**
     * The logger, which is used for logging.
     */
    private final Logger logger;

    /**
     * The callback, which is notified about the layout's events.
     */
    private Callback callback;

    /**
     * The number of animations, which are currently running.
     */
    private int runningAnimations;

    /**
     * The animation, which is used to fling the tabs.
     */
    private android.view.animation.Animation flingAnimation;

    /**
     * Adapts the visibility of the toolbars, which are shown, when the tab switcher is shown.
     */
    private void adaptToolbarVisibility() {
        Toolbar[] toolbars = getToolbars();

        if (toolbars != null) {
            for (Toolbar toolbar : toolbars) {
                toolbar.setVisibility(
                        getModel().areToolbarsShown() ? View.VISIBLE : View.INVISIBLE);
            }
        }

        // TODO: Detach and re-inflate layout
    }

    /**
     * Adapts the title of the toolbar, which is shown, when the tab switcher is shown.
     */
    private void adaptToolbarTitle() {
        Toolbar[] toolbars = getToolbars();

        if (toolbars != null) {
            toolbars[0].setTitle(getModel().getToolbarTitle());
        }
    }

    /**
     * Adapts the navigation icon of the toolbar, which is shown, when the tab switcher is shown.
     */
    private void adaptToolbarNavigationIcon() {
        Toolbar[] toolbars = getToolbars();

        if (toolbars != null) {
            Toolbar toolbar = toolbars[0];
            toolbar.setNavigationIcon(getModel().getToolbarNavigationIcon());
            toolbar.setNavigationOnClickListener(getModel().getToolbarNavigationIconListener());
        }
    }

    /**
     * Inflates the menu of the toolbar, which is shown, when the tab switcher is shown.
     */
    private void inflateToolbarMenu() {
        Toolbar[] toolbars = getToolbars();
        int menuId = getModel().getToolbarMenuId();

        if (toolbars != null && menuId != -1) {
            Toolbar toolbar = toolbars.length > 1 ? toolbars[1] : toolbars[0];
            toolbar.inflateMenu(menuId);
            toolbar.setOnMenuItemClickListener(getModel().getToolbarMenuItemListener());
        }
    }

    /**
     * Creates and returns an animation listener, which allows to handle, when a fling animation
     * ended.
     *
     * @return The listener, which has been created, as an instance of the class {@link
     * Animation.AnimationListener}. The listener may not be null
     */
    @NonNull
    private Animation.AnimationListener createFlingAnimationListener() {
        return new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(final android.view.animation.Animation animation) {

            }

            @Override
            public void onAnimationEnd(final android.view.animation.Animation animation) {
                getDragHandler().handleRelease(null, dragThreshold);
                flingAnimation = null;
                notifyOnAnimationsEnded();
            }

            @Override
            public void onAnimationRepeat(final android.view.animation.Animation animation) {

            }

        };
    }

    /**
     * Notifies the callback, that all animations have been ended.
     */
    private void notifyOnAnimationsEnded() {
        if (callback != null) {
            callback.onAnimationsEnded();
        }
    }

    /**
     * Returns the tab switcher, the layout belongs to.
     *
     * @return The tab switcher, the layout belongs to, as an instance of the class {@link
     * TabSwitcher}. The tab switcher may not be null
     */
    @NonNull
    protected final TabSwitcher getTabSwitcher() {
        return tabSwitcher;
    }

    /**
     * Returns the model of the tab switcher, the layout belongs to.
     *
     * @return The model of the tab switcher, the layout belongs to, as an instance of the class
     * {@link TabSwitcherModel}. The model may not be null
     */
    @NonNull
    protected final TabSwitcherModel getModel() {
        return model;
    }

    /**
     * Returns the arithmetics, which are used by the layout.
     *
     * @return The arithmetics, which are used by the layout, as an instance of the type {@link
     * Arithmetics}. The arithmetics may not be null
     */
    @NonNull
    protected final Arithmetics getArithmetics() {
        return arithmetics;
    }

    /**
     * Returns the threshold, which must be reached until tabs are dragged.
     *
     * @return The threshold, which must be reached until tabs are dragged, in pixels as an {@link
     * Integer} value
     */
    protected final int getDragThreshold() {
        return dragThreshold;
    }

    /**
     * Returns the number of tabs, which are contained by a stack.
     *
     * @return The number of tabs, which are contained by a stack, as an {@link Integer} value
     */
    protected final int getStackedTabCount() {
        return stackedTabCount;
    }

    /**
     * Returns the space between tabs, which are part of a stack.
     *
     * @return The space between tabs, which are part of a stack, in pixels as an {@link Integer}
     * value
     */
    protected final int getStackedTabSpacing() {
        return stackedTabSpacing;
    }

    /**
     * Returns the logger, which is used for logging.
     *
     * @return The logger, which is used for logging, as an instance of the class Logger. The logger
     * may not be null
     */
    @NonNull
    protected final Logger getLogger() {
        return logger;
    }

    /**
     * Returns the context, which is used by the layout.
     *
     * @return The context, which is used by the layout, as an instance of the class {@link
     * Context}. The context may not be null
     */
    @NonNull
    protected final Context getContext() {
        return tabSwitcher.getContext();
    }

    /**
     * Clips the position of a specific tab.
     *
     * @param count
     *         The total number of tabs, which are currently contained by the tab switcher, as an
     *         {@link Integer} value
     * @param index
     *         The index of the tab, whose position should be clipped, as an {@link Integer} value
     * @param position
     *         The position, which should be clipped, in pixels as a {@link Float} value
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     * @return A pair, which contains the position and state of the tab item, as an instance of the
     * class {@link Pair}. The pair may not be null
     */
    @NonNull
    protected final Pair<Float, State> clipTabPosition(final int count, final int index,
                                                       final float position,
                                                       @Nullable final TabItem predecessor) {
        return clipTabPosition(count, index, position,
                predecessor != null ? predecessor.getTag().getState() : null);
    }

    /**
     * Clips the position of a specific tab.
     *
     * @param count
     *         The total number of tabs, which are currently contained by the tab switcher, as an
     *         {@link Integer} value
     * @param index
     *         The index of the tab, whose position should be clipped, as an {@link Integer} value
     * @param position
     *         The position, which should be clipped, in pixels as a {@link Float} value
     * @param predecessorState
     *         The state of the predecessor of the given tab item as a value of the enum {@link
     *         State} or null, if the tab item does not have a predecessor
     * @return A pair, which contains the position and state of the tab item, as an instance of the
     * class {@link Pair}. The pair may not be null
     */
    protected final Pair<Float, State> clipTabPosition(final int count, final int index,
                                                       final float position,
                                                       @Nullable final State predecessorState) {
        Pair<Float, State> startPair =
                calculatePositionAndStateWhenStackedAtStart(count, index, predecessorState);
        float startPosition = startPair.first;

        if (position <= startPosition) {
            State state = startPair.second;
            return Pair.create(startPosition, state);
        } else {
            Pair<Float, State> endPair = calculatePositionAndStateWhenStackedAtEnd(index);
            float endPosition = endPair.first;

            if (position >= endPosition) {
                State state = endPair.second;
                return Pair.create(endPosition, state);
            } else {
                State state = State.FLOATING;
                return Pair.create(position, state);
            }
        }
    }

    /**
     * Calculates and returns the position and state of a specific tab, when stacked at the start.
     *
     * @param count
     *         The total number of tabs, which are currently contained by the tab switcher, as an
     *         {@link Integer} value
     * @param index
     *         The index of the tab, whose position and state should be returned, as an {@link
     *         Integer} value
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     * @return A pair, which contains the position and state of the given tab item, when stacked at
     * the start, as an instance of the class {@link Pair}. The pair may not be null
     */
    @NonNull
    protected final Pair<Float, State> calculatePositionAndStateWhenStackedAtStart(final int count,
                                                                                   final int index,
                                                                                   @Nullable final TabItem predecessor) {
        return calculatePositionAndStateWhenStackedAtStart(count, index,
                predecessor != null ? predecessor.getTag().getState() : null);
    }

    /**
     * Calculates and returns the position and state of a specific tab, when stacked at the start.
     *
     * @param count
     *         The total number of tabs, which are currently contained by the tab switcher, as an
     *         {@link Integer} value
     * @param index
     *         The index of the tab, whose position and state should be returned, as an {@link
     *         Integer} value
     * @param predecessorState
     *         The state of the predecessor of the given tab item as a value of the enum {@link
     *         State} or null, if the tab item does not have a predecessor
     * @return A pair, which contains the position and state of the given tab item, when stacked at
     * the start, as an instance of the class {@link Pair}. The pair may not be null
     */
    @NonNull
    private Pair<Float, State> calculatePositionAndStateWhenStackedAtStart(final int count,
                                                                           final int index,
                                                                           @Nullable final State predecessorState) {
        if ((count - index) <= stackedTabCount) {
            float position = stackedTabSpacing * (count - (index + 1));
            return Pair.create(position,
                    (predecessorState == null || predecessorState == State.FLOATING) ?
                            State.STACKED_START_ATOP : State.STACKED_START);
        } else {
            float position = stackedTabSpacing * stackedTabCount;
            return Pair.create(position,
                    (predecessorState == null || predecessorState == State.FLOATING) ?
                            State.STACKED_START_ATOP : State.HIDDEN);
        }
    }

    /**
     * Inflates the view, which is used to visualize a specific tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be inflated, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @param listener
     *         The layout listener, which should be notified, when the view has been inflated, as an
     *         instance of the type {@link OnGlobalLayoutListener} or null, if no listener should be
     *         notified
     * @param params
     *         An array, which contains optional parameters, which should be passed to the view
     *         recycler, which is used to inflate the view, as an array of the generic type
     *         ViewRecyclerParamType. The array may not be null
     */
    @SafeVarargs
    protected final void inflateView(@NonNull final TabItem tabItem,
                                     @Nullable final OnGlobalLayoutListener listener,
                                     @NonNull final ViewRecyclerParamType... params) {
        Pair<View, Boolean> pair = getViewRecycler().inflate(tabItem, params);

        if (listener != null) {
            boolean inflated = pair.second;

            if (inflated) {
                View view = pair.first;
                view.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new LayoutListenerWrapper(view, listener));
            } else {
                listener.onGlobalLayout();
            }
        }
    }

    /**
     * Creates a new layout, which implements the functionality of a {@link TabSwitcher}.
     *
     * @param tabSwitcher
     *         The tab switcher, the layout belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param model
     *         The model of the tab switcher, the layout belongs to, as an instance of the class
     *         {@link TabSwitcherModel}. The model may not be null
     * @param arithmetics
     *         The arithmetics, which should be used by the layout, as an instance of the type
     *         {@link Arithmetics}. The arithmetics may not be null
     */
    public AbstractTabSwitcherLayout(@NonNull final TabSwitcher tabSwitcher,
                                     @NonNull final TabSwitcherModel model,
                                     @NonNull final Arithmetics arithmetics) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(model, "The model may not be null");
        ensureNotNull(arithmetics, "The arithmetics may not be null");
        this.tabSwitcher = tabSwitcher;
        this.model = model;
        this.arithmetics = arithmetics;
        Resources resources = tabSwitcher.getResources();
        this.dragThreshold = resources.getDimensionPixelSize(R.dimen.drag_threshold);
        this.stackedTabCount = resources.getInteger(R.integer.stacked_tab_count);
        this.stackedTabSpacing = resources.getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        this.logger = new Logger(model.getLogLevel());
        this.callback = null;
        this.runningAnimations = 0;
        this.flingAnimation = null;
    }

    /**
     * The method, which is invoked on implementing subclasses in order to inflate the layout.
     *
     * @param tabsOnly
     *         True, if only the tabs should be inflated, false otherwise
     */
    protected abstract void onInflateLayout(final boolean tabsOnly);

    /**
     * The method, which is invoked on implementing subclasses in order to detach the layout.
     *
     * @param tabsOnly
     *         True, if only the tabs should be detached, false otherwise
     * @return A pair, which contains the index of the first visible tab, as well as its current
     * position, as an instance of the class Pair or null, if the tab switcher is not shown
     */
    @Nullable
    protected abstract Pair<Integer, Float> onDetachLayout(final boolean tabsOnly);

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the drag
     * handler, which is used by the layout.
     *
     * @return The drag handler, which is used by the layout, as an instance of the class {@link
     * AbstractDragHandler} or null, if the drag handler has not been initialized yet
     */
    protected abstract AbstractDragHandler<?> getDragHandler();

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the view
     * recycler, which allows to inflate the views, which are used to visualize the tabs.
     *
     * @return The view recycler, which allows to inflate the views, which are used to visualize the
     * tabs, as an instance of the class {@link AttachedViewRecycler} or null, if the view recycler
     * has not been initialized yet
     */
    protected abstract AttachedViewRecycler<TabItem, ViewRecyclerParamType> getViewRecycler();

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the position and
     * state of a specific tab, when stacked at the end.
     *
     * @param index
     *         The index of the tab, whose position and state should be returned, as an {@link
     *         Integer} value
     * @return A pair, which contains the position and state of the given tab item, when stacked at
     * the end, as an instance of the class {@link Pair}. The pair may not be null
     */
    @NonNull
    protected abstract Pair<Float, State> calculatePositionAndStateWhenStackedAtEnd(
            final int index);

    /**
     * Handles a touch event.
     *
     * @param event
     *         The touch event as an instance of the class {@link MotionEvent}. The touch event may
     *         not be null
     * @return True, if the event has been handled, false otherwise
     */
    public abstract boolean handleTouchEvent(@NonNull final MotionEvent event);

    /**
     * Inflates the layout.
     *
     * @param tabsOnly
     *         True, if only the tabs should be inflated, false otherwise
     */
    public final void inflateLayout(final boolean tabsOnly) {
        onInflateLayout(tabsOnly);

        if (!tabsOnly) {
            adaptToolbarVisibility();
            adaptToolbarTitle();
            adaptToolbarNavigationIcon();
            inflateToolbarMenu();
        }
    }

    /**
     * Detaches the layout.
     *
     * @param tabsOnly
     *         True, if only the tabs should be detached, false otherwise
     * @return A pair, which contains the index of the first visible tab, as well as its current
     * position, as an instance of the class Pair or null, if the tab switcher is not shown
     */
    @Nullable
    public final Pair<Integer, Float> detachLayout(final boolean tabsOnly) {
        return onDetachLayout(tabsOnly);
    }

    /**
     * Sets the callback, which should be notified about the layout's events.
     *
     * @param callback
     *         The callback, which should be set, as an instance of the type {@link Callback} or
     *         null, if no callback should be notified
     */
    public final void setCallback(@Nullable final Callback callback) {
        this.callback = callback;
    }

    @Override
    public final boolean isAnimationRunning() {
        return runningAnimations > 0 || flingAnimation != null;
    }

    @Nullable
    @Override
    public final Menu getToolbarMenu() {
        Toolbar[] toolbars = getToolbars();

        if (toolbars != null) {
            Toolbar toolbar = toolbars.length > 1 ? toolbars[1] : toolbars[0];
            return toolbar.getMenu();
        }

        return null;
    }

    @CallSuper
    @Override
    public void onDecoratorChanged(@NonNull final TabSwitcherDecorator decorator) {
        detachLayout(true);
        onGlobalLayout();
    }

    @Override
    public final void onToolbarVisibilityChanged(final boolean visible) {
        adaptToolbarVisibility();
    }

    @Override
    public final void onToolbarTitleChanged(@Nullable final CharSequence title) {
        adaptToolbarTitle();
    }

    @Override
    public final void onToolbarNavigationIconChanged(@Nullable final Drawable icon,
                                                     @Nullable final OnClickListener listener) {
        adaptToolbarNavigationIcon();
    }

    @Override
    public final void onToolbarMenuInflated(@MenuRes final int resourceId,
                                            @Nullable final OnMenuItemClickListener listener) {
        inflateToolbarMenu();
    }

    @Override
    public final void onFling(final float distance, final long duration) {
        if (getDragHandler() != null) {
            flingAnimation = new FlingAnimation(distance);
            flingAnimation.setFillAfter(true);
            flingAnimation.setAnimationListener(createFlingAnimationListener());
            flingAnimation.setDuration(duration);
            flingAnimation.setInterpolator(new DecelerateInterpolator());
            getTabSwitcher().startAnimation(flingAnimation);
            logger.logVerbose(getClass(),
                    "Started fling animation using a distance of " + distance +
                            " pixels and a duration of " + duration + " milliseconds");
        }
    }

    @Override
    public final void onCancelFling() {
        if (flingAnimation != null) {
            flingAnimation.cancel();
            flingAnimation = null;
            getDragHandler().handleRelease(null, dragThreshold);
            logger.logVerbose(getClass(), "Canceled fling animation");
        }
    }

    @Override
    public void onRevertStartOvershoot() {

    }

    @Override
    public void onRevertEndOvershoot() {

    }

    @Override
    public void onSwipe(@NonNull final TabItem tabItem, final float distance) {

    }

    @Override
    public void onSwipeEnded(@NonNull final TabItem tabItem, final boolean remove,
                             final float velocity) {

    }

}