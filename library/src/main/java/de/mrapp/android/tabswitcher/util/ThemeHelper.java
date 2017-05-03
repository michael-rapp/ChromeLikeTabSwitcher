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
package de.mrapp.android.tabswitcher.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.util.ThemeUtil;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A helper class, which allows to retrieve resources, depending on a tab switcher's theme.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class ThemeHelper {

    /**
     * The context, which is used by the tab switcher.
     */
    private final Context context;

    /**
     * The resource id of the theme, which is used, when using the smartphone layout.
     */
    private final int phoneTheme;

    /**
     * The resource id of the theme, which is used, when using the tablet layout.
     */
    private final int tabletTheme;

    /**
     * Returns the resource id of the theme, which is used when using a specific layout. The theme
     * is obtained from the tab switcher's XML attributes. If the theme is not specified, a {@link
     * NotFoundException} will be thrown.
     *
     * @param layout
     *         The layout as a value of the enum {@link Layout}. The layout may not be null
     * @return The resource id of the theme, which is used when using the given layout, as an {@link
     * Integer} value
     */
    private int obtainLocalTheme(@NonNull final Layout layout) {
        int resourceId = layout == Layout.TABLET ? tabletTheme : phoneTheme;

        if (resourceId == 0) {
            throw new NotFoundException();
        }

        return resourceId;
    }

    /**
     * Returns the resource id of the theme, which is used when using a specific layout. The theme
     * is obtained from the tab context's theme attributes.
     *
     * @param layout
     *         The layout as a value of the enum {@link Layout}. The layout may not be null
     * @return The resource id of the theme, which is used when using the given layout, as an {@link
     * Integer} value
     */
    private int obtainGlobalTheme(@NonNull final Layout layout) {
        // TODO Remove -1 parameter after updating AndroidUtil library
        int themeResourceId = ThemeUtil.getResourceId(context, -1, R.attr.tabSwitcherTheme);
        int resourceId = layout == Layout.TABLET ? R.attr.themeTablet : R.attr.themePhone;
        return ThemeUtil.getResourceId(context, themeResourceId, resourceId);
    }

    /**
     * Creates a new helper class, which allows to retrieve resources, depending on a tab switcher's
     * theme.
     *
     * @param context
     *         The context, which is used by the tab switcher, as an instance of the class {@link
     *         Context}. The context may not be null
     * @param phoneTheme
     *         The resource id of the theme, which is used, when using the smartphone layout, as an
     *         {@link Integer} value or 0, if the theme is not specified
     * @param tabletTheme
     *         The resource id of the theme, which is used, when using the tablet layout, as an
     *         {@link Integer} value or 0, if the theme is not specified
     */
    public ThemeHelper(@NonNull final Context context, final int phoneTheme,
                       final int tabletTheme) {
        ensureNotNull(context, "The context may not be null");
        this.context = context;
        this.phoneTheme = phoneTheme;
        this.tabletTheme = tabletTheme;
    }

    /**
     * Returns the color, which corresponds to a specific theme attribute, regarding the theme,
     * which is used when using a specific layout.
     *
     * @param layout
     *         The layout as a value of the enum {@link Layout}. The layout may not be null
     * @param resourceId
     *         The resource id of the theme attribute, the color should be obtained from, as an
     *         {@link Integer} value. The resource id must correspond to a valid theme attribute
     * @return The color, which has been obtained, as an {@link Integer} value
     */
    @ColorInt
    public int getColor(@NonNull final Layout layout, @AttrRes final int resourceId) {
        try {
            return ThemeUtil.getColor(context, resourceId);
        } catch (NotFoundException e1) {
            int themeResourceId;

            try {
                themeResourceId = obtainLocalTheme(layout);
            } catch (NotFoundException e2) {
                themeResourceId = obtainGlobalTheme(layout);
            }

            return ThemeUtil.getColor(context, themeResourceId, resourceId);
        }
    }

    /**
     * Returns the color state list, which corresponds to a specific theme attribute, regarding the
     * theme, which is used when using a specific layout.
     *
     * @param layout
     *         The layout as a value of the enum {@link Layout}. The layout may not be null
     * @param resourceId
     *         The resource id of the theme attribute, the color state list should be obtained from,
     *         as an {@link Integer} value. The resource id must correspond to a valid theme
     *         attribute
     * @return The color state list, which has been obtained, as an instance of the class {@link
     * ColorStateList}
     */
    public ColorStateList getColorStateList(@NonNull final Layout layout,
                                            @AttrRes final int resourceId) {
        try {
            return ThemeUtil.getColorStateList(context, resourceId);
        } catch (NotFoundException e1) {
            int themeResourceId;

            try {
                themeResourceId = obtainLocalTheme(layout);
            } catch (NotFoundException e2) {
                themeResourceId = obtainGlobalTheme(layout);
            }

            return ThemeUtil.getColorStateList(context, themeResourceId, resourceId);
        }
    }

    /**
     * Returns the drawable, which corresponds to a specific theme attribute, regarding the
     * theme, which is used when using a specific layout.
     *
     * @param layout
     *         The layout as a value of the enum {@link Layout}. The layout may not be null
     * @param resourceId
     *         The resource id of the theme attribute, the drawable should be obtained from, as an
     *         {@link Integer} value. The resource id must correspond to a valid theme attribute
     * @return The color state list, which has been obtained, as an instance of the class {@link
     * ColorStateList}
     */
    public Drawable getDrawable(@NonNull final Layout layout, @AttrRes final int resourceId) {
        try {
            return ThemeUtil.getDrawable(context, resourceId);
        } catch (NotFoundException e1) {
            int themeResourceId;

            try {
                themeResourceId = obtainLocalTheme(layout);
            } catch (NotFoundException e2) {
                themeResourceId = obtainGlobalTheme(layout);
            }

            return ThemeUtil.getDrawable(context, themeResourceId, resourceId);
        }
    }

}