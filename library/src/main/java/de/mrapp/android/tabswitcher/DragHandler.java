package de.mrapp.android.tabswitcher;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import de.mrapp.android.tabswitcher.arithmetic.Arithmetics;
import de.mrapp.android.tabswitcher.model.Axis;
import de.mrapp.android.tabswitcher.model.DragState;
import de.mrapp.android.tabswitcher.iterator.TabIterator;
import de.mrapp.android.tabswitcher.model.Layout;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.util.DragHelper;
import de.mrapp.android.tabswitcher.util.ViewRecycler;

import static de.mrapp.android.util.Condition.ensureAtLeast;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * @author Michael Rapp
 */
public class DragHandler {

    public interface Callback {

        void onClick(@NonNull TabItem tabItem);

        void onCancelFling();

        void onFling(float distance, long duration);

        void onRevertStartOvershoot(float maxAngle);

        void onRevertEndOvershoot(float maxAngle);

        void onStartOvershoot(float position);

        void onTiltOnStartOvershoot(float angle);

        void onTiltOnEndOvershoot(float angle);

        void onSwipe(@NonNull TabItem tabItem, float distance);

        void onSwipeEnded(@NonNull TabItem tabItem, boolean remove, float velocity);

        void onViewStateChanged(@NonNull TabItem tabItem);

    }

    private void notifyOnClick(@NonNull final TabItem tabItem) {
        if (callback != null) {
            callback.onClick(tabItem);
        }
    }

    private void notifyOnCancelFling() {
        if (callback != null) {
            callback.onCancelFling();
        }
    }

    private void notifyOnFling(final float distance, final long duration) {
        if (callback != null) {
            callback.onFling(distance, duration);
        }
    }

    private void notifyOnRevertStartOvershoot() {
        if (callback != null) {
            callback.onRevertStartOvershoot(maxStartOvershootAngle);
        }
    }

    private void notifyOnRevertEndOvershoot() {
        if (callback != null) {
            callback.onRevertEndOvershoot(maxEndOvershootAngle);
        }
    }

    private void notifyOnStartOvershoot(final float position) {
        if (callback != null) {
            callback.onStartOvershoot(position);
        }
    }

    private void notifyOnTiltOnStartOvershoot(final float angle) {
        if (callback != null) {
            callback.onTiltOnStartOvershoot(angle);
        }
    }

    private void notifyOnTiltOnEndOvershoot(final float angle) {
        if (callback != null) {
            callback.onTiltOnEndOvershoot(angle);
        }
    }

    private void notifyOnSwipe(@NonNull final TabItem tabItem, final float distance) {
        if (callback != null) {
            callback.onSwipe(tabItem, distance);
        }
    }

    private void notifyOnSwipeEnded(@NonNull final TabItem tabItem, final boolean remove,
                                    final float velocity) {
        if (callback != null) {
            callback.onSwipeEnded(tabItem, remove, velocity);
        }
    }

    private void notifyOnViewStateChanged(@NonNull final TabItem tabItem) {
        if (callback != null) {
            callback.onViewStateChanged(tabItem);
        }
    }

    private final TabSwitcher tabSwitcher;

    private final ViewRecycler<TabItem, Integer> viewRecycler;

    /**
     * The arithmetics, which are used to calculate the positions, size and rotation of tabs.
     */
    private final Arithmetics arithmetics;

    /**
     * The threshold, which must be reached until tabs are dragged, in pixels.
     */
    private final int dragThreshold;

    /**
     * The drag helper, which is used to recognize drag gestures on the dragging axis.
     */
    private final DragHelper dragHelper;

    /**
     * The drag helper, which is used to recognize drag gestures when overshooting.
     */
    private final DragHelper overshootDragHelper;

    /**
     * The drag helper, which is used to recognize swipe gestures on the orthogonal axis.
     */
    private final DragHelper swipeDragHelper;

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
     * The minimum space between neighboring tabs in pixels.
     */
    private final int minTabSpacing;

    /**
     * The maximum space between neighboring tabs in pixels.
     */
    private final int maxTabSpacing;

    /**
     * The number of tabs, which are contained by a stack.
     */
    private final int stackedTabCount;

    /**
     * The space between tabs, which are part of a stack, in pixels.
     */
    private final int stackedTabSpacing;

