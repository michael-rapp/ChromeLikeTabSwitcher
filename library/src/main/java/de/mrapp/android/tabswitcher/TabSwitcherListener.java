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
package de.mrapp.android.tabswitcher;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Defines the interface, a class, which should be notified about a tab switcher's events, must
 * implement.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public interface TabSwitcherListener {

    /**
     * The method, which is invoked, when the tab switcher has been shown.
     *
     * @param tabSwitcher
     *         The observed tab switcher as an instance of the class {@link TabSwitcher}. The tab
     *         switcher may not be null
     */
    void onSwitcherShown(@NonNull final TabSwitcher tabSwitcher);

    /**
     * The method, which is invoked, when the tab switcher has been hidden.
     *
     * @param tabSwitcher
     *         The observed tab switcher as an instance of the class {@link TabSwitcher}. The tab
     *         switcher may not be null
     */
    void onSwitcherHidden(@NonNull final TabSwitcher tabSwitcher);

    /**
     * The method, which is invoked, when the currently selected tab has been changed.
     *
     * @param tabSwitcher
     *         The observed tab switcher as an instance of the class {@link TabSwitcher}. The tab
     *         switcher may not be null
     * @param selectedTabIndex
     *         The index of the currently selected tab as an {@link Integer} value or -1, if the tab
     *         switcher does not contain any tabs
     * @param selectedTab
     *         The currently selected tab as an instance of the class {@link Tab} or null, if the
     *         tab switcher does not contain any tabs
     */
    void onSelectionChanged(@NonNull final TabSwitcher tabSwitcher, int selectedTabIndex,
                            @Nullable Tab selectedTab);

    /**
     * The method, which is invoked, when a tab has been added to the tab switcher.
     *
     * @param tabSwitcher
     *         The observed tab switcher as an instance of the class {@link TabSwitcher}. The tab
     *         switcher may not be null
     * @param index
     *         The index of the tab, which has been added, as an {@link Integer} value
     * @param tab
     *         The tab, which has been added, as an instance of the class {@link Tab}. The tab may
     *         not be null
     */
    void onTabAdded(@NonNull final TabSwitcher tabSwitcher, int index, @NonNull Tab tab);

    /**
     * The method, which is invoked, when a tab has been removed from the tab switcher.
     *
     * @param tabSwitcher
     *         The observed tab switcher as an instance of the class {@link TabSwitcher}. The tab
     *         switcher may not be null
     * @param index
     *         The index of the tab, which has been removed, as an {@link Integer} value
     * @param tab
     *         The tab, which has been removed, as an instance of the class {@link Tab}. The tab may
     *         not be null
     */
    void onTabRemoved(@NonNull final TabSwitcher tabSwitcher, int index, @NonNull Tab tab);

    /**
     * The method, which is invoked, when all tabs have been removed from the tab switcher.
     *
     * @param tabSwitcher
     *         The observed tab switcher as an instance of the class {@link TabSwitcher}. The tab
     *         switcher may not be null
     */
    void onAllTabsRemoved(@NonNull final TabSwitcher tabSwitcher);

}