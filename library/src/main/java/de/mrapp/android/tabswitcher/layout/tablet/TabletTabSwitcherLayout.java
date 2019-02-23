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
package de.mrapp.android.tabswitcher.layout.tablet;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.gesture.TouchEventDispatcher;
import de.mrapp.android.tabswitcher.iterator.AbstractItemIterator;
import de.mrapp.android.tabswitcher.iterator.ItemIterator;
import de.mrapp.android.tabswitcher.layout.AbstractDragTabsEventHandler;
import de.mrapp.android.tabswitcher.layout.AbstractTabRecyclerAdapter;
import de.mrapp.android.tabswitcher.layout.AbstractTabSwitcherLayout;
import de.mrapp.android.tabswitcher.layout.AbstractTabViewHolder;
import de.mrapp.android.tabswitcher.layout.Arithmetics;
import de.mrapp.android.tabswitcher.layout.Arithmetics.Axis;
import de.mrapp.android.tabswitcher.model.AbstractItem;
import de.mrapp.android.tabswitcher.model.AddTabItem;
import de.mrapp.android.tabswitcher.model.ItemComparator;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.tabswitcher.model.TabSwitcherStyle;
import de.mrapp.android.tabswitcher.model.Tag;
import de.mrapp.android.util.view.AbstractViewRecycler;
import de.mrapp.android.util.view.AttachedViewRecycler;
import de.mrapp.util.Condition;

import static de.mrapp.android.util.DisplayUtil.dpToPixels;

