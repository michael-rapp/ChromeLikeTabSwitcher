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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;

import static de.mrapp.android.util.Condition.ensureNotEqual;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * The model of a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabSwitcherModel implements Model {

    /**
     * Defines the interface, a class, which should be notified about the model's events, must
     * implement.
     */
    public interface Callback {

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
         * @param selectedTabIndex
         *         The index of the currently selected tab as an {@link Integer} value or -1, if the
         *         tab switcher does not contain any tabs
         * @param selectedTab
         *         The currently selected tab as an instance of the class {@link Tab} or null, if
         *         the tab switcher does not contain any tabs
         */
        void onSelectionChanged(int selectedTabIndex, @Nullable Tab selectedTab);

        /**
         * The method, which is invoked, when a tab has been added to the model.
         *
         * @param index
         *         The index of the tab, which has been added, as an {@link Integer} value
         * @param tab
         *         The tab, which has been added, as an instance of the class {@link Tab}. The tab
         *         may not be null
         * @param animation
         *         The animation, which has been used to add the tab, as an instance of the class
         *         {@link Animation}. The animation may not be null
         */
        void onTabAdded(int index, @NonNull Tab tab, @NonNull Animation animation);

        /**
         * The method, which is invoked, when multiple tabs have been added to the model.
         *
         * @param index
         *         The index of the first tab, which has been added, as an {@link Integer} value
         * @param tabs
         *         An array, which contains the tabs, which have been added, as an array of the type
         *         {@link Tab} or an empty array, if no tabs have been added
         * @param animation
         *         The animation, which has been used to add the tabs, as an instance of the class
         *         {@link Animation}. The animation may not be null
         */
        void onAllTabsAdded(int index, @NonNull Tab[] tabs, @NonNull Animation animation);

        /**
         * The method, which is invoked, when a tab has been removed from the model.
         *
         * @param index
         *         The index of the tab, which has been removed, as an {@link Integer} value
         * @param tab
         *         The tab, which has been removed, as an instance of the class {@link Tab}. The tab
         *         may not be null
         * @param animation
         *         The animation, which has been used to remove the tab, as an instance of the class
         *         {@link Animation}. The animation may not be null
         */
        void onTabRemoved(int index, @NonNull Tab tab, @NonNull Animation animation);

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

    }

    /**
     * A list, which contains the tabs, which are contained by the tab switcher.
     */
    private final List<Tab> tabs;

    /**
     * True, if the tab switcher is currently shown, false otherwise.
     */
    private boolean switcherShown;

    /**
     * The currently selected tab.
     */
    private Tab selectedTab;

    /**
     * The callback, which is notified about the model's events.
     */
    private Callback callback;

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
        ensureNotEqual(index, -1, "No such tab: " + tab, NoSuchElementException.class);
        return index;
    }

    /**
     * Sets, whether the tab switcher is currently shown, or not.
     *
     * @param shown
     *         True, if the tab switcher is currently shown, false otherwise
     */
    private void setSwitcherShown(final boolean shown) {
        if (switcherShown != shown) {
            switcherShown = shown;

            if (shown) {
                notifyOnSwitcherShown();
            } else {
                notifyOnSwitcherHidden();
            }
        }
    }

    /**
     * Sets the currently selected tab.
     *
     * @param tab
     *         The currently selected tab as an instance of the class {@link Tab} or null, if no tab
     *         is currently selected
     */
    private void setSelectedTab(@Nullable final Tab tab) {
        int index = tab != null ? indexOfOrThrowException(tab) : -1;
        selectedTab = tab;
        notifyOnSelectionChanged(index, tab);
    }

    /**
     * Notifies the callback, that the tab switcher has been shown.
     */
    private void notifyOnSwitcherShown() {
        if (callback != null) {
            callback.onSwitcherShown();
        }
    }

    /**
     * Notifies the callback, that the tab switcher has been shown.
     */
    private void notifyOnSwitcherHidden() {
        if (callback != null) {
            callback.onSwitcherHidden();
        }
    }

    /**
     * Notifies the callback, that the currently selected tab has been changed.
     *
     * @param index
     *         The index of the tab, which has been selected, as an {@link Integer} value or -1, if
     *         no tab has been selected
     * @param tab
     *         The tab, which has been selected, as an instance of the class {@link Tab} or null, if
     *         no tab has been selected
     */
    private void notifyOnSelectionChanged(final int index, @Nullable final Tab tab) {
        if (callback != null) {
            callback.onSelectionChanged(index, tab);
        }
    }

    /**
     * Notifies the callback, that a specific tab has been added to the model.
     *
     * @param index
     *         The index, the tab has been added at, as an {@link Integer} value
     * @param tab
     *         The tab, which has been added, as an instance of the class {@link Tab}. The tab may
     *         not be null
     * @param animation
     *         The animation, which has been used to add the tab, as an instance of the class {@link
     *         Animation}. The animation may not be null
     */
    private void notifyOnTabAdded(final int index, @NonNull final Tab tab,
                                  @NonNull final Animation animation) {
        if (callback != null) {
            callback.onTabAdded(index, tab, animation);
        }
    }

    /**
     * Notifies the callback, that multiple tabs have been added to the model.
     *
     * @param index
     *         The index of the tab, which has been added, as an {@link Integer} value
     * @param tabs
     *         An array, which contains the tabs, which have been added, as an array of the type
     *         {@link Tab}. The array may not be null
     * @param animation
     *         The animation, which has been used to add the tabs, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    private void notifyOnAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                      @NonNull final Animation animation) {
        if (callback != null) {
            callback.onAllTabsAdded(index, tabs, animation);
        }
    }

    /**
     * Notifies the callback, that a tab has been removed from the model.
     *
     * @param index
     *         The index of the tab, which has been removed, as an {@link Integer} value
     * @param tab
     *         The tab, which has been removed, as an instance of the class {@link Tab}. The tab may
     *         not be null
     * @param animation
     *         The animation, which has been used to remove the tab, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    private void notifyOnTabRemoved(final int index, @NonNull final Tab tab,
                                    @NonNull final Animation animation) {
        if (callback != null) {
            callback.onTabRemoved(index, tab, animation);
        }
    }

    /**
     * Notifies the callback, that all tabs have been removed.
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
        if (callback != null) {
            callback.onAllTabsRemoved(tabs, animation);
        }
    }

    /**
     * Creates a new model of a {@link TabSwitcher}.
     */
    public TabSwitcherModel() {
        this.tabs = new ArrayList<>();
        this.switcherShown = false;
        this.selectedTab = null;
        this.callback = null;
    }

    /**
     * Sets the callback, which should be notified about the model's events.
     *
     * @param callback
     *         The callback, which should be set, as an instance of the type {@link Callback} or
     *         null, if no callback should be notified
     */
    public final void setCallback(@Nullable final Callback callback) {
        this.callback = callback;
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
        ensureNotNull(tab, "The tab may not be null");
        return tabs.indexOf(tab);
    }

    @Override
    public final void addTab(@NonNull Tab tab) {
        addTab(tab, getCount());
    }

    @Override
    public final void addTab(@NonNull final Tab tab, final int index) {
        addTab(tab, index, Animation.createSwipeAnimation());
    }

    @Override
    public final void addTab(@NonNull final Tab tab, final int index,
                             @NonNull final Animation animation) {
        ensureNotNull(tab, "The tab may not be null");
        ensureNotNull(animation, "The animation may not be null");
        tabs.add(index, tab);
        notifyOnTabAdded(index, tab, animation);
    }

    @Override
    public final void addAllTabs(@NonNull final Collection<? extends Tab> tabs) {
        addAllTabs(tabs, getCount());
    }

    @Override
    public final void addAllTabs(@NonNull final Collection<? extends Tab> tabs, final int index) {
        addAllTabs(tabs, index, Animation.createSwipeAnimation());
    }

    @Override
    public final void addAllTabs(@NonNull final Collection<? extends Tab> tabs, final int index,
                                 @NonNull final Animation animation) {
        ensureNotNull(tabs, "The collection may not be null");
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
        addAllTabs(tabs, index, Animation.createSwipeAnimation());
    }

    @Override
    public final void addAllTabs(@NonNull final Tab[] tabs, final int index,
                                 @NonNull final Animation animation) {
        ensureNotNull(tabs, "The array may not be null");
        ensureNotNull(animation, "The animation may not be null");

        if (tabs.length > 0) {
            for (int i = 0; i < tabs.length; i++) {
                Tab tab = tabs[i];
                this.tabs.add(index + i, tab);
            }

            notifyOnAllTabsAdded(index, tabs, animation);

            if (getSelectedTab() == null) {
                setSelectedTab(tabs[0]);
            }
        }
    }

    @Override
    public final void removeTab(@NonNull final Tab tab) {
        removeTab(tab, Animation.createSwipeAnimation());
    }

    @Override
    public final void removeTab(@NonNull final Tab tab, @NonNull final Animation animation) {
        ensureNotNull(tab, "The tab may not be null");
        ensureNotNull(animation, "The animation may not be null");
        int index = indexOfOrThrowException(tab);
        tabs.remove(index);
        notifyOnTabRemoved(index, tab, animation);

        if (isEmpty()) {
            setSelectedTab(null);
        } else {
            int selectedTabIndex = getSelectedTabIndex();

            if (index == selectedTabIndex) {
                if (index > 0) {
                    setSelectedTab(getTab(index - 1));
                } else {
                    setSelectedTab(getTab(1));
                }
            }
        }
    }

    @Override
    public final void clear() {
        clear(Animation.createSwipeAnimation());
    }

    @Override
    public final void clear(@NonNull final Animation animation) {
        ensureNotNull(animation, "The animation may not be null");
        Tab[] result = new Tab[tabs.size()];
        tabs.toArray(result);
        tabs.clear();
        notifyOnAllTabsRemoved(result, animation);
        setSelectedTab(null);
    }

    @Override
    public final boolean isSwitcherShown() {
        return switcherShown;
    }

    @Override
    public final void showSwitcher() {
        setSwitcherShown(true);
    }

    @Override
    public final void hideSwitcher() {
        setSwitcherShown(false);
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
        ensureNotNull(tab, "The tab may not be null");
        setSelectedTab(tab);
    }

    @Override
    public final Iterator<Tab> iterator() {
        return tabs.iterator();
    }

}