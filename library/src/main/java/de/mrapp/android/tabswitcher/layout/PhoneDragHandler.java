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

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.view.Gravity;
import android.view.View;

import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.arithmetic.Arithmetics;
import de.mrapp.android.tabswitcher.iterator.AbstractTabItemIterator;
import de.mrapp.android.tabswitcher.model.Axis;
import de.mrapp.android.tabswitcher.model.DragState;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.gesture.DragHelper;

import static de.mrapp.android.util.Condition.ensureGreater;
import static de.mrapp.android.util.Condition.ensureNotEqual;

/**
 * A drag handler, which allows to calculate the position and state of tabs on touch events, when
 * using the smartphone layout.
 *
 * @author Michael Rapp
 * @since 1.0.0
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

        /**
         * The method, which is invoked, when the position or state of a tab has been changed.
         *
         * @param tabItem
         *         The tab item, which corresponds to the tab, whose position or state has been
         *         changed, as an instance of the class {@link TabItem}. The tab item may not be
         *         null
         */
        void onViewStateChanged(@NonNull TabItem tabItem);

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
     * The space between tabs, which are part of a stack, in pixels.
     */
    private final int stackedTabSpacing;

    /**
     * The inset of tabs in pixels.
     */
    private final int tabInset;

    /**
     * The index of the first visible tab.
     */
    private int firstVisibleIndex;

    /**
     * The position on the dragging axis, where the distance between a tab and its predecessor
     * should have reached the maximum, in pixels.
     */
    private float attachedPosition;

    /**
     * The maximum space between neighboring tabs in pixels.
     */
    private float maxTabSpacing;

    /**
     * Calculates the positions of all tabs, when dragging towards the start.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     */
    private void calculatePositionsWhenDraggingToEnd(
            @NonNull final AbstractTabItemIterator.Factory factory, final float dragDistance) {
        firstVisibleIndex = -1;
        AbstractTabItemIterator.AbstractBuilder builder = factory.create();
        AbstractTabItemIterator iterator = builder.start(Math.max(0, firstVisibleIndex)).create();
        TabItem tabItem;
        boolean abort = false;

        while ((tabItem = iterator.next()) != null && !abort) {
            if (getTabSwitcher().getCount() - tabItem.getIndex() > 1) {
                abort = calculatePositionWhenDraggingToEnd(factory, dragDistance, tabItem,
                        iterator.previous());

                if (firstVisibleIndex == -1 && tabItem.getTag().getState() == State.FLOATING) {
                    firstVisibleIndex = tabItem.getIndex();
                }
            } else {
                Pair<Float, State> pair =
                        clipTabPosition(getTabSwitcher().getCount(), tabItem.getIndex(),
                                tabItem.getTag().getPosition(), iterator.previous());
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
            }

            notifyOnViewStateChanged(tabItem);
        }
    }

    /**
     * Calculates the position of a specific tab, when dragging towards the end.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose position should be calculated, as
     *         an instance of the class {@link TabItem}. The tab item may not be null
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     * @return True, if calculating the position of subsequent tabs can be omitted, false otherwise
     */
    private boolean calculatePositionWhenDraggingToEnd(
            @NonNull final AbstractTabItemIterator.Factory factory, final float dragDistance,
            @NonNull final TabItem tabItem, @Nullable final TabItem predecessor) {
        if (predecessor == null || predecessor.getTag().getState() != State.FLOATING) {
            if ((tabItem.getTag().getState() == State.STACKED_START_ATOP &&
                    tabItem.getIndex() == 0) || tabItem.getTag().getState() == State.FLOATING) {
                float currentPosition = tabItem.getTag().getPosition();
                float thresholdPosition = calculateEndPosition(factory, tabItem.getIndex());
                float newPosition = Math.min(currentPosition + dragDistance, thresholdPosition);
                Pair<Float, State> pair =
                        clipTabPosition(getTabSwitcher().getCount(), tabItem.getIndex(),
                                newPosition, predecessor);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
            } else if (tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                return true;
            }
        } else {
            float thresholdPosition = calculateEndPosition(factory, tabItem.getIndex());
            float newPosition =
                    Math.min(calculateNonLinearPosition(tabItem, predecessor), thresholdPosition);
            Pair<Float, State> pair =
                    clipTabPosition(getTabSwitcher().getCount(), tabItem.getIndex(), newPosition,
                            predecessor);
            tabItem.getTag().setPosition(pair.first);
            tabItem.getTag().setState(pair.second);
        }

        return false;
    }

    /**
     * Calculates the positions of all tabs, when dragging towards the end.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     */
    private void calculatePositionsWhenDraggingToStart(
            @NonNull final AbstractTabItemIterator.Factory factory, final float dragDistance) {
        AbstractTabItemIterator.AbstractBuilder builder = factory.create();
        AbstractTabItemIterator iterator = builder.start(Math.max(0, firstVisibleIndex)).create();
        TabItem tabItem;
        boolean abort = false;

        while ((tabItem = iterator.next()) != null && !abort) {
            if (getTabSwitcher().getCount() - tabItem.getIndex() > 1) {
                abort = calculatePositionWhenDraggingToStart(dragDistance, tabItem,
                        iterator.previous());
            } else {
                Pair<Float, State> pair =
                        clipTabPosition(getTabSwitcher().getCount(), tabItem.getIndex(),
                                tabItem.getTag().getPosition(), iterator.previous());
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
            }

            notifyOnViewStateChanged(tabItem);
        }

        if (firstVisibleIndex > 0) {
            int start = firstVisibleIndex - 1;
            iterator = builder.reverse(true).start(start).create();
            abort = false;

            while ((tabItem = iterator.next()) != null && !abort) {
                TabItem predecessor = iterator.previous();
                float predecessorPosition = predecessor.getTag().getPosition();
                float newPosition = predecessorPosition +
                        calculateMaxTabSpacing(getTabSwitcher().getCount(), predecessor);
                tabItem.getTag().setPosition(newPosition);

                if (tabItem.getIndex() < start) {
                    Pair<Float, State> pair =
                            clipTabPosition(getTabSwitcher().getCount(), predecessor.getIndex(),
                                    predecessor.getTag().getPosition(), tabItem);
                    predecessor.getTag().setPosition(pair.first);
                    predecessor.getTag().setState(pair.second);
                    notifyOnViewStateChanged(predecessor);

                    if (predecessor.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = predecessor.getIndex();
                    } else {
                        abort = true;
                    }
                }

                if (!iterator.hasNext()) {
                    Pair<Float, State> pair =
                            clipTabPosition(getTabSwitcher().getCount(), tabItem.getIndex(),
                                    newPosition, (TabItem) null);
                    tabItem.getTag().setPosition(pair.first);
                    tabItem.getTag().setState(pair.second);
                    notifyOnViewStateChanged(tabItem);

                    if (tabItem.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = tabItem.getIndex();
                    }
                }
            }
        }
    }

    /**
     * Calculates the position of a specific tab, when dragging towards the start.
     *
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose position should be calculated, as
     *         an instance of the class {@link TabItem}. The tab item may not be null
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     * @return True, if calculating the position of subsequent tabs can be omitted, false otherwise
     */
    private boolean calculatePositionWhenDraggingToStart(final float dragDistance,
                                                         @NonNull final TabItem tabItem,
                                                         @Nullable final TabItem predecessor) {
        if (predecessor == null || predecessor.getTag().getState() != State.FLOATING ||
                predecessor.getTag().getPosition() >
                        getAttachedPosition(false, getTabSwitcher().getCount())) {
            if (tabItem.getTag().getState() == State.FLOATING) {
                float currentPosition = tabItem.getTag().getPosition();
                float newPosition = currentPosition + dragDistance;
                Pair<Float, State> pair =
                        clipTabPosition(getTabSwitcher().getCount(), tabItem.getIndex(),
                                newPosition, predecessor);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
            } else if (tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                float currentPosition = tabItem.getTag().getPosition();
                Pair<Float, State> pair =
                        clipTabPosition(getTabSwitcher().getCount(), tabItem.getIndex(),
                                currentPosition, predecessor);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
                return true;
            } else if (tabItem.getTag().getState() == State.HIDDEN ||
                    tabItem.getTag().getState() == State.STACKED_START) {
                return true;
            }
        } else {
            float newPosition = calculateNonLinearPosition(tabItem, predecessor);
            Pair<Float, State> pair =
                    clipTabPosition(getTabSwitcher().getCount(), tabItem.getIndex(), newPosition,
                            predecessor);
            tabItem.getTag().setPosition(pair.first);
            tabItem.getTag().setState(pair.second);
        }

        return false;
    }

    /**
     * Calculates the non-linear position of a tab in relation to position of its predecessor.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose non-linear position should be
     *         calculated, as an instance of the class {@link TabItem}. The tab item may not be
     *         null
     * @param predecessor
     *         The predecessor as an instance of the class {@link TabItem}. The predecessor may not
     *         be null
     * @return The position, which has been calculated, as a {@link Float} value
     */
    public final float calculateNonLinearPosition(@NonNull final TabItem tabItem,
                                                  @NonNull final TabItem predecessor) {
        float predecessorPosition = predecessor.getTag().getPosition();
        float maxTabSpacing = calculateMaxTabSpacing(getTabSwitcher().getCount(), tabItem);
        return calculateNonLinearPosition(predecessorPosition, maxTabSpacing);
    }

    public final float calculateNonLinearPosition(final float predecessorPosition,
                                                  final float maxTabSpacing) {
        float ratio = Math.min(1,
                predecessorPosition / getAttachedPosition(false, getTabSwitcher().getCount()));
        float minTabSpacing = calculateMinTabSpacing(getTabSwitcher().getCount());
        return predecessorPosition - minTabSpacing - (ratio * (maxTabSpacing - minTabSpacing));
    }

    /**
     * Calculates and returns the position of a specific tab, when located at the end.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param index
     *         The index of the tab, whose position should be calculated, as an {@link Integer}
     *         value
     * @return The position, which has been calculated, as a {@link Float} value
     */
    public final float calculateEndPosition(@NonNull final AbstractTabItemIterator.Factory factory,
                                            final int index) {
        float defaultMaxTabSpacing = calculateMaxTabSpacing(getTabSwitcher().getCount(), null);
        int selectedTabIndex = getTabSwitcher().getSelectedTabIndex();

        if (selectedTabIndex > index) {
            AbstractTabItemIterator.AbstractBuilder builder = factory.create();
            AbstractTabItemIterator iterator = builder.create();
            TabItem selectedTabItem = iterator.getItem(selectedTabIndex);
            float selectedTabSpacing =
                    calculateMaxTabSpacing(getTabSwitcher().getCount(), selectedTabItem);
            return (getTabSwitcher().getCount() - 2 - index) * defaultMaxTabSpacing +
                    selectedTabSpacing;
        }

        return (getTabSwitcher().getCount() - 1 - index) * defaultMaxTabSpacing;
    }

    /**
     * Calculates and returns the position of a tab, when it is swiped.
     *
     * @return The position, which has been calculated, in pixels as an {@link Float} value
     */
    public final float calculateSwipePosition() {
        return getArithmetics().getSize(Axis.ORTHOGONAL_AXIS, getTabSwitcher().getTabContainer());
    }

    /**
     * Calculates and returns the maximum space between a specific tab and its predecessor. The
     * maximum space is greater for the currently selected tab.
     *
     * @param count
     *         The total number of tabs, which are contained by the tabs switcher, as an {@link
     *         Integer} value
     * @param tabItem
     *         The tab item, which corresponds to the tab, the maximum space should be returned for,
     *         as an instance of the class {@link TabItem} or null, if the default maximum space
     *         should be returned
     * @return The maximum space between the given tab and its predecessor in pixels as a {@link
     * Float} value
     */
    public final float calculateMaxTabSpacing(final int count, @Nullable final TabItem tabItem) {
        ensureNotEqual(maxTabSpacing, -1, "No maximum tab spacing has been set",
                IllegalStateException.class);
        return count > 4 && tabItem != null &&
                tabItem.getTab() == getTabSwitcher().getSelectedTab() ?
                maxTabSpacing * SELECTED_TAB_SPACING_RATIO : maxTabSpacing;
    }

    /**
     * Calculates and returns the minimum space between two neighboring tabs.
     *
     * @param count
     *         The total number of tabs, which are contained by the tabs switcher, as an {@link
     *         Integer} value
     * @return The minimum space between two neighboring tabs in pixels as a {@link Float} value
     */
    public final float calculateMinTabSpacing(final int count) {
        return calculateMaxTabSpacing(count, null) * MIN_TAB_SPACING_RATIO;
    }

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
     * Notifies, that the position or state of a tab has been changed.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose position or state has been changed,
     *         as an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void notifyOnViewStateChanged(@NonNull final TabItem tabItem) {
        if (getCallback() != null) {
            getCallback().onViewStateChanged(tabItem);
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
     */
    public PhoneDragHandler(@NonNull final TabSwitcher tabSwitcher,
                            @NonNull final Arithmetics arithmetics) {
        super(tabSwitcher, arithmetics, true);
        this.overshootDragHelper = new DragHelper(0);
        Resources resources = tabSwitcher.getResources();
        this.tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        this.stackedTabCount = resources.getInteger(R.integer.stacked_tab_count);
        this.stackedTabSpacing = resources.getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        this.maxOvershootDistance = resources.getDimensionPixelSize(R.dimen.max_overshoot_distance);
        this.maxStartOvershootAngle = resources.getInteger(R.integer.max_start_overshoot_angle);
        this.maxEndOvershootAngle = resources.getInteger(R.integer.max_end_overshoot_angle);
        this.maxTabSpacing = -1;
    }

    /**
     * Sets the maximum space between two neighboring tabs.
     *
     * @param maxTabSpacing
     *         The maximum space, which should be set, in pixels as a {@link Float} value
     */
    public final void setMaxTabSpacing(final float maxTabSpacing) {
        ensureGreater(maxTabSpacing, 0, "The maximum tab spacing must be greater than 0");
        this.maxTabSpacing = maxTabSpacing;
    }

    /**
     * Calculates and returns the position on the dragging axis, where the distance between a tab
     * and its predecessor should have reached the maximum.
     *
     * @param recalculate
     *         True, if the position should be forced to be recalculated, false otherwise
     * @param count
     *         The total number of tabs, which are contained by the tabs switcher, as an {@link
     *         Integer} value
     * @return The position, which has been calculated, in pixels as an {@link Float} value
     */
    public final float getAttachedPosition(final boolean recalculate, final int count) {
        if (recalculate || attachedPosition == -1) {
            float totalSpace = getArithmetics()
                    .getSize(Axis.DRAGGING_AXIS, getTabSwitcher().getTabContainer()) -
                    (getTabSwitcher().getLayout() == Layout.PHONE_PORTRAIT &&
                            getTabSwitcher().areToolbarsShown() ?
                            getTabSwitcher().getToolbars()[0].getHeight() + tabInset : 0);

            if (count == 3) {
                attachedPosition = totalSpace * 0.66f;
            } else if (count == 4) {
                attachedPosition = totalSpace * 0.6f;
            } else {
                attachedPosition = totalSpace * 0.5f;
            }
        }

        return attachedPosition;
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
    public final Pair<Float, State> clipTabPosition(final int count, final int index,
                                                    final float position,
                                                    @Nullable final TabItem predecessor) {
        return clipTabPosition(count, index, position,
                predecessor != null ? predecessor.getTag().getState() : null);
    }

    // TODO: Comment or remove the other method with same name
    public final Pair<Float, State> clipTabPosition(final int count, final int index,
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
    public final Pair<Float, State> calculatePositionAndStateWhenStackedAtStart(final int count,
                                                                                final int index,
                                                                                @Nullable final TabItem predecessor) {
        return calculatePositionAndStateWhenStackedAtStart(count, index,
                predecessor != null ? predecessor.getTag().getState() : null);
    }

    // TODO: Comment or remove method with same name
    @NonNull
    public final Pair<Float, State> calculatePositionAndStateWhenStackedAtStart(final int count,
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
     * Calculates and returns the position and state of a specific tab, when stacked at the end.
     *
     * @param index
     *         The index of the tab, whose position and state should be returned, as an {@link
     *         Integer} value
     * @return A pair, which contains the position and state of the given tab item, when stacked at
     * the end, as an instance of the class {@link Pair}. The pair may not be null
     */
    @NonNull
    public final Pair<Float, State> calculatePositionAndStateWhenStackedAtEnd(final int index) {
        float size =
                getArithmetics().getSize(Axis.DRAGGING_AXIS, getTabSwitcher().getTabContainer());
        int toolbarHeight = getTabSwitcher().areToolbarsShown() &&
                getTabSwitcher().getLayout() != Layout.PHONE_LANDSCAPE ?
                getTabSwitcher().getToolbars()[0].getHeight() - tabInset : 0;
        int padding =
                getArithmetics().getPadding(Axis.DRAGGING_AXIS, Gravity.START, getTabSwitcher()) +
                        getArithmetics()
                                .getPadding(Axis.DRAGGING_AXIS, Gravity.END, getTabSwitcher());
        int offset = getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ?
                stackedTabCount * stackedTabSpacing : 0;

        if (index < stackedTabCount) {
            float position =
                    size - toolbarHeight - tabInset - (stackedTabSpacing * (index + 1)) - padding +
                            offset;
            return Pair.create(position, State.STACKED_END);
        } else {
            float position =
                    size - toolbarHeight - tabInset - (stackedTabSpacing * stackedTabCount) -
                            padding + offset;
            return Pair.create(position, State.HIDDEN);
        }
    }

    // TODO: Remove
    public final int getFirstVisibleIndex() {
        return firstVisibleIndex;
    }

    // TODO: Remove
    public final void setFirstVisibleIndex(final int index) {
        this.firstVisibleIndex = index;
    }

    @Nullable
    protected final TabItem getFocusedTab(@NonNull final AbstractTabItemIterator.Factory factory,
                                          final float position) {
        AbstractTabItemIterator.AbstractBuilder<?, ?> builder = factory.create();
        AbstractTabItemIterator iterator = builder.start(Math.max(0, firstVisibleIndex)).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.getTag().getState() == State.FLOATING ||
                    tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                View view = tabItem.getView();
                float toolbarHeight = getTabSwitcher().areToolbarsShown() &&
                        getTabSwitcher().getLayout() != Layout.PHONE_LANDSCAPE ?
                        getTabSwitcher().getToolbars()[0].getHeight() - tabInset : 0;
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
    protected final boolean isOvershootingAtStart(
            @NonNull final AbstractTabItemIterator.Factory factory) {
        if (getTabSwitcher().getCount() <= 1) {
            return true;
        } else {
            AbstractTabItemIterator.AbstractBuilder builder = factory.create();
            AbstractTabItemIterator iterator = builder.create();
            TabItem tabItem = iterator.getItem(0);
            return tabItem.getTag().getState() == State.STACKED_START_ATOP;
        }
    }

    @Override
    protected final boolean isOvershootingAtEnd(
            @NonNull final AbstractTabItemIterator.Factory factory) {
        if (getTabSwitcher().getCount() <= 1) {
            return true;
        } else {
            AbstractTabItemIterator.AbstractBuilder builder = factory.create();
            AbstractTabItemIterator iterator = builder.create();
            TabItem lastTabItem = iterator.getItem(getTabSwitcher().getCount() - 1);
            TabItem predecessor = iterator.getItem(getTabSwitcher().getCount() - 2);
            return Math.round(predecessor.getTag().getPosition()) >=
                    Math.round(calculateMaxTabSpacing(getTabSwitcher().getCount(), lastTabItem));
        }
    }

    @Override
    protected final void onDrag(@NonNull final AbstractTabItemIterator.Factory factory,
                                @NonNull final DragState dragState, final float dragDistance) {
        if (dragState == DragState.DRAG_TO_END) {
            calculatePositionsWhenDraggingToEnd(factory, dragDistance);
        } else {
            calculatePositionsWhenDraggingToStart(factory, dragDistance);
        }
    }

    @Override
    protected final float onOvershootStart(@NonNull final AbstractTabItemIterator.Factory factory,
                                           final float dragPosition,
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
                AbstractTabItemIterator.AbstractBuilder builder = factory.create();
                AbstractTabItemIterator iterator = builder.create();
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
    protected final float onOvershootEnd(@NonNull final AbstractTabItemIterator.Factory factory,
                                         final float dragPosition, final float overshootThreshold) {
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
        firstVisibleIndex = -1;
        attachedPosition = -1;
    }

    @Override
    protected final boolean isSwipeThresholdReached(@NonNull final TabItem swipedTabItem) {
        View view = swipedTabItem.getView();
        return Math.abs(getArithmetics().getPosition(Axis.ORTHOGONAL_AXIS, view)) >
                getArithmetics().getSize(Axis.ORTHOGONAL_AXIS, getTabSwitcher()) / 6f;
    }

}