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
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.mrapp.android.util.ThemeUtil;
import de.mrapp.android.util.ViewUtil;
import de.mrapp.android.util.gesture.DragHelper;

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

    private static class ViewHolder {

        private CardView cardView;

        private ViewGroup titleContainer;

        private TextView titleTextView;

        private ImageButton closeButton;

        private ViewGroup childContainer;

    }

    private static class TabProperties {

        private float position;

        private float projectedPosition;

        private float tempPosition;

        private TabPosition tabPosition;

        public TabProperties(final float position, final float projectedPosition,
                             final TabPosition tabPosition) {
            this.position = position;
            this.projectedPosition = projectedPosition;
            this.tempPosition = projectedPosition;
            this.tabPosition = tabPosition;
        }

    }

    /**
     * A list, which contains the tab switcher's tabs.
     */
    private List<Tab> tabs;

    /**
     * An instance of the class {@link DragHelper}, which is used to recognize drag gestures.
     */
    private transient DragHelper dragHelper;

    private boolean switcherShown;

    private int stackedTabSpacing;

    private int cardViewMargin;

    private int draggedIndex;

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
        cardViewMargin = getResources().getDimensionPixelSize(R.dimen.card_view_margin);
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
            int count = 1;
            TabProperties previous = null;

            for (int i = getChildCount() - 1; i >= 0; i--) {
                View view = getChildAt(i);
                ViewHolder viewHolder = (ViewHolder) view.getTag(R.id.tag_view_holder);
                viewHolder.cardView.setUseCompatPadding(true);
                viewHolder.cardView.setRadius(
                        getResources().getDimensionPixelSize(R.dimen.card_view_corner_radius));
                viewHolder.titleContainer.setVisibility(View.VISIBLE);
                int padding = getResources().getDimensionPixelSize(R.dimen.card_view_padding);
                int actionBarSize =
                        ThemeUtil.getDimensionPixelSize(getContext(), R.attr.actionBarSize);
                viewHolder.childContainer.setPadding(padding, actionBarSize, padding, padding);
                long duration = getResources().getInteger(android.R.integer.config_longAnimTime);
                previous = calculateInitialTabPosition(count, previous);
                float position = previous.position;
                TabPosition tabPosition = previous.tabPosition;
                view.setVisibility(tabPosition == TabPosition.TOP_MOST_HIDDEN ||
                        tabPosition == TabPosition.BOTTOM_MOST_HIDDEN ? View.INVISIBLE :
                        View.VISIBLE);
                view.setTag(R.id.tag_position, previous);

                if (position > 0) {
                    view.animate().setInterpolator(new AccelerateDecelerateInterpolator())
                            .setDuration(duration).y(position).start();
                }

                count++;
            }
        }
    }

    private float calculateInitialTabPosition(final int index) {
        if (getCount() < VISIBLE_TAB_COUNT && index == getCount()) {
            return 0;
        } else {
            float modifier = 0.45f +
                    (Math.min(1, Math.max(0, getCount() - 2) / (VISIBLE_TAB_COUNT - 2)) * 0.05f);

            if (index < VISIBLE_TAB_COUNT) {
                return (float) (getHeight() * Math.pow(modifier, index) - cardViewMargin);
            } else {
                float position = (float) (getHeight() * Math.pow(modifier, VISIBLE_TAB_COUNT - 1) -
                        cardViewMargin);
                return position - ((index - VISIBLE_TAB_COUNT + 1) * MIN_TAB_DISTANCE);
            }
        }
    }

    private TabProperties calculateInitialTabPosition(final int index,
                                                      @Nullable final TabProperties previous) {
        float position = calculateInitialTabPosition(index);
        Pair<Float, TabPosition> topMostPair = calculateTopMostPosition(index, previous);

        float topMostPosition = topMostPair.first;

        if (position <= topMostPosition) {
            return new TabProperties(topMostPair.first, position, topMostPair.second);
        } else {
            Pair<Float, TabPosition> bottomMostPair = calculateBottomMostPosition(index);
            float bottomMostPosition = bottomMostPair.first;

            if (position >= bottomMostPosition) {
                return new TabProperties(bottomMostPair.first, position, topMostPair.second);
            }
        }

        return new TabProperties(position, position, TabPosition.VISIBLE);
    }

    @SuppressWarnings("unchecked")
    private TabProperties calculateDraggedTabPosition(final int distance, final int index,
                                                      @NonNull final TabProperties tag,
                                                      @Nullable final TabProperties previous) {
        if (getChildCount() - index > 0) {
            float newPosition = tag.projectedPosition + distance;
            Pair<Float, TabPosition> topMostPair = calculateTopMostPosition(index, previous);
            float topMostPosition = topMostPair.first;

            if (newPosition <= topMostPosition) {
                tag.position = topMostPair.first;
                tag.tempPosition = newPosition;
                tag.tabPosition = topMostPair.second;
                return tag;
            } else {
                Pair<Float, TabPosition> bottomMostPair = calculateBottomMostPosition(index);
                float bottomMostPosition = bottomMostPair.first;

                if (newPosition >= bottomMostPosition) {
                    tag.position = bottomMostPair.first;
                    tag.tempPosition = newPosition;
                    tag.tabPosition = bottomMostPair.second;
                    return tag;
                }
            }

            tag.position = newPosition;
            tag.tempPosition = newPosition;
            tag.tabPosition = TabPosition.VISIBLE;
            return tag;
        }

        return tag;
    }

    private Pair<Float, TabPosition> calculateTopMostPosition(final int index,
                                                              @Nullable final TabProperties previous) {
        if ((getCount() - index) < STACKED_TAB_COUNT) {
            float position = stackedTabSpacing * (getCount() - index);
            return Pair.create(position, TabPosition.STACKED_TOP);
        } else {
            float position = stackedTabSpacing * STACKED_TAB_COUNT;
            return Pair.create(position,
                    (previous == null || previous.tabPosition == TabPosition.VISIBLE) ?
                            TabPosition.TOP_MOST : TabPosition.TOP_MOST_HIDDEN);
        }
    }

    private Pair<Float, TabPosition> calculateBottomMostPosition(final int index) {
        if (index <= STACKED_TAB_COUNT) {
            float position = getHeight() - cardViewMargin - stackedTabSpacing * index;
            return Pair.create(position, TabPosition.STACKED_BOTTOM);
        } else {
            float position = getHeight() - cardViewMargin - stackedTabSpacing * STACKED_TAB_COUNT;
            return Pair.create(position, TabPosition.BOTTOM_MOST_HIDDEN);
        }
    }

    private int calculateDraggedIndex(final float position) {
        int count = 0;

        for (int i = getChildCount() - 1; i >= 0; i--) {
            count++;
            View view = getChildAt(i);

            if (view.getY() + cardViewMargin <= position) {
                return count;
            }
        }

        return count;
    }

    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        if (isSwitcherShown()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    draggedIndex = calculateDraggedIndex(event.getY());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    handleDrag(event.getY());
                    return true;
                case MotionEvent.ACTION_UP:
                    if (dragHelper.hasThresholdBeenReached()) {
                        handleRelease();
                    }

                    performClick();
                    return true;
                default:
                    break;
            }
        }

        return super.onTouchEvent(event);
    }

    @SuppressWarnings("unchecked")
    private void handleDrag(final float dragPosition) {
        dragHelper.update(dragPosition);

        if (dragHelper.hasThresholdBeenReached()) {
            int count = 1;
            TabProperties previous = null;

            for (int i = getChildCount() - 1; i >= 0; i--) {
                View view = getChildAt(i);
                TabProperties tag = (TabProperties) view.getTag(R.id.tag_position);
                previous =
                        calculateDraggedTabPosition(dragHelper.getDistance(), count, tag, previous);
                float position = previous.position;
                TabPosition tabPosition = previous.tabPosition;
                view.setY(position);
                view.setVisibility(tabPosition == TabPosition.TOP_MOST_HIDDEN ||
                        tabPosition == TabPosition.BOTTOM_MOST_HIDDEN ? View.INVISIBLE :
                        View.VISIBLE);
                count++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRelease() {
        dragHelper.reset();

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            TabProperties tag = (TabProperties) view.getTag(R.id.tag_position);
            tag.position = view.getY();
            tag.projectedPosition = tag.tempPosition;
        }
    }

    private static final int MIN_TAB_DISTANCE = 200;

    private static final int STACKED_TAB_COUNT = 3;

    private static final int VISIBLE_TAB_COUNT = 4;

    private enum TabPosition {

        STACKED_TOP,

        TOP_MOST_HIDDEN,

        TOP_MOST,

        VISIBLE,

        BOTTOM_MOST_HIDDEN,

        STACKED_BOTTOM

    }

}