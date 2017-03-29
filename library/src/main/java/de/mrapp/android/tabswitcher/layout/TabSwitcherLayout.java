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
package de.mrapp.android.tabswitcher.layout;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.Menu;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.NoSuchElementException;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Animation.SwipeAnimation;
import de.mrapp.android.tabswitcher.Animation.SwipeDirection;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabCloseListener;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.TabSwitcherListener;
import de.mrapp.android.tabswitcher.model.Model;

/**
 * Defines the interface, a layout, which implements the functionality of a {@link TabSwitcher},
 * must implement.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public interface TabSwitcherLayout extends Model {

    /**
     * Sets the decorator, which allows to inflate the views, which correspond to the tabs of the
     * tab switcher.
     *
     * @param decorator
     *         The decorator, which should be set, as an instance of the class {@link
     *         TabSwitcherDecorator}. The decorator may not be null
     */
    void setDecorator(@NonNull TabSwitcherDecorator decorator);

    /**
     * Returns the decorator, which allows to inflate the views, which correspond to the tabs of the
     * tab switcher.
     *
     * @return The decorator as an instance of the class {@link TabSwitcherDecorator} or null, if no
     * decorator has been set
     */
    TabSwitcherDecorator getDecorator();

    /**
     * Adds a listener, which should be notified about the tab switcher's events.
     *
     * @param listener
     *         The listener, which should be added, as an instance of the type {@link
     *         TabSwitcherListener}. The listener may not be null
     */
    void addListener(@NonNull TabSwitcherListener listener);

    /**
     * Removes a specific listener, which should not be notified about the tab switcher's events,
     * anymore.
     *
     * @param listener
     *         The listener, which should be removed, as an instance of the type {@link
     *         TabSwitcherListener}. The listener may not be null
     */
    void removeListener(@NonNull TabSwitcherListener listener);

    /**
     * Adds a new listener, which should be notified, when a tab is about to be closed by clicking
     * its close button.
     *
     * @param listener
     *         The listener, which should be added, as an instance of the type {@link
     *         TabCloseListener}. The listener may not be null
     */
    void addCloseTabListener(@NonNull TabCloseListener listener);

    /**
     * Removes a specific listener, which should not be notified, when a tab is about to be closed
     * by clicking its close button, anymore.
     *
     * @param listener
     *         The listener, which should be removed, as an instance of the type {@link
     *         TabCloseListener}. The listener may not be null
     */
    void removeCloseTabListener(@NonNull TabCloseListener listener);

    /**
     * Returns the layout of the tab switcher.
     *
     * @return The layout of the tab switcher as a value of the enum {@link Layout}
     */
    @NonNull
    Layout getLayout();

    /**
     * Returns, whether an animation is currently running, or not.
     *
     * @return True, if an animation is currently running, false otherwise
     */
    boolean isAnimationRunning();

    /**
     * Returns the view group, which contains the tab switcher's tabs.
     *
     * @return The view group, which contains the tab switcher's tabs, as an instance of the class
     * {@link ViewGroup}. The view group may not be null
     */
    @NonNull
    ViewGroup getTabContainer();

    /**
     * Returns the toolbar, which is shown, when the tab switcher is shown.
     *
     * @return The toolbar, which is shown, when the tab switcher is shown, as an instance of the
     * class {@link Toolbar}. The toolbar may not be null
     */
    @NonNull
    Toolbar getToolbar();

    /**
     * Sets, whether the toolbar should be shown, when the tab switcher is shown, or not.
     *
     * @param show
     *         True, if the toolbar should be shown, false otherwise
     */
    void showToolbar(boolean show);

    /**
     * Returns, whether the toolbar is shown, when the tab switcher is shown, or not.
     *
     * @return True, if the toolbar is shown, false otherwise
     */
    boolean isToolbarShown();

    /**
     * Sets the title of the toolbar, which is shown, when the tab switcher is shown.
     *
     * @param title
     *         The title, which should be set, as an instance of the type {@link CharSequence} or
     *         null, if no title should be set
     */
    void setToolbarTitle(@Nullable CharSequence title);

    /**
     * Sets the title of the toolbar, which is shown, when the tab switcher is shown.
     *
     * @param resourceId
     *         The resource id of the title, which should be set, as an {@link Integer} value. The
     *         resource id must correspond to a valid string resource
     */
    void setToolbarTitle(@StringRes int resourceId);

    /**
     * Inflates the menu of the toolbar, which is shown, when the tab switcher is shown.
     *
     * @param resourceId
     *         The resource id of the menu, which should be inflated, as an {@link Integer} value.
     *         The resource id must correspond to a valid menu resource
     * @param listener
     *         The listener, which should be notified, when an menu item has been clicked, as an
     *         instance of the type {@link OnMenuItemClickListener} or null, if no listener should
     *         be notified
     */
    void inflateToolbarMenu(@MenuRes int resourceId, @Nullable OnMenuItemClickListener listener);

    /**
     * Returns the menu of the toolbar, which is shown, when the tab switcher is shown.
     *
     * @return The menu of the toolbar as an instance of the type {@link Menu}. The menu may not be
     * null
     */
    @NonNull
    Menu getToolbarMenu();

    /**
     * Sets the navigation icon of the toolbar, which is shown, when the tab switcher is shown.
     *
     * @param icon
     *         The icon, which should be set, as an instance of the class {@link Drawable} or null,
     *         if no icon should be set
     * @param listener
     *         The listener, which should be notified, when the navigation item has been clicked, as
     *         an instance of the type {@link OnClickListener} or null, if no listener should be
     *         notified
     */
    void setToolbarNavigationIcon(@Nullable Drawable icon, @Nullable OnClickListener listener);

    /**
     * Sets the navigation icon of the toolbar, which is shown, when the tab switcher is shown.
     *
     * @param resourceId
     *         The resource id of the icon, which should be set, as an {@link Integer} value. The
     *         resource id must correspond to a valid drawable resource
     * @param listener
     *         The listener, which should be notified, when the navigation item has been clicked, as
     *         an instance of the type {@link OnClickListener} or null, if no listener should be
     *         notified
     */
    void setToolbarNavigationIcon(@DrawableRes int resourceId, @Nullable OnClickListener listener);

    /**
     * Sets the padding of the tab switcher.
     *
     * @param left
     *         The left padding, which should be set, as an {@link Integer} value
     * @param top
     *         The top padding, which should be set, as an {@link Integer} value
     * @param right
     *         The right padding, which should be set, as an {@link Integer} value
     * @param bottom
     *         The bottom padding, which should be set, as an {@link Integer} value
     */
    void setPadding(int left, int top, int right, int bottom);

    /**
     * Returns the left padding of the tab switcher.
     *
     * @return The left padding of the tab switcher as an {@link Integer} value
     */
    int getPaddingLeft();

    /**
     * Returns the top padding of the tab switcher.
     *
     * @return The top padding of the tab switcher as an {@link Integer} value
     */
    int getPaddingTop();

    /**
     * Returns the right padding of the tab switcher.
     *
     * @return The right padding of the tab switcher as an {@link Integer} value
     */
    int getPaddingRight();

    /**
     * Returns the bottom padding of the tab switcher.
     *
     * @return The bottom padding of the tab switcher as an {@link Integer} value
     */
    int getPaddingBottom();

    /**
     * Returns the start padding of the tab switcher. This corresponds to the right padding, if a
     * right-to-left layout is used, or to the left padding otherwise.
     *
     * @return The start padding of the tab switcher as an {@link Integer} value
     */
    int getPaddingStart();

    /**
     * Returns the end padding of the tab switcher. This corresponds ot the left padding, if a
     * right-to-left layout is used, or to the right padding otherwise.
     *
     * @return The end padding of the tab switcher as an {@link Integer} value
     */
    int getPaddingEnd();

    /**
     * Returns the default icon of a tab.
     *
     * @return The default icon of a tab as an instance of the class {@link Drawable} or null, if no
     * icon is set
     */
    @Nullable
    Drawable getTabIcon();

    /**
     * Sets the default icon of a tab.
     *
     * @param resourceId
     *         The resource id of the icon, which should be set, as an {@link Integer} value. The
     *         resource id must correspond to a valid drawable resource
     */
    void setTabIcon(@DrawableRes int resourceId);

    /**
     * Sets the default icon of a tab.
     *
     * @param icon
     *         The icon, which should be set, as an instance of the class {@link Bitmap} or null, if
     *         no icon should be set
     */
    void setTabIcon(@Nullable Bitmap icon);

    /**
     * Returns the default background color of a tab.
     *
     * @return The default background color of a tab as an {@link Integer} value
     */
    @ColorInt
    int getTabBackgroundColor();

    /**
     * Sets the default background color of a tab.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value
     */
    void setTabBackgroundColor(@ColorInt int color);

    /**
     * Returns the default text color of a tab's title.
     *
     * @return The default text color of a tab's title as an {@link Integer} value
     */
    @ColorInt
    int getTabTitleTextColor();

    /**
     * Sets the default text color of a tab's title.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value
     */
    void setTabTitleTextColor(@ColorInt int color);

    /**
     * Returns the default icon of a tab's close button.
     *
     * @return The default icon of a tab's close button as an instance of the class {@link Drawable}
     */
    @NonNull
    Drawable getTabCloseButtonIcon();

    /**
     * Sets the default icon of a tab's close button.
     *
     * @param resourceId
     *         The resource id of the icon, which should be set, as an {@link Integer} value. The
     *         resource id must correspond to a valid drawable resource
     */
    void setTabCloseButtonIcon(@DrawableRes int resourceId);

    /**
     * Sets the default icon of a tab's close button.
     *
     * @param icon
     *         The icon, which should be set, as an instance of the class {@link Bitmap}. The icon
     *         may not be null
     */
    void setTabCloseButtonIcon(@NonNull final Bitmap icon);

}