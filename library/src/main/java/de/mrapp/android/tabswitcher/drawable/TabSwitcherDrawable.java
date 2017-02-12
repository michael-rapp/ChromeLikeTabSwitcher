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

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherListener;
import de.mrapp.android.util.ThemeUtil;

/**
 * A drawable, which allows to show the number of tabs, which are currently contained by a {@link
 * TabSwitcher}. It must be registered at a {@link TabSwitcher} instance in order to keep the
 * displayed label up to date.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabSwitcherDrawable extends Drawable implements TabSwitcherListener {

    private final int size;

    private final int textSizeNormal;

    private final int textSizeSmall;

    private final Drawable background;

    private final Paint paint;

    private String label;

    private void update(final int count) {
        label = Integer.toString(count);

        if (label.length() > 2) {
            label = "99+";
            paint.setTextSize(textSizeSmall);
        } else {
            paint.setTextSize(textSizeNormal);
        }

        invalidateSelf();
    }

    public TabSwitcherDrawable(@NonNull final Context context) {
        Resources resources = context.getResources();
        size = resources.getDimensionPixelSize(R.dimen.tab_switcher_drawable_size);
        textSizeNormal =
                resources.getDimensionPixelSize(R.dimen.tab_switcher_drawable_font_size_normal);
        textSizeSmall =
                resources.getDimensionPixelSize(R.dimen.tab_switcher_drawable_font_size_small);
        background =
                ContextCompat.getDrawable(context, R.drawable.tab_switcher_menu_item_background)
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

    public final void setCount(final int count) {
        update(count);
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
                                 @NonNull final Tab tab) {
        update(tabSwitcher.getCount());
    }

    @Override
    public final void onTabRemoved(@NonNull final TabSwitcher tabSwitcher, final int index,
                                   @NonNull final Tab tab) {
        update(tabSwitcher.getCount());
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final TabSwitcher tabSwitcher) {
        update(tabSwitcher.getCount());
    }

}