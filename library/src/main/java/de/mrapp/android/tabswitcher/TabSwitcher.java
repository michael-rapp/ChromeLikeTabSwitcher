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

    /**
     * A list, which contains the tab switcher's tabs.
     */
    private List<Tab> tabs;

    /**
     * An instance of the class {@link DragHelper}, which is used to recognize drag gestures.
     */
    private transient DragHelper dragHelper;

    private boolean switcherShown;

    /**
     * Initializes the view.
     */
    private void initialize() {
        tabs = new ArrayList<>();
        dragHelper = new DragHelper(10);
        switcherShown = false;
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

    public TabSwitcher(@NonNull final Context context) {
        this(context, null);
    }

    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize();
    }

    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet,
                       @AttrRes final int defaultStyle) {
        super(context, attributeSet, defaultStyle);
        initialize();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet,
                       @AttrRes final int defaultStyle, @StyleRes final int defaultStyleResource) {
        super(context, attributeSet, defaultStyle, defaultStyleResource);
        initialize();
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
                viewHolder.childContainer
                        .setPadding(padding, padding + actionBarSize, padding, padding);
                long duration = getResources().getInteger(android.R.integer.config_longAnimTime);
                Pair<Float, TabPosition> pair = calculateInitialTabPosition(view, count);
                float position = pair.first;
                view.setVisibility(
                        pair.second == TabPosition.TOP_MOST ? View.INVISIBLE : View.VISIBLE);
                view.setTag(R.id.tag_position, position);
                view.animate().setInterpolator(new AccelerateDecelerateInterpolator())
                        .setDuration(duration).scaleX(0.95f).scaleY(0.95f).y(position).start();
                count++;
            }
        }
    }

    private Pair<Float, TabPosition> calculateInitialTabPosition(@NonNull final View view,
                                                                 final int index) {
        if (index < VISIBLE_TAB_COUNT) {
            float position = (float) (view.getHeight() * Math.pow(0.5, index));
            return Pair.create(position, TabPosition.VISIBLE);
        } else if ((getCount() - index) <= STACKED_TAB_COUNT) {
            int spacing = getResources().getDimensionPixelSize(R.dimen.stacked_tab_spacing);
            float position = spacing * (getCount() - index);
            return Pair.create(position, TabPosition.STACKED_TOP);
        } else {
            return Pair.create(calculateTopMostPosition(), TabPosition.TOP_MOST);
        }
    }

    private float calculateTopMostPosition() {
        int spacing = getResources().getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        return spacing * STACKED_TAB_COUNT;
    }

    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        if (isSwitcherShown()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
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

    private void handleDrag(final float dragPosition) {
        dragHelper.update(dragPosition);

        if (dragHelper.hasThresholdBeenReached()) {
            int count = 1;
            float topMostPosition = calculateTopMostPosition();

            for (int i = getChildCount() - 1; i >= 0; i--) {
                if (count < VISIBLE_TAB_COUNT) {
                    View view = getChildAt(i);
                    float initialPosition = (float) view.getTag(R.id.tag_position);
                    float newPosition = (float) (initialPosition +
                            (dragHelper.getDistance() * Math.pow(0.7, count)));
                    newPosition = Math.max(newPosition, topMostPosition);
                    view.setY(newPosition);
                }

                count++;
            }
        }
    }

    private void handleRelease() {
        dragHelper.reset();

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            view.setTag(R.id.tag_position, view.getY());
        }
    }

    private static final int VISIBLE_TAB_COUNT = 4;

    private static final int STACKED_TAB_COUNT = 3;

    private enum TabPosition {

        STACKED_TOP,

        TOP_MOST,

        VISIBLE,

        STACKED_BOTTOM

    }

}