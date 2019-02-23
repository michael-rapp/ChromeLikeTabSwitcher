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
package de.mrapp.android.tabswitcher.layout.phone;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.PeekAnimation;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.RevealAnimation;
import de.mrapp.android.tabswitcher.SwipeAnimation;
import de.mrapp.android.tabswitcher.SwipeAnimation.SwipeDirection;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.gesture.TouchEventDispatcher;
import de.mrapp.android.tabswitcher.iterator.AbstractItemIterator;
import de.mrapp.android.tabswitcher.iterator.ArrayItemIterator;
import de.mrapp.android.tabswitcher.iterator.ItemIterator;
import de.mrapp.android.tabswitcher.layout.AbstractDragTabsEventHandler;
import de.mrapp.android.tabswitcher.layout.AbstractDragTabsEventHandler.DragState;
import de.mrapp.android.tabswitcher.layout.AbstractTabRecyclerAdapter;
import de.mrapp.android.tabswitcher.layout.AbstractTabSwitcherLayout;
import de.mrapp.android.tabswitcher.layout.Arithmetics;
import de.mrapp.android.tabswitcher.layout.Arithmetics.Axis;
import de.mrapp.android.tabswitcher.model.AbstractItem;
import de.mrapp.android.tabswitcher.model.ItemComparator;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.tabswitcher.model.TabSwitcherStyle;
import de.mrapp.android.tabswitcher.model.Tag;
import de.mrapp.android.util.view.AbstractViewRecycler;
import de.mrapp.android.util.view.AttachedViewRecycler;
import de.mrapp.android.util.view.ViewRecycler;
import de.mrapp.util.Condition;

