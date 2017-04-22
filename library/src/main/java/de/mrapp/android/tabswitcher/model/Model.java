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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.View.OnClickListener;

import java.util.Collection;
import java.util.NoSuchElementException;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.SwipeAnimation;
import de.mrapp.android.tabswitcher.SwipeAnimation.SwipeDirection;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabCloseListener;
import de.mrapp.android.tabswitcher.TabPreviewListener;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.util.logging.LogLevel;

/**
 * Defines the interface, a class, which implements the model of a {@link TabSwitcher} must
 * implement.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public interface Model extends Iterable<Tab> {

    /**
     * Defines the interface, a class, which should be notified about the model's events, must
     * implement.
     */
    interface Listener {

        /**
         * The method, which is invoked, when the log level has been changed.
         *
         * @param logLevel
         *         The log level, which has been set, as a value of the enum LogLevel. The log level
         *         may not be null
         */
        void onLogLevelChanged(@NonNull LogLevel logLevel);

        /**
         * The method, which is invoked, when the decorator has been changed.
         *
         * @param decorator
         *         The decorator, which has been set, as an instance of the class {@link
         *         TabSwitcherDecorator}. The decorator may not be null
         */
        void onDecoratorChanged(@NonNull TabSwitcherDecorator decorator);

        /**
         * The method, which is invoked, when the tab switcher has been shown.
         */
        void onSwitcherShown();

        /**
         * The method, which is invoked, when the tab switcher has been hidden.
         */
        void onSwitcherHidden();

        /**
         * The method, which is invoked, when the currently selected tab has been changed.
         *
         * @param previousIndex
         *         The index of the previously selected tab as an {@link Integer} value or -1, if no
         *         tab was previously selected
         * @param index
         *         The index of the currently selected tab as an {@link Integer} value or -1, if the
         *         tab switcher does not contain any tabs
         * @param selectedTab
         *         The currently selected tab as an instance of the class {@link Tab} or null, if
         *         the tab switcher does not contain any tabs
         * @param switcherHidden
         *         True, if selecting the tab caused the tab switcher to be hidden, false otherwise
         */
        void onSelectionChanged(int previousIndex, int index, @Nullable Tab selectedTab,
                                boolean switcherHidden);

        /**
         * The method, which is invoked, when a tab has been added to the model.
         *
         * @param index
         *         The index of the tab, which has been added, as an {@link Integer} value
         * @param tab
         *         The tab, which has been added, as an instance of the class {@link Tab}. The tab
         *         may not be null
         * @param previousSelectedTabIndex
         *         The index of the previously selected tab as an {@link Integer} value or -1, if no
         *         tab was selected
         * @param selectedTabIndex
         *         The index of the currently selected tab as an {@link Integer} value or -1, if the
         *         tab switcher does not contain any tabs
         * @param switcherVisibilityChanged
         *         True, if adding the tab caused the visibility of the tab switcher to be changed,
         *         false otherwise
         * @param animation
         *         The animation, which has been used to add the tab, as an instance of the class
         *         {@link Animation}. The animation may not be null
         */
        void onTabAdded(int index, @NonNull Tab tab, int previousSelectedTabIndex,
                        int selectedTabIndex, boolean switcherVisibilityChanged,
                        @NonNull Animation animation);

        /**
         * The method, which is invoked, when multiple tabs have been added to the model.
         *
         * @param index
         *         The index of the first tab, which has been added, as an {@link Integer} value
         * @param tabs
         *         An array, which contains the tabs, which have been added, as an array of the type
         *         {@link Tab} or an empty array, if no tabs have been added
         * @param previousSelectedTabIndex
         *         The index of the previously selected tab as an {@link Integer} value or -1, if no
         *         tab was selected
         * @param selectedTabIndex
         *         The index of the currently selected tab as an {@link Integer} value or -1, if the
         *         tab switcher does not contain any tabs
         * @param animation
         *         The animation, which has been used to add the tabs, as an instance of the class
         *         {@link Animation}. The animation may not be null
         */
        void onAllTabsAdded(int index, @NonNull Tab[] tabs, int previousSelectedTabIndex,
                            int selectedTabIndex, @NonNull Animation animation);

        /**
         * The method, which is invoked, when a tab has been removed from the model.
         *
         * @param index
         *         The index of the tab, which has been removed, as an {@link Integer} value
         * @param tab
         *         The tab, which has been removed, as an instance of the class {@link Tab}. The tab
         *         may not be null
         * @param previousSelectedTabIndex
         *         The index of the previously selected tab as an {@link Integer} value or -1, if no
         *         tab was selected
         * @param selectedTabIndex
         *         The index of the currently selected tab as an {@link Integer} value or -1, if the
         *         tab switcher does not contain any tabs
         * @param animation
         *         The animation, which has been used to remove the tab, as an instance of the class
         *         {@link Animation}. The animation may not be null
         */
        void onTabRemoved(int index, @NonNull Tab tab, int previousSelectedTabIndex,
                          int selectedTabIndex, @NonNull Animation animation);

        /**
         * The method, which is invoked, when all tabs have been removed from the tab switcher.
         *
         * @param tabs
         *         An array, which contains the tabs, which have been removed, as an array of the
         *         type {@link Tab} or an empty array, if no tabs have been removed
         * @param animation
         *         The animation, which has been used to remove the tabs, as an instance of the
         *         class {@link Animation}. The animation may not be null
         */
        void onAllTabsRemoved(@NonNull Tab[] tabs, @NonNull Animation animation);

        /**
         * The method, which is invoked, when the padding has been changed.
         *
         * @param left
         *         The left padding, which has been set, in pixels as an {@link Integer} value
         * @param top
         *         The top padding, which has been set, in pixels as an {@link Integer} value
         * @param right
         *         The right padding, which has been set, in pixels as an {@link Integer} value
         * @param bottom
         *         The bottom padding, which has been set, in pixels as an {@link Integer} value
         */
        void onPaddingChanged(int left, int top, int right, int bottom);

        /**
         * The method, which is invoked, when the default icon of a tab has been changed.
         *
         * @param icon
         *         The icon, which has been set, as an instance of the class {@link Drawable} or
         *         null, if no icon is set
         */
        void onTabIconChanged(@Nullable Drawable icon);

        /**
         * The method, which is invoked, when the background color of a tab has been changed.
         *
         * @param colorStateList
         *         The color state list, which has been set, as an instance of the class {@link
         *         ColorStateList} or null, if the default color should be used
         */
        void onTabBackgroundColorChanged(@Nullable ColorStateList colorStateList);

        /**
         * The method, which is invoked, when the text color of a tab's title has been changed.
         *
         * @param colorStateList
         *         The color state list, which has been set, as an instance of the class {@link
         *         ColorStateList} or null, if the default color should be used
         */
        void onTabTitleColorChanged(@Nullable ColorStateList colorStateList);

        /**
         * The method, which is invoked, when the icon of a tab's close button has been changed.
         *
         * @param icon
         *         The icon, which has been set, as an instance of the class {@link Drawable} or
         *         null, if the default icon should be used
         */
        void onTabCloseButtonIconChanged(@Nullable Drawable icon);

        /**
         * The method, which is invoked, when it has been changed, whether the toolbars should be
         * shown, when the tab switcher is shown, or not.
         *
         * @param visible
         *         True, if the toolbars should be shown, when the tab switcher is shown, false
         *         otherwise
         */
        void onToolbarVisibilityChanged(boolean visible);

        /**
         * The method, which is invoked, when the title of the toolbar, which is shown, when the tab
         * switcher is shown, has been changed.
         *
         * @param title
         *         The title, which has been set, as an instance of the type {@link CharSequence} or
         *         null, if no title is set
         */
        void onToolbarTitleChanged(@Nullable CharSequence title);

        /**
         * The method, which is invoked, when the navigation icon of the toolbar, which is shown,
         * when the tab switcher is shown, has been changed.
         *
         * @param icon
         *         The navigation icon, which has been set, as an instance of the class {@link
         *         Drawable} or null, if no navigation icon is set
         * @param listener
         *         The listener, which should be notified, when the navigation item has been
         *         clicked, as an instance of the type {@link OnClickListener} or null, if no
         *         listener should be notified
         */
        void onToolbarNavigationIconChanged(@Nullable Drawable icon,
                                            @Nullable OnClickListener listener);

        /**
         * The method, which is invoked, when the menu of the toolbar, which is shown, when the tab
         * switcher is shown, has been inflated.
         *
         * @param resourceId
         *         The resource id of the menu, which has been inflated, as an {@link Integer}
         *         value. The resource id must correspond to a valid menu resource
         * @param listener
         *         The listener, which has been registered to be notified, when an item of the menu
         *         has been clicked, as an instance of the type OnMenuItemClickListener or null, if
         *         no listener should be notified
         */
        void onToolbarMenuInflated(@MenuRes int resourceId,
                                   @Nullable OnMenuItemClickListener listener);

    }

    /**
     * Returns the context, which is used by the tab switcher.
     *
     * @return The context, which is used by the tab switcher, as an instance of the class {@link
     * Context}. The context may not be null
     */
    @NonNull
    Context getContext();

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
     * Returns the log level, which is used for logging.
     *
     * @return The log level, which is used for logging, as a value of the enum LogLevel. The log
     * level may not be null
     */
    @NonNull
    LogLevel getLogLevel();

    /**
     * Sets the log level, which should be used for logging.
     *
     * @param logLevel
     *         The log level, which should be set, as a value of the enum LogLevel. The log level
     *         may not be null
     */
    void setLogLevel(@NonNull LogLevel logLevel);

    /**
     * Returns, whether the tab switcher is empty, or not.
     *
     * @return True, if the tab switcher is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Returns the number of tabs, which are contained by the tab switcher.
     *
     * @return The number of tabs, which are contained by the tab switcher, as an {@link Integer}
     * value
     */
    int getCount();

    /**
     * Returns the tab at a specific index.
     *
     * @param index
     *         The index of the tab, which should be returned, as an {@link Integer} value. The
     *         index must be at least 0 and at maximum <code>getCount() - 1</code>, otherwise a
     *         {@link IndexOutOfBoundsException} will be thrown
     * @return The tab, which corresponds to the given index, as an instance of the class {@link
     * Tab}. The tab may not be null
     */
    @NonNull
    Tab getTab(int index);

    /**
     * Returns the index of a specific tab.
     *
     * @param tab
     *         The tab, whose index should be returned, as an instance of the class {@link Tab}. The
     *         tab may not be null
     * @return The index of the given tab as an {@link Integer} value or -1, if the given tab is not
     * contained by the tab switcher
     */
    int indexOf(@NonNull Tab tab);

    /**
     * Adds a new tab to the tab switcher. By default, the tab is added at the end. If the switcher
     * is currently shown, the tab is added by using an animation. By default, a {@link
     * SwipeAnimation} with direction {@link SwipeDirection#RIGHT} is used. If
     * an animation is currently running, the tab will be added once all previously started
     * animations have been finished.
     *
     * @param tab
     *         The tab, which should be added, as an instance of the class {@link Tab}. The tab may
     *         not be null
     */
    void addTab(@NonNull Tab tab);

    /**
     * Adds a new tab to the tab switcher at a specific index. If the switcher is currently shown,
     * the tab is added by using an animation. By default, a {@link SwipeAnimation} with
     * direction {@link SwipeDirection#RIGHT} is used. If an animation is currently
     * running, the tab will be added once all previously started animations have been finished.
     *
     * @param tab
     *         The tab, which should be added, as an instance of the class {@link Tab}. The tab may
     *         not be null
     * @param index
     *         The index, the tab should be added at, as an {@link Integer} value. The index must be
     *         at least 0 and at maximum <code>getCount()</code>, otherwise an {@link
     *         IndexOutOfBoundsException} will be thrown
     */
    void addTab(@NonNull Tab tab, int index);

    /**
     * Adds a new tab to the tab switcher at a specific index. If the switcher is currently shown,
     * the tab is added by using a specific animation. If an animation is currently
     * running, the tab will be added once all previously started animations have been finished.
     *
     * @param tab
     *         The tab, which should be added, as an instance of the class {@link Tab}. The tab may
     *         not be null
     * @param index
     *         The index, the tab should be added at, as an {@link Integer} value. The index must be
     *         at least 0 and at maximum <code>getCount()</code>, otherwise an {@link
     *         IndexOutOfBoundsException} will be thrown
     * @param animation
     *         The animation, which should be used to add the tab, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    void addTab(@NonNull Tab tab, int index, @NonNull Animation animation);

    /**
     * Adds all tabs, which are contained by a collection, to the tab switcher. By default, the tabs
     * are added at the end. If the switcher is currently shown, the tabs are added by using an
     * animation. By default, a {@link SwipeAnimation} with direction {@link
     * SwipeDirection#RIGHT} is used. If an animation is currently running, the tabs will
     * be added once all previously started animations have been finished.
     *
     * @param tabs
     *         A collection, which contains the tabs, which should be added, as an instance of the
     *         type {@link Collection} or an empty collection, if no tabs should be added
     */
    void addAllTabs(@NonNull Collection<? extends Tab> tabs);

    /**
     * Adds all tabs, which are contained by a collection, to the tab switcher, starting at a
     * specific index. If the switcher is currently shown, the tabs are added by using an animation.
     * By default, a {@link SwipeAnimation} with direction {@link
     * SwipeDirection#RIGHT} is used. If an animation is currently running, the tabs will
     * be added once all previously started animations have been finished.
     *
     * @param tabs
     *         A collection, which contains the tabs, which should be added, as an instance of the
     *         type {@link Collection} or an empty collection, if no tabs should be added
     * @param index
     *         The index, the first tab should be started at, as an {@link Integer} value. The index
     *         must be at least 0 and at maximum <code>getCount()</code>, otherwise an {@link
     *         IndexOutOfBoundsException} will be thrown
     */
    void addAllTabs(@NonNull Collection<? extends Tab> tabs, int index);

    /**
     * Adds all tabs, which are contained by a collection, to the tab switcher, starting at a
     * specific index. If the switcher is currently shown, the tabs are added by using a specific
     * animation. If an animation is currently running, the tabs will be added once all previously
     * started animations have been finished.
     *
     * @param tabs
     *         A collection, which contains the tabs, which should be added, as an instance of the
     *         type {@link Collection} or an empty collection, if no tabs should be added
     * @param index
     *         The index, the first tab should be started at, as an {@link Integer} value. The index
     *         must be at least 0 and at maximum <code>getCount()</code>, otherwise an {@link
     *         IndexOutOfBoundsException} will be thrown
     * @param animation
     *         The animation, which should be used to add the tabs, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    void addAllTabs(@NonNull Collection<? extends Tab> tabs, int index,
                    @NonNull Animation animation);

    /**
     * Adds all tabs, which are contained by an array, to the tab switcher. By default, the tabs are
     * added at the end. If the switcher is currently shown, the tabs are added by using an
     * animation. By default, a {@link SwipeAnimation} with direction {@link
     * SwipeDirection#RIGHT} is used. If an animation is currently running, the tabs will
     * be added once all previously started animations have been finished.
     *
     * @param tabs
     *         An array, which contains the tabs, which should be added, as an array of the type
     *         {@link Tab} or an empty array, if no tabs should be added
     */
    void addAllTabs(@NonNull Tab[] tabs);

    /**
     * Adds all tabs, which are contained by an array, to the tab switcher, starting at a specific
     * index. If the switcher is currently shown, the tabs are added by using an animation. By
     * default, a {@link SwipeAnimation} with direction {@link
     * SwipeDirection#RIGHT} is used. If an animation is currently running, the tabs will
     * be added once all previously started animations have been finished.
     *
     * @param tabs
     *         An array, which contains the tabs, which should be added, as an array of the type
     *         {@link Tab} or an empty array, if no tabs should be added
     * @param index
     *         The index, the first tab should be started at, as an {@link Integer} value. The index
     *         must be at least 0 and at maximum <code>getCount()</code>, otherwise an {@link
     *         IndexOutOfBoundsException} will be thrown
     */
    void addAllTabs(@NonNull Tab[] tabs, int index);

    /**
     * Adds all tabs, which are contained by an array, to the tab switcher, starting at a
     * specific index. If the switcher is currently shown, the tabs are added by using a specific
     * animation. If an animation is currently running, the tabs will be added once all previously
     * started animations have been finished.
     *
     * @param tabs
     *         An array, which contains the tabs, which should be added, as an array of the type
     *         {@link Tab} or an empty array, if no tabs should be added
     * @param index
     *         The index, the first tab should be started at, as an {@link Integer} value. The index
     *         must be at least 0 and at maximum <code>getCount()</code>, otherwise an {@link
     *         IndexOutOfBoundsException} will be thrown
     * @param animation
     *         The animation, which should be used to add the tabs, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    void addAllTabs(@NonNull Tab[] tabs, int index, @NonNull Animation animation);

    /**
     * Removes a specific tab from the tab switcher. If the switcher is currently shown, the tab is
     * removed by using an animation. By default, a {@link SwipeAnimation} with direction
     * {@link SwipeDirection#RIGHT} is used. If an animation is currently running, the tab
     * will be removed once all previously started animations have been finished.
     *
     * @param tab
     *         The tab, which should be removed, as an instance of the class {@link Tab}. The tab
     *         may not be null
     */
    void removeTab(@NonNull Tab tab);

    /**
     * Removes a specific tab from the tab switcher. If the switcher is currently shown, the tab is
     * removed by using a specific animation. If an animation is currently running, the
     * tab will be removed once all previously started animations have been finished.
     *
     * @param tab
     *         The tab, which should be removed, as an instance of the class {@link Tab}. The tab
     *         may not be null
     * @param animation
     *         The animation, which should be used to remove the tabs, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    void removeTab(@NonNull Tab tab, @NonNull Animation animation);

    /**
     * Removes all tabs from the tab switcher. If the switcher is currently shown, the tabs are
     * removed by using an animation. By default, a {@link SwipeAnimation} with direction
     * {@link SwipeDirection#RIGHT} is used. If an animation is currently running, the
     * tabs will be removed once all previously started animations have been finished.
     */
    void clear();

    /**
     * Removes all tabs from the tab switcher. If the switcher is currently shown, the tabs are
     * removed by using a specific animation. If an animation is currently running, the
     * tabs will be removed once all previously started animations have been finished.
     *
     * @param animation
     *         The animation, which should be used to remove the tabs, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    void clear(@NonNull Animation animation);

    /**
     * Returns, whether the tab switcher is currently shown.
     *
     * @return True, if the tab switcher is currently shown, false otherwise
     */
    boolean isSwitcherShown();

    /**
     * Shows the tab switcher by using an animation, if it is not already shown.
     */
    void showSwitcher();

    /**
     * Hides the tab switcher by using an animation, if it is currently shown.
     */
    void hideSwitcher();

    /**
     * Toggles the visibility of the tab switcher by using an animation, i.e. if the switcher is
     * currently shown, it is hidden, otherwise it is shown.
     */
    void toggleSwitcherVisibility();

    /**
     * Returns the currently selected tab.
     *
     * @return The currently selected tab as an instance of the class {@link Tab} or null, if no tab
     * is currently selected
     */
    @Nullable
    Tab getSelectedTab();

    /**
     * Returns the index of the currently selected tab.
     *
     * @return The index of the currently selected tab as an {@link Integer} value or -1, if no tab
     * is currently selected
     */
    int getSelectedTabIndex();

    /**
     * Selects a specific tab.
     *
     * @param tab
     *         The tab, which should be selected, as an instance of the class {@link Tab}. The tab
     *         may not be null. If the tab is not contained by the tab switcher, a {@link
     *         NoSuchElementException} will be thrown
     */
    void selectTab(@NonNull Tab tab);

    /**
     * Sets the padding of the tab switcher.
     *
     * @param left
     *         The left padding, which should be set, in pixels as an {@link Integer} value
     * @param top
     *         The top padding, which should be set, in pixels as an {@link Integer} value
     * @param right
     *         The right padding, which should be set, in pixels as an {@link Integer} value
     * @param bottom
     *         The bottom padding, which should be set, in pixels as an {@link Integer} value
     */
    void setPadding(int left, int top, int right, int bottom);

    /**
     * Returns the left padding of the tab switcher.
     *
     * @return The left padding of the tab switcher in pixels as an {@link Integer} value
     */
    int getPaddingLeft();

    /**
     * Returns the top padding of the tab switcher.
     *
     * @return The top padding of the tab switcher in pixels as an {@link Integer} value
     */
    int getPaddingTop();

    /**
     * Returns the right padding of the tab switcher.
     *
     * @return The right padding of the tab switcher in pixels as an {@link Integer} value
     */
    int getPaddingRight();

    /**
     * Returns the bottom padding of the tab switcher.
     *
     * @return The bottom padding of the tab switcher in pixels as an {@link Integer} value
     */
    int getPaddingBottom();

    /**
     * Returns the start padding of the tab switcher. This corresponds to the right padding, if a
     * right-to-left layout is used, or to the left padding otherwise.
     *
     * @return The start padding of the tab switcher in pixels as an {@link Integer} value
     */
    int getPaddingStart();

    /**
     * Returns the end padding of the tab switcher. This corresponds ot the left padding, if a
     * right-to-left layout is used, or to the right padding otherwise.
     *
     * @return The end padding of the tab switcher in pixels as an {@link Integer} value
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
     * @return The default background color of a tab as an instance of the class {@link
     * ColorStateList} or null, if the default color is used
     */
    @Nullable
    ColorStateList getTabBackgroundColor();

    /**
     * Sets the default background color of a tab.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value or -1, if the default
     *         color should be used
     */
    void setTabBackgroundColor(@ColorInt int color);

    /**
     * Sets the default background color of a tab.
     *
     * @param colorStateList
     *         The color, which should be set, as an instance of the class {@link ColorStateList} or
     *         null, if the default color should be used
     */
    void setTabBackgroundColor(@Nullable ColorStateList colorStateList);

    /**
     * Returns the default text color of a tab's title.
     *
     * @return The default text color of a tab's title as an instance of the class {@link
     * ColorStateList} or null, if the default color is used
     */
    @Nullable
    ColorStateList getTabTitleTextColor();

    /**
     * Sets the default text color of a tab's title.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value or -1, if the default
     *         color should be used
     */
    void setTabTitleTextColor(@ColorInt int color);

    /**
     * Sets the default text color of a tab's title.
     *
     * @param colorStateList
     *         The color state list, which should be set, as an instance of the class {@link
     *         ColorStateList} or null, if the default color should be used
     */
    void setTabTitleTextColor(@Nullable ColorStateList colorStateList);

    /**
     * Returns the default icon of a tab's close button.
     *
     * @return The default icon of a tab's close button as an instance of the class {@link Drawable}
     * or null, if the default icon is used
     */
    @Nullable
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
     *         The icon, which should be set, as an instance of the class {@link Bitmap} or null, if
     *         the default icon should be used
     */
    void setTabCloseButtonIcon(@Nullable final Bitmap icon);

    /**
     * Returns, whether the toolbars are shown, when the tab switcher is shown, or not. When using
     * the tablet layout, the toolbars are always shown.
     *
     * @return True, if the toolbars are shown, false otherwise
     */
    boolean areToolbarsShown();

    /**
     * Sets, whether the toolbars should be shown, when the tab switcher is shown, or not. This
     * method does not have any effect when using the tablet layout.
     *
     * @param show
     *         True, if the toolbars should be shown, false otherwise
     */
    void showToolbars(boolean show);

    /**
     * Returns the title of the toolbar, which is shown, when the tab switcher is shown. When using
     * the tablet layout, the title corresponds to the primary toolbar.
     *
     * @return The title of the toolbar, which is shown, when the tab switcher is shown, as an
     * instance of the type {@link CharSequence} or null, if no title is set
     */
    @Nullable
    CharSequence getToolbarTitle();

    /**
     * Sets the title of the toolbar, which is shown, when the tab switcher is shown. When using the
     * tablet layout, the title is set to the primary toolbar.
     *
     * @param resourceId
     *         The resource id of the title, which should be set, as an {@link Integer} value. The
     *         resource id must correspond to a valid string resource
     */
    void setToolbarTitle(@StringRes int resourceId);

    /**
     * Sets the title of the toolbar, which is shown, when the tab switcher is shown. When using the
     * tablet layout, the title is set to the primary toolbar.
     *
     * @param title
     *         The title, which should be set, as an instance of the type {@link CharSequence} or
     *         null, if no title should be set
     */
    void setToolbarTitle(@Nullable CharSequence title);

    /**
     * Returns the navigation icon of the toolbar, which is shown, when the tab switcher is shown.
     * When using the tablet layout, the icon corresponds to the primary toolbar.
     *
     * @return The icon of the toolbar, which is shown, when the tab switcher is shown, as an
     * instance of the class {@link Drawable} or null, if no icon is set
     */
    @Nullable
    Drawable getToolbarNavigationIcon();

    /**
     * Sets the navigation icon of the toolbar, which is shown, when the tab switcher is shown. When
     * using the tablet layout, the icon is set to the primary toolbar.
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
     * Sets the navigation icon of the toolbar, which is shown, when the tab switcher is shown. When
     * using the tablet layout, the icon is set to the primary toolbar.
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
     * Inflates the menu of the toolbar, which is shown, when the tab switcher is shown. When using
     * the tablet layout, the menu is inflated into the secondary toolbar.
     *
     * @param resourceId
     *         The resource id of the menu, which should be inflated, as an {@link Integer} value.
     *         The resource id must correspond to a valid menu resource
     * @param listener
     *         The listener, which should be notified, when an menu item has been clicked, as an
     *         instance of the type OnMenuItemClickListener or null, if no listener should be
     *         notified
     */
    void inflateToolbarMenu(@MenuRes int resourceId, @Nullable OnMenuItemClickListener listener);

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
     * Adds a new listener, which should be notified, when the preview of a tab is about to be
     * loaded. Previews are only loaded when using the smartphone layout.
     *
     * @param listener
     *         The listener, which should be added, as an instance of the type {@link
     *         TabPreviewListener}. The listener may not be null
     */
    void addTabPreviewListener(@NonNull TabPreviewListener listener);

    /**
     * Removes a specific listener, which should not be notified, when the preview of a tab is about
     * to be loaded.
     *
     * @param listener
     *         The listener, which should be removed, as an instance of the type {@link
     *         TabPreviewListener}. The listener may not be null
     */
    void removeTabPreviewListener(@NonNull TabPreviewListener listener);

}