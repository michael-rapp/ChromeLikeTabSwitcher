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
package de.mrapp.android.tabswitcher.layout;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.core.util.Pair;
import de.mrapp.android.tabswitcher.AddTabButtonListener;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.gesture.AbstractTouchEventHandler;
import de.mrapp.android.tabswitcher.gesture.PullDownGestureEventHandler;
import de.mrapp.android.tabswitcher.gesture.SwipeGestureEventHandler;
import de.mrapp.android.tabswitcher.gesture.TouchEventDispatcher;
import de.mrapp.android.tabswitcher.iterator.AbstractItemIterator;
import de.mrapp.android.tabswitcher.iterator.ItemIterator;
import de.mrapp.android.tabswitcher.layout.AbstractDragTabsEventHandler.DragState;
import de.mrapp.android.tabswitcher.layout.Arithmetics.Axis;
import de.mrapp.android.tabswitcher.model.AbstractItem;
import de.mrapp.android.tabswitcher.model.AddTabItem;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.tabswitcher.model.TabSwitcherStyle;
import de.mrapp.android.util.ViewUtil;
import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.android.util.logging.Logger;
import de.mrapp.android.util.view.AbstractViewRecycler;
import de.mrapp.android.util.view.AttachedViewRecycler;
import de.mrapp.util.Condition;

