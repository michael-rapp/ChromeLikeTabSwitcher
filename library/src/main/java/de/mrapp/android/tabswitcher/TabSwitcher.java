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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
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
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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

        private boolean reverse;

        private int index;

        private TabView current;

        private TabView previous;

        private TabView first;

        public Iterator() {
            this(false);
        }

        public Iterator(final boolean reverse) {
            this(reverse, -1);
        }

        public Iterator(final boolean reverse, final int start) {
            this.reverse = reverse;
            this.previous = null;
            this.index = start != -1 ? start : (reverse ? getChildCount() : 1);
            int previousIndex = reverse ? this.index + 1 : this.index - 1;

            if (previousIndex >= 1 && previousIndex <= getChildCount()) {
                this.current =
                        new TabView(previousIndex, getChildAt(getChildCount() - previousIndex));
            } else {
                this.current = null;
            }
        }

        public TabView first() {
            return first;
        }

        public TabView previous() {
            return previous;
        }

        @Override
        public boolean hasNext() {
            return reverse ? index >= 1 : getChildCount() - index >= 0;
        }

        @Override
        public TabView next() {
            if (hasNext()) {
                View view = getChildAt(getChildCount() - index);
                previous = current;

                if (first == null) {
                    first = current;
                }

                current = new TabView(index, view);
                index += reverse ? -1 : 1;
                return current;
            }

            return null;
        }

    }

    private static class ViewHolder {

        private ViewGroup titleContainer;

        private TextView titleTextView;

        private ImageButton closeButton;

        private ViewGroup childContainer;

        private View borderView;

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

        DRAGGING_UP,

        DRAGGING_DOWN,

        OVERSHOOT_UP,

        OVERSHOOT_DOWN;

    }

    private enum Axis {

        DRAGGING_AXIS,

        ORTHOGONAL_AXIS

    }

    private class ShowSwitcherAnimation extends Animation {

        @Override
        protected void applyTransformation(final float interpolatedTime, final Transformation t) {
            if (dragAnimation != null) {
                handleDrag(0, (2 * maxTabSpacing + minTabSpacing) * interpolatedTime);
            }
        }

    }

    private class FlingAnimation extends Animation {

        private final float flingDistance;

        public FlingAnimation(final float flingDistance) {
            this.flingDistance = flingDistance;
        }

        @Override
        protected void applyTransformation(final float interpolatedTime, final Transformation t) {
            if (dragAnimation != null) {
                handleDrag(0, flingDistance * interpolatedTime);
            }
        }

    }

    private class OvershootUpAnimation extends Animation {

        private Float startPosition = null;

        @SuppressWarnings("WrongConstant")
        @Override
        protected void applyTransformation(final float interpolatedTime, final Transformation t) {
            if (overshootUpAnimation != null) {
                Iterator iterator = new Iterator();
                TabView tabView;

                while ((tabView = iterator.next()) != null) {
                    View view = tabView.view;

                    if (tabView.index == 1) {
                        if (startPosition == null) {
                            startPosition = getPosition(Axis.DRAGGING_AXIS, view);
                        }

                        float targetPosition = tabView.tag.projectedPosition;
                        setPosition(Axis.DRAGGING_AXIS, view, startPosition +
                                (targetPosition - startPosition) * interpolatedTime);
                    } else {
                        View firstView = iterator.first().view;
                        view.setVisibility(getPosition(Axis.DRAGGING_AXIS, firstView) <=
                                getPosition(Axis.DRAGGING_AXIS, view) ? View.INVISIBLE :
                                getVisibility(tabView));
                    }
                }
            }
        }

    }

    private static final int STACKED_TAB_COUNT = 3;

    private static final float MAX_DOWN_OVERSHOOT_ANGLE = 4f;

    private static final float MAX_UP_OVERSHOOT_ANGLE = 2f;

    /**
     * A list, which contains the tab switcher's tabs.
     */
    private List<Tab> tabs;

    private int tabBackgroundColor;

    /**
     * An instance of the class {@link DragHelper}, which is used to recognize drag gestures.
     */
    private DragHelper dragHelper;

    private DragHelper overshootDragHelper;

    private DragHelper closeDragHelper;

    private VelocityTracker velocityTracker;

    private boolean switcherShown;

    private int stackedTabSpacing;

    private int minTabSpacing;

    private int maxTabSpacing;

    private int maxOvershootDistance;

    private float minFlingVelocity;

    private float maxFlingVelocity;

    private float minCloseFlingVelocity;

    private float closedTabAlpha;

    private float closedTabScale;

    private int tabInset;

    private int tabBorderWidth;

    private ScrollDirection scrollDirection;

    private TabView draggedTabView;

    private int lastAttachedIndex;

    private float attachedPosition;

    private float topDragThreshold = -Float.MIN_VALUE;

    private float bottomDragThreshold = Float.MAX_VALUE;

    private int pointerId = -1;

    private Animation dragAnimation;

    private ViewPropertyAnimator overshootAnimation;

    private Animation overshootUpAnimation;

    private ViewPropertyAnimator closeAnimation;

    private ViewPropertyAnimator relocateAnimation;

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
        switcherShown = false;
        Resources resources = getResources();
        dragHelper = new DragHelper(resources.getDimensionPixelSize(R.dimen.drag_threshold));
        overshootDragHelper = new DragHelper(0);
        closeDragHelper =
                new DragHelper(resources.getDimensionPixelSize(R.dimen.close_drag_threshold));
        stackedTabSpacing = resources.getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        minTabSpacing = resources.getDimensionPixelSize(R.dimen.min_tab_spacing);
        maxTabSpacing = resources.getDimensionPixelSize(R.dimen.max_tab_spacing);
        maxOvershootDistance = resources.getDimensionPixelSize(R.dimen.max_overshoot_distance);
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        minFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        maxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        minCloseFlingVelocity = resources.getDimensionPixelSize(R.dimen.min_close_fling_velocity);
        TypedValue typedValue = new TypedValue();
        resources.getValue(R.dimen.closed_tab_scale, typedValue, true);
        closedTabScale = typedValue.getFloat();
        resources.getValue(R.dimen.closed_tab_alpha, typedValue, true);
        closedTabAlpha = typedValue.getFloat();
        tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        tabBorderWidth = resources.getDimensionPixelSize(R.dimen.tab_border_width);
        scrollDirection = ScrollDirection.NONE;
        obtainStyledAttributes(attributeSet, defaultStyle, defaultStyleResource);
    }

    private ViewGroup inflateLayout(@NonNull final Tab tab) {
        Decorator decorator = tab.getDecorator();
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        ViewHolder viewHolder = new ViewHolder();
        ViewGroup tabView = (ViewGroup) layoutInflater.inflate(R.layout.tab_view, this, false);
        viewHolder.titleContainer = (ViewGroup) tabView.findViewById(R.id.tab_title_container);
        viewHolder.titleTextView = (TextView) tabView.findViewById(R.id.tab_title_text_view);
        viewHolder.titleTextView.setText(tab.getTitle());
        viewHolder.titleTextView
                .setCompoundDrawablesWithIntrinsicBounds(tab.getIcon(), null, null, null);
        viewHolder.closeButton = (ImageButton) tabView.findViewById(R.id.close_tab_button);
        viewHolder.closeButton.setVisibility(tab.isCloseable() ? View.VISIBLE : View.GONE);
        viewHolder.closeButton.setOnClickListener(createCloseButtonClickListener(tab));
        viewHolder.childContainer = (ViewGroup) tabView.findViewById(R.id.child_container);
        View childView = decorator.inflateLayout(layoutInflater, viewHolder.childContainer);
        viewHolder.childContainer.addView(childView, 0,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        viewHolder.borderView = tabView.findViewById(R.id.border_view);
        tabView.setTag(R.id.tag_view_holder, viewHolder);
        return tabView;
    }

    private OnClickListener createCloseButtonClickListener(@NonNull final Tab tab) {
        return new OnClickListener() {

            @Override
            public void onClick(final View v) {
                int index = tabs.indexOf(tab);

                if (index != -1) {
                    int childIndex = getChildCount() - (index + 1);
                    View view = getChildAt(childIndex);
                    TabView tabView = new TabView(index + 1, view);
                    animateClose(tabView, true, 0);
                }
            }

        };
    }

    private void animateClose(@NonNull final TabView tabView, final boolean close,
                              final float flingVelocity) {
        View view = tabView.view;
        float closedTabPosition = calculateClosedTabPosition();
        float position = getPosition(Axis.ORTHOGONAL_AXIS, view);
        float targetX = close ? (position < 0 ? -1 * closedTabPosition : closedTabPosition) : 0;
        float distance = Math.abs(targetX - position);
        long animationDuration;

        if (flingVelocity >= minCloseFlingVelocity) {
            animationDuration = Math.round((distance / flingVelocity) * 1000);
        } else {
            animationDuration = Math.round(
                    getResources().getInteger(android.R.integer.config_mediumAnimTime) *
                            (distance / closedTabPosition));
        }

        closeAnimation = view.animate();
        closeAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        closeAnimation.setListener(createCloseAnimationListener(tabView, close));
        closeAnimation.setDuration(animationDuration);
        animatePosition(Axis.ORTHOGONAL_AXIS, closeAnimation, targetX);
        animateScale(Axis.ORTHOGONAL_AXIS, closeAnimation, close ? closedTabScale : 1);
        animateScale(Axis.DRAGGING_AXIS, closeAnimation, close ? closedTabScale : 1);
        closeAnimation.alpha(close ? closedTabAlpha : 1);
        closeAnimation.setStartDelay(0);
        closeAnimation.start();
    }

    private Animator.AnimatorListener createCloseAnimationListener(@NonNull final TabView tabView,
                                                                   final boolean close) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(final Animator animation) {
                super.onAnimationStart(animation);

                if (close && tabView.index > 1) {
                    long animationDuration =
                            getResources().getInteger(android.R.integer.config_mediumAnimTime);
                    long startDelay =
                            getResources().getInteger(android.R.integer.config_shortAnimTime);
                    int start = tabView.index - 1;
                    Iterator iterator = new Iterator(true, start);
                    TabView tabView;

                    while ((tabView = iterator.next()) != null) {
                        TabView previous = iterator.previous();
                        View view = tabView.view;
                        closeAnimation = view.animate();
                        closeAnimation.setListener(
                                createRelocateAnimationListener(tabView, previous.tag));
                        closeAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
                        closeAnimation.setDuration(animationDuration);
                        closeAnimation.y(previous.tag.projectedPosition);
                        closeAnimation.setStartDelay((start + 1 - tabView.index) * startDelay);
                        closeAnimation.start();
                    }
                }
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);

                if (close) {
                    removeView(tabView.view);
                    tabs.remove(tabView.index - 1);
                }

                closeAnimation = null;
                draggedTabView = null;
            }

        };
    }

    private Animator.AnimatorListener createRelocateAnimationListener(
            @NonNull final TabView tabView, @NonNull final Tag tag) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                View view = tabView.view;
                view.setTag(R.id.tag_properties, tag);
                tabView.tag = tag;
                applyTag(tabView);

                if (tabView.index == 1) {
                    relocateAnimation = null;
                }
            }

        };
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
            obtainTabBackgroundColor(typedArray);
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

    /**
     * Obtains the background color of tabs from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the background color should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabBackgroundColor(@NonNull final TypedArray typedArray) {
        int defaultValue = ContextCompat.getColor(getContext(), R.color.tab_background_color);
        tabBackgroundColor =
                typedArray.getColor(R.styleable.TabSwitcher_tabBackgroundColor, defaultValue);
    }

    private float getPosition(@NonNull final Axis axis, @NonNull final View view) {
        if (axis == Axis.DRAGGING_AXIS) {
            return view.getY();
        } else {
            return view.getX();
        }
    }

    private void setPosition(@NonNull final Axis axis, @NonNull final View view,
                             final float position) {
        if (axis == Axis.DRAGGING_AXIS) {
            view.setY(position);
        } else {
            view.setX(position);
        }
    }

    private void animatePosition(@NonNull final Axis axis,
                                 @NonNull final ViewPropertyAnimator animator,
                                 final float position) {
        if (axis == Axis.DRAGGING_AXIS) {
            animator.y(position);
        } else {
            animator.x(position);
        }
    }

    private float getRotation(@NonNull final Axis axis, @NonNull final View view) {
        if (axis == Axis.DRAGGING_AXIS) {
            return view.getRotationY();
        } else {
            return view.getRotationX();
        }
    }

    private void setRotation(@NonNull final Axis axis, @NonNull final View view,
                             final float angle) {
        if (axis == Axis.DRAGGING_AXIS) {
            view.setRotationY(angle);
        } else {
            view.setRotationX(angle);
        }
    }

    private void setScale(@NonNull final Axis axis, @NonNull final View view, final float scale) {
        if (axis == Axis.DRAGGING_AXIS) {
            view.setScaleY(scale);
        } else {
            view.setScaleY(scale);
        }
    }

    private void animateScale(@NonNull final Axis axis,
                              @NonNull final ViewPropertyAnimator animator, final float scale) {
        if (axis == Axis.DRAGGING_AXIS) {
            animator.scaleY(scale);
        } else {
            animator.scaleX(scale);
        }
    }

    private void setPivot(@NonNull final Axis axis, @NonNull final View view, final float pivot) {
        if (axis == Axis.DRAGGING_AXIS) {
            view.setPivotY(pivot);
        } else {
            view.setPivotX(pivot);
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
                dragging = handleDrag(0, drag);
            }

            handleRelease(null);
            printProjectedPositions();

            dragging = true;
            drag = 0;

            while (dragging) {
                drag -= 20;
                dragging = handleDrag(0, drag);
            }

            handleRelease(null);
            printProjectedPositions();

            /*
            dragAnimation = new ShowSwitcherAnimation();
            dragAnimation.setFillAfter(true);
            dragAnimation.setAnimationListener(createDragAnimationListener());
            dragAnimation
                    .setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
            dragAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            startAnimation(dragAnimation);
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

    private Animation.AnimationListener createDragAnimationListener() {
        return new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(final Animation animation) {

            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                handleRelease(null);
                dragAnimation = null;
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {

            }

        };
    }

    private Animator.AnimatorListener createOvershootAnimationListener() {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                handleRelease(null);
                overshootAnimation = null;
            }

        };
    }

    private Animation.AnimationListener createOvershootUpAnimationListener() {
        return new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                handleRelease(null);
                overshootUpAnimation = null;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

        };
    }

    private void dragToTopThresholdPosition() {
        Iterator iterator = new Iterator();
        TabView tabView;

        while ((tabView = iterator.next()) != null) {
            ViewHolder viewHolder = tabView.viewHolder;
            Drawable backgroundDrawable =
                    ContextCompat.getDrawable(getContext(), R.drawable.tab_background);
            backgroundDrawable.setColorFilter(tabBackgroundColor, PorterDuff.Mode.MULTIPLY);
            ViewUtil.setBackground(tabView.view, backgroundDrawable);
            int padding = tabInset + tabBorderWidth;
            tabView.view.setPadding(padding, tabInset, padding, padding);
            viewHolder.titleContainer.setVisibility(View.VISIBLE);
            Drawable borderDrawable =
                    ContextCompat.getDrawable(getContext(), R.drawable.tab_border);
            borderDrawable.setColorFilter(tabBackgroundColor, PorterDuff.Mode.MULTIPLY);
            ViewUtil.setBackground(viewHolder.borderView, borderDrawable);
            calculateTopThresholdPosition(tabView, iterator.previous());
            applyTag(tabView);
        }
    }

    private void calculateTopThresholdPosition(@NonNull final TabView tabView,
                                               @Nullable final TabView previous) {
        float thresholdPosition = 0 - (tabView.index - 1) * minTabSpacing;
        clipDraggedTabPosition(thresholdPosition, tabView, previous);
    }

    private void dragToBottomThresholdPosition() {
        Iterator iterator = new Iterator();
        TabView tabView;

        while ((tabView = iterator.next()) != null) {
            calculateBottomThresholdPosition(tabView, iterator.previous());
            applyTag(tabView);
        }
    }

    private void calculateBottomThresholdPosition(@NonNull final TabView tabView,
                                                  @Nullable final TabView previous) {
        float position = (getChildCount() - tabView.index) * maxTabSpacing;
        clipDraggedTabPosition(position, tabView, previous);
    }

    private void updateTags() {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            Tag tag = (Tag) view.getTag(R.id.tag_properties);
            tag.projectedPosition = getPosition(Axis.DRAGGING_AXIS, view);
            tag.distance = 0;
        }
    }

    @SuppressWarnings("WrongConstant")
    private void applyTag(@NonNull final TabView tabView) {
        Tag tag = tabView.tag;
        float position = tag.projectedPosition;
        View view = tabView.view;
        setPosition(Axis.DRAGGING_AXIS, view, position);
        setRotation(Axis.ORTHOGONAL_AXIS, view, 0);
        view.setVisibility(getVisibility(tabView));
    }

    private int getVisibility(@NonNull final TabView tabView) {
        State state = tabView.tag.state;
        return state == State.TOP_MOST_HIDDEN || state == State.BOTTOM_MOST_HIDDEN ?
                View.INVISIBLE : View.VISIBLE;
    }

    private void calculateTabPosition(final int dragDistance, @NonNull final TabView tabView,
                                      @Nullable final TabView previous) {
        if (getChildCount() - tabView.index > 0) {
            int distance = dragDistance - tabView.tag.distance;
            tabView.tag.distance = dragDistance;

            if (distance != 0) {
                float currentPosition = tabView.tag.actualPosition;
                float newPosition = currentPosition + distance;
                clipDraggedTabPosition(newPosition, tabView, previous);

                if (scrollDirection == ScrollDirection.DRAGGING_DOWN) {
                    calculateNonLinearPositionWhenDraggingDown(distance, tabView, previous,
                            currentPosition);
                } else if (scrollDirection == ScrollDirection.DRAGGING_UP) {
                    calculateNonLinearPositionWhenDraggingUp(distance, tabView, previous,
                            currentPosition);
                }
            }
        }
    }

    private void calculateNonLinearPositionWhenDraggingDown(final int dragDistance,
                                                            @NonNull final TabView tabView,
                                                            @Nullable final TabView previous,
                                                            final float currentPosition) {
        if (previous != null && previous.tag.state == State.VISIBLE &&
                tabView.tag.state == State.VISIBLE) {
            float newPosition = calculateNonLinearPosition(dragDistance, currentPosition, tabView);
            boolean attached = false;

            if (previous.tag.projectedPosition - newPosition >= maxTabSpacing) {
                lastAttachedIndex = tabView.index;
                newPosition = previous.tag.projectedPosition - maxTabSpacing;
                attached = true;
            }

            clipDraggedTabPosition(newPosition, tabView, previous);

            if (attached && attachedPosition == 0) {
                attachedPosition = tabView.tag.projectedPosition;
            }
        }
    }

    private void calculateNonLinearPositionWhenDraggingUp(final int dragDistance,
                                                          @NonNull final TabView tabView,
                                                          @Nullable final TabView previous,
                                                          final float currentPosition) {
        if (tabView.tag.state == State.VISIBLE) {
            boolean attached = tabView.tag.projectedPosition <= attachedPosition;

            if (previous == null || !attached) {
                lastAttachedIndex = tabView.index;
            }

            if (previous != null && attached) {
                float newPosition =
                        calculateNonLinearPosition(dragDistance, currentPosition, tabView);

                if (previous.tag.state != State.STACKED_BOTTOM &&
                        previous.tag.state != State.BOTTOM_MOST_HIDDEN &&
                        previous.tag.projectedPosition - newPosition <= minTabSpacing) {
                    newPosition = previous.tag.projectedPosition - minTabSpacing;
                }

                clipDraggedTabPosition(newPosition, tabView, previous);
            }
        }
    }

    private float calculateNonLinearPosition(final int dragDistance, final float currentPosition,
                                             @NonNull final TabView tabView) {
        return currentPosition +
                (float) (dragDistance * Math.pow(0.5, tabView.index - lastAttachedIndex));
    }

    private void clipDraggedTabPosition(final float dragPosition, @NonNull final TabView tabView,
                                        @Nullable final TabView previous) {
        Pair<Float, State> topMostPair = calculateTopMostPosition(tabView, previous);
        float topMostPosition = topMostPair.first;

        if (dragPosition <= topMostPosition) {
            tabView.tag.projectedPosition = topMostPair.first;
            tabView.tag.actualPosition = dragPosition;
            tabView.tag.state = topMostPair.second;
            return;
        } else {
            Pair<Float, State> bottomMostPair = calculateBottomMostPosition(tabView);
            float bottomMostPosition = bottomMostPair.first;

            if (dragPosition >= bottomMostPosition) {
                tabView.tag.projectedPosition = bottomMostPair.first;
                tabView.tag.actualPosition = dragPosition;
                tabView.tag.state = bottomMostPair.second;
                return;
            }
        }

        tabView.tag.projectedPosition = dragPosition;
        tabView.tag.actualPosition = dragPosition;
        tabView.tag.state = State.VISIBLE;
    }

    private Pair<Float, State> calculateTopMostPosition(@NonNull final TabView tabView,
                                                        @Nullable final TabView previous) {
        if ((getCount() - tabView.index) < STACKED_TAB_COUNT) {
            float position = stackedTabSpacing * (getCount() - tabView.index);
            return Pair.create(position, State.STACKED_TOP);
        } else {
            float position = stackedTabSpacing * STACKED_TAB_COUNT;
            return Pair.create(position,
                    (previous == null || previous.tag.state == State.VISIBLE) ? State.TOP_MOST :
                            State.TOP_MOST_HIDDEN);
        }
    }

    private Pair<Float, State> calculateBottomMostPosition(@NonNull final TabView tabView) {
        if (tabView.index <= STACKED_TAB_COUNT) {
            float position = getHeight() - tabInset - (stackedTabSpacing * tabView.index);
            return Pair.create(position, State.STACKED_BOTTOM);
        } else {
            float position = getHeight() - tabInset - (stackedTabSpacing * STACKED_TAB_COUNT);
            return Pair.create(position, State.BOTTOM_MOST_HIDDEN);
        }
    }

    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        if (isSwitcherShown()) {
            if (dragAnimation != null) {
                dragAnimation.cancel();
                dragAnimation = null;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handleDown(event);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!isAnimationRunning() && event.getPointerId(0) == pointerId) {
                        if (velocityTracker == null) {
                            velocityTracker = VelocityTracker.obtain();
                        }

                        velocityTracker.addMovement(event);
                        handleDrag(event.getX(0), event.getY(0));
                    } else {
                        handleRelease(null);
                        handleDown(event);
                    }

                    return true;
                case MotionEvent.ACTION_UP:
                    if (!isAnimationRunning() && event.getPointerId(0) == pointerId) {
                        handleRelease(event);
                    }

                    return true;
                default:
                    break;
            }
        }

        return super.onTouchEvent(event);
    }

    private boolean isAnimationRunning() {
        return overshootAnimation != null || overshootUpAnimation != null ||
                closeAnimation != null || relocateAnimation != null;
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

    private void tiltOnOvershootDown(final float angle) {
        float maxCameraDistance = getMaxCameraDistance();
        float minCameraDistance = maxCameraDistance / 2f;
        int firstVisibleIndex = -1;
        Iterator iterator = new Iterator();
        TabView tabView;

        while ((tabView = iterator.next()) != null) {
            View view = tabView.view;

            if (firstVisibleIndex == -1) {
                view.setCameraDistance(minCameraDistance);

                if (tabView.tag.state == State.VISIBLE) {
                    firstVisibleIndex = tabView.index;
                }
            } else {
                int diff = tabView.index - firstVisibleIndex;
                float ratio = (float) diff / (float) (getChildCount() - firstVisibleIndex);
                view.setCameraDistance(
                        minCameraDistance + (maxCameraDistance - minCameraDistance) * ratio);
            }

            setPivot(Axis.DRAGGING_AXIS, view, maxTabSpacing);
            setRotation(Axis.ORTHOGONAL_AXIS, view, angle);
        }
    }

    private void tiltOnOvershootUp(final float angle) {
        float cameraDistance = getMaxCameraDistance();
        Iterator iterator = new Iterator();
        TabView tabView;

        while ((tabView = iterator.next()) != null) {
            View view = tabView.view;

            if (tabView.index == 1) {
                view.setVisibility(View.VISIBLE);
                view.setCameraDistance(cameraDistance);
                setPivot(Axis.DRAGGING_AXIS, view, view.getHeight() / 2f);
                setRotation(Axis.ORTHOGONAL_AXIS, view, angle);
            } else {
                view.setVisibility(View.INVISIBLE);
            }
        }
    }

    private float getMaxCameraDistance() {
        float density = getResources().getDisplayMetrics().density;
        return density * 1280;
    }

    @SuppressWarnings("WrongConstant")
    private boolean handleDrag(final float x, final float y) {
        if (y <= topDragThreshold) {
            scrollDirection = ScrollDirection.OVERSHOOT_UP;
            overshootDragHelper.update(y);
            float overshootDistance = Math.abs(overshootDragHelper.getDistance());

            if (overshootDistance <= maxOvershootDistance) {
                float ratio = Math.max(0, Math.min(1, overshootDistance / maxOvershootDistance));
                Iterator iterator = new Iterator();
                TabView tabView;

                while ((tabView = iterator.next()) != null) {
                    View view = tabView.view;

                    if (tabView.index == 1) {
                        float currentPosition = tabView.tag.projectedPosition;
                        setPosition(Axis.DRAGGING_AXIS, view,
                                currentPosition - (currentPosition * ratio));
                    } else {
                        View firstView = iterator.first().view;
                        view.setVisibility(getPosition(Axis.DRAGGING_AXIS, firstView) <=
                                getPosition(Axis.DRAGGING_AXIS, view) ? View.INVISIBLE :
                                getVisibility(tabView));
                    }
                }
            } else {
                float ratio = Math.max(0, Math.min(1,
                        (overshootDistance - maxOvershootDistance) / maxOvershootDistance));
                tiltOnOvershootUp(ratio * MAX_UP_OVERSHOOT_ANGLE);
            }
        } else if (y >= bottomDragThreshold) {
            scrollDirection = ScrollDirection.OVERSHOOT_DOWN;
            overshootDragHelper.update(y);
            float overshootDistance = overshootDragHelper.getDistance();
            float ratio = Math.max(0, Math.min(1, overshootDistance / maxOvershootDistance));
            tiltOnOvershootDown(ratio * -MAX_DOWN_OVERSHOOT_ANGLE);
        } else {
            overshootDragHelper.reset();
            int previousDistance = dragHelper.getDistance();
            dragHelper.update(y);
            closeDragHelper.update(x);

            if (scrollDirection == ScrollDirection.NONE && draggedTabView == null &&
                    closeDragHelper.hasThresholdBeenReached()) {
                TabView tabView = getDraggedTabView(dragHelper.getStartPosition());

                if (tabView != null && tabs.get(tabView.index - 1).isCloseable()) {
                    draggedTabView = tabView;
                }
            }

            if (draggedTabView == null && dragHelper.hasThresholdBeenReached()) {
                scrollDirection = previousDistance - dragHelper.getDistance() < 0 ?
                        ScrollDirection.DRAGGING_DOWN : ScrollDirection.DRAGGING_UP;
            }

            if (draggedTabView != null) {
                handleDragToClose();
            } else if (scrollDirection != ScrollDirection.NONE) {
                lastAttachedIndex = 1;
                Iterator iterator = new Iterator();
                TabView tabView;

                while ((tabView = iterator.next()) != null) {
                    calculateTabPosition(dragHelper.getDistance(), tabView, iterator.previous());
                    applyTag(tabView);
                }

                if (isBottomDragThresholdReached()) {
                    bottomDragThreshold = y;
                    scrollDirection = ScrollDirection.OVERSHOOT_DOWN;
                    dragToBottomThresholdPosition();
                } else if (isTopDragThresholdReached()) {
                    topDragThreshold = y;
                    scrollDirection = ScrollDirection.OVERSHOOT_UP;
                    // TODO: dragToTopThresholdPosition();
                }
            }

            return true;
        }

        return false;
    }

    private void handleDragToClose() {
        int dragDistance = closeDragHelper.getDistance();
        View view = draggedTabView.view;
        setPosition(Axis.ORTHOGONAL_AXIS, view, dragDistance);
        float ratio = 1 - (float) Math.abs(dragDistance) / (float) calculateClosedTabPosition();
        float scale = closedTabScale + ratio * (1 - closedTabScale);
        setScale(Axis.ORTHOGONAL_AXIS, view, scale);
        setScale(Axis.DRAGGING_AXIS, view, scale);
        view.setAlpha(closedTabAlpha + ratio * (1 - closedTabAlpha));
    }

    private int calculateClosedTabPosition() {
        return getWidth();
    }

    @Nullable
    private TabView getDraggedTabView(final float y) {
        Iterator iterator = new Iterator();
        TabView tabView;

        while ((tabView = iterator.next()) != null) {
            if ((tabView.tag.state == State.VISIBLE || tabView.tag.state == State.STACKED_TOP) &&
                    tabView.tag.projectedPosition <= y) {
                return tabView;
            }
        }

        return null;
    }

    private void handleRelease(@Nullable final MotionEvent event) {
        boolean thresholdReached = dragHelper.hasThresholdBeenReached();
        ScrollDirection flingDirection = this.scrollDirection;
        this.dragHelper.reset();
        this.overshootDragHelper.reset();
        this.closeDragHelper.reset();
        this.topDragThreshold = -Float.MAX_VALUE;
        this.bottomDragThreshold = Float.MAX_VALUE;
        this.scrollDirection = ScrollDirection.NONE;

        if (draggedTabView != null) {
            updateTags();
            float flingVelocity = 0;

            if (event != null && velocityTracker != null) {
                int pointerId = event.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                flingVelocity = Math.abs(velocityTracker.getXVelocity(pointerId));
            }

            View view = draggedTabView.view;
            boolean close = flingVelocity >= minCloseFlingVelocity ||
                    Math.abs(getPosition(Axis.ORTHOGONAL_AXIS, view)) > view.getWidth() / 4f;
            animateClose(draggedTabView, close, flingVelocity);
        } else if (flingDirection == ScrollDirection.DRAGGING_UP ||
                flingDirection == ScrollDirection.DRAGGING_DOWN) {
            updateTags();

            if (event != null && velocityTracker != null && thresholdReached) {
                animateFling(event, flingDirection);
            }
        } else if (flingDirection == ScrollDirection.OVERSHOOT_DOWN) {
            animateOvershootDown();
        } else if (flingDirection == ScrollDirection.OVERSHOOT_UP) {
            animateOvershootUp();
        } else {
            updateTags();
        }

        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private void animateOvershootDown() {
        animateTilt(new AccelerateDecelerateInterpolator(), createOvershootAnimationListener(),
                MAX_DOWN_OVERSHOOT_ANGLE);

    }

    private void animateOvershootUp() {
        boolean tilted = animateTilt(new AccelerateInterpolator(), createTiltAnimationListener(),
                MAX_UP_OVERSHOOT_ANGLE);

        if (!tilted) {
            animateOvershootUp(new AccelerateDecelerateInterpolator());
        }
    }

    private Animator.AnimatorListener createTiltAnimationListener() {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                animateOvershootUp(new DecelerateInterpolator());
                overshootAnimation = null;
            }

        };
    }

    private void animateOvershootUp(@NonNull final Interpolator interpolator) {
        long animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        overshootUpAnimation = new OvershootUpAnimation();
        overshootUpAnimation.setFillAfter(true);
        overshootUpAnimation.setDuration(
                animationDuration); // TODO: Calculate duration based on overshoot distance
        overshootUpAnimation.setInterpolator(interpolator);
        overshootUpAnimation.setAnimationListener(createOvershootUpAnimationListener());
        startAnimation(overshootUpAnimation);
    }

    private boolean animateTilt(@NonNull final Interpolator interpolator,
                                @Nullable final Animator.AnimatorListener listener,
                                final float maxAngle) {
        long animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        Iterator iterator = new Iterator(true);
        TabView tabView;
        boolean result = false;

        while ((tabView = iterator.next()) != null) {
            View view = tabView.view;

            if (getRotation(Axis.ORTHOGONAL_AXIS, view) != 0) {
                result = true;
                overshootAnimation = view.animate();
                overshootAnimation.setListener(iterator.hasNext() ? null : listener);
                overshootAnimation.setDuration(Math.round(animationDuration *
                        (Math.abs(getRotation(Axis.ORTHOGONAL_AXIS, view)) / maxAngle)));
                overshootAnimation.setInterpolator(interpolator);
                overshootAnimation.rotationX(0);
                overshootAnimation.setStartDelay(0);
                overshootAnimation.start();
            }
        }

        return result;
    }

    private void animateFling(@NonNull final MotionEvent event,
                              @NonNull final ScrollDirection flingDirection) {
        int pointerId = event.getPointerId(0);
        velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
        float flingVelocity = Math.abs(velocityTracker.getYVelocity(pointerId));

        if (flingVelocity > minFlingVelocity) {
            float flingDistance = 0.25f * flingVelocity;

            if (flingDirection == ScrollDirection.DRAGGING_UP) {
                flingDistance = -1 * flingDistance;
            }

            dragAnimation = new FlingAnimation(flingDistance);
            dragAnimation.setFillAfter(true);
            dragAnimation.setAnimationListener(createDragAnimationListener());
            dragAnimation.setDuration(Math.round(Math.abs(flingDistance) / flingVelocity * 1000));
            dragAnimation.setInterpolator(new DecelerateInterpolator());
            startAnimation(dragAnimation);
        }
    }

}