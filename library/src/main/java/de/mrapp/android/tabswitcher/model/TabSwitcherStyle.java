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
package de.mrapp.android.tabswitcher.model;

import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.util.ThemeHelper;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * Allows to retrieve the style attributes of a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabSwitcherStyle {

    /**
     * The tab switcher, whose style attributes are retrieved.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The theme helper, which allows to retrieve resources, depending on the tab switcher's theme.
     */
    private final ThemeHelper themeHelper;

    /**
     * Creates a new class, which allows to retrieve the style attributes of a {@link TabSwitcher}.
     *
     * @param tabSwitcher
     *         The tab switcher, whose style attributes are retrieved, as an instance of the class
     *         {@link TabSwitcher}. The tab switcher may not be null
     * @param themeHelper
     *         The theme helper, which allows to retrieve resources, depending on the tab switcher's
     *         theme, as an instance of the class {@link ThemeHelper}. The theme helper may not be
     *         null
     */
    public TabSwitcherStyle(@NonNull final TabSwitcher tabSwitcher,
                            @NonNull final ThemeHelper themeHelper) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(themeHelper, "The theme helper may not be null");
        this.tabSwitcher = tabSwitcher;
        this.themeHelper = themeHelper;
    }

    /**
     * Returns the theme helper, which allows to retrieve resources, depending on the tab switcher's
     * theme.
     *
     * @return The theme helper, which allows to retrieve resources, depending on the tab switcher's
     * theme, as an instance of the class {@link ThemeHelper}. The theme helper may not be null
     */
    @NonNull
    public final ThemeHelper getThemeHelper() {
        return themeHelper;
    }

    /**
     * Returns the icon of tabs.
     *
     * @param tab
     *         The tab, the icon should be returned for, as an instance of the class {@link Tab} or
     *         null, if the icon should not be returned for a specific tab
     * @return The icon of tabs as an instance of the class {@link Drawable} or null, if no icon is
     * set
     */
    @Nullable
    public final Drawable getTabIcon(@Nullable final Tab tab) {
        Drawable icon = tab != null ? tab.getIcon(tabSwitcher.getContext()) : null;

        if (icon == null) {
            icon = tabSwitcher.getTabIcon();

            if (icon == null) {
                try {
                    icon = themeHelper
                            .getDrawable(tabSwitcher.getLayout(), R.attr.tabSwitcherTabIcon);
                } catch (NotFoundException e) {
                    icon = null;
                }
            }
        }

        return icon;
    }

    /**
     * Returns the background color of tabs.
     *
     * @param tab
     *         The tab, the background color should be returned for, as an instance of the class
     *         {@link Tab} or null, if the background color should not be returned for a specific
     *         tab
     * @return The background color of tabs as an instance of the class {@link ColorStateList}
     */
    public final ColorStateList getTabBackgroundColor(@Nullable final Tab tab) {
        ColorStateList colorStateList = tab != null ? tab.getBackgroundColor() : null;

        if (colorStateList == null) {
            colorStateList = tabSwitcher.getTabBackgroundColor();

            if (colorStateList == null) {
                colorStateList = themeHelper.getColorStateList(tabSwitcher.getLayout(),
                        R.attr.tabSwitcherTabBackgroundColor);
            }
        }

        return colorStateList;
    }

    /**
     * Returns the content background color of tabs.
     *
     * @param tab
     *         The tab, the background color should be returned for, as an instance of the class
     *         {@link Tab} or null, if the background color should not be returned for a specific
     *         tab
     * @return The content background color of tabs as an {@link Integer} value
     */
    @ColorInt
    public final int getTabContentBackgroundColor(@Nullable final Tab tab) {
        int color = tab != null ? tab.getContentBackgroundColor() : -1;

        if (color == -1) {
            color = tabSwitcher.getTabContentBackgroundColor();

            if (color == -1) {
                color = themeHelper.getColor(tabSwitcher.getLayout(),
                        R.attr.tabSwitcherTabContentBackgroundColor);
            }
        }

        return color;
    }

    /**
     * Returns the title text color of tabs.
     *
     * @param tab
     *         The tab, the text color should be returned for, as an instance of the class {@link
     *         Tab} or null, if the text color should not be returned for a specific tab
     * @return The title text color of tabs as an instance of the class {@link ColorStateList}
     */
    public final ColorStateList getTabTitleTextColor(@Nullable final Tab tab) {
        ColorStateList colorStateList = tab != null ? tab.getTitleTextColor() : null;

        if (colorStateList == null) {
            colorStateList = tabSwitcher.getTabTitleTextColor();

            if (colorStateList == null) {
                colorStateList = themeHelper.getColorStateList(tabSwitcher.getLayout(),
                        R.attr.tabSwitcherTabTitleTextColor);
            }
        }

        return colorStateList;
    }

    /**
     * Returns the close button icon of tabs.
     *
     * @param tab
     *         The tab as an instance of the class {@link Tab}. The tab may not be null
     * @return The close button icon of tabs as an instance of the class {@link Drawable}
     */
    public final Drawable getTabCloseButtonIcon(@Nullable final Tab tab) {
        Drawable icon = tab != null ? tab.getCloseButtonIcon(tabSwitcher.getContext()) : null;

        if (icon == null) {
            icon = tabSwitcher.getTabCloseButtonIcon();

            if (icon == null) {
                icon = themeHelper
                        .getDrawable(tabSwitcher.getLayout(), R.attr.tabSwitcherTabCloseButtonIcon);
            }
        }

        return icon;
    }

    /**
     * Returns the color of the button, which allows to add a new tab. When using the smartphone
     * layout, such a button is never shown. When using the tablet layout, the button is shown next
     * to the tabs.
     *
     * @return The color of the button, which allows to add a new tab, as an instance of the class
     * {@link ColorStateList} or null, if the default color is used
     */
    public final ColorStateList getAddTabButtonColor() {
        ColorStateList colorStateList = tabSwitcher.getAddTabButtonColor();

        if (colorStateList == null) {
            colorStateList = themeHelper.getColorStateList(tabSwitcher.getLayout(),
                    R.attr.tabSwitcherAddTabButtonColor);
        }

        return colorStateList;
    }

    /**
     * Returns the title of the toolbar, which is shown, when the tab switcher is shown. When using
     * the tablet layout, the title corresponds to the primary toolbar.
     *
     * @return The title of the toolbar, which is shown, when the tab switcher is shown, as an
     * instance of the type {@link CharSequence} or null, if no title is set
     */
    @Nullable
    public final CharSequence getToolbarTitle() {
        CharSequence title = tabSwitcher.getToolbarTitle();

        if (TextUtils.isEmpty(title)) {
            try {
                title = themeHelper
                        .getText(tabSwitcher.getLayout(), R.attr.tabSwitcherToolbarTitle);
            } catch (NotFoundException e) {
                title = null;
            }
        }

        return title;
    }

    /**
     * Returns the navigation icon of the toolbar, which is shown, when the tab switcher is shown.
     * When using the tablet layout, the icon corresponds to the primary toolbar.
     *
     * @return The icon of the toolbar, which is shown, when the tab switcher is shown, as an
     * instance of the class {@link Drawable} or null, if no icon is set
     */
    @Nullable
    public final Drawable getToolbarNavigationIcon() {
        Drawable icon = tabSwitcher.getToolbarNavigationIcon();

        if (icon == null) {
            try {
                themeHelper.getDrawable(tabSwitcher.getLayout(),
                        R.attr.tabSwitcherToolbarNavigationIcon);
            } catch (NotFoundException e) {
                icon = null;
            }
        }

        return icon;
    }

}