    /**
     * The minimum velocity, which must be reached by a drag gesture to start a fling animation.
     */
    private final float minFlingVelocity;

    /**
     * The velocity, which may be reached by a drag gesture at maximum to start a fling animation.
     */
    private final float maxFlingVelocity;

    /**
     * The velocity, which must be reached by a drag gesture in order to start a swipe animation.
     */
    private final float minSwipeVelocity;

    /**
     * The inset of tabs in pixels.
     */
    private final int tabInset;

    /**
     * The velocity tracker, which is used to measure the velocity of dragging gestures.
     */
    private VelocityTracker velocityTracker;

    /**
     * The id of the pointer, which has been used to start the current drag gesture.
     */
    private int pointerId;

    /**
     * The currently swiped tab item.
     */
    private TabItem swipedTabItem;

    /**
     * The state of the currently performed drag gesture.
     */
    private DragState dragState;

    /**
     * The distance of the current drag gesture in pixels.
     */
    private float dragDistance;

    /**
     * The drag distance at which the start overshoot begins.
     */
    private float startOvershootThreshold;

    /**
     * The drag distance at which the end overshoot begins.
     */
    private float endOvershootThreshold;

    /**
     * The index of the first visible tab.
     */
    private int firstVisibleIndex;

    /**
     * The position on the dragging axis, where the distance between a tab and its predecessor
     * should have reached the maximum, in pixels.
     */
    private float attachedPosition;

    private Callback callback;