/**
 * An abstract base class for all layouts, which implement the functionality of a {@link
 * TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public abstract class AbstractTabSwitcherLayout
        implements TabSwitcherLayout, OnGlobalLayoutListener, Model.Listener,
        TouchEventDispatcher.Callback, AbstractDragTabsEventHandler.Callback,
        SwipeGestureEventHandler.Callback, PullDownGestureEventHandler.Callback {

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
            Condition.INSTANCE.ensureNotNull(view, "The view may not be null");
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
            runningAnimations++;
        }

        @Override
        public void onAnimationStart(final Animator animation) {
            super.onAnimationStart(animation);

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
     * A builder, which allows to configure and create instances of the class {@link
     * InitialItemIterator}.
     */
    protected class InitialItemIteratorBuilder extends
            AbstractItemIterator.AbstractBuilder<InitialItemIteratorBuilder, InitialItemIterator> {

        /**
         * The backing array, which is used to store items, once their initial position and state
         * has been calculated.
         */
        private final AbstractItem[] backingArray;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * InitialItemIterator}.
         *
         * @param backingArray
         *         The backing array, which should be used to store items, once their initial
         *         position and state has been calculated. The backing array may not be null
         */
        public InitialItemIteratorBuilder(@NonNull final AbstractItem[] backingArray) {
            Condition.INSTANCE.ensureNotNull(backingArray, "The backing array may not be null");
            this.backingArray = backingArray;
        }

        @NonNull
        @Override
        public InitialItemIterator create() {
            return new InitialItemIterator(backingArray, reverse, start);
        }

    }

    /**
     * An iterator, which allows to iterate the items, which correspond to the child views of a
     * {@link TabSwitcher}. When an item is referenced for the first time, its initial position and
     * state is calculated and the item is stored in a backing array. When the item is iterated
     * again, it is retrieved from the backing array.
     */
    protected class InitialItemIterator extends AbstractItemIterator {

        /**
         * The backing array, which is used to store items, once their initial position and state
         * has been calculated.
         */
        private final AbstractItem[] backingArray;

        /**
         * Calculates the initial position and state of a specific item.
         *
         * @param item
         *         The item, whose position and state should be calculated, as an instance of the
         *         class {@link AbstractItem}. The item may not be null
         * @param predecessor
         *         The predecessor of the given item as an instance of the class {@link
         *         AbstractItem} or null, if the item does not have a predecessor
         */
        private void calculateAndClipStartPosition(@NonNull final AbstractItem item,
                                                   @Nullable final AbstractItem predecessor) {
            float position = calculateStartPosition(item);
            Pair<Float, State> pair = clipPosition(item.getIndex(), position, predecessor);
            item.getTag().setPosition(pair.first);
            item.getTag().setState(pair.second);
        }

        /**
         * Calculates and returns the initial position of a specific item.
         *
         * @param item
         *         The item, whose position should be calculated, as an instance of the class {@link
         *         AbstractItem}. The item may not be null
         * @return The position, which has been calculated, as a {@link Float} value
         */
        private float calculateStartPosition(@NonNull final AbstractItem item) {
            if (item.getIndex() == 0) {
                return getCount() > getStackedTabCount() ?
                        getStackedTabCount() * stackedTabSpacing :
                        (getCount() - 1) * stackedTabSpacing;
            } else {
                return -1;
            }
        }

        /**
         * Creates a new iterator, which allows to iterate the items, which corresponds to the child
         * views of a {@link TabSwitcher}. When an item is referenced for the first time, its
         * initial position and state is calculated and the item is stored in a backing array. When
         * the item is iterated again, it is retrieved from the backing array.
         *
         * @param backingArray
         *         The backing array, which should be used to store items, once their initial
         *         position and state has been calculated, as an array of the type {@link
         *         AbstractItem}. The array may not be null
         * @param reverse
         *         True, if the items should be iterated in reverse order, false otherwise
         * @param start
         *         The index of the first item, which should be iterated, as an {@link Integer}
         *         value or -1, if all items should be iterated
         */
        private InitialItemIterator(@NonNull final AbstractItem[] backingArray,
                                    final boolean reverse, final int start) {
            Condition.INSTANCE.ensureNotNull(backingArray, "The backing array may not be null");
            this.backingArray = backingArray;
            initialize(reverse, start);
        }

        @Override
        public final int getCount() {
            return backingArray.length;
        }

        @NonNull
        @Override
        public final AbstractItem getItem(final int index) {
            AbstractItem item = backingArray[index];

            if (item == null) {
                if (index == 0 && getModel().isAddTabButtonShown()) {
                    item = AddTabItem.create(getTabViewRecycler());
                } else {
                    item = TabItem.create(getModel(), getTabViewRecycler(),
                            index - (getModel().isAddTabButtonShown() ? 1 : 0));
                }

                calculateAndClipStartPosition(item, index > 0 ? getItem(index - 1) : null);
                backingArray[index] = item;
            }

            return item;
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
     * The style, which allows to retrieve style attributes of the tab switcher.
     */
    private final TabSwitcherStyle style;

    /**
     * The dispatcher, which is used to dispatch touch events to event handlers.
     */
    private final TouchEventDispatcher touchEventDispatcher;

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
     * The index of the first visible tab.
     */
    private int firstVisibleIndex;

    /**
     * Registers the layout as the callback of all touch event handlers.
     */
    private void registerEventHandlerCallbacks() {
        for (AbstractTouchEventHandler eventHandler : touchEventDispatcher) {
            registerEventHandlerCallback(eventHandler);
        }

        touchEventDispatcher.setCallback(this);
    }

    /**
     * Registers the layout as the callback of a specific event handler.
     *
     * @param eventHandler
     *         The event handler as an instance of the class {@link AbstractTouchEventHandler}. The
     *         event handler may not be null
     */
    private void registerEventHandlerCallback(
            @NonNull final AbstractTouchEventHandler eventHandler) {
        if (eventHandler instanceof SwipeGestureEventHandler) {
            ((SwipeGestureEventHandler) eventHandler).setCallback(this);
        } else if (eventHandler instanceof PullDownGestureEventHandler) {
            ((PullDownGestureEventHandler) eventHandler).setCallback(this);
        }
    }

    /**
     * Unregisters the layout as the callback of all touch event handlers.
     */
    private void unregisterEventHandlerCallbacks() {
        for (AbstractTouchEventHandler eventHandler : touchEventDispatcher) {
            unregisterEventHandlerCallback(eventHandler);
        }

        touchEventDispatcher.setCallback(null);
    }

    /**
     * Unregisters the layout as the callback of a specific event handler.
     *
     * @param eventHandler
     *         The event handler as an instance of the class {@link AbstractTouchEventHandler}. The
     *         event handler may not be null
     */
    private void unregisterEventHandlerCallback(
            @NonNull final AbstractTouchEventHandler eventHandler) {
        if (eventHandler instanceof SwipeGestureEventHandler) {
            ((SwipeGestureEventHandler) eventHandler).setCallback(null);
        } else if (eventHandler instanceof PullDownGestureEventHandler) {
            ((PullDownGestureEventHandler) eventHandler).setCallback(null);
        }
    }

    /**
     * Adapts the visibility of the toolbars, which are shown, when the tab switcher is shown.
     */
    private void adaptToolbarVisibility() {
        Toolbar[] toolbars = getToolbars();

        if (toolbars != null) {
            for (Toolbar toolbar : toolbars) {
                toolbar.setVisibility(
                        getTabSwitcher().isSwitcherShown() && getModel().areToolbarsShown() ?
                                View.VISIBLE : View.INVISIBLE);
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
            CharSequence title = style.getToolbarTitle();
            toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].setTitle(title);
        }
    }

    /**
     * Adapts the navigation icon of the toolbar, which is shown, when the tab switcher is shown.
     */
    private void adaptToolbarNavigationIcon() {
        Toolbar[] toolbars = getToolbars();

        if (toolbars != null) {
            Toolbar toolbar = toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX];
            Drawable icon = style.getToolbarNavigationIcon();
            toolbar.setNavigationIcon(icon);
            toolbar.setNavigationOnClickListener(getModel().getToolbarNavigationIconListener());
        }
    }

    /**
     * Adapts the decorator.
     */
    private void adaptDecorator() {
        getContentViewRecycler().setAdapter(onCreateContentRecyclerAdapter());
    }

    /**
     * Adapts the log level.
     */
    private void adaptLogLevel() {
        getTabViewRecycler().setLogLevel(getModel().getLogLevel());
        getContentViewRecycler().setLogLevel(getModel().getLogLevel());
    }

    /**
     * Inflates the menu of the toolbar, which is shown, when the tab switcher is shown.
     */
    private void inflateToolbarMenu() {
        Toolbar[] toolbars = getToolbars();
        int menuId = getModel().getToolbarMenuId();

        if (toolbars != null && menuId != -1) {
            Toolbar toolbar = toolbars.length > 1 ? toolbars[TabSwitcher.SECONDARY_TOOLBAR_INDEX] :
                    toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX];
            Menu previousMenu = toolbar.getMenu();

            if (previousMenu != null) {
                previousMenu.clear();
            }

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
                getDragHandler().onUp(null);
                flingAnimation = null;
                notifyOnAnimationsEnded();
            }

            @Override
            public void onAnimationRepeat(final android.view.animation.Animation animation) {

            }

        };
    }

    /**
     * Calculates the positions of all items, when dragging towards the start.
     *
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     */
    private void calculatePositionsWhenDraggingToEnd(final float dragDistance) {
        firstVisibleIndex = -1;
        AbstractItemIterator iterator =
                new ItemIterator.Builder(getTabSwitcher(), getTabViewRecycler()).create();
        AbstractItem item;
        boolean abort = false;

        while ((item = iterator.next()) != null && !abort) {
            if (getItemCount() - item.getIndex() > 1) {
                abort = calculatePositionWhenDraggingToEnd(dragDistance, item, iterator.previous());

                if (firstVisibleIndex == -1 && item.getTag().getState() == State.FLOATING) {
                    firstVisibleIndex = item.getIndex();
                }
            } else {
                Pair<Float, State> pair = clipPosition(item.getIndex(), item.getTag().getPosition(),
                        iterator.previous());
                item.getTag().setPosition(pair.first);
                item.getTag().setState(pair.second);
            }

            inflateOrRemoveView(item, true);
        }
    }

    /**
     * The method, which is invoked on implementing subclasses in order to calculate the position of
     * a specific item, when dragging towards the end.
     *
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     * @param item
     *         The item whose position should be calculated, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param predecessor
     *         The predecessor of the given item as an instance of the class {@link AbstractItem} or
     *         null, if the item does not have a predecessor
     * @return True, if calculating the position of subsequent items can be omitted, false otherwise
     */
    private boolean calculatePositionWhenDraggingToEnd(final float dragDistance,
                                                       @NonNull final AbstractItem item,
                                                       @Nullable final AbstractItem predecessor) {
        if (predecessor == null || predecessor.getTag().getState() != State.FLOATING) {
            if ((item.getTag().getState() == State.STACKED_START_ATOP && item.getIndex() == 0) ||
                    item.getTag().getState() == State.FLOATING) {
                float currentPosition = item.getTag().getPosition();
                float newPosition = currentPosition + dragDistance;
                float maxEndPosition = calculateMaxEndPosition(item.getIndex());

                if (maxEndPosition != -1) {
                    newPosition = Math.min(newPosition, maxEndPosition);
                }

                Pair<Float, State> pair = clipPosition(item.getIndex(), newPosition, predecessor);
                item.getTag().setPosition(pair.first);
                item.getTag().setState(pair.second);
            } else if (item.getTag().getState() == State.STACKED_START_ATOP) {
                return true;
            }
        } else {
            float newPosition = calculateSuccessorPosition(item, predecessor);
            float maxEndPosition = calculateMaxEndPosition(item.getIndex());

            if (maxEndPosition != -1) {
                newPosition = Math.min(newPosition, maxEndPosition);
            }
            Pair<Float, State> pair = clipPosition(item.getIndex(), newPosition, predecessor);
            item.getTag().setPosition(pair.first);
            item.getTag().setState(pair.second);
        }

        return false;
    }

    /**
     * Calculates the positions of all items, when dragging towards the end.
     *
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     */
    private void calculatePositionsWhenDraggingToStart(final float dragDistance) {
        AbstractItemIterator iterator =
                new ItemIterator.Builder(getTabSwitcher(), getTabViewRecycler())
                        .start(Math.max(0, firstVisibleIndex)).create();
        AbstractItem item;
        boolean abort = false;

        while ((item = iterator.next()) != null && !abort) {
            if (getItemCount() - item.getIndex() > 1) {
                abort = calculatePositionWhenDraggingToStart(dragDistance, item,
                        iterator.previous());
            } else {
                Pair<Float, State> pair = clipPosition(item.getIndex(), item.getTag().getPosition(),
                        iterator.previous());
                item.getTag().setPosition(pair.first);
                item.getTag().setState(pair.second);
            }

            inflateOrRemoveView(item, true);
        }

        if (firstVisibleIndex > 0) {
            int start = firstVisibleIndex - 1;
            iterator =
                    new ItemIterator.Builder(getTabSwitcher(), getTabViewRecycler()).reverse(true)
                            .start(start).create();

            while ((item = iterator.next()) != null) {
                AbstractItem successor = iterator.previous();

                if (item.getIndex() < start) {
                    float successorPosition = successor.getTag().getPosition();
                    Pair<Float, State> pair =
                            clipPosition(successor.getIndex(), successorPosition, item);
                    successor.getTag().setPosition(pair.first);
                    successor.getTag().setState(pair.second);
                    inflateOrRemoveView(successor, true);

                    if (successor.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = successor.getIndex();
                    } else {
                        break;
                    }
                }

                float newPosition = calculatePredecessorPosition(item, successor);
                item.getTag().setPosition(newPosition);

                if (!iterator.hasNext()) {
                    Pair<Float, State> pair =
                            clipPosition(item.getIndex(), newPosition, (AbstractItem) null);
                    item.getTag().setPosition(pair.first);
                    item.getTag().setState(pair.second);
                    inflateOrRemoveView(item, true);

                    if (item.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = item.getIndex();
                    }
                }
            }
        }
    }

    /**
     * Calculates the position of a specific item, when dragging towards the start.
     *
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     * @param item
     *         The item, whose position should be calculated, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param predecessor
     *         The predecessor of the given item as an instance of the class {@link AbstractItem} or
     *         null, if the item does not have a predecessor
     * @return True, if calculating the position of subsequent items can be omitted, false otherwise
     */
    private boolean calculatePositionWhenDraggingToStart(final float dragDistance,
                                                         @NonNull final AbstractItem item,
                                                         @Nullable final AbstractItem predecessor) {
        float attachedPosition = calculateAttachedPosition(getTabSwitcher().getCount());

        if (predecessor == null || predecessor.getTag().getState() != State.FLOATING ||
                (attachedPosition != -1 && predecessor.getTag().getPosition() > attachedPosition)) {
            if (item.getTag().getState() == State.FLOATING) {
                float currentPosition = item.getTag().getPosition();
                float newPosition = currentPosition + dragDistance;
                float minStartPosition = calculateMinStartPosition(item.getIndex());

                if (minStartPosition != -1) {
                    newPosition = Math.max(newPosition, minStartPosition);
                }

                Pair<Float, State> pair = clipPosition(item.getIndex(), newPosition, predecessor);
                item.getTag().setPosition(pair.first);
                item.getTag().setState(pair.second);
            } else if (item.getTag().getState() == State.STACKED_START_ATOP) {
                float currentPosition = item.getTag().getPosition();
                Pair<Float, State> pair =
                        clipPosition(item.getIndex(), currentPosition, predecessor);
                item.getTag().setPosition(pair.first);
                item.getTag().setState(pair.second);
                return true;
            } else if (item.getTag().getState() == State.HIDDEN ||
                    item.getTag().getState() == State.STACKED_START) {
                return true;
            }
        } else {
            float newPosition = calculateSuccessorPosition(item, predecessor);
            float minStartPosition = calculateMinStartPosition(item.getIndex());

            if (minStartPosition != -1) {
                newPosition = Math.max(newPosition, minStartPosition);
            }

            Pair<Float, State> pair = clipPosition(item.getIndex(), newPosition, predecessor);
            item.getTag().setPosition(pair.first);
            item.getTag().setState(pair.second);
        }

        return false;
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
     * Returns the style, which allows to retrieve style attributes of the tab switcher.
     *
     * @return The style, which allows to retrieve style attributes of the tab switcher, as an
     * instance of the class {@link TabSwitcherStyle}. The style may not be null
     */
    @NonNull
    protected final TabSwitcherStyle getStyle() {
        return style;
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
     * Returns the index of the first visible tab.
     *
     * @return The index of the first visible tab as an {@link Integer} value or -1, if no tabs is
     * visible
     */
    protected final int getFirstVisibleIndex() {
        return firstVisibleIndex;
    }

    /**
     * Sets the index of the first visible tab.
     *
     * @param firstVisibleIndex
     *         The index, which should be set, as an {@link Integer} value or -1, if no tab is
     *         visible
     */
    protected final void setFirstVisibleIndex(final int firstVisibleIndex) {
        this.firstVisibleIndex = firstVisibleIndex;
    }

    /**
     * Returns the number of child views, which are contained by the tab switcher.
     *
     * @return The number of child views, which are contained by the tab switcher, as an {@link
     * Integer} value
     */
    protected final int getItemCount() {
        return getModel().getCount() + (getModel().isAddTabButtonShown() ? 1 : 0);
    }

    /**
     * Returns, whether a hidden tab at a specific index, is part of the stack, which is located at
     * the start, or not.
     *
     * @param index
     *         The index of the hidden tab, as an {@link Integer} value
     * @return True, if the hidden tab is part of the stack, which is located at the start, false
     * otherwise
     */
    protected final boolean isStackedAtStart(final int index) {
        boolean start = true;
        AbstractItemIterator iterator =
                new ItemIterator.Builder(getTabSwitcher(), getTabViewRecycler()).start(index + 1)
                        .create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            State state = item.getTag().getState();

            if (state == State.STACKED_START) {
                start = true;
                break;
            } else if (state == State.FLOATING) {
                start = false;
                break;
            }
        }

        return start;
    }

    /**
     * Clips the position of a specific item.
     *
     * @param index
     *         The index of the item, whose position should be clipped, as an {@link Integer} value
     * @param position
     *         The position, which should be clipped, in pixels as a {@link Float} value
     * @param predecessor
     *         The predecessor of the given item as an instance of the class {@link AbstractItem} or
     *         null, if the item does not have a predecessor
     * @return A pair, which contains the position and state of the item, as an instance of the
     * class Pair. The pair may not be null
     */
    @NonNull
    protected final Pair<Float, State> clipPosition(final int index, final float position,
                                                    @Nullable final AbstractItem predecessor) {
        return clipPosition(index, position,
                predecessor != null ? predecessor.getTag().getState() : null);
    }

    /**
     * Clips the position of a specific item.
     *
     * @param index
     *         The index of the item, whose position should be clipped, as an {@link Integer} value
     * @param position
     *         The position, which should be clipped, in pixels as a {@link Float} value
     * @param predecessorState
     *         The state of the predecessor of the given item as a value of the enum {@link State}
     *         or null, if the item does not have a predecessor
     * @return A pair, which contains the position and state of the item, as an instance of the
     * class Pair. The pair may not be null
     */
    protected final Pair<Float, State> clipPosition(final int index, final float position,
                                                    @Nullable final State predecessorState) {
        Pair<Float, State> startPair =
                calculatePositionAndStateWhenStackedAtStart(getItemCount(), index,
                        predecessorState);
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
     * Calculates and returns the position and state of a specific item, when stacked at the start.
     *
     * @param count
     *         The total number of items, which are currently contained by the tab switcher, as an
     *         {@link Integer} value
     * @param index
     *         The index of the item, whose position and state should be returned, as an {@link
     *         Integer} value
     * @param predecessor
     *         The predecessor of the given item as an instance of the class {@link AbstractItem} or
     *         null, if the item does not have a predecessor
     * @return A pair, which contains the position and state of the given item, when stacked at the
     * start, as an instance of the class Pair. The pair may not be null
     */
    @NonNull
    protected final Pair<Float, State> calculatePositionAndStateWhenStackedAtStart(final int count,
                                                                                   final int index,
                                                                                   @Nullable final AbstractItem predecessor) {
        return calculatePositionAndStateWhenStackedAtStart(count, index,
                predecessor != null ? predecessor.getTag().getState() : null);
    }

    /**
     * Inflates or removes the view, which is used to visualize a specific item, depending on the
     * item's current state.
     *
     * @param item
     *         The item, whose view should be inflated or removed, as an instance of the class
     *         {@link AbstractItem}. The item may not be null
     * @param dragging
     *         True, if the item is currently being dragged, false otherwise
     */
    protected final void inflateOrRemoveView(@NonNull final AbstractItem item,
                                             final boolean dragging) {
        if (item.isInflated() && !item.isVisible()) {
            getTabViewRecycler().remove(item);
        } else if (item.isVisible()) {
            if (!item.isInflated()) {
                inflateAndUpdateView(item, dragging, null);
            } else {
                updateView(item, dragging);
            }
        }
    }

    /**
     * Inflates the view, which is used to visualize a specific item.
     *
     * @param item
     *         The item, whose view should be inflated, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param listener
     *         The layout listener, which should be notified, when the view has been inflated, as an
     *         instance of the type {@link OnGlobalLayoutListener} or null, if no listener should be
     *         notified
     * @param params
     *         An array, which contains optional parameters, which should be passed to the view
     *         recycler, which is used to inflate the view, as an {@link Integer} array or null, if
     *         no optional parameters should be used
     */
    protected final void inflateView(@NonNull final AbstractItem item,
                                     @Nullable final OnGlobalLayoutListener listener,
                                     @NonNull final Integer... params) {
        Pair<View, Boolean> pair = getTabViewRecycler().inflate(item, params);

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
     * Updates the view, which is used to visualize a specific item.
     *
     * @param item
     *         The item, whose view should be updated, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param dragging
     *         True, if the item is currently being dragged, false otherwise
     */
    @CallSuper
    protected void updateView(@NonNull final AbstractItem item, final boolean dragging) {
        float position = item.getTag().getPosition();
        getArithmetics().setPosition(Axis.DRAGGING_AXIS, item, position);
        getArithmetics().setPosition(Axis.ORTHOGONAL_AXIS, item, 0);
    }

    /**
     * Calculates and returns the position on the dragging axis, where the distance between an item
     * and its predecessor should have reached the maximum.
     *
     * @param count
     *         The total number of items, which are contained by the tab switcher, as an {@link
     *         Integer} value
     * @return The position, which has been calculated, in pixels as an {@link Float} value or -1,
     * if no attached position is used
     */
    protected float calculateAttachedPosition(final int count) {
        return -1;
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
     * @param style
     *         The style, which allows to retrieve style attributes of the tab switcher, as an
     *         instance of the class {@link TabSwitcherStyle}. The style may not be null
     * @param touchEventDispatcher
     *         The dispatcher, which is used to dispatch touch events to event handlers, as an
     *         instance of the class {@link TouchEventDispatcher}. The dispatcher may not be null
     */
    public AbstractTabSwitcherLayout(@NonNull final TabSwitcher tabSwitcher,
                                     @NonNull final TabSwitcherModel model,
                                     @NonNull final Arithmetics arithmetics,
                                     @NonNull final TabSwitcherStyle style,
                                     @NonNull final TouchEventDispatcher touchEventDispatcher) {
        Condition.INSTANCE.ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        Condition.INSTANCE.ensureNotNull(model, "The model may not be null");
        Condition.INSTANCE.ensureNotNull(arithmetics, "The arithmetics may not be null");
        Condition.INSTANCE.ensureNotNull(style, "The style may not be null");
        Condition.INSTANCE.ensureNotNull(touchEventDispatcher, "The dispatcher may not be null");
        this.tabSwitcher = tabSwitcher;
        this.model = model;
        this.arithmetics = arithmetics;
        this.style = style;
        this.touchEventDispatcher = touchEventDispatcher;
        Resources resources = tabSwitcher.getResources();
        this.stackedTabSpacing = resources.getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        this.logger = new Logger(model.getLogLevel());
        this.callback = null;
        this.runningAnimations = 0;
        this.flingAnimation = null;
        this.firstVisibleIndex = -1;
    }

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the drag
     * handler, which is used by the layout.
     *
     * @return The drag handler, which is used by the layout, as an instance of the class {@link
     * AbstractDragTabsEventHandler} or null, if the drag handler has not been initialized yet
     */
    public abstract AbstractDragTabsEventHandler<?> getDragHandler();

    /**
     * The method, which is invoked on implementing subclasses in order to inflate the layout.
     *
     * @param inflater
     *         The layout inflater, which should be used, as an instance of the class {@link
     *         LayoutInflater}. The layout inflater may not be null
     * @param tabsOnly
     *         True, if only the tabs should be inflated, false otherwise
     */
    protected abstract void onInflateLayout(@NonNull final LayoutInflater inflater,
                                            final boolean tabsOnly);

    /**
     * The method, which is invoked on implementing subclasses in order to detach the layout.
     *
     * @param tabsOnly
     *         True, if only the tabs should be detached, false otherwise
     * @return A pair, which contains the index of the tab, which should be used as a reference,
     * when restoring the positions of tabs, as well as its current position in relation to the
     * available space, as an instance of the class Pair or null, if the positions of tabs should
     * not be restored
     */
    @Nullable
    protected abstract Pair<Integer, Float> onDetachLayout(final boolean tabsOnly);

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the view
     * recycler, which allows to recycle the views, which are associated with tabs.
     *
     * @return The view recycler, which allows to recycle the views, which are associated with tabs,
     * as an instance of the class ViewRecycler or null, if the view recycler has not been
     * initialized yet
     */
    public abstract AbstractViewRecycler<Tab, Void> getContentViewRecycler();

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the view
     * recycler, which allows to inflate the views, which are used to visualize the tabs.
     *
     * @return The view recycler, which allows to inflate the views, which are used to visualize the
     * tabs, as an instance of the class AttachedViewRecycler or null, if the view recycler has not
     * been initialized yet
     */
    protected abstract AttachedViewRecycler<AbstractItem, Integer> getTabViewRecycler();

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the adapter of
     * the view recycler, which allows to inflate the views, which are used to visualize the tabs.
     *
     * @return The adapter of the view recycler, which allows to inflated the views, which are used
     * to visualize the tabs, as an instance of the class {@link AbstractTabRecyclerAdapter} or
     * null, if the view recycler has not been initialized yet
     */
    protected abstract AbstractTabRecyclerAdapter getTabRecyclerAdapter();

    /**
     * The method, which is invoked on implementing subclasses in order to inflate and update the
     * view, which is used to visualize a specific item.
     *
     * @param item
     *         The item, whose view should be inflated, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param dragging
     *         True, if the view is currently being dragged, false otherwise
     * @param listener
     *         The layout listener, which should be notified, when the view has been inflated, as an
     *         instance of the type {@link OnGlobalLayoutListener} or null, if no listener should be
     *         notified
     */
    protected abstract void inflateAndUpdateView(@NonNull final AbstractItem item,
                                                 final boolean dragging,
                                                 @Nullable final OnGlobalLayoutListener listener);

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the number of
     * tabs, which are contained by a stack.
     *
     * @return The number of tabs, which are contained by a stack, as an {@link Integer} value
     */
    protected abstract int getStackedTabCount();

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the position and
     * state of a specific item, when stacked at the start.
     *
     * @param count
     *         The total number of items, which are currently contained by the tab switcher, as an
     *         {@link Integer} value
     * @param index
     *         The index of the item, whose position and state should be returned, as an {@link
     *         Integer} value
     * @param predecessorState
     *         The state of the predecessor of the given item as a value of the enum {@link State}
     *         or null, if the item does not have a predecessor
     * @return A pair, which contains the position and state of the given item, when stacked at the
     * start, as an instance of the class Pair. The pair may not be null
     */
    @NonNull
    protected abstract Pair<Float, State> calculatePositionAndStateWhenStackedAtStart(
            final int count, final int index, @Nullable final State predecessorState);

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the position and
     * state of a specific item, when stacked at the end.
     *
     * @param index
     *         The index of the item, whose position and state should be returned, as an {@link
     *         Integer} value
     * @return A pair, which contains the position and state of the given item, when stacked at the
     * end, as an instance of the class Pair. The pair may not be null
     */
    @NonNull
    protected abstract Pair<Float, State> calculatePositionAndStateWhenStackedAtEnd(
            final int index);

    /**
     * Calculates the position of an item in relation to the position of its predecessor.
     *
     * @param item
     *         The item, whose position should be calculated, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param predecessor
     *         The predecessor as an instance of the class {@link AbstractItem}. The predecessor may
     *         not be null
     * @return The position, which has been calculated, as a {@link Float} value
     */
    protected abstract float calculateSuccessorPosition(@NonNull final AbstractItem item,
                                                        @NonNull final AbstractItem predecessor);

    /**
     * Calculates the position of an item in relation to the position of its successor.
     *
     * @param item
     *         The item, whose position should be calculated, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param successor
     *         The successor as an instance of the class {@link AbstractItem}. The successor may not
     *         be null
     * @return The position, which has been calculated, as a {@link Float} value
     */
    protected abstract float calculatePredecessorPosition(@NonNull final AbstractItem item,
                                                          @NonNull final AbstractItem successor);

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the minimum
     * position of a specific item, when dragging towards the start.
     *
     * @param index
     *         The index of the item, whose position should be calculated, as an {@link Integer}
     *         value
     * @return The position, which has been calculated, as a {@link Float} value or -1, if no
     * minimum position is available
     */
    protected float calculateMinStartPosition(final int index) {
        return -1;
    }

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the maximum
     * position of a specific item, when dragging towards the end.
     *
     * @param index
     *         The index of the item, whose position should be calculated, as an {@link Integer}
     *         value
     * @return The position, which has been calculated, as a {@link Float} value or -1, if no
     * maximum position is available
     */
    protected float calculateMaxEndPosition(final int index) {
        return -1;
    }

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve, whether the
     * items are overshooting at the start.
     *
     * @return True, if the items are overshooting at the start, false otherwise
     */
    protected boolean isOvershootingAtStart() {
        return false;
    }

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve, whether the
     * items are overshooting at the end.
     *
     * @param dragState
     *         The current drag state as an instance of the enum {@link DragState}. The drag state
     *         may not be null
     * @param iterator
     *         An iterator, which allows to iterate the items, which are contained by the tab
     *         switcher, as an instance of the class {@link AbstractItemIterator}. The iterator may
     *         not be null
     * @return True, if the items are overshooting at the end, false otherwise
     */
    protected boolean isOvershootingAtEnd(@NonNull final DragState dragState,
                                          @NonNull final AbstractItemIterator iterator) {
        return false;
    }

    /**
     * The method, which is called when dragging after the positions and states of all tabs have
     * been calculated. It may be overridden by subclasses in order to implement a second layout
     * pass, which requires the information, which has been calculated in the first pass, and allows
     * to perform additional modifications of the tabs based on that information.
     *
     * @param builder
     *         The builder, which allows to create the iterator, which should be used to iterate the
     *         tabs, as an instance of the class {@link AbstractItemIterator.AbstractBuilder}. The
     *         builder may not be null
     */
    protected void secondLayoutPass(@NonNull final AbstractItemIterator.AbstractBuilder builder) {

    }

    /**
     * The method, which is invoked on implementing subclasses in order to create the view recycler
     * adapter, which allows to inflate the views, which are associated with tabs.
     *
     * @return The view recycler adapter, which has been created, as an instance of the class
     * AttachedViewRecycler.Adapter. The recycler adapter may not be null }
     */
    @NonNull
    protected AttachedViewRecycler.Adapter<Tab, Void> onCreateContentRecyclerAdapter() {
        return getModel().getContentRecyclerAdapter();
    }

    /**
     * Inflates the layout.
     *
     * @param tabsOnly
     *         True, if only the tabs should be inflated, false otherwise
     */
    public final void inflateLayout(final boolean tabsOnly) {
        int themeResourceId = style.getThemeHelper().getThemeResourceId(tabSwitcher.getLayout());
        LayoutInflater inflater =
                LayoutInflater.from(new ContextThemeWrapper(getContext(), themeResourceId));
        onInflateLayout(inflater, tabsOnly);
        registerEventHandlerCallbacks();
        adaptDecorator();
        adaptLogLevel();

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
     * position in relation to the available space, as an instance of the class Pair or null, if the
     * tab switcher is not shown
     */
    @Nullable
    public final Pair<Integer, Float> detachLayout(final boolean tabsOnly) {
        Pair<Integer, Float> pair = onDetachLayout(tabsOnly);
        getTabViewRecycler().removeAll();
        getTabViewRecycler().clearCache();
        unregisterEventHandlerCallbacks();
        touchEventDispatcher.removeEventHandler(getDragHandler());

        if (!tabsOnly) {
            getTabSwitcher().removeAllViews();
        }

        return pair;
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
            Toolbar toolbar = toolbars.length > 1 ? toolbars[TabSwitcher.SECONDARY_TOOLBAR_INDEX] :
                    toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX];
            return toolbar.getMenu();
        }

        return null;
    }

    @Override
    public final void onLogLevelChanged(@NonNull final LogLevel logLevel) {
        adaptLogLevel();
    }

    @CallSuper
    @Override
    public void onDecoratorChanged(@NonNull final TabSwitcherDecorator decorator) {
        adaptDecorator();
        detachLayout(true);
        onGlobalLayout();
    }

    @Override
    public void onAddTabButtonVisibilityChanged(final boolean visible) {

    }

    @Override
    public final void onAddTabButtonColorChanged(@Nullable final ColorStateList colorStateList) {

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
    public final void onTabIconChanged(@Nullable final Drawable icon) {

    }

    @Override
    public void onTabBackgroundColorChanged(@Nullable final ColorStateList colorStateList) {

    }

    @Override
    public void onTabContentBackgroundColorChanged(@ColorInt final int color) {

    }

    @Override
    public final void onTabTitleColorChanged(@Nullable final ColorStateList colorStateList) {

    }

    @Override
    public final void onTabCloseButtonIconChanged(@Nullable final Drawable icon) {

    }

    @Override
    public final void onTabProgressBarColorChanged(@ColorInt final int color) {

    }

    @Override
    public void onSwitcherShown() {

    }

    @Override
    public void onSwitcherHidden() {

    }

    @Override
    public final void onAddedEventHandler(@NonNull final TouchEventDispatcher dispatcher,
                                          @NonNull final AbstractTouchEventHandler eventHandler) {
        registerEventHandlerCallback(eventHandler);
    }

    @Override
    public final void onRemovedEventHandler(@NonNull final TouchEventDispatcher dispatcher,
                                            @NonNull final AbstractTouchEventHandler eventHandler) {
        unregisterEventHandlerCallback(eventHandler);
    }

    @Nullable
    @Override
    public final DragState onDrag(@NonNull final DragState dragState, final float dragDistance) {
        if (dragDistance != 0) {
            if (dragState == DragState.DRAG_TO_END) {
                calculatePositionsWhenDraggingToEnd(dragDistance);
            } else {
                calculatePositionsWhenDraggingToStart(dragDistance);
            }

            secondLayoutPass(new ItemIterator.Builder(getTabSwitcher(), getTabViewRecycler()));
        }

        DragState overshoot = isOvershootingAtEnd(dragState,
                new ItemIterator.Builder(getTabSwitcher(), getTabViewRecycler()).create()) ?
                DragState.OVERSHOOT_END :
                (isOvershootingAtStart() ? DragState.OVERSHOOT_START : null);
        getLogger().logVerbose(getClass(),
                "Dragging using a distance of " + dragDistance + " pixels. Drag state is " +
                        dragState + ", overshoot is " + overshoot);
        return overshoot;
    }

    @Override
    public final void onPressStarted(@NonNull final AbstractItem item) {
        ColorStateList colorStateList = null;
        boolean selected = false;

        if (item instanceof TabItem) {
            TabItem tabItem = (TabItem) item;
            Tab tab = tabItem.getTab();
            colorStateList = style.getTabBackgroundColor(tab);
            selected = getModel().getSelectedTab() == tab;
        } else if (item instanceof AddTabItem) {
            colorStateList = style.getAddTabButtonColor();
        }

        if (colorStateList != null) {
            int[] stateSet = selected ?
                    new int[]{android.R.attr.state_pressed, android.R.attr.state_selected} :
                    new int[]{android.R.attr.state_pressed};
            int color = colorStateList.getColorForState(stateSet, -1);

            if (color != -1) {
                View view = item.getView();
                Drawable background = view.getBackground();
                background.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            }
        }
    }

    @Override
    public final void onPressEnded(@NonNull final AbstractItem item) {
        getTabRecyclerAdapter().onTabBackgroundColorChanged(getModel().getTabBackgroundColor());
    }

    @Override
    public final void onClick(@NonNull final AbstractItem item) {
        if (item instanceof TabItem) {
            TabItem tabItem = (TabItem) item;
            getModel().selectTab(tabItem.getTab());
            getLogger().logVerbose(getClass(), "Clicked tab at index " +
                    (tabItem.getIndex() - (getModel().isAddTabButtonShown() ? 1 : 0)));
        } else if (item instanceof AddTabItem) {
            AddTabButtonListener listener = getModel().getAddTabButtonListener();

            if (listener != null) {
                listener.onAddTab(getTabSwitcher());
            }

            getLogger().logVerbose(getClass(), "Clicked add tab button");
        }
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
            getDragHandler().onUp(null);
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

    @Override
    public void onPulledDown() {

    }

}