/**
 * A layout, which implements the functionality of a {@link TabSwitcher} on smartphones.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class PhoneTabSwitcherLayout extends AbstractTabSwitcherLayout
        implements PhoneDragTabsEventHandler.Callback {

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
            Condition.INSTANCE.ensureGreater(count, 0, "The count must be greater than 0");
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
     * The ratio, which specifies the maximum space between the currently selected tab and its
     * predecessor in relation to the default space.
     */
    private static final float SELECTED_TAB_SPACING_RATIO = 1.5f;

    /**
     * The ratio, which specifies the minimum space between two neighboring tabs in relation to the
     * maximum space.
     */
    private static final float MIN_TAB_SPACING_RATIO = 0.375f;

    /**
     * The number of tabs, which are contained by a stack.
     */
    private final int stackedTabCount;

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
     * The duration of the animation, which is used to relocate tabs.
     */
    private final long relocateAnimationDuration;

    /**
     * The duration of the animation, which is used to revert overshoots.
     */
    private final long revertOvershootAnimationDuration;

    /**
     * The duration of a reveal animation.
     */
    private final long revealAnimationDuration;

    /**
     * The duration of a peek animation.
     */
    private final long peekAnimationDuration;

    /**
     * The duration of the fade animation, which is used to show or hide the empty view.
     */
    private final long emptyViewAnimationDuration;

    /**
     * The maximum angle, tabs can be rotated by, when overshooting at the start, in degrees.
     */
    private final float maxStartOvershootAngle;

    /**
     * The maximum angle, tabs can be rotated by, when overshooting at the end, in degrees.
     */
    private final float maxEndOvershootAngle;

    /**
     * The distance between two neighboring tabs when being swiped in pixels.
     */
    private final int swipedTabDistance;

    /**
     * The drag handler, which is used by the layout.
     */
    private PhoneDragTabsEventHandler dragHandler;

    /**
     * The view recycler, which allows to recycle the views, which are associated with tabs.
     */
    private ViewRecycler<Tab, Void> contentViewRecycler;

    /**
     * The adapter, which allows to inflate the views, which are used to visualize tabs.
     */
    private PhoneTabRecyclerAdapter tabRecyclerAdapter;

    /**
     * The view recycler, which allows to recycle the views, which are used to visualize tabs.
     */
    private AttachedViewRecycler<AbstractItem, Integer> tabViewRecycler;

    /**
     * The view group, which contains the tab switcher's tabs.
     */
    private ViewGroup tabContainer;

    /**
     * The toolbar, which is shown, when the tab switcher is shown.
     */
    private Toolbar toolbar;

    /**
     * The view, which is shown, when the tab switcher is empty.
     */
    private View emptyView;

    /**
     * The bottom margin of a view, which visualizes a tab.
     */
    private int tabViewBottomMargin;

    /**
     * The animation, which is used to show or hide the toolbar.
     */
    private ViewPropertyAnimator toolbarAnimation;

    /**
     * Adapts the decorator.
     */
    private void adaptDecorator() {
        tabRecyclerAdapter.clearCachedPreviews();
    }

    /**
     * Adapts the visibility of the view, which is shown, when the tab switcher is empty.
     *
     * @param animationDuration
     *         The duration of the fade animation, which should be used to show or hide the view, in
     *         milliseconds as a {@link Long} value
     */
    private void adaptEmptyView(final long animationDuration) {
        detachEmptyView();

        if (getModel().isEmpty()) {
            emptyView = getModel().getEmptyView();

            if (emptyView != null) {
                emptyView.setAlpha(0);
                FrameLayout.LayoutParams layoutParams =
                        new FrameLayout.LayoutParams(emptyView.getLayoutParams().width,
                                emptyView.getLayoutParams().height);
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
                getTabSwitcher().addView(emptyView, 0, layoutParams);
                ViewPropertyAnimator animation = emptyView.animate();
                animation.setDuration(
                        animationDuration == -1 ? emptyViewAnimationDuration : animationDuration);
                animation.alpha(1);
                animation.start();
            }
        }
    }

    /**
     * Detaches the view, which is shown, when the tab switcher is empty.
     */
    private void detachEmptyView() {
        if (emptyView != null) {
            getTabSwitcher().removeView(emptyView);
            emptyView = null;
        }
    }

    /**
     * Adapts the margin of the toolbar, which is shown, when the tab switcher is shown.
     */
    private void adaptToolbarMargin() {
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) toolbar.getLayoutParams();
        layoutParams.setMargins(getModel().getPaddingLeft(), getModel().getPaddingTop(),
                getModel().getPaddingRight(), 0);
    }

    /**
     * Calculates and returns the delays of subsequently started animations, depending on the
     * animations' durations.
     *
     * @param animationDuration
     *         The duration of the animations in milliseconds as a {@link Long} value
     * @return The delay, which has been calculated, as a {@link Long} value
     */
    private long calculateAnimationDelay(final long animationDuration) {
        return Math.round(animationDuration * 0.4);
    }

    /**
     * Calculates the position of a tab in relation to the position of its predecessor.
     *
     * @param predecessorPosition
     *         The position of the predecessor in pixels as a {@link Float} value
     * @param maxTabSpacing
     *         The maximum space between two neighboring tabs in pixels as a {@link Float} value
     * @return The position, which has been calculated, as a {@link Float} value
     */
    private float calculateSuccessorPosition(final float predecessorPosition,
                                             final float maxTabSpacing) {
        float ratio = Math.min(1,
                predecessorPosition / calculateAttachedPosition(getTabSwitcher().getCount()));
        float minTabSpacing = calculateMinTabSpacing();
        return predecessorPosition - minTabSpacing - (ratio * (maxTabSpacing - minTabSpacing));
    }

    /**
     * Calculates and returns the position of a tab, when it is swiped.
     *
     * @return The position, which has been calculated, in pixels as an {@link Float} value
     */
    private float calculateSwipePosition() {
        return getArithmetics().getTabContainerSize(Axis.ORTHOGONAL_AXIS, true);
    }

    /**
     * Calculates and returns the maximum space between a specific tab and its predecessor. The
     * maximum space is greater for the currently selected tab.
     *
     * @param item
     *         The item, which corresponds to the tab, the maximum space should be returned for, as
     *         an instance of the class {@link AbstractItem} or null, if the default maximum space
     *         should be returned
     * @return The maximum space between the given tab and its predecessor in pixels as a {@link
     * Float} value
     */
    private float calculateMaxTabSpacing(@Nullable final AbstractItem item) {
        float totalSpace = getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false);
        float maxTabSpacing;
        int count = getModel().getCount();

        if (count <= 2) {
            maxTabSpacing = totalSpace * 0.66f;
        } else if (count == 3) {
            maxTabSpacing = totalSpace * 0.33f;
        } else if (count == 4) {
            maxTabSpacing = totalSpace * 0.3f;
        } else {
            maxTabSpacing = totalSpace * 0.25f;
        }

        return count > 4 && item != null &&
                ((TabItem) item).getTab() == getTabSwitcher().getSelectedTab() ?
                maxTabSpacing * SELECTED_TAB_SPACING_RATIO : maxTabSpacing;
    }

    /**
     * Calculates and returns the minimum space between two neighboring tabs.
     *
     * @return The minimum space between two neighboring tabs in pixels as a {@link Float} value
     */
    private float calculateMinTabSpacing() {
        return calculateMaxTabSpacing(null) * MIN_TAB_SPACING_RATIO;
    }

    /**
     * Calculates and returns the bottom margin of a specific tab.
     *
     * @param item
     *         The item, which corresponds to the tab, whose bottom margin should be calculated, as
     *         an instance of the class {@link AbstractItem}. The item may not be null
     * @return The bottom margin, which has been calculated, in pixels as an {@link Integer} value
     */
    private int calculateBottomMargin(@NonNull final AbstractItem item) {
        View view = item.getView();
        float tabHeight = (view.getHeight() - 2 * tabInset) * getArithmetics().getScale(item, true);
        float containerHeight = getArithmetics().getTabContainerSize(Axis.Y_AXIS, false);
        int stackHeight = getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ? 0 :
                getStackedTabCount() * getStackedTabSpacing();
        return Math.round(tabHeight + tabInset + stackHeight - containerHeight);
    }

    /**
     * Animates the bottom margin of a specific view.
     *
     * @param view
     *         The view, whose bottom margin should be animated, as an instance of the class {@link
     *         View}. The view may not be null
     * @param margin
     *         The bottom margin, which should be set by the animation, as an {@link Integer} value
     * @param duration
     *         The duration of the animation in milliseconds as a {@link Long} value
     * @param delay
     *         The delay of the animation in milliseconds as a {@link Long} value
     */
    private void animateBottomMargin(@NonNull final View view, final int margin,
                                     final long duration, final long delay) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        final int initialMargin = layoutParams.bottomMargin;
        ValueAnimator animation = ValueAnimator.ofInt(margin - initialMargin);
        animation.setDuration(duration);
        animation.addListener(new AnimationListenerWrapper(null));
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setStartDelay(delay);
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

        toolbarAnimation = toolbar.animate();
        toolbarAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        toolbarAnimation.setDuration(toolbarVisibilityAnimationDuration);
        toolbarAnimation.setListener(createToolbarAnimationListener(visible));
        toolbarAnimation.setStartDelay(delay);
        toolbarAnimation.alpha(visible ? 1 : 0);
        toolbarAnimation.start();
    }

    /**
     * Shows the tab switcher in an animated manner.
     *
     * @param referenceTabIndex
     *         The index of the tab, which is used as a reference, when restoring the positions of
     *         tabs, as an {@link Integer} value or -1, if the positions of tabs should not be
     *         restored
     * @param referenceTabPosition
     *         The position of tab, which is used as a reference, when restoring the positions of
     *         tabs, in relation to the available space as a {@link Float} value or -1, if the
     *         positions of tabs should not be restored
     */
    private void animateShowSwitcher(final int referenceTabIndex,
                                     final float referenceTabPosition) {
        AbstractItem[] items = calculateInitialItems(referenceTabIndex, referenceTabPosition);
        AbstractItemIterator iterator = new InitialItemIteratorBuilder(items).create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            if (((TabItem) item).getTab() == getModel().getSelectedTab() || item.isVisible()) {
                tabViewRecycler.inflate(item);
                View view = item.getView();

                if (!ViewCompat.isLaidOut(view)) {
                    view.getViewTreeObserver().addOnGlobalLayoutListener(
                            new LayoutListenerWrapper(view,
                                    createShowSwitcherLayoutListener(item)));
                } else {
                    animateShowSwitcher(item, createUpdateViewAnimationListener(item));
                }
            }
        }

        animateToolbarVisibility(getModel().areToolbarsShown(), toolbarVisibilityAnimationDelay);
    }

    /**
     * Calculates and returns the items, which correspond to the tabs, when the tab switcher is
     * shown initially.
     *
     * @param referenceTabIndex
     *         The index of the tab, which is used as a reference, when restoring the positions of
     *         tabs, as an {@link Integer} value or -1, if the positions of tabs should not be
     *         restored
     * @param referenceTabPosition
     *         The position of tab, which is used as a reference, when restoring the positions of
     *         tabs, in relation to the available space as a {@link Float} value or -1, if the
     *         positions of tabs should not be restored
     * @return An array, which contains the items, as an array of the type {@link AbstractItem}. The
     * array may not be null
     */
    @NonNull
    private AbstractItem[] calculateInitialItems(final int referenceTabIndex,
                                                 final float referenceTabPosition) {
        dragHandler.reset();
        setFirstVisibleIndex(-1);
        AbstractItem[] items = new AbstractItem[getModel().getCount()];

        if (!getModel().isEmpty()) {
            int selectedTabIndex = getModel().getSelectedTabIndex();
            float attachedPosition = calculateAttachedPosition(getModel().getCount());
            int referenceIndex =
                    referenceTabIndex != -1 && referenceTabPosition != -1 ? referenceTabIndex :
                            selectedTabIndex;
            float referencePosition = referenceTabIndex != -1 && referenceTabPosition != -1 ?
                    referenceTabPosition *
                            getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false) :
                    attachedPosition;
            referencePosition =
                    Math.min(calculateMaxEndPosition(referenceIndex), referencePosition);
            AbstractItemIterator iterator =
                    new InitialItemIteratorBuilder(items).start(referenceIndex).create();
            AbstractItem item;

            while ((item = iterator.next()) != null) {
                AbstractItem predecessor = iterator.previous();
                float position;

                if (item.getIndex() == getModel().getCount() - 1) {
                    position = 0;
                } else if (item.getIndex() == referenceIndex) {
                    position = referencePosition;
                } else {
                    position = calculateSuccessorPosition(item, predecessor);
                }

                State predecessorState =
                        item.getIndex() == referenceIndex && referenceIndex > 0 ? State.FLOATING :
                                (predecessor != null ? predecessor.getTag().getState() : null);
                Pair<Float, State> pair = clipPosition(item.getIndex(), position, predecessorState);
                item.getTag().setPosition(pair.first);
                item.getTag().setState(pair.second);

                if (getFirstVisibleIndex() == -1 && pair.second != State.STACKED_END &&
                        pair.second != State.HIDDEN) {
                    setFirstVisibleIndex(item.getIndex());
                }

                if (pair.second == State.STACKED_START || pair.second == State.STACKED_START_ATOP) {
                    break;
                }
            }

            boolean overshooting = referenceIndex == getModel().getCount() - 1 ||
                    isOvershootingAtEnd(DragState.NONE, iterator);
            iterator = new InitialItemIteratorBuilder(items).reverse(true).start(referenceIndex - 1)
                    .create();
            float minTabSpacing = calculateMinTabSpacing();
            float defaultTabSpacing = calculateMaxTabSpacing(null);
            AbstractItem selectedItem =
                    TabItem.create(getModel(), tabViewRecycler, selectedTabIndex);
            float maxTabSpacing = calculateMaxTabSpacing(selectedItem);
            AbstractItem currentReferenceItem = iterator.getItem(referenceIndex);

            while ((item = iterator.next()) != null &&
                    (overshooting || item.getIndex() < referenceIndex)) {
                float currentTabSpacing = calculateMaxTabSpacing(currentReferenceItem);
                AbstractItem predecessor = iterator.peek();
                Pair<Float, State> pair;

                if (overshooting) {
                    float position;

                    if (referenceIndex > item.getIndex()) {
                        position = maxTabSpacing +
                                ((getModel().getCount() - 1 - item.getIndex() - 1) *
                                        defaultTabSpacing);
                    } else {
                        position =
                                (getModel().getCount() - 1 - item.getIndex()) * defaultTabSpacing;
                    }

                    pair = clipPosition(item.getIndex(), position, predecessor);
                } else if (referencePosition >= attachedPosition - currentTabSpacing) {
                    float position;

                    if (selectedTabIndex > item.getIndex() && selectedTabIndex <= referenceIndex) {
                        position = referencePosition + maxTabSpacing +
                                ((referenceIndex - item.getIndex() - 1) * defaultTabSpacing);
                    } else {
                        position = referencePosition +
                                ((referenceIndex - item.getIndex()) * defaultTabSpacing);
                    }

                    pair = clipPosition(item.getIndex(), position, predecessor);
                } else {
                    AbstractItem successor = iterator.previous();
                    float successorPosition = successor.getTag().getPosition();
                    float position = (attachedPosition * (successorPosition + minTabSpacing)) /
                            (minTabSpacing + attachedPosition - currentTabSpacing);
                    pair = clipPosition(item.getIndex(), position, predecessor);

                    if (pair.first >= attachedPosition - currentTabSpacing) {
                        currentReferenceItem = item;
                        referencePosition = pair.first;
                        referenceIndex = item.getIndex();
                    }
                }

                item.getTag().setPosition(pair.first);
                item.getTag().setState(pair.second);

                if ((getFirstVisibleIndex() == -1 || getFirstVisibleIndex() > item.getIndex()) &&
                        pair.second == State.FLOATING) {
                    setFirstVisibleIndex(item.getIndex());
                }
            }
        }

        dragHandler.setCallback(this);
        return items;
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
                AbstractItem[] items = new AbstractItem[tabs.length];
                OnGlobalLayoutListener compoundListener = new CompoundLayoutListener(tabs.length,
                        createSwipeLayoutListener(items, swipeAnimation));

                for (int i = 0; i < tabs.length; i++) {
                    Tab tab = tabs[i];
                    AbstractItem item = TabItem.create(getModel(), index + i, tab);
                    items[i] = item;
                    inflateView(item, compoundListener);
                }
            } else if (!getModel().isSwitcherShown()) {
                toolbar.setAlpha(0);

                if (getModel().getSelectedTab() == tabs[0]) {
                    AbstractItem item = TabItem.create(getTabSwitcher(), tabViewRecycler, index);
                    inflateView(item, createAddSelectedTabLayoutListener(item));
                }
            }
        }
    }

    /**
     * Animates the position and size of a specific tab in order to show the tab switcher.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be animated, as an instance of
     *         the class {@link AbstractItem}. The item may not be null
     * @param listener
     *         The listener, which should be notified about the animation's progress, as an instance
     *         of the type {@link AnimatorListener} or null, if no listener should be notified
     */
    private void animateShowSwitcher(@NonNull final AbstractItem item,
                                     @Nullable final AnimatorListener listener) {
        animateShowSwitcher(item, showSwitcherAnimationDuration,
                new AccelerateDecelerateInterpolator(), listener);
    }

    /**
     * Animates the position and size of a specific tab in order to show the tab switcher.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be animated, as an instance of
     *         the class {@link AbstractItem}. The item may not be null
     * @param duration
     *         The duration of the animation in milliseconds as a {@link Long} value
     * @param interpolator
     *         The interpolator, which should be used by the animation, as an instance of the type
     *         {@link Interpolator}. The interpolator may not be null
     * @param listener
     *         The listener, which should be notified about the animation's progress, as an instance
     *         of the type {@link AnimatorListener} or null, if no listener should be notified
     */
    private void animateShowSwitcher(@NonNull final AbstractItem item, final long duration,
                                     @NonNull final Interpolator interpolator,
                                     @Nullable final AnimatorListener listener) {
        View view = item.getView();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        view.setX(layoutParams.leftMargin);
        view.setY(layoutParams.topMargin);
        getArithmetics().setScale(Axis.DRAGGING_AXIS, item, 1);
        getArithmetics().setScale(Axis.ORTHOGONAL_AXIS, item, 1);
        getArithmetics().setPivot(Axis.DRAGGING_AXIS, item,
                getArithmetics().getPivot(Axis.DRAGGING_AXIS, item, DragState.NONE));
        getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item,
                getArithmetics().getPivot(Axis.ORTHOGONAL_AXIS, item, DragState.NONE));
        float scale = getArithmetics().getScale(item, true);
        int selectedTabIndex = getModel().getSelectedTabIndex();

        if (item.getIndex() < selectedTabIndex) {
            getArithmetics().setPosition(Axis.DRAGGING_AXIS, item,
                    getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS));
        } else if (item.getIndex() > selectedTabIndex) {
            getArithmetics().setPosition(Axis.DRAGGING_AXIS, item,
                    getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ? 0 :
                            layoutParams.topMargin);
        }

        if (tabViewBottomMargin == -1) {
            tabViewBottomMargin = calculateBottomMargin(item);
        }

        animateBottomMargin(view, tabViewBottomMargin, duration, 0);
        ViewPropertyAnimator animation = view.animate();
        animation.setDuration(duration);
        animation.setInterpolator(interpolator);
        animation.setListener(new AnimationListenerWrapper(listener));
        getArithmetics().animateScale(Axis.DRAGGING_AXIS, animation, scale);
        getArithmetics().animateScale(Axis.ORTHOGONAL_AXIS, animation, scale);
        getArithmetics()
                .animatePosition(Axis.DRAGGING_AXIS, animation, item, item.getTag().getPosition(),
                        true);
        getArithmetics().animatePosition(Axis.ORTHOGONAL_AXIS, animation, item, 0, true);
        animation.setStartDelay(0);
        animation.start();
    }

    /**
     * Hides the tab switcher in an animated manner.
     */
    private void animateHideSwitcher() {
        dragHandler.setCallback(null);
        ItemIterator iterator =
                new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler).create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            if (item.isInflated()) {
                animateHideSwitcher(item, item.getIndex() == getModel().getSelectedTabIndex() ?
                        createHideSwitcherAnimationListener() : null);
            } else if (((TabItem) item).getTab() == getModel().getSelectedTab()) {
                inflateAndUpdateView(item, false, createHideSwitcherLayoutListener(item));
            }
        }

        animateToolbarVisibility(getModel().areToolbarsShown() && getModel().isEmpty(), 0);
    }

    /**
     * Animates the position and size of a specific tab in order to hide the tab switcher.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be animated, as an instance of
     *         the class {@link AbstractItem}. The item may not be null
     * @param listener
     *         The listener, which should be notified about the animation's progress, as an instance
     *         of the type {@link AnimatorListener} or null, if no listener should be notified
     */
    private void animateHideSwitcher(@NonNull final AbstractItem item,
                                     @Nullable final AnimatorListener listener) {
        animateHideSwitcher(item, hideSwitcherAnimationDuration,
                new AccelerateDecelerateInterpolator(), 0, listener);
    }

    /**
     * Animates the position and size of a specific tab in order to hide the tab switcher.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be animated, as an instance of
     *         the class {@link AbstractItem}. The item may not be null
     * @param duration
     *         The duration of the animation in milliseconds as a {@link Long} value
     * @param interpolator
     *         The interpolator, which should be used by the animation, as an instance of the class
     *         {@link Interpolator}. The interpolator may not be null
     * @param delay
     *         The delay of the animation in milliseconds as a {@link Long} value
     * @param listener
     *         The listener, which should be notified about the animation's progress, as an instance
     *         of the type {@link AnimatorListener} or null, if no listener should be notified
     */
    private void animateHideSwitcher(@NonNull final AbstractItem item, final long duration,
                                     @NonNull final Interpolator interpolator, final long delay,
                                     @Nullable final AnimatorListener listener) {
        View view = item.getView();
        animateBottomMargin(view, -(tabInset + tabBorderWidth), duration, delay);
        ViewPropertyAnimator animation = view.animate();
        animation.setDuration(duration);
        animation.setInterpolator(interpolator);
        animation.setListener(new AnimationListenerWrapper(listener));
        getArithmetics().animateScale(Axis.DRAGGING_AXIS, animation, 1);
        getArithmetics().animateScale(Axis.ORTHOGONAL_AXIS, animation, 1);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        getArithmetics().animatePosition(Axis.ORTHOGONAL_AXIS, animation, item,
                getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ? layoutParams.topMargin :
                        0);
        int selectedTabIndex = getModel().getSelectedTabIndex();

        if (item.getIndex() < selectedTabIndex) {
            getArithmetics().animatePosition(Axis.DRAGGING_AXIS, animation, item,
                    getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS));
        } else if (item.getIndex() > selectedTabIndex) {
            getArithmetics().animatePosition(Axis.DRAGGING_AXIS, animation, item,
                    getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ? 0 :
                            layoutParams.topMargin);
        } else {
            getArithmetics().animatePosition(Axis.DRAGGING_AXIS, animation, item,
                    getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ? 0 :
                            layoutParams.topMargin);
        }

        animation.setStartDelay(delay);
        animation.start();
    }

    /**
     * Animates the position, size and alpha of a specific tab in order to swipe it orthogonally.
     *
     * @param item
     *         The item, corresponds to the tab, which should be animated, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param remove
     *         True, if the tab should be removed after the animation has finished, false otherwise
     * @param delayMultiplier
     *         The multiplied, which should be used to calculate the delay after which the animation
     *         should be started, by being multiplied with the default delay, as an {@link Integer}
     *         value
     * @param swipeAnimation
     *         The animation, which should be used, as an instance of the class {@link
     *         SwipeAnimation}. The animation may not be null
     * @param listener
     *         The listener, which should be notified about the progress of the animation, as an
     *         instance of the type {@link AnimatorListener} or null, if no listener should be
     *         notified
     */
    private void animateSwipe(@NonNull final AbstractItem item, final boolean remove,
                              final int delayMultiplier,
                              @NonNull final SwipeAnimation swipeAnimation,
                              @Nullable final AnimatorListener listener) {
        View view = item.getView();
        float currentScale = getArithmetics().getScale(item, true);
        float swipePosition = calculateSwipePosition();
        float targetPosition = remove ?
                (swipeAnimation.getDirection() == SwipeDirection.LEFT_OR_TOP ? -1 * swipePosition :
                        swipePosition) : 0;
        float currentPosition = getArithmetics().getPosition(Axis.ORTHOGONAL_AXIS, item);
        float distance = Math.abs(targetPosition - currentPosition);
        long animationDuration = swipeAnimation.getDuration() != -1 ? swipeAnimation.getDuration() :
                Math.round(swipeAnimationDuration * (distance / swipePosition));
        ViewPropertyAnimator animation = view.animate();
        animation.setInterpolator(
                swipeAnimation.getInterpolator() != null ? swipeAnimation.getInterpolator() :
                        new AccelerateDecelerateInterpolator());
        animation.setListener(new AnimationListenerWrapper(listener));
        animation.setDuration(animationDuration);
        getArithmetics()
                .animatePosition(Axis.ORTHOGONAL_AXIS, animation, item, targetPosition, true);
        getArithmetics().animateScale(Axis.ORTHOGONAL_AXIS, animation,
                remove ? swipedTabScale * currentScale : currentScale);
        getArithmetics().animateScale(Axis.DRAGGING_AXIS, animation,
                remove ? swipedTabScale * currentScale : currentScale);
        animation.alpha(remove ? swipedTabAlpha : 1);
        animation.setStartDelay(delayMultiplier * calculateAnimationDelay(animationDuration));
        animation.start();
    }

    /**
     * Animates a tab to be swiped horizontally.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which should be swiped, as an instance of
     *         the class {@link TabItem}. The tab item may not be null
     * @param targetPosition
     *         The position on the x-axis, the tab should be moved to, in pixels as a {@link Float}
     *         value
     * @param selected
     *         True, if the tab should become the selected one, false otherwise
     * @param animationDuration
     *         The duration of the animation in milliseconds as a {@link Long} value
     * @param velocity
     *         The velocity of the drag gesture, which caused the tab to be swiped, in pixels per
     *         second as a {@link Float} value
     */
    private void animateSwipe(@NonNull final TabItem tabItem, final float targetPosition,
                              final boolean selected, final long animationDuration,
                              final float velocity) {
        View view = tabItem.getView();
        float currentPosition = getArithmetics().getPosition(Axis.X_AXIS, tabItem);
        float distance = Math.abs(targetPosition - currentPosition);
        float maxDistance = getArithmetics().getSize(Axis.X_AXIS, tabItem) + swipedTabDistance;
        long duration = velocity > 0 ? Math.round((distance / velocity) * 1000) :
                Math.round(animationDuration * (distance / maxDistance));
        ViewPropertyAnimator animation = view.animate();
        animation.setListener(new AnimationListenerWrapper(
                selected ? createSwipeSelectedTabAnimationListener(tabItem) :
                        createSwipeNeighborAnimationListener(tabItem)));
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setDuration(duration);
        animation.setStartDelay(0);
        getArithmetics().animatePosition(Axis.X_AXIS, animation, tabItem, targetPosition, true);
        animation.start();
    }

    /**
     * Animates the removal of a specific tab.
     *
     * @param removedItem
     *         The item, corresponds to the tab, which should be animated, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param swipeAnimation
     *         The animation, which should be used, as an instance of the class {@link
     *         SwipeAnimation}. The animation may not be null
     */
    private void animateRemove(@NonNull final AbstractItem removedItem,
                               @NonNull final SwipeAnimation swipeAnimation) {
        getArithmetics().setPivot(Axis.DRAGGING_AXIS, removedItem,
                getArithmetics().getPivot(Axis.DRAGGING_AXIS, removedItem, DragState.SWIPE));
        getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, removedItem,
                getArithmetics().getPivot(Axis.ORTHOGONAL_AXIS, removedItem, DragState.SWIPE));
        animateSwipe(removedItem, true, 0, swipeAnimation,
                createRemoveAnimationListener(removedItem, swipeAnimation));
    }

    /**
     * Animates the position of a specific tab in order to relocate it.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be animated, as an instance of
     *         the class {@link AbstractItem}. The item may not be null
     * @param position
     *         The position, the tab should be relocated to, in pixels as a {@link Float} value
     * @param tag
     *         The tag, which should be applied to the given item, as an instance of the class
     *         {@link Tag} or null, if no tag should be applied
     * @param delayMultiplier
     *         The multiplier, which should be used to calculate the delay of the relocate
     *         animation, by being multiplied with the default delay, as an {@link Integer} value
     * @param listener
     *         The listener, which should be notified about the progress of the relocate animation,
     *         as an instance of the type {@link AnimatorListener} or null, if no listener should be
     *         notified
     * @param swipeAnimation
     *         The animation, which has been used to add or remove the tab, which caused the other
     *         tabs to be relocated, as an instance of the class {@link SwipeAnimation}. The
     *         animation may not be null
     */
    private void animateRelocate(@NonNull final AbstractItem item, final float position,
                                 @Nullable final Tag tag, final int delayMultiplier,
                                 @Nullable final AnimatorListener listener,
                                 @NonNull final SwipeAnimation swipeAnimation) {
        if (tag != null) {
            item.getView().setTag(R.id.tag_properties, tag);
            item.setTag(tag);
        }

        View view = item.getView();
        long animationDuration = swipeAnimation.getRelocateAnimationDuration() != -1 ?
                swipeAnimation.getRelocateAnimationDuration() : relocateAnimationDuration;
        ViewPropertyAnimator animation = view.animate();
        animation.setListener(new AnimationListenerWrapper(listener));
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setDuration(animationDuration);
        getArithmetics().animatePosition(Axis.DRAGGING_AXIS, animation, item, position, true);
        animation.setStartDelay(delayMultiplier * calculateAnimationDelay(animationDuration));
        animation.start();
    }

    /**
     * Animates reverting an overshoot at the start.
     */
    private void animateRevertStartOvershoot() {
        boolean tilted = animateTilt(new AccelerateInterpolator(), maxStartOvershootAngle,
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
        AbstractItem item = TabItem.create(getTabSwitcher(), tabViewRecycler, 0);
        getArithmetics().setPivot(Axis.DRAGGING_AXIS, item,
                getArithmetics().getPivot(Axis.DRAGGING_AXIS, item, DragState.NONE));
        getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item,
                getArithmetics().getPivot(Axis.ORTHOGONAL_AXIS, item, DragState.NONE));
        float position = getArithmetics().getPosition(Axis.DRAGGING_AXIS, item);
        float targetPosition = item.getTag().getPosition();
        final float startPosition = getArithmetics().getPosition(Axis.DRAGGING_AXIS, item);
        ValueAnimator animation = ValueAnimator.ofFloat(targetPosition - position);
        animation.setDuration(Math.round(revertOvershootAnimationDuration * Math.abs(
                (targetPosition - position) /
                        (float) (getStackedTabCount() * getStackedTabSpacing()))));
        animation.addListener(new AnimationListenerWrapper(null));
        animation.setInterpolator(interpolator);
        animation.setStartDelay(0);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                ItemIterator iterator =
                        new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler).create();
                AbstractItem item;

                while ((item = iterator.next()) != null) {
                    if (item.getIndex() == 0) {
                        getArithmetics().setPosition(Axis.DRAGGING_AXIS, item,
                                startPosition + (float) animation.getAnimatedValue());
                    } else if (item.isInflated()) {
                        AbstractItem firstItem = iterator.first();
                        View view = item.getView();
                        view.setVisibility(
                                getArithmetics().getPosition(Axis.DRAGGING_AXIS, firstItem) <=
                                        getArithmetics().getPosition(Axis.DRAGGING_AXIS, item) ?
                                        View.INVISIBLE : View.VISIBLE);
                    }
                }
            }

        });

        animation.start();
    }

    /**
     * Animates reverting an overshoot at the end.
     */
    private void animateRevertEndOvershoot() {
        animateTilt(new AccelerateDecelerateInterpolator(), maxEndOvershootAngle, null);
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
        ItemIterator iterator =
                new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler).reverse(true).create();
        AbstractItem item;
        boolean result = false;

        while ((item = iterator.next()) != null) {
            if (item.isInflated() &&
                    getArithmetics().getRotation(Axis.ORTHOGONAL_AXIS, item) != 0) {
                View view = item.getView();
                ViewPropertyAnimator animation = view.animate();
                animation.setListener(new AnimationListenerWrapper(
                        createRevertOvershootAnimationListener(item, !result ? listener : null)));
                animation.setDuration(Math.round(revertOvershootAnimationDuration *
                        (Math.abs(getArithmetics().getRotation(Axis.ORTHOGONAL_AXIS, item)) /
                                maxAngle)));
                animation.setInterpolator(interpolator);
                getArithmetics().animateRotation(Axis.ORTHOGONAL_AXIS, animation, 0);
                animation.setStartDelay(0);
                animation.start();
                result = true;
            }
        }

        return result;
    }

    /**
     * Starts a reveal animation to add a specific tab.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be added, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param revealAnimation
     *         The reveal animation, which should be started, as an instance of the class {@link
     *         RevealAnimation}. The reveal animation may not be null
     */
    private void animateReveal(@NonNull final AbstractItem item,
                               @NonNull final RevealAnimation revealAnimation) {
        tabViewBottomMargin = -1;
        tabRecyclerAdapter.clearCachedPreviews();
        dragHandler.setCallback(null);
        View view = item.getView();
        ViewPropertyAnimator animation = view.animate();
        animation.setInterpolator(
                revealAnimation.getInterpolator() != null ? revealAnimation.getInterpolator() :
                        new AccelerateDecelerateInterpolator());
        animation.setListener(new AnimationListenerWrapper(createHideSwitcherAnimationListener()));
        animation.setStartDelay(0);
        animation.setDuration(revealAnimation.getDuration() != -1 ? revealAnimation.getDuration() :
                revealAnimationDuration);
        getArithmetics().animateScale(Axis.DRAGGING_AXIS, animation, 1);
        getArithmetics().animateScale(Axis.ORTHOGONAL_AXIS, animation, 1);
        animation.start();
        animateToolbarVisibility(getModel().areToolbarsShown() && getModel().isEmpty(), 0);
    }

    /**
     * Starts a peek animation to add a specific tab.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be added, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param duration
     *         The duration of the animation in milliseconds as a {@link Long} value
     * @param interpolator
     *         The interpolator, which should be used by the animation, as an instance of the type
     *         {@link Interpolator}. The interpolator may not be null
     * @param peekPosition
     *         The position on the dragging axis, the tab should be moved to, in pixels as a {@link
     *         Float} value
     * @param peekAnimation
     *         The peek animation, which has been used to add the tab, as an instance of the class
     *         {@link PeekAnimation}. The peek animation may not be null
     */
    private void animatePeek(@NonNull final AbstractItem item, final long duration,
                             @NonNull final Interpolator interpolator, final float peekPosition,
                             @NonNull final PeekAnimation peekAnimation) {
        PhoneTabViewHolder viewHolder = (PhoneTabViewHolder) ((TabItem) item).getViewHolder();
        viewHolder.closeButton.setVisibility(View.GONE);
        View view = item.getView();
        float x = peekAnimation.getX();
        float y = peekAnimation.getY() + tabTitleContainerHeight;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        view.setAlpha(1f);
        getArithmetics().setPivot(Axis.X_AXIS, item, x);
        getArithmetics().setPivot(Axis.Y_AXIS, item, y);
        view.setX(layoutParams.leftMargin);
        view.setY(layoutParams.topMargin);
        getArithmetics().setScale(Axis.DRAGGING_AXIS, item, 0);
        getArithmetics().setScale(Axis.ORTHOGONAL_AXIS, item, 0);
        ViewPropertyAnimator animation = view.animate();
        animation.setInterpolator(interpolator);
        animation.setListener(
                new AnimationListenerWrapper(createPeekAnimationListener(item, peekAnimation)));
        animation.setStartDelay(0);
        animation.setDuration(duration);
        getArithmetics().animateScale(Axis.DRAGGING_AXIS, animation, 1);
        getArithmetics().animateScale(Axis.ORTHOGONAL_AXIS, animation, 1);
        getArithmetics().animatePosition(Axis.DRAGGING_AXIS, animation, item, peekPosition, true);
        animation.start();
        int selectedTabIndex = getModel().getSelectedTabIndex();
        TabItem selectedItem = TabItem.create(getModel(), tabViewRecycler, selectedTabIndex);
        tabViewRecycler.inflate(selectedItem);
        selectedItem.getTag().setPosition(0);
        PhoneTabViewHolder selectedTabViewHolder =
                (PhoneTabViewHolder) selectedItem.getViewHolder();
        selectedTabViewHolder.closeButton.setVisibility(View.GONE);
        animateShowSwitcher(selectedItem, duration, interpolator,
                createZoomOutAnimationListener(selectedItem, peekAnimation));
    }

    /**
     * Creates and returns a layout listener, which allows to animate the position and size of a tab
     * in order to show the tab switcher, once its view has been inflated.
     *
     * @param item
     *         The item, which corresponds to the tab, whose view should be animated, as an instance
     *         of the class {@link AbstractItem}. The item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createShowSwitcherLayoutListener(
            @NonNull final AbstractItem item) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                animateShowSwitcher(item, createUpdateViewAnimationListener(item));
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to animate the position and size of a tab
     * in order to hide the tab switcher, once its view has been inflated.
     *
     * @param item
     *         The item, which corresponds to the tab, whose view should be animated, as an instance
     *         of the class {@link AbstractItem}. The item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createHideSwitcherLayoutListener(
            @NonNull final AbstractItem item) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                animateHideSwitcher(item, item.getIndex() == getModel().getSelectedTabIndex() ?
                        createHideSwitcherAnimationListener() : null);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to remove a tab, once its view has been
     * inflated.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be removed, as an instance of
     *         the class {@link AbstractItem}. The item may not be null
     * @param swipeAnimation
     *         The animation, which should be used, as an instance of the class {@link
     *         SwipeAnimation}. The animation may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createRemoveLayoutListener(@NonNull final AbstractItem item,
                                                              @NonNull final SwipeAnimation swipeAnimation) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                animateRemove(item, swipeAnimation);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to relocate a tab, once its view has been
     * inflated.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be relocated, as an instance of
     *         the class {@link AbstractItem}. The item may not be null
     * @param position
     *         The position, the tab should be relocated to, in pixels as a {@link Float} value
     * @param tag
     *         The tag, which should be applied to the given item, as an instance of the class
     *         {@link Tag} or null, if no tag should be applied
     * @param delayMultiplier
     *         The multiplier, which should be used to calculate the delay of the relocate
     *         animation, by being multiplied with the default delay, as an {@link Integer} value
     * @param listener
     *         The listener, which should be notified about the progress of the relocate animation,
     *         as an instance of the type {@link AnimatorListener} or null, if no listener should be
     *         notified
     * @param swipeAnimation
     *         The animation, which has been used to add or remove the tab, which caused the other
     *         tabs to be relocated, as an instance of the class {@link SwipeAnimation}. The
     *         animation may not be null
     * @return The listener, which has been created, as an instance of the class {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createRelocateLayoutListener(@NonNull final AbstractItem item,
                                                                final float position,
                                                                @Nullable final Tag tag,
                                                                final int delayMultiplier,
                                                                @Nullable final AnimatorListener listener,
                                                                @NonNull final SwipeAnimation swipeAnimation) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                animateRelocate(item, position, tag, delayMultiplier, listener, swipeAnimation);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to show a tab as the currently selected
     * one, once it view has been inflated.
     *
     * @param item
     *         The item, which corresponds to the tab, which has been added, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createAddSelectedTabLayoutListener(
            @NonNull final AbstractItem item) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                View view = item.getView();
                FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) view.getLayoutParams();
                view.setAlpha(1f);
                getArithmetics().setPivot(Axis.DRAGGING_AXIS, item,
                        getArithmetics().getPivot(Axis.DRAGGING_AXIS, item, DragState.NONE));
                getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item,
                        getArithmetics().getPivot(Axis.ORTHOGONAL_AXIS, item, DragState.NONE));
                view.setX(layoutParams.leftMargin);
                view.setY(layoutParams.topMargin);
                getArithmetics().setScale(Axis.DRAGGING_AXIS, item, 1);
                getArithmetics().setScale(Axis.ORTHOGONAL_AXIS, item, 1);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to start a reveal animation to add a tab,
     * once its view has been inflated.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be added, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param revealAnimation
     *         The reveal animation, which should be started, as an instance of the class {@link
     *         RevealAnimation}. The reveal animation may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createRevealLayoutListener(@NonNull final AbstractItem item,
                                                              @NonNull final RevealAnimation revealAnimation) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                View view = item.getView();
                float x = revealAnimation.getX();
                float y = revealAnimation.getY() + tabTitleContainerHeight;
                FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) view.getLayoutParams();
                view.setAlpha(1f);
                getArithmetics().setPivot(Axis.X_AXIS, item, x);
                getArithmetics().setPivot(Axis.Y_AXIS, item, y);
                view.setX(layoutParams.leftMargin);
                view.setY(layoutParams.topMargin);
                getArithmetics().setScale(Axis.DRAGGING_AXIS, item, 0);
                getArithmetics().setScale(Axis.ORTHOGONAL_AXIS, item, 0);
                animateReveal(item, revealAnimation);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to start a peek animation to add a tab,
     * once its view has been inflated.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be added, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param peekAnimation
     *         The peek animation, which should be started, as an instance of the class {@link
     *         PeekAnimation}. The peek animation may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    private OnGlobalLayoutListener createPeekLayoutListener(@NonNull final AbstractItem item,
                                                            @NonNull final PeekAnimation peekAnimation) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                long totalDuration =
                        peekAnimation.getDuration() != -1 ? peekAnimation.getDuration() :
                                peekAnimationDuration;
                long duration = totalDuration / 3;
                Interpolator interpolator =
                        peekAnimation.getInterpolator() != null ? peekAnimation.getInterpolator() :
                                new AccelerateDecelerateInterpolator();
                float peekPosition =
                        getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false) * 0.66f;
                animatePeek(item, duration, interpolator, peekPosition, peekAnimation);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to start a swipe animations to add
     * several tabs, once their views have been inflated.
     *
     * @param addedItems
     *         An array, which contains the items, which correspond to the tabs, which should be
     *         added, as an array of the type {@link AbstractItem}. The array may not be null
     * @param swipeAnimation
     *         The swipe animation, which should be started, as an instance of the class {@link
     *         SwipeAnimation}. The swipe animation may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createSwipeLayoutListener(
            @NonNull final AbstractItem[] addedItems,
            @NonNull final SwipeAnimation swipeAnimation) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                int count = getModel().getCount();
                float previousAttachedPosition =
                        calculateAttachedPosition(count - addedItems.length);
                float attachedPosition = calculateAttachedPosition(count);
                AbstractItem[] items;

                if (count - addedItems.length == 0) {
                    items = calculateInitialItems(-1, -1);
                } else {
                    AbstractItem firstAddedItem = addedItems[0];
                    int index = firstAddedItem.getIndex();
                    boolean isReferencingPredecessor = index > 0;
                    int referenceIndex = isReferencingPredecessor ? index - 1 :
                            (index + addedItems.length - 1 < count - 1 ? index + addedItems.length :
                                    -1);
                    AbstractItem referenceItem = referenceIndex != -1 ?
                            TabItem.create(getTabSwitcher(), tabViewRecycler, referenceIndex) :
                            null;
                    State state = referenceItem != null ? referenceItem.getTag().getState() : null;

                    if (state == null || state == State.STACKED_START) {
                        items = relocateWhenAddingStackedTabs(true, addedItems, swipeAnimation);
                    } else if (state == State.STACKED_END) {
                        items = relocateWhenAddingStackedTabs(false, addedItems, swipeAnimation);
                    } else if (state == State.FLOATING ||
                            (state == State.STACKED_START_ATOP && (index > 0 || count <= 2))) {
                        items = relocateWhenAddingFloatingTabs(addedItems, referenceItem,
                                isReferencingPredecessor, attachedPosition,
                                attachedPosition != previousAttachedPosition, swipeAnimation);
                    } else {
                        items = relocateWhenAddingHiddenTabs(addedItems, referenceItem);
                    }
                }

                Tag previousTag = null;

                for (AbstractItem item : items) {
                    Tag tag = item.getTag();

                    if (previousTag == null || tag.getPosition() != previousTag.getPosition()) {
                        createBottomMarginLayoutListener(item).onGlobalLayout();
                        View view = item.getView();
                        view.setTag(R.id.tag_properties, tag);
                        view.setAlpha(swipedTabAlpha);
                        float swipePosition = calculateSwipePosition();
                        float scale = getArithmetics().getScale(item, true);
                        getArithmetics().setPivot(Axis.DRAGGING_AXIS, item, getArithmetics()
                                .getPivot(Axis.DRAGGING_AXIS, item, DragState.NONE));
                        getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item, getArithmetics()
                                .getPivot(Axis.ORTHOGONAL_AXIS, item, DragState.NONE));
                        getArithmetics().setPosition(Axis.DRAGGING_AXIS, item, tag.getPosition());
                        getArithmetics().setPosition(Axis.ORTHOGONAL_AXIS, item,
                                swipeAnimation.getDirection() == SwipeDirection.LEFT_OR_TOP ?
                                        -1 * swipePosition : swipePosition);
                        getArithmetics().setScale(Axis.DRAGGING_AXIS, item, scale);
                        getArithmetics().setScale(Axis.ORTHOGONAL_AXIS, item, scale);
                        getArithmetics().setPivot(Axis.DRAGGING_AXIS, item, getArithmetics()
                                .getPivot(Axis.DRAGGING_AXIS, item, DragState.SWIPE));
                        getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item, getArithmetics()
                                .getPivot(Axis.ORTHOGONAL_AXIS, item, DragState.SWIPE));
                        getArithmetics().setScale(Axis.DRAGGING_AXIS, item, swipedTabScale * scale);
                        getArithmetics()
                                .setScale(Axis.ORTHOGONAL_AXIS, item, swipedTabScale * scale);
                        animateSwipe(item, false, 0, swipeAnimation,
                                createSwipeAnimationListener(item));
                    } else {
                        tabViewRecycler.remove(item);
                    }

                    previousTag = tag;
                }
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to position a tab, which is a neighbor of
     * the currently selected tab, when swiping horizontally.
     *
     * @param neighbor
     *         The tab item, which corresponds to the neighboring tab, as an instance of the class
     *         {@link TabItem}. The tab item may not be null
     * @param dragDistance
     *         The distance of the swipe gesture in pixels as a {@link Float} value
     * @return The layout listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The layout listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createSwipeNeighborLayoutListener(
            @NonNull final TabItem neighbor, final float dragDistance) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                float position;

                if (dragDistance > 0) {
                    position = -getTabSwitcher().getWidth() + dragDistance - swipedTabDistance;
                } else {
                    position = getTabSwitcher().getWidth() + dragDistance + swipedTabDistance;
                }

                getArithmetics().setPosition(Axis.X_AXIS, neighbor, position);
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to adapt the bottom margin of a tab, once
     * its view has been inflated.
     *
     * @param item
     *         The item, which corresponds to the tab, whose view should be adapted, as an instance
     *         of the class {@link AbstractItem}. The item may not be null
     * @return The layout listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The layout listener may not be null
     */
    private OnGlobalLayoutListener createBottomMarginLayoutListener(
            @NonNull final AbstractItem item) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                View view = item.getView();

                if (tabViewBottomMargin == -1) {
                    tabViewBottomMargin = calculateBottomMargin(item);
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
     * @param item
     *         The item, which corresponds to the tab, whose view should be adapted, as an instance
     *         of the class {@link AbstractItem}. The item may not be null
     * @param dragging
     *         True, if the item is currently being dragged, false otherwise
     * @param layoutListener
     *         The layout lister, which should be notified, when the created listener is invoked, as
     *         an instance of the type {@link OnGlobalLayoutListener} or null, if no listener should
     *         be notified
     * @return The layout listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The layout listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createInflateViewLayoutListener(@NonNull final AbstractItem item,
                                                                   final boolean dragging,
                                                                   @Nullable final OnGlobalLayoutListener layoutListener) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                adaptViewSize(item);
                updateView(item, dragging);

                if (layoutListener != null) {
                    layoutListener.onGlobalLayout();
                }
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to adapt the visibility of the
     * toolbar, when an animation, which is used to animate the alpha of the toolbar, has been
     * started or ended, depending on whether the toolbar should be shown, or hidden.
     *
     * @param show
     *         True, if the toolbar should be shown by the animation, false otherwise
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createToolbarAnimationListener(final boolean show) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);

                if (!show) {
                    toolbar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAnimationStart(final Animator animation) {
                super.onAnimationStart(animation);

                if (show) {
                    toolbar.setVisibility(View.VISIBLE);
                }
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to update the view, which is used to
     * visualize a specific tab, when an animation has been finished.
     *
     * @param item
     *         The item, which corresponds to the tab, whose view should be updated, as an instance
     *         of the class {@link AbstractItem}. The item may not be null
     * @return The animation listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createUpdateViewAnimationListener(@NonNull final AbstractItem item) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                inflateOrRemoveView(item, false);
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to inflate or remove the views, which
     * are used to visualize tabs, when an animation, which is used to hide the tab switcher, has
     * been finished.
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
                AbstractItemIterator iterator =
                        new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler).create();
                AbstractItem item;

                while ((item = iterator.next()) != null) {
                    if (((TabItem) item).getTab() == getModel().getSelectedTab()) {
                        Pair<View, Boolean> pair = tabViewRecycler.inflate(item);
                        View view = pair.first;
                        FrameLayout.LayoutParams layoutParams =
                                (FrameLayout.LayoutParams) view.getLayoutParams();
                        view.setAlpha(1f);
                        getArithmetics().setScale(Axis.DRAGGING_AXIS, item, 1);
                        getArithmetics().setScale(Axis.ORTHOGONAL_AXIS, item, 1);
                        view.setX(layoutParams.leftMargin);
                        view.setY(layoutParams.topMargin);
                    } else {
                        tabViewRecycler.remove(item);
                    }
                }

                tabViewRecycler.clearCache();
                tabRecyclerAdapter.clearCachedPreviews();
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
                tabViewRecycler.removeAll();
                setFirstVisibleIndex(-1);
                animateToolbarVisibility(getModel().areToolbarsShown(), 0);
            }

        };
    }

    /**
     * Creates and returns a listener, which allows to handle, when a tab has been swiped, but was
     * not removed.
     *
     * @param item
     *         The item, which corresponds to the tab, which has been swiped, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createSwipeAnimationListener(@NonNull final AbstractItem item) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                inflateOrRemoveView(item, false);
                adaptStackOnSwipeAborted(item, item.getIndex() + 1);
                item.getTag().setClosing(false);
                getArithmetics().setPivot(Axis.DRAGGING_AXIS, item,
                        getArithmetics().getPivot(Axis.DRAGGING_AXIS, item, DragState.NONE));
                animateToolbarVisibility(true, 0);
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to adapt the currently selected tab,
     * when swiping the tabs horizontally has been ended.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which should be selected, as an instance
     *         of the class {@link TabItem}. The tab item may not be null
     * @return The animation listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createSwipeSelectedTabAnimationListener(
            @NonNull final TabItem tabItem) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                Tab tab = tabItem.getTab();

                if (getModel().getSelectedTab() != tab) {
                    getModel().selectTab(tab);
                }
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to remove the view, which is used to
     * visualize a specific tab, when swiping the tabs horizontally has been ended.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be removed, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @return The animation listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createSwipeNeighborAnimationListener(@NonNull final TabItem tabItem) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                getTabViewRecycler().remove(tabItem);
            }

        };
    }

    /**
     * Creates and returns a listener, which allows to relocate all previous tabs, when a tab has
     * been removed.
     *
     * @param removedItem
     *         The item, which corresponds to the tab, which has been removed, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param swipeAnimation
     *         The animation, which has been used to remove the tab, as an instance of the class
     *         {@link SwipeAnimation}. The animation may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createRemoveAnimationListener(@NonNull final AbstractItem removedItem,
                                                           @NonNull final SwipeAnimation swipeAnimation) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(final Animator animation) {
                super.onAnimationStart(animation);

                if (getModel().isEmpty()) {
                    setFirstVisibleIndex(-1);
                    animateToolbarVisibility(getModel().areToolbarsShown(), 0);
                }

                float previousAttachedPosition =
                        calculateAttachedPosition(getModel().getCount() + 1);
                float attachedPosition = calculateAttachedPosition(getModel().getCount());
                State state = removedItem.getTag().getState();

                if (state == State.STACKED_END) {
                    relocateWhenRemovingStackedTab(removedItem, false, swipeAnimation);
                } else if (state == State.STACKED_START) {
                    relocateWhenRemovingStackedTab(removedItem, true, swipeAnimation);
                } else if (state == State.FLOATING || state == State.STACKED_START_ATOP) {
                    relocateWhenRemovingFloatingTab(removedItem, attachedPosition,
                            previousAttachedPosition != attachedPosition, swipeAnimation);
                }
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                tabViewRecycler.remove(removedItem);
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to update or remove the view, which
     * is used to visualize a tab, when the animation, which has been used to relocate it, has been
     * ended.
     *
     * @param item
     *         The item, which corresponds to the tab, which has been relocated, as an instance of
     *         the class {@link AbstractItem}. The item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createRelocateAnimationListener(@NonNull final AbstractItem item) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(final Animator animation) {
                super.onAnimationStart(animation);
                item.getView().setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);

                if (item.getTag().getState() == State.STACKED_START_ATOP) {
                    adaptStackOnSwipeAborted(item, item.getIndex() + 1);
                }

                if (item.isVisible()) {
                    updateView(item, false);
                } else {
                    tabViewRecycler.remove(item);
                }

                ItemIterator iterator =
                        new ItemIterator.Builder(getModel(), getTabViewRecycler()).create();
                AbstractItem item;

                while ((item = iterator.next()) != null) {
                    if (item.getTag().getState() == State.FLOATING) {
                        setFirstVisibleIndex(item.getIndex());
                        break;
                    }
                }
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to adapt the pivot of a specific tab,
     * when an animation, which reverted an overshoot, has been ended.
     *
     * @param item
     *         The item, which corresponds to the tab, whose pivot should be adapted, as an instance
     *         of the class {@link AbstractItem}. The item may not be null
     * @param listener
     *         The listener, which should be notified about the animation's progress, as an instance
     *         of the type {@link AnimatorListener} or null, if no listener should be notified
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createRevertOvershootAnimationListener(
            @NonNull final AbstractItem item, @Nullable final AnimatorListener listener) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                getArithmetics().setPivot(Axis.DRAGGING_AXIS, item,
                        getArithmetics().getPivot(Axis.DRAGGING_AXIS, item, DragState.NONE));
                getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item,
                        getArithmetics().getPivot(Axis.DRAGGING_AXIS, item, DragState.NONE));

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
     * Creates and returns an animation listener, which allows to hide a tab, which has been added
     * by using a peek animation, when the animation has been ended.
     *
     * @param item
     *         The item, which corresponds to the tab, which has been added by using the peek
     *         animation, as an instance of the class {@link AbstractItem}. The item may not be
     *         null
     * @param peekAnimation
     *         The peek animation as an instance of the class {@link PeekAnimation}. The peek
     *         animation may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createPeekAnimationListener(@NonNull final AbstractItem item,
                                                         @NonNull final PeekAnimation peekAnimation) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                long totalDuration =
                        peekAnimation.getDuration() != -1 ? peekAnimation.getDuration() :
                                peekAnimationDuration;
                long duration = totalDuration / 3;
                Interpolator interpolator =
                        peekAnimation.getInterpolator() != null ? peekAnimation.getInterpolator() :
                                new AccelerateDecelerateInterpolator();
                View view = item.getView();
                getArithmetics().setPivot(Axis.DRAGGING_AXIS, item, tabTitleContainerHeight);
                getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item,
                        getArithmetics().getSize(Axis.ORTHOGONAL_AXIS, item) / 2f);
                ViewPropertyAnimator animator = view.animate();
                animator.setDuration(duration);
                animator.setStartDelay(duration);
                animator.setInterpolator(interpolator);
                animator.setListener(
                        new AnimationListenerWrapper(createRevertPeekAnimationListener(item)));
                animator.alpha(0);
                getArithmetics().animatePosition(Axis.DRAGGING_AXIS, animator, item,
                        getArithmetics().getPosition(Axis.DRAGGING_AXIS, item) * 1.5f);
                getArithmetics().animateScale(Axis.DRAGGING_AXIS, animator, 0);
                getArithmetics().animateScale(Axis.ORTHOGONAL_AXIS, animator, 0);
                animator.start();
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to remove the view of a tab, which
     * has been added by using a peek animation, when the animation, which reverts the peek
     * animation, has been ended.
     *
     * @param item
     *         The item, which corresponds to the tab, which has been added by using the peek
     *         animation, as an instance of the class {@link AbstractItem}. The item may not be
     *         null
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createRevertPeekAnimationListener(@NonNull final AbstractItem item) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                tabViewRecycler.remove(item);
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to zoom in the currently selected
     * tab, when a peek animation has been ended.
     *
     * @param selectedItem
     *         The item, which corresponds to the currently selected tab, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param peekAnimation
     *         The peek animation as an instance of the class {@link PeekAnimation}. The peek
     *         animation may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createZoomOutAnimationListener(
            @NonNull final AbstractItem selectedItem, @NonNull final PeekAnimation peekAnimation) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                getModel().removeListener(PhoneTabSwitcherLayout.this);
                getModel().hideSwitcher();
                long totalDuration =
                        peekAnimation.getDuration() != -1 ? peekAnimation.getDuration() :
                                peekAnimationDuration;
                long duration = totalDuration / 3;
                Interpolator interpolator =
                        peekAnimation.getInterpolator() != null ? peekAnimation.getInterpolator() :
                                new AccelerateDecelerateInterpolator();
                animateHideSwitcher(selectedItem, duration, interpolator, duration,
                        createZoomInAnimationListener(selectedItem));
            }

        };
    }

    /**
     * Creates and returns an animation listener, which allows to restore the original state of a
     * tab, when an animation, which zooms in the tab, has been ended.
     *
     * @param item
     *         The item, which corresponds to the tab, which has been zoomed in, as an instance of
     *         the class {@link AbstractItem}. The item may not be null
     * @return The listener, which has been created, as an instance of the type {@link
     * AnimatorListener}. The listener may not be null
     */
    private AnimatorListener createZoomInAnimationListener(@NonNull final AbstractItem item) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                getModel().addListener(PhoneTabSwitcherLayout.this);
                tabViewRecycler.inflate(item);
                tabViewRecycler.clearCache();
                tabRecyclerAdapter.clearCachedPreviews();
                tabViewBottomMargin = -1;
            }

        };
    }

    /**
     * Adapts the stack, which is located at the start, when swiping a tab.
     *
     * @param swipedItem
     *         The item, which corresponds to the swiped tab, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param successorIndex
     *         The index of the tab, which is located after the swiped tab, as an {@link Integer}
     *         value
     * @param count
     *         The number of tabs, which are contained by the tab switcher, excluding the swiped
     *         tab, as an {@link Integer} value
     */
    private void adaptStackOnSwipe(@NonNull final AbstractItem swipedItem, final int successorIndex,
                                   final int count) {
        if (swipedItem.getTag().getState() == State.STACKED_START_ATOP &&
                successorIndex < getModel().getCount()) {
            AbstractItem item = TabItem.create(getTabSwitcher(), tabViewRecycler, successorIndex);
            State state = item.getTag().getState();

            if (state == State.HIDDEN || state == State.STACKED_START) {
                Pair<Float, State> pair =
                        calculatePositionAndStateWhenStackedAtStart(count, swipedItem.getIndex(),
                                (State) null);
                item.getTag().setPosition(pair.first);
                item.getTag().setState(pair.second);
                inflateOrRemoveView(item, false);
            }
        }
    }

    /**
     * Adapts the stack, which located at the start, when swiping a tab has been aborted.
     *
     * @param swipedItem
     *         The item, which corresponds to the swiped tab, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param successorIndex
     *         The index of the the tab, which is located after the swiped tab, as an {@link
     *         Integer} value
     */
    private void adaptStackOnSwipeAborted(@NonNull final AbstractItem swipedItem,
                                          final int successorIndex) {
        if (swipedItem.getTag().getState() == State.STACKED_START_ATOP &&
                successorIndex < getModel().getCount()) {
            AbstractItem item = TabItem.create(getTabSwitcher(), tabViewRecycler, successorIndex);

            if (item.getTag().getState() == State.STACKED_START_ATOP) {
                Pair<Float, State> pair =
                        calculatePositionAndStateWhenStackedAtStart(getTabSwitcher().getCount(),
                                item.getIndex(), swipedItem);
                item.getTag().setPosition(pair.first);
                item.getTag().setState(pair.second);
                inflateOrRemoveView(item, false);
            }
        }
    }

    /**
     * Adapts the size of the view, which is used to visualize a specific tab.
     *
     * @param item
     *         The item, which corresponds to the tab, whose view should be adapted, as an instance
     *         of the class {@link AbstractItem}. The item may not be null
     */
    private void adaptViewSize(@NonNull final AbstractItem item) {
        getArithmetics().setPivot(Axis.DRAGGING_AXIS, item,
                getArithmetics().getPivot(Axis.DRAGGING_AXIS, item, DragState.NONE));
        getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item,
                getArithmetics().getPivot(Axis.ORTHOGONAL_AXIS, item, DragState.NONE));
        float scale = getArithmetics().getScale(item, true);
        getArithmetics().setScale(Axis.DRAGGING_AXIS, item, scale);
        getArithmetics().setScale(Axis.ORTHOGONAL_AXIS, item, scale);
    }

    /**
     * Relocates all previous tabs, when a floating tab has been removed from the tab switcher.
     *
     * @param removedItem
     *         The item, which corresponds to the tab, which has been removed, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param attachedPosition
     *         The attached position in pixels as a {@link Float} value
     * @param attachedPositionChanged
     *         True, if removing the tab caused the attached position to be changed, false
     *         otherwise
     * @param swipeAnimation
     *         The animation, which has been used to remove the tab, as an instance of the class
     *         {@link SwipeAnimation}. The animation may not be null
     */
    private void relocateWhenRemovingFloatingTab(@NonNull final AbstractItem removedItem,
                                                 final float attachedPosition,
                                                 final boolean attachedPositionChanged,
                                                 final SwipeAnimation swipeAnimation) {
        AbstractItemIterator iterator;
        AbstractItem item;
        float defaultTabSpacing = calculateMaxTabSpacing(null);
        float minTabSpacing = calculateMinTabSpacing();
        int referenceIndex = removedItem.getIndex();
        AbstractItem currentReferenceItem = removedItem;
        float referencePosition = removedItem.getTag().getPosition();

        if (attachedPositionChanged && getModel().getCount() > 0) {
            int neighboringIndex = removedItem.getIndex() > 0 ? referenceIndex - 1 : referenceIndex;
            referencePosition += Math.abs(
                    TabItem.create(getTabSwitcher(), tabViewRecycler, neighboringIndex).getTag()
                            .getPosition() - referencePosition) / 2f;
        }

        referencePosition =
                Math.min(calculateMaxEndPosition(removedItem.getIndex() - 1), referencePosition);
        float initialReferencePosition = referencePosition;

        if (removedItem.getIndex() > 0) {
            int selectedTabIndex = getModel().getSelectedTabIndex();
            AbstractItem selectedItem =
                    TabItem.create(getTabSwitcher(), tabViewRecycler, selectedTabIndex);
            float maxTabSpacing = calculateMaxTabSpacing(selectedItem);
            iterator = new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler)
                    .start(removedItem.getIndex() - 1).reverse(true).create();

            while ((item = iterator.next()) != null) {
                AbstractItem predecessor = iterator.peek();
                float currentTabSpacing = calculateMaxTabSpacing(currentReferenceItem);
                Pair<Float, State> pair;

                if (item.getIndex() == removedItem.getIndex() - 1) {
                    pair = clipPosition(item.getIndex(), referencePosition, predecessor);
                    currentReferenceItem = item;
                    referencePosition = pair.first;
                    referenceIndex = item.getIndex();
                } else if (referencePosition >= attachedPosition - currentTabSpacing) {
                    float position;

                    if (selectedTabIndex > item.getIndex() && selectedTabIndex <= referenceIndex) {
                        position = referencePosition + maxTabSpacing +
                                ((referenceIndex - item.getIndex() - 1) * defaultTabSpacing);
                    } else {
                        position = referencePosition +
                                ((referenceIndex - item.getIndex()) * defaultTabSpacing);
                    }

                    pair = clipPosition(item.getIndex(), position, predecessor);
                } else {
                    AbstractItem successor = iterator.previous();
                    float successorPosition = successor.getTag().getPosition();
                    float position = (attachedPosition * (successorPosition + minTabSpacing)) /
                            (minTabSpacing + attachedPosition - currentTabSpacing);
                    pair = clipPosition(item.getIndex(), position, predecessor);

                    if (pair.first >= attachedPosition - currentTabSpacing) {
                        currentReferenceItem = item;
                        referencePosition = pair.first;
                        referenceIndex = item.getIndex();
                    }
                }

                Tag tag = item.getTag().clone();
                tag.setPosition(pair.first);
                tag.setState(pair.second);

                if (tag.getState() != State.HIDDEN) {
                    int delayMultiplier = Math.abs(removedItem.getIndex() - item.getIndex());

                    if (!item.isInflated()) {
                        Pair<Float, State> pair2 =
                                calculatePositionAndStateWhenStackedAtEnd(item.getIndex());
                        item.getTag().setPosition(pair2.first);
                        item.getTag().setState(pair2.second);
                    }

                    relocate(item, tag.getPosition(), tag, delayMultiplier, swipeAnimation);
                } else {
                    break;
                }
            }
        }

        if (attachedPositionChanged && getModel().getCount() > 2 &&
                removedItem.getTag().getState() != State.STACKED_START_ATOP) {
            iterator = new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler)
                    .start(removedItem.getIndex()).create();
            float previousPosition = initialReferencePosition;
            Tag previousTag = removedItem.getTag();

            while ((item = iterator.next()) != null &&
                    item.getIndex() < getModel().getCount() - 1) {
                float position =
                        calculateSuccessorPosition(previousPosition, calculateMaxTabSpacing(item));
                Pair<Float, State> pair =
                        clipPosition(item.getIndex(), position, previousTag.getState());
                Tag tag = item.getTag().clone();
                tag.setPosition(pair.first);
                tag.setState(pair.second);
                int delayMultiplier = Math.abs(removedItem.getIndex() - item.getIndex()) + 1;

                if (!item.isInflated()) {
                    Pair<Float, State> pair2 =
                            calculatePositionAndStateWhenStackedAtStart(getModel().getCount(),
                                    item.getIndex(), iterator.previous());
                    item.getTag().setPosition(pair2.first);
                    item.getTag().setState(pair2.second);
                }

                relocate(item, tag.getPosition(), tag, delayMultiplier, swipeAnimation);
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
     * @param removedItem
     *         The item, which corresponds to the tab, which has been removed, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param start
     *         True, if the removed tab was part of the stack, which is located at the start, false,
     *         if it was part of the stack, which is located at the end
     * @param swipeAnimation
     *         The animation, which has been used to remove the tab, as an instance of the class
     *         {@link SwipeAnimation}. The animation may not be nullD
     */
    private void relocateWhenRemovingStackedTab(@NonNull final AbstractItem removedItem,
                                                final boolean start,
                                                @NonNull final SwipeAnimation swipeAnimation) {
        int startIndex = removedItem.getIndex() + (start ? -1 : 0);
        ItemIterator iterator =
                new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler).reverse(start)
                        .start(startIndex).create();
        AbstractItem item;
        float previousProjectedPosition = removedItem.getTag().getPosition();

        while ((item = iterator.next()) != null && (item.getTag().getState() == State.HIDDEN ||
                item.getTag().getState() == State.STACKED_START ||
                item.getTag().getState() == State.STACKED_START_ATOP ||
                item.getTag().getState() == State.STACKED_END)) {
            float projectedPosition = item.getTag().getPosition();

            if (item.getTag().getState() == State.HIDDEN) {
                AbstractItem previous = iterator.previous();
                item.getTag().setState(previous.getTag().getState());

                if (item.isVisible()) {
                    Pair<Float, State> pair = start ?
                            calculatePositionAndStateWhenStackedAtStart(getTabSwitcher().getCount(),
                                    item.getIndex(), item) :
                            calculatePositionAndStateWhenStackedAtEnd(item.getIndex());
                    item.getTag().setPosition(pair.first);
                    item.getTag().setState(pair.second);
                    inflateAndUpdateView(item, false, null);
                }

                break;
            } else {
                item.getTag().setPosition(previousProjectedPosition);
                int delayMultiplier = Math.abs(startIndex - item.getIndex()) + 1;
                animateRelocate(item, previousProjectedPosition, null, delayMultiplier,
                        createRelocateAnimationListener(item), swipeAnimation);
            }

            previousProjectedPosition = projectedPosition;
        }
    }

    /**
     * Relocates all previous tabs, when floating tabs have been added to the tab switcher.
     *
     * @param addedItems
     *         An array, which contains the items, which correspond to the tabs, which have been
     *         added, as an array of the type {@link AbstractItem}. The array may not be null
     * @param referenceItem
     *         The item, which corresponds to the tab, which is used as a reference, as an instance
     *         of the class {@link AbstractItem}. The item may not be null
     * @param isReferencingPredecessor
     *         True, if the tab, which is used as a reference, is the predecessor of the added tab,
     *         false if it is the successor
     * @param attachedPosition
     *         The current attached position in pixels as a {@link Float} value
     * @param attachedPositionChanged
     *         True, if adding the tab caused the attached position to be changed, false otherwise
     * @param swipeAnimation
     *         The animation, which has been used to add the tabs, as an instance of the class
     *         {@link SwipeAnimation}. The animation may not be null
     * @return An array, which contains the items, which correspond to the tabs, which have been
     * added, as an array of the type {@link AbstractItem}. The array may not be null
     */
    @NonNull
    private AbstractItem[] relocateWhenAddingFloatingTabs(@NonNull final AbstractItem[] addedItems,
                                                          @NonNull final AbstractItem referenceItem,
                                                          final boolean isReferencingPredecessor,
                                                          final float attachedPosition,
                                                          final boolean attachedPositionChanged,
                                                          @NonNull final SwipeAnimation swipeAnimation) {
        AbstractItem firstAddedItem = addedItems[0];
        AbstractItem lastAddedItem = addedItems[addedItems.length - 1];
        float referencePosition = referenceItem.getTag().getPosition();

        if (isReferencingPredecessor && attachedPositionChanged &&
                lastAddedItem.getIndex() < getModel().getCount() - 1) {
            int neighboringIndex = lastAddedItem.getIndex() + 1;
            referencePosition -= Math.abs(referencePosition -
                    TabItem.create(getTabSwitcher(), tabViewRecycler, neighboringIndex).getTag()
                            .getPosition()) / 2f;
        }

        float initialReferencePosition = referencePosition;
        int selectedTabIndex = getModel().getSelectedTabIndex();
        AbstractItem selectedItem =
                TabItem.create(getTabSwitcher(), tabViewRecycler, selectedTabIndex);
        float defaultTabSpacing = calculateMaxTabSpacing(null);
        float maxTabSpacing = calculateMaxTabSpacing(selectedItem);
        float minTabSpacing = calculateMinTabSpacing();
        AbstractItem currentReferenceItem = referenceItem;
        int referenceIndex = referenceItem.getIndex();
        AbstractItemIterator.AbstractBuilder builder =
                new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler);

        for (AbstractItem addedItem : addedItems) {
            int iterationReferenceIndex = referenceIndex;
            float iterationReferencePosition = referencePosition;
            AbstractItem iterationReferenceItem = currentReferenceItem;
            AbstractItemIterator iterator =
                    builder.start(addedItem.getIndex()).reverse(true).create();
            AbstractItem item;

            while ((item = iterator.next()) != null) {
                AbstractItem predecessor = iterator.peek();
                Pair<Float, State> pair;
                float currentTabSpacing = calculateMaxTabSpacing(iterationReferenceItem);

                if (isReferencingPredecessor && item.getIndex() == addedItem.getIndex()) {
                    State predecessorState =
                            predecessor != null ? predecessor.getTag().getState() : null;
                    pair = clipPosition(item.getIndex(), iterationReferencePosition,
                            predecessorState == State.STACKED_START_ATOP ? State.FLOATING :
                                    predecessorState);
                    currentReferenceItem = iterationReferenceItem = item;
                    initialReferencePosition =
                            referencePosition = iterationReferencePosition = pair.first;
                    referenceIndex = iterationReferenceIndex = item.getIndex();
                } else if (iterationReferencePosition >= attachedPosition - currentTabSpacing) {
                    float position;

                    if (selectedTabIndex > item.getIndex() &&
                            selectedTabIndex <= iterationReferenceIndex) {
                        position = iterationReferencePosition + maxTabSpacing +
                                ((iterationReferenceIndex - item.getIndex() - 1) *
                                        defaultTabSpacing);
                    } else {
                        position = iterationReferencePosition +
                                ((iterationReferenceIndex - item.getIndex()) * defaultTabSpacing);
                    }

                    pair = clipPosition(item.getIndex(), position, predecessor);
                } else {
                    AbstractItem successor = iterator.previous();
                    float successorPosition = successor.getTag().getPosition();
                    float position = (attachedPosition * (successorPosition + minTabSpacing)) /
                            (minTabSpacing + attachedPosition - currentTabSpacing);
                    pair = clipPosition(item.getIndex(), position, predecessor);

                    if (pair.first >= attachedPosition - currentTabSpacing) {
                        iterationReferenceItem = item;
                        iterationReferencePosition = pair.first;
                        iterationReferenceIndex = item.getIndex();
                    }
                }

                if (item.getIndex() >= firstAddedItem.getIndex() &&
                        item.getIndex() <= lastAddedItem.getIndex()) {
                    if (!isReferencingPredecessor && attachedPositionChanged &&
                            getModel().getCount() > 3) {
                        AbstractItem successor = iterator.previous();
                        float successorPosition = successor.getTag().getPosition();
                        float position = pair.first - Math.abs(pair.first - successorPosition) / 2f;
                        pair = clipPosition(item.getIndex(), position, predecessor);
                        initialReferencePosition = pair.first;
                    }

                    Tag tag = addedItems[item.getIndex() - firstAddedItem.getIndex()].getTag();
                    tag.setPosition(pair.first);
                    tag.setState(pair.second);
                } else {
                    Tag tag = item.getTag().clone();
                    tag.setPosition(pair.first);
                    tag.setState(pair.second);

                    if (!item.isInflated()) {
                        Pair<Float, State> pair2 =
                                calculatePositionAndStateWhenStackedAtEnd(item.getIndex());
                        item.getTag().setPosition(pair2.first);
                        item.getTag().setState(pair2.second);
                    }

                    relocate(item, tag.getPosition(), tag, 0, swipeAnimation);
                }

                if (pair.second == State.HIDDEN || pair.second == State.STACKED_END) {
                    setFirstVisibleIndex(getFirstVisibleIndex() + 1);
                    break;
                }
            }
        }

        if (attachedPositionChanged && getModel().getCount() > 3) {
            AbstractItemIterator iterator =
                    builder.start(lastAddedItem.getIndex() + 1).reverse(false).create();
            AbstractItem item;
            float previousPosition = initialReferencePosition;
            Tag previousTag = lastAddedItem.getTag();

            while ((item = iterator.next()) != null &&
                    item.getIndex() < getModel().getCount() - 1) {
                float position =
                        calculateSuccessorPosition(previousPosition, calculateMaxTabSpacing(item));
                Pair<Float, State> pair =
                        clipPosition(item.getIndex(), position, previousTag.getState());
                Tag tag = item.getTag().clone();
                tag.setPosition(pair.first);
                tag.setState(pair.second);

                if (!item.isInflated()) {
                    Pair<Float, State> pair2 =
                            calculatePositionAndStateWhenStackedAtStart(getModel().getCount(),
                                    item.getIndex(), iterator.previous());
                    item.getTag().setPosition(pair2.first);
                    item.getTag().setState(pair2.second);
                }

                relocate(item, tag.getPosition(), tag, 0, swipeAnimation);
                previousPosition = pair.first;
                previousTag = tag;

                if (pair.second == State.HIDDEN || pair.second == State.STACKED_START) {
                    break;
                }
            }
        }

        return addedItems;
    }

    /**
     * Relocates all neighboring tabs, when stacked tabs have been added to the tab switcher.
     *
     * @param start
     *         True, if the added tab was part of the stack, which is located at the start, false,
     *         if it was part of the stack, which is located at the end
     * @param addedItems
     *         An array, which contains the items, which correspond to the tabs, which have been
     *         added, as an array of the type {@link AbstractItem}. The array may not be null
     * @param swipeAnimation
     *         The animation, which has been used to add the tabs, as an instance of the class
     *         {@link SwipeAnimation}. The animation may not be null
     * @return An array, which contains the items, which correspond to the tabs, which have been
     * added, as an array of the type {@link AbstractItem}. The array may not be null
     */
    @NonNull
    private AbstractItem[] relocateWhenAddingStackedTabs(final boolean start,
                                                         @NonNull final AbstractItem[] addedItems,
                                                         @NonNull final SwipeAnimation swipeAnimation) {
        if (!start) {
            setFirstVisibleIndex(getFirstVisibleIndex() + addedItems.length);
        }

        AbstractItem firstAddedItem = addedItems[0];
        AbstractItem lastAddedItem = addedItems[addedItems.length - 1];
        AbstractItemIterator iterator = new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler)
                .start(start ? lastAddedItem.getIndex() : firstAddedItem.getIndex()).reverse(start)
                .create();
        AbstractItem item;

        while ((item = iterator.next()) != null &&
                (item.getTag().getState() == State.STACKED_START ||
                        item.getTag().getState() == State.STACKED_START_ATOP ||
                        item.getTag().getState() == State.STACKED_END ||
                        item.getTag().getState() == State.HIDDEN)) {
            AbstractItem predecessor = start ? iterator.peek() : iterator.previous();
            Pair<Float, State> pair = start ?
                    calculatePositionAndStateWhenStackedAtStart(getModel().getCount(),
                            item.getIndex(), predecessor) :
                    calculatePositionAndStateWhenStackedAtEnd(item.getIndex());

            if (start && predecessor != null && predecessor.getTag().getState() == State.FLOATING) {
                float predecessorPosition = predecessor.getTag().getPosition();
                float distance = predecessorPosition - pair.first;

                if (distance > calculateMinTabSpacing()) {
                    float position = calculateSuccessorPosition(item, predecessor);
                    pair = clipPosition(item.getIndex(), position, predecessor);
                }
            }

            if (item.getIndex() >= firstAddedItem.getIndex() &&
                    item.getIndex() <= lastAddedItem.getIndex()) {
                Tag tag = addedItems[item.getIndex() - firstAddedItem.getIndex()].getTag();
                tag.setPosition(pair.first);
                tag.setState(pair.second);
            } else if (item.isInflated()) {
                Tag tag = item.getTag().clone();
                tag.setPosition(pair.first);
                tag.setState(pair.second);
                animateRelocate(item, tag.getPosition(), tag, 0,
                        createRelocateAnimationListener(item), swipeAnimation);
            } else {
                break;
            }
        }

        return addedItems;
    }

    /**
     * Calculates the position and state of hidden tabs, which have been added to the tab switcher.
     *
     * @param addedItems
     *         An array, which contains the items, which correspond to the tabs, which have been
     *         added, as an array of the type {@link AbstractItem}. The array may not be null
     * @param referenceItem
     *         The item, which corresponds to the tab, which is used as a reference, as an instance
     *         of the class {@link AbstractItem}. The item may not be null
     * @return An array, which contains the items, which correspond to the tabs, which have been
     * added, as an array of the type {@link AbstractItem}. The array may not be null
     */
    @NonNull
    private AbstractItem[] relocateWhenAddingHiddenTabs(@NonNull final AbstractItem[] addedItems,
                                                        @NonNull final AbstractItem referenceItem) {
        boolean stackedAtStart = isStackedAtStart(referenceItem.getIndex());

        for (AbstractItem item : addedItems) {
            Pair<Float, State> pair;

            if (stackedAtStart) {
                AbstractItem predecessor = item.getIndex() > 0 ?
                        TabItem.create(getTabSwitcher(), tabViewRecycler, item.getIndex() - 1) :
                        null;
                pair = calculatePositionAndStateWhenStackedAtStart(getModel().getCount(),
                        item.getIndex(), predecessor);
            } else {
                pair = calculatePositionAndStateWhenStackedAtEnd(item.getIndex());
            }

            Tag tag = item.getTag();
            tag.setPosition(pair.first);
            tag.setState(pair.second);
        }

        return addedItems;
    }

    /**
     * Relocates a specific tab. If its view is now yet inflated, it is inflated first.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be relocated, as an instance of
     *         the class {@link AbstractItem}. The item may not be null
     * @param relocatePosition
     *         The position, the tab should be moved to, in pixels as an {@link Float} value
     * @param tag
     *         The tag, which should be applied to the tab, once it has been relocated, as an
     *         instance of the class {@link Tag} or null, if no tag should be applied
     * @param delayMultiplier
     *         The multiplier, which should be used to calculate the delay of the relocate
     *         animation, by being multiplied with the default delay, as an {@link Integer} value
     * @param swipeAnimation
     *         The animation, which has been used to add or remove the tab, which caused the other
     *         tabs to be relocated, as an instance of the class {@link SwipeAnimation}. The
     *         animation may not be null
     */
    private void relocate(@NonNull final AbstractItem item, final float relocatePosition,
                          @Nullable final Tag tag, final int delayMultiplier,
                          @NonNull final SwipeAnimation swipeAnimation) {
        if (item.isInflated()) {
            animateRelocate(item, relocatePosition, tag, delayMultiplier,
                    createRelocateAnimationListener(item), swipeAnimation);
        } else {
            inflateAndUpdateView(item, false,
                    createRelocateLayoutListener(item, relocatePosition, tag, delayMultiplier,
                            createRelocateAnimationListener(item), swipeAnimation));
            item.getView().setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Swipes a specific tab.
     *
     * @param item
     *         The item, which corresponds to the tab, which should be swiped, as an instance of the
     *         class {@link AbstractItem}. The item may not be null
     * @param distance
     *         The distance, the tab should be swiped by, in pixels as a {@link Float} value
     */
    private void swipe(@NonNull final AbstractItem item, final float distance) {
        View view = item.getView();

        if (!item.getTag().isClosing()) {
            adaptStackOnSwipe(item, item.getIndex() + 1, getModel().getCount() - 1);
        }

        item.getTag().setClosing(true);
        float dragDistance = distance;

        if (!((TabItem) item).getTab().isCloseable()) {
            dragDistance = (float) Math.pow(Math.abs(distance), 0.75);
            dragDistance = distance < 0 ? dragDistance * -1 : dragDistance;
        }

        getArithmetics().setPivot(Axis.DRAGGING_AXIS, item,
                getArithmetics().getPivot(Axis.DRAGGING_AXIS, item, DragState.SWIPE));
        getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item,
                getArithmetics().getPivot(Axis.ORTHOGONAL_AXIS, item, DragState.SWIPE));
        float scale = getArithmetics().getScale(item, true);
        float ratio = 1 - (Math.abs(dragDistance) / calculateSwipePosition());
        float scaledClosedTabScale = swipedTabScale * scale;
        float targetScale = scaledClosedTabScale + ratio * (scale - scaledClosedTabScale);
        getArithmetics().setScale(Axis.DRAGGING_AXIS, item, targetScale);
        getArithmetics().setScale(Axis.ORTHOGONAL_AXIS, item, targetScale);
        view.setAlpha(swipedTabAlpha + ratio * (1 - swipedTabAlpha));
        getArithmetics().setPosition(Axis.ORTHOGONAL_AXIS, item, dragDistance);
    }

    /**
     * Moves the first tab to overlap the other tabs, when overshooting at the start.
     *
     * @param position
     *         The position of the first tab in pixels as a {@link Float} value
     */
    private void startOvershoot(final float position) {
        ItemIterator iterator =
                new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler).create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            if (item.getIndex() == 0) {
                getArithmetics().setPivot(Axis.DRAGGING_AXIS, item,
                        getArithmetics().getPivot(Axis.DRAGGING_AXIS, item, DragState.NONE));
                getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item,
                        getArithmetics().getPivot(Axis.ORTHOGONAL_AXIS, item, DragState.NONE));
                getArithmetics().setPosition(Axis.DRAGGING_AXIS, item, position);
            } else if (item.isInflated()) {
                AbstractItem firstItem = iterator.first();
                View view = item.getView();
                view.setVisibility(getArithmetics().getPosition(Axis.DRAGGING_AXIS, firstItem) <=
                        getArithmetics().getPosition(Axis.DRAGGING_AXIS, item) ? View.INVISIBLE :
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
        ItemIterator iterator =
                new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler).create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            View view = item.getView();

            if (item.getIndex() == 0) {
                view.setCameraDistance(maxCameraDistance);
                getArithmetics().setPivot(Axis.DRAGGING_AXIS, item, getArithmetics()
                        .getPivot(Axis.DRAGGING_AXIS, item, DragState.OVERSHOOT_START));
                getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item, getArithmetics()
                        .getPivot(Axis.ORTHOGONAL_AXIS, item, DragState.OVERSHOOT_START));
                getArithmetics().setRotation(Axis.ORTHOGONAL_AXIS, item, angle);
            } else if (item.isInflated()) {
                item.getView().setVisibility(View.INVISIBLE);
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
        ItemIterator iterator =
                new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler).create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            if (item.isInflated()) {
                View view = item.getView();

                if (!iterator.hasNext()) {
                    view.setCameraDistance(maxCameraDistance);
                } else if (firstVisibleIndex == -1) {
                    view.setCameraDistance(minCameraDistance);

                    if (item.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = item.getIndex();
                    }
                } else {
                    int diff = item.getIndex() - firstVisibleIndex;
                    float ratio =
                            (float) diff / (float) (getModel().getCount() - firstVisibleIndex);
                    view.setCameraDistance(
                            minCameraDistance + (maxCameraDistance - minCameraDistance) * ratio);
                }

                getArithmetics().setPivot(Axis.DRAGGING_AXIS, item, getArithmetics()
                        .getPivot(Axis.DRAGGING_AXIS, item, DragState.OVERSHOOT_END));
                getArithmetics().setPivot(Axis.ORTHOGONAL_AXIS, item, getArithmetics()
                        .getPivot(Axis.ORTHOGONAL_AXIS, item, DragState.OVERSHOOT_END));
                getArithmetics().setRotation(Axis.ORTHOGONAL_AXIS, item, angle);
            }
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
     *         The model of the tab switcher, the layout belongs to, as an instance of the class
     *         {@link TabSwitcherModel}. The model may not be null
     * @param arithmetics
     *         The arithmetics, which should be used by the layout, as an instance of the class
     *         {@link PhoneArithmetics}. The arithmetics may not be null
     * @param style
     *         The style, which allows to retrieve style attributes of the tab switcher, as an
     *         instance of the class {@link TabSwitcherStyle}. The style may not be null
     * @param touchEventDispatcher
     *         The dispatcher, which is used to dispatch touch events to event handlers, as an
     *         instance of the class {@link TouchEventDispatcher}. The dispatcher may not be null
     */
    public PhoneTabSwitcherLayout(@NonNull final TabSwitcher tabSwitcher,
                                  @NonNull final TabSwitcherModel model,
                                  @NonNull final PhoneArithmetics arithmetics,
                                  @NonNull final TabSwitcherStyle style,
                                  @NonNull final TouchEventDispatcher touchEventDispatcher) {
        super(tabSwitcher, model, arithmetics, style, touchEventDispatcher);
        Resources resources = tabSwitcher.getResources();
        stackedTabCount = resources.getInteger(R.integer.phone_stacked_tab_count);
        tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        tabBorderWidth = resources.getDimensionPixelSize(R.dimen.tab_border_width);
        tabTitleContainerHeight =
                resources.getDimensionPixelSize(R.dimen.tab_title_container_height);
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
        relocateAnimationDuration = resources.getInteger(R.integer.relocate_animation_duration);
        revertOvershootAnimationDuration =
                resources.getInteger(R.integer.revert_overshoot_animation_duration);
        revealAnimationDuration = resources.getInteger(R.integer.reveal_animation_duration);
        peekAnimationDuration = resources.getInteger(R.integer.peek_animation_duration);
        emptyViewAnimationDuration = resources.getInteger(R.integer.empty_view_animation_duration);
        maxStartOvershootAngle = resources.getInteger(R.integer.max_start_overshoot_angle);
        maxEndOvershootAngle = resources.getInteger(R.integer.max_end_overshoot_angle);
        swipedTabDistance = resources.getDimensionPixelSize(R.dimen.swiped_tab_distance);
        tabViewBottomMargin = -1;
        toolbarAnimation = null;
    }

    @Override
    public final AbstractDragTabsEventHandler<?> getDragHandler() {
        return dragHandler;
    }

    @Override
    protected final void onInflateLayout(@NonNull final LayoutInflater inflater,
                                         final boolean tabsOnly) {
        if (tabsOnly) {
            toolbar = getTabSwitcher().findViewById(R.id.primary_toolbar);
            tabContainer = getTabSwitcher().findViewById(R.id.tab_container);
        } else {
            toolbar = (Toolbar) inflater.inflate(R.layout.phone_toolbar, getTabSwitcher(), false);
            getTabSwitcher().addView(toolbar);
            tabContainer = new FrameLayout(getContext());
            tabContainer.setId(R.id.tab_container);
            getTabSwitcher().addView(tabContainer, FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
        }

        contentViewRecycler = new ViewRecycler<>(inflater);
        tabRecyclerAdapter = new PhoneTabRecyclerAdapter(getTabSwitcher(), getModel(), getStyle(),
                contentViewRecycler);
        getModel().addListener(tabRecyclerAdapter);
        tabViewRecycler = new AttachedViewRecycler<>(tabContainer, inflater,
                Collections.reverseOrder(new ItemComparator(getTabSwitcher())));
        tabViewRecycler.setAdapter(tabRecyclerAdapter);
        tabRecyclerAdapter.setViewRecycler(tabViewRecycler);
        dragHandler =
                new PhoneDragTabsEventHandler(getTabSwitcher(), getArithmetics(), tabViewRecycler);
        adaptDecorator();
        adaptToolbarMargin();
    }

    @Nullable
    @Override
    protected final Pair<Integer, Float> onDetachLayout(final boolean tabsOnly) {
        Pair<Integer, Float> result = null;

        if (getTabSwitcher().isSwitcherShown() && getFirstVisibleIndex() != -1) {
            TabItem tabItem =
                    TabItem.create(getModel(), getTabViewRecycler(), getFirstVisibleIndex());
            Tag tag = tabItem.getTag();

            if (tag.getState() != State.HIDDEN) {
                float position = tag.getPosition();
                float draggingAxisSize =
                        getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false);
                float orthogonalAxisSize =
                        getArithmetics().getTabContainerSize(Axis.ORTHOGONAL_AXIS, false);
                result = Pair.create(getFirstVisibleIndex(),
                        position / Math.max(draggingAxisSize, orthogonalAxisSize));
            }
        }

        contentViewRecycler.removeAll();
        contentViewRecycler.clearCache();
        tabRecyclerAdapter.clearCachedPreviews();
        detachEmptyView();

        if (!tabsOnly) {
            getModel().removeListener(tabRecyclerAdapter);
        }

        return result;
    }

    @Override
    public final AbstractViewRecycler<Tab, Void> getContentViewRecycler() {
        return contentViewRecycler;
    }

    @Override
    protected final AttachedViewRecycler<AbstractItem, Integer> getTabViewRecycler() {
        return tabViewRecycler;
    }

    @Override
    protected final AbstractTabRecyclerAdapter getTabRecyclerAdapter() {
        return tabRecyclerAdapter;
    }

    @Override
    protected final void updateView(@NonNull final AbstractItem item, final boolean dragging) {
        View view = item.getView();
        view.setAlpha(1f);
        view.setVisibility(View.VISIBLE);
        getArithmetics().setPivot(Arithmetics.Axis.DRAGGING_AXIS, item, getArithmetics()
                .getPivot(Arithmetics.Axis.DRAGGING_AXIS, item,
                        AbstractDragTabsEventHandler.DragState.NONE));
        getArithmetics().setPivot(Arithmetics.Axis.ORTHOGONAL_AXIS, item, getArithmetics()
                .getPivot(Arithmetics.Axis.ORTHOGONAL_AXIS, item,
                        AbstractDragTabsEventHandler.DragState.NONE));
        super.updateView(item, dragging);
        getArithmetics().setRotation(Arithmetics.Axis.ORTHOGONAL_AXIS, item, 0);
    }

    @Override
    protected final float calculateAttachedPosition(final int count) {
        float totalSpace = getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false);
        float attachedPosition;

        if (count == 3) {
            attachedPosition = totalSpace * 0.66f;
        } else if (count == 4) {
            attachedPosition = totalSpace * 0.6f;
        } else {
            attachedPosition = totalSpace * 0.5f;
        }

        return attachedPosition;
    }

    @Override
    protected final void inflateAndUpdateView(@NonNull final AbstractItem item,
                                              final boolean dragging,
                                              @Nullable final OnGlobalLayoutListener listener) {
        inflateView(item, createInflateViewLayoutListener(item, dragging, listener),
                tabViewBottomMargin);
    }

    @Override
    protected final int getStackedTabCount() {
        return stackedTabCount;
    }

    @NonNull
    @Override
    protected final Pair<Float, State> calculatePositionAndStateWhenStackedAtStart(final int count,
                                                                                   final int index,
                                                                                   @Nullable final State predecessorState) {
        if ((count - index) <= getStackedTabCount()) {
            float position = getStackedTabSpacing() * (count - (index + 1));
            return Pair.create(position,
                    (predecessorState == null || predecessorState == State.FLOATING) ?
                            State.STACKED_START_ATOP : State.STACKED_START);
        } else {
            float position = getStackedTabSpacing() * getStackedTabCount();
            return Pair.create(position,
                    (predecessorState == null || predecessorState == State.FLOATING) ?
                            State.STACKED_START_ATOP : State.HIDDEN);
        }
    }

    @NonNull
    @Override
    protected final Pair<Float, State> calculatePositionAndStateWhenStackedAtEnd(final int index) {
        float size = getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false);

        if (index < getStackedTabCount()) {
            float position = size - tabInset - (getStackedTabSpacing() * (index + 1));
            return Pair.create(position, State.STACKED_END);
        } else {
            float position = size - tabInset - (getStackedTabSpacing() * getStackedTabCount());
            return Pair.create(position, State.HIDDEN);
        }
    }

    @Override
    protected final boolean isOvershootingAtStart() {
        if (getTabSwitcher().getCount() <= 1) {
            return true;
        } else {
            AbstractItemIterator iterator =
                    new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler).create();
            AbstractItem item = iterator.getItem(0);
            return item.getTag().getState() == State.STACKED_START_ATOP;
        }
    }

    @Override
    protected final boolean isOvershootingAtEnd(@NonNull final DragState dragState,
                                                @NonNull final AbstractItemIterator iterator) {
        if (getTabSwitcher().getCount() <= 1) {
            return dragState != DragState.DRAG_TO_START;
        } else {
            AbstractItem lastItem = iterator.getItem(getTabSwitcher().getCount() - 1);
            AbstractItem predecessor = iterator.getItem(getTabSwitcher().getCount() - 2);
            return Math.round(predecessor.getTag().getPosition()) >=
                    Math.round(calculateMaxTabSpacing(lastItem));
        }
    }

    @Override
    protected final float calculateMaxEndPosition(final int index) {
        float defaultMaxTabSpacing = calculateMaxTabSpacing(null);
        int selectedTabIndex = getTabSwitcher().getSelectedTabIndex();

        if (selectedTabIndex > index) {
            AbstractItemIterator iterator =
                    new ItemIterator.Builder(getTabSwitcher(), tabViewRecycler).create();
            AbstractItem selectedItem = iterator.getItem(selectedTabIndex);
            float selectedTabSpacing = calculateMaxTabSpacing(selectedItem);
            return (getTabSwitcher().getCount() - 2 - index) * defaultMaxTabSpacing +
                    selectedTabSpacing;
        }

        return (getTabSwitcher().getCount() - 1 - index) * defaultMaxTabSpacing;
    }

    @Override
    protected final float calculateSuccessorPosition(@NonNull final AbstractItem item,
                                                     @NonNull final AbstractItem predecessor) {
        float predecessorPosition = predecessor.getTag().getPosition();
        float maxTabSpacing = calculateMaxTabSpacing(item);
        return calculateSuccessorPosition(predecessorPosition, maxTabSpacing);
    }

    @Override
    protected final float calculatePredecessorPosition(@NonNull final AbstractItem item,
                                                       @NonNull final AbstractItem successor) {
        float successorPosition = successor.getTag().getPosition();
        return successorPosition + calculateMaxTabSpacing(successor);
    }

    @Nullable
    @Override
    public final ViewGroup getTabContainer() {
        return tabContainer;
    }

    @NonNull
    @Override
    public final Toolbar[] getToolbars() {
        return toolbar != null ? new Toolbar[]{toolbar} : null;
    }

    @Override
    public final void onDecoratorChanged(@NonNull final TabSwitcherDecorator decorator) {
        adaptDecorator();
        super.onDecoratorChanged(decorator);
    }

    @Override
    public final void onSwitcherShown() {
        getLogger().logInfo(getClass(), "Showed tab switcher");
        animateShowSwitcher(-1, -1);
    }

    @Override
    public final void onSwitcherHidden() {
        getLogger().logInfo(getClass(), "Hid tab switcher");
        animateHideSwitcher();
    }

    @Override
    public final void onSelectionChanged(final int previousIndex, final int index,
                                         @Nullable final Tab selectedTab,
                                         final boolean switcherHidden) {
        getLogger().logInfo(getClass(), "Selected tab at index " + index);

        if (switcherHidden) {
            animateHideSwitcher();
        } else {
            tabViewRecycler
                    .remove(TabItem.create(getTabSwitcher(), tabViewRecycler, previousIndex));
            tabViewRecycler.inflate(TabItem.create(getTabSwitcher(), tabViewRecycler, index));
        }
    }

    @Override
    public final void onTabAdded(final int index, @NonNull final Tab tab,
                                 final int previousSelectedTabIndex, final int selectedTabIndex,
                                 final boolean selectionChanged,
                                 final boolean switcherVisibilityChanged,
                                 @NonNull final Animation animation) {
        getLogger().logInfo(getClass(),
                "Added tab at index " + index + " using a " + animation.getClass().getSimpleName());

        if (animation instanceof PeekAnimation && getModel().getCount() > 1) {
            Condition.INSTANCE.ensureTrue(switcherVisibilityChanged,
                    animation.getClass().getSimpleName() +
                            " not supported when the tab switcher is shown");
            PeekAnimation peekAnimation = (PeekAnimation) animation;
            AbstractItem item = TabItem.create(getModel(), 0, tab);
            inflateView(item, createPeekLayoutListener(item, peekAnimation));
        } else if (animation instanceof RevealAnimation && switcherVisibilityChanged) {
            AbstractItem item = TabItem.create(getModel(), 0, tab);
            RevealAnimation revealAnimation = (RevealAnimation) animation;
            inflateView(item, createRevealLayoutListener(item, revealAnimation));
        } else {
            addAllTabs(index, new Tab[]{tab}, animation);
        }

        adaptEmptyView(
                getModel().isSwitcherShown() ? getModel().getEmptyViewAnimationDuration() : 0);
    }

    @Override
    public final void onAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                     final int previousSelectedTabIndex, final int selectedTabIndex,
                                     final boolean selectionChanged,
                                     @NonNull final Animation animation) {
        Condition.INSTANCE.ensureTrue(animation instanceof SwipeAnimation,
                animation.getClass().getSimpleName() + " not supported for adding multiple tabs");
        getLogger().logInfo(getClass(),
                "Added " + tabs.length + " tabs at index " + index + " using a " +
                        animation.getClass().getSimpleName());
        addAllTabs(index, tabs, animation);
    }

    @Override
    public final void onTabRemoved(final int index, @NonNull final Tab tab,
                                   final int previousSelectedTabIndex, final int selectedTabIndex,
                                   final boolean selectionChanged,
                                   @NonNull final Animation animation) {
        Condition.INSTANCE.ensureTrue(animation instanceof SwipeAnimation,
                animation.getClass().getSimpleName() + " not supported for removing tabs");
        getLogger().logInfo(getClass(), "Removed tab at index " + index + " using a " +
                animation.getClass().getSimpleName());
        AbstractItem removedItem = TabItem.create(getModel(), tabViewRecycler, index, tab);

        if (!getModel().isSwitcherShown()) {
            tabViewRecycler.remove(removedItem);

            if (getModel().isEmpty()) {
                toolbar.setAlpha(getModel().areToolbarsShown() ? 1 : 0);
            } else if (selectionChanged) {
                tabViewRecycler.inflate(
                        TabItem.create(getTabSwitcher(), tabViewRecycler, selectedTabIndex));
            }
        } else {
            adaptStackOnSwipe(removedItem, removedItem.getIndex(), getModel().getCount());
            removedItem.getTag().setClosing(true);
            SwipeAnimation swipeAnimation =
                    animation instanceof SwipeAnimation ? (SwipeAnimation) animation :
                            new SwipeAnimation.Builder().create();

            if (removedItem.isInflated()) {
                animateRemove(removedItem, swipeAnimation);
            } else {
                boolean start = isStackedAtStart(index);
                AbstractItem predecessor =
                        TabItem.create(getTabSwitcher(), tabViewRecycler, index - 1);
                Pair<Float, State> pair = start ?
                        calculatePositionAndStateWhenStackedAtStart(getModel().getCount(), index,
                                predecessor) : calculatePositionAndStateWhenStackedAtEnd(index);
                removedItem.getTag().setPosition(pair.first);
                removedItem.getTag().setState(pair.second);
                inflateAndUpdateView(removedItem, false,
                        createRemoveLayoutListener(removedItem, swipeAnimation));
            }
        }

        adaptEmptyView(
                getModel().isSwitcherShown() ? getModel().getEmptyViewAnimationDuration() : 0);
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final Tab[] tabs,
                                       @NonNull final Animation animation) {
        Condition.INSTANCE.ensureTrue(animation instanceof SwipeAnimation,
                animation.getClass().getSimpleName() + " not supported for removing tabs ");
        getLogger().logInfo(getClass(),
                "Removed all tabs using a " + animation.getClass().getSimpleName());

        if (!getModel().isSwitcherShown()) {
            tabViewRecycler.removeAll();
            toolbar.setAlpha(getModel().areToolbarsShown() ? 1 : 0);
        } else {
            SwipeAnimation swipeAnimation =
                    animation instanceof SwipeAnimation ? (SwipeAnimation) animation :
                            new SwipeAnimation.Builder().create();
            AbstractItemIterator iterator =
                    new ArrayItemIterator.Builder(getModel(), tabViewRecycler, tabs, 0)
                            .reverse(true).create();
            AbstractItem item;
            int delayMultiplier = 0;

            while ((item = iterator.next()) != null) {
                AbstractItem previous = iterator.previous();

                if (item.getTag().getState() == State.FLOATING ||
                        (previous != null && previous.getTag().getState() == State.FLOATING)) {
                    delayMultiplier++;
                }

                if (item.isInflated()) {
                    animateSwipe(item, true, delayMultiplier, swipeAnimation,
                            !iterator.hasNext() ? createClearAnimationListener() : null);
                }
            }
        }

        adaptEmptyView(
                getModel().isSwitcherShown() ? getModel().getEmptyViewAnimationDuration() : 0);
    }

    @Override
    public final void onPaddingChanged(final int left, final int top, final int right,
                                       final int bottom) {
        // TODO: Detach and re-inflate tabs
        adaptToolbarMargin();
    }

    @Override
    public final void onApplyPaddingToTabsChanged(final boolean applyPaddingToTabs) {

    }

    @Override
    public final void onEmptyViewChanged(@Nullable final View view, final long animationDuration) {
        if (getModel().isEmpty()) {
            adaptEmptyView(0);
        }
    }

    @Override
    public final void onGlobalLayout() {
        if (getModel().isSwitcherShown()) {
            AbstractItem[] items = calculateInitialItems(getModel().getReferenceTabIndex(),
                    getModel().getReferenceTabPosition());
            AbstractItemIterator iterator = new InitialItemIteratorBuilder(items).create();
            AbstractItem item;

            while ((item = iterator.next()) != null) {
                if (item.isVisible()) {
                    inflateAndUpdateView(item, false, createBottomMarginLayoutListener(item));
                }
            }
        } else if (getModel().getSelectedTab() != null) {
            AbstractItem item = TabItem.create(getTabSwitcher(), tabViewRecycler,
                    getModel().getSelectedTabIndex());
            tabViewRecycler.inflate(item);
        }

        boolean showToolbar = getModel().areToolbarsShown() &&
                (getModel().isEmpty() || getModel().isSwitcherShown());
        toolbar.setAlpha(showToolbar ? 1 : 0);
        toolbar.setVisibility(showToolbar ? View.VISIBLE : View.INVISIBLE);
        adaptEmptyView(0);
    }

    @Override
    public final void onRevertStartOvershoot() {
        animateRevertStartOvershoot();
        getLogger().logVerbose(getClass(), "Reverting overshoot at the start");
    }

    @Override
    public final void onRevertEndOvershoot() {
        animateRevertEndOvershoot();
        getLogger().logVerbose(getClass(), "Reverting overshoot at the end");
    }

    public final void onStartOvershoot(final float position) {
        startOvershoot(position);
        getLogger().logVerbose(getClass(),
                "Overshooting at the start using a position of " + position + " pixels");
    }

    @Override
    public final void onTiltOnStartOvershoot(final float angle) {
        tiltOnStartOvershoot(angle);
        getLogger().logVerbose(getClass(),
                "Tilting on start overshoot using an angle of " + angle + " degrees");
    }

    @Override
    public final void onTiltOnEndOvershoot(final float angle) {
        tiltOnEndOvershoot(angle);
        getLogger().logVerbose(getClass(),
                "Tilting on end overshoot using an angle of " + angle + " degrees");
    }

    @Override
    public final void onSwipe(@NonNull final TabItem tabItem, final float distance) {
        swipe(tabItem, distance);
        getLogger().logVerbose(getClass(),
                "Swiping tab at index " + tabItem.getIndex() + ". Current swipe distance is " +
                        distance + " pixels");
    }

    @Override
    public final void onSwipeEnded(@NonNull final TabItem tabItem, final boolean remove,
                                   final float velocity) {
        if (remove) {
            SwipeDirection direction =
                    getArithmetics().getPosition(Axis.ORTHOGONAL_AXIS, tabItem) < 0 ?
                            SwipeDirection.LEFT_OR_TOP : SwipeDirection.RIGHT_OR_BOTTOM;
            long animationDuration =
                    velocity > 0 ? Math.round((calculateSwipePosition() / velocity) * 1000) : -1;
            Animation animation = new SwipeAnimation.Builder().setDirection(direction)
                    .setDuration(animationDuration).create();
            getModel().removeTab(tabItem.getTab(), animation);
        } else {
            animateSwipe(tabItem, false, 0, new SwipeAnimation.Builder().create(),
                    createSwipeAnimationListener(tabItem));
        }

        getLogger().logVerbose(getClass(),
                "Ended swiping tab at index " + tabItem.getIndex() + ". Tab will " +
                        (remove ? "" : "not ") + "be removed");
    }

    @Override
    public final void onSwitchingBetweenTabs(final int selectedTabIndex, final float distance) {
        TabItem tabItem = TabItem.create(getModel(), getTabViewRecycler(), selectedTabIndex);

        if (distance == 0 || (distance > 0 && selectedTabIndex < getModel().getCount() - 1) ||
                (distance < 0 && selectedTabIndex > 0)) {
            getArithmetics().setPosition(Axis.X_AXIS, tabItem, distance);
            float position = getArithmetics().getPosition(Axis.X_AXIS, tabItem);

            if (distance != 0) {
                TabItem neighbor = TabItem.create(getModel(), getTabViewRecycler(),
                        position > 0 ? selectedTabIndex + 1 : selectedTabIndex - 1);

                if (Math.abs(position) >= swipedTabDistance) {
                    inflateView(neighbor, createSwipeNeighborLayoutListener(neighbor, distance));
                } else {
                    getTabViewRecycler().remove(neighbor);
                }
            }
        } else {
            float position = (float) Math.pow(Math.abs(distance), 0.75);
            position = distance < 0 ? position * -1 : position;
            getArithmetics().setPosition(Axis.X_AXIS, tabItem, position);
        }

        getLogger().logVerbose(getClass(), "Swiping content of tab at index " + selectedTabIndex +
                ". Current swipe distance is " + distance + " pixels");
    }

    @Override
    public final void onSwitchingBetweenTabsEnded(final int selectedTabIndex,
                                                  final int previousSelectedTabIndex,
                                                  final boolean selectionChanged,
                                                  final float velocity,
                                                  final long animationDuration) {
        TabItem selectedTabItem =
                TabItem.create(getModel(), getTabViewRecycler(), selectedTabIndex);
        animateSwipe(selectedTabItem, 0, true, animationDuration, velocity);
        TabItem neighbor = null;
        boolean left = false;

        if (selectionChanged) {
            neighbor = TabItem.create(getModel(), getTabViewRecycler(), previousSelectedTabIndex);
            left = selectedTabIndex < previousSelectedTabIndex;
        } else if (getArithmetics().getPosition(Axis.X_AXIS, selectedTabItem) > 0) {
            if (selectedTabIndex + 1 < getModel().getCount()) {
                neighbor = TabItem.create(getModel(), getTabViewRecycler(), selectedTabIndex + 1);
                left = true;
            }
        } else {
            if (selectedTabIndex - 1 >= 0) {
                neighbor = TabItem.create(getModel(), getTabViewRecycler(), selectedTabIndex - 1);
                left = false;
            }
        }

        if (neighbor != null && neighbor.isInflated()) {
            float width = getArithmetics().getSize(Axis.X_AXIS, neighbor);
            float targetPosition =
                    left ? (width + swipedTabDistance) * -1 : width + swipedTabDistance;
            animateSwipe(neighbor, targetPosition, false, animationDuration, velocity);
        }
    }

    @Override
    public final void onPulledDown() {
        getModel().removeListener(this);
        getModel().showSwitcher();
        getModel().addListener(this);

        if (getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE) {
            animateShowSwitcher(-1, -1);
        } else {
            animateShowSwitcher(getModel().getSelectedTabIndex(), 0);
        }

        getDragHandler().getDragHelper().reset();
        getDragHandler().setPointerId(0);
        getDragHandler().setDragState(DragState.PULLING_DOWN);
    }

}