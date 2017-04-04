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
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;

import java.util.Collections;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.RevealAnimation;
import de.mrapp.android.tabswitcher.SwipeAnimation;
import de.mrapp.android.tabswitcher.SwipeAnimation.SwipeDirection;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabCloseListener;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.arithmetic.Arithmetics;
import de.mrapp.android.tabswitcher.arithmetic.PhoneArithmetics;
import de.mrapp.android.tabswitcher.iterator.AbstractTabItemIterator;
import de.mrapp.android.tabswitcher.iterator.ArrayTabItemIterator;
import de.mrapp.android.tabswitcher.iterator.InitialTabItemIterator;
import de.mrapp.android.tabswitcher.iterator.TabItemIterator;
import de.mrapp.android.tabswitcher.model.Axis;
import de.mrapp.android.tabswitcher.model.DragState;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.Tag;
import de.mrapp.android.tabswitcher.view.ChildRecyclerAdapter;
import de.mrapp.android.tabswitcher.view.RecyclerAdapter;
import de.mrapp.android.tabswitcher.view.TabViewHolder;
import de.mrapp.android.util.DisplayUtil.Orientation;
import de.mrapp.android.util.ViewUtil;
import de.mrapp.android.util.view.AttachedViewRecycler;
import de.mrapp.android.util.view.ViewRecycler;

import static de.mrapp.android.util.Condition.ensureGreater;
import static de.mrapp.android.util.Condition.ensureTrue;
import static de.mrapp.android.util.DisplayUtil.getOrientation;

