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
package de.mrapp.android.tabswitcher.model;

import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.util.ThemeHelper;
import de.mrapp.util.Condition;

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
     * The model of the tab switcher, whose style attributes are retrieved.
     */
    private final Model model;

    /**
     * The theme helper, which allows to retrieve resources, depending on the tab switcher's theme.
     */
    private final ThemeHelper themeHelper;

    /**
     * Return the color state list, which should be used to tint the icon of tabs.
     *
     * @param tab
     *         The tab, the color state list should be returned for, as an instance of the class
     *         {@link Tab} or null, if the color state list should not be returned for a specific
     *         tab
     * @return The color state list, which should be used to tint the icon of tabs, as an instance
     * of the class {@link ColorStateList} or null, if the icon should not be tinted
     */
    @Nullable
    private ColorStateList getTabIconTintList(@Nullable final Tab tab) {
        ColorStateList tintList = tab != null ? tab.getCloseButtonIconTintList() : null;

        if (tintList == null) {
            tintList = model.getTabCloseButtonIconTintList();

            if (tintList == null) {
                try {
                    tintList = themeHelper.getColorStateList(tabSwitcher.getLayout(),
                            R.attr.tabSwitcherTabIconTint);
                } catch (NotFoundException e) {
                    tintList = null;
                }

            }
        }

        return tintList;
    }

    /**
     * Returns the mode, which should be used to tint the icon of tabs.
     *
     * @param tab
     *         The tab, the mode should be returned for, as an instance of the class {@link Tab} or
     *         null, if the mode should not be returned for a specific tab
     * @return The mode, which should be used to tint the icon of tabs, as a value of the enum
     * {@link PorterDuff.Mode}. The mode may not be null
     */
    @NonNull
    private PorterDuff.Mode getTabIconTintMode(@Nullable final Tab tab) {
        PorterDuff.Mode tintMode = tab != null ? tab.getIconTintMode() : null;

        if (tintMode == null) {
            tintMode = model.getTabIconTintMode();
        }

        return tintMode != null ? tintMode : PorterDuff.Mode.SRC_ATOP;
    }

    /**
     * Return the color state list, which should be used to tint the close button of tabs.
     *
     * @param tab
     *         The tab, the color state list should be returned for, as an instance of the class
     *         {@link Tab} or null, if the color state list should not be returned for a specific
     *         tab
     * @return The color state list, which should be used to tint the close button of tabs, as an
     * instance of the class {@link ColorStateList} or null, if the close button should not be
     * tinted
     */
    @Nullable
    private ColorStateList getTabCloseButtonIconTintList(@Nullable final Tab tab) {
        ColorStateList tintList = tab != null ? tab.getCloseButtonIconTintList() : null;

        if (tintList == null) {
            tintList = model.getTabCloseButtonIconTintList();

            if (tintList == null) {
                try {
                    tintList = themeHelper.getColorStateList(tabSwitcher.getLayout(),
                            R.attr.tabSwitcherTabCloseButtonIconTint);
                } catch (NotFoundException e) {
                    tintList = null;
                }

            }
        }

        return tintList;
    }

    /**
     * Returns the mode, which should be used to tint the close button of tabs.
     *
     * @param tab
     *         The tab, the mode should be returned for, as an instance of the class {@link Tab} or
     *         null, if the mode should not be returned for a specific tab
     * @return The mode, which should be used to tint the close button of tabs, as a value of the
     * enum {@link PorterDuff.Mode}. The mode may not be null
     */
    @NonNull
    private PorterDuff.Mode getTabCloseButtonIconTintMode(@Nullable final Tab tab) {
        PorterDuff.Mode tintMode = tab != null ? tab.getCloseButtonIconTintMode() : null;

        if (tintMode == null) {
            tintMode = model.getTabCloseButtonIconTintMode();
        }

        return tintMode != null ? tintMode : PorterDuff.Mode.SRC_ATOP;
    }

    /**
     * Return the color state list, which should be used to tint the navigation icon of the toolbar,
     * which is shown, when the tab switcher is shown.
     *
     * @return The color state list, which should be used to tint the navigation icon of the
     * toolbar, which is shown, then the tab switcher is shown, as an instance of the class {@link
     * ColorStateList} or null, if the navigation icon should not be tinted
     */
    @Nullable
    private ColorStateList getToolbarNavigationIconTintList() {
        ColorStateList tintList = model.getTabCloseButtonIconTintList();

        if (tintList == null) {
            try {
                tintList = themeHelper.getColorStateList(tabSwitcher.getLayout(),
                        R.attr.tabSwitcherToolbarNavigationIconTint);
            } catch (NotFoundException e) {
                tintList = null;
            }

        }

        return tintList;
    }

    /**
     * Returns the mode, which should be used to tint the navigation icon of the toolbar, which is
     * shown, when the tab switcher is shown.
     *
     * @return The mode, which should be used to tint the navigation icon of the toolbar, which is
     * shown, when the tab switcher is shown, as a value of the enum {@link PorterDuff.Mode}. The
     * mode may not be null
     */
    @NonNull
    private PorterDuff.Mode getToolbarNavigationIconTintMode() {
        PorterDuff.Mode tintMode = model.getToolbarNavigationIconTintMode();
        return tintMode != null ? tintMode : PorterDuff.Mode.SRC_ATOP;
    }

    /**
     * Creates a new class, which allows to retrieve the style attributes of a {@link TabSwitcher}.
     *
     * @param tabSwitcher
     *         The tab switcher, whose style attributes are retrieved, as an instance of the type
     *         {@link TabSwitcher}. The tab switcher may not be null
     * @param model
     *         The model of the tab switcher, whose style attributes are retrieved, as an instance
     *         of the class {@link TabSwitcher}. The model may not be null
     * @param themeHelper
     *         The theme helper, which allows to retrieve resources, depending on the tab switcher's
     *         theme, as an instance of the class {@link ThemeHelper}. The theme helper may not be
     *         null
     */
    public TabSwitcherStyle(@NonNull final TabSwitcher tabSwitcher, @NonNull final Model model,
                            @NonNull final ThemeHelper themeHelper) {
        Condition.INSTANCE.ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        Condition.INSTANCE.ensureNotNull(model, "The model may not be null");
        Condition.INSTANCE.ensureNotNull(themeHelper, "The theme helper may not be null");
        this.tabSwitcher = tabSwitcher;
        this.model = model;
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
        Drawable icon = tab != null ? tab.getIcon(model.getContext()) : null;

        if (icon == null) {
            icon = model.getTabIcon();

            if (icon == null) {
                try {
                    icon = themeHelper
                            .getDrawable(tabSwitcher.getLayout(), R.attr.tabSwitcherTabIcon);
                } catch (NotFoundException e) {
                    icon = null;
                }
            }
        }

        if (icon != null) {
            ColorStateList tintList = getTabIconTintList(tab);

            if (tintList != null) {
                PorterDuff.Mode tintMode = getTabIconTintMode(tab);
                DrawableCompat.setTintList(icon, tintList);
                DrawableCompat.setTintMode(icon, tintMode);
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
            colorStateList = model.getTabBackgroundColor();

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
            color = model.getTabContentBackgroundColor();

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
            colorStateList = model.getTabTitleTextColor();

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
     *         The tab, the icon should be returned for, as an instance of the class {@link Tab} or
     *         null, if the icon should not be returned for a specific tab
     * @return The close button icon of tabs as an instance of the class {@link Drawable}
     */
    public final Drawable getTabCloseButtonIcon(@Nullable final Tab tab) {
        Drawable icon = tab != null ? tab.getCloseButtonIcon(model.getContext()) : null;

        if (icon == null) {
            icon = model.getTabCloseButtonIcon();

            if (icon == null) {
                icon = themeHelper
                        .getDrawable(tabSwitcher.getLayout(), R.attr.tabSwitcherTabCloseButtonIcon);
            }
        }

        if (icon != null) {
            ColorStateList tintList = getTabCloseButtonIconTintList(tab);

            if (tintList != null) {
                PorterDuff.Mode tintMode = getTabCloseButtonIconTintMode(tab);
                DrawableCompat.setTintList(icon, tintList);
                DrawableCompat.setTintMode(icon, tintMode);
            }
        }

        return icon;
    }

    /**
     * Returns the progress bar color of tabs.
     *
     * @param tab
     *         The tab, the color should be returned for, as an instance of the class {@link Tab} or
     *         null, if the color should not be returned for a specific tab
     * @return The progress bar color of tabs as an {@link Integer} value
     */
    @ColorInt
    public final int getTabProgressBarColor(@Nullable final Tab tab) {
        int color = tab != null ? tab.getProgressBarColor() : -1;

        if (color == -1) {
            color = model.getTabProgressBarColor();

            if (color == -1) {
                color = themeHelper
                        .getColor(tabSwitcher.getLayout(), R.attr.tabSwitcherTabProgressBarColor);
            }
        }

        return color;
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
        ColorStateList colorStateList = model.getAddTabButtonColor();

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
        CharSequence title = model.getToolbarTitle();

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
        Drawable icon = model.getToolbarNavigationIcon();

        if (icon == null) {
            try {
                themeHelper.getDrawable(tabSwitcher.getLayout(),
                        R.attr.tabSwitcherToolbarNavigationIcon);
            } catch (NotFoundException e) {
                icon = null;
            }
        }

        if (icon != null) {
            ColorStateList tintList = getToolbarNavigationIconTintList();

            if (tintList != null) {
                PorterDuff.Mode tintMode = getToolbarNavigationIconTintMode();
                DrawableCompat.setTintList(icon, tintList);
                DrawableCompat.setTintMode(icon, tintMode);
            }
        }

        return icon;
    }

}