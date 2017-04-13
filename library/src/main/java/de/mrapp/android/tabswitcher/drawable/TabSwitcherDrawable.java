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
package de.mrapp.android.tabswitcher.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherListener;
import de.mrapp.android.util.ThemeUtil;

import static de.mrapp.android.util.Condition.ensureAtLeast;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A drawable, which allows to display the number of tabs, which are currently contained by a {@link
 * TabSwitcher}. It must be registered at a {@link TabSwitcher} instance in order to keep the
 * displayed label up to date. It therefore implements the interface {@link TabSwitcherListener}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class TabSwitcherDrawable extends Drawable implements TabSwitcherListener {

    /**
     * The size of the drawable in pixels.
     */
    private final int size;

    /**
     * The default text size of the displayed label in pixels.
     */
    private final int textSizeNormal;

    /**
     * The text size of the displayed label, which is used when displaying a value greater than 99,
     * in pixels.
     */
    private final int textSizeSmall;

    /**
     * The drawable, which is shown as the background.
     */
    private final Drawable background;

    /**
     * The paint, which is used to draw the drawable's label.
     */
    private final Paint paint;

    /**
     * The currently displayed label.
     */
    private String label;

    /**
     * Creates a new drawable, which allows to display the number of tabs, which are currently
     * contained by a {@link TabSwitcher}.
     *
     * @param context
     *         The context, which should be used by the drawable, as an instance of the class {@link
     *         Context}. The context may not be null
     */
    public TabSwitcherDrawable(@NonNull final Context context) {
        ensureNotNull(context, "The context may not be null");
        Resources resources = context.getResources();
        size = resources.getDimensionPixelSize(R.dimen.tab_switcher_drawable_size);
        textSizeNormal =
                resources.getDimensionPixelSize(R.dimen.tab_switcher_drawable_font_size_normal);
        textSizeSmall =
                resources.getDimensionPixelSize(R.dimen.tab_switcher_drawable_font_size_small);
        background =
                ContextCompat.getDrawable(context, R.drawable.tab_switcher_drawable_background)
                        .mutate();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Align.CENTER);
        paint.setTextSize(textSizeNormal);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        label = Integer.toString(0);
        int tint = ThemeUtil.getColor(context, android.R.attr.textColorPrimary);
        setColorFilter(tint, PorterDuff.Mode.MULTIPLY);
    }

    /**
     * Updates the drawable to display a specific value.
     *
     * @param count
     *         The value, which should be displayed, as an {@link Integer} value. The value must be
     *         at least 0
     */
    public final void setCount(final int count) {
        ensureAtLeast(count, 0, "The count must be at least 0");
        label = Integer.toString(count);

        if (label.length() > 2) {
            label = "99+";
            paint.setTextSize(textSizeSmall);
        } else {
            paint.setTextSize(textSizeNormal);
        }

        invalidateSelf();
    }

    @Override
    public final void draw(@NonNull final Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int intrinsicWidth = background.getIntrinsicWidth();
        int intrinsicHeight = background.getIntrinsicHeight();
        int left = (width / 2) - (intrinsicWidth / 2);
        int top = (height / 2) - (intrinsicHeight / 2);
        background.getIntrinsicWidth();
        background.setBounds(left, top, left + intrinsicWidth, top + intrinsicHeight);
        background.draw(canvas);
        float x = width / 2f;
        float y = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(label, x, y, paint);
    }

    @Override
    public final int getIntrinsicWidth() {
        return size;
    }

    @Override
    public final int getIntrinsicHeight() {
        return size;
    }

    @Override
    public final void setAlpha(final int alpha) {
        background.setAlpha(alpha);
        paint.setAlpha(alpha);
    }

    @Override
    public final void setColorFilter(@Nullable final ColorFilter colorFilter) {
        background.setColorFilter(colorFilter);
        paint.setColorFilter(colorFilter);
    }

    @Override
    public final int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public final void onSwitcherShown(@NonNull final TabSwitcher tabSwitcher) {

    }

    @Override
    public final void onSwitcherHidden(@NonNull final TabSwitcher tabSwitcher) {

    }

    @Override
    public final void onSelectionChanged(@NonNull final TabSwitcher tabSwitcher,
                                         final int selectedTabIndex,
                                         @Nullable final Tab selectedTab) {

    }

    @Override
    public final void onTabAdded(@NonNull final TabSwitcher tabSwitcher, final int index,
                                 @NonNull final Tab tab, @NonNull final Animation animation) {
        setCount(tabSwitcher.getCount());
    }

    @Override
    public final void onTabRemoved(@NonNull final TabSwitcher tabSwitcher, final int index,
                                   @NonNull final Tab tab, @NonNull final Animation animation) {
        setCount(tabSwitcher.getCount());
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final TabSwitcher tabSwitcher,
                                       @NonNull final Tab[] tab,
                                       @NonNull final Animation animation) {
        setCount(tabSwitcher.getCount());
    }

}