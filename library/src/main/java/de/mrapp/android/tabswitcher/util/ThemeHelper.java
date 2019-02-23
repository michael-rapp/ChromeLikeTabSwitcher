/*
 * Copyright 2016 - 2019 Michael Rapp
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

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.util.ThemeUtil;
import de.mrapp.util.Condition;

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
     * The resource id of the theme, which is used regardless of the used layout.
     */
    private final int globalTheme;

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
    private int obtainThemeFromXmlAttributes(@NonNull final Layout layout) {
        int result = layout == Layout.TABLET ? tabletTheme : phoneTheme;

        if (result == 0) {
            result = globalTheme;
        }

        if (result != 0) {
            return result;
        }

        throw new NotFoundException();
    }

    /**
     * Returns the resource id of the theme, which is used when using a specific layout. The theme
     * is obtained from a theme's attributes. If the theme is not specified, the resource id of the
     * default theme is returned.
     *
     * @param layout
     *         The layout as a value of the enum {@link Layout}. The layout may not be null
     * @param themeResourceId
     *         The resource id of the theme, the resource id should be obtained from, as an {@link
     *         Integer} value or -1, if the global app theme should be used
     * @return The resource id of the theme, which is used when using the given layout, as an {@link
     * Integer} value
     */
    private int obtainThemeFromThemeAttributes(@NonNull final Layout layout,
                                               final int themeResourceId) {
        int resourceId = layout == Layout.TABLET ? R.attr.tabSwitcherThemeTablet :
                R.attr.tabSwitcherThemePhone;
        int result = ThemeUtil.getResId(context, themeResourceId, resourceId, 0);

        if (result == 0) {
            result = ThemeUtil.getResId(context, R.attr.tabSwitcherThemeGlobal, 0);

            if (result == 0) {
                result = R.style.TabSwitcher_Light;
            }
        }

        return result;
    }

    /**
     * Creates a new helper class, which allows to retrieve resources, depending on a tab switcher's
     * theme.
     *
     * @param context
     *         The context, which is used by the tab switcher, as an instance of the class {@link
     *         Context}. The context may not be null
     * @param globalTheme
     *         The resource id of the theme, which should be used regardless of the used layout, as
     *         an {@link Integer} value or 0, if the theme is not specified
     * @param phoneTheme
     *         The resource id of the theme, which should be used, when using the smartphone layout,
     *         as an {@link Integer} value or 0, if the theme is not specified
     * @param tabletTheme
     *         The resource id of the theme, which should be used, when using the tablet layout, as
     *         an {@link Integer} value or 0, if the theme is not specified
     */
    public ThemeHelper(@NonNull final Context context, final int globalTheme, final int phoneTheme,
                       final int tabletTheme) {
        Condition.INSTANCE.ensureNotNull(context, "The context may not be null");
        this.context = context;
        this.globalTheme = globalTheme;
        this.phoneTheme = phoneTheme;
        this.tabletTheme = tabletTheme;
    }

    /**
     * Returns the resource id of the theme, which is used when using a specific layout.
     *
     * @param layout
     *         The layout as a value of the enum {@link Layout}. The layout may not be null
     * @return The resource id of the theme as an {@link Integer} value
     */
    public final int getThemeResourceId(@NonNull final Layout layout) {
        int themeResourceId;

        try {
            themeResourceId = obtainThemeFromXmlAttributes(layout);
        } catch (NotFoundException e) {
            themeResourceId = obtainThemeFromThemeAttributes(layout, -1);
        }

        return themeResourceId;
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
            int themeResourceId = getThemeResourceId(layout);

            try {
                return ThemeUtil.getColor(context, themeResourceId, resourceId);
            } catch (NotFoundException e) {
                themeResourceId = obtainThemeFromThemeAttributes(layout, themeResourceId);
                return ThemeUtil.getColor(context, themeResourceId, resourceId);
            }
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
            int themeResourceId = getThemeResourceId(layout);

            try {
                return ThemeUtil.getColorStateList(context, themeResourceId, resourceId);
            } catch (NotFoundException e) {
                themeResourceId = obtainThemeFromThemeAttributes(layout, themeResourceId);
                return ThemeUtil.getColorStateList(context, themeResourceId, resourceId);
            }
        }
    }

    /**
     * Returns the drawable, which corresponds to a specific theme attribute, regarding the theme,
     * which is used when using a specific layout.
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
            int themeResourceId = getThemeResourceId(layout);

            try {
                return ThemeUtil.getDrawable(context, themeResourceId, resourceId);
            } catch (NotFoundException e) {
                themeResourceId = obtainThemeFromThemeAttributes(layout, themeResourceId);
                return ThemeUtil.getDrawable(context, themeResourceId, resourceId);
            }
        }
    }

    /**
     * Returns the text, which corresponds to a specific theme attribute, regarding the theme, which
     * is used when using a specific layout.
     *
     * @param layout
     *         The layout as a value of the enum {@link Layout}. The layout may not be null
     * @param resourceId
     *         The resource id of the theme attribute, the text should be obtained from, as an
     *         {@link Integer} value. The resource id must correspond to a valid theme attribute
     * @return The text, which has been obtained, as an instance of the type {@link CharSequence}
     */
    public CharSequence getText(@NonNull final Layout layout, @AttrRes final int resourceId) {
        try {
            return ThemeUtil.getText(context, resourceId);
        } catch (NotFoundException e1) {
            int themeResourceId = getThemeResourceId(layout);

            try {
                return ThemeUtil.getText(context, themeResourceId, resourceId);
            } catch (NotFoundException e) {
                themeResourceId = obtainThemeFromThemeAttributes(layout, themeResourceId);
                return ThemeUtil.getText(context, themeResourceId, resourceId);
            }
        }
    }

    /**
     * Returns the resource id, which corresponds to a specific theme attribute, regarding the
     * theme, which is used when using a specific layout.
     *
     * @param layout
     *         The layout as a value of the enum {@link Layout}. The layout may not be null
     * @param resourceId
     *         The resource id of the theme attribute, the resource id should be obtained from, as
     *         an {@link Integer} value. The resource id must correspond to a valid theme attribute
     * @param defaultValue
     *         The default value, which should be returned, if the given resource id is invalid, as
     *         an {@link Integer} value
     * @return The resource id, which has been obtained, as an {@link Integer} value
     */
    public int getResourceId(@NonNull final Layout layout, @AttrRes final int resourceId,
                             final int defaultValue) {
        int result = ThemeUtil.getResId(context, resourceId, 0);

        if (result == 0) {
            int themeResourceId = getThemeResourceId(layout);
            result = ThemeUtil.getResId(context, themeResourceId, resourceId, 0);

            if (result == 0) {
                themeResourceId = obtainThemeFromThemeAttributes(layout, themeResourceId);
                return ThemeUtil.getResId(context, themeResourceId, resourceId, defaultValue);
            }
        }

        return result;
    }

    /**
     * Returns the integer value, which corresponds to a specific theme attribute, regarding the
     * theme, which is used when using a specific layout.
     *
     * @param layout
     *         The layout as a value of the enum {@link Layout}. The layout may not be null
     * @param resourceId
     *         The resource id of the theme attribute, the integer value should be obtained from, as
     *         an {@link Integer} value. The resource id must correspond to a valid theme attribute
     * @param defaultValue
     *         The default value, which should be returned, if the given resource id is invalid, as
     *         an {@link Integer} value
     * @return The integer value, which has been obtained, as an {@link Integer} value
     */
    public int getInteger(@NonNull final Layout layout, @AttrRes final int resourceId,
                          final int defaultValue) {
        int result = ThemeUtil.getInt(context, resourceId, 0);

        if (result == 0) {
            int themeResourceId = getThemeResourceId(layout);
            result = ThemeUtil.getInt(context, themeResourceId, resourceId, 0);

            if (result == 0) {
                themeResourceId = obtainThemeFromThemeAttributes(layout, themeResourceId);
                return ThemeUtil.getInt(context, themeResourceId, resourceId, defaultValue);
            }
        }

        return result;
    }

}