/**
 * A layout, which implements the functionality of a {@link TabSwitcher} on tablets.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletTabSwitcherLayout extends AbstractTabSwitcherLayout implements Tab.Callback {

    /**
     * A comparator, which allows to compare two instances of the class {@link AbstractItem}. The
     * item, which corresponds to the currently selected tab, is always sorted before all other
     * items.
     */
    private static class TabletItemComparator extends ItemComparator {

        /**
         * Creates a new comparator, which allows to compare two instances of the class {@link
         * AbstractItem}. The item, which corresponds to the currently selected tab, is always
         * sorted before all other items.
         *
         * @param tabSwitcher
         *         The tab switcher, the items, which should be compared by the comparator, belong
         *         to, as a instance of the class {@link TabSwitcher}. The tab switcher may not be
         *         null
         */
        TabletItemComparator(@NonNull final TabSwitcher tabSwitcher) {
            super(tabSwitcher);
        }

        @Override
        public int compare(final AbstractItem o1, final AbstractItem o2) {
            if (o1 instanceof AddTabItem && o2 instanceof AddTabItem) {
                return 0;
            } else if (o1 instanceof AddTabItem) {
                return -1;
            } else if (o2 instanceof AddTabItem) {
                return 1;
            } else {
                TabItem tabItem1 = (TabItem) o1;
                TabItem tabItem2 = (TabItem) o2;
                Tab tab1 = tabItem1.getTab();
                Tab tab2 = tabItem2.getTab();
                int index1 = getTabSwitcher().indexOf(tab1);
                index1 = index1 == -1 ? o1.getIndex() : index1;
                int index2 = getTabSwitcher().indexOf(tab2);
                index2 = index2 == -1 ? o2.getIndex() : index2;
                Condition.INSTANCE.ensureNotEqual(index1, -1,
                        "Tab " + tab1 + " not contained by tab switcher", RuntimeException.class);
                Condition.INSTANCE.ensureNotEqual(index2, -1,
                        "Tab " + tab2 + " not contained by tab switcher", RuntimeException.class);
                int selectedTabIndex = getTabSwitcher().getSelectedTabIndex();

                if (index1 < selectedTabIndex) {
                    if (index2 == selectedTabIndex) {
                        return 1;
                    } else if (index2 > selectedTabIndex) {
                        return -1;
                    } else {
                        return index1 >= index2 ? -1 : 1;
                    }
                } else if (index1 > selectedTabIndex) {
                    if (index2 == selectedTabIndex) {
                        return 1;
                    } else if (index2 > selectedTabIndex) {
                        return index1 < index2 ? -1 : 1;
                    } else {
                        return 1;
                    }
                } else {
                    return -1;
                }
            }
        }
    }

    /**
     * The number of tabs, which are contained by a stack.
     */
    private final int stackedTabCount;

    /**
     * The maximum width of a tab in pixels.
     */
    private final int maxTabWidth;

    /**
     * The minimum width of a tab in pixels.
     */
    private final int minTabWidth;

    /**
     * The offset between two neighboring tabs in pixels.
     */
    private final int tabOffset;

    /**
     * The width of a button, which allows to add a new tab.
     */
    private final int addTabButtonWidth;

    /**
     * The offset between a button, which allows to add a new tab, and a neighboring tab.
     */
    private final int addTabButtonOffset;

    /**
     * The distance between two neighboring tabs when being swiped in pixels.
     */
    private final int swipedTabDistance;

    /**
     * The duration of the animation, which is used to show or hide the close button of tabs.
     */
    private final long closeButtonVisibilityAnimationDuration;

    /**
     * The drag handler, which is used by the layout.
     */
    private TabletDragTabsEventHandler dragHandler;

    /**
     * The view recycler, which allows to recycle the views, which are associated with tabs.
     */
    private AttachedViewRecycler<Tab, Void> contentViewRecycler;

    /**
     * The adapter, which allows to inflate the views, which are used to visualize tabs.
     */
    private TabletTabRecyclerAdapter tabRecyclerAdapter;

    /**
     * The view recycler, which allows to recycle the views, which are used to visualize tabs.
     */
    private AttachedViewRecycler<AbstractItem, Integer> tabViewRecycler;

    /**
     * The layout's primary toolbar.
     */
    private Toolbar primaryToolbar;

    /**
     * The layout's secondary toolbar.
     */
    private Toolbar secondaryToolbar;

    /**
     * The view group, which contains the tab switcher's tabs.
     */
    private ViewGroup tabContainer;

    /**
     * The view, which is used to display a border below the tab switcher's tabs.
     */
    private View borderView;

    /**
     * Adapts the margins of the tab container and the toolbars.
     */
    private void adaptTabContainerAndToolbarMargins() {
        FrameLayout.LayoutParams tabContainerLayoutParams =
                (FrameLayout.LayoutParams) tabContainer.getLayoutParams();
        tabContainerLayoutParams.setMargins(getModel().getPaddingLeft(), getModel().getPaddingTop(),
                getModel().getPaddingRight(), 0);
        FrameLayout.LayoutParams primaryToolbarLayoutParams =
                (FrameLayout.LayoutParams) primaryToolbar.getLayoutParams();
        primaryToolbarLayoutParams
                .setMargins(getModel().getPaddingLeft(), 0, getModel().getPaddingRight(), 0);
        FrameLayout.LayoutParams secondaryToolbarLayoutParams =
                (FrameLayout.LayoutParams) secondaryToolbar.getLayoutParams();
        secondaryToolbarLayoutParams
                .setMargins(getModel().getPaddingLeft(), 0, getModel().getPaddingRight(), 0);
    }

    /**
     * Adapts the color of the border, which is shown below the tab switcher's tabs.
     */
    private void adaptBorderColor() {
        Tab selectedTab = getModel().getSelectedTab();
        ColorStateList colorStateList = getStyle().getTabBackgroundColor(selectedTab);
        int[] stateSet = new int[]{android.R.attr.state_selected};
        int color = colorStateList.getColorForState(stateSet, colorStateList.getDefaultColor());
        Drawable background = borderView.getBackground();
        background.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }

    /**
     * Adapts the visibility of the close button of a specific tab.
     *
     * @param tabItem
     *         The item, which corresponds to the tab, whose close button should be adapted, as an
     *         instance of the class {@link TabItem}. The item may not be null
     */
    private void adaptCloseButtonVisibility(@NonNull final TabItem tabItem) {
        if (tabItem.getIndex() > (getModel().isAddTabButtonShown() ? 1 : 0)) {
            TabItem predecessor = TabItem.create(getModel(), getTabViewRecycler(),
                    tabItem.getIndex() - (getModel().isAddTabButtonShown() ? 2 : 1));
            AbstractTabViewHolder viewHolder = tabItem.getViewHolder();

            if (predecessor.isInflated()) {
                adaptCloseButtonVisibility(viewHolder, tabItem, predecessor);
            } else {
                animateCloseButtonVisibility(viewHolder, true);
            }
        }
    }

    /**
     * Adapts the visibility of the close button of a specific tab's predecessor.
     *
     * @param tabItem
     *         The item, which corresponds to the tab, whose predecessor's close button should be
     *         adapted, as an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptPredecessorPaddingAndCloseButtonVisibility(@NonNull final TabItem tabItem) {
        if (tabItem.getIndex() > (getModel().isAddTabButtonShown() ? 1 : 0)) {
            TabItem predecessor = TabItem.create(getModel(), getTabViewRecycler(),
                    tabItem.getIndex() - (getModel().isAddTabButtonShown() ? 2 : 1));

            if (predecessor.isInflated()) {
                AbstractTabViewHolder viewHolder = predecessor.getViewHolder();
                float offset = adaptCloseButtonVisibility(viewHolder, tabItem, predecessor);
                View titleContainer = viewHolder.titleContainer;
                titleContainer.setPadding(Math.round(offset), 0, 0, 0);
            }
        }
    }

    /**
     * Adapts the visibility of the close button of a specific tab, depending on the positions of
     * two successive tabs.
     *
     * @param viewHolder
     *         The view holder, which holds a reference to the close button, whose visibility should
     *         be adapted, as an instance of the class {@link AbstractTabViewHolder}. The view
     *         holder may not be null
     * @param successor
     *         The item, which corresponds to the rear one of both successive tabs, as an item of
     *         the class {@link AbstractItem}. The item may not be null
     * @param predecessor
     *         The item, which corresponds to the front one of both successive tabs, as an item of
     *         the class {@link AbstractItem}. The item may not be null
     * @return The offset between the two successive tabs in pixels as a {@link Float} value
     */
    private float adaptCloseButtonVisibility(@NonNull final AbstractTabViewHolder viewHolder,
                                             @NonNull final AbstractItem successor,
                                             @NonNull final AbstractItem predecessor) {
        float predecessorPosition = getArithmetics().getPosition(Axis.DRAGGING_AXIS, predecessor);
        float successorPosition = getArithmetics().getPosition(Axis.DRAGGING_AXIS, successor);
        float successorWidth = getArithmetics().getSize(Axis.DRAGGING_AXIS, successor);
        float offset = successorPosition + successorWidth - tabOffset - predecessorPosition;
        animateCloseButtonVisibility(viewHolder, offset <= dpToPixels(getContext(), 8));
        return offset;
    }

    /**
     * Animates the visibility of a tab's close button.
     *
     * @param viewHolder
     *         The view holder, which holds a reference to the close button, whose visibility should
     *         be animated, as an instance of the class {@link AbstractTabViewHolder}. The view
     *         holder may not be null
     * @param show
     *         True, if the close button should be shown, false otherwise
     */
    private void animateCloseButtonVisibility(@NonNull final AbstractTabViewHolder viewHolder,
                                              final boolean show) {
        ImageButton closeButton = viewHolder.closeButton;
        Boolean visible = (Boolean) closeButton.getTag(R.id.tag_visibility);

        if (visible == null || visible != show) {
            closeButton.setTag(R.id.tag_visibility, show);

            if (closeButton.getAnimation() != null) {
                closeButton.getAnimation().cancel();
            }

            ViewPropertyAnimator animation = closeButton.animate();
            animation.setListener(createCloseButtonVisibilityAnimationListener(viewHolder, show));
            animation.alpha(show ? 1 : 0);
            animation.setStartDelay(0);
            animation.setDuration(closeButtonVisibilityAnimationDuration);
            animation.start();
        }
    }

    /**
     * Creates and returns a listener, which allows to observe the progress of the animation, which
     * is used to animate the visibility of a tab's close button.
     *
     * @param viewHolder
     *         The view holder, which holds a reference to the close button, whose visibility is
     *         animated, as an instance of the class {@link AbstractTabViewHolder}. The view holder
     *         may not be null
     * @param show
     *         True, if the animation shows the close button, false otherwise
     * @return The listener, which has been created, as an instance of the class {@link
     * AnimatorListener}. The listener may not be null
     */
    @NonNull
    private AnimatorListener createCloseButtonVisibilityAnimationListener(
            @NonNull final AbstractTabViewHolder viewHolder, final boolean show) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                if (show) {
                    viewHolder.closeButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                if (!show) {
                    viewHolder.closeButton.setVisibility(View.GONE);
                }
            }

        };
    }

    /**
     * Inflates the content, which is associated with a specific tab.
     *
     * @param tab
     *         The tab, whose content should be inflated, as an instance of the class {@link Tab}.
     *         The tab may not be null
     * @param listener
     *         The layout listener, which should be notified, when the view has been inflated, as an
     *         instance of the type {@link OnGlobalLayoutListener} or null, if no listener should be
     *         notified
     */
    private void inflateContent(@NonNull final Tab tab,
                                @Nullable final OnGlobalLayoutListener listener) {
        Pair<View, Boolean> pair = contentViewRecycler.inflate(tab);
        View view = pair.first;

        if (listener != null) {
            boolean inflated = pair.second;

            if (inflated) {
                view.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new LayoutListenerWrapper(view, listener));
            } else {
                listener.onGlobalLayout();
            }
        }
    }

    /**
     * Calculates and returns the width of the tabs, depending on the total number of tabs, which
     * are currently contained by the tab switcher.
     *
     * @return The width, which has been calculated, in pixels as an {@link Integer} value
     */
    private int calculateTabWidth() {
        return maxTabWidth;
    }

    /**
     * Calculates and returns the space between two neighboring tabs.
     *
     * @return The space, which has been calculated, in pixels as an {@link Integer} value
     */
    private int calculateTabSpacing() {
        return calculateTabWidth() - tabOffset;
    }

    /**
     * Calculates and returns the space between a button, which allows to add a new tab, and a
     * neighboring tab.
     *
     * @return The space, which has been calculated, in pixels as an {@link Integer} value
     */
    private int calculateAddTabButtonSpacing() {
        return getModel().isAddTabButtonShown() ? addTabButtonWidth + addTabButtonOffset : 0;
    }

    /**
     * Returns, whether the tab container is large enough to take up all tabs, or not.
     *
     * @return True, if the tab container is large enough to take up all tabs, false otherwise
     */
    private boolean areTabsFittingIntoTabContainer() {
        int thresholdPosition = getModel().getCount() * calculateTabSpacing() +
                (getModel().isAddTabButtonShown() ? calculateAddTabButtonSpacing() : 0);
        float tabContainerSize = getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false);
        return thresholdPosition <= tabContainerSize;
    }

    /**
     * Calculates and returns the items when the tab switcher is shown initially.
     *
     * @param referenceTabIndex
     *         The index of the tab, which is used as a reference, when restoring the positions of
     *         tabs, as an {@link Integer} value or -1, if the positions of tabs should not be
     *         restored
     * @param referenceTabPosition
     *         The position of tab, which is used as a reference, when restoring the positions of
     *         tabs, in relation to the available space as a {@link Float} value or -1, if the
     *         positions of tabs should not be restored
     * @return An array, which contains the tab items, as an array of the type {@link AbstractItem}.
     * The array may not be null
     */
    @NonNull
    private AbstractItem[] calculateInitialItems(final int referenceTabIndex,
                                                 final float referenceTabPosition) {
        dragHandler.reset();
        setFirstVisibleIndex(-1);
        AbstractItem[] items = new AbstractItem[getItemCount()];

        if (items.length > 0) {

            if (referenceTabIndex != -1 && referenceTabPosition != -1) {
                float referencePosition = referenceTabPosition *
                        getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false);
                referencePosition = Math.max(calculateMinStartPosition(referenceTabIndex),
                        Math.min(calculateMaxEndPosition(referenceTabIndex), referencePosition));
                AbstractItemIterator iterator =
                        new InitialItemIteratorBuilder(items).start(referenceTabIndex).create();
                AbstractItem item;

                while ((item = iterator.next()) != null) {
                    float position;

                    if (item.getIndex() == referenceTabIndex) {
                        position = referencePosition;
                    } else {
                        position = referencePosition -
                                (calculateTabSpacing() * (item.getIndex() - referenceTabIndex));
                    }

                    Pair<Float, State> pair =
                            clipPosition(item.getIndex(), position, iterator.previous());
                    item.getTag().setPosition(pair.first);
                    item.getTag().setState(pair.second);

                    if (item.getIndex() == referenceTabIndex) {
                        referencePosition = pair.first;
                    }

                    if (getFirstVisibleIndex() == -1 && pair.second == State.FLOATING) {
                        setFirstVisibleIndex(item.getIndex());
                    }
                }

                iterator = new InitialItemIteratorBuilder(items).create();
                int firstVisibleIndex = -1;

                while ((item = iterator.next()) != null && item.getIndex() < referenceTabIndex) {
                    float position;

                    if (item instanceof AddTabItem) {
                        position = calculateMaxEndPosition(item.getIndex());
                    } else {
                        position = referencePosition +
                                (calculateTabSpacing() * (referenceTabIndex - item.getIndex()));
                    }

                    Pair<Float, State> pair =
                            clipPosition(item.getIndex(), position, iterator.previous());
                    item.getTag().setPosition(pair.first);
                    item.getTag().setState(pair.second);

                    if (firstVisibleIndex == -1 && pair.second == State.FLOATING) {
                        firstVisibleIndex = item.getIndex();
                    }
                }

                if (firstVisibleIndex != -1) {
                    setFirstVisibleIndex(firstVisibleIndex);
                }
            } else {
                AbstractItemIterator iterator =
                        new InitialItemIteratorBuilder(items).start(0).create();
                AbstractItem item;

                while ((item = iterator.next()) != null) {
                    AbstractItem predecessor = iterator.previous();
                    float position = calculateMaxEndPosition(item.getIndex());
                    Pair<Float, State> pair = clipPosition(item.getIndex(), position, predecessor);
                    item.getTag().setPosition(pair.first);
                    item.getTag().setState(pair.second);

                    if (getFirstVisibleIndex() == -1 && pair.second == State.FLOATING) {
                        setFirstVisibleIndex(item.getIndex());
                    }
                }

            }

            secondLayoutPass(new InitialItemIteratorBuilder(items).start(0));
        }

        dragHandler.setCallback(this);
        return items;
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
        View view = contentViewRecycler.getView(tabItem.getTab());

        if (view != null) {
            float currentPosition = view.getX();
            float distance = Math.abs(targetPosition - currentPosition);
            float maxDistance = getTabSwitcher().getWidth() + swipedTabDistance;
            long duration = velocity > 0 ? Math.round((distance / velocity) * 1000) :
                    Math.round(animationDuration * (distance / maxDistance));
            ViewPropertyAnimator animation = view.animate();
            animation.setListener(new AnimationListenerWrapper(
                    selected ? createSwipeSelectedTabAnimationListener(tabItem) :
                            createSwipeNeighborAnimationListener(tabItem)));
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            animation.setDuration(duration);
            animation.setStartDelay(0);
            animation.x(targetPosition);
            animation.start();
        }
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
                contentViewRecycler.remove(tabItem.getTab());
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to adapt the size and position of an
     * item, once its view has been inflated.
     *
     * @param item
     *         The item, whose view should be adapted, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
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
                updateView(item, dragging);

                if (layoutListener != null) {
                    layoutListener.onGlobalLayout();
                }
            }

        };
    }

    /**
     * Creates and returns a layout listener, which allows to position the content of a tab, after
     * it has been inflated.
     *
     * @param tab
     *         The tab, whose content has been inflated, as an instance of the class {@link Tab}.
     *         The tab may not be null
     * @return The layout listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createContentLayoutListener(@NonNull final Tab tab) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                View view = contentViewRecycler.getView(tab);

                if (view != null) {
                    view.setX(0);
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
                View view = contentViewRecycler.getView(neighbor.getTab());

                if (view != null) {
                    float position;

                    if (dragDistance > 0) {
                        position = -getTabSwitcher().getWidth() + dragDistance - swipedTabDistance;
                    } else {
                        position = getTabSwitcher().getWidth() + dragDistance + swipedTabDistance;
                    }

                    view.setX(position);
                }
            }

        };
    }

    /**
     * Creates a new layout, which implements the functionality of a {@link TabSwitcher} on
     * tablets.
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
    public TabletTabSwitcherLayout(@NonNull final TabSwitcher tabSwitcher,
                                   @NonNull final TabSwitcherModel model,
                                   @NonNull final Arithmetics arithmetics,
                                   @NonNull final TabSwitcherStyle style,
                                   @NonNull final TouchEventDispatcher touchEventDispatcher) {
        super(tabSwitcher, model, arithmetics, style, touchEventDispatcher);
        Resources resources = tabSwitcher.getResources();
        stackedTabCount = resources.getInteger(R.integer.tablet_stacked_tab_count);
        maxTabWidth = resources.getDimensionPixelSize(R.dimen.tablet_tab_max_width);
        minTabWidth = resources.getDimensionPixelSize(R.dimen.tablet_tab_min_width);
        tabOffset = resources.getDimensionPixelSize(R.dimen.tablet_tab_offset);
        addTabButtonWidth = resources.getDimensionPixelSize(R.dimen.tablet_add_tab_button_width);
        addTabButtonOffset = resources.getDimensionPixelSize(R.dimen.tablet_add_tab_button_offset);
        swipedTabDistance = resources.getDimensionPixelSize(R.dimen.swiped_tab_distance);
        closeButtonVisibilityAnimationDuration =
                resources.getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public final AbstractDragTabsEventHandler<?> getDragHandler() {
        return dragHandler;
    }

    @Override
    protected final void onInflateLayout(@NonNull final LayoutInflater inflater,
                                         final boolean tabsOnly) {
        if (!tabsOnly) {
            inflater.inflate(R.layout.tablet_layout, getTabSwitcher(), true);
        }

        primaryToolbar = getTabSwitcher().findViewById(R.id.primary_toolbar);
        secondaryToolbar = getTabSwitcher().findViewById(R.id.secondary_toolbar);
        tabContainer = getTabSwitcher().findViewById(R.id.tab_container);
        borderView = getTabSwitcher().findViewById(R.id.border_view);
        ViewGroup contentContainer = getTabSwitcher().findViewById(R.id.content_container);
        contentViewRecycler = new AttachedViewRecycler<>(contentContainer, inflater);
        tabRecyclerAdapter = new TabletTabRecyclerAdapter(getTabSwitcher(), getModel(), getStyle());
        getModel().addListener(tabRecyclerAdapter);
        tabViewRecycler = new AttachedViewRecycler<>(tabContainer, inflater,
                Collections.reverseOrder(new TabletItemComparator(getTabSwitcher())));
        tabViewRecycler.setAdapter(tabRecyclerAdapter);
        tabRecyclerAdapter.setViewRecycler(tabViewRecycler);
        dragHandler =
                new TabletDragTabsEventHandler(getTabSwitcher(), getArithmetics(), tabViewRecycler);
        adaptTabContainerAndToolbarMargins();
        adaptBorderColor();
    }

    @Nullable
    @Override
    protected final Pair<Integer, Float> onDetachLayout(final boolean tabsOnly) {
        // TODO: contentViewRecycler.removeAll();
        // TODO: contentViewRecycler.clearCache();
        Pair<Integer, Float> result = null;
        ItemIterator iterator =
                new ItemIterator.Builder(getModel(), getTabViewRecycler()).start(getItemCount() - 1)
                        .reverse(true).create();
        AbstractItem item;

        if (!areTabsFittingIntoTabContainer()) {
            while ((item = iterator.next()) != null) {
                if (item.getTag().getState() == State.FLOATING) {
                    float position = item.getTag().getPosition();
                    float tabContainerSize =
                            getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false);
                    result = Pair.create(item.getIndex(), position / tabContainerSize);
                    break;
                }
            }
        }

        if (!tabsOnly) {
            getModel().removeListener(tabRecyclerAdapter);
            AttachedViewRecycler.Adapter<Tab, Void> contentViewRecyclerAdapter =
                    contentViewRecycler.getAdapter();

            if (contentViewRecyclerAdapter instanceof TabletContentRecyclerAdapterWrapper) {
                getModel().removeListener(
                        (TabletContentRecyclerAdapterWrapper) contentViewRecyclerAdapter);
            }
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
    protected final void inflateAndUpdateView(@NonNull final AbstractItem item,
                                              final boolean dragging,
                                              @Nullable final OnGlobalLayoutListener listener) {
        inflateView(item, createInflateViewLayoutListener(item, dragging, listener));
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
        int selectedItemIndex =
                getModel().getSelectedTabIndex() + (getModel().isAddTabButtonShown() ? 1 : 0);
        float position;
        State state;

        if (index == 0 && getModel().isAddTabButtonShown()) {
            position = getStackedTabSpacing() * Math.min(count - 2, getStackedTabCount()) +
                    calculateTabSpacing() + addTabButtonOffset;
            state = State.FLOATING;
        } else if (index == selectedItemIndex) {
            position = getStackedTabSpacing() * Math.min(count - (index + 1), getStackedTabCount());
            state = State.STACKED_START_ATOP;
        } else if (index < selectedItemIndex) {
            if ((selectedItemIndex - index) < getStackedTabCount()) {
                position = (getStackedTabSpacing() *
                        Math.min(count - (selectedItemIndex + 1), getStackedTabCount())) +
                        (getStackedTabSpacing() * (selectedItemIndex - index));
                state = State.STACKED_END;
            } else {
                position = (getStackedTabSpacing() *
                        Math.min(count - (selectedItemIndex + 1), getStackedTabCount())) +
                        (getStackedTabSpacing() * getStackedTabCount());
                state = State.HIDDEN;
            }
        } else {
            if ((count - index) <= getStackedTabCount()) {
                position = getStackedTabSpacing() * (count - (index + 1));
                state = predecessorState == null || predecessorState == State.FLOATING ?
                        State.STACKED_START_ATOP : State.STACKED_START;
            } else {
                position = getStackedTabSpacing() * getStackedTabCount();
                state = predecessorState == null || predecessorState == State.FLOATING ?
                        State.STACKED_START_ATOP : State.HIDDEN;
            }
        }

        return Pair.create(position, state);
    }

    @NonNull
    @Override
    protected final Pair<Float, State> calculatePositionAndStateWhenStackedAtEnd(final int index) {
        float tabContainerWidth = getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false);
        int selectedTabIndex = getModel().getSelectedTabIndex();
        int selectedItemIndex = selectedTabIndex + (getModel().isAddTabButtonShown() ? 1 : 0);
        int i = getModel().isAddTabButtonShown() ? index - 1 : index;
        float position;
        State state;

        if (index == 0 && getModel().isAddTabButtonShown()) {
            position = tabContainerWidth - addTabButtonWidth;
            state = State.STACKED_END;
        } else if (index == selectedItemIndex) {
            position = tabContainerWidth - calculateAddTabButtonSpacing() - calculateTabSpacing() -
                    (getStackedTabSpacing() * Math.min(getStackedTabCount(), i));
            state = State.STACKED_END;
        } else if (index < selectedItemIndex) {
            if (i < getStackedTabCount()) {
                position =
                        tabContainerWidth - calculateAddTabButtonSpacing() - calculateTabSpacing() -
                                (getStackedTabSpacing() * i);
                state = State.STACKED_END;
            } else {
                position =
                        tabContainerWidth - calculateAddTabButtonSpacing() - calculateTabSpacing() -
                                (getStackedTabSpacing() * getStackedTabCount());
                state = State.STACKED_END;
            }
        } else {
            float selectedItemPosition =
                    tabContainerWidth - calculateAddTabButtonSpacing() - calculateTabSpacing() -
                            (getStackedTabSpacing() *
                                    Math.min(getStackedTabCount(), selectedTabIndex));

            if (index <= selectedItemIndex + getStackedTabCount()) {
                position = selectedItemPosition -
                        (getStackedTabSpacing() * (index - selectedItemIndex));
                state = State.STACKED_END;
            } else {
                position = selectedItemPosition - (getStackedTabSpacing() * getStackedTabCount());
                state = State.HIDDEN;
            }
        }

        return Pair.create(position, state);
    }

    @Override
    protected final float calculateMinStartPosition(final int index) {
        if (areTabsFittingIntoTabContainer()) {
            return calculateMaxEndPosition(index);
        } else {
            float tabContainerSize =
                    getArithmetics().getTabContainerSize(Axis.DRAGGING_AXIS, false);

            if (index == 0 && getModel().isAddTabButtonShown()) {
                return tabContainerSize - addTabButtonWidth;
            } else {
                int i = getModel().isAddTabButtonShown() ? index : index + 1;
                return tabContainerSize - calculateAddTabButtonSpacing() -
                        (calculateTabSpacing() * i);
            }
        }
    }

    @Override
    protected final float calculateMaxEndPosition(final int index) {
        if (index == 0 && getModel().isAddTabButtonShown()) {
            return getModel().getCount() * calculateTabSpacing() + addTabButtonOffset;
        } else {
            int i = getModel().isAddTabButtonShown() ? index : index + 1;
            return (getModel().getCount() - i) * calculateTabSpacing();
        }
    }

    @Override
    protected final float calculateSuccessorPosition(@NonNull final AbstractItem item,
                                                     @NonNull final AbstractItem predecessor) {
        float predecessorPosition = predecessor.getTag().getPosition();

        if (predecessor instanceof AddTabItem) {
            return predecessorPosition - addTabButtonOffset - calculateTabSpacing();
        } else {
            return predecessorPosition - calculateTabSpacing();
        }
    }

    @Override
    protected final float calculatePredecessorPosition(@NonNull final AbstractItem item,
                                                       @NonNull final AbstractItem successor) {
        float successorPosition = successor.getTag().getPosition();
        return successorPosition + calculateTabSpacing();
    }

    @Override
    protected final void secondLayoutPass(
            @NonNull final AbstractItemIterator.AbstractBuilder builder) {
        int selectedItemIndex =
                getModel().getSelectedTabIndex() + (getModel().isAddTabButtonShown() ? 1 : 0);
        int stackedTabCount = getStackedTabCount() + (getModel().isAddTabButtonShown() ? 1 : 0);
        AbstractItemIterator iterator =
                builder.start(getModel().isAddTabButtonShown() ? 1 : 0).create();
        AbstractItem item;

        while ((item = iterator.next()) != null && item.getIndex() < selectedItemIndex) {
            Tag tag = item.getTag();

            if (tag.getState() == State.STACKED_END && item.getIndex() >= stackedTabCount) {
                AbstractItem successor = iterator.peek();

                if (successor != null && successor.getTag().getState() == State.STACKED_END) {
                    tag.setState(State.HIDDEN);
                    inflateOrRemoveView(item, false);
                }
            } else if (tag.getState() == State.FLOATING) {
                return;
            }
        }
    }

    @NonNull
    @Override
    protected final AttachedViewRecycler.Adapter<Tab, Void> onCreateContentRecyclerAdapter() {
        AttachedViewRecycler.Adapter<Tab, Void> adapter = contentViewRecycler.getAdapter();

        if (adapter instanceof TabletContentRecyclerAdapterWrapper) {
            getModel().removeListener((TabletContentRecyclerAdapterWrapper) adapter);
        }

        TabletContentRecyclerAdapterWrapper recyclerAdapter =
                new TabletContentRecyclerAdapterWrapper(getTabSwitcher(), getStyle(),
                        contentViewRecycler, getModel().getContentRecyclerAdapter());
        getModel().addListener(recyclerAdapter);
        return recyclerAdapter;
    }

    @Override
    public final void onGlobalLayout() {
        AbstractItem[] items = calculateInitialItems(getModel().getReferenceTabIndex(),
                getModel().getReferenceTabPosition());
        AbstractItemIterator iterator = new InitialItemIteratorBuilder(items).create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            if (item.isVisible()) {
                inflateAndUpdateView(item, true, null);
            }
        }

        Tab selectedTab = getModel().getSelectedTab();

        if (selectedTab != null) {
            inflateContent(selectedTab, createContentLayoutListener(selectedTab));
        }
    }

    @Nullable
    @Override
    public final ViewGroup getTabContainer() {
        return tabContainer;
    }

    @Nullable
    @Override
    public final Toolbar[] getToolbars() {
        return primaryToolbar != null && secondaryToolbar != null ?
                new Toolbar[]{primaryToolbar, secondaryToolbar} : null;
    }

    @Override
    public final void onSelectionChanged(final int previousIndex, final int index,
                                         @Nullable final Tab selectedTab,
                                         final boolean switcherHidden) {
        contentViewRecycler.removeAll();

        if (selectedTab != null) {
            inflateContent(selectedTab, createContentLayoutListener(selectedTab));
            tabViewRecycler.setComparator(
                    Collections.reverseOrder(new TabletItemComparator(getTabSwitcher())));
            int tabSpacing = calculateTabSpacing();
            int previousSelectedItemIndex = previousIndex +
                    (previousIndex != -1 && getModel().isAddTabButtonShown() ? 1 : 0);
            int selectedItemIndex = index + (getModel().isAddTabButtonShown() ? 1 : 0);
            TabItem selectedTabItem = TabItem.create(getModel(), tabViewRecycler, index);
            float referencePosition;

            if (selectedTabItem.isInflated()) {
                referencePosition = selectedTabItem.getTag().getPosition();
            } else if (isStackedAtStart(selectedTabItem.getIndex())) {
                Pair<Float, State> pair =
                        calculatePositionAndStateWhenStackedAtStart(getModel().getCount(),
                                selectedTabItem.getIndex(), selectedTabItem);
                referencePosition = pair.first;
            } else {
                Pair<Float, State> pair =
                        calculatePositionAndStateWhenStackedAtEnd(selectedTabItem.getIndex());
                referencePosition = pair.first;
            }

            AbstractItemIterator iterator =
                    new ItemIterator.Builder(getModel(), getTabViewRecycler())
                            .start(getModel().isAddTabButtonShown() ? 1 : 0).create();
            AbstractItem item;

            while ((item = iterator.next()) != null) {
                float position = -1;

                if (item.getIndex() == selectedItemIndex) {
                    position = referencePosition;
                } else if (item.getIndex() < selectedItemIndex) {
                    position = referencePosition +
                            ((selectedItemIndex - item.getIndex()) * tabSpacing);
                } else if (item.getIndex() == previousSelectedItemIndex) {
                    position = item.getTag().getPosition();
                } else if (item.getTag().getState() == State.STACKED_END ||
                        (item.getTag().getState() == State.HIDDEN &&
                                item.getIndex() < (selectedItemIndex + getStackedTabCount()))) {
                    position = referencePosition;
                }

                if (position != -1) {
                    Pair<Float, State> pair =
                            clipPosition(item.getIndex(), position, iterator.previous());
                    item.getTag().setPosition(pair.first);
                    item.getTag().setState(pair.second);
                    inflateOrRemoveView(item, false);
                }
            }

            secondLayoutPass(new ItemIterator.Builder(getTabSwitcher(), getTabViewRecycler()));
        }
    }

    @Override
    protected final void updateView(@NonNull final AbstractItem item, final boolean dragging) {
        super.updateView(item, dragging);

        if (dragging && item instanceof TabItem && item.isInflated()) {
            TabItem tabItem = (TabItem) item;

            if (tabItem.getTab().isCloseable()) {
                int selectedItemIndex = getModel().getSelectedTabIndex() +
                        (getModel().isAddTabButtonShown() ? 1 : 0);

                if (item.getIndex() == selectedItemIndex) {
                    animateCloseButtonVisibility(tabItem.getViewHolder(), true);
                    adaptPredecessorPaddingAndCloseButtonVisibility(tabItem);
                } else if (item.getIndex() >= selectedItemIndex) {
                    adaptCloseButtonVisibility(tabItem);
                } else {
                    adaptPredecessorPaddingAndCloseButtonVisibility(tabItem);
                }
            }
        }
    }

    @Override
    public final void onTabAdded(final int index, @NonNull final Tab tab,
                                 final int previousSelectedTabIndex, final int selectedTabIndex,
                                 final boolean selectionChanged,
                                 final boolean switcherVisibilityChanged,
                                 @NonNull final Animation animation) {
        tab.addCallback(this);
        // TODO: Implement
    }

    @Override
    public final void onAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                     final int previousSelectedTabIndex, final int selectedTabIndex,
                                     final boolean selectionChanged,
                                     @NonNull final Animation animation) {
        for (Tab tab : tabs) {
            tab.addCallback(this);
        }
        // TODO: Implement
    }

    @Override
    public final void onTabRemoved(final int index, @NonNull final Tab tab,
                                   final int previousSelectedTabIndex, final int selectedTabIndex,
                                   final boolean selectionChanged,
                                   @NonNull final Animation animation) {
        tab.removeCallback(this);
        // TODO: Implement
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final Tab[] tabs,
                                       @NonNull final Animation animation) {
        for (Tab tab : tabs) {
            tab.removeCallback(this);
        }
        // TODO: Implement
    }

    @Override
    public final void onPaddingChanged(final int left, final int top, final int right,
                                       final int bottom) {
        adaptTabContainerAndToolbarMargins();
    }

    @Override
    public final void onApplyPaddingToTabsChanged(final boolean applyPaddingToTabs) {

    }

    @Override
    public void onTabBackgroundColorChanged(@Nullable final ColorStateList colorStateList) {
        adaptBorderColor();
    }

    @Override
    public final void onAddTabButtonVisibilityChanged(final boolean visible) {
        // TODO: Implement
    }

    @Override
    public final void onEmptyViewChanged(@Nullable final View view, final long animationDuration) {
        if (getModel().isEmpty()) {
            // TODO: Adapt empty view
        }
    }

    @Override
    public final void onSwitchingBetweenTabs(final int selectedTabIndex, final float distance) {
        TabItem tabItem = TabItem.create(getModel(), getTabViewRecycler(), selectedTabIndex);
        View view = contentViewRecycler.getView(tabItem.getTab());

        if (view != null) {
            if (distance == 0 || (distance > 0 && selectedTabIndex < getModel().getCount() - 1) ||
                    (distance < 0 && selectedTabIndex > 0)) {
                view.setX(distance);

                if (distance != 0) {
                    TabItem neighbor = TabItem.create(getModel(), getTabViewRecycler(),
                            distance > 0 ? selectedTabIndex + 1 : selectedTabIndex - 1);

                    if (Math.abs(distance) >= swipedTabDistance) {
                        inflateContent(neighbor.getTab(),
                                createSwipeNeighborLayoutListener(neighbor, distance));
                    } else {
                        contentViewRecycler.remove(neighbor.getTab());
                    }
                }
            } else {
                float position = (float) Math.pow(Math.abs(distance), 0.75);
                position = distance < 0 ? position * -1 : position;
                view.setX(position);
            }

            getLogger().logVerbose(getClass(),
                    "Swiping content of tab at index " + selectedTabIndex +
                            ". Current swipe distance is " + distance + " pixels");
        }
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
        } else {
            View view = contentViewRecycler.getView(selectedTabItem.getTab());

            if (view != null) {
                if (view.getX() > 0) {
                    if (selectedTabIndex + 1 < getModel().getCount()) {
                        neighbor = TabItem.create(getModel(), getTabViewRecycler(),
                                selectedTabIndex + 1);
                        left = true;
                    }
                } else {
                    if (selectedTabIndex - 1 >= 0) {
                        neighbor = TabItem.create(getModel(), getTabViewRecycler(),
                                selectedTabIndex - 1);
                        left = false;
                    }
                }
            }
        }

        if (neighbor != null) {
            View neighborView = contentViewRecycler.getView(neighbor.getTab());

            if (neighborView != null) {
                float width = neighborView.getWidth();
                float targetPosition =
                        left ? (width + swipedTabDistance) * -1 : width + swipedTabDistance;
                animateSwipe(neighbor, targetPosition, false, animationDuration, velocity);
            }
        }
    }

    @Override
    public final void onTitleChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onIconChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onCloseableChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onCloseButtonIconChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onBackgroundColorChanged(@NonNull final Tab tab) {
        if (getModel().getSelectedTab() == tab) {
            adaptBorderColor();
        }
    }

    @Override
    public final void onContentBackgroundColorChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onTitleTextColorChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onProgressBarVisibilityChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onProgressBarColorChanged(@NonNull final Tab tab) {

    }

}