/**
 * A layout, which implements the functionality of a {@link TabSwitcher} on smartphones.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class PhoneTabSwitcherLayout extends AbstractTabSwitcherLayout
        implements DragHandler.Callback {

    /**
     * A layout listener, which encapsulates another listener, which is notified, when the listener
     * has been invoked a specific number of times.
     */
    private class CompoundLayoutListener implements OnGlobalLayoutListener {

        /**
         * The number of times, the listener must still be invoked, until the encapsulated listener
         * is notified.
         */
        private int count;

        /**
         * The encapsulated listener;
         */
        private final OnGlobalLayoutListener listener;

        /**
         * Creates a new layout listener, which encapsulates another listener, which is notified,
         * when the listener has been invoked a specific number of times.
         *
         * @param count
         *         The number of times, the listener should be invoked until the encapsulated
         *         listener is notified, as an {@link Integer} value. The count must be greater than
         *         0
         * @param listener
         *         The encapsulated listener, which should be notified, when the listener has been
         *         notified the given number of times, as an instance of the type {@link
         *         OnGlobalLayoutListener} or null, if no listener should be notified
         */
        CompoundLayoutListener(final int count, @Nullable final OnGlobalLayoutListener listener) {
            ensureGreater(count, 0, "The count must be greater than 0");
            this.count = count;
            this.listener = listener;
        }

        @Override
        public void onGlobalLayout() {
            if (--count == 0) {
                if (listener != null) {
                    listener.onGlobalLayout();
                }
            }
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
                dragHandler.handleDrag(new TabItemIterator.Factory(getTabSwitcher(), viewRecycler),
                        distance * interpolatedTime, 0);
            }
        }

    }

    /**
     * The arithmetics, which are used to calculate the positions, size and rotation of tabs.
     */
    private final Arithmetics arithmetics;

    /**
     * The threshold, which must be reached until tabs are dragged, in pixels.
     */
    private final int dragThreshold;

    /**
     * The inset of tabs in pixels.
     */
    private final int tabInset;

    /**
     * The width of the border, which is drawn around the preview of tabs.
     */
    private final int tabBorderWidth;

    /**
     * The height of a tab's title container in pixels.
     */
    private final int tabTitleContainerHeight;

    /**
     * The number of tabs, which are contained by a stack.
     */
    private final int stackedTabCount;

    /**
     * The space between tabs, which are part of a stack, in pixels.
     */
    private final int stackedTabSpacing;

    /**
     * The maximum camera distance, when tilting a tab, in pixels.
     */
    private final int maxCameraDistance;

    /**
     * The alpha of a tab, when it is swiped.
     */
    private final float swipedTabAlpha;

    /**
     * The scale of a tab, when it is swiped.
     */
    private final float swipedTabScale;

    /**
     * The duration of the animation, which is used to show the switcher.
     */
    private final long showSwitcherAnimationDuration;

    /**
     * The duration of the animation, which is used to hide the switcher.
     */
    private final long hideSwitcherAnimationDuration;

    /**
     * The duration of the animation, which is used to show or hide the toolbar.
     */
    private final long toolbarVisibilityAnimationDuration;

    /**
     * The delay of the animation, which is used to show or hide the toolbar.
     */
    private final long toolbarVisibilityAnimationDelay;

    /**
     * The duration of the animation, which is used to swipe tabs.
     */
    private final long swipeAnimationDuration;

    /**
     * The delay of the animation, which is used to remove all tabs.
     */
    private final long clearAnimationDelay;

    /**
     * The duration of the animation, which is used to relocate tabs.
     */
    private final long relocateAnimationDuration;

    /**
     * The delay of the animation, which is used to relocate tabs.
     */
    private final long relocateAnimationDelay;

    /**
     * The duration of the animation, which is used to revert overshoots.
     */
    private final long revertOvershootAnimationDuration;

    /**
     * The duration of a reveal animation.
     */
    private final long revealAnimationDuration;

    /**
     * The view recycler, which allows to recycler the child views of tabs.
     */
    private ViewRecycler<Tab, Void> childViewRecycler;

    /**
     * The adapter, which allows to inflate the views, which are used to visualize tabs.
     */
    private RecyclerAdapter recyclerAdapter;

    /**
     * The view recycler, which allows to recycle the views, which are used to visualize tabs.
     */
    private AttachedViewRecycler<TabItem, Integer> viewRecycler;

    /**
     * The drag handler, which is used to calculate the positions of tabs.
     */
    private DragHandler dragHandler;

    /**
     * The view group, which contains the tab switcher's tabs.
     */
    private ViewGroup tabContainer;

    /**
     * The toolbar, which is shown, when the tab switcher is shown.
     */
    private Toolbar toolbar;

    /**
     * The bottom margin of a view, which visualizes a tab.
     */
    private int tabViewBottomMargin;

    /**
     * The animation, which is used to show or hide the toolbar.
     */
    private ViewPropertyAnimator toolbarAnimation;

    /**
     * The animation, which is used to fling the tabs.
     */
    private android.view.animation.Animation flingAnimation;

    /**
     * Calculates and returns the bottom margin of a view, which visualizes a tab.
     *
     * @param view
     *         The view, whose bottom margin should be calculated, as an instance of the class
     *         {@link View}. The view may not be null
     * @return The bottom margin, which has been calculated, in pixels as an {@link Integer} value
     */
    private int calculateBottomMargin(@NonNull final View view) {
        float tabHeight = (view.getHeight() - 2 * tabInset) * arithmetics.getScale(view, true);
        float containerHeight = arithmetics.getSize(Axis.Y_AXIS, tabContainer);
        int toolbarHeight = isToolbarShown() ? toolbar.getHeight() - tabInset : 0;
        int stackHeight =
                getLayout() == Layout.PHONE_LANDSCAPE ? 0 : stackedTabCount * stackedTabSpacing;
        return Math.round(tabHeight + tabInset + toolbarHeight + stackHeight -
                (containerHeight - getPaddingTop() - getPaddingBottom()));
    }

    /**
     * Animates the bottom margin of a specific view.
     *
     * @param view
     *         The view, whose bottom margin should be animated, as an instance of the class {@link
     *         View}. The view may not be null
     * @param margin
     *         The bottom margin, which should be set by the animation, as an {@link Integer} value
     * @param animationDuration
     *         The duration of the animation in milliseconds as a {@link Long} value
     */
    private void animateBottomMargin(@NonNull final View view, final int margin,
                                     final long animationDuration) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        final int initialMargin = layoutParams.bottomMargin;
        ValueAnimator animation = ValueAnimator.ofInt(margin - initialMargin);
        animation.setDuration(animationDuration);
        animation.addListener(new AnimationListenerWrapper(null));
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setStartDelay(0);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) view.getLayoutParams();
                layoutParams.bottomMargin = initialMargin + (int) animation.getAnimatedValue();
                view.setLayoutParams(layoutParams);
            }

        });

        animation.start();
    }

    /**
     * Animates the visibility of the toolbar, which is shown, when the tab switcher is shown.
     *
     * @param visible
     *         True, if the toolbar should become visible, false otherwise
     * @param delay
     *         The delay of the animation in milliseconds as a {@link Long} value
     */
    private void animateToolbarVisibility(final boolean visible, final long delay) {
        if (toolbarAnimation != null) {
            toolbarAnimation.cancel();
        }

        float targetAlpha = visible ? 1 : 0;

        if (toolbar.getAlpha() != targetAlpha) {
            toolbarAnimation = toolbar.animate();
            toolbarAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            toolbarAnimation.setDuration(toolbarVisibilityAnimationDuration);
            toolbarAnimation.setStartDelay(delay);
            toolbarAnimation.alpha(targetAlpha);
            toolbarAnimation.start();
        }
    }

    /**
     * Shows the tab switcher in an animated manner.
     */
    private void animateShowSwitcher() {
        TabItem[] tabItems = calculateInitialTabItems();
        AbstractTabItemIterator iterator =
                new InitialTabItemIterator.Builder(getTabSwitcher(), viewRecycler, dragHandler,
                        tabItems).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.getTab() == getModel().getSelectedTab() || tabItem.isVisible()) {
                viewRecycler.inflate(tabItem);
                View view = tabItem.getView();

                if (!ViewCompat.isLaidOut(view)) {
                    view.getViewTreeObserver().addOnGlobalLayoutListener(
                            new LayoutListenerWrapper(view,
                                    createShowSwitcherLayoutListener(tabItem)));
                } else {
                    animateShowSwitcher(tabItem);
                }
            }
        }
    }

    /**
     * Calculates and returns the tab items, which correspond to the tabs, when the tab switcher is
     * shown initially.
     *
     * @return An array, which contains the tab items, as an array of the type {@link TabItem}. The
     * array may not be null
     */
    @NonNull
    private TabItem[] calculateInitialTabItems() {
        int count = getTabSwitcher().getCount();
        dragHandler.reset(dragThreshold);
        dragHandler.setMaxTabSpacing(calculateMaxTabSpacing(count));
        TabItem[] tabItems = new TabItem[count];

        if (!getModel().isEmpty()) {
            int selectedTabIndex = getModel().getSelectedTabIndex();
            AbstractTabItemIterator.Factory factory =
                    new InitialTabItemIterator.Factory(getTabSwitcher(), viewRecycler, dragHandler,
                            tabItems);
            AbstractTabItemIterator iterator = factory.create().start(selectedTabIndex).create();
            TabItem tabItem;

            while ((tabItem = iterator.next()) != null) {
                TabItem predecessor = iterator.previous();
                float position;

                if (tabItem.getIndex() == count - 1) {
                    position = 0;
                } else if (tabItem.getIndex() == selectedTabIndex) {
                    position = dragHandler.getAttachedPosition(false, count);
                } else {
                    position = dragHandler.calculateNonLinearPosition(tabItem, predecessor);
                }

                Pair<Float, State> pair = dragHandler
                        .clipTabPosition(count, tabItem.getIndex(), position, predecessor);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);

                if (pair.second != State.FLOATING) {
                    break;
                }
            }

            boolean overshooting =
                    selectedTabIndex == count - 1 || dragHandler.isOvershootingAtEnd(factory);
            iterator = factory.create().create();
            float defaultTabSpacing = dragHandler.calculateMaxTabSpacing(count, null);
            TabItem selectedTabItem =
                    TabItem.create(getTabSwitcher(), viewRecycler, selectedTabIndex);
            float maxTabSpacing = dragHandler.calculateMaxTabSpacing(count, selectedTabItem);

            while ((tabItem = iterator.next()) != null &&
                    (overshooting || tabItem.getIndex() < selectedTabIndex)) {
                float position;

                if (overshooting) {
                    if (selectedTabIndex > tabItem.getIndex()) {
                        position = maxTabSpacing +
                                ((count - 1 - tabItem.getIndex() - 1) * defaultTabSpacing);
                    } else {
                        position = (count - 1 - tabItem.getIndex()) * defaultTabSpacing;
                    }
                } else {
                    position = dragHandler.getAttachedPosition(false, count) + maxTabSpacing +
                            ((selectedTabIndex - tabItem.getIndex() - 1) * defaultTabSpacing);
                }

                Pair<Float, State> pair = dragHandler
                        .clipTabPosition(count, tabItem.getIndex(), position, iterator.previous());
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);

                if (dragHandler.getFirstVisibleIndex() == -1 && pair.second == State.FLOATING) {
                    dragHandler.setFirstVisibleIndex(tabItem.getIndex());
                }
            }

            if (dragHandler.getFirstVisibleIndex() == -1) {
                dragHandler.setFirstVisibleIndex(selectedTabIndex);
            }
        }

        dragHandler.setCallback(PhoneTabSwitcherLayout.this);
        return tabItems;
    }

    /**
     * Calculates and returns the maximum space between two neighboring tabs, depending on the
     * number of tabs, which are contained by the tab switcher.
     *
     * @param count
     *         The total number of tabs, which are contained by the tabs switcher, as an {@link
     *         Integer} value
     * @return The maximum space, which has been calculated, in pixels as a {@link Float} value
     */
    private float calculateMaxTabSpacing(final int count) {
        float totalSpace = arithmetics.getSize(Axis.DRAGGING_AXIS, tabContainer) -
                (getLayout() == Layout.PHONE_PORTRAIT && isToolbarShown() ?
                        toolbar.getHeight() + tabInset : 0);

        if (count <= 2) {
            return totalSpace * 0.66f;
        } else if (count == 3) {
            return totalSpace * 0.33f;
        } else if (count == 4) {
            return totalSpace * 0.3f;
        } else {
            return totalSpace * 0.25f;
        }
    }

    /**
     * Adds all tabs, which are contained by an array, to the tab switcher.
     *
     * @param index
     *         The index, the first tab should be added at, as an {@link Integer} value
     * @param tabs
     *         The array, which contains the tabs, which should be added, as an array of the type
     *         {@link Tab}. The array may not be null
     * @param animation
     *         The animation, which should be used to add the tabs, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    private void addAllTabs(final int index, @NonNull final Tab[] tabs,
                            @NonNull final Animation animation) {
        if (tabs.length > 0) {
            if (getModel().isSwitcherShown()) {
                SwipeAnimation swipeAnimation =
                        animation instanceof SwipeAnimation ? (SwipeAnimation) animation :
                                new SwipeAnimation.Builder().create();
                TabItem[] tabItems = new TabItem[tabs.length];

                for (int i = 0; i < tabs.length; i++) {
                    Tab tab = tabs[i];
                    tabItems[i] = new TabItem(index + i, tab);
                }

                inflateViews(tabItems, createSwipeLayoutListener(tabItems, swipeAnimation));
            } else if (!getModel().isSwitcherShown()) {
                toolbar.setAlpha(0);

                if (getModel().getSelectedTab() == tabs[0]) {
                    TabItem tabItem = TabItem.create(getTabSwitcher(), viewRecycler, index);
                    inflateView(tabItem, createAddSelectedTabLayoutListener(tabItem));
                }
            }
        }
    }

    /**
     * Animates the position and size of a specific tab item in order to show the tab switcher.
     *
     * @param tabItem
     *         The tab item, which should be animated, as an instance of the class {@link TabItem}.
     *         The tab item may not be null
     */
    private void animateShowSwitcher(@NonNull final TabItem tabItem) {
        View view = tabItem.getView();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        view.setX(layoutParams.leftMargin);
        view.setY(layoutParams.topMargin);
        arithmetics.setScale(Axis.DRAGGING_AXIS, view, 1);
        arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, 1);
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.NONE));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.NONE));
        float scale = arithmetics.getScale(view, true);
        int selectedTabIndex = getModel().getSelectedTabIndex();

        if (tabItem.getIndex() < selectedTabIndex) {
            arithmetics.setPosition(Axis.DRAGGING_AXIS, view,
                    arithmetics.getSize(Axis.DRAGGING_AXIS, tabContainer));
        } else if (tabItem.getIndex() > selectedTabIndex) {
            arithmetics.setPosition(Axis.DRAGGING_AXIS, view,
                    getLayout() == Layout.PHONE_LANDSCAPE ? 0 : layoutParams.topMargin);
        }

        if (tabViewBottomMargin == -1) {
            tabViewBottomMargin = calculateBottomMargin(view);
        }

        animateBottomMargin(view, tabViewBottomMargin, showSwitcherAnimationDuration);
        ViewPropertyAnimator animation = view.animate();
        animation.setDuration(showSwitcherAnimationDuration);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setListener(
                new AnimationListenerWrapper(createUpdateViewAnimationListener(tabItem)));
        arithmetics.animateScale(Axis.DRAGGING_AXIS, animation, scale);
        arithmetics.animateScale(Axis.ORTHOGONAL_AXIS, animation, scale);
        arithmetics.animatePosition(Axis.DRAGGING_AXIS, animation, view,
                tabItem.getTag().getPosition(), true);
        arithmetics.animatePosition(Axis.ORTHOGONAL_AXIS, animation, view, 0, true);
        animation.setStartDelay(0);
        animation.start();
        animateToolbarVisibility(isToolbarShown(), toolbarVisibilityAnimationDelay);
    }

    /**
     * Hides the tab switcher in an animated manner.
     */
    private void animateHideSwitcher() {
        dragHandler.setCallback(null);
        TabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.isInflated()) {
                animateHideSwitcher(tabItem);
            } else if (tabItem.getTab() == getModel().getSelectedTab()) {
                inflateAndUpdateView(tabItem, createHideSwitcherLayoutListener(tabItem));
            }
        }
    }

    /**
     * Animates the position and size of a specific tab item in order to hide the tab switcher.
     *
     * @param tabItem
     *         The tab item, which should be animated, as an instance of the class {@link TabItem}.
     *         The tab item may not be null
     */
    private void animateHideSwitcher(@NonNull final TabItem tabItem) {
        int selectedTabIndex = getModel().getSelectedTabIndex();
        View view = tabItem.getView();
        animateBottomMargin(view, -(tabInset + tabBorderWidth), hideSwitcherAnimationDuration);
        AnimatorListener listener =
                tabItem.getIndex() == selectedTabIndex ? createHideSwitcherAnimationListener() :
                        null;
        ViewPropertyAnimator animation = view.animate();
        animation.setDuration(hideSwitcherAnimationDuration);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setListener(new AnimationListenerWrapper(listener));
        arithmetics.animateScale(Axis.DRAGGING_AXIS, animation, 1);
        arithmetics.animateScale(Axis.ORTHOGONAL_AXIS, animation, 1);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        arithmetics.animatePosition(Axis.ORTHOGONAL_AXIS, animation, view,
                getLayout() == Layout.PHONE_LANDSCAPE ? layoutParams.topMargin : 0, false);

        if (tabItem.getIndex() < selectedTabIndex) {
            arithmetics.animatePosition(Axis.DRAGGING_AXIS, animation, view,
                    arithmetics.getSize(Axis.DRAGGING_AXIS, getTabSwitcher()), false);
        } else if (tabItem.getIndex() > selectedTabIndex) {
            arithmetics.animatePosition(Axis.DRAGGING_AXIS, animation, view,
                    getLayout() == Layout.PHONE_LANDSCAPE ? 0 : layoutParams.topMargin, false);
        } else {
            arithmetics.animatePosition(Axis.DRAGGING_AXIS, animation, view,
                    getLayout() == Layout.PHONE_LANDSCAPE ? 0 : layoutParams.topMargin, false);
        }

        animation.setStartDelay(0);
        animation.start();
        animateToolbarVisibility(isToolbarShown() && getModel().isEmpty(), 0);
    }

    /**
     * Animates the position, size and alpha of a specific tab item in order to swipe it
     * orthogonally.
     *
     * @param tabItem
     *         The tab item, which should be animated, as an instance of the class {@link TabItem}.
     *         The tab item may not be null
     * @param remove
     *         True, if the tab should be removed after the animation has finished, false otherwise
     * @param delay
     *         The delay after which the animation should be started in milliseconds as a {@link
     *         Long} value
     * @param swipeAnimation
     *         The animation, which should be used, as an instance of the class {@link
     *         SwipeAnimation}. The animation may not be null
     * @param listener
     *         The listener, which should be notified about the progress of the animation, as an
     *         instance of the type {@link AnimatorListener} or null, if no listener should be
     *         notified
     */
    private void animateSwipe(@NonNull final TabItem tabItem, final boolean remove,
                              final long delay, @NonNull final SwipeAnimation swipeAnimation,
                              @Nullable final AnimatorListener listener) {
        View view = tabItem.getView();
        float currentScale = arithmetics.getScale(view, true);
        float swipePosition = dragHandler.calculateSwipePosition();
        float targetPosition = remove ?
                (swipeAnimation.getDirection() == SwipeDirection.LEFT ? -1 * swipePosition :
                        swipePosition) : 0;
        float currentPosition = arithmetics.getPosition(Axis.ORTHOGONAL_AXIS, view);
        float distance = Math.abs(targetPosition - currentPosition);
        long animationDuration = swipeAnimation.getDuration() != -1 ? swipeAnimation.getDuration() :
                Math.round(swipeAnimationDuration * (distance / swipePosition));
        ViewPropertyAnimator animation = view.animate();
        animation.setInterpolator(
                swipeAnimation.getInterpolator() != null ? swipeAnimation.getInterpolator() :
                        new AccelerateDecelerateInterpolator());
        animation.setListener(new AnimationListenerWrapper(listener));
        animation.setDuration(animationDuration);
        arithmetics.animatePosition(Axis.ORTHOGONAL_AXIS, animation, view, targetPosition, true);
        arithmetics.animateScale(Axis.ORTHOGONAL_AXIS, animation,
                remove ? swipedTabScale * currentScale : currentScale);
        arithmetics.animateScale(Axis.DRAGGING_AXIS, animation,
                remove ? swipedTabScale * currentScale : currentScale);
        animation.alpha(remove ? swipedTabAlpha : 1);
        animation.setStartDelay(delay);
        animation.start();
    }

    /**
     * Animates the removal of a specific tab item.
     *
     * @param removedTabItem
     *         The tab item, which should be animated, as an instance of the class {@link TabItem}.
     *         The tab item may not be null
     * @param swipeAnimation
     *         The animation, which should be used, as an instance of the class {@link
     *         SwipeAnimation}. The animation may not be null
     */
    private void animateRemove(@NonNull final TabItem removedTabItem,
                               @NonNull final SwipeAnimation swipeAnimation) {
        View view = removedTabItem.getView();
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.SWIPE));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.SWIPE));
        animateSwipe(removedTabItem, true, 0, swipeAnimation,
                createRemoveAnimationListener(removedTabItem));
    }

    /**
     * Animates the position of a specific tab item in order to relocate it.
     *
     * @param tabItem
     *         The tab item, which should be animated, as an instance of the class {@link TabItem}.
     *         The tab item may not be null
     * @param position
     *         The position, the tab should be relocated to, in pixels as a {@link Float} value
     * @param tag
     *         The tag, which should be applied to the given tab item, as an instance of the class
     *         {@link Tag} or null, if no tag should be applied
     * @param delay
     *         The delay of the relocate animation in milliseconds as a {@link Long} value
     * @param listener
     *         The listener, which should be notified about the progress of the relocate animation,
     *         as an instance of the type {@link AnimatorListener} or null, if no listener should be
     *         notified
     */
    private void animateRelocate(@NonNull final TabItem tabItem, final float position,
                                 @Nullable final Tag tag, final long delay,
                                 @Nullable final AnimatorListener listener) {
        if (tag != null) {
            tabItem.getView().setTag(R.id.tag_properties, tag);
            tabItem.setTag(tag);
        }

        View view = tabItem.getView();
        ViewPropertyAnimator animation = view.animate();
        animation.setListener(new AnimationListenerWrapper(listener));
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setDuration(relocateAnimationDuration);
        arithmetics.animatePosition(Axis.DRAGGING_AXIS, animation, view, position, true);
        animation.setStartDelay(delay);
        animation.start();
    }

    /**
     * Animates reverting an overshoot at the start.
     *
     * @param maxAngle
     *         The maximum angle, the tabs can be rotated by, in degrees as a {@link Float} value
     */
    private void animateRevertStartOvershoot(final float maxAngle) {
        boolean tilted = animateTilt(new AccelerateInterpolator(), maxAngle,
                createRevertStartOvershootAnimationListener());

        if (!tilted) {
            animateRevertStartOvershoot(new AccelerateDecelerateInterpolator());
        }
    }

    /**
     * Animates reverting an overshoot at the start using a specific interpolator.
     *
     * @param interpolator
     *         The interpolator, which should be used by the animation, as an instance of the type
     *         {@link Interpolator}. The interpolator may not be null
     */
    private void animateRevertStartOvershoot(@NonNull final Interpolator interpolator) {
        TabItem tabItem = TabItem.create(getTabSwitcher(), viewRecycler, 0);
        View view = tabItem.getView();
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.NONE));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.NONE));
        float position = arithmetics.getPosition(Axis.DRAGGING_AXIS, view);
        float targetPosition = tabItem.getTag().getPosition();
        final float startPosition = arithmetics.getPosition(Axis.DRAGGING_AXIS, view);
        ValueAnimator animation = ValueAnimator.ofFloat(targetPosition - position);
        animation.setDuration(Math.round(revertOvershootAnimationDuration * Math.abs(
                (targetPosition - position) / (float) (stackedTabCount * stackedTabSpacing))));
        animation.addListener(new AnimationListenerWrapper(null));
        animation.setInterpolator(interpolator);
        animation.setStartDelay(0);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                TabItemIterator iterator =
                        new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).create();
                TabItem tabItem;

                while ((tabItem = iterator.next()) != null) {
                    if (tabItem.getIndex() == 0) {
                        View view = tabItem.getView();
                        arithmetics.setPosition(Axis.DRAGGING_AXIS, view,
                                startPosition + (float) animation.getAnimatedValue());
                    } else if (tabItem.isInflated()) {
                        View firstView = iterator.first().getView();
                        View view = tabItem.getView();
                        view.setVisibility(arithmetics.getPosition(Axis.DRAGGING_AXIS, firstView) <=
                                arithmetics.getPosition(Axis.DRAGGING_AXIS, view) ? View.INVISIBLE :
                                View.VISIBLE);
                    }
                }
            }

        });

        animation.start();
    }

    /**
     * Animates reverting an overshoot at the end.
     *
     * @param maxAngle
     *         The maximum angle, the tabs can be rotated by, in degrees as a {@link Float} value
     */
    private void animateRevertEndOvershoot(final float maxAngle) {
        animateTilt(new AccelerateDecelerateInterpolator(), maxAngle, null);
    }

    /**
     * Animates to rotation of all tabs to be reset to normal.
     *
     * @param interpolator
     *         The interpolator, which should be used by the animation, as an instance of the type
     *         {@link Interpolator}. The interpolator may not be null
     * @param maxAngle
     *         The angle, the tabs may be rotated by at maximum, in degrees as a {@link Float}
     *         value
     * @param listener
     *         The listener, which should be notified about the animation's progress, as an instance
     *         of the type {@link AnimatorListener} or null, if no listener should be notified
     * @return True, if at least one tab was animated, false otherwise
     */
    private boolean animateTilt(@NonNull final Interpolator interpolator, final float maxAngle,
                                @Nullable final AnimatorListener listener) {
        TabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).reverse(true).create();
        TabItem tabItem;
        boolean result = false;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.isInflated()) {
                View view = tabItem.getView();

                if (arithmetics.getRotation(Axis.ORTHOGONAL_AXIS, view) != 0) {
                    ViewPropertyAnimator animation = view.animate();
                    animation.setListener(new AnimationListenerWrapper(
                            createRevertOvershootAnimationListener(view,
                                    !result ? listener : null)));
                    animation.setDuration(Math.round(revertOvershootAnimationDuration *
                            (Math.abs(arithmetics.getRotation(Axis.ORTHOGONAL_AXIS, view)) /
                                    maxAngle)));
                    animation.setInterpolator(interpolator);
                    arithmetics.animateRotation(Axis.ORTHOGONAL_AXIS, animation, 0);
                    animation.setStartDelay(0);
                    animation.start();
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Animates flinging the tabs.
     *
     * @param distance
     *         The distance, the tabs should be flinged, in pixels as a {@link Float} value
     * @param duration
     *         The duration of the fling in milliseconds as a {@link Long} value
     */
    private void animateFling(final float distance, final long duration) {
        flingAnimation = new FlingAnimation(distance);
        flingAnimation.setFillAfter(true);
        flingAnimation.setAnimationListener(createFlingAnimationListener());
        flingAnimation.setDuration(duration);
        flingAnimation.setInterpolator(new DecelerateInterpolator());
        getTabSwitcher().startAnimation(flingAnimation);
    }

    /**
     * Starts a reveal animation to add a specific tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which should be added, as an instance of
     *         the class {@link TabItem}. The tab item may not be null
     * @param revealAnimation
     *         The reveal animation, which should be started, as an instance of the class {@link
     *         RevealAnimation}. The reveal animation may not be null
     */
    private void animateReveal(@NonNull final TabItem tabItem,
                               @NonNull final RevealAnimation revealAnimation) {
        tabViewBottomMargin = -1;
        recyclerAdapter.clearCachedPreviews();
        dragHandler.setCallback(null);
        View view = tabItem.getView();
        ViewPropertyAnimator animation = view.animate();
        animation.setInterpolator(
                revealAnimation.getInterpolator() != null ? revealAnimation.getInterpolator() :
                        new AccelerateDecelerateInterpolator());
        animation.setListener(new AnimationListenerWrapper(createHideSwitcherAnimationListener()));
        animation.setStartDelay(0);
        animation.setDuration(revealAnimation.getDuration() != -1 ? revealAnimation.getDuration() :
                revealAnimationDuration);
        arithmetics.animateScale(Axis.DRAGGING_AXIS, animation, 1);
        arithmetics.animateScale(Axis.ORTHOGONAL_AXIS, animation, 1);
        animation.start();
        animateToolbarVisibility(isToolbarShown() && getModel().isEmpty(), 0);
    }

    /**
     * Creates and returns a layout listener, which allows to animate the position and size of a tab
     * in order to show the tab switcher, once its view has been inflated.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be animated, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createShowSwitcherLayoutListener(
            @NonNull final TabItem tabItem) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                animateShowSwitcher(tabItem);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to animate the position and size of a tab
     * in order to hide the tab switcher, once its view has been inflated.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be animated, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createHideSwitcherLayoutListener(
            @NonNull final TabItem tabItem) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                animateHideSwitcher(tabItem);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to remove a tab, once its view has been
     * inflated.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which should be removed, as an instance
     *         of the class {@link TabItem}. The tab item may not be null
     * @param swipeAnimation
     *         The animation, which should be used, as an instance of the class {@link
     *         SwipeAnimation}. The animation may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createRemoveLayoutListener(@NonNull final TabItem tabItem,
                                                              @NonNull final SwipeAnimation swipeAnimation) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                animateRemove(tabItem, swipeAnimation);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to relocate a tab, once its view has been
     * inflated.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which should be relocated, as an instance
     *         of the class {@link TabItem}. The tab item may not be null
     * @param position
     *         The position, the tab should be relocated to, in pixels as a {@link Float} value
     * @param tag
     *         The tag, which should be applied to the given tab item, as an instance of the class
     *         {@link Tag} or null, if no tag should be applied
     * @param delay
     *         The delay of the relocate animation in milliseconds as a {@link Long} value
     * @param listener
     *         The listener, which should be notified about the progress of the relocate animation,
     *         as an instance of the type {@link AnimatorListener} or null, if no listener should be
     *         notified
     * @return The listener, which has been created, as an instance of the class {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createRelocateLayoutListener(@NonNull final TabItem tabItem,
                                                                final float position,
                                                                @Nullable final Tag tag,
                                                                final long delay,
                                                                @Nullable final AnimatorListener listener) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                animateRelocate(tabItem, position, tag, delay, listener);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to show a tab as the currently selected
     * one, once it view has been inflated.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which has been added, as an instance of
     *         the class {@link TabItem}. The tab item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createAddSelectedTabLayoutListener(
            @NonNull final TabItem tabItem) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                View view = tabItem.getView();
                FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) view.getLayoutParams();
                view.setAlpha(1f);
                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.NONE));
                arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                        arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.NONE));
                view.setX(layoutParams.leftMargin);
                view.setY(layoutParams.topMargin);
                arithmetics.setScale(Axis.DRAGGING_AXIS, view, 1);
                arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, 1);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to start a reveal animation to add a tab,
     * once its view has been inflated.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which should be added, as an instance of
     *         the class {@link TabItem}. The tab item may not be null
     * @param revealAnimation
     *         The reveal animation, which should be started, as an instance of the class {@link
     *         RevealAnimation}. The reveal animation may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createRevealLayoutListener(@NonNull final TabItem tabItem,
                                                              @NonNull final RevealAnimation revealAnimation) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                View view = tabItem.getView();
                float x = revealAnimation.getX();
                float y = revealAnimation.getY() + tabTitleContainerHeight;
                FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) view.getLayoutParams();
                view.setAlpha(1f);
                arithmetics.setPivot(Axis.X_AXIS, view, x);
                arithmetics.setPivot(Axis.Y_AXIS, view, y);
                view.setX(layoutParams.leftMargin);
                view.setY(layoutParams.topMargin);
                arithmetics.setScale(Axis.DRAGGING_AXIS, view, 0);
                arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, 0);
                animateReveal(tabItem, revealAnimation);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to start a swipe animations to add
     * several tabs, once their views have been inflated.
     *
     * @param addedTabItems
     *         An array, which contains the tab items, which correspond to the tabs, which should be
     *         added, as an array of the type {@link TabItem}. The array may not be null
     * @param swipeAnimation
     *         The swipe animation, which should be started, as an instance of the class {@link
     *         SwipeAnimation}. The swipe animation may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createSwipeLayoutListener(@NonNull final TabItem[] addedTabItems,
                                                             @NonNull final SwipeAnimation swipeAnimation) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                int count = getModel().getCount();
                float previousAttachedPosition = dragHandler.getAttachedPosition(false, count - 1);
                float attachedPosition = dragHandler.getAttachedPosition(true, count);
                float maxTabSpacing = calculateMaxTabSpacing(count);
                dragHandler.setMaxTabSpacing(maxTabSpacing);
                TabItem[] tabItems;

                if (count - addedTabItems.length == 0) {
                    tabItems = calculateInitialTabItems();
                } else {
                    TabItem firstAddedTabItem = addedTabItems[0];
                    int index = firstAddedTabItem.getIndex();
                    boolean isReferencingPredecessor = index > 0;
                    int referenceIndex = isReferencingPredecessor ? index - 1 :
                            (index + addedTabItems.length - 1 < count - 1 ?
                                    index + addedTabItems.length : -1);
                    TabItem referenceTabItem = referenceIndex != -1 ?
                            TabItem.create(getTabSwitcher(), viewRecycler, referenceIndex) : null;
                    State state =
                            referenceTabItem != null ? referenceTabItem.getTag().getState() : null;

                    if (state == null || state == State.STACKED_START) {
                        tabItems = relocateWhenAddingStackedTabs(true, addedTabItems);
                    } else if (state == State.STACKED_END) {
                        tabItems = relocateWhenAddingStackedTabs(false, addedTabItems);
                    } else if (state == State.FLOATING ||
                            (state == State.STACKED_START_ATOP && (index > 0 || count <= 2))) {
                        tabItems = relocateWhenAddingFloatingTabs(addedTabItems, referenceTabItem,
                                isReferencingPredecessor, attachedPosition,
                                attachedPosition != previousAttachedPosition);
                    } else {
                        tabItems = relocateWhenAddingHiddenTabs(addedTabItems, referenceTabItem);
                    }
                }

                for (TabItem tabItem : tabItems) {
                    Tag tag = tabItem.getTag();
                    createBottomMarginLayoutListener(tabItem).onGlobalLayout();
                    View view = tabItem.getView();
                    view.setTag(R.id.tag_properties, tag);
                    view.setAlpha(swipedTabAlpha);
                    float swipePosition = dragHandler.calculateSwipePosition();
                    float scale = arithmetics.getScale(view, true);
                    arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                            arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.NONE));
                    arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                            arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.NONE));
                    arithmetics.setPosition(Axis.DRAGGING_AXIS, view, tag.getPosition());
                    arithmetics.setPosition(Axis.ORTHOGONAL_AXIS, view,
                            swipeAnimation.getDirection() == SwipeDirection.LEFT ?
                                    -1 * swipePosition : swipePosition);
                    arithmetics.setScale(Axis.DRAGGING_AXIS, view, scale);
                    arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, scale);
                    arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                            arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.SWIPE));
                    arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                            arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.SWIPE));
                    arithmetics.setScale(Axis.DRAGGING_AXIS, view, swipedTabScale * scale);
                    arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, swipedTabScale * scale);
                    animateSwipe(tabItem, false, 0, swipeAnimation,
                            createSwipeAnimationListener(tabItem));
                }
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to adapt the bottom margin of a tab, once
     * its view has been inflated.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be adapted, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @return The layout listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The layout listener may not be null
     */
    private OnGlobalLayoutListener createBottomMarginLayoutListener(
            @NonNull final TabItem tabItem) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                View view = tabItem.getView();

                if (tabViewBottomMargin == -1) {
                    tabViewBottomMargin = calculateBottomMargin(view);
                }

                FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) view.getLayoutParams();
                layoutParams.bottomMargin = tabViewBottomMargin;
                view.setLayoutParams(layoutParams);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to adapt the size and position of a tab,
     * once its view has been inflated.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be adapted, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @param layoutListener
     *         The layout lister, which should be notified, when the created listener is invoked, as
     *         an instance of the type {@link OnGlobalLayoutListener} or null, if no listener should
     *         be notified
     * @return The layout listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The layout listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createInflateViewLayoutListener(@NonNull final TabItem tabItem,
                                                                   @Nullable final OnGlobalLayoutListener layoutListener) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                adaptViewSize(tabItem);
                updateView(tabItem);

                if (layoutListener != null) {
                    layoutListener.onGlobalLayout();
                }
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to update the view, which is used to
     * visualize a specific tab, when an animation has been finished.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be updated, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @return The animation listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createUpdateViewAnimationListener(@NonNull final TabItem tabItem) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                inflateOrRemoveView(tabItem);
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to inflate or remove the views, which
     * are used to visualize tabs, when an animation, which is used to hide the tab switcher,
     * has been finished.
     *
     * @return The animation listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createHideSwitcherAnimationListener() {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                AbstractTabItemIterator iterator =
                        new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).create();
                TabItem tabItem;

                while ((tabItem = iterator.next()) != null) {
                    if (tabItem.getTab() == getModel().getSelectedTab()) {
                        Pair<View, Boolean> pair = viewRecycler.inflate(tabItem);
                        View view = pair.first;
                        FrameLayout.LayoutParams layoutParams =
                                (FrameLayout.LayoutParams) view.getLayoutParams();
                        view.setAlpha(1f);
                        arithmetics.setScale(Axis.DRAGGING_AXIS, view, 1);
                        arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, 1);
                        view.setX(layoutParams.leftMargin);
                        view.setY(layoutParams.topMargin);
                    } else {
                        viewRecycler.remove(tabItem);
                    }
                }

                viewRecycler.clearCache();
                recyclerAdapter.clearCachedPreviews();
                tabViewBottomMargin = -1;
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to remove all tabs, when the
     * animation, which is used to swipe all tabs, has been finished.
     *
     * @return The animation listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createClearAnimationListener() {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                viewRecycler.removeAll();
                animateToolbarVisibility(isToolbarShown(), 0);
            }

        };
    }

    /**
     * Creates and returns a listener, which allows to handle, when a tab has been swiped, but was
     * not removed.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which has been swiped, as an instance of
     *         the class {@link TabItem}. The tab item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createSwipeAnimationListener(@NonNull final TabItem tabItem) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                inflateOrRemoveView(tabItem);
                View view = tabItem.getView();
                adaptStackOnSwipeAborted(tabItem, tabItem.getIndex() + 1);
                tabItem.getTag().setClosing(false);
                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.NONE));
                animateToolbarVisibility(true, 0);
            }

        };
    }

    /**
     * Creates and returns a listener, which allows to relocate all previous tabs, when a tab has
     * been removed.
     *
     * @param removedTabItem
     *         The tab item, which corresponds to the tab, which has been removed, as an instance of
     *         the class {@link TabItem}. The tab item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createRemoveAnimationListener(@NonNull final TabItem removedTabItem) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(final Animator animation) {
                super.onAnimationStart(animation);

                if (getModel().isEmpty()) {
                    animateToolbarVisibility(isToolbarShown(), 0);
                }

                float previousAttachedPosition = dragHandler.getAttachedPosition(false, -1);
                float attachedPosition =
                        dragHandler.getAttachedPosition(true, getModel().getCount());
                float maxTabSpacing = calculateMaxTabSpacing(getModel().getCount());
                dragHandler.setMaxTabSpacing(maxTabSpacing);
                State state = removedTabItem.getTag().getState();

                if (state == State.STACKED_END) {
                    relocateWhenRemovingStackedTab(removedTabItem, false);
                } else if (state == State.STACKED_START) {
                    relocateWhenRemovingStackedTab(removedTabItem, true);
                } else if (state == State.FLOATING || state == State.STACKED_START_ATOP) {
                    relocateWhenRemovingFloatingTab(removedTabItem, attachedPosition,
                            previousAttachedPosition != attachedPosition);
                }
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                viewRecycler.remove(removedTabItem);
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to update or remove the view, which
     * is used to visualize a tab, when the animation, which has been used to relocate it, has been
     * ended.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which has been relocated, as an instance
     *         of the class {@link TabItem}. The tab item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createRelocateAnimationListener(@NonNull final TabItem tabItem) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(final Animator animation) {
                super.onAnimationStart(animation);
                tabItem.getView().setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);

                if (tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                    adaptStackOnSwipeAborted(tabItem, tabItem.getIndex() + 1);
                }

                if (tabItem.isVisible()) {
                    updateView(tabItem);
                } else {
                    viewRecycler.remove(tabItem);
                }
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to adapt the pivot of a specific
     * view, when an animation, which reverted an overshoot, has been ended.
     *
     * @param view
     *         The view, whose pivot should be adapted, as an instance of the class {@link View}.
     *         The view may not be null
     * @param listener
     *         The listener, which should be notified about the animation's progress, as an instance
     *         of the type {@link AnimatorListener} or null, if no listener should be notified
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createRevertOvershootAnimationListener(@NonNull final View view,
                                                                    @Nullable final AnimatorListener listener) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.NONE));
                arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                        arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.NONE));

                if (listener != null) {
                    listener.onAnimationEnd(animation);
                }
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to revert an overshoot at the start,
     * when an animation has been ended.
     *
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createRevertStartOvershootAnimationListener() {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                animateRevertStartOvershoot(new DecelerateInterpolator());
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to handle, when a fling animation
     * ended.
     *
     * @return The listener, which has been created, as an instance of the class {@link
     * AnimationListener}. The listener may not be null
     */
    @NonNull
    private AnimationListener createFlingAnimationListener() {
        return new AnimationListener() {

            @Override
            public void onAnimationStart(final android.view.animation.Animation animation) {

            }

            @Override
            public void onAnimationEnd(final android.view.animation.Animation animation) {
                dragHandler
                        .handleRelease(new TabItemIterator.Factory(getTabSwitcher(), viewRecycler),
                                null, dragThreshold);
                flingAnimation = null;
                notifyOnAnimationsEnded();
            }

            @Override
            public void onAnimationRepeat(final android.view.animation.Animation animation) {

            }

        };
    }

    /**
     * Adapts the stack, which is located at the start, when swiping a tab.
     *
     * @param swipedTabItem
     *         The tab item, which corresponds to the swiped tab, as an instance of the class {@link
     *         TabItem}. The tab item may not be null
     * @param successorIndex
     *         The index of the tab, which is located after the swiped tab, as an {@link Integer}
     *         value
     * @param count
     *         The number of tabs, which are contained by the tab switcher, excluding the swiped
     *         tab, as an {@link Integer} value
     */
    private void adaptStackOnSwipe(@NonNull final TabItem swipedTabItem, final int successorIndex,
                                   final int count) {
        if (swipedTabItem.getTag().getState() == State.STACKED_START_ATOP &&
                successorIndex < getModel().getCount()) {
            TabItem tabItem = TabItem.create(getTabSwitcher(), viewRecycler, successorIndex);
            State state = tabItem.getTag().getState();

            if (state == State.HIDDEN || state == State.STACKED_START) {
                Pair<Float, State> pair = dragHandler
                        .calculatePositionAndStateWhenStackedAtStart(count,
                                swipedTabItem.getIndex(), (TabItem) null);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
                inflateOrRemoveView(tabItem);
            }
        }
    }

    /**
     * Adapts the stack, which located at the start, when swiping a tab has been aborted.
     *
     * @param swipedTabItem
     *         The tab item, which corresponds to the swiped tab, as an instance of the class {@link
     *         TabItem}. The tab item may not be null
     * @param successorIndex
     *         The index of the the tab, which is located after the swiped tab, as an {@link
     *         Integer} value
     */
    private void adaptStackOnSwipeAborted(@NonNull final TabItem swipedTabItem,
                                          final int successorIndex) {
        if (swipedTabItem.getTag().getState() == State.STACKED_START_ATOP &&
                successorIndex < getModel().getCount()) {
            TabItem tabItem = TabItem.create(getTabSwitcher(), viewRecycler, successorIndex);

            if (tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                Pair<Float, State> pair = dragHandler
                        .calculatePositionAndStateWhenStackedAtStart(getTabSwitcher().getCount(),
                                tabItem.getIndex(), swipedTabItem);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
                inflateOrRemoveView(tabItem);
            }
        }
    }

    /**
     * Inflates or removes the view, which is used to visualize a specific tab, depending on the
     * tab's current state.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be inflated or removed,
     *         as an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void inflateOrRemoveView(@NonNull final TabItem tabItem) {
        if (tabItem.isInflated() && !tabItem.isVisible()) {
            viewRecycler.remove(tabItem);
        } else if (tabItem.isVisible()) {
            if (!tabItem.isInflated()) {
                inflateAndUpdateView(tabItem, null);
            } else {
                updateView(tabItem);
            }
        }
    }

    /**
     * Inflates and updates the view, which is used to visualize a specific tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be inflated, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @param listener
     *         The layout listener, which should be notified, when the view has been inflated, as an
     *         instance of the type {@link OnGlobalLayoutListener} or null, if no listener should be
     *         notified
     */
    private void inflateAndUpdateView(@NonNull final TabItem tabItem,
                                      @Nullable final OnGlobalLayoutListener listener) {
        inflateView(tabItem, createInflateViewLayoutListener(tabItem, listener),
                tabViewBottomMargin);
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
     *         recycler, which is used to inflate the view, as an array of the type {@link Integer}.
     *         The array may not be null
     */
    private void inflateView(@NonNull final TabItem tabItem,
                             @Nullable final OnGlobalLayoutListener listener,
                             @NonNull final Integer... params) {
        Pair<View, Boolean> pair = viewRecycler.inflate(tabItem, params);

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
     * Inflates the views, which are used to visualize several tabs.
     *
     * @param tabItems
     *         An array, which contains the tab items, which correspond to the tabs, whose views
     *         should be inflated, as an array of the type {@link TabItem}. The array may not be
     *         null
     * @param listener
     *         The layout listener, which should be notified, when all views have been inflated, as
     *         an instance of the type {@link OnGlobalLayoutListener} or null, if no listener should
     *         be notified
     * @param params
     *         An array, which contains optional parameters, which should be passed to the view
     *         recycler, which is used to inflate the views, as an array of the type {@link
     *         Integer}. The array may not be null
     */
    private void inflateViews(@NonNull final TabItem[] tabItems,
                              @Nullable final OnGlobalLayoutListener listener,
                              @NonNull final Integer... params) {
        OnGlobalLayoutListener compoundListener =
                new CompoundLayoutListener(tabItems.length, listener);

        for (TabItem tabItem : tabItems) {
            inflateView(tabItem, compoundListener, params);
        }
    }

    /**
     * Adapts the size of the view, which is used to visualize a specific tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be adapted, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptViewSize(@NonNull final TabItem tabItem) {
        View view = tabItem.getView();
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.NONE));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.NONE));
        float scale = arithmetics.getScale(view, true);
        arithmetics.setScale(Axis.DRAGGING_AXIS, view, scale);
        arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, scale);
    }

    /**
     * Updates the view, which is used to visualize a specific tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be updated, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     */
    private void updateView(@NonNull final TabItem tabItem) {
        float position = tabItem.getTag().getPosition();
        View view = tabItem.getView();
        view.setAlpha(1f);
        view.setVisibility(View.VISIBLE);
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.NONE));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.NONE));
        arithmetics.setPosition(Axis.DRAGGING_AXIS, view, position);
        arithmetics.setPosition(Axis.ORTHOGONAL_AXIS, view, 0);
        arithmetics.setRotation(Axis.ORTHOGONAL_AXIS, view, 0);
    }

    /**
     * Relocates all previous tabs, when a floating tab has been removed from the tab switcher.
     *
     * @param removedTabItem
     *         The tab item, which corresponds to the tab, which has been removed, as an instance of
     *         the class {@link TabItem}. The tab item may not be null
     * @param attachedPositionChanged
     *         True, if removing the tab caused the attached position to be changed, false
     *         otherwise
     */
    private void relocateWhenRemovingFloatingTab(@NonNull final TabItem removedTabItem,
                                                 final float attachedPosition,
                                                 boolean attachedPositionChanged) {
        AbstractTabItemIterator.Factory factory =
                new TabItemIterator.Factory(getTabSwitcher(), viewRecycler);
        AbstractTabItemIterator.AbstractBuilder builder = factory.create();
        AbstractTabItemIterator iterator;
        TabItem tabItem;
        float defaultTabSpacing = dragHandler.calculateMaxTabSpacing(getModel().getCount(), null);
        float minTabSpacing = dragHandler.calculateMinTabSpacing(getModel().getCount());
        int referenceIndex = removedTabItem.getIndex();
        TabItem currentReferenceTabItem = removedTabItem;
        float referencePosition = removedTabItem.getTag().getPosition();

        if (attachedPositionChanged && getModel().getCount() > 0) {
            int neighboringIndex =
                    removedTabItem.getIndex() > 0 ? referenceIndex - 1 : referenceIndex;
            referencePosition += Math.abs(
                    TabItem.create(getTabSwitcher(), viewRecycler, neighboringIndex).getTag()
                            .getPosition() - referencePosition) / 2f;
        }

        referencePosition =
                Math.min(dragHandler.calculateEndPosition(factory, removedTabItem.getIndex() - 1),
                        referencePosition);
        float initialReferencePosition = referencePosition;

        if (removedTabItem.getIndex() > 0) {
            int selectedTabIndex = getModel().getSelectedTabIndex();
            TabItem selectedTabItem =
                    TabItem.create(getTabSwitcher(), viewRecycler, selectedTabIndex);
            float maxTabSpacing =
                    dragHandler.calculateMaxTabSpacing(getModel().getCount(), selectedTabItem);
            iterator = builder.start(removedTabItem.getIndex() - 1).reverse(true).create();

            while ((tabItem = iterator.next()) != null) {
                TabItem predecessor = iterator.peek();
                float currentTabSpacing = dragHandler
                        .calculateMaxTabSpacing(getModel().getCount(), currentReferenceTabItem);
                Pair<Float, State> pair;

                if (tabItem.getIndex() == removedTabItem.getIndex() - 1) {
                    pair = dragHandler.clipTabPosition(getModel().getCount(), tabItem.getIndex(),
                            referencePosition, predecessor);
                    currentReferenceTabItem = tabItem;
                    referencePosition = pair.first;
                    referenceIndex = tabItem.getIndex();
                } else if (referencePosition >= attachedPosition - currentTabSpacing) {
                    float position;

                    if (selectedTabIndex > tabItem.getIndex() &&
                            selectedTabIndex <= referenceIndex) {
                        position = referencePosition + maxTabSpacing +
                                ((referenceIndex - tabItem.getIndex() - 1) * defaultTabSpacing);
                    } else {
                        position = referencePosition +
                                ((referenceIndex - tabItem.getIndex()) * defaultTabSpacing);
                    }

                    pair = dragHandler
                            .clipTabPosition(getModel().getCount(), tabItem.getIndex(), position,
                                    predecessor);
                } else {
                    TabItem successor = iterator.previous();
                    float successorPosition = successor.getTag().getPosition();
                    float position = (attachedPosition * (successorPosition + minTabSpacing)) /
                            (minTabSpacing + attachedPosition - currentTabSpacing);
                    pair = dragHandler
                            .clipTabPosition(getModel().getCount(), tabItem.getIndex(), position,
                                    predecessor);

                    if (pair.first >= attachedPosition - currentTabSpacing) {
                        currentReferenceTabItem = tabItem;
                        referencePosition = pair.first;
                        referenceIndex = tabItem.getIndex();
                    }
                }

                Tag tag = tabItem.getTag().clone();
                tag.setPosition(pair.first);
                tag.setState(pair.second);

                if (tag.getState() != State.HIDDEN) {
                    long startDelay = Math.abs(removedTabItem.getIndex() - tabItem.getIndex()) *
                            relocateAnimationDelay;

                    if (!tabItem.isInflated()) {
                        Pair<Float, State> pair2 = dragHandler
                                .calculatePositionAndStateWhenStackedAtEnd(tabItem.getIndex());
                        tabItem.getTag().setPosition(pair2.first);
                        tabItem.getTag().setState(pair2.second);
                    }

                    relocate(tabItem, tag.getPosition(), tag, startDelay);
                } else {
                    break;
                }
            }
        }

        if (attachedPositionChanged && getModel().getCount() > 2 &&
                removedTabItem.getTag().getState() != State.STACKED_START_ATOP) {
            iterator = builder.start(removedTabItem.getIndex()).reverse(false).create();
            float previousPosition = initialReferencePosition;
            Tag previousTag = removedTabItem.getTag();

            while ((tabItem = iterator.next()) != null &&
                    tabItem.getIndex() < getModel().getCount() - 1) {
                float position = dragHandler.calculateNonLinearPosition(previousPosition,
                        dragHandler.calculateMaxTabSpacing(getModel().getCount(), tabItem));
                Pair<Float, State> pair = dragHandler
                        .clipTabPosition(getModel().getCount(), tabItem.getIndex(), position,
                                previousTag.getState());
                Tag tag = tabItem.getTag().clone();
                tag.setPosition(pair.first);
                tag.setState(pair.second);
                long startDelay = (Math.abs(removedTabItem.getIndex() - tabItem.getIndex()) + 1) *
                        relocateAnimationDelay;

                if (!tabItem.isInflated()) {
                    Pair<Float, State> pair2 = dragHandler
                            .calculatePositionAndStateWhenStackedAtStart(getModel().getCount(),
                                    tabItem.getIndex(), iterator.previous());
                    tabItem.getTag().setPosition(pair2.first);
                    tabItem.getTag().setState(pair2.second);
                }

                relocate(tabItem, tag.getPosition(), tag, startDelay);
                previousPosition = pair.first;
                previousTag = tag;

                if (pair.second == State.HIDDEN || pair.second == State.STACKED_START) {
                    break;
                }
            }
        }
    }

    /**
     * Relocates all neighboring tabs, when a stacked tab has been removed from the tab switcher.
     *
     * @param removedTabItem
     *         The tab item, which corresponds to the tab, which has been removed, as an instance of
     *         the class {@link TabItem}. The tab item may not be null
     * @param start
     *         True, if the removed tab was part of the stack, which is located at the start, false,
     *         if it was part of the stack, which is located at the end
     */
    private void relocateWhenRemovingStackedTab(@NonNull final TabItem removedTabItem,
                                                final boolean start) {
        int startIndex = removedTabItem.getIndex() + (start ? -1 : 0);
        TabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).reverse(start)
                        .start(startIndex).create();
        TabItem tabItem;
        float previousProjectedPosition = removedTabItem.getTag().getPosition();

        while ((tabItem = iterator.next()) != null &&
                (tabItem.getTag().getState() == State.HIDDEN ||
                        tabItem.getTag().getState() == State.STACKED_START ||
                        tabItem.getTag().getState() == State.STACKED_START_ATOP ||
                        tabItem.getTag().getState() == State.STACKED_END)) {
            float projectedPosition = tabItem.getTag().getPosition();

            if (tabItem.getTag().getState() == State.HIDDEN) {
                TabItem previous = iterator.previous();
                tabItem.getTag().setState(previous.getTag().getState());

                if (tabItem.isVisible()) {
                    Pair<Float, State> pair = start ? dragHandler
                            .calculatePositionAndStateWhenStackedAtStart(
                                    getTabSwitcher().getCount(), tabItem.getIndex(), tabItem) :
                            dragHandler
                                    .calculatePositionAndStateWhenStackedAtEnd(tabItem.getIndex());
                    tabItem.getTag().setPosition(pair.first);
                    tabItem.getTag().setState(pair.second);
                    inflateAndUpdateView(tabItem, null);
                }

                break;
            } else {
                tabItem.getTag().setPosition(previousProjectedPosition);
                long startDelay =
                        (Math.abs(startIndex - tabItem.getIndex()) + 1) * relocateAnimationDelay;
                animateRelocate(tabItem, previousProjectedPosition, null, startDelay,
                        createRelocateAnimationListener(tabItem));
            }

            previousProjectedPosition = projectedPosition;
        }
    }

    /**
     * Relocates all previous tabs, when floating tabs have been added to the tab switcher.
     *
     * @param addedTabItems
     *         An array, which contains the tab items, which correspond to the tabs, which have been
     *         added, as an array of the type {@link TabItem}. The array may not be null
     * @param referenceTabItem
     *         The tab item, which corresponds to the tab, which is used as a reference, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @param isReferencingPredecessor
     *         True, if the tab, which is used as a reference, is the predecessor of the added tab,
     *         false if it is the successor
     * @param attachedPosition
     *         The current attached position in pixels as a {@link Float} value
     * @param attachedPositionChanged
     *         True, if adding the tab caused the attached position to be changed, false otherwise
     * @return An array, which contains the tab items, which correspond to the tabs, which have been
     * added, as an array of the type {@link TabItem}. The array may not be null
     */
    @NonNull
    private TabItem[] relocateWhenAddingFloatingTabs(@NonNull final TabItem[] addedTabItems,
                                                     @NonNull final TabItem referenceTabItem,
                                                     final boolean isReferencingPredecessor,
                                                     final float attachedPosition,
                                                     final boolean attachedPositionChanged) {
        int count = getTabSwitcher().getCount();
        TabItem firstAddedTabItem = addedTabItems[0];
        TabItem lastAddedTabItem = addedTabItems[addedTabItems.length - 1];

        float referencePosition = referenceTabItem.getTag().getPosition();

        if (isReferencingPredecessor && attachedPositionChanged &&
                lastAddedTabItem.getIndex() < count - 1) {
            int neighboringIndex = lastAddedTabItem.getIndex() + 1;
            referencePosition -= Math.abs(referencePosition -
                    TabItem.create(getTabSwitcher(), viewRecycler, neighboringIndex).getTag()
                            .getPosition()) / 2f;
        }

        float initialReferencePosition = referencePosition;
        int selectedTabIndex = getModel().getSelectedTabIndex();
        TabItem selectedTabItem = TabItem.create(getTabSwitcher(), viewRecycler, selectedTabIndex);
        float defaultTabSpacing = dragHandler.calculateMaxTabSpacing(count, null);
        float maxTabSpacing = dragHandler.calculateMaxTabSpacing(count, selectedTabItem);
        float minTabSpacing = dragHandler.calculateMinTabSpacing(count);
        TabItem currentReferenceTabItem = referenceTabItem;
        int referenceIndex = referenceTabItem.getIndex();

        AbstractTabItemIterator.AbstractBuilder builder =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler);

        for (TabItem addedTabItem : addedTabItems) {
            int iterationReferenceIndex = referenceIndex;
            float iterationReferencePosition = referencePosition;
            TabItem iterationReferenceTabItem = currentReferenceTabItem;
            AbstractTabItemIterator iterator =
                    builder.start(addedTabItem.getIndex()).reverse(true).create();
            TabItem tabItem;

            while ((tabItem = iterator.next()) != null) {
                TabItem predecessor = iterator.peek();
                Pair<Float, State> pair;
                float currentTabSpacing =
                        dragHandler.calculateMaxTabSpacing(count, iterationReferenceTabItem);

                if (isReferencingPredecessor && tabItem.getIndex() == addedTabItem.getIndex()) {
                    State predecessorState =
                            predecessor != null ? predecessor.getTag().getState() : null;
                    pair = dragHandler
                            .clipTabPosition(count, tabItem.getIndex(), iterationReferencePosition,
                                    predecessorState == State.STACKED_START_ATOP ? State.FLOATING :
                                            predecessorState);
                    currentReferenceTabItem = iterationReferenceTabItem = tabItem;
                    initialReferencePosition =
                            referencePosition = iterationReferencePosition = pair.first;
                    referenceIndex = iterationReferenceIndex = tabItem.getIndex();
                } else if (iterationReferencePosition >= attachedPosition - currentTabSpacing) {
                    float position;

                    if (selectedTabIndex > tabItem.getIndex() &&
                            selectedTabIndex <= iterationReferenceIndex) {
                        position = iterationReferencePosition + maxTabSpacing +
                                ((iterationReferenceIndex - tabItem.getIndex() - 1) *
                                        defaultTabSpacing);
                    } else {
                        position = iterationReferencePosition +
                                ((iterationReferenceIndex - tabItem.getIndex()) *
                                        defaultTabSpacing);
                    }

                    pair = dragHandler
                            .clipTabPosition(count, tabItem.getIndex(), position, predecessor);
                } else {
                    TabItem successor = iterator.previous();
                    float successorPosition = successor.getTag().getPosition();
                    float position = (attachedPosition * (successorPosition + minTabSpacing)) /
                            (minTabSpacing + attachedPosition - currentTabSpacing);
                    pair = dragHandler
                            .clipTabPosition(count, tabItem.getIndex(), position, predecessor);

                    if (pair.first >= attachedPosition - currentTabSpacing) {
                        iterationReferenceTabItem = tabItem;
                        iterationReferencePosition = pair.first;
                        iterationReferenceIndex = tabItem.getIndex();
                    }
                }

                if (tabItem.getIndex() >= firstAddedTabItem.getIndex() &&
                        tabItem.getIndex() <= lastAddedTabItem.getIndex()) {
                    if (!isReferencingPredecessor && attachedPositionChanged && count > 3) {
                        TabItem successor = iterator.previous();
                        float successorPosition = successor.getTag().getPosition();
                        float position = pair.first - Math.abs(pair.first - successorPosition) / 2f;
                        pair = dragHandler
                                .clipTabPosition(count, tabItem.getIndex(), position, predecessor);
                        initialReferencePosition = pair.first;
                    }

                    Tag tag = addedTabItems[tabItem.getIndex() - firstAddedTabItem.getIndex()]
                            .getTag();
                    tag.setPosition(pair.first);
                    tag.setState(pair.second);
                } else {
                    Tag tag = tabItem.getTag().clone();
                    tag.setPosition(pair.first);
                    tag.setState(pair.second);

                    if (!tabItem.isInflated()) {
                        Pair<Float, State> pair2 = dragHandler
                                .calculatePositionAndStateWhenStackedAtEnd(tabItem.getIndex());
                        tabItem.getTag().setPosition(pair2.first);
                        tabItem.getTag().setState(pair2.second);
                    }

                    relocate(tabItem, tag.getPosition(), tag, 0);
                }

                if (pair.second == State.HIDDEN || pair.second == State.STACKED_END) {
                    dragHandler.setFirstVisibleIndex(dragHandler.getFirstVisibleIndex() + 1);
                    break;
                }
            }
        }

        if (attachedPositionChanged && count > 3) {
            AbstractTabItemIterator iterator =
                    builder.start(lastAddedTabItem.getIndex() + 1).reverse(false).create();
            TabItem tabItem;
            float previousPosition = initialReferencePosition;
            Tag previousTag = lastAddedTabItem.getTag();

            while ((tabItem = iterator.next()) != null && tabItem.getIndex() < count - 1) {
                float position = dragHandler.calculateNonLinearPosition(previousPosition,
                        dragHandler.calculateMaxTabSpacing(count, tabItem));
                Pair<Float, State> pair = dragHandler
                        .clipTabPosition(count, tabItem.getIndex(), position,
                                previousTag.getState());
                Tag tag = tabItem.getTag().clone();
                tag.setPosition(pair.first);
                tag.setState(pair.second);

                if (!tabItem.isInflated()) {
                    Pair<Float, State> pair2 = dragHandler
                            .calculatePositionAndStateWhenStackedAtStart(count, tabItem.getIndex(),
                                    iterator.previous());
                    tabItem.getTag().setPosition(pair2.first);
                    tabItem.getTag().setState(pair2.second);
                }

                relocate(tabItem, tag.getPosition(), tag, 0);
                previousPosition = pair.first;
                previousTag = tag;

                if (pair.second == State.HIDDEN || pair.second == State.STACKED_START) {
                    break;
                }
            }
        }

        return addedTabItems;
    }

    /**
     * Relocates all neighboring tabs, when stacked tabs have been added to the tab switcher.
     *
     * @param start
     *         True, if the added tab was part of the stack, which is located at the start, false,
     *         if it was part of the stack, which is located at the end
     * @param addedTabItems
     *         An array, which contains the tab items, which correspond to the tabs, which have been
     *         added, as an array of the type {@link TabItem}. The array may not be null
     * @return An array, which contains the tab items, which correspond to the tabs, which have been
     * added, as an array of the type {@link TabItem}. The array may not be null
     */
    @NonNull
    private TabItem[] relocateWhenAddingStackedTabs(final boolean start,
                                                    @NonNull final TabItem[] addedTabItems) {
        if (!start) {
            dragHandler.setFirstVisibleIndex(
                    dragHandler.getFirstVisibleIndex() + addedTabItems.length);
        }

        int count = getTabSwitcher().getCount();
        TabItem firstAddedTabItem = addedTabItems[0];
        TabItem lastAddedTabItem = addedTabItems[addedTabItems.length - 1];
        AbstractTabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler)
                        .start(start ? lastAddedTabItem.getIndex() : firstAddedTabItem.getIndex())
                        .reverse(start).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null &&
                (tabItem.getTag().getState() == State.STACKED_START ||
                        tabItem.getTag().getState() == State.STACKED_START_ATOP ||
                        tabItem.getTag().getState() == State.STACKED_END ||
                        tabItem.getTag().getState() == State.HIDDEN)) {
            TabItem predecessor = start ? iterator.peek() : iterator.previous();
            Pair<Float, State> pair = start ? dragHandler
                    .calculatePositionAndStateWhenStackedAtStart(count, tabItem.getIndex(),
                            predecessor) :
                    dragHandler.calculatePositionAndStateWhenStackedAtEnd(tabItem.getIndex());

            if (start && predecessor != null && predecessor.getTag().getState() == State.FLOATING) {
                float predecessorPosition = predecessor.getTag().getPosition();
                float distance = predecessorPosition - pair.first;

                if (distance > dragHandler.calculateMinTabSpacing(count)) {
                    float position = dragHandler.calculateNonLinearPosition(tabItem, predecessor);
                    pair = dragHandler
                            .clipTabPosition(count, tabItem.getIndex(), position, predecessor);
                }
            }

            if (tabItem.getIndex() >= firstAddedTabItem.getIndex() &&
                    tabItem.getIndex() <= lastAddedTabItem.getIndex()) {
                Tag tag = addedTabItems[tabItem.getIndex() - firstAddedTabItem.getIndex()].getTag();
                tag.setPosition(pair.first);
                tag.setState(pair.second);
            } else if (tabItem.isInflated()) {
                Tag tag = tabItem.getTag().clone();
                tag.setPosition(pair.first);
                tag.setState(pair.second);
                animateRelocate(tabItem, tag.getPosition(), tag, 0,
                        createRelocateAnimationListener(tabItem));
            } else {
                break;
            }
        }

        return addedTabItems;
    }

    /**
     * Calculates the position and state of hidden tabs, which have been added to the tab switcher.
     *
     * @param addedTabItems
     *         An array, which contains the tab items, which correspond to the tabs, which have been
     *         added, as an array of the type {@link TabItem}. The array may not be null
     * @param referenceTabItem
     *         The tab item, which corresponds to the tab, which is used as a reference, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @return An array, which contains the tab items, which correspond to the tabs, which have been
     * added, as an array of the type {@link TabItem}. The array may not be null
     */
    @NonNull
    private TabItem[] relocateWhenAddingHiddenTabs(@NonNull final TabItem[] addedTabItems,
                                                   @NonNull final TabItem referenceTabItem) {
        boolean stackedAtStart = isStackedAtStart(referenceTabItem.getIndex());

        for (TabItem tabItem : addedTabItems) {
            Pair<Float, State> pair;

            if (stackedAtStart) {
                TabItem predecessor = tabItem.getIndex() > 0 ?
                        TabItem.create(getTabSwitcher(), viewRecycler, tabItem.getIndex() - 1) :
                        null;
                pair = dragHandler
                        .calculatePositionAndStateWhenStackedAtStart(getModel().getCount(),
                                tabItem.getIndex(), predecessor);
            } else {
                pair = dragHandler.calculatePositionAndStateWhenStackedAtEnd(tabItem.getIndex());
            }

            Tag tag = tabItem.getTag();
            tag.setPosition(pair.first);
            tag.setState(pair.second);
        }

        return addedTabItems;
    }

    /**
     * Relocates a specific tab. If its view is now yet inflated, it is inflated first.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which should be relocated, as an instance
     *         of the class {@link TabItem}. The tab item may not be null
     * @param relocatePosition
     *         The position, the tab should be moved to, in pixels as an {@link Float} value
     * @param tag
     *         The tag, which should be applied to the tab, once it has been relocated, as an
     *         instance of the class {@link Tag} or null, if no tag should be applied
     * @param startDelay
     *         The start delay of the relocate animation in milliseconds as a {@link Long} value
     */
    private void relocate(@NonNull final TabItem tabItem, final float relocatePosition,
                          @Nullable final Tag tag, final long startDelay) {
        if (tabItem.isInflated()) {
            animateRelocate(tabItem, relocatePosition, tag, startDelay,
                    createRelocateAnimationListener(tabItem));
        } else {
            inflateAndUpdateView(tabItem,
                    createRelocateLayoutListener(tabItem, relocatePosition, tag, startDelay,
                            createRelocateAnimationListener(tabItem)));
            tabItem.getView().setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Swipes a specific tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which should be swiped, as an instance of
     *         the class {@link TabItem}. The tab item may not be null
     * @param distance
     *         The distance, the tab should be swiped by, in pixels as a {@link Float} value
     */
    private void swipe(@NonNull final TabItem tabItem, final float distance) {
        View view = tabItem.getView();

        if (!tabItem.getTag().isClosing()) {
            adaptStackOnSwipe(tabItem, tabItem.getIndex() + 1, getModel().getCount() - 1);
        }

        tabItem.getTag().setClosing(true);
        float dragDistance = distance;

        if (!tabItem.getTab().isCloseable()) {
            dragDistance = (float) Math.pow(Math.abs(distance), 0.75);
            dragDistance = distance < 0 ? dragDistance * -1 : dragDistance;
        }

        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.SWIPE));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.SWIPE));
        float scale = arithmetics.getScale(view, true);
        float ratio = 1 - (Math.abs(dragDistance) / dragHandler.calculateSwipePosition());
        float scaledClosedTabScale = swipedTabScale * scale;
        float targetScale = scaledClosedTabScale + ratio * (scale - scaledClosedTabScale);
        arithmetics.setScale(Axis.DRAGGING_AXIS, view, targetScale);
        arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, targetScale);
        view.setAlpha(swipedTabAlpha + ratio * (1 - swipedTabAlpha));
        arithmetics.setPosition(Axis.ORTHOGONAL_AXIS, view, dragDistance);
    }

    /**
     * Moves the first tab to overlap the other tabs, when overshooting at the start.
     *
     * @param position
     *         The position of the first tab in pixels as a {@link Float} value
     */
    private void startOvershoot(final float position) {
        TabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.getIndex() == 0) {
                View view = tabItem.getView();
                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.NONE));
                arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                        arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.NONE));
                arithmetics.setPosition(Axis.DRAGGING_AXIS, view, position);
            } else if (tabItem.isInflated()) {
                View firstView = iterator.first().getView();
                View view = tabItem.getView();
                view.setVisibility(arithmetics.getPosition(Axis.DRAGGING_AXIS, firstView) <=
                        arithmetics.getPosition(Axis.DRAGGING_AXIS, view) ? View.INVISIBLE :
                        View.VISIBLE);
            }
        }
    }

    /**
     * Tilts the tabs, when overshooting at the start.
     *
     * @param angle
     *         The angle, the tabs should be rotated by, in degrees as a {@link Float} value
     */
    private void tiltOnStartOvershoot(final float angle) {
        TabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            View view = tabItem.getView();

            if (tabItem.getIndex() == 0) {
                view.setCameraDistance(maxCameraDistance);
                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.OVERSHOOT_START));
                arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view, arithmetics
                        .getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.OVERSHOOT_START));
                arithmetics.setRotation(Axis.ORTHOGONAL_AXIS, view, angle);
            } else if (tabItem.isInflated()) {
                tabItem.getView().setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * Tilts the tabs, when overshooting at the end.
     *
     * @param angle
     *         The angle, the tabs should be rotated by, in degrees as a {@link Float} value
     */
    private void tiltOnEndOvershoot(final float angle) {
        float minCameraDistance = maxCameraDistance / 2f;
        int firstVisibleIndex = -1;
        TabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.isInflated()) {
                View view = tabItem.getView();

                if (!iterator.hasNext()) {
                    view.setCameraDistance(maxCameraDistance);
                } else if (firstVisibleIndex == -1) {
                    view.setCameraDistance(minCameraDistance);

                    if (tabItem.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = tabItem.getIndex();
                    }
                } else {
                    int diff = tabItem.getIndex() - firstVisibleIndex;
                    float ratio =
                            (float) diff / (float) (getModel().getCount() - firstVisibleIndex);
                    view.setCameraDistance(
                            minCameraDistance + (maxCameraDistance - minCameraDistance) * ratio);
                }

                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getPivot(Axis.DRAGGING_AXIS, view, DragState.OVERSHOOT_END));
                arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                        arithmetics.getPivot(Axis.ORTHOGONAL_AXIS, view, DragState.OVERSHOOT_END));
                arithmetics.setRotation(Axis.ORTHOGONAL_AXIS, view, angle);
            }
        }
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
    private boolean isStackedAtStart(final int index) {
        boolean start = true;
        AbstractTabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).start(index + 1)
                        .create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            State state = tabItem.getTag().getState();

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
     * Obtains the view's background from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the background should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    private void obtainBackground(@NonNull final TypedArray typedArray) {
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_android_background, 0);

        if (resourceId != 0) {
            ViewUtil.setBackground(getTabSwitcher(),
                    ContextCompat.getDrawable(getContext(), resourceId));
        } else {
            int defaultValue =
                    ContextCompat.getColor(getContext(), R.color.tab_switcher_background_color);
            int color =
                    typedArray.getColor(R.styleable.TabSwitcher_android_background, defaultValue);
            getTabSwitcher().setBackgroundColor(color);
        }
    }

    /**
     * Obtains the icon of a tab from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the icon should be obtained from, as an instance of the class {@link
     *         TypedArray}. The typed array may not be null
     */
    private void obtainTabIcon(@NonNull final TypedArray typedArray) {
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_tabIcon, -1);

        if (resourceId != -1) {
            setTabIcon(resourceId);
        } else {
            setTabIcon(null);
        }
    }

    /**
     * Obtains the background color of a tab from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the background color should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabBackgroundColor(@NonNull final TypedArray typedArray) {
        int defaultValue = ContextCompat.getColor(getContext(), R.color.tab_background_color);
        setTabBackgroundColor(
                typedArray.getColor(R.styleable.TabSwitcher_tabBackgroundColor, defaultValue));
    }

    /**
     * Obtains the text color of a tab's title from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the text color should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabTitleTextColor(@NonNull final TypedArray typedArray) {
        int defaultValue = ContextCompat.getColor(getContext(), R.color.tab_title_text_color);
        setTabTitleTextColor(
                typedArray.getColor(R.styleable.TabSwitcher_tabTitleTextColor, defaultValue));
    }

    /**
     * Obtains the icon of a tab's close button from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the icon should be obtained from, as an instance of the class {@link
     *         TypedArray}. The typed array may not be null
     */
    private void obtainTabCloseButtonIcon(@NonNull final TypedArray typedArray) {
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_tabCloseButtonIcon, -1);

        if (resourceId != -1) {
            setTabCloseButtonIcon(resourceId);
        } else {
            setTabCloseButtonIcon(R.drawable.ic_close_tab_18dp);
        }
    }

    /**
     * Creates a new layout, which implements the functionality of a {@link TabSwitcher} on
     * smartphones.
     *
     * @param tabSwitcher
     *         The tab switcher, the layout belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param model
     *         The model of the tab switcher, the layout belongs to, as an instance of the type
     *         {@link Model}. The model may not be null
     */
    public PhoneTabSwitcherLayout(@NonNull final TabSwitcher tabSwitcher,
                                  @NonNull final Model model) {
        super(tabSwitcher, model);
        arithmetics = new PhoneArithmetics(tabSwitcher);
        Resources resources = tabSwitcher.getResources();
        dragThreshold =
                getTabSwitcher().getResources().getDimensionPixelSize(R.dimen.drag_threshold);
        tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        tabBorderWidth = resources.getDimensionPixelSize(R.dimen.tab_border_width);
        tabTitleContainerHeight =
                resources.getDimensionPixelSize(R.dimen.tab_title_container_height);
        stackedTabCount = resources.getInteger(R.integer.stacked_tab_count);
        stackedTabSpacing = resources.getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        maxCameraDistance = resources.getDimensionPixelSize(R.dimen.max_camera_distance);
        TypedValue typedValue = new TypedValue();
        resources.getValue(R.dimen.swiped_tab_scale, typedValue, true);
        swipedTabScale = typedValue.getFloat();
        resources.getValue(R.dimen.swiped_tab_alpha, typedValue, true);
        swipedTabAlpha = typedValue.getFloat();
        showSwitcherAnimationDuration =
                resources.getInteger(R.integer.show_switcher_animation_duration);
        hideSwitcherAnimationDuration =
                resources.getInteger(R.integer.hide_switcher_animation_duration);
        toolbarVisibilityAnimationDuration =
                resources.getInteger(R.integer.toolbar_visibility_animation_duration);
        toolbarVisibilityAnimationDelay =
                resources.getInteger(R.integer.toolbar_visibility_animation_delay);
        swipeAnimationDuration = resources.getInteger(R.integer.swipe_animation_duration);
        clearAnimationDelay = resources.getInteger(R.integer.clear_animation_delay);
        relocateAnimationDuration = resources.getInteger(R.integer.relocate_animation_duration);
        relocateAnimationDelay = resources.getInteger(R.integer.relocate_animation_delay);
        revertOvershootAnimationDuration =
                resources.getInteger(R.integer.revert_overshoot_animation_duration);
        revealAnimationDuration = resources.getInteger(R.integer.reveal_animation_duration);
        tabViewBottomMargin = -1;
        toolbarAnimation = null;
        flingAnimation = null;
    }

    @Override
    protected final void onDecoratorChanged(@NonNull final TabSwitcherDecorator decorator) {
        childViewRecycler.setAdapter(new ChildRecyclerAdapter(getTabSwitcher(), decorator));
        recyclerAdapter.clearCachedPreviews();
    }

    @Override
    protected final void onPaddingChanged(final int left, final int top, final int right,
                                          final int bottom) {
        FrameLayout.LayoutParams toolbarLayoutParams =
                (FrameLayout.LayoutParams) toolbar.getLayoutParams();
        toolbarLayoutParams.setMargins(left, top, right, 0);
        TabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            TabViewHolder viewHolder = tabItem.getViewHolder();

            if (viewHolder != null) {
                recyclerAdapter.adaptPadding(viewHolder);
            }
        }
    }

    @Override
    protected final void onTabIconChanged(@Nullable final Drawable icon) {
        for (Tab tab : getModel()) {
            recyclerAdapter.onIconChanged(tab);
        }
    }

    @Override
    protected final void onTabBackgroundColorChanged(@ColorInt final int color) {
        for (Tab tab : getModel()) {
            recyclerAdapter.onBackgroundColorChanged(tab);
        }
    }

    @Override
    protected final void onTabTitleColorChanged(@ColorInt final int color) {
        for (Tab tab : getModel()) {
            recyclerAdapter.onTitleTextColorChanged(tab);
        }
    }

    @Override
    protected final void onTabCloseButtonIconChanged(@NonNull final Drawable icon) {
        for (Tab tab : getModel()) {
            recyclerAdapter.onCloseButtonIconChanged(tab);
        }
    }

    @Override
    public final void obtainStyledAttributes(@NonNull final TypedArray typedArray) {
        super.obtainStyledAttributes(typedArray);
        obtainBackground(typedArray);
        obtainTabIcon(typedArray);
        obtainTabBackgroundColor(typedArray);
        obtainTabTitleTextColor(typedArray);
        obtainTabCloseButtonIcon(typedArray);
    }

    @Override
    public final void inflateLayout() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        toolbar =
                (Toolbar) inflater.inflate(R.layout.tab_switcher_toolbar, getTabSwitcher(), false);
        getTabSwitcher().addView(toolbar);
        tabContainer = new FrameLayout(getContext());
        getTabSwitcher().addView(tabContainer, FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        childViewRecycler = new ViewRecycler<>(inflater);
        recyclerAdapter = new RecyclerAdapter(getTabSwitcher(), childViewRecycler);
        viewRecycler = new AttachedViewRecycler<>(tabContainer, inflater,
                Collections.reverseOrder(new TabItem.Comparator(getTabSwitcher())));
        viewRecycler.setAdapter(recyclerAdapter);
        recyclerAdapter.setViewRecycler(viewRecycler);
        dragHandler = new DragHandler(getTabSwitcher(), arithmetics);
    }

    @Override
    public final boolean handleTouchEvent(@NonNull final MotionEvent event) {
        return dragHandler
                .handleTouchEvent(new TabItemIterator.Factory(getTabSwitcher(), viewRecycler),
                        event);
    }

    @Override
    public final void addCloseTabListener(@NonNull final TabCloseListener listener) {
        recyclerAdapter.addCloseTabListener(listener);
    }

    @Override
    public final void removeCloseTabListener(@NonNull final TabCloseListener listener) {
        recyclerAdapter.removeCloseTabListener(listener);
    }

    @NonNull
    @Override
    public final Layout getLayout() {
        return getOrientation(getContext()) == Orientation.LANDSCAPE ? Layout.PHONE_LANDSCAPE :
                Layout.PHONE_PORTRAIT;
    }

    @Override
    public final boolean isAnimationRunning() {
        return super.isAnimationRunning() || flingAnimation != null;
    }

    @NonNull
    @Override
    public final ViewGroup getTabContainer() {
        return tabContainer;
    }

    @NonNull
    @Override
    public final Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    public final void onSwitcherShown() {
        animateShowSwitcher();
    }

    @Override
    public final void onSwitcherHidden() {
        animateHideSwitcher();
    }

    @Override
    public final void onSelectionChanged(final int previousIndex, final int index,
                                         @Nullable final Tab selectedTab,
                                         final boolean switcherHidden) {
        if (switcherHidden) {
            animateHideSwitcher();
        } else {
            viewRecycler.remove(TabItem.create(getTabSwitcher(), viewRecycler, previousIndex));
            viewRecycler.inflate(TabItem.create(getTabSwitcher(), viewRecycler, index));
        }
    }

    @Override
    public final void onTabAdded(final int index, @NonNull final Tab tab,
                                 final int previousSelectedTabIndex, final int selectedTabIndex,
                                 final boolean switcherHidden, @NonNull final Animation animation) {
        if (switcherHidden) {
            TabItem tabItem = new TabItem(0, tab);
            RevealAnimation revealAnimation =
                    animation instanceof RevealAnimation ? (RevealAnimation) animation :
                            new RevealAnimation.Builder().create();
            inflateView(tabItem, createRevealLayoutListener(tabItem, revealAnimation));
        } else {
            addAllTabs(index, new Tab[]{tab}, animation);
        }
    }

    @Override
    public final void onAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                     final int previousSelectedTabIndex, final int selectedTabIndex,
                                     @NonNull final Animation animation) {
        ensureTrue(animation instanceof SwipeAnimation,
                animation.getClass().getSimpleName() + " not supported when using layout " +
                        getLayout());
        addAllTabs(index, tabs, animation);
    }

    @Override
    public final void onTabRemoved(final int index, @NonNull final Tab tab,
                                   final int previousSelectedTabIndex, final int selectedTabIndex,
                                   @NonNull final Animation animation) {
        ensureTrue(animation instanceof SwipeAnimation,
                animation.getClass().getSimpleName() + " not supported when using layout " +
                        getLayout());
        TabItem removedTabItem = TabItem.create(viewRecycler, index, tab);

        if (!getModel().isSwitcherShown()) {
            viewRecycler.remove(removedTabItem);

            if (getModel().isEmpty()) {
                toolbar.setAlpha(isToolbarShown() ? 1 : 0);
            } else if (selectedTabIndex != previousSelectedTabIndex) {
                viewRecycler
                        .inflate(TabItem.create(getTabSwitcher(), viewRecycler, selectedTabIndex));
            }
        } else {
            adaptStackOnSwipe(removedTabItem, removedTabItem.getIndex(), getModel().getCount());
            removedTabItem.getTag().setClosing(true);
            SwipeAnimation swipeAnimation =
                    animation instanceof SwipeAnimation ? (SwipeAnimation) animation :
                            new SwipeAnimation.Builder().create();

            if (removedTabItem.isInflated()) {
                animateRemove(removedTabItem, swipeAnimation);
            } else {
                boolean start = isStackedAtStart(index);
                TabItem predecessor = TabItem.create(getTabSwitcher(), viewRecycler, index - 1);
                Pair<Float, State> pair = start ? dragHandler
                        .calculatePositionAndStateWhenStackedAtStart(getModel().getCount(), index,
                                predecessor) :
                        dragHandler.calculatePositionAndStateWhenStackedAtEnd(index);
                removedTabItem.getTag().setPosition(pair.first);
                removedTabItem.getTag().setState(pair.second);
                inflateAndUpdateView(removedTabItem,
                        createRemoveLayoutListener(removedTabItem, swipeAnimation));
            }
        }
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final Tab[] tabs,
                                       @NonNull final Animation animation) {
        ensureTrue(animation instanceof SwipeAnimation,
                animation.getClass().getSimpleName() + " not supported when using layout " +
                        getLayout());

        if (!getModel().isSwitcherShown()) {
            viewRecycler.removeAll();
            toolbar.setAlpha(isToolbarShown() ? 1 : 0);
        } else {
            SwipeAnimation swipeAnimation =
                    animation instanceof SwipeAnimation ? (SwipeAnimation) animation :
                            new SwipeAnimation.Builder().create();
            AbstractTabItemIterator iterator =
                    new ArrayTabItemIterator.Builder(viewRecycler, tabs).reverse(true).create();
            TabItem tabItem;
            int startDelay = 0;

            while ((tabItem = iterator.next()) != null) {
                TabItem previous = iterator.previous();

                if (tabItem.getTag().getState() == State.FLOATING ||
                        (previous != null && previous.getTag().getState() == State.FLOATING)) {
                    startDelay += clearAnimationDelay;
                }

                if (tabItem.isInflated()) {
                    animateSwipe(tabItem, true, startDelay, swipeAnimation,
                            !iterator.hasNext() ? createClearAnimationListener() : null);
                }
            }
        }
    }

    @Override
    public final void onGlobalLayout() {
        if (getModel().isSwitcherShown()) {
            TabItem[] tabItems = calculateInitialTabItems();
            AbstractTabItemIterator iterator =
                    new InitialTabItemIterator.Builder(getTabSwitcher(), viewRecycler, dragHandler,
                            tabItems).create();
            TabItem tabItem;

            while ((tabItem = iterator.next()) != null) {
                if (tabItem.isVisible()) {
                    inflateAndUpdateView(tabItem, createBottomMarginLayoutListener(tabItem));
                }
            }

            toolbar.setAlpha(isToolbarShown() ? 1 : 0);
        } else if (getModel().getSelectedTab() != null) {
            TabItem tabItem = TabItem.create(getTabSwitcher(), viewRecycler,
                    getModel().getSelectedTabIndex());
            viewRecycler.inflate(tabItem);
        }
    }

    @Override
    public final void onClick(@NonNull final TabItem tabItem) {
        getModel().selectTab(tabItem.getTab());
    }

    @Override
    public final void onCancelFling() {
        if (flingAnimation != null) {
            flingAnimation.cancel();
            flingAnimation = null;
            dragHandler.handleRelease(new TabItemIterator.Factory(getTabSwitcher(), viewRecycler),
                    null, dragThreshold);
        }
    }

    @Override
    public final void onFling(final float distance, final long duration) {
        animateFling(distance, duration);
    }

    @Override
    public final void onRevertStartOvershoot(final float maxAngle) {
        animateRevertStartOvershoot(maxAngle);
    }

    @Override
    public final void onRevertEndOvershoot(final float maxAngle) {
        animateRevertEndOvershoot(maxAngle);
    }

    public final void onStartOvershoot(final float position) {
        startOvershoot(position);
    }

    @Override
    public final void onTiltOnStartOvershoot(final float angle) {
        tiltOnStartOvershoot(angle);
    }

    @Override
    public final void onTiltOnEndOvershoot(final float angle) {
        tiltOnEndOvershoot(angle);
    }

    @Override
    public final void onSwipe(@NonNull final TabItem tabItem, final float distance) {
        swipe(tabItem, distance);
    }

    @Override
    public final void onSwipeEnded(@NonNull final TabItem tabItem, final boolean remove,
                                   final long animationDuration) {
        if (remove) {
            View view = tabItem.getView();
            SwipeDirection direction =
                    arithmetics.getPosition(Axis.ORTHOGONAL_AXIS, view) < 0 ? SwipeDirection.LEFT :
                            SwipeDirection.RIGHT;
            Animation animation = new SwipeAnimation.Builder().setDirection(direction)
                    .setDuration(animationDuration).create();
            getModel().removeTab(tabItem.getTab(), animation);
        } else {
            animateSwipe(tabItem, false, 0, new SwipeAnimation.Builder().create(),
                    createSwipeAnimationListener(tabItem));
        }
    }

    @Override
    public final void onViewStateChanged(@NonNull final TabItem tabItem) {
        inflateOrRemoveView(tabItem);
    }

}