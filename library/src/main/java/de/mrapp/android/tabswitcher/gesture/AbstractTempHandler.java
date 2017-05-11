package de.mrapp.android.tabswitcher.gesture;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.util.gesture.DragHelper;

import static de.mrapp.android.util.Condition.ensureAtLeast;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * @author Michael Rapp
 */
// TODO: Move code into parent class
public abstract class AbstractTempHandler extends AbstractTouchEventHandler {

    /**
     * The tab switcher, whose tabs should be switched.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The velocity tracker, which is used to measure the velocity of dragging gestures.
     */
    private VelocityTracker velocityTracker;

    private final DragHelper dragHelper;

    private int dragThreshold;

    /**
     * The id of the pointer, which has been used to start the current drag gesture.
     */
    private int pointerId;

    private void handleDown(@NonNull final MotionEvent event) {
        pointerId = event.getPointerId(0);

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }

        velocityTracker.addMovement(event);
    }

    @NonNull
    protected final TabSwitcher getTabSwitcher() {
        return tabSwitcher;
    }

    @NonNull
    protected final DragHelper getDragHelper() {
        return dragHelper;
    }

    @Nullable
    protected final VelocityTracker getVelocityTracker() {
        return velocityTracker;
    }

    protected void reset(final int dragThreshold) {
        if (this.velocityTracker != null) {
            this.velocityTracker.recycle();
            this.velocityTracker = null;
        }

        this.pointerId = -1;
        this.dragThreshold = dragThreshold;
        this.dragHelper.reset(dragThreshold);
    }

    public AbstractTempHandler(final int priority, @NonNull final TabSwitcher tabSwitcher,
                               final int dragThreshold) {
        super(priority);
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureAtLeast(dragThreshold, 0, "The drag threshold must be at least 0");
        this.tabSwitcher = tabSwitcher;
        this.dragThreshold = dragThreshold;
        this.dragHelper = new DragHelper(0);
        this.velocityTracker = null;
        this.pointerId = -1;
    }

    protected abstract boolean isDraggingAllowed();

    protected abstract void onHandleTouchEvent();

    protected abstract void handleDrag(@NonNull final MotionEvent event);

    protected abstract void handleRelease(@Nullable final MotionEvent event,
                                          final int dragThreshold);

    @Override
    protected final boolean onHandleTouchEvent(@NonNull final MotionEvent event) {
        if (isDraggingAllowed()) {
            onHandleTouchEvent();

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
                        handleDrag(event);
                    } else {
                        handleRelease(null, dragThreshold);
                        handleDown(event);
                    }

                    return true;
                case MotionEvent.ACTION_UP:
                    if (!tabSwitcher.isAnimationRunning() && event.getPointerId(0) == pointerId) {
                        handleRelease(event, dragThreshold);
                    }

                    return true;
                default:
                    break;
            }
        }

        return false;
    }

}
