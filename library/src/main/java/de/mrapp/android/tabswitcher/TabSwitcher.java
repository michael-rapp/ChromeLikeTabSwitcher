/*
 * Copyright 2016 Michael Rapp
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
package de.mrapp.android.tabswitcher;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.mrapp.android.util.ThemeUtil;
import de.mrapp.android.util.ViewUtil;
import de.mrapp.android.util.gesture.DragHelper;

import static de.mrapp.android.util.Condition.ensureAtLeast;
import static de.mrapp.android.util.Condition.ensureNotEmpty;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A chrome-like tab switcher.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabSwitcher extends FrameLayout {

    public interface Decorator {

        @NonNull
        View inflateLayout(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent);

    }

    public static class Tab {

        private final Decorator decorator;

        private final CharSequence title;

        private Drawable icon;

        private boolean closeable;

        public Tab(@NonNull final CharSequence title, @NonNull final Decorator decorator) {
            ensureNotNull(title, "The title may not be null");
            ensureNotEmpty(title, "The title may not be empty");
            ensureNotNull(decorator, "The decorator may not be null");
            this.decorator = decorator;
            this.title = title;
            this.closeable = true;
            this.icon = null;
        }

        public Tab(@NonNull final Context context, @StringRes final int resourceId,
                   @NonNull final Decorator decorator) {
            ensureNotNull(context, "The context may not be null");
            ensureNotNull(decorator, "The decorator may not be null");
            this.decorator = decorator;
            this.title = context.getString(resourceId);
            this.closeable = true;
            this.icon = null;
        }

        @NonNull
        public final Decorator getDecorator() {
            return decorator;
        }

        @NonNull
        public final CharSequence getTitle() {
            return title;
        }

        @Nullable
        public final Drawable getIcon() {
            return icon;
        }

        public final void setIcon(@NonNull final Context context,
                                  @DrawableRes final int resourceId) {
            ensureNotNull(context, "The context may not be null");
            this.icon = ContextCompat.getDrawable(context, resourceId);
        }

        public final void setIcon(@Nullable final Drawable icon) {
            this.icon = icon;
        }

        public final boolean isCloseable() {
            return closeable;
        }

        public final void setCloseable(final boolean closeable) {
            this.closeable = closeable;
        }

    }

    private class TabView {

        private int index;

        private View view;

        private Tag tag;

        private ViewHolder viewHolder;

        public TabView(final int index, @NonNull final View view) {
            ensureAtLeast(index, 1, "The index must be at least 1");
            ensureNotNull(view, "The view may not be null");
            this.index = index;
            this.view = view;
            this.viewHolder = (ViewHolder) view.getTag(R.id.tag_view_holder);
            this.tag = (Tag) view.getTag(R.id.tag_properties);

            if (this.tag == null) {
                this.tag = new Tag();
                this.view.setTag(R.id.tag_properties, this.tag);
            }
        }

    }

    private class Iterator implements java.util.Iterator<TabView> {

        private int index = 1;

        private TabView current = null;

        private TabView previous = null;

        public TabView previous() {
            return previous;
        }

        @Override
        public boolean hasNext() {
            return getChildCount() - index >= 0;
        }

        @Override
        public TabView next() {
            int i = getChildCount() - index;

            if (i >= 0) {
                View view = getChildAt(i);
                previous = current;
                current = new TabView(index, view);
                index++;
                return current;
            }

            return null;
        }

    }

    private static class ViewHolder {

        private CardView cardView;

        private ViewGroup titleContainer;

        private TextView titleTextView;

        private ImageButton closeButton;

        private ViewGroup childContainer;

    }

    private static class Tag {

        private float projectedPosition;

        private float actualPosition;

        private int distance;

        private State state;

    }

    private enum State {

        STACKED_TOP,

        TOP_MOST_HIDDEN,

        TOP_MOST,

        VISIBLE,

        BOTTOM_MOST_HIDDEN,

        STACKED_BOTTOM

    }

    private enum ScrollDirection {

        NONE,

        UP,

        DOWN,

        TOP_THRESHOLD,

        BOTTOM_THRESHOLD;

    }

    private class ShowSwitcherAnimation extends Animation {

        @Override
        protected void applyTransformation(final float interpolatedTime, final Transformation t) {
            handleDrag((getHeight() / 2f - cardViewMargin) * interpolatedTime);
        }

    }

    private class FlingAnimation extends Animation {

        private final float flingDistance;

        public FlingAnimation(final float flingDistance) {
            this.flingDistance = flingDistance;
        }

        @Override
        protected void applyTransformation(final float interpolatedTime, final Transformation t) {
            handleDrag(flingDistance * interpolatedTime);
        }

    }

    private static final int STACKED_TAB_COUNT = 3;

    /**
     * A list, which contains the tab switcher's tabs.
     */
    private List<Tab> tabs;

    /**
     * An instance of the class {@link DragHelper}, which is used to recognize drag gestures.
     */
    private transient DragHelper dragHelper;

    private VelocityTracker velocityTracker;

    private boolean switcherShown;

    private int stackedTabSpacing;

    private int minTabSpacing;

    private int maxTabSpacing;

    private float minimumFlingVelocity;

    private float maximumFlingVelocity;

    private int cardViewMargin;

    private ScrollDirection scrollDirection;

    private int lastAttachedIndex;

    private float attachedPosition;

    private float topDragThreshold = -Float.MIN_VALUE;

    private float bottomDragThreshold = Float.MAX_VALUE;

    private int pointerId = -1;

    /**
     * Initializes the view.
     *
     * @param attributeSet
     *         The attribute set, which should be used to initialize the view, as an instance of the
     *         type {@link AttributeSet} or null, if no attributes should be obtained
     * @param defaultStyle
     *         The default style to apply to this view. If 0, no style will be applied (beyond what
     *         is included in the theme). This may either be an attribute resource, whose value will
     *         be retrieved from the current theme, or an explicit style resource
     * @param defaultStyleResource
     *         A resource identifier of a style resource that supplies default values for the view,
     *         used only if the default style is 0 or can not be found in the theme. Can be 0 to not
     *         look for defaults
     */
    private void initialize(@Nullable final AttributeSet attributeSet,
                            @AttrRes final int defaultStyle,
                            @StyleRes final int defaultStyleResource) {
        tabs = new ArrayList<>();
        dragHelper = new DragHelper(10);
        switcherShown = false;
        stackedTabSpacing = getResources().getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        minTabSpacing = getResources().getDimensionPixelSize(R.dimen.min_tab_spacing);
        maxTabSpacing = getResources().getDimensionPixelSize(R.dimen.max_tab_spacing);
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        minimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        cardViewMargin = getResources().getDimensionPixelSize(R.dimen.card_view_margin);
        scrollDirection = ScrollDirection.NONE;
        obtainStyledAttributes(attributeSet, defaultStyle, defaultStyleResource);
    }

    private ViewGroup inflateLayout(@NonNull final Tab tab) {
        Decorator decorator = tab.getDecorator();
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        FrameLayout frameLayout = new FrameLayout(getContext());
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.cardView =
                (CardView) layoutInflater.inflate(R.layout.card_view, frameLayout, false);
        frameLayout.addView(viewHolder.cardView);
        viewHolder.titleContainer =
                (ViewGroup) viewHolder.cardView.findViewById(R.id.tab_title_container);
        viewHolder.titleTextView =
                (TextView) viewHolder.cardView.findViewById(R.id.tab_title_text_view);
        viewHolder.titleTextView.setText(tab.getTitle());
        viewHolder.titleTextView
                .setCompoundDrawablesWithIntrinsicBounds(tab.getIcon(), null, null, null);
        viewHolder.closeButton =
                (ImageButton) viewHolder.cardView.findViewById(R.id.close_tab_button);
        viewHolder.closeButton.setVisibility(tab.isCloseable() ? View.VISIBLE : View.GONE);
        viewHolder.childContainer =
                (ViewGroup) viewHolder.cardView.findViewById(R.id.child_container);
        View view = decorator.inflateLayout(layoutInflater, viewHolder.childContainer);
        viewHolder.childContainer.addView(view, 0,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        frameLayout.setTag(R.id.tag_view_holder, viewHolder);
        return frameLayout;
    }

    /**
     * Obtains all attributes froma specific attribute set.
     *
     * @param attributeSet
     *         The attribute set, the attributes should be obtained from, as an instance of the type
     *         {@link AttributeSet} or null, if no attributes should be obtained
     * @param defaultStyle
     *         The default style to apply to this view. If 0, no style will be applied (beyond what
     *         is included in the theme). This may either be an attribute resource, whose value will
     *         be retrieved from the current theme, or an explicit style resource
     * @param defaultStyleResource
     *         A resource identifier of a style resource that supplies default values for the view,
     *         used only if the default style is 0 or can not be found in the theme. Can be 0 to not
     *         look for defaults
     */
    private void obtainStyledAttributes(@Nullable final AttributeSet attributeSet,
                                        @AttrRes final int defaultStyle,
                                        @StyleRes final int defaultStyleResource) {
        TypedArray typedArray = getContext()
                .obtainStyledAttributes(attributeSet, R.styleable.TabSwitcher, defaultStyle,
                        defaultStyleResource);

        try {
            obtainBackground(typedArray);
        } finally {
            typedArray.recycle();
        }
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
            ViewUtil.setBackground(this, ContextCompat.getDrawable(getContext(), resourceId));
        } else {
            int defaultValue =
                    ContextCompat.getColor(getContext(), R.color.tab_switcher_background_color);
            int color =
                    typedArray.getColor(R.styleable.TabSwitcher_android_background, defaultValue);
            setBackgroundColor(color);
        }
    }

    public TabSwitcher(@NonNull final Context context) {
        this(context, null);
    }

    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize(attributeSet, 0, 0);
    }

    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet,
                       @AttrRes final int defaultStyle) {
        super(context, attributeSet, defaultStyle);
        initialize(attributeSet, defaultStyle, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet,
                       @AttrRes final int defaultStyle, @StyleRes final int defaultStyleResource) {
        super(context, attributeSet, defaultStyle, defaultStyleResource);
        initialize(attributeSet, defaultStyle, defaultStyleResource);
    }

    public final void addTab(@NonNull final Tab tab) {
        addTab(tab, tabs.size());
    }

    public final void addTab(@NonNull final Tab tab, final int index) {
        ensureNotNull(tab, "The tab may not be null");
        tabs.add(index, tab);
        ViewGroup view = inflateLayout(tab);
        addView(view, getChildCount() - index,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public final int getCount() {
        return tabs.size();
    }

    public final boolean isSwitcherShown() {
        return switcherShown;
    }

    public final void showSwitcher() {
        if (!isSwitcherShown()) {
            switcherShown = true;
            dragToTopThresholdPosition();
            printProjectedPositions();

            boolean dragging = true;
            int drag = 0;

            while (dragging) {
                drag += 20;
                dragging = handleDrag(drag);
            }

            handleRelease(null);
            printProjectedPositions();

            dragging = true;
            drag = 0;

            while (dragging) {
                drag -= 20;
                dragging = handleDrag(drag);
            }

            handleRelease(null);
            printProjectedPositions();

/*
            Animation animation = new ShowSwitcherAnimation();
            animation.setAnimationListener(createAnimationListener());
            animation.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            startAnimation(animation);
            */
        }
    }

    private void printProjectedPositions() {
        Iterator iterator = new Iterator();
        TabView tabView;

        while ((tabView = iterator.next()) != null) {
            System.out.println(tabView.index + ": " + tabView.tag.actualPosition);
        }

        System.out.println("-----------------------");
    }

    private Animation.AnimationListener createAnimationListener() {
        return new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(final Animation animation) {

            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                handleRelease(null);
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {

            }

        };
    }

    private void dragToTopThresholdPosition() {
        Iterator iterator = new Iterator();
        TabView tabView;

        while ((tabView = iterator.next()) != null) {
            ViewHolder viewHolder = tabView.viewHolder;
            viewHolder.cardView.setUseCompatPadding(true);
            viewHolder.cardView.setRadius(
                    getResources().getDimensionPixelSize(R.dimen.card_view_corner_radius));
            viewHolder.titleContainer.setVisibility(View.VISIBLE);
            int padding = getResources().getDimensionPixelSize(R.dimen.card_view_padding);
            int actionBarSize = ThemeUtil.getDimensionPixelSize(getContext(), R.attr.actionBarSize);
            viewHolder.childContainer.setPadding(padding, actionBarSize, padding, padding);
            Tag previous = iterator.previous() != null ? iterator.previous().tag : null;
            calculateTopThresholdPosition(tabView.index, tabView.tag, previous);
            applyTag(tabView.tag, tabView.view);
        }
    }

    private void calculateTopThresholdPosition(final int index, @NonNull final Tag tag,
                                               @Nullable final Tag previous) {
        float position = 0 - (index - 1) * minTabSpacing;
        clipDraggedTabPosition(position, index, tag, previous);
    }

    private void dragToBottomThresholdPosition() {
        Iterator iterator = new Iterator();
        TabView tabView;

        while ((tabView = iterator.next()) != null) {
            Tag previous = iterator.previous() != null ? iterator.previous().tag : null;
            calculateBottomThresholdPosition(tabView.index, tabView.tag, previous);
            applyTag(tabView.tag, tabView.view);
        }
    }

    private void calculateBottomThresholdPosition(final int index, @NonNull final Tag tag,
                                                  @Nullable final Tag previous) {
        float position = (getChildCount() - index) * maxTabSpacing;
        clipDraggedTabPosition(position, index, tag, previous);
    }

    private void applyTag(@NonNull final Tag tag, @NonNull final View view) {
        float position = tag.projectedPosition;
        State state = tag.state;
        view.setY(position);
        view.setVisibility(state == State.TOP_MOST_HIDDEN || state == State.BOTTOM_MOST_HIDDEN ?
                View.INVISIBLE : View.VISIBLE);
    }

    private void calculateTabPosition(final int dragDistance, final int index,
                                      @NonNull final Tag tag, @Nullable final Tag previous) {
        if (getChildCount() - index > 0) {
            int distance = dragDistance - tag.distance;
            tag.distance = dragDistance;

            if (distance != 0) {
                float currentPosition = tag.actualPosition;
                float newPosition = currentPosition + distance;
                clipDraggedTabPosition(newPosition, index, tag, previous);

                if (scrollDirection == ScrollDirection.DOWN) {
                    calculateNonLinearPositionWhenDraggingDown(distance, index, tag, previous,
                            currentPosition);
                } else if (scrollDirection == ScrollDirection.UP) {
                    calculateNonLinearPositionWhenDraggingUp(distance, index, tag, previous,
                            currentPosition);
                }
            }
        }
    }

    private void calculateNonLinearPositionWhenDraggingDown(final int dragDistance, final int index,
                                                            @NonNull final Tag tag,
                                                            @Nullable final Tag previous,
                                                            final float currentPosition) {
        if (previous != null && previous.state == State.VISIBLE &&
                tag.state == State.VISIBLE) {
            float newPosition = calculateNonLinearPosition(dragDistance, currentPosition, index);
            boolean attached = false;

            if (previous.projectedPosition - newPosition >= maxTabSpacing) {
                lastAttachedIndex = index;
                newPosition = previous.projectedPosition - maxTabSpacing;
                attached = true;
            }

            clipDraggedTabPosition(newPosition, index, tag, previous);

            if (attached && attachedPosition == 0) {
                attachedPosition = tag.projectedPosition;
            }
        }
    }

    private void calculateNonLinearPositionWhenDraggingUp(final int dragDistance, final int index,
                                                          @NonNull final Tag tag,
                                                          @Nullable final Tag previous,
                                                          final float currentPosition) {
        if (tag.state == State.VISIBLE) {
            boolean attached = tag.projectedPosition <= attachedPosition;

            if (previous == null || !attached) {
                lastAttachedIndex = index;
            }

            if (previous != null && attached) {
                float newPosition =
                        calculateNonLinearPosition(dragDistance, currentPosition, index);

                if (previous.state != State.STACKED_BOTTOM &&
                        previous.state != State.BOTTOM_MOST_HIDDEN &&
                        previous.projectedPosition - newPosition <= minTabSpacing) {
                    newPosition = previous.projectedPosition - minTabSpacing;
                }

                clipDraggedTabPosition(newPosition, index, tag, previous);
            }
        }
    }

    private float calculateNonLinearPosition(final int dragDistance, final float currentPosition,
                                             final int index) {
        return currentPosition + (float) (dragDistance * Math.pow(0.5, index - lastAttachedIndex));
    }

    private void clipDraggedTabPosition(final float dragPosition, final int index,
                                        @NonNull final Tag tag, @Nullable final Tag previous) {
        Pair<Float, State> topMostPair = calculateTopMostPosition(index, previous);
        float topMostPosition = topMostPair.first;

        if (dragPosition <= topMostPosition) {
            tag.projectedPosition = topMostPair.first;
            tag.actualPosition = dragPosition;
            tag.state = topMostPair.second;
            return;
        } else {
            Pair<Float, State> bottomMostPair = calculateBottomMostPosition(index);
            float bottomMostPosition = bottomMostPair.first;

            if (dragPosition >= bottomMostPosition) {
                tag.projectedPosition = bottomMostPair.first;
                tag.actualPosition = dragPosition;
                tag.state = bottomMostPair.second;
                return;
            }
        }

        tag.projectedPosition = dragPosition;
        tag.actualPosition = dragPosition;
        tag.state = State.VISIBLE;
    }

    private Pair<Float, State> calculateTopMostPosition(final int index,
                                                        @Nullable final Tag previous) {
        if ((getCount() - index) < STACKED_TAB_COUNT) {
            float position = stackedTabSpacing * (getCount() - index);
            return Pair.create(position, State.STACKED_TOP);
        } else {
            float position = stackedTabSpacing * STACKED_TAB_COUNT;
            return Pair.create(position,
                    (previous == null || previous.state == State.VISIBLE) ? State.TOP_MOST :
                            State.TOP_MOST_HIDDEN);
        }
    }

    private Pair<Float, State> calculateBottomMostPosition(final int index) {
        if (index <= STACKED_TAB_COUNT) {
            float position = getHeight() - cardViewMargin - stackedTabSpacing * index;
            return Pair.create(position, State.STACKED_BOTTOM);
        } else {
            float position = getHeight() - cardViewMargin - stackedTabSpacing * STACKED_TAB_COUNT;
            return Pair.create(position, State.BOTTOM_MOST_HIDDEN);
        }
    }

    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        if (isSwitcherShown()) {
            if (getAnimation() != null) {
                getAnimation().cancel();
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handleDown(event);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerId(0) == pointerId) {
                        velocityTracker.addMovement(event);
                        handleDrag(event.getY(0));
                    } else {
                        handleRelease(null);
                        handleDown(event);
                    }

                    return true;
                case MotionEvent.ACTION_UP:
                    if (event.getPointerId(0) == pointerId) {
                        handleRelease(event);
                    }

                    return true;
                default:
                    break;
            }
        }

        return super.onTouchEvent(event);
    }

    private void handleDown(@NonNull final MotionEvent event) {
        pointerId = event.getPointerId(0);

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }

        velocityTracker.addMovement(event);
    }

    private boolean isTopDragThresholdReached() {
        View view = getChildAt(getChildCount() - 1);
        Tag tag = (Tag) view.getTag(R.id.tag_properties);
        return tag.state == State.TOP_MOST;
    }

    private boolean isBottomDragThresholdReached() {
        View view = getChildAt(1);
        Tag tag = (Tag) view.getTag(R.id.tag_properties);
        return tag.projectedPosition >= maxTabSpacing;
    }

    private boolean handleDrag(final float dragPosition) {
        if (dragPosition > topDragThreshold && dragPosition < bottomDragThreshold) {
            int previousDistance = dragHelper.getDistance();
            dragHelper.update(dragPosition);
            int diff = previousDistance - dragHelper.getDistance();
            scrollDirection = diff == 0 ? ScrollDirection.NONE :
                    diff < 0 ? ScrollDirection.DOWN : ScrollDirection.UP;

            if (scrollDirection != ScrollDirection.NONE && dragHelper.hasThresholdBeenReached()) {
                lastAttachedIndex = 1;
                Iterator iterator = new Iterator();
                TabView tabView;

                while ((tabView = iterator.next()) != null) {
                    Tag previous = iterator.previous() != null ? iterator.previous().tag : null;
                    calculateTabPosition(dragHelper.getDistance(), tabView.index, tabView.tag,
                            previous);
                    applyTag(tabView.tag, tabView.view);
                }

                if (isBottomDragThresholdReached()) {
                    bottomDragThreshold = dragPosition;
                    scrollDirection = ScrollDirection.BOTTOM_THRESHOLD;
                    dragToBottomThresholdPosition();
                } else if (isTopDragThresholdReached()) {
                    topDragThreshold = dragPosition;
                    scrollDirection = ScrollDirection.TOP_THRESHOLD;
                    // TODO: dragToTopThresholdPosition();
                }
            }

            return true;
        }

        return false;
    }

    private void handleRelease(@Nullable final MotionEvent event) {
        boolean thresholdReached = dragHelper.hasThresholdBeenReached();
        ScrollDirection flingDirection = this.scrollDirection;
        this.dragHelper.reset();
        this.topDragThreshold = -Float.MAX_VALUE;
        this.bottomDragThreshold = Float.MAX_VALUE;
        this.scrollDirection = ScrollDirection.NONE;

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            Tag tag = (Tag) view.getTag(R.id.tag_properties);
            tag.projectedPosition = view.getY();
            tag.distance = 0;
        }

        if (velocityTracker != null) {
            if (thresholdReached && event != null && (flingDirection == ScrollDirection.UP ||
                    flingDirection == ScrollDirection.DOWN)) {
                int pointerId = event.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, maximumFlingVelocity);
                float flingVelocity = Math.abs(velocityTracker.getYVelocity(pointerId));

                if (flingVelocity > minimumFlingVelocity) {
                    float flingDistance = 0.25f * flingVelocity;
                    flingDistance = flingDirection == ScrollDirection.UP ? -1 * flingDistance :
                            flingDistance;
                    Animation animation = new FlingAnimation(flingDistance);
                    animation.setAnimationListener(createAnimationListener());
                    animation.setDuration(
                            Math.round(Math.abs(flingDistance) / flingVelocity * 1000));
                    animation.setInterpolator(new DecelerateInterpolator());
                    startAnimation(animation);
                }
            }

            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

}