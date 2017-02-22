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
package de.mrapp.android.tabswitcher;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import de.mrapp.android.tabswitcher.model.AnimationType;
import de.mrapp.android.tabswitcher.model.Axis;
import de.mrapp.android.tabswitcher.model.DragState;
import de.mrapp.android.tabswitcher.model.Iterator;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.Tag;
import de.mrapp.android.tabswitcher.util.DragHelper;
import de.mrapp.android.tabswitcher.util.ViewRecycler;
import de.mrapp.android.tabswitcher.arithmetic.Arithmetics;
import de.mrapp.android.tabswitcher.view.ChildViewRecycler;
import de.mrapp.android.tabswitcher.view.RecyclerAdapter;
import de.mrapp.android.tabswitcher.view.TabSwitcherButton;
import de.mrapp.android.tabswitcher.view.TabViewHolder;
import de.mrapp.android.util.DisplayUtil.Orientation;
import de.mrapp.android.util.ThemeUtil;
import de.mrapp.android.util.ViewUtil;

import static de.mrapp.android.util.Condition.ensureNotNull;
import static de.mrapp.android.util.DisplayUtil.getOrientation;

/**
 * A chrome-like tab switcher.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabSwitcher extends FrameLayout implements OnGlobalLayoutListener {

    private class FlingAnimation extends Animation {

        private final float flingDistance;

        public FlingAnimation(final float flingDistance) {
            this.flingDistance = flingDistance;
        }

        @Override
        protected void applyTransformation(final float interpolatedTime, final Transformation t) {
            if (dragAnimation != null) {
                handleDrag(flingDistance * interpolatedTime, 0);
            }
        }

    }

    @Deprecated
    private static final int STACKED_TAB_COUNT = 3;

    private static final float MAX_DOWN_OVERSHOOT_ANGLE = 3f;

    private static final float MAX_UP_OVERSHOOT_ANGLE = 2f;

    private int[] padding;

    private Toolbar toolbar;

    private ViewGroup tabContainer;

    private Set<TabSwitcherListener> listeners;

    private LayoutInflater inflater;

    private ChildViewRecycler childViewRecycler;

    private ViewRecycler<TabItem, Integer> viewRecycler;

    private RecyclerAdapter recyclerAdapter;

    private TabSwitcherDecorator decorator;

    private Arithmetics arithmetics;

    private Queue<Runnable> pendingActions;

    /**
     * A list, which contains the tab switcher's tabs.
     */
    private List<Tab> tabs;

    private int selectedTabIndex;

    private int tabBackgroundColor;

    private int dragThreshold;

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

    private int tabTitleContainerHeight;

    private int tabViewBottomMargin;

    private DragState dragState;

    private TabItem draggedTabItem;

    private float dragDistance;

    private float attachedPosition;

    private int firstVisibleIndex;

    private float topDragThreshold = -Float.MAX_VALUE;

    private float bottomDragThreshold = Float.MAX_VALUE;

    private int pointerId = -1;

    private Animation dragAnimation;

    private ViewPropertyAnimator toolbarAnimation;

    private int runningAnimations;

    private void applyTag(@NonNull final TabItem tabItem) {
        float position = tabItem.getTag().getPosition();
        View view = tabItem.getView();
        view.setAlpha(1f);
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getDefaultPivot(Axis.DRAGGING_AXIS, view));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getDefaultPivot(Axis.ORTHOGONAL_AXIS, view));
        arithmetics.setPosition(Axis.DRAGGING_AXIS, view, position);
        arithmetics.setPosition(Axis.ORTHOGONAL_AXIS, view, 0);
        arithmetics.setRotation(Axis.ORTHOGONAL_AXIS, view, 0);
    }

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
        getViewTreeObserver().addOnGlobalLayoutListener(this);
        runningAnimations = 0;
        inflater = LayoutInflater.from(getContext());
        padding = new int[]{0, 0, 0, 0};
        listeners = new LinkedHashSet<>();
        pendingActions = new LinkedList<>();
        tabs = new ArrayList<>();
        selectedTabIndex = -1;
        switcherShown = false;
        Resources resources = getResources();
        dragThreshold = resources.getDimensionPixelSize(R.dimen.drag_threshold);
        dragHelper = new DragHelper(dragThreshold);
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
        tabTitleContainerHeight =
                resources.getDimensionPixelSize(R.dimen.tab_title_container_height);
        tabViewBottomMargin = -1;
        dragState = DragState.NONE;
        inflateLayout();
        childViewRecycler = new ChildViewRecycler(inflater);
        recyclerAdapter = new RecyclerAdapter(this, childViewRecycler);
        viewRecycler = new ViewRecycler<>(tabContainer, recyclerAdapter, inflater,
                Collections.reverseOrder(TabItem.COMPARATOR));
        recyclerAdapter.setViewRecycler(viewRecycler);
        arithmetics = new Arithmetics(this);
        obtainStyledAttributes(attributeSet, defaultStyle, defaultStyleResource);
    }

    private void inflateLayout() {
        toolbar = (Toolbar) inflater.inflate(R.layout.tab_switcher_toolbar, this, false);
        toolbar.setVisibility(View.INVISIBLE);
        addView(toolbar, LayoutParams.MATCH_PARENT,
                ThemeUtil.getDimensionPixelSize(getContext(), R.attr.actionBarSize));
        tabContainer = new FrameLayout(getContext());
        addView(tabContainer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    }

    private void notifyOnSwitcherShown() {
        for (TabSwitcherListener listener : listeners) {
            listener.onSwitcherShown(this);
        }
    }

    private void notifyOnSwitcherHidden() {
        for (TabSwitcherListener listener : listeners) {
            listener.onSwitcherHidden(this);
        }
    }

    private void notifyOnSelectionChanged(final int selectedTabIndex,
                                          @Nullable final Tab selectedTab) {
        for (TabSwitcherListener listener : listeners) {
            listener.onSelectionChanged(this, selectedTabIndex, selectedTab);
        }
    }

    private void notifyOnTabAdded(final int index, @NonNull final Tab tab) {
        for (TabSwitcherListener listener : listeners) {
            listener.onTabAdded(this, index, tab);
        }
    }

    private void notifyOnTabRemoved(final int index, @NonNull final Tab tab) {
        for (TabSwitcherListener listener : listeners) {
            listener.onTabRemoved(this, index, tab);
        }
    }

    private void notifyOnAllTabsRemoved() {
        for (TabSwitcherListener listener : listeners) {
            listener.onAllTabsRemoved(this);
        }
    }

    private void animateOrthogonalDrag(@NonNull final TabItem tabItem, final boolean close,
                                       final float flingVelocity, final long startDelay,
                                       @Nullable final AnimatorListener listener) {
        View view = tabItem.getView();
        float scale = arithmetics.getScale(view, true);
        float closedTabPosition = calculateClosedTabPosition();
        float position = arithmetics.getPosition(Axis.ORTHOGONAL_AXIS, view);
        float targetPosition =
                close ? (position < 0 ? -1 * closedTabPosition : closedTabPosition) : 0;
        float distance = Math.abs(targetPosition - position);
        long animationDuration;

        if (flingVelocity >= minCloseFlingVelocity) {
            animationDuration = Math.round((distance / flingVelocity) * 1000);
        } else {
            animationDuration = Math.round(
                    getResources().getInteger(android.R.integer.config_longAnimTime) *
                            (distance / closedTabPosition));
        }

        ViewPropertyAnimator animation = view.animate();
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setListener(createAnimationListenerWrapper(listener));
        animation.setDuration(animationDuration);
        arithmetics.animatePosition(Axis.ORTHOGONAL_AXIS, animation, view, targetPosition, true);
        arithmetics.animateScale(Axis.ORTHOGONAL_AXIS, animation,
                close ? closedTabScale * scale : scale);
        arithmetics.animateScale(Axis.DRAGGING_AXIS, animation,
                close ? closedTabScale * scale : scale);
        animation.alpha(close ? closedTabAlpha : 1);
        animation.setStartDelay(startDelay);
        animation.start();
    }

    private AnimatorListener createCloseAnimationListener(@NonNull final TabItem closedTabItem,
                                                          final boolean close) {
        return new AnimatorListenerAdapter() {

            private void relocateWhenStackedTabViewWasRemoved(final boolean top) {
                long delay = getResources().getInteger(android.R.integer.config_shortAnimTime);
                int start = closedTabItem.getIndex() + (top ? -1 : 1);
                Iterator iterator =
                        new Iterator.Builder(TabSwitcher.this, viewRecycler).reverse(top)
                                .start(closedTabItem.getIndex()).create();
                TabItem tabItem;
                Float previousProjectedPosition = null;

                while ((tabItem = iterator.next()) != null &&
                        (tabItem.getTag().getState() == State.HIDDEN ||
                                tabItem.getTag().getState() == State.STACKED_START ||
                                tabItem.getTag().getState() == State.STACKED_END)) {
                    float projectedPosition = tabItem.getTag().getPosition();

                    if (previousProjectedPosition != null) {
                        if (tabItem.getTag().getState() == State.HIDDEN) {
                            TabItem previous = iterator.previous();
                            tabItem.getTag().setState(previous.getTag().getState());

                            if (top) {
                                tabItem.getTag().setPosition(previousProjectedPosition);
                            }

                            if (tabItem.isVisible()) {
                                Pair<Float, State> pair =
                                        top ? calculateTopMostPositionAndState(previous, tabItem) :
                                                calculateBottomMostPositionAndState(previous);
                                tabItem.getTag().setPosition(pair.first);
                                tabItem.getTag().setState(pair.second);
                                inflateTabView(tabItem, null);
                            }

                            break;
                        } else {
                            tabItem.getTag().setPosition(previousProjectedPosition);
                            long startDelay = (top ? (start + 1 - tabItem.getIndex()) :
                                    (tabItem.getIndex() - start)) * delay;
                            animateRelocate(tabItem, previousProjectedPosition, null, startDelay,
                                    createRelocateAnimationListener(tabItem));
                        }
                    }

                    previousProjectedPosition = projectedPosition;
                }
            }

            private void relocateWhenVisibleTabViewWasRemoved() {
                if (closedTabItem.getIndex() > 0) {
                    long delay = getResources().getInteger(android.R.integer.config_shortAnimTime);
                    Iterator iterator =
                            new Iterator.Builder(TabSwitcher.this, viewRecycler).reverse(true)
                                    .start(closedTabItem.getIndex()).create();
                    TabItem tabItem;
                    Tag previousTag = null;
                    boolean abort = false;

                    while ((tabItem = iterator.next()) != null && !abort) {
                        Tag currentTag = tabItem.getTag().clone();

                        if (previousTag != null) {
                            if (tabItem.getTag().getState() != State.FLOATING) {
                                abort = true;
                            }

                            float relocatePosition = previousTag.getPosition();
                            long startDelay =
                                    (closedTabItem.getIndex() - tabItem.getIndex()) * delay;
                            AnimatorListener relocateAnimationListener =
                                    createRelocateAnimationListener(tabItem);
                            AnimatorListener listener =
                                    tabItem.getIndex() == closedTabItem.getIndex() - 1 ?
                                            createRelocateAnimationListenerWrapper(closedTabItem,
                                                    relocateAnimationListener) :
                                            relocateAnimationListener;

                            if (tabItem.isInflated()) {
                                animateRelocate(tabItem, relocatePosition, previousTag, startDelay,
                                        listener);
                            } else {
                                Pair<Float, State> pair =
                                        calculateBottomMostPositionAndState(tabItem);
                                tabItem.getTag().setPosition(pair.first);
                                tabItem.getTag().setState(pair.second);
                                inflateTabView(tabItem,
                                        createRelocateLayoutListener(tabItem, relocatePosition,
                                                previousTag, startDelay, listener));
                                tabItem.getView().setVisibility(View.INVISIBLE);
                            }
                        }

                        previousTag = currentTag;
                        previousTag.setClosing(false);
                    }
                }
            }

            private OnGlobalLayoutListener createRelocateLayoutListener(
                    @NonNull final TabItem tabItem, final float relocatePosition,
                    @Nullable final Tag tag, final long startDelay,
                    @Nullable final AnimatorListener listener) {
                return new OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        animateRelocate(tabItem, relocatePosition, tag, startDelay, listener);
                    }

                };
            }

            private void animateRelocate(@NonNull final TabItem tabItem,
                                         final float relocatePosition, @Nullable final Tag tag,
                                         final long startDelay,
                                         @Nullable final AnimatorListener listener) {
                if (tag != null) {
                    tabItem.getView().setTag(R.id.tag_properties, tag);
                    tabItem.setTag(tag);
                }

                View view = tabItem.getView();
                ViewPropertyAnimator animation = view.animate();
                animation.setListener(createAnimationListenerWrapper(listener));
                animation.setInterpolator(new AccelerateDecelerateInterpolator());
                animation.setDuration(
                        getResources().getInteger(android.R.integer.config_mediumAnimTime));
                arithmetics.animatePosition(Axis.DRAGGING_AXIS, animation, view, relocatePosition,
                        true);
                animation.setStartDelay(startDelay);
                animation.start();
            }

            @Override
            public void onAnimationStart(final Animator animation) {
                super.onAnimationStart(animation);

                if (close) {
                    if (closedTabItem.getTag().getState() == State.STACKED_END) {
                        relocateWhenStackedTabViewWasRemoved(false);
                    } else if (closedTabItem.getTag().getState() == State.STACKED_START) {
                        relocateWhenStackedTabViewWasRemoved(true);
                    } else {
                        relocateWhenVisibleTabViewWasRemoved();
                    }
                }
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);

                if (close) {
                    int index = closedTabItem.getIndex();
                    viewRecycler.remove(closedTabItem);
                    Tab tab = tabs.remove(index);
                    tab.removeCallback(recyclerAdapter);
                    notifyOnTabRemoved(index, tab);

                    if (isEmpty()) {
                        selectedTabIndex = -1;
                        notifyOnSelectionChanged(-1, null);
                        animateToolbarVisibility(isToolbarShown(), 0);
                    } else if (selectedTabIndex == closedTabItem.getIndex()) {
                        if (selectedTabIndex > 0) {
                            selectedTabIndex--;
                        }

                        notifyOnSelectionChanged(selectedTabIndex, getTab(selectedTabIndex));
                    }
                } else {
                    View view = closedTabItem.getView();
                    adaptTopMostTabViewWhenClosingAborted(closedTabItem,
                            closedTabItem.getIndex() + 1);
                    closedTabItem.getTag().setClosing(false);
                    arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                            arithmetics.getDefaultPivot(Axis.DRAGGING_AXIS, view));
                    handleRelease(null);
                    animateToolbarVisibility(true, 0);
                }

                draggedTabItem = null;
            }

        };
    }

    private AnimatorListener createRelocateAnimationListenerWrapper(
            @NonNull final TabItem closedTabItem, @Nullable final AnimatorListener listener) {
        return new AnimatorListenerAdapter() {

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
                adaptTopMostTabViewWhenClosingAborted(closedTabItem, closedTabItem.getIndex());

                if (listener != null) {
                    listener.onAnimationEnd(animation);
                }
            }

        };
    }

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

                if (tabItem.isVisible()) {
                    applyTag(tabItem);
                } else {
                    viewRecycler.remove(tabItem);
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

    public final boolean isDraggingHorizontally() {
        return getOrientation(getContext()) == Orientation.LANDSCAPE;
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
        addTab(tab, getCount());
    }

    public final void addTab(@NonNull final Tab tab, final int index) {
        addTab(tab, index, AnimationType.SWIPE_RIGHT);
    }

    // TODO: Add support for adding tab, while switcher is shown
    public final void addTab(@NonNull final Tab tab, final int index,
                             @NonNull final AnimationType animationType) {
        ensureNotNull(tab, "The tab may not be null");
        ensureNotNull(animationType, "The animation type may not be null");
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                tabs.add(index, tab);
                tab.addCallback(recyclerAdapter);
                notifyOnTabAdded(index, tab);

                if (getCount() == 1) {
                    selectedTabIndex = 0;
                    notifyOnSelectionChanged(0, tab);
                }

                if (!isSwitcherShown()) {
                    toolbar.setAlpha(0);

                    if (selectedTabIndex == index && ViewCompat.isLaidOut(TabSwitcher.this)) {
                        viewRecycler.inflate(TabItem.create(TabSwitcher.this, viewRecycler, index));
                    }
                } else {
                    TabItem tabItem = TabItem.create(TabSwitcher.this, viewRecycler, index);
                    tabItem.getView().getViewTreeObserver().addOnGlobalLayoutListener(
                            createAddTabViewLayoutListener(tabItem, animationType));
                }
            }

        });
    }

    private OnGlobalLayoutListener createAddTabViewLayoutListener(@NonNull final TabItem tabItem,
                                                                  @NonNull final AnimationType animationType) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                View view = tabItem.getView();
                ViewUtil.removeOnGlobalLayoutListener(view.getViewTreeObserver(), this);
                view.setVisibility(View.VISIBLE);
                view.setAlpha(closedTabAlpha);
                float closedPosition = calculateClosedTabPosition();
                float dragPosition = arithmetics.getPosition(Axis.DRAGGING_AXIS,
                        tabContainer.getChildAt(getChildIndex(tabItem.getIndex())));
                float scale = arithmetics.getScale(view, true);
                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getDefaultPivot(Axis.DRAGGING_AXIS, view));
                arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                        arithmetics.getDefaultPivot(Axis.ORTHOGONAL_AXIS, view));
                arithmetics.setPosition(Axis.ORTHOGONAL_AXIS, view,
                        animationType == AnimationType.SWIPE_LEFT ? -1 * closedPosition :
                                closedPosition);
                arithmetics.setPosition(Axis.DRAGGING_AXIS, view, dragPosition);
                arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, scale);
                arithmetics.setScale(Axis.DRAGGING_AXIS, view, scale);
                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getPivotWhenClosing(Axis.DRAGGING_AXIS, view));
                arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                        arithmetics.getPivotWhenClosing(Axis.ORTHOGONAL_AXIS, view));
                arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, closedTabScale * scale);
                arithmetics.setScale(Axis.DRAGGING_AXIS, view, closedTabScale * scale);
                animateOrthogonalDrag(tabItem, false, 0, 0, createAddAnimationListener(tabItem));
            }

        };
    }

    private AnimatorListener createAddAnimationListener(@NonNull final TabItem tabItem) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                applyTag(tabItem);
            }

        };
    }

    public final void removeTab(@NonNull final Tab tab) {
        ensureNotNull(tab, "The tab may not be null");
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                int index = indexOfOrThrowException(tab);
                TabItem tabItem = TabItem.create(TabSwitcher.this, viewRecycler, index);

                if (!isSwitcherShown()) {
                    viewRecycler.remove(tabItem);
                    Tab tab = tabs.remove(index);
                    tab.removeCallback(recyclerAdapter);
                    notifyOnTabRemoved(index, tab);

                    if (isEmpty()) {
                        selectedTabIndex = -1;
                        notifyOnSelectionChanged(-1, null);
                        toolbar.setAlpha(isToolbarShown() ? 1 : 0);
                    } else if (selectedTabIndex == index) {
                        if (selectedTabIndex > 0) {
                            selectedTabIndex--;
                        }

                        viewRecycler.inflate(
                                TabItem.create(TabSwitcher.this, viewRecycler, selectedTabIndex));
                        notifyOnSelectionChanged(selectedTabIndex, getTab(selectedTabIndex));
                    }
                } else {
                    adaptTopMostTabViewWhenClosing(tabItem, tabItem.getIndex() + 1);
                    tabItem.getTag().setClosing(true);

                    if (tabItem.isInflated()) {
                        animateClose(tabItem);
                    } else {
                        inflateTabView(tabItem, createCloseTabViewLayoutListener(tabItem));
                    }
                }
            }

        });
    }

    private OnGlobalLayoutListener createCloseTabViewLayoutListener(
            @NonNull final TabItem tabItem) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                animateClose(tabItem);
            }

        };
    }

    private void animateClose(@NonNull final TabItem tabItem) {
        View view = tabItem.getView();
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getPivotWhenClosing(Axis.DRAGGING_AXIS, view));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getPivotWhenClosing(Axis.ORTHOGONAL_AXIS, view));
        animateOrthogonalDrag(tabItem, true, 0, 0, createCloseAnimationListener(tabItem, true));
    }

    public final void clear() {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                if (!isSwitcherShown()) {
                    for (int i = tabs.size() - 1; i >= 0; i--) {
                        Tab tab = tabs.remove(i);
                        tab.removeCallback(recyclerAdapter);
                    }

                    selectedTabIndex = -1;
                    viewRecycler.removeAll();
                    notifyOnSelectionChanged(-1, null);
                    notifyOnAllTabsRemoved();
                    toolbar.setAlpha(isToolbarShown() ? 1 : 0);
                } else {
                    Iterator iterator =
                            new Iterator.Builder(TabSwitcher.this, viewRecycler).reverse(true)
                                    .create();
                    TabItem tabItem;
                    int startDelay = 0;

                    while ((tabItem = iterator.next()) != null) {
                        TabItem previous = iterator.previous();

                        if (tabItem.getTag().getState() == State.FLOATING || previous != null &&
                                previous.getTag().getState() == State.FLOATING) {
                            startDelay += getResources()
                                    .getInteger(android.R.integer.config_shortAnimTime);
                        }

                        if (tabItem.isInflated()) {
                            animateOrthogonalDrag(tabItem, true, 0, startDelay,
                                    !iterator.hasNext() ? createClearAnimationListener() : null);
                        }
                    }
                }
            }

        });
    }

    private void animateToolbarVisibility(final boolean visible, final long startDelay) {
        if (toolbarAnimation != null) {
            toolbarAnimation.cancel();
        }

        float targetAlpha = visible ? 1 : 0;

        if (toolbar.getAlpha() != targetAlpha) {
            toolbarAnimation = toolbar.animate();
            toolbarAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            toolbarAnimation.setDuration(
                    getResources().getInteger(android.R.integer.config_mediumAnimTime));
            toolbarAnimation.setStartDelay(startDelay);
            toolbarAnimation.alpha(targetAlpha);
            toolbarAnimation.start();
        }
    }

    private AnimatorListener createClearAnimationListener() {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                for (int i = tabs.size() - 1; i >= 0; i--) {
                    Tab tab = tabs.remove(i);
                    tab.removeCallback(recyclerAdapter);
                }

                selectedTabIndex = -1;
                notifyOnAllTabsRemoved();
                notifyOnSelectionChanged(-1, null);
                animateToolbarVisibility(isToolbarShown(), 0);
            }

        };
    }

    public final void selectTab(@NonNull final Tab tab) {
        ensureNotNull(tab, "The tab may not be null");
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                int index = indexOfOrThrowException(tab);

                if (!isSwitcherShown()) {
                    viewRecycler.remove(TabItem
                            .create(TabSwitcher.this, viewRecycler, selectedTabIndex));
                    viewRecycler.inflate(TabItem.create(TabSwitcher.this, viewRecycler, index));
                    selectedTabIndex = index;
                    notifyOnSelectionChanged(index, tab);
                } else {
                    selectedTabIndex = index;
                    hideSwitcher();
                }
            }

        });
    }

    @Deprecated
    private int getChildIndex(final int index) {
        return getCount() - (index + 1);
    }

    private void enqueuePendingAction(@NonNull final Runnable action) {
        pendingActions.add(action);
        executePendingAction();
    }

    private void executePendingAction() {
        if (!isAnimationRunning()) {
            final Runnable action = pendingActions.poll();

            if (action != null) {
                new Runnable() {

                    @Override
                    public void run() {
                        action.run();
                        executePendingAction();
                    }

                }.run();
            }
        }
    }

    @Nullable
    public final Tab getSelectedTab() {
        return selectedTabIndex != -1 ? getTab(selectedTabIndex) : null;
    }

    public final int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public final boolean isEmpty() {
        return getCount() == 0;
    }

    public final int getCount() {
        return tabs.size();
    }

    public final Tab getTab(final int index) {
        return tabs.get(index);
    }

    public final int indexOf(@NonNull final Tab tab) {
        ensureNotNull(tab, "The tab may not be null");
        return tabs.indexOf(tab);
    }

    private int indexOfOrThrowException(@NonNull final Tab tab) {
        int index = indexOf(tab);

        if (index == -1) {
            throw new NoSuchElementException("No such tab: " + tab);
        }

        return index;
    }

    public final boolean isSwitcherShown() {
        return switcherShown;
    }

    private int calculateTabViewBottomMargin(@NonNull final View view) {
        Axis axis = isDraggingHorizontally() ? Axis.ORTHOGONAL_AXIS : Axis.DRAGGING_AXIS;
        float tabHeight = (view.getHeight() - 2 * tabInset) * arithmetics.getScale(view, true);
        float totalHeight = arithmetics.getSize(axis, tabContainer);
        int toolbarHeight = isToolbarShown() ? toolbar.getHeight() - tabInset : 0;
        int stackHeight = isDraggingHorizontally() ? 0 : STACKED_TAB_COUNT * stackedTabSpacing;
        return Math.round(tabHeight + tabInset + toolbarHeight + stackHeight -
                (totalHeight - getPaddingTop() - getPaddingBottom()));
    }

    private OnGlobalLayoutListener createShowSwitcherLayoutListener(
            @NonNull final TabItem tabItem) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                ViewUtil.removeOnGlobalLayoutListener(tabItem.getView().getViewTreeObserver(),
                        this);
                animateShowSwitcher(tabItem);
            }

        };
    }

    private OnGlobalLayoutListener createInflateTabViewLayoutListener(
            @NonNull final TabItem tabItem, @Nullable final OnGlobalLayoutListener layoutListener) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                View view = tabItem.getView();
                ViewUtil.removeOnGlobalLayoutListener(view.getViewTreeObserver(), this);
                adaptTabViewSize(tabItem);
                applyTag(tabItem);

                if (layoutListener != null) {
                    layoutListener.onGlobalLayout();
                }
            }

        };
    }

    private void adaptTabViewSize(@NonNull final TabItem tabItem) {
        View view = tabItem.getView();
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getDefaultPivot(Axis.DRAGGING_AXIS, view));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getDefaultPivot(Axis.ORTHOGONAL_AXIS, view));
        float scale = arithmetics.getScale(view, true);
        arithmetics.setScale(Axis.DRAGGING_AXIS, view, scale);
        arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, scale);
    }

    private void animateShowSwitcher(@NonNull final TabItem tabItem) {
        View view = tabItem.getView();
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getDefaultPivot(Axis.DRAGGING_AXIS, view));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getDefaultPivot(Axis.ORTHOGONAL_AXIS, view));
        float scale = arithmetics.getScale(view, true);

        if (tabItem.getIndex() < selectedTabIndex) {
            arithmetics.setPosition(Axis.DRAGGING_AXIS, view,
                    arithmetics.getSize(Axis.DRAGGING_AXIS, tabContainer));
        } else if (tabItem.getIndex() > selectedTabIndex) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            arithmetics.setPosition(Axis.DRAGGING_AXIS, view,
                    isDraggingHorizontally() ? 0 : layoutParams.topMargin);
        }

        if (tabViewBottomMargin == -1) {
            tabViewBottomMargin = calculateTabViewBottomMargin(view);
        }

        long animationDuration = getResources().getInteger(android.R.integer.config_longAnimTime);
        animateMargin(view, calculateTabViewBottomMargin(view), animationDuration);
        ViewPropertyAnimator animation = view.animate();
        animation.setDuration(animationDuration);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setListener(
                createAnimationListenerWrapper(createShowSwitcherAnimationListener(tabItem)));
        arithmetics.animateScale(Axis.DRAGGING_AXIS, animation, scale);
        arithmetics.animateScale(Axis.ORTHOGONAL_AXIS, animation, scale);
        arithmetics.animatePosition(Axis.DRAGGING_AXIS, animation, view,
                tabItem.getTag().getPosition(), true);
        arithmetics.animatePosition(Axis.ORTHOGONAL_AXIS, animation, view, 0, true);
        animation.setStartDelay(0);
        animation.start();
        animateToolbarVisibility(isToolbarShown(),
                getResources().getInteger(android.R.integer.config_shortAnimTime));
    }

    private void animateHideSwitcher(@NonNull final TabItem tabItem) {
        View view = tabItem.getView();
        long animationDuration = getResources().getInteger(android.R.integer.config_longAnimTime);
        animateMargin(view, -(tabInset + tabBorderWidth), animationDuration);
        ViewPropertyAnimator animation = view.animate();
        animation.setDuration(animationDuration);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setListener(
                createAnimationListenerWrapper(createHideSwitcherAnimationListener(tabItem)));
        arithmetics.animateScale(Axis.DRAGGING_AXIS, animation, 1);
        arithmetics.animateScale(Axis.ORTHOGONAL_AXIS, animation, 1);
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        arithmetics.animatePosition(Axis.ORTHOGONAL_AXIS, animation, view,
                isDraggingHorizontally() ? layoutParams.topMargin : 0, false);

        if (tabItem.getIndex() < selectedTabIndex) {
            arithmetics.animatePosition(Axis.DRAGGING_AXIS, animation, view,
                    arithmetics.getSize(Axis.DRAGGING_AXIS, this), false);
        } else if (tabItem.getIndex() > selectedTabIndex) {
            arithmetics.animatePosition(Axis.DRAGGING_AXIS, animation, view,
                    isDraggingHorizontally() ? 0 : layoutParams.topMargin, false);
        } else {
            arithmetics.animatePosition(Axis.DRAGGING_AXIS, animation, view,
                    isDraggingHorizontally() ? 0 : layoutParams.topMargin, false);
        }

        animation.setStartDelay(0);
        animation.start();
        animateToolbarVisibility(isToolbarShown() && isEmpty(), 0);
    }

    private AnimatorListener createAnimationListenerWrapper(
            @Nullable final AnimatorListener listener) {
        return new AnimatorListenerAdapter() {

            private void endAnimation() {
                if (--runningAnimations == 0) {
                    executePendingAction();
                }
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

        };
    }

    // TODO: Calling this method should also work when the view is not yet inflated
    // TODO: Should this be executed as a pending action?
    @SuppressWarnings("WrongConstant")
    public final void showSwitcher() {
        if (!isSwitcherShown() && !isAnimationRunning()) {
            switcherShown = true;
            dragDistance = 0;
            firstVisibleIndex = -1;
            attachedPosition = calculateAttachedPosition();
            notifyOnSwitcherShown();
            Iterator iterator = new Iterator.Builder(this, viewRecycler).create();
            TabItem tabItem;

            while ((tabItem = iterator.next()) != null) {
                calculateAndClipTopThresholdPosition(tabItem, iterator.previous());

                if (tabItem.getIndex() == selectedTabIndex || tabItem.isVisible()) {
                    viewRecycler.inflate(tabItem);
                    View view = tabItem.getView();

                    if (!ViewCompat.isLaidOut(view)) {
                        view.getViewTreeObserver().addOnGlobalLayoutListener(
                                createShowSwitcherLayoutListener(tabItem));
                    } else {
                        animateShowSwitcher(tabItem);
                    }
                }
            }
        }
    }

    private void animateMargin(@NonNull final View view, final int targetMargin,
                               final long animationDuration) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        int initialMargin = layoutParams.bottomMargin;
        ValueAnimator animation = ValueAnimator.ofInt(targetMargin - initialMargin);
        animation.setDuration(animationDuration);
        animation.addListener(createAnimationListenerWrapper(null));
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setStartDelay(0);
        animation.addUpdateListener(createMarginAnimatorUpdateListener(view, initialMargin));
        animation.start();

    }

    private AnimatorUpdateListener createMarginAnimatorUpdateListener(@NonNull final View view,
                                                                      final int initialMargin) {
        return new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
                layoutParams.bottomMargin = initialMargin + (int) animation.getAnimatedValue();
                view.setLayoutParams(layoutParams);
            }

        };
    }

    private AnimatorUpdateListener createOvershootUpAnimatorUpdateListener() {
        return new AnimatorUpdateListener() {

            private Float startPosition;

            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                Iterator iterator = new Iterator.Builder(TabSwitcher.this, viewRecycler).create();
                TabItem tabItem;

                while ((tabItem = iterator.next()) != null) {
                    if (tabItem.getIndex() == 0) {
                        View view = tabItem.getView();

                        if (startPosition == null) {
                            startPosition = arithmetics.getPosition(Axis.DRAGGING_AXIS, view);
                        }

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

        };
    }

    // TODO: Calling this method should also work when the view is not yet inflated
    // TODO: Should this be executed as a pending action?
    public final void hideSwitcher() {
        if (isSwitcherShown() && !isAnimationRunning()) {
            switcherShown = false;
            notifyOnSwitcherHidden();
            tabViewBottomMargin = -1;
            recyclerAdapter.clearCachedPreviews();
            Iterator iterator = new Iterator.Builder(this, viewRecycler).create();
            TabItem tabItem;

            while ((tabItem = iterator.next()) != null) {
                if (tabItem.isInflated()) {
                    animateHideSwitcher(tabItem);
                } else if (tabItem.getIndex() == selectedTabIndex) {
                    inflateTabView(tabItem, createHideSwitcherLayoutListener(tabItem));
                }
            }
        }
    }

    private OnGlobalLayoutListener createHideSwitcherLayoutListener(
            @NonNull final TabItem tabItem) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                animateHideSwitcher(tabItem);
            }

        };
    }

    public final void toggleSwitcherVisibility() {
        if (switcherShown) {
            hideSwitcher();
        } else {
            showSwitcher();
        }
    }

    private AnimatorListener createShowSwitcherAnimationListener(@NonNull final TabItem tabItem) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                applyTag(tabItem);
            }

        };
    }

    private AnimatorListener createHideSwitcherAnimationListener(@NonNull final TabItem tabItem) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                if (tabItem.getIndex() == selectedTabIndex) {
                    viewRecycler.inflate(tabItem);
                } else {
                    viewRecycler.remove(tabItem);
                    viewRecycler.clearCache();
                }
            }

        };
    }

    private AnimationListener createDragAnimationListener() {
        return new AnimationListener() {

            @Override
            public void onAnimationStart(final Animation animation) {

            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                handleRelease(null);
                dragAnimation = null;
                executePendingAction();
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {

            }

        };
    }

    private AnimatorListener createOvershootAnimationListenerWrapper(@NonNull final View view,
                                                                     @Nullable final AnimatorListener listener) {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getDefaultPivot(Axis.DRAGGING_AXIS, view));
                arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                        arithmetics.getDefaultPivot(Axis.DRAGGING_AXIS, view));

                if (listener != null) {
                    listener.onAnimationEnd(animation);
                }
            }

        };
    }

    private AnimatorListener createOvershootAnimationListener() {
        return new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                handleRelease(null);
            }

        };
    }

    private void calculateAndClipTopThresholdPosition(@NonNull final TabItem tabItem,
                                                      @Nullable final TabItem previous) {
        float position = calculateTopThresholdPosition(tabItem, previous);
        clipDraggedTabPosition(position, tabItem, previous);
    }

    private float calculateTopThresholdPosition(@NonNull final TabItem tabItem,
                                                @Nullable final TabItem previous) {
        if (previous == null) {
            return calculateFirstTabTopThresholdPosition();
        } else {
            return -1;
        }
    }

    private float calculateFirstTabTopThresholdPosition() {
        return getCount() > STACKED_TAB_COUNT ? STACKED_TAB_COUNT * stackedTabSpacing :
                (getCount() - 1) * stackedTabSpacing;
    }

    private float calculateBottomThresholdPosition(@NonNull final TabItem tabItem) {
        return (getCount() - (tabItem.getIndex() + 1)) * maxTabSpacing;
    }

    private void clipDraggedTabPosition(final float dragPosition, @NonNull final TabItem tabItem,
                                        @Nullable final TabItem previous) {
        Pair<Float, State> topMostPair = calculateTopMostPositionAndState(tabItem, previous);
        float topMostPosition = topMostPair.first;

        if (dragPosition <= topMostPosition) {
            tabItem.getTag().setPosition(topMostPosition);
            tabItem.getTag().setState(topMostPair.second);
            return;
        } else {
            Pair<Float, State> bottomMostPair = calculateBottomMostPositionAndState(tabItem);
            float bottomMostPosition = bottomMostPair.first;

            if (dragPosition >= bottomMostPosition) {
                tabItem.getTag().setPosition(bottomMostPosition);
                tabItem.getTag().setState(bottomMostPair.second);
                return;
            }
        }

        tabItem.getTag().setPosition(dragPosition);
        tabItem.getTag().setState(State.FLOATING);
    }

    private Pair<Float, State> calculateTopMostPositionAndState(@NonNull final TabItem tabItem,
                                                                @Nullable final TabItem previous) {
        if ((getCount() - tabItem.getIndex()) <= STACKED_TAB_COUNT) {
            float position = stackedTabSpacing * (getCount() - (tabItem.getIndex() + 1));
            return Pair.create(position,
                    (previous == null || previous.getTag().getState() == State.FLOATING) ?
                            State.STACKED_START_ATOP : State.STACKED_START);
        } else {
            float position = stackedTabSpacing * STACKED_TAB_COUNT;
            return Pair.create(position,
                    (previous == null || previous.getTag().getState() == State.FLOATING) ?
                            State.STACKED_START_ATOP : State.HIDDEN);
        }
    }

    private Pair<Float, State> calculateBottomMostPositionAndState(@NonNull final TabItem tabItem) {
        float size = arithmetics.getSize(Axis.DRAGGING_AXIS, tabContainer);
        int toolbarHeight =
                isToolbarShown() && !isDraggingHorizontally() ? toolbar.getHeight() - tabInset : 0;
        int padding = arithmetics.getPadding(Axis.DRAGGING_AXIS, Gravity.START, this) +
                arithmetics.getPadding(Axis.DRAGGING_AXIS, Gravity.END, this);
        int offset = isDraggingHorizontally() ? STACKED_TAB_COUNT * stackedTabSpacing : 0;

        if (tabItem.getIndex() < STACKED_TAB_COUNT) {
            float position = size - toolbarHeight - tabInset -
                    (stackedTabSpacing * (tabItem.getIndex() + 1)) -
                    padding + offset;
            return Pair.create(position, State.STACKED_END);
        } else {
            float position =
                    size - toolbarHeight - tabInset - (stackedTabSpacing * STACKED_TAB_COUNT) -
                            padding + offset;
            return Pair.create(position, State.HIDDEN);
        }
    }

    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        if (isSwitcherShown() && !isEmpty()) {
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
                        handleDrag(arithmetics.getPosition(Axis.DRAGGING_AXIS, event),
                                arithmetics.getPosition(Axis.ORTHOGONAL_AXIS, event));
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
        return runningAnimations > 0;
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
        if (getCount() <= 1) {
            return true;
        } else {
            TabItem tabItem = TabItem.create(this, viewRecycler, 0);
            return tabItem.getTag().getState() == State.STACKED_START_ATOP;
        }
    }

    private boolean isBottomDragThresholdReached() {
        if (getCount() <= 1) {
            return true;
        } else {
            TabItem tabItem = TabItem.create(this, viewRecycler, getCount() - 2);
            return tabItem.getTag().getPosition() >= maxTabSpacing;
        }
    }

    private void tiltOnOvershootDown(final float angle) {
        float maxCameraDistance = getMaxCameraDistance();
        float minCameraDistance = maxCameraDistance / 2f;
        int firstVisibleIndex = -1;
        Iterator iterator = new Iterator.Builder(this, viewRecycler).create();
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
                    float ratio = (float) diff / (float) (getCount() - firstVisibleIndex);
                    view.setCameraDistance(
                            minCameraDistance + (maxCameraDistance - minCameraDistance) * ratio);
                }

                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getPivotOnOvershootEnd(Axis.DRAGGING_AXIS, view));
                arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                        arithmetics.getPivotOnOvershootEnd(Axis.ORTHOGONAL_AXIS, view));
                arithmetics.setRotation(Axis.ORTHOGONAL_AXIS, view, angle);
            }
        }
    }

    private void tiltOnOvershootUp(final float angle) {
        Iterator iterator = new Iterator.Builder(this, viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            View view = tabItem.getView();

            if (tabItem.getIndex() == 0) {
                view.setCameraDistance(getMaxCameraDistance());
                arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                        arithmetics.getPivotOnOvershootStart(Axis.DRAGGING_AXIS, view));
                arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                        arithmetics.getPivotOnOvershootStart(Axis.ORTHOGONAL_AXIS, view));
                arithmetics.setRotation(Axis.ORTHOGONAL_AXIS, view, angle);
            } else if (tabItem.isInflated()) {
                tabItem.getView().setVisibility(View.INVISIBLE);
            }
        }
    }

    private float getMaxCameraDistance() {
        float density = getResources().getDisplayMetrics().density;
        return density * 1280;
    }

    @SuppressWarnings("WrongConstant")
    private boolean handleDrag(final float dragPosition, final float orthogonalPosition) {
        if (dragPosition <= topDragThreshold) {
            if (!dragHelper.isReset()) {
                dragHelper.reset(0);
            }

            dragState = DragState.OVERSHOOT_START;
            overshootDragHelper.update(dragPosition);
            float overshootDistance = Math.abs(overshootDragHelper.getDragDistance());

            if (overshootDistance <= maxOvershootDistance) {
                float ratio = Math.max(0, Math.min(1, overshootDistance / maxOvershootDistance));
                Iterator iterator = new Iterator.Builder(this, viewRecycler).create();
                TabItem tabItem;

                while ((tabItem = iterator.next()) != null) {
                    if (tabItem.getIndex() == 0) {
                        View view = tabItem.getView();
                        float currentPosition = tabItem.getTag().getPosition();
                        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                                arithmetics.getDefaultPivot(Axis.DRAGGING_AXIS, view));
                        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                                arithmetics.getDefaultPivot(Axis.ORTHOGONAL_AXIS, view));
                        arithmetics.setPosition(Axis.DRAGGING_AXIS, view,
                                currentPosition - (currentPosition * ratio));
                    } else if (tabItem.isInflated()) {
                        View firstView = iterator.first().getView();
                        View view = tabItem.getView();
                        view.setVisibility(arithmetics.getPosition(Axis.DRAGGING_AXIS, firstView) <=
                                arithmetics.getPosition(Axis.DRAGGING_AXIS, view) ? View.INVISIBLE :
                                View.VISIBLE);
                    }
                }
            } else {
                float ratio = Math.max(0, Math.min(1,
                        (overshootDistance - maxOvershootDistance) / maxOvershootDistance));
                tiltOnOvershootUp(ratio * MAX_UP_OVERSHOOT_ANGLE);
            }
        } else if (dragPosition >= bottomDragThreshold) {
            if (!dragHelper.isReset()) {
                dragHelper.reset(0);
            }

            dragState = DragState.OVERSHOOT_END;
            overshootDragHelper.update(dragPosition);
            float overshootDistance = overshootDragHelper.getDragDistance();
            float ratio = Math.max(0, Math.min(1, overshootDistance / maxOvershootDistance));
            tiltOnOvershootDown(ratio * -MAX_DOWN_OVERSHOOT_ANGLE);
        } else {
            overshootDragHelper.reset();
            float previousDistance = dragHelper.isReset() ? 0 : dragHelper.getDragDistance();
            dragHelper.update(dragPosition);
            closeDragHelper.update(orthogonalPosition);

            if (dragState == DragState.NONE && draggedTabItem == null &&
                    closeDragHelper.hasThresholdBeenReached()) {
                TabItem tabItem = getFocusedTabView(dragHelper.getDragStartPosition());

                if (tabItem != null && tabItem.getTab().isCloseable()) {
                    draggedTabItem = tabItem;
                }
            }

            if (draggedTabItem == null && dragHelper.hasThresholdBeenReached()) {
                if (dragState == DragState.OVERSHOOT_START) {
                    dragState = DragState.DRAG_TO_END;
                } else if (dragState == DragState.OVERSHOOT_END) {
                    dragState = DragState.DRAG_TO_START;
                } else {
                    dragState = previousDistance - dragHelper.getDragDistance() <= 0 ?
                            DragState.DRAG_TO_END : DragState.DRAG_TO_START;
                }
            }

            if (draggedTabItem != null) {
                handleDragToClose();
            } else if (dragState != DragState.NONE) {
                calculateTabPositions();
                checkIfDragThresholdReached(dragPosition);
            }

            return true;
        }

        return false;
    }

    private void calculateTabPositions() {
        float currentDragDistance = dragHelper.getDragDistance();
        float distance = currentDragDistance - dragDistance;
        dragDistance = currentDragDistance;

        if (distance != 0) {
            if (dragState == DragState.DRAG_TO_END) {
                calculateTabPositionsWhenDraggingDown(distance);
            } else {
                calculateTabPositionsWhenDraggingUp(distance);
            }
        }
    }

    private void calculateTabPositionsWhenDraggingDown(final float dragDistance) {
        firstVisibleIndex = -1;
        Iterator iterator =
                new Iterator.Builder(this, viewRecycler).start(Math.max(0, firstVisibleIndex))
                        .create();
        TabItem tabItem;
        boolean abort = false;

        while ((tabItem = iterator.next()) != null && !abort) {
            if (getCount() - tabItem.getIndex() > 1) {
                abort = calculateTabPositionWhenDraggingDown(dragDistance, tabItem,
                        iterator.previous());

                if (firstVisibleIndex == -1 && tabItem.getTag().getState() == State.FLOATING) {
                    firstVisibleIndex = tabItem.getIndex();
                }
            }

            inflateOrRemoveTabView(tabItem);
        }
    }

    private void calculateTabPositionsWhenDraggingUp(final float dragDistance) {
        Iterator iterator =
                new Iterator.Builder(this, viewRecycler).start(Math.max(0, firstVisibleIndex))
                        .create();
        TabItem tabItem;
        boolean abort = false;

        while ((tabItem = iterator.next()) != null && !abort) {
            if (getCount() - tabItem.getIndex() > 1) {
                abort = calculateTabPositionWhenDraggingUp(dragDistance, tabItem,
                        iterator.previous());
            }

            inflateOrRemoveTabView(tabItem);
        }

        if (firstVisibleIndex > 0) {
            int start = firstVisibleIndex - 1;
            iterator = new Iterator.Builder(this, viewRecycler).start(start).create();
            abort = false;

            while ((tabItem = iterator.next()) != null && !abort) {
                TabItem previous = iterator.previous();
                float previousPosition = previous.getTag().getPosition();
                float newPosition = previousPosition + maxTabSpacing;
                tabItem.getTag().setPosition(newPosition);

                if (tabItem.getIndex() < start) {
                    clipDraggedTabPosition(previous.getTag().getPosition(), previous, tabItem);
                    inflateOrRemoveTabView(previous);

                    if (previous.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = previous.getIndex();
                    } else {
                        abort = true;
                    }
                }

                if (!iterator.hasNext()) {
                    clipDraggedTabPosition(newPosition, tabItem, null);
                    inflateOrRemoveTabView(tabItem);

                    if (tabItem.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = tabItem.getIndex();
                    }
                }
            }
        }
    }

    private boolean calculateTabPositionWhenDraggingDown(final float dragDistance,
                                                         @NonNull final TabItem tabItem,
                                                         @Nullable final TabItem previous) {
        if (previous == null || previous.getTag().getState() != State.FLOATING) {
            if ((tabItem.getTag().getState() == State.STACKED_START_ATOP &&
                    tabItem.getIndex() == 0) || tabItem.getTag().getState() == State.FLOATING) {
                float currentPosition = tabItem.getTag().getPosition();
                float thresholdPosition = calculateBottomThresholdPosition(tabItem);
                float newPosition = Math.min(currentPosition + dragDistance, thresholdPosition);
                clipDraggedTabPosition(newPosition, tabItem, previous);
            } else if (tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                return true;
            }
        } else {
            float thresholdPosition = calculateBottomThresholdPosition(tabItem);
            float newPosition = Math.min(calculateNonLinearPosition(previous), thresholdPosition);
            clipDraggedTabPosition(newPosition, tabItem, previous);
        }

        return false;
    }

    private boolean calculateTabPositionWhenDraggingUp(final float dragDistance,
                                                       @NonNull final TabItem tabItem,
                                                       @Nullable final TabItem previous) {
        if (previous == null || previous.getTag().getState() != State.FLOATING ||
                previous.getTag().getPosition() > attachedPosition) {
            if (tabItem.getTag().getState() == State.FLOATING) {
                float currentPosition = tabItem.getTag().getPosition();
                float newPosition = currentPosition + dragDistance;
                clipDraggedTabPosition(newPosition, tabItem, previous);
            } else if (tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                float currentPosition = tabItem.getTag().getPosition();
                clipDraggedTabPosition(currentPosition, tabItem, previous);
                return true;
            } else if (tabItem.getTag().getState() == State.HIDDEN ||
                    tabItem.getTag().getState() == State.STACKED_START) {
                return true;
            }
        } else {
            float newPosition = calculateNonLinearPosition(previous);
            clipDraggedTabPosition(newPosition, tabItem, previous);
        }

        return false;
    }

    private float calculateNonLinearPosition(@NonNull final TabItem previous) {
        float previousPosition = previous.getTag().getPosition();
        float ratio = Math.min(1, previousPosition / attachedPosition);
        return previousPosition - minTabSpacing -
                (ratio * (maxTabSpacing - minTabSpacing));
    }

    private void inflateOrRemoveTabView(@NonNull final TabItem tabItem) {
        if (tabItem.isInflated() && !tabItem.isVisible()) {
            viewRecycler.remove(tabItem);
        } else if (tabItem.isVisible()) {
            if (!tabItem.isInflated()) {
                inflateTabView(tabItem, null);
            } else {
                applyTag(tabItem);
            }
        }
    }

    private void inflateTabView(@NonNull final TabItem tabItem,
                                @Nullable final OnGlobalLayoutListener layoutListener) {
        boolean inflated = viewRecycler.inflate(tabItem, tabViewBottomMargin);

        if (inflated) {
            View view = tabItem.getView();
            view.getViewTreeObserver().addOnGlobalLayoutListener(
                    createInflateTabViewLayoutListener(tabItem, layoutListener));
        } else {
            adaptTabViewSize(tabItem);
            applyTag(tabItem);

            if (layoutListener != null) {
                layoutListener.onGlobalLayout();
            }
        }
    }

    private float calculateAttachedPosition() {
        return (arithmetics.getSize(Axis.DRAGGING_AXIS, tabContainer) -
                (isDraggingHorizontally() && isToolbarShown() ? toolbar.getHeight() + tabInset :
                        0)) / 2f;
    }

    private boolean checkIfDragThresholdReached(final float dragPosition) {
        if (isBottomDragThresholdReached() &&
                (dragState == DragState.DRAG_TO_END || dragState == DragState.OVERSHOOT_END)) {
            bottomDragThreshold = dragPosition;
            dragState = DragState.OVERSHOOT_END;
            return true;
        } else if (isTopDragThresholdReached() &&
                (dragState == DragState.DRAG_TO_START || dragState == DragState.OVERSHOOT_START)) {
            topDragThreshold = dragPosition;
            dragState = DragState.OVERSHOOT_START;
            return true;
        }

        return false;
    }

    private void handleDragToClose() {
        View view = draggedTabItem.getView();

        if (!draggedTabItem.getTag().isClosing()) {
            adaptTopMostTabViewWhenClosing(draggedTabItem, draggedTabItem.getIndex() + 1);
        }

        draggedTabItem.getTag().setClosing(true);
        float dragDistance = closeDragHelper.getDragDistance();
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getPivotWhenClosing(Axis.DRAGGING_AXIS, view));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getPivotWhenClosing(Axis.ORTHOGONAL_AXIS, view));
        float scale = arithmetics.getScale(view, true);
        arithmetics.setPosition(Axis.ORTHOGONAL_AXIS, view, dragDistance);
        float ratio = 1 - (Math.abs(dragDistance) / calculateClosedTabPosition());
        float scaledClosedTabScale = closedTabScale * scale;
        float targetScale = scaledClosedTabScale + ratio * (scale - scaledClosedTabScale);
        arithmetics.setScale(Axis.DRAGGING_AXIS, view, targetScale);
        arithmetics.setScale(Axis.ORTHOGONAL_AXIS, view, targetScale);
        view.setAlpha(closedTabAlpha + ratio * (1 - closedTabAlpha));
    }

    private void adaptTopMostTabViewWhenClosing(@NonNull final TabItem closedTabItem,
                                                final int index) {
        if (closedTabItem.getTag().getState() == State.STACKED_START_ATOP) {
            TabItem tabItem = TabItem.create(this, viewRecycler, index);

            if (tabItem.getTag().getState() == State.HIDDEN) {
                Pair<Float, State> pair = calculateTopMostPositionAndState(closedTabItem, tabItem);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
                inflateTabView(tabItem, null);
            }
        }
    }

    private void adaptTopMostTabViewWhenClosingAborted(@NonNull final TabItem closedTabItem,
                                                       final int index) {
        if (closedTabItem.getTag().getState() == State.STACKED_START_ATOP) {
            TabItem tabItem = TabItem.create(this, viewRecycler, index);
            tabItem.getTag().setPosition(Float.NaN);
            tabItem.getTag().setState(State.HIDDEN);
            viewRecycler.remove(tabItem);
        }
    }

    private float calculateClosedTabPosition() {
        return arithmetics.getSize(Axis.ORTHOGONAL_AXIS, tabContainer);
    }

    @Nullable
    private TabItem getFocusedTabView(final float position) {
        Iterator iterator = new Iterator.Builder(this, viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.getTag().getState() == State.FLOATING ||
                    tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                View view = tabItem.getView();
                float toolbarHeight = isToolbarShown() && !isDraggingHorizontally() ?
                        toolbar.getHeight() - tabInset : 0;
                float viewPosition =
                        arithmetics.getPosition(Axis.DRAGGING_AXIS, view) + toolbarHeight +
                                arithmetics.getPadding(Axis.DRAGGING_AXIS, Gravity.START, this);

                if (viewPosition <= position) {
                    return tabItem;
                }
            }
        }

        return null;
    }

    private void handleRelease(@Nullable final MotionEvent event) {
        boolean thresholdReached = dragHelper.hasThresholdBeenReached();
        DragState flingDirection = this.dragState;
        this.dragHelper.reset(dragThreshold);
        this.overshootDragHelper.reset();
        this.closeDragHelper.reset();
        this.topDragThreshold = -Float.MAX_VALUE;
        this.bottomDragThreshold = Float.MAX_VALUE;
        this.dragState = DragState.NONE;
        this.dragDistance = 0;

        if (draggedTabItem != null) {
            float flingVelocity = 0;

            if (event != null && velocityTracker != null) {
                int pointerId = event.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                flingVelocity = Math.abs(velocityTracker.getXVelocity(pointerId));
            }

            View view = draggedTabItem.getView();
            boolean close = flingVelocity >= minCloseFlingVelocity ||
                    Math.abs(arithmetics.getPosition(Axis.ORTHOGONAL_AXIS, view)) >
                            arithmetics.getSize(Axis.ORTHOGONAL_AXIS, view) / 4f;
            animateOrthogonalDrag(draggedTabItem, close, flingVelocity, 0,
                    createCloseAnimationListener(draggedTabItem, close));
        } else if (flingDirection == DragState.DRAG_TO_START ||
                flingDirection == DragState.DRAG_TO_END) {

            if (event != null && velocityTracker != null && thresholdReached) {
                animateFling(event, flingDirection);
            }
        } else if (flingDirection == DragState.OVERSHOOT_END) {
            animateOvershootDown();
        } else if (flingDirection == DragState.OVERSHOOT_START) {
            animateOvershootUp();
        } else if (event != null && !dragHelper.hasThresholdBeenReached() &&
                !closeDragHelper.hasThresholdBeenReached()) {
            handleClick(event);
        }

        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private void handleClick(@NonNull final MotionEvent event) {
        TabItem tabItem = getFocusedTabView(arithmetics.getPosition(Axis.DRAGGING_AXIS, event));

        if (tabItem != null) {
            selectTab(tabItem.getTab());
        }
    }

    private void animateOvershootDown() {
        animateTilt(new AccelerateDecelerateInterpolator(), createOvershootAnimationListener(),
                MAX_DOWN_OVERSHOOT_ANGLE);
    }

    private void animateOvershootUp() {
        boolean tilted = animateTilt(new AccelerateInterpolator(), null, MAX_UP_OVERSHOOT_ANGLE);

        if (tilted) {
            enqueuePendingAction(new Runnable() {

                @Override
                public void run() {
                    animateOvershootUp(new DecelerateInterpolator());
                }

            });
        } else {
            animateOvershootUp(new AccelerateDecelerateInterpolator());
        }
    }

    private void animateOvershootUp(@NonNull final Interpolator interpolator) {
        TabItem tabItem = TabItem.create(this, viewRecycler, 0);
        View view = tabItem.getView();
        arithmetics.setPivot(Axis.DRAGGING_AXIS, view,
                arithmetics.getDefaultPivot(Axis.DRAGGING_AXIS, view));
        arithmetics.setPivot(Axis.ORTHOGONAL_AXIS, view,
                arithmetics.getDefaultPivot(Axis.ORTHOGONAL_AXIS, view));
        float position = arithmetics.getPosition(Axis.DRAGGING_AXIS, view);
        float targetPosition = tabItem.getTag().getPosition();
        long animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        ValueAnimator animation = ValueAnimator.ofFloat(targetPosition - position);
        animation.setDuration(Math.round(animationDuration * Math.abs(
                (targetPosition - position) / (float) (STACKED_TAB_COUNT * stackedTabSpacing))));
        animation.addListener(createAnimationListenerWrapper(createOvershootAnimationListener()));
        animation.setInterpolator(interpolator);
        animation.setStartDelay(0);
        animation.addUpdateListener(createOvershootUpAnimatorUpdateListener());
        animation.start();
    }

    private boolean animateTilt(@NonNull final Interpolator interpolator,
                                @Nullable final AnimatorListener listener, final float maxAngle) {
        long animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        Iterator iterator = new Iterator.Builder(this, viewRecycler).reverse(true).create();
        TabItem tabItem;
        boolean result = false;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.isInflated()) {
                View view = tabItem.getView();

                if (arithmetics.getRotation(Axis.ORTHOGONAL_AXIS, view) != 0) {
                    result = true;
                    ViewPropertyAnimator animation = view.animate();
                    animation.setListener(createAnimationListenerWrapper(
                            createOvershootAnimationListenerWrapper(view, listener)));
                    animation.setDuration(Math.round(animationDuration *
                            (Math.abs(arithmetics.getRotation(Axis.ORTHOGONAL_AXIS, view)) /
                                    maxAngle)));
                    animation.setInterpolator(interpolator);
                    arithmetics.animateRotation(Axis.ORTHOGONAL_AXIS, animation, 0);
                    animation.setStartDelay(0);
                    animation.start();
                }
            }
        }

        return result;
    }

    private void animateFling(@NonNull final MotionEvent event,
                              @NonNull final DragState dragState) {
        int pointerId = event.getPointerId(0);
        velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
        float flingVelocity = Math.abs(velocityTracker.getYVelocity(pointerId));

        if (flingVelocity > minFlingVelocity) {
            float flingDistance = 0.25f * flingVelocity;

            if (dragState == DragState.DRAG_TO_START) {
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

    public final void setDecorator(@NonNull final TabSwitcherDecorator decorator) {
        ensureNotNull(decorator, "The decorator may not be null");
        this.decorator = decorator;
        this.childViewRecycler.setDecorator(decorator);
        this.recyclerAdapter.clearCachedPreviews();
    }

    public final TabSwitcherDecorator getDecorator() {
        ensureNotNull(decorator, "No decorator has been set", IllegalStateException.class);
        return decorator;
    }

    public final void addListener(@NonNull final TabSwitcherListener listener) {
        ensureNotNull(listener, "The listener may not be null");
        this.listeners.add(listener);
    }

    public final void removeListener(@NonNull final TabSwitcherListener listener) {
        ensureNotNull(listener, "The listener may not be null");
        this.listeners.remove(listener);
    }

    @NonNull
    public final ViewGroup getTabContainer() {
        return tabContainer;
    }

    @NonNull
    public final Toolbar getToolbar() {
        return toolbar;
    }

    public final void showToolbar(final boolean show) {
        toolbar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    public final boolean isToolbarShown() {
        return toolbar.getVisibility() == View.VISIBLE;
    }

    public final void setToolbarTitle(@Nullable final CharSequence title) {
        toolbar.setTitle(title);
    }

    public final void setToolbarTitle(@StringRes final int resourceId) {
        setToolbarTitle(getContext().getText(resourceId));
    }

    public final void inflateToolbarMenu(@MenuRes final int resourceId,
                                         @Nullable final OnMenuItemClickListener listener) {
        toolbar.inflateMenu(resourceId);
        toolbar.setOnMenuItemClickListener(listener);

    }

    public final Menu getToolbarMenu() {
        return toolbar.getMenu();
    }

    public static void setupWithMenu(@NonNull final TabSwitcher tabSwitcher,
                                     @NonNull final Menu menu,
                                     @Nullable final OnClickListener listener) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(menu, "The menu may not be null");

        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            View view = menuItem.getActionView();

            if (view instanceof TabSwitcherButton) {
                TabSwitcherButton tabSwitcherButton = (TabSwitcherButton) view;
                tabSwitcherButton.setOnClickListener(listener);
                tabSwitcherButton.setCount(tabSwitcher.getCount());
                tabSwitcher.addListener(tabSwitcherButton);
            }
        }
    }

    public final void setToolbarNavigationIcon(@Nullable final Drawable icon,
                                               @Nullable final OnClickListener listener) {
        toolbar.setNavigationIcon(icon);
        toolbar.setNavigationOnClickListener(listener);
    }

    public final void setToolbarNavigationIcon(@DrawableRes final int resourceId,
                                               @Nullable final OnClickListener listener) {
        setToolbarNavigationIcon(ContextCompat.getDrawable(getContext(), resourceId), listener);
    }

    @Override
    public final void setPadding(final int left, final int top, final int right, final int bottom) {
        padding = new int[]{left, top, right, bottom};
        LayoutParams toolbarLayoutParams = (LayoutParams) toolbar.getLayoutParams();
        toolbarLayoutParams.setMargins(left, top, right, 0);
        Iterator iterator = new Iterator.Builder(this, viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            TabViewHolder viewHolder = tabItem.getViewHolder();

            if (viewHolder != null) {
                recyclerAdapter.adaptPadding(viewHolder);
            }
        }
    }

    @Override
    public final int getPaddingLeft() {
        return padding[0];
    }

    @Override
    public final int getPaddingTop() {
        return padding[1];
    }

    @Override
    public final int getPaddingRight() {
        return padding[2];
    }

    @Override
    public final int getPaddingBottom() {
        return padding[3];
    }

    @Override
    public final int getPaddingStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return getLayoutDirection() == LAYOUT_DIRECTION_RTL ? getPaddingRight() :
                    getPaddingLeft();
        }

        return getPaddingLeft();
    }

    @Override
    public final int getPaddingEnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return getLayoutDirection() == LAYOUT_DIRECTION_RTL ? getPaddingLeft() :
                    getPaddingRight();
        }

        return getPaddingRight();
    }

    @Override
    public final void onGlobalLayout() {
        ViewUtil.removeOnGlobalLayoutListener(getViewTreeObserver(), this);

        if (selectedTabIndex != -1) {
            TabItem tabItem = TabItem.create(this, viewRecycler, selectedTabIndex);
            viewRecycler.inflate(tabItem);
        }
    }

}