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
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * A drawable, which allows to show the number of tabs, which are currently contained by a {@link
 * TabSwitcher}. It must be registered at a {@link TabSwitcher} instance in order to keep the
 * displayed count up to date.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabSwitcherDrawable extends Drawable {

    private final int size;

    private final Drawable background;

    public TabSwitcherDrawable(@NonNull final Context context) {
        size = context.getResources().getDimensionPixelSize(R.dimen.tab_switcher_drawable_size);
        background =
                ContextCompat.getDrawable(context, R.drawable.tab_switcher_menu_item_background);
    }

    @Override
    public final void draw(@NonNull final Canvas canvas) {
        background.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        background.draw(canvas);
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
    }

    @Override
    public final void setColorFilter(@Nullable final ColorFilter colorFilter) {
        background.setColorFilter(colorFilter);
    }

    @Override
    public final int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}