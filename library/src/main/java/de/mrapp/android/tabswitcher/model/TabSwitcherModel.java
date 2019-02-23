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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import de.mrapp.android.tabswitcher.AddTabButtonListener;
import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.PeekAnimation;
import de.mrapp.android.tabswitcher.RevealAnimation;
import de.mrapp.android.tabswitcher.SwipeAnimation;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabCloseListener;
import de.mrapp.android.tabswitcher.TabPreviewListener;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.layout.ContentRecyclerAdapter;
import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.util.Condition;
import de.mrapp.util.datastructure.ListenerList;

/**
 * The model of a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class TabSwitcherModel implements Model, Restorable {

    /**
     * The name of the extra, which is used to store the index of the tab, which is used as a
     * reference, when restoring the positions of tabs, within a bundle.
     */
    public static final String REFERENCE_TAB_INDEX_EXTRA =
            TabSwitcherModel.class.getName() + "::ReferenceTabIndex";

    /**
     * The name of the extra, which is used to store the position of the tab, which is used as a
     * reference, when restoring the positions of tabs, within a bundle.
     */
    public static final String REFERENCE_TAB_POSITION_EXTRA =
            TabSwitcherModel.class.getName() + "::ReferenceTabPosition";

    /**
     * The name of the extra, which is used to store the log level within a bundle.
     */
    private static final String LOG_LEVEL_EXTRA = TabSwitcherModel.class.getName() + "::LogLevel";

    /**
     * The name of the extra, which is used to store the tabs within a bundle.
     */
    private static final String TABS_EXTRA = TabSwitcherModel.class.getName() + "::Tabs";

    /**
     * The name of the extra, which is used to store, whether the tab switcher is shown, or not,
     * within a bundle.
     */
    private static final String SWITCHER_SHOWN_EXTRA =
            TabSwitcherModel.class.getName() + "::SwitcherShown";

    /**
     * The name of the extra, which is used to store the index of the selected tab within a bundle.
     */
    private static final String SELECTED_TAB_INDEX_EXTRA =
            TabSwitcherModel.class.getName() + "::SelectedTabIndex";

    /**
     * The name of the extra, which is used to store the padding within a bundle.
     */
    private static final String PADDING_EXTRA = TabSwitcherModel.class.getName() + "::Padding";

    /**
     * The name of the extra, which is used to store, whether the padding of the tab switcher is
     * applied to the content of its tabs, or not, within a bundle.
     */
    private static final String APPLY_PADDING_TO_TABS_EXTRA =
            TabSwitcherModel.class.getName() + "::ApplyPaddingToTabs";

    /**
     * The name of the extra, which is used to store the resource id of a tab's icon within a
     * bundle.
     */
    private static final String TAB_ICON_ID_EXTRA =
            TabSwitcherModel.class.getName() + "::TabIconId";

    /**
     * The name of the extra, which is used to store the bitmap of a tab's icon within a bundle.
     */
    private static final String TAB_ICON_BITMAP_EXTRA =
            TabSwitcherModel.class.getName() + "::TabIconBitmap";

    /**
     * The name of the extra, which is used to store the tint list, which is used to tint a tab's
     * icon, within a bundle.
     */
    private static final String TAB_ICON_TINT_LIST_EXTRA =
            TabSwitcherModel.class.getName() + "::TabIconTintList";

    /**
     * The name of the extra, which is used to store the mode, which is used to tint a tab's icon,
     * within a bundle.
     */
    private static final String TAB_ICON_TINT_MODE_EXTRA =
            TabSwitcherModel.class.getName() + "::TabIconTintMode";

    /**
     * The name of the extra, which is used to store the background color of a tab within a bundle.
     */
    private static final String TAB_BACKGROUND_COLOR_EXTRA =
            TabSwitcherModel.class.getName() + "::TabBackgroundColor";

    /**
     * The name of the extra, which is used to store the background color of a tab's content within
     * a bundle.
     */
    private static final String TAB_CONTENT_BACKGROUND_COLOR_EXTRA =
            TabSwitcherModel.class.getName() + "::TabContentBackgroundColor";

    /**
     * The name of the extra, which is used to store the text color of a tab's title within a
     * bundle.
     */
    private static final String TAB_TITLE_TEXT_COLOR_EXTRA =
            TabSwitcherModel.class.getName() + "::TabTitleTextColor";

    /**
     * The name of the extra, which is used to store the resource id of a tab's icon within a
     * bundle.
     */
    private static final String TAB_CLOSE_BUTTON_ICON_ID_EXTRA =
            TabSwitcherModel.class.getName() + "::TabCloseButtonIconId";

    /**
     * The name of the extra, which is used to store the bitmap of a tab's icon within a bundle.
     */
    private static final String TAB_CLOSE_BUTTON_ICON_BITMAP_EXTRA =
            TabSwitcherModel.class.getName() + "::TabCloseButtonIconBitmap";

    /**
     * The name of the extra, which is used to store the tint list, which is used to tint a tab's
     * close button, within a bundle.
     */
    private static final String TAB_CLOSE_BUTTON_ICON_TINT_LIST_EXTRA =
            TabSwitcherModel.class.getName() + "::TabCloseButtonIconTintList";

    /**
     * The name of the extra, which is used to store the mode, which is used to tint a tab's close
     * button, within a bundle.
     */
    private static final String TAB_CLOSE_BUTTON_ICON_TINT_MODE_EXTRA =
            TabSwitcherModel.class.getName() + "::TabCloseButtonIconTintMode";

    /**
     * The name of the extra, which is used to store the color of a tab's progress bar within a
     * bundle.
     */
    private static final String TAB_PROGRESS_BAR_COLOR_EXTRA =
            TabSwitcherModel.class.getName() + "::TabProgressBarColor";

    /**
     * The name of the extra, which is used to store, whether the toolbars are shown, or not, within
     * a bundle.
     */
    private static final String SHOW_TOOLBARS_EXTRA =
            TabSwitcherModel.class.getName() + "::ShowToolbars";

    /**
     * The name of the extra, which is used to store the title of the toolbar within a bundle.
     */
    private static final String TOOLBAR_TITLE_EXTRA =
            TabSwitcherModel.class.getName() + "::ToolbarTitle";

    /**
     * The name of the extra, which is used to store the color state list, which is used to tint the
     * navigation icon fof the toolbar, within a bundle.
     */
    private static final String TOOLBAR_NAVIGATION_ICON_TINT_LIST_EXTRA =
            TabSwitcherModel.class.getName() + "::ToolbarNavigationIconTintList";

    /**
     * The name of the extra, which is used to store the mode, which is used to tint the navigation
     * icon fof the toolbar, within a bundle.
     */
    private static final String TOOLBAR_NAVIGATION_ICON_TINT_MODE_EXTRA =
            TabSwitcherModel.class.getName() + "::ToolbarNavigationIconTintMode";

    /**
     * The name of the extra, which is used to store the duration, which must be reached when
     * loading the preview of tabs to use a fade animation, within a bundle.
     */
    private static final String TAB_PREVIEW_FADE_THRESHOLD_EXTRA =
            TabSwitcherModel.class.getName() + "::TabPreviewFadeThreshold";

    /**
     * The name of the extra, which is used to store the duration of the fade animation, which is
     * used to show the preview of tabs, within a bundle.
     */
    private static final String TAB_PREVIEW_FADE_DURATION =
            TabSwitcherModel.class.getName() + "::TabPreviewFadeDuration";

    /**
     * The name of the extra, which is used to store, whether saved states of tabs are cleared when
     * removing the corresponding tabs from the tabs switcher, or not, within a bundle.
     */
    private static final String CLEAR_SAVED_STATES_WHEN_REMOVING_TABS_EXTRA =
            TabSwitcherModel.class.getName() + "::ClearSavedStatesWhenRemovingTabs";

    /**
     * The tab switcher, the model belongs to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * A set, which contains the listeners, which are notified about the model's events.
     */
    private final Set<Listener> listeners;

    /**
     * The index of the tab, which is used as a reference, when restoring the positions of tabs.
     */
    private int referenceTabIndex;

    /**
     * The position of the tab, which is used as a reference, when restoring the positions of tabs.
     */
    private float referenceTabPosition;

    /**
     * The log level, which is used for logging.
     */
    private LogLevel logLevel;

    /**
     * A list, which contains the tabs, which are contained by the tab switcher.
     */
    private ArrayList<Tab> tabs;

    /**
     * True, if the tab switcher is currently shown, false otherwise.
     */
    private boolean switcherShown;

    /**
     * The currently selected tab.
     */
    private Tab selectedTab;

    /**
     * The decorator, which allows to inflate the views, which correspond to the tab switcher's
     * tabs.
     */
    private TabSwitcherDecorator decorator;

    /**
     * The adapter, which allows to inflate the views, which are associated with tabs.
     */
    private ContentRecyclerAdapter contentRecyclerAdapter;

    /**
     * An array, which contains the left, top, right and bottom padding of the tab switcher.
     */
    private int[] padding;

    /**
     * True, if the padding of tab switcher should is applied to the content of its tabs, false
     * otherwise.
     */
    private boolean applyPaddingToTabs;

    /**
     * The resource id of a tab's icon.
     */
    private int tabIconId;

    /**
     * The bitmap of a tab's icon.
     */
    private Bitmap tabIconBitmap;

    /**
     * The color state list, which is used to tint the icon of a tab.
     */
    private ColorStateList tabIconTintList;

    /**
     * The mode, which is used to tint the icon of a tab.
     */
    private PorterDuff.Mode tabIconTintMode;

    /**
     * The background color of a tab;
     */
    private ColorStateList tabBackgroundColor;

    /**
     * The background color of a tab's content.
     */
    private int tabContentBackgroundColor;

    /**
     * The text color of a tab's title.
     */
    private ColorStateList tabTitleTextColor;

    /**
     * The resource id of the icon of a tab's close button.
     */
    private int tabCloseButtonIconId;

    /**
     * The bitmap of the icon of a tab's close button.
     */
    private Bitmap tabCloseButtonIconBitmap;

    /**
     * The color state list, which is used to tint the close button of a tab.
     */
    private ColorStateList tabCloseButtonIconTintList;

    /**
     * The mode, which is used to tint the close button of a tab.
     */
    private PorterDuff.Mode tabCloseButtonIconTintMode;

    /**
     * The color of a tab's progress bar.
     */
    private int tabProgressBarColor;

    /**
     * The listener, which is notified, when the button, which allows to add a tab, has been
     * clicked.
     */
    private AddTabButtonListener addTabButtonListener;

    /**
     * The color of the button, which allows to add a new tab.
     */
    private ColorStateList addTabButtonColor;

    /**
     * True, if the toolbars should be shown, when the tab switcher is shown, false otherwise.
     */
    private boolean showToolbars;

    /**
     * The title of the toolbar, which is shown, when the tab switcher is shown.
     */
    private CharSequence toolbarTitle;

    /**
     * The navigation icon of the toolbar, which is shown, when the tab switcher is shown.
     */
    private Drawable toolbarNavigationIcon;

    /**
     * The color state list, which is used to tint the navigation icon of the toolbar, which is
     * shown, when the tab switcher is shown.
     */
    private ColorStateList toolbarNavigationIconTintList;

    /**
     * The mode, which is used to tint the navigation icon of the toolbar, which is shown, when the
     * tab switcher is shown.
     */
    private PorterDuff.Mode toolbarNavigationIconTintMode;

    /**
     * The listener, which is notified, when the navigation icon of the toolbar, which is shown,
     * when the tab switcher is shown, has been clicked.
     */
    private OnClickListener toolbarNavigationIconListener;

    /**
     * The resource id of the menu of the toolbar, which is shown, when the tab switcher is shown.
     */
    private int toolbarMenuId;

    /**
     * The duration, which must be reached when loading the preview of a tab to use a fade
     * animation.
     */
    private long tabPreviewFadeThreshold;

    /**
     * The duration of the fade animation, which is used to show the preview of tabs.
     */
    private long tabPreviewFadeDuration;

    /**
     * The view, which is shown, when the tab switcher is empty.
     */
    private View emptyView;

    /**
     * The duration of the fade animation, which is used to show or hide the view, which is shown,
     * when the tab switcher is empty.
     */
    private long emptyViewAnimationDuration;

    /**
     * True, if the saved states of tabs are cleared when the corresponding tabs are removed from
     * the tab switcher, false otherwise.
     */
    private boolean clearSavedStatesWhenRemovingTabs;

    /**
     * The listener, which is notified, when an item of the menu of the toolbar, which is shown,
     * when the tab switcher is shown, is clicked.
     */
    private OnMenuItemClickListener toolbarMenuItemListener;

    /**
     * A set, which contains the listeners, which should be notified, when a tab is about to be
     * closed by clicking its close button.
     */
    private final ListenerList<TabCloseListener> tabCloseListeners;

    /**
     * A set, which contains the listeners, which should be notified, when the previews of tabs are
     * about to be loaded.
     */
    private final ListenerList<TabPreviewListener> tabPreviewListeners;

    /**
     * Returns the index of a specific tab or throws a {@link NoSuchElementException}, if the model
     * does not contain the given tab.
     *
     * @param tab
     *         The tab, whose index should be returned, as an instance of the class {@link Tab}. The
     *         tab may not be null
     * @return The index of the given tab as an {@link Integer} value
     */
    private int indexOfOrThrowException(@NonNull final Tab tab) {
        int index = indexOf(tab);
        Condition.INSTANCE
                .ensureNotEqual(index, -1, "No such tab: " + tab, NoSuchElementException.class);
        return index;
    }

    /**
     * Sets, whether the tab switcher is currently shown, or not.
     *
     * @param shown
     *         True, if the tab switcher is currently shown, false otherwise
     * @return True, if the visibility of the tab switcher has been changed, false otherwise
     */
    private boolean setSwitcherShown(final boolean shown) {
        if (switcherShown != shown) {
            switcherShown = shown;
            return true;
        }

        return false;
    }

    /**
     * Notifies the listeners, that the log level has been changed.
     *
     * @param logLevel
     *         The log level, which has been set, as a value of the enum {@link LogLevel}. The log
     *         level may not be null
     */
    private void notifyOnLogLevelChanged(@NonNull final LogLevel logLevel) {
        for (Listener listener : listeners) {
            listener.onLogLevelChanged(logLevel);
        }
    }

    /**
     * Notifies the listeners, that the decorator has been changed.
     *
     * @param decorator
     *         The decorator, which has been set, as an instance of the class {@link
     *         TabSwitcherDecorator}. The decorator may not be null
     */
    private void notifyOnDecoratorChanged(@NonNull final TabSwitcherDecorator decorator) {
        for (Listener listener : listeners) {
            listener.onDecoratorChanged(decorator);
        }
    }

    /**
     * Notifies the listeners, that the tab switcher has been shown.
     */
    private void notifyOnSwitcherShown() {
        for (Listener listener : listeners) {
            listener.onSwitcherShown();
        }
    }

    /**
     * Notifies the listeners, that the tab switcher has been shown.
     */
    private void notifyOnSwitcherHidden() {
        for (Listener listener : listeners) {
            listener.onSwitcherHidden();
        }
    }

    /**
     * Notifies the listeners, that the currently selected tab has been changed.
     *
     * @param previousIndex
     *         The index of the previously selected tab as an {@link Integer} value or -1, if no tab
     *         was selected
     * @param index
     *         The index of the tab, which has been selected, as an {@link Integer} value or -1, if
     *         no tab has been selected
     * @param tab
     *         The tab, which has been selected, as an instance of the class {@link Tab} or null, if
     *         no tab has been selected
     * @param switcherHidden
     *         True, if selecting the tab caused the tab switcher to be hidden, false otherwise
     */
    private void notifyOnSelectionChanged(final int previousIndex, final int index,
                                          @Nullable final Tab tab, final boolean switcherHidden) {
        for (Listener listener : listeners) {
            listener.onSelectionChanged(previousIndex, index, tab, switcherHidden);
        }
    }

    /**
     * Notifies the listeners, that a specific tab has been added to the model.
     *
     * @param index
     *         The index, the tab has been added at, as an {@link Integer} value
     * @param tab
     *         The tab, which has been added, as an instance of the class {@link Tab}. The tab may
     *         not be null
     * @param previousSelectedTabIndex
     *         The index of the previously selected tab as an {@link Integer} value or -1, if no tab
     *         was selected
     * @param selectedTabIndex
     *         The index of the currently selected tab as an {@link Integer} value or -1, if the tab
     *         switcher does not contain any tabs
     * @param selectionChanged
     *         True, if the selection has changed, false otherwise
     * @param switcherVisibilityChanged
     *         True, if adding the tab caused the visibility of the tab switcher to be changed,
     *         false otherwise
     * @param animation
     *         The animation, which has been used to add the tab, as an instance of the class {@link
     *         Animation}. The animation may not be null
     */
    private void notifyOnTabAdded(final int index, @NonNull final Tab tab,
                                  final int previousSelectedTabIndex, final int selectedTabIndex,
                                  final boolean selectionChanged,
                                  final boolean switcherVisibilityChanged,
                                  @NonNull final Animation animation) {
        for (Listener listener : listeners) {
            listener.onTabAdded(index, tab, previousSelectedTabIndex, selectedTabIndex,
                    selectionChanged, switcherVisibilityChanged, animation);
        }
    }

    /**
     * Notifies the listeners, that multiple tabs have been added to the model.
     *
     * @param index
     *         The index of the tab, which has been added, as an {@link Integer} value
     * @param tabs
     *         An array, which contains the tabs, which have been added, as an array of the type
     *         {@link Tab}. The array may not be null
     * @param previousSelectedTabIndex
     *         The index of the previously selected tab as an {@link Integer} value or -1, if no tab
     *         was selected
     * @param selectedTabIndex
     *         The index of the currently selected tab as an {@link Integer} value or -1, if the tab
     *         switcher does not contain any tabs
     * @param selectionChanged
     *         True, if the selection has changed, false otherwise
     * @param animation
     *         The animation, which has been used to add the tabs, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    private void notifyOnAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                      final int previousSelectedTabIndex,
                                      final int selectedTabIndex, final boolean selectionChanged,
                                      @NonNull final Animation animation) {
        for (Listener listener : listeners) {
            listener.onAllTabsAdded(index, tabs, previousSelectedTabIndex, selectedTabIndex,
                    selectionChanged, animation);
        }
    }

    /**
     * Notifies the listeners, that a tab has been removed from the model.
     *
     * @param index
     *         The index of the tab, which has been removed, as an {@link Integer} value
     * @param tab
     *         The tab, which has been removed, as an instance of the class {@link Tab}. The tab may
     *         not be null
     * @param previousSelectedTabIndex
     *         The index of the previously selected tab as an {@link Integer} value or -1, if no tab
     *         was selected
     * @param selectedTabIndex
     *         The index of the currently selected tab as an {@link Integer} value or -1, if the tab
     *         switcher does not contain any tabs
     * @param selectionChanged
     *         True, if the selection has changed, false otherwiseF
     * @param animation
     *         The animation, which has been used to remove the tab, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    private void notifyOnTabRemoved(final int index, @NonNull final Tab tab,
                                    final int previousSelectedTabIndex, final int selectedTabIndex,
                                    final boolean selectionChanged,
                                    @NonNull final Animation animation) {
        for (Listener listener : listeners) {
            listener.onTabRemoved(index, tab, previousSelectedTabIndex, selectedTabIndex,
                    selectionChanged, animation);
        }
    }

    /**
     * Notifies the listeners, that all tabs have been removed.
     *
     * @param tabs
     *         An array, which contains the tabs, which have been removed, as an array of the type
     *         {@link Tab} or an empty array, if no tabs have been removed
     * @param animation
     *         The animation, which has been used to remove the tabs, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    private void notifyOnAllTabsRemoved(@NonNull final Tab[] tabs,
                                        @NonNull final Animation animation) {
        for (Listener listener : listeners) {
            listener.onAllTabsRemoved(tabs, animation);
        }
    }

    /**
     * Notifies the listeners, that the padding has been changed.
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
    private void notifyOnPaddingChanged(final int left, final int top, final int right,
                                        final int bottom) {
        for (Listener listener : listeners) {
            listener.onPaddingChanged(left, top, right, bottom);
        }
    }

    /**
     * Notifies the listeners, that it has been changed, whether the padding of the tab switcher is
     * applied to the content of its tabs, or not.
     *
     * @param applyPaddingToTabs
     *         True, if the padding of the tab switcher is applied to the content of its tabs, false
     *         otherwise
     */
    private void notifyOnApplyPaddingToTabsChanged(final boolean applyPaddingToTabs) {
        for (Listener listener : listeners) {
            listener.onApplyPaddingToTabsChanged(applyPaddingToTabs);
        }
    }

    /**
     * Notifies the listeners, that the default icon of a tab has been changed.
     *
     * @param icon
     *         The icon, which has been set, as an instance of the class {@link Drawable} or null,
     *         if no icon is set
     */
    private void notifyOnTabIconChanged(@Nullable final Drawable icon) {
        for (Listener listener : listeners) {
            listener.onTabIconChanged(icon);
        }
    }

    /**
     * Notifies the listeners, that the default background color of a tab has been changed.
     *
     * @param colorStateList
     *         The color state list, which has been set, as an instance of the class {@link
     *         ColorStateList} or null, if the default color should be used
     */
    private void notifyOnTabBackgroundColorChanged(@Nullable final ColorStateList colorStateList) {
        for (Listener listener : listeners) {
            listener.onTabBackgroundColorChanged(colorStateList);
        }
    }

    /**
     * Notifies the listeners, the the default background color of a tab's content has been
     * changed.
     *
     * @param color
     *         The color, which has been set, as an {@link Integer} value
     */
    private void notifyOnTabContentBackgroundColorChanged(@ColorInt final int color) {
        for (Listener listener : listeners) {
            listener.onTabContentBackgroundColorChanged(color);
        }
    }

    /**
     * Notifies the listeners, that the default text color of a tab's title has been changed.
     *
     * @param colorStateList
     *         The color state list, which has been set, as an instance of the class {@link
     *         ColorStateList} or null, if the default color should be used
     */
    private void notifyOnTabTitleColorChanged(@Nullable final ColorStateList colorStateList) {
        for (Listener listener : listeners) {
            listener.onTabTitleColorChanged(colorStateList);
        }
    }

    /**
     * Notifies the listeners, that the icon of a tab's close button has been changed.
     *
     * @param icon
     *         The icon, which has been set, as an instance of the class {@link Drawable} or null,
     *         if the default icon should be used
     */
    private void notifyOnTabCloseButtonIconChanged(@Nullable final Drawable icon) {
        for (Listener listener : listeners) {
            listener.onTabCloseButtonIconChanged(icon);
        }
    }

    /**
     * Notifies the listeners, that the color of a tab's progress bar has been changed.
     *
     * @param color
     *         The color, which has been set, as an {@link Integer} value or -1, if the default
     *         color should be used
     */
    private void notifyOnTabProgressBarColorChanged(@ColorInt final int color) {
        for (Listener listener : listeners) {
            listener.onTabProgressBarColorChanged(color);
        }
    }

    /**
     * Notifies the listeners, that is has been changed, whether the button, which allows to add a
     * new tab, should be shown, or not.
     *
     * @param visible
     *         True, if the button, which allows to add a new tab, should be shown, false otherwise
     */
    private void notifyOnAddTabButtonVisibilityChanged(final boolean visible) {
        for (Listener listener : listeners) {
            listener.onAddTabButtonVisibilityChanged(visible);
        }
    }

    /**
     * Notifies the listeners, that the color of the button, which allows to add a new tab, has been
     * changed.
     *
     * @param colorStateList
     *         The color, which has been set, as an instance of the class {@link ColorStateList} or
     *         null, if the default color should be used
     */
    private void notifyOnAddTabButtonColorChanged(@Nullable final ColorStateList colorStateList) {
        for (Listener listener : listeners) {
            listener.onAddTabButtonColorChanged(colorStateList);
        }
    }

    /**
     * Notifies the listeners, that it has been changed, whether the toolbars should be shown, when
     * the tab switcher is shown, or not.
     *
     * @param visible
     *         True, if the toolbars should be shown, when the tab switcher is shown, false
     *         otherwise
     */
    private void notifyOnToolbarVisibilityChanged(final boolean visible) {
        for (Listener listener : listeners) {
            listener.onToolbarVisibilityChanged(visible);
        }
    }

    /**
     * Notifies the listeners, that the title of the toolbar, which is shown, when the tab switcher
     * is shown, has been changed.
     *
     * @param title
     *         The title, which has been set, as an instance of the type {@link CharSequence} or
     *         null, if no title is set
     */
    private void notifyOnToolbarTitleChanged(@Nullable final CharSequence title) {
        for (Listener listener : listeners) {
            listener.onToolbarTitleChanged(title);
        }
    }

    /**
     * Notifies the listeners, that the menu of the toolbar, which is shown, when the tab switcher
     * is shown, has been inflated.
     *
     * @param resourceId
     *         The resource id of the menu, which has been inflated, as an {@link Integer} value.
     *         The resource id must correspond to a valid menu resource
     * @param menuItemClickListener
     *         The listener, which has been registered to be notified, when an item of the menu has
     *         been clicked, as an instance of the type OnMenuItemClickListener or null, if no
     *         listener should be notified
     */
    private void notifyOnToolbarMenuInflated(@MenuRes final int resourceId,
                                             @Nullable final OnMenuItemClickListener menuItemClickListener) {
        for (Listener listener : listeners) {
            listener.onToolbarMenuInflated(resourceId, menuItemClickListener);
        }
    }

    /**
     * Notifies the listeners, that the navigation icon of the toolbar, which is shown, when the tab
     * switcher is shown, has been changed.
     *
     * @param icon
     *         The navigation icon, which has been set, as an instance of the class {@link Drawable}
     *         or null, if no navigation icon is set
     * @param clickListener
     *         The listener, which should be notified, when the navigation item has been clicked, as
     *         an instance of the type {@link OnClickListener} or null, if no listener should be
     *         notified
     */
    private void notifyOnToolbarNavigationIconChanged(@Nullable final Drawable icon,
                                                      @Nullable final OnClickListener clickListener) {
        for (Listener listener : listeners) {
            listener.onToolbarNavigationIconChanged(icon, clickListener);
        }
    }

    /**
     * Notifies the listeners, that the view, which is shown, when the tab switcher is empty, has
     * been changed.
     *
     * @param view
     *         The view, which has been set, as an instance of the class {@link View} or null, if no
     *         view should be shown, when the tab switcher is empty
     * @param animationDuration
     *         The duration of the fade animation, which is used to show or hide the view, in
     *         milliseconds as a {@link Long} value. The duration must be at least 0 or -1, if the
     *         default duration should be used
     */
    private void notifyOnEmptyViewChanged(@Nullable final View view, final long animationDuration) {
        for (Listener listener : listeners) {
            listener.onEmptyViewChanged(view, animationDuration);
        }
    }

    /**
     * Creates a new model of a {@link TabSwitcher}.
     *
     * @param tabSwitcher
     *         The tab switcher, the model belongs to, as an instance of the class {@link
     *         ViewGroup}. The parent may not be null
     */
    public TabSwitcherModel(@NonNull final TabSwitcher tabSwitcher) {
        Condition.INSTANCE.ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        this.tabSwitcher = tabSwitcher;
        this.listeners = new LinkedHashSet<>();
        this.referenceTabIndex = -1;
        this.referenceTabPosition = -1;
        this.logLevel = LogLevel.INFO;
        this.tabs = new ArrayList<>();
        this.switcherShown = false;
        this.selectedTab = null;
        this.decorator = null;
        this.contentRecyclerAdapter = null;
        this.padding = new int[]{0, 0, 0, 0};
        this.applyPaddingToTabs = true;
        this.tabIconId = -1;
        this.tabIconBitmap = null;
        this.tabBackgroundColor = null;
        this.tabContentBackgroundColor = -1;
        this.tabTitleTextColor = null;
        this.tabCloseButtonIconId = -1;
        this.tabCloseButtonIconBitmap = null;
        this.tabProgressBarColor = -1;
        this.addTabButtonListener = null;
        this.addTabButtonColor = null;
        this.showToolbars = false;
        this.toolbarTitle = null;
        this.tabPreviewFadeThreshold = 200;
        this.tabPreviewFadeDuration =
                getContext().getResources().getInteger(android.R.integer.config_longAnimTime);
        this.clearSavedStatesWhenRemovingTabs = true;
        this.toolbarNavigationIcon = null;
        this.toolbarNavigationIconListener = null;
        this.toolbarMenuId = -1;
        this.toolbarMenuItemListener = null;
        this.tabCloseListeners = new ListenerList<>();
        this.tabPreviewListeners = new ListenerList<>();
    }

    /**
     * Adds a new listener, which should be notified about the model's events.
     *
     * @param listener
     *         The listener, which should be added, as an instance of the type {@link Listener}. The
     *         listener may not be null
     */
    public final void addListener(@NonNull final Listener listener) {
        Condition.INSTANCE.ensureNotNull(listener, "The listener may not be null");
        listeners.add(listener);
    }

    /**
     * Removes a specific listener, which should not be notified about the model's events, anymore.
     *
     * @param listener
     *         The listener, which should be removed, as an instance of the type {@link Listener}.
     *         The listener may not be null
     */
    public final void removeListener(@NonNull final Listener listener) {
        Condition.INSTANCE.ensureNotNull(listener, "The listener may not be null");
        listeners.remove(listener);
    }

    /**
     * Returns the index of the tab, which is used as a reference, when restoring the positions of
     * tabs.
     *
     * @return The index of the tab, which is used as a reference, when restoring the positions of
     * tabs, as an {@link Integer} value or -1, if the positions of tabs should not be restored
     */
    public final int getReferenceTabIndex() {
        return referenceTabIndex;
    }

    /**
     * Sets the index of the tab, which should be used as a reference, when restoring the positions
     * of tabs.
     *
     * @param referenceTabIndex
     *         The index, which should be set, as an {@link Integer} value or -1, if the positions
     *         of tabs should not be restored
     */
    public final void setReferenceTabIndex(final int referenceTabIndex) {
        this.referenceTabIndex = referenceTabIndex;
    }

    /**
     * Returns the position of the tab, which is used as a reference, when restoring the positions
     * of tabs.
     *
     * @return The position of the tab, which is used as a reference, when restoring the positions
     * of tabs, in relation to the available space as a {@link Float} value or -1, if the positions
     * of tabs should not be restored
     */
    public final float getReferenceTabPosition() {
        return referenceTabPosition;
    }

    /**
     * Sets the position of the tab, which should be used as a reference, when restoring the
     * positions of tabs.
     *
     * @param referenceTabPosition
     *         The position, which should be set, in relation to the available space as a {@link
     *         Float} value or -1, if the position of tabs should not be restored
     */
    public final void setReferenceTabPosition(final float referenceTabPosition) {
        this.referenceTabPosition = referenceTabPosition;
    }

    /**
     * Returns the listener, which is notified, when the button, which allows to add a new tab, has
     * been clicked.
     *
     * @return The listener, which is notified, when the button, which allows to add a new tab, has
     * been clicked, as an instance of the type {@link AddTabButtonListener} or null, if the button
     * is not shown
     */
    @Nullable
    public final AddTabButtonListener getAddTabButtonListener() {
        return addTabButtonListener;
    }

    /**
     * Returns the listener, which is notified, when the navigation icon of the toolbar, which is
     * shown, when the tab switcher is shown, has been clicked.
     *
     * @return The listener, which is notified, when the navigation icon of the toolbar, which is
     * shown, when the tab switcher is shown, has been clicked as an instance of the type {@link
     * OnClickListener} or null, if no listener should be notified
     */
    @Nullable
    public final OnClickListener getToolbarNavigationIconListener() {
        return toolbarNavigationIconListener;
    }

    /**
     * Returns the resource id of the menu of the toolbar, which is shown, when the tab switcher is
     * shown.
     *
     * @return The resource id of the menu of the toolbar, which is shown, when the tab switcher is
     * shown, as an {@link Integer} value. The resource id must correspond to a valid menu resource
     */
    @MenuRes
    public final int getToolbarMenuId() {
        return toolbarMenuId;
    }

    /**
     * Returns the listener, which is notified, when an item of the menu of the toolbar, which is
     * shown, when the tab switcher is shown, has been clicked.
     *
     * @return The listener, which is notified, when an item of the menu of the toolbar, which is
     * shown, when the tab switcher is shown, has been clicked as an instance of the type
     * OnMenuItemClickListener or null, if no listener should be notified
     */
    @Nullable
    public final OnMenuItemClickListener getToolbarMenuItemListener() {
        return toolbarMenuItemListener;
    }

    /**
     * Returns the duration of the fade animation, which should be used to show or hide the view,
     * which is shown, when the tab switcher is empty.
     *
     * @return The duration of the fade animation, which should be used to show or hide the view,
     * which is shown, when the tab switcher is empty, in milliseconds as a {@link Long} value
     */
    public final long getEmptyViewAnimationDuration() {
        return emptyViewAnimationDuration;
    }

    /**
     * Returns the listeners, which should be notified, when a tab is about to be closed by clicking
     * its close button.
     *
     * @return A list, which contains the listeners, which should be notified, when a tab is about
     * to be closed by clicking its close button, as an instance of the type ListenerList or an
     * empty list, if no listeners should be notified
     */
    @NonNull
    public final ListenerList<TabCloseListener> getTabCloseListeners() {
        return tabCloseListeners;
    }

    /**
     * Returns the listeners, which should be notified, when the previews of tabs are about to be
     * loaded.
     *
     * @return A list, which contains the listeners, which should be notified, when the previews of
     * tabs are about to be loaded, as an instance of the type ListenerList or an empty list, if no
     * listeners should be notified
     */
    @NonNull
    public final ListenerList<TabPreviewListener> getTabPreviewListeners() {
        return tabPreviewListeners;
    }

    /**
     * Returns the adapter, which allows to inflate the views, which are associated with tabs.
     *
     * @return The adapter, which allows to inflate the views, which are associated with tabs, as an
     * instance of the class {@link ContentRecyclerAdapter}
     */
    public final ContentRecyclerAdapter getContentRecyclerAdapter() {
        Condition.INSTANCE.ensureNotNull(contentRecyclerAdapter, "No decorator has been set",
                IllegalStateException.class);
        return contentRecyclerAdapter;
    }

    @NonNull
    @Override
    public final Context getContext() {
        return tabSwitcher.getContext();
    }

    @Override
    public final void setDecorator(@NonNull final TabSwitcherDecorator decorator) {
        Condition.INSTANCE.ensureNotNull(decorator, "The decorator may not be null");
        this.decorator = decorator;
        this.contentRecyclerAdapter = new ContentRecyclerAdapter(tabSwitcher, decorator);
        notifyOnDecoratorChanged(decorator);
    }

    @Override
    public final TabSwitcherDecorator getDecorator() {
        return decorator;
    }

    @NonNull
    @Override
    public final LogLevel getLogLevel() {
        return logLevel;
    }

    @Override
    public final void setLogLevel(@NonNull final LogLevel logLevel) {
        Condition.INSTANCE.ensureNotNull(logLevel, "The log level may not be null");
        this.logLevel = logLevel;
        notifyOnLogLevelChanged(logLevel);
    }

    @Override
    public final boolean isEmpty() {
        return tabs.isEmpty();
    }

    @Override
    public final int getCount() {
        return tabs.size();
    }

    @NonNull
    @Override
    public final Tab getTab(final int index) {
        return tabs.get(index);
    }

    @Override
    public final int indexOf(@NonNull final Tab tab) {
        Condition.INSTANCE.ensureNotNull(tab, "The tab may not be null");
        return tabs.indexOf(tab);
    }

    @Override
    public final void addTab(@NonNull Tab tab) {
        addTab(tab, getCount());
    }

    @Override
    public final void addTab(@NonNull final Tab tab, final int index) {
        addTab(tab, index, new SwipeAnimation.Builder().create());
    }

    @Override
    public final void addTab(@NonNull final Tab tab, final int index,
                             @NonNull final Animation animation) {
        Condition.INSTANCE.ensureNotNull(tab, "The tab may not be null");
        Condition.INSTANCE.ensureNotNull(animation, "The animation may not be null");
        tabs.add(index, tab);
        int previousSelectedTabIndex = getSelectedTabIndex();
        int selectedTabIndex = previousSelectedTabIndex;
        boolean selectionChanged = false;
        boolean switcherVisibilityChanged = false;

        if (previousSelectedTabIndex == -1) {
            selectedTab = tab;
            selectedTabIndex = index;
            selectionChanged = true;
        }

        if (animation instanceof RevealAnimation) {
            selectedTab = tab;
            selectedTabIndex = index;
            selectionChanged = true;
            switcherVisibilityChanged = setSwitcherShown(false);
        }

        if (animation instanceof PeekAnimation && getCount() > 1) {
            switcherVisibilityChanged = setSwitcherShown(true);
        }

        notifyOnTabAdded(index, tab, previousSelectedTabIndex, selectedTabIndex, selectionChanged,
                switcherVisibilityChanged, animation);
    }

    @Override
    public final void addAllTabs(@NonNull final Collection<? extends Tab> tabs) {
        addAllTabs(tabs, getCount());
    }

    @Override
    public final void addAllTabs(@NonNull final Collection<? extends Tab> tabs, final int index) {
        addAllTabs(tabs, index, new SwipeAnimation.Builder().create());
    }

    @Override
    public final void addAllTabs(@NonNull final Collection<? extends Tab> tabs, final int index,
                                 @NonNull final Animation animation) {
        Condition.INSTANCE.ensureNotNull(tabs, "The collection may not be null");
        Tab[] array = new Tab[tabs.size()];
        tabs.toArray(array);
        addAllTabs(array, index, animation);
    }

    @Override
    public final void addAllTabs(@NonNull final Tab[] tabs) {
        addAllTabs(tabs, getCount());
    }

    @Override
    public final void addAllTabs(@NonNull final Tab[] tabs, final int index) {
        addAllTabs(tabs, index, new SwipeAnimation.Builder().create());
    }

    @Override
    public final void addAllTabs(@NonNull final Tab[] tabs, final int index,
                                 @NonNull final Animation animation) {
        Condition.INSTANCE.ensureNotNull(tabs, "The array may not be null");
        Condition.INSTANCE.ensureNotNull(animation, "The animation may not be null");

        if (tabs.length > 0) {
            int previousSelectedTabIndex = getSelectedTabIndex();
            int selectedTabIndex = previousSelectedTabIndex;
            boolean selectionChanged = false;

            for (int i = 0; i < tabs.length; i++) {
                Tab tab = tabs[i];
                this.tabs.add(index + i, tab);
            }

            if (previousSelectedTabIndex == -1) {
                selectedTabIndex = 0;
                selectedTab = tabs[selectedTabIndex];
                selectionChanged = true;
            }

            notifyOnAllTabsAdded(index, tabs, previousSelectedTabIndex, selectedTabIndex,
                    selectionChanged, animation);
        }
    }

    @Override
    public final void removeTab(@NonNull final Tab tab) {
        removeTab(tab, new SwipeAnimation.Builder().create());
    }

    @Override
    public final void removeTab(@NonNull final Tab tab, @NonNull final Animation animation) {
        Condition.INSTANCE.ensureNotNull(tab, "The tab may not be null");
        Condition.INSTANCE.ensureNotNull(animation, "The animation may not be null");
        int index = indexOfOrThrowException(tab);
        int previousSelectedTabIndex = getSelectedTabIndex();
        int selectedTabIndex = previousSelectedTabIndex;
        boolean selectionChanged = false;
        tabs.remove(index);

        if (isEmpty()) {
            selectedTabIndex = -1;
            selectedTab = null;
            selectionChanged = true;
        } else if (index == previousSelectedTabIndex) {
            if (index > 0) {
                selectedTabIndex = index - 1;
            }

            selectedTab = getTab(selectedTabIndex);
            selectionChanged = true;
        }

        notifyOnTabRemoved(index, tab, previousSelectedTabIndex, selectedTabIndex, selectionChanged,
                animation);

    }

    @Override
    public final void clear() {
        clear(new SwipeAnimation.Builder().create());
    }

    @Override
    public final void clear(@NonNull final Animation animation) {
        Condition.INSTANCE.ensureNotNull(animation, "The animation may not be null");
        Tab[] result = new Tab[tabs.size()];
        tabs.toArray(result);
        tabs.clear();
        notifyOnAllTabsRemoved(result, animation);
        selectedTab = null;
    }

    @Override
    public final boolean isSwitcherShown() {
        return tabSwitcher.getLayout() == Layout.TABLET || switcherShown;
    }

    @Override
    public final void showSwitcher() {
        setSwitcherShown(true);
        notifyOnSwitcherShown();
    }

    @Override
    public final void hideSwitcher() {
        setSwitcherShown(false);
        notifyOnSwitcherHidden();
    }

    @Override
    public final void toggleSwitcherVisibility() {
        if (isSwitcherShown()) {
            hideSwitcher();
        } else {
            showSwitcher();
        }
    }

    @Nullable
    @Override
    public final Tab getSelectedTab() {
        return selectedTab;
    }

    @Override
    public final int getSelectedTabIndex() {
        return selectedTab != null ? indexOf(selectedTab) : -1;
    }

    @Override
    public final void selectTab(@NonNull final Tab tab) {
        Condition.INSTANCE.ensureNotNull(tab, "The tab may not be null");
        int previousIndex = getSelectedTabIndex();
        int index = indexOfOrThrowException(tab);
        selectedTab = tab;
        boolean switcherHidden = setSwitcherShown(false);
        notifyOnSelectionChanged(previousIndex, index, tab, switcherHidden);
    }

    @Override
    public final void selectTab(final int index) {
        selectTab(getTab(index));
    }

    @Override
    public final Iterator<Tab> iterator() {
        return tabs.iterator();
    }

    @Override
    public final void setPadding(final int left, final int top, final int right, final int bottom) {
        padding = new int[]{left, top, right, bottom};
        notifyOnPaddingChanged(left, top, right, bottom);
    }

    @Override
    public final int getPaddingLeft() {
        return padding[0];
    }

    @Override
    public final int getPaddingTop() {
        return padding[1];
    }

    @Override
    public final int getPaddingRight() {
        return padding[2];
    }

    @Override
    public final int getPaddingBottom() {
        return padding[3];
    }

    @Override
    public final int getPaddingStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return tabSwitcher.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ?
                    getPaddingRight() : getPaddingLeft();
        }

        return getPaddingLeft();
    }

    @Override
    public final int getPaddingEnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return tabSwitcher.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ?
                    getPaddingLeft() : getPaddingRight();
        }

        return getPaddingRight();
    }

    @Override
    public final void applyPaddingToTabs(final boolean applyPaddingToTabs) {
        this.applyPaddingToTabs = applyPaddingToTabs;
        notifyOnApplyPaddingToTabsChanged(applyPaddingToTabs);
    }

    @Override
    public final boolean isPaddingAppliedToTabs() {
        return applyPaddingToTabs;
    }

    @Nullable
    @Override
    public final Drawable getTabIcon() {
        if (tabIconId != -1) {
            return AppCompatResources.getDrawable(getContext(), tabIconId);
        } else {
            return tabIconBitmap != null ?
                    new BitmapDrawable(getContext().getResources(), tabIconBitmap) : null;
        }
    }

    @Override
    public final void setTabIcon(@DrawableRes final int resourceId) {
        this.tabIconId = resourceId;
        this.tabIconBitmap = null;
        notifyOnTabIconChanged(getTabIcon());
    }

    @Override
    public final void setTabIcon(@Nullable final Bitmap icon) {
        this.tabIconId = -1;
        this.tabIconBitmap = icon;
        notifyOnTabIconChanged(getTabIcon());
    }

    @Override
    public final ColorStateList getTabIconTintList() {
        return tabIconTintList;
    }

    @Override
    public final void setTabIconTint(@ColorInt final int color) {
        setTabIconTintList(ColorStateList.valueOf(color));
    }

    @Override
    public final void setTabIconTintList(@Nullable final ColorStateList tintList) {
        this.tabIconTintList = tintList;
        notifyOnTabIconChanged(getTabIcon());
    }

    @Override
    public final PorterDuff.Mode getTabIconTintMode() {
        return tabIconTintMode;
    }

    @Override
    public final void setTabIconTintMode(@Nullable final PorterDuff.Mode mode) {
        this.tabIconTintMode = mode;
        notifyOnTabIconChanged(getTabIcon());
    }

    @Nullable
    @Override
    public final ColorStateList getTabBackgroundColor() {
        return tabBackgroundColor;
    }

    @Override
    public final void setTabBackgroundColor(@ColorInt final int color) {
        setTabBackgroundColor(color != -1 ? ColorStateList.valueOf(color) : null);
    }

    @Override
    public final void setTabBackgroundColor(@Nullable final ColorStateList colorStateList) {
        this.tabBackgroundColor = colorStateList;
        notifyOnTabBackgroundColorChanged(colorStateList);
    }

    @ColorInt
    @Override
    public final int getTabContentBackgroundColor() {
        return tabContentBackgroundColor;
    }

    @Override
    public final void setTabContentBackgroundColor(@ColorInt final int color) {
        this.tabContentBackgroundColor = color;
        notifyOnTabContentBackgroundColorChanged(color);
    }

    @Nullable
    @Override
    public final ColorStateList getTabTitleTextColor() {
        return tabTitleTextColor;
    }

    @Override
    public final void setTabTitleTextColor(@ColorInt final int color) {
        setTabTitleTextColor(color != -1 ? ColorStateList.valueOf(color) : null);
    }

    @Override
    public final void setTabTitleTextColor(@Nullable final ColorStateList colorStateList) {
        this.tabTitleTextColor = colorStateList;
        notifyOnTabTitleColorChanged(colorStateList);
    }

    @Nullable
    @Override
    public final Drawable getTabCloseButtonIcon() {
        if (tabCloseButtonIconId != -1) {
            return AppCompatResources.getDrawable(getContext(), tabCloseButtonIconId);
        } else {
            return tabCloseButtonIconBitmap != null ?
                    new BitmapDrawable(getContext().getResources(), tabCloseButtonIconBitmap) :
                    null;
        }
    }

    @Override
    public final int getTabProgressBarColor() {
        return tabProgressBarColor;
    }

    @Override
    public final void setTabProgressBarColor(@ColorInt final int color) {
        this.tabProgressBarColor = color;
        notifyOnTabProgressBarColorChanged(color);
    }

    @Override
    public final void setTabCloseButtonIcon(@DrawableRes final int resourceId) {
        tabCloseButtonIconId = resourceId;
        tabCloseButtonIconBitmap = null;
        notifyOnTabCloseButtonIconChanged(getTabCloseButtonIcon());
    }

    @Override
    public final void setTabCloseButtonIcon(@Nullable final Bitmap icon) {
        tabCloseButtonIconId = -1;
        tabCloseButtonIconBitmap = icon;
        notifyOnTabCloseButtonIconChanged(getTabCloseButtonIcon());
    }

    @Override
    public final ColorStateList getTabCloseButtonIconTintList() {
        return tabCloseButtonIconTintList;
    }

    @Override
    public final void setTabCloseButtonIconTint(@ColorInt final int color) {
        setTabCloseButtonIconTintList(ColorStateList.valueOf(color));
    }

    @Override
    public final void setTabCloseButtonIconTintList(@Nullable final ColorStateList tintList) {
        this.tabCloseButtonIconTintList = tintList;
    }

    @Override
    public final PorterDuff.Mode getTabCloseButtonIconTintMode() {
        return tabCloseButtonIconTintMode;
    }

    @Override
    public final void setTabCloseButtonIconTintMode(@Nullable final PorterDuff.Mode mode) {
        this.tabCloseButtonIconTintMode = mode;
        notifyOnTabIconChanged(getTabIcon());
    }

    @Override
    public final boolean isAddTabButtonShown() {
        return tabSwitcher.getLayout() == Layout.TABLET && addTabButtonListener != null;
    }

    @Override
    public final void showAddTabButton(@Nullable final AddTabButtonListener listener) {
        this.addTabButtonListener = listener;
        notifyOnAddTabButtonVisibilityChanged(listener != null);
    }

    @Nullable
    @Override
    public final ColorStateList getAddTabButtonColor() {
        return addTabButtonColor;
    }

    @Override
    public final void setAddTabButtonColor(@ColorInt final int color) {
        setAddTabButtonColor(color != -1 ? ColorStateList.valueOf(color) : null);
    }

    @Override
    public final void setAddTabButtonColor(@Nullable final ColorStateList colorStateList) {
        this.addTabButtonColor = colorStateList;
        notifyOnAddTabButtonColorChanged(colorStateList);
    }

    @Override
    public final boolean areToolbarsShown() {
        return showToolbars;
    }

    @Override
    public final void showToolbars(final boolean show) {
        this.showToolbars = show;
        notifyOnToolbarVisibilityChanged(show);
    }

    @Nullable
    @Override
    public final CharSequence getToolbarTitle() {
        return toolbarTitle;
    }

    @Override
    public void setToolbarTitle(@StringRes final int resourceId) {
        setToolbarTitle(getContext().getText(resourceId));
    }

    @Override
    public final void setToolbarTitle(@Nullable final CharSequence title) {
        this.toolbarTitle = title;
        notifyOnToolbarTitleChanged(title);
    }

    @Nullable
    @Override
    public final Drawable getToolbarNavigationIcon() {
        return toolbarNavigationIcon;
    }

    @Override
    public final void setToolbarNavigationIcon(@DrawableRes final int resourceId,
                                               @Nullable final OnClickListener listener) {
        setToolbarNavigationIcon(AppCompatResources.getDrawable(getContext(), resourceId),
                listener);
    }

    @Override
    public final void setToolbarNavigationIcon(@Nullable final Drawable icon,
                                               @Nullable final OnClickListener listener) {
        this.toolbarNavigationIcon = icon;
        this.toolbarNavigationIconListener = listener;
        notifyOnToolbarNavigationIconChanged(icon, listener);
    }

    @Override
    public final ColorStateList getToolbarNavigationIconTintList() {
        return toolbarNavigationIconTintList;
    }

    @Override
    public final void setToolbarNavigationIconTint(@ColorInt final int color) {
        setToolbarNavigationIconTintList(ColorStateList.valueOf(color));
    }

    @Override
    public final void setToolbarNavigationIconTintList(@Nullable final ColorStateList tintList) {
        this.toolbarNavigationIconTintList = tintList;
        notifyOnToolbarNavigationIconChanged(toolbarNavigationIcon, toolbarNavigationIconListener);
    }

    @Override
    public final PorterDuff.Mode getToolbarNavigationIconTintMode() {
        return toolbarNavigationIconTintMode;
    }

    @Override
    public final void setToolbarNavigationIconTintMode(@Nullable final PorterDuff.Mode mode) {
        this.toolbarNavigationIconTintMode = mode;
        notifyOnToolbarNavigationIconChanged(toolbarNavigationIcon, toolbarNavigationIconListener);
    }

    @Override
    public final void inflateToolbarMenu(@MenuRes final int resourceId,
                                         @Nullable final OnMenuItemClickListener listener) {
        this.toolbarMenuId = resourceId;
        this.toolbarMenuItemListener = listener;
        notifyOnToolbarMenuInflated(resourceId, listener);
    }

    @Override
    public final long getTabPreviewFadeThreshold() {
        return tabPreviewFadeThreshold;
    }

    @Override
    public final void setTabPreviewFadeThreshold(final long threshold) {
        Condition.INSTANCE.ensureAtLeast(threshold, 0, "The threshold must be at least 0");
        this.tabPreviewFadeThreshold = threshold;
    }

    @Override
    public final long getTabPreviewFadeDuration() {
        return tabPreviewFadeDuration;
    }

    @Override
    public final void setTabPreviewFadeDuration(final long duration) {
        Condition.INSTANCE.ensureAtLeast(duration, 0, "The duration must be at least 0");
        this.tabPreviewFadeDuration = duration;
    }

    @Nullable
    @Override
    public final View getEmptyView() {
        return emptyView;
    }

    @Override
    public final void setEmptyView(@Nullable final View view) {
        setEmptyView(view, -1);
    }

    @Override
    public final void setEmptyView(@Nullable final View view, final long animationDuration) {
        this.emptyView = view;
        this.emptyViewAnimationDuration = view != null ? animationDuration : -1;
        notifyOnEmptyViewChanged(emptyView, animationDuration);
    }

    @Override
    public final void setEmptyView(@LayoutRes final int resourceId) {
        setEmptyView(resourceId, -1);
    }

    @Override
    public final void setEmptyView(@LayoutRes final int resourceId, final long animationDuration) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        setEmptyView(inflater.inflate(resourceId, tabSwitcher, false), animationDuration);
    }

    @Override
    public final boolean areSavedStatesClearedWhenRemovingTabs() {
        return clearSavedStatesWhenRemovingTabs;
    }

    @Override
    public final void clearSavedStatesWhenRemovingTabs(final boolean clear) {
        this.clearSavedStatesWhenRemovingTabs = clear;
    }

    @Override
    public final void addCloseTabListener(@NonNull final TabCloseListener listener) {
        Condition.INSTANCE.ensureNotNull(listener, "The listener may not be null");
        tabCloseListeners.add(listener);
    }

    @Override
    public final void removeCloseTabListener(@NonNull final TabCloseListener listener) {
        Condition.INSTANCE.ensureNotNull(listener, "The listener may not be null");
        tabCloseListeners.remove(listener);
    }

    @Override
    public final void addTabPreviewListener(@NonNull final TabPreviewListener listener) {
        Condition.INSTANCE.ensureNotNull(listener, "The listener may not be null");
        tabPreviewListeners.add(listener);
    }

    @Override
    public final void removeTabPreviewListener(@NonNull final TabPreviewListener listener) {
        Condition.INSTANCE.ensureNotNull(listener, "The listener may not be null");
        tabPreviewListeners.remove(listener);
    }

    @Override
    public final void saveInstanceState(@NonNull final Bundle outState) {
        outState.putSerializable(LOG_LEVEL_EXTRA, logLevel);
        outState.putParcelableArrayList(TABS_EXTRA, tabs);
        outState.putBoolean(SWITCHER_SHOWN_EXTRA, switcherShown);
        outState.putInt(SELECTED_TAB_INDEX_EXTRA, selectedTab != null ? indexOf(selectedTab) : -1);
        outState.putIntArray(PADDING_EXTRA, padding);
        outState.putBoolean(APPLY_PADDING_TO_TABS_EXTRA, applyPaddingToTabs);
        outState.putInt(TAB_ICON_ID_EXTRA, tabIconId);
        outState.putParcelable(TAB_ICON_BITMAP_EXTRA, tabIconBitmap);
        outState.putParcelable(TAB_ICON_TINT_LIST_EXTRA, tabIconTintList);
        outState.putSerializable(TAB_ICON_TINT_MODE_EXTRA, tabIconTintMode);
        outState.putParcelable(TAB_BACKGROUND_COLOR_EXTRA, tabBackgroundColor);
        outState.putInt(TAB_CONTENT_BACKGROUND_COLOR_EXTRA, tabContentBackgroundColor);
        outState.putParcelable(TAB_TITLE_TEXT_COLOR_EXTRA, tabTitleTextColor);
        outState.putInt(TAB_CLOSE_BUTTON_ICON_ID_EXTRA, tabCloseButtonIconId);
        outState.putParcelable(TAB_CLOSE_BUTTON_ICON_BITMAP_EXTRA, tabCloseButtonIconBitmap);
        outState.putParcelable(TAB_CLOSE_BUTTON_ICON_TINT_LIST_EXTRA, tabCloseButtonIconTintList);
        outState.putSerializable(TAB_CLOSE_BUTTON_ICON_TINT_MODE_EXTRA, tabCloseButtonIconTintMode);
        outState.putInt(TAB_PROGRESS_BAR_COLOR_EXTRA, tabProgressBarColor);
        outState.putBoolean(SHOW_TOOLBARS_EXTRA, showToolbars);
        outState.putCharSequence(TOOLBAR_TITLE_EXTRA, toolbarTitle);
        outState.putParcelable(TOOLBAR_NAVIGATION_ICON_TINT_LIST_EXTRA,
                toolbarNavigationIconTintList);
        outState.putSerializable(TOOLBAR_NAVIGATION_ICON_TINT_MODE_EXTRA,
                toolbarNavigationIconTintMode);
        outState.putLong(TAB_PREVIEW_FADE_THRESHOLD_EXTRA, tabPreviewFadeThreshold);
        outState.putLong(TAB_PREVIEW_FADE_DURATION, tabPreviewFadeDuration);
        outState.putBoolean(CLEAR_SAVED_STATES_WHEN_REMOVING_TABS_EXTRA,
                clearSavedStatesWhenRemovingTabs);
        getContentRecyclerAdapter().saveInstanceState(outState);
    }

    @Override
    public final void restoreInstanceState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            referenceTabIndex = savedInstanceState.getInt(REFERENCE_TAB_INDEX_EXTRA, -1);
            referenceTabPosition = savedInstanceState.getFloat(REFERENCE_TAB_POSITION_EXTRA, -1);
            logLevel = (LogLevel) savedInstanceState.getSerializable(LOG_LEVEL_EXTRA);
            tabs = savedInstanceState.getParcelableArrayList(TABS_EXTRA);
            switcherShown = savedInstanceState.getBoolean(SWITCHER_SHOWN_EXTRA);
            int selectedTabIndex = savedInstanceState.getInt(SELECTED_TAB_INDEX_EXTRA);
            selectedTab = selectedTabIndex != -1 ? tabs.get(selectedTabIndex) : null;
            padding = savedInstanceState.getIntArray(PADDING_EXTRA);
            applyPaddingToTabs = savedInstanceState.getBoolean(APPLY_PADDING_TO_TABS_EXTRA);
            tabIconId = savedInstanceState.getInt(TAB_ICON_ID_EXTRA);
            tabIconBitmap = savedInstanceState.getParcelable(TAB_ICON_BITMAP_EXTRA);
            tabIconTintList = savedInstanceState.getParcelable(TAB_ICON_TINT_LIST_EXTRA);
            tabIconTintMode =
                    (PorterDuff.Mode) savedInstanceState.getSerializable(TAB_ICON_TINT_MODE_EXTRA);
            tabBackgroundColor = savedInstanceState.getParcelable(TAB_BACKGROUND_COLOR_EXTRA);
            tabContentBackgroundColor =
                    savedInstanceState.getInt(TAB_CONTENT_BACKGROUND_COLOR_EXTRA);
            tabTitleTextColor = savedInstanceState.getParcelable(TAB_TITLE_TEXT_COLOR_EXTRA);
            tabCloseButtonIconId = savedInstanceState.getInt(TAB_CLOSE_BUTTON_ICON_ID_EXTRA);
            tabCloseButtonIconBitmap =
                    savedInstanceState.getParcelable(TAB_CLOSE_BUTTON_ICON_BITMAP_EXTRA);
            tabCloseButtonIconTintList =
                    savedInstanceState.getParcelable(TAB_CLOSE_BUTTON_ICON_TINT_LIST_EXTRA);
            tabCloseButtonIconTintMode = (PorterDuff.Mode) savedInstanceState
                    .getSerializable(TAB_CLOSE_BUTTON_ICON_TINT_MODE_EXTRA);
            tabProgressBarColor = savedInstanceState.getInt(TAB_PROGRESS_BAR_COLOR_EXTRA, -1);
            showToolbars = savedInstanceState.getBoolean(SHOW_TOOLBARS_EXTRA);
            toolbarTitle = savedInstanceState.getCharSequence(TOOLBAR_TITLE_EXTRA);
            toolbarNavigationIconTintList =
                    savedInstanceState.getParcelable(TOOLBAR_NAVIGATION_ICON_TINT_LIST_EXTRA);
            toolbarNavigationIconTintMode = (PorterDuff.Mode) savedInstanceState
                    .getSerializable(TOOLBAR_NAVIGATION_ICON_TINT_MODE_EXTRA);
            tabPreviewFadeThreshold = savedInstanceState.getLong(TAB_PREVIEW_FADE_THRESHOLD_EXTRA);
            tabPreviewFadeDuration = savedInstanceState.getLong(TAB_PREVIEW_FADE_DURATION);
            clearSavedStatesWhenRemovingTabs =
                    savedInstanceState.getBoolean(CLEAR_SAVED_STATES_WHEN_REMOVING_TABS_EXTRA);
            getContentRecyclerAdapter().restoreInstanceState(savedInstanceState);
        }
    }

}