    /**
     * Resets the drag handler to its previous state, when a drag gesture has ended.
     */
    private void resetDragging() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }

        pointerId = -1;
        dragState = DragState.NONE;
        swipedTabItem = null;
        dragDistance = 0;
        startOvershootThreshold = -Float.MAX_VALUE;
        endOvershootThreshold = Float.MAX_VALUE;
        dragHelper.reset(dragThreshold);
        overshootDragHelper.reset();
        swipeDragHelper.reset();
    }

    /**
     * Handles, when a drag gesture has been started.
     *
     * @param event
     *         The motion event, which started the drag gesture, as an instance of the class {@link
     *         MotionEvent}. The motion event may not be null
     */
    private void handleDown(@NonNull final MotionEvent event) {
        pointerId = event.getPointerId(0);

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }

        velocityTracker.addMovement(event);
    }

    /**
     * Handles a click.
     *
     * @param event
     *         The motion event, which triggered the click, as an instance of the class {@link
     *         MotionEvent}. The motion event may not be null
     */
    private void handleClick(@NonNull final MotionEvent event) {
        TabItem tabItem = getFocusedTabView(arithmetics.getPosition(Axis.DRAGGING_AXIS, event));

        if (tabItem != null) {
            notifyOnClick(tabItem);
        }
    }

    /**
     * Handles a fling gesture.
     *
     * @param event
     *         The motion event, which triggered the fling gesture, as an instance of the class
     *         {@link MotionEvent}. The motion event may not be null
     * @param dragState
     *         The current drag state, which determines the fling direction, as a value of the enum
     *         {@link DragState}. The drag state may not be null
     */
    private void handleFling(@NonNull final MotionEvent event, @NonNull final DragState dragState) {
        int pointerId = event.getPointerId(0);
        velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
        float flingVelocity = Math.abs(velocityTracker.getYVelocity(pointerId));

        if (flingVelocity > minFlingVelocity) {
            float flingDistance = 0.25f * flingVelocity;

            if (dragState == DragState.DRAG_TO_START) {
                flingDistance = -1 * flingDistance;
            }

            long duration = Math.round(Math.abs(flingDistance) / flingVelocity * 1000);
            notifyOnFling(flingDistance, duration);
        }
    }

    /**
     * Clips the position of a specific tab item.
     *
     * @param position
     *         The position, which should be clipped, in pixels as a {@link Float} value
     * @param tabItem
     *         The tab item, whose position should be clipped, as an instance of the class {@link
     *         TabItem}. The tab item may not be null
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     */
    public final void clipTabPosition(final float position, @NonNull final TabItem tabItem,
                                      @Nullable final TabItem predecessor) {
        Pair<Float, State> startPair =
                calculatePositionAndStateWhenStackedAtStart(tabItem, predecessor);
        float startPosition = startPair.first;

        if (position <= startPosition) {
            tabItem.getTag().setPosition(startPosition);
            tabItem.getTag().setState(startPair.second);
        } else {
            Pair<Float, State> endPair = calculatePositionAndStateWhenStackedAtEnd(tabItem);
            float endPosition = endPair.first;

            if (position >= endPosition) {
                tabItem.getTag().setPosition(endPosition);
                tabItem.getTag().setState(endPair.second);
            } else {
                tabItem.getTag().setPosition(position);
                tabItem.getTag().setState(State.FLOATING);
            }
        }
    }

    /**
     * Calculates and returns the position and state of a specific tab item, when stacked at the
     * start.
     *
     * @param tabItem
     *         The tab item, whose position and state should be returned, as an instance of the
     *         class {@link TabItem}. The tab item may not be null
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     * @return A pair, which contains the position and state of the given tab item, when stacked at
     * the start, as an instance of the class {@link Pair}. The pair may not be null
     */
    @NonNull
    public final Pair<Float, State> calculatePositionAndStateWhenStackedAtStart(
            @NonNull final TabItem tabItem, @Nullable final TabItem predecessor) {
        if ((tabSwitcher.getCount() - tabItem.getIndex()) <= stackedTabCount) {
            float position =
                    stackedTabSpacing * (tabSwitcher.getCount() - (tabItem.getIndex() + 1));
            return Pair.create(position,
                    (predecessor == null || predecessor.getTag().getState() == State.FLOATING) ?
                            State.STACKED_START_ATOP : State.STACKED_START);
        } else {
            float position = stackedTabSpacing * stackedTabCount;
            return Pair.create(position,
                    (predecessor == null || predecessor.getTag().getState() == State.FLOATING) ?
                            State.STACKED_START_ATOP : State.HIDDEN);
        }
    }

    /**
     * Calculates and returns the position and state of a specific tab item, when stacked at the
     * end.
     *
     * @param tabItem
     *         The tab item, whose position and state should be returned, as an instance of the
     *         class {@link TabItem}. The tab item may not be null
     * @return A pair, which contains the position and state of the given tab item, when stacked at
     * the end, as an instance of the class {@link Pair}. The pair may not be null
     */
    @NonNull
    public final Pair<Float, State> calculatePositionAndStateWhenStackedAtEnd(
            @NonNull final TabItem tabItem) {
        float size = arithmetics.getSize(Axis.DRAGGING_AXIS, tabSwitcher.getTabContainer());
        int toolbarHeight =
                tabSwitcher.isToolbarShown() && tabSwitcher.getLayout() != Layout.PHONE_LANDSCAPE ?
                        tabSwitcher.getToolbar().getHeight() - tabInset : 0;
        int padding = arithmetics.getPadding(Axis.DRAGGING_AXIS, Gravity.START, tabSwitcher) +
                arithmetics.getPadding(Axis.DRAGGING_AXIS, Gravity.END, tabSwitcher);
        int offset = tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE ?
                stackedTabCount * stackedTabSpacing : 0;

        if (tabItem.getIndex() < stackedTabCount) {
            float position = size - toolbarHeight - tabInset -
                    (stackedTabSpacing * (tabItem.getIndex() + 1)) -
                    padding + offset;
            return Pair.create(position, State.STACKED_END);
        } else {
            float position =
                    size - toolbarHeight - tabInset - (stackedTabSpacing * stackedTabCount) -
                            padding + offset;
            return Pair.create(position, State.HIDDEN);
        }
    }

    /**
     * Calculates the positions of all tabs, depending on the current drag distance.
     */
    private void calculatePositions() {
        float currentDragDistance = dragHelper.getDragDistance();
        float distance = currentDragDistance - dragDistance;
        dragDistance = currentDragDistance;

        if (distance != 0) {
            if (dragState == DragState.DRAG_TO_END) {
                calculatePositionsWhenDraggingToEnd(distance);
            } else {
                calculatePositionsWhenDraggingToStart(distance);
            }
        }
    }

    /**
     * Calculates the positions of all tabs, when dragging towards the start.
     *
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     */
    private void calculatePositionsWhenDraggingToEnd(final float dragDistance) {
        firstVisibleIndex = -1;
        TabIterator iterator = new TabIterator.Builder(tabSwitcher, viewRecycler)
                .start(Math.max(0, firstVisibleIndex)).create();
        TabItem tabItem;
        boolean abort = false;

        while ((tabItem = iterator.next()) != null && !abort) {
            if (tabSwitcher.getCount() - tabItem.getIndex() > 1) {
                abort = calculatePositionWhenDraggingToEnd(dragDistance, tabItem,
                        iterator.previous());

                if (firstVisibleIndex == -1 && tabItem.getTag().getState() == State.FLOATING) {
                    firstVisibleIndex = tabItem.getIndex();
                }
            } else {
                clipTabPosition(tabItem.getTag().getPosition(), tabItem, iterator.previous());
            }

            notifyOnViewStateChanged(tabItem);
        }
    }

    /**
     * Calculates the positions of all tabs, when dragging towards the end.
     *
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     */
    private void calculatePositionsWhenDraggingToStart(final float dragDistance) {
        TabIterator iterator = new TabIterator.Builder(tabSwitcher, viewRecycler)
                .start(Math.max(0, firstVisibleIndex)).create();
        TabItem tabItem;
        boolean abort = false;

        while ((tabItem = iterator.next()) != null && !abort) {
            if (tabSwitcher.getCount() - tabItem.getIndex() > 1) {
                abort = calculatePositionWhenDraggingToStart(dragDistance, tabItem,
                        iterator.previous());
            } else {
                clipTabPosition(tabItem.getTag().getPosition(), tabItem, iterator.previous());
            }

            notifyOnViewStateChanged(tabItem);
        }

        if (firstVisibleIndex > 0) {
            int start = firstVisibleIndex - 1;
            iterator = new TabIterator.Builder(tabSwitcher, viewRecycler).reverse(true).start(start)
                    .create();
            abort = false;

            while ((tabItem = iterator.next()) != null && !abort) {
                TabItem previous = iterator.previous();
                float previousPosition = previous.getTag().getPosition();
                float newPosition = previousPosition + maxTabSpacing;
                tabItem.getTag().setPosition(newPosition);

                if (tabItem.getIndex() < start) {
                    clipTabPosition(previous.getTag().getPosition(), previous, tabItem);
                    notifyOnViewStateChanged(previous);

                    if (previous.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = previous.getIndex();
                    } else {
                        abort = true;
                    }
                }

                if (!iterator.hasNext()) {
                    clipTabPosition(newPosition, tabItem, null);
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
                predecessor.getTag().getPosition() > calculateAttachedPosition()) {
            if (tabItem.getTag().getState() == State.FLOATING) {
                float currentPosition = tabItem.getTag().getPosition();
                float newPosition = currentPosition + dragDistance;
                clipTabPosition(newPosition, tabItem, predecessor);
            } else if (tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                float currentPosition = tabItem.getTag().getPosition();
                clipTabPosition(currentPosition, tabItem, predecessor);
                return true;
            } else if (tabItem.getTag().getState() == State.HIDDEN ||
                    tabItem.getTag().getState() == State.STACKED_START) {
                return true;
            }
        } else {
            float newPosition = calculateNonLinearPosition(predecessor);
            clipTabPosition(newPosition, tabItem, predecessor);
        }

        return false;
    }

    /**
     * Calculates the position of a specific tab, when dragging towards the end.
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
    private boolean calculatePositionWhenDraggingToEnd(final float dragDistance,
                                                       @NonNull final TabItem tabItem,
                                                       @Nullable final TabItem predecessor) {
        if (predecessor == null || predecessor.getTag().getState() != State.FLOATING) {
            if ((tabItem.getTag().getState() == State.STACKED_START_ATOP &&
                    tabItem.getIndex() == 0) || tabItem.getTag().getState() == State.FLOATING) {
                float currentPosition = tabItem.getTag().getPosition();
                float thresholdPosition = calculateEndPosition(tabItem);
                float newPosition = Math.min(currentPosition + dragDistance, thresholdPosition);
                clipTabPosition(newPosition, tabItem, predecessor);
            } else if (tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                return true;
            }
        } else {
            float thresholdPosition = calculateEndPosition(tabItem);
            float newPosition =
                    Math.min(calculateNonLinearPosition(predecessor), thresholdPosition);
            clipTabPosition(newPosition, tabItem, predecessor);
        }

        return false;
    }

    /**
     * Calculates the non-linear position of a tab in relation to position of its predecessor.
     *
     * @param predecessor
     *         The predecessor as an instance of the class {@link TabItem}. The predecessor may not
     *         be null
     * @return The position, which has been calculated, as a {@link Float} value
     */
    private float calculateNonLinearPosition(@NonNull final TabItem predecessor) {
        float previousPosition = predecessor.getTag().getPosition();
        float ratio = Math.min(1, previousPosition / calculateAttachedPosition());
        return previousPosition - minTabSpacing - (ratio * (maxTabSpacing - minTabSpacing));
    }

    /**
     * Calculates and returns the position of a specific tab item, when located at the end.
     *
     * @param tabItem
     *         The tab item, whose position should be calculated, as an instance of the class {@link
     *         TabItem}. The tab item may not be null
     * @return The position, which has been calculated, as a {@link Float} value
     */
    private float calculateEndPosition(@NonNull final TabItem tabItem) {
        return (tabSwitcher.getCount() - (tabItem.getIndex() + 1)) * maxTabSpacing;
    }

    /**
     * Calculates and returns the position on the dragging axis, where the distance between a tab
     * and its predecessor should have reached the maximum.
     *
     * @return The position, which has been calculated, in pixels as an {@link Float} value
     */
    private float calculateAttachedPosition() {
        if (attachedPosition == -1) {
            attachedPosition =
                    (arithmetics.getSize(Axis.DRAGGING_AXIS, tabSwitcher.getTabContainer()) -
                            (tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE &&
                                    tabSwitcher.isToolbarShown() ?
                                    tabSwitcher.getToolbar().getHeight() + tabInset : 0)) / 2f;
        }

        return attachedPosition;
    }

    /**
     * Checks if a drag gesture resulted in overshooting.
     *
     * @param dragPosition
     *         The position of the pointer on the dragging axis in pixels as a {@link Float} value
     * @return True, if the drag gesture resulted in overshooting, false otherwise
     */
    private boolean checkIfOvershooting(final float dragPosition) {
        if (isOvershootingAtEnd() &&
                (dragState == DragState.DRAG_TO_END || dragState == DragState.OVERSHOOT_END)) {
            endOvershootThreshold = dragPosition;
            dragState = DragState.OVERSHOOT_END;
            return true;
        } else if (isOvershootingAtStart() &&
                (dragState == DragState.DRAG_TO_START || dragState == DragState.OVERSHOOT_START)) {
            startOvershootThreshold = dragPosition;
            dragState = DragState.OVERSHOOT_START;
            return true;
        }

        return false;
    }

    /**
     * Returns, whether the tabs are overshooting at the start.
     *
     * @return True, if the tabs are overshooting at the start, false otherwise
     */
    private boolean isOvershootingAtStart() {
        if (tabSwitcher.getCount() <= 1) {
            return true;
        } else {
            TabItem tabItem = TabItem.create(tabSwitcher, viewRecycler, 0);
            return tabItem.getTag().getState() == State.STACKED_START_ATOP;
        }
    }

    /**
     * Returns, whether the tabs are overshooting at the end.
     *
     * @return True, if the tabs are overshooting at the end, false otherwise
     */
    private boolean isOvershootingAtEnd() {
        if (tabSwitcher.getCount() <= 1) {
            return true;
        } else {
            TabItem tabItem = TabItem.create(tabSwitcher, viewRecycler, tabSwitcher.getCount() - 2);
            return tabItem.getTag().getPosition() >= maxTabSpacing;
        }
    }

    /**
     * Returns the tab item, which corresponds to the tab, which is focused when clicking/dragging
     * at a specific position.
     *
     * @param position
     *         The position in pixels as a {@link Float} value
     * @return The tab item, which corresponds to the focused tab, as an instance of the class
     * {@link TabItem} or null, if no tab is focused
     */
    @Nullable
    private TabItem getFocusedTabView(final float position) {
        TabIterator iterator = new TabIterator.Builder(tabSwitcher, viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.getTag().getState() == State.FLOATING ||
                    tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                View view = tabItem.getView();
                float toolbarHeight = tabSwitcher.isToolbarShown() &&
                        tabSwitcher.getLayout() != Layout.PHONE_LANDSCAPE ?
                        tabSwitcher.getToolbar().getHeight() - tabInset : 0;
                float viewPosition =
                        arithmetics.getPosition(Axis.DRAGGING_AXIS, view) + toolbarHeight +
                                arithmetics
                                        .getPadding(Axis.DRAGGING_AXIS, Gravity.START, tabSwitcher);

                if (viewPosition <= position) {
                    return tabItem;
                }
            }
        }

        return null;
    }

    public DragHandler(@NonNull final TabSwitcher tabSwitcher,
                       @NonNull final ViewRecycler<TabItem, Integer> viewRecycler,
                       @NonNull final Arithmetics arithmetics, final int dragThreshold) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        ensureNotNull(arithmetics, "The arithmetics may not be null");
        ensureAtLeast(dragThreshold, 0, "The drag threshold must be at least 0");
        this.tabSwitcher = tabSwitcher;
        this.viewRecycler = viewRecycler;
        this.arithmetics = arithmetics;
        this.dragThreshold = dragThreshold;
        this.dragHelper = new DragHelper(dragThreshold);
        this.overshootDragHelper = new DragHelper(0);
        Resources resources = tabSwitcher.getResources();
        this.swipeDragHelper =
                new DragHelper(resources.getDimensionPixelSize(R.dimen.swipe_threshold));
        this.tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        this.stackedTabCount = resources.getInteger(R.integer.stacked_tab_count);
        this.stackedTabSpacing = resources.getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        this.minTabSpacing = resources.getDimensionPixelSize(R.dimen.min_tab_spacing);
        this.maxTabSpacing = resources.getDimensionPixelSize(R.dimen.max_tab_spacing);
        this.maxOvershootDistance = resources.getDimensionPixelSize(R.dimen.max_overshoot_distance);
        this.maxStartOvershootAngle = resources.getInteger(R.integer.max_start_overshoot_angle);
        this.maxEndOvershootAngle = resources.getInteger(R.integer.max_end_overshoot_angle);
        ViewConfiguration configuration = ViewConfiguration.get(tabSwitcher.getContext());
        this.minFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.maxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        this.minSwipeVelocity = resources.getDimensionPixelSize(R.dimen.min_swipe_velocity);
        reset();
    }

    public final void setCallback(@Nullable final Callback callback) {
        this.callback = callback;
    }

    public final boolean handleTouchEvent(@NonNull final MotionEvent event) {
        if (tabSwitcher.isSwitcherShown() && !tabSwitcher.isEmpty()) {
            notifyOnCancelFling();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handleDown(event);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!tabSwitcher.isAnimationRunning() && event.getPointerId(0) == pointerId) {
                        if (velocityTracker == null) {
                            velocityTracker = VelocityTracker.obtain();
                        }

                        velocityTracker.addMovement(event);
                        handleDrag(arithmetics.getPosition(Axis.DRAGGING_AXIS, event),
                                arithmetics.getPosition(Axis.ORTHOGONAL_AXIS, event));
                    } else {
                        handleRelease(null);
                        handleDown(event);
                    }

                    return true;
                case MotionEvent.ACTION_UP:
                    if (!tabSwitcher.isAnimationRunning() && event.getPointerId(0) == pointerId) {
                        handleRelease(event);
                    }

                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    /**
     * Handles drag gestures.
     *
     * @param dragPosition
     *         The position of the pointer on the dragging axis in pixels as a {@link Float} value
     * @param orthogonalPosition
     *         The position of the pointer of the orthogonal axis in pixels as a {@link Float}
     *         value
     */
    public final void handleDrag(final float dragPosition, final float orthogonalPosition) {
        if (dragPosition <= startOvershootThreshold) {
            if (!dragHelper.isReset()) {
                dragHelper.reset(0);
            }

            dragState = DragState.OVERSHOOT_START;
            overshootDragHelper.update(dragPosition);
            float overshootDistance = Math.abs(overshootDragHelper.getDragDistance());

            if (overshootDistance <= maxOvershootDistance) {
                float ratio = Math.max(0, Math.min(1, overshootDistance / maxOvershootDistance));
                TabItem tabItem = TabItem.create(tabSwitcher, viewRecycler, 0);
                float currentPosition = tabItem.getTag().getPosition();
                float position = currentPosition - (currentPosition * ratio);
                notifyOnStartOvershoot(position);
            } else {
                float ratio = Math.max(0, Math.min(1,
                        (overshootDistance - maxOvershootDistance) / maxOvershootDistance));
                notifyOnTiltOnStartOvershoot(ratio * maxStartOvershootAngle);
            }
        } else if (dragPosition >= endOvershootThreshold) {
            if (!dragHelper.isReset()) {
                dragHelper.reset(0);
            }

            dragState = DragState.OVERSHOOT_END;
            overshootDragHelper.update(dragPosition);
            float overshootDistance = overshootDragHelper.getDragDistance();
            float ratio = Math.max(0, Math.min(1, overshootDistance / maxOvershootDistance));
            notifyOnTiltOnEndOvershoot(ratio * -maxEndOvershootAngle);
        } else {
            overshootDragHelper.reset();
            float previousDistance = dragHelper.isReset() ? 0 : dragHelper.getDragDistance();
            dragHelper.update(dragPosition);
            swipeDragHelper.update(orthogonalPosition);

            if (dragState == DragState.NONE && swipedTabItem == null &&
                    swipeDragHelper.hasThresholdBeenReached()) {
                TabItem tabItem = getFocusedTabView(dragHelper.getDragStartPosition());

                if (tabItem != null && tabItem.getTab().isCloseable()) {
                    swipedTabItem = tabItem;
                }
            }

            if (swipedTabItem == null && dragHelper.hasThresholdBeenReached()) {
                if (dragState == DragState.OVERSHOOT_START) {
                    dragState = DragState.DRAG_TO_END;
                } else if (dragState == DragState.OVERSHOOT_END) {
                    dragState = DragState.DRAG_TO_START;
                } else {
                    dragState = previousDistance - dragHelper.getDragDistance() <= 0 ?
                            DragState.DRAG_TO_END : DragState.DRAG_TO_START;
                }
            }

            if (swipedTabItem != null) {
                notifyOnSwipe(swipedTabItem, swipeDragHelper.getDragDistance());
            } else if (dragState != DragState.NONE) {
                calculatePositions();
                checkIfOvershooting(dragPosition);
            }
        }
    }

    /**
     * Handles, when a drag gesture has been ended.
     *
     * @param event
     *         The motion event, which ended the drag gesture, as an instance of the class {@link
     *         MotionEvent} or null, if no fling animation should be triggered
     */
    public final void handleRelease(@Nullable final MotionEvent event) {
        if (swipedTabItem != null) {
            float swipeVelocity = 0;

            if (event != null && velocityTracker != null) {
                int pointerId = event.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                swipeVelocity = Math.abs(velocityTracker.getXVelocity(pointerId));
            }

            View view = swipedTabItem.getView();
            boolean remove = swipeVelocity >= minSwipeVelocity ||
                    Math.abs(arithmetics.getPosition(Axis.ORTHOGONAL_AXIS, view)) >
                            arithmetics.getSize(Axis.ORTHOGONAL_AXIS, view) / 4f;
            notifyOnSwipeEnded(swipedTabItem, remove,
                    swipeVelocity > minSwipeVelocity ? swipeVelocity : 0);
            swipedTabItem = null;
        } else if (dragState == DragState.DRAG_TO_START || dragState == DragState.DRAG_TO_END) {
            if (event != null && velocityTracker != null && dragHelper.hasThresholdBeenReached()) {
                handleFling(event, dragState);
            }
        } else if (dragState == DragState.OVERSHOOT_END) {
            notifyOnRevertEndOvershoot();
        } else if (dragState == DragState.OVERSHOOT_START) {
            notifyOnRevertStartOvershoot();
        } else if (event != null && !dragHelper.hasThresholdBeenReached() &&
                !swipeDragHelper.hasThresholdBeenReached()) {
            handleClick(event);
        }

        resetDragging();
    }

    /**
     * Resets the drag handler to its initial state.
     */
    public final void reset() {
        resetDragging();
        firstVisibleIndex = -1;
        attachedPosition = -1;

    }

}