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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.RevealAnimation;
import de.mrapp.android.tabswitcher.SwipeAnimation;
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
     * A set, which contains the listeners, which are notified about the model's events.
     */
    private final Set<Listener> listeners;

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
     * @param switcherHidden
     *         True, if adding the tab caused the tab switcher to be hidden, false otherwise
     * @param animation
     *         The animation, which has been used to add the tab, as an instance of the class {@link
     *         Animation}. The animation may not be null
     */
    private void notifyOnTabAdded(final int index, @NonNull final Tab tab,
                                  final int previousSelectedTabIndex, final int selectedTabIndex,
                                  final boolean switcherHidden,
                                  @NonNull final Animation animation) {
        for (Listener listener : listeners) {
            listener.onTabAdded(index, tab, previousSelectedTabIndex, selectedTabIndex,
                    switcherHidden, animation);
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
     * @param animation
     *         The animation, which has been used to add the tabs, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    private void notifyOnAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                      final int previousSelectedTabIndex,
                                      final int selectedTabIndex,
                                      @NonNull final Animation animation) {
        for (Listener listener : listeners) {
            listener.onAllTabsAdded(index, tabs, previousSelectedTabIndex, selectedTabIndex,
                    animation);
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
     * @param animation
     *         The animation, which has been used to remove the tab, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    private void notifyOnTabRemoved(final int index, @NonNull final Tab tab,
                                    final int previousSelectedTabIndex, final int selectedTabIndex,
                                    @NonNull final Animation animation) {
        for (Listener listener : listeners) {
            listener.onTabRemoved(index, tab, previousSelectedTabIndex, selectedTabIndex,
                    animation);
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
     * Creates a new model of a {@link TabSwitcher}.
     */
    public TabSwitcherModel() {
        this.listeners = new LinkedHashSet<>();
        this.tabs = new ArrayList<>();
        this.switcherShown = false;
        this.selectedTab = null;
    }

    /**
     * Adds a new listener, which should be notified about the model's events.
     *
     * @param listener
     *         The listener, which should be added, as an instance of the type {@link Listener}. The
     *         listener may not be null
     */
    public final void addListener(@NonNull final Listener listener) {
        ensureNotNull(listener, "The listener may not be null");
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
        ensureNotNull(listener, "The listener may not be null");
        listeners.remove(listener);
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
        addTab(tab, index, new SwipeAnimation.Builder().create());
    }

    @Override
    public final void addTab(@NonNull final Tab tab, final int index,
                             @NonNull final Animation animation) {
        ensureNotNull(tab, "The tab may not be null");
        ensureNotNull(animation, "The animation may not be null");
        tabs.add(index, tab);
        int previousSelectedTabIndex = getSelectedTabIndex();
        int selectedTabIndex = previousSelectedTabIndex;
        boolean switcherHidden = false;

        if (previousSelectedTabIndex == -1) {
            selectedTab = tab;
            selectedTabIndex = index;
        }

        if (animation instanceof RevealAnimation) {
            selectedTab = tab;
            selectedTabIndex = index;
            switcherHidden = setSwitcherShown(false);
        }

        notifyOnTabAdded(index, tab, previousSelectedTabIndex, selectedTabIndex, switcherHidden,
                animation);
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
        addAllTabs(tabs, index, new SwipeAnimation.Builder().create());
    }

    @Override
    public final void addAllTabs(@NonNull final Tab[] tabs, final int index,
                                 @NonNull final Animation animation) {
        ensureNotNull(tabs, "The array may not be null");
        ensureNotNull(animation, "The animation may not be null");

        if (tabs.length > 0) {
            int previousSelectedTabIndex = getSelectedTabIndex();
            int selectedTabIndex = previousSelectedTabIndex;

            for (int i = 0; i < tabs.length; i++) {
                Tab tab = tabs[i];
                this.tabs.add(index + i, tab);
            }

            if (previousSelectedTabIndex == -1) {
                selectedTabIndex = 0;
                selectedTab = tabs[selectedTabIndex];
            }

            notifyOnAllTabsAdded(index, tabs, previousSelectedTabIndex, selectedTabIndex,
                    animation);
        }
    }

    @Override
    public final void removeTab(@NonNull final Tab tab) {
        removeTab(tab, new SwipeAnimation.Builder().create());
    }

    @Override
    public final void removeTab(@NonNull final Tab tab, @NonNull final Animation animation) {
        ensureNotNull(tab, "The tab may not be null");
        ensureNotNull(animation, "The animation may not be null");
        int index = indexOfOrThrowException(tab);
        int previousSelectedTabIndex = getSelectedTabIndex();
        int selectedTabIndex = previousSelectedTabIndex;
        tabs.remove(index);

        if (isEmpty()) {
            selectedTabIndex = -1;
            selectedTab = null;
        } else if (index == previousSelectedTabIndex) {
            if (index > 0) {
                selectedTabIndex = index - 1;
            }

            selectedTab = getTab(selectedTabIndex);
        }

        notifyOnTabRemoved(index, tab, previousSelectedTabIndex, selectedTabIndex, animation);

    }

    @Override
    public final void clear() {
        clear(new SwipeAnimation.Builder().create());
    }

    @Override
    public final void clear(@NonNull final Animation animation) {
        ensureNotNull(animation, "The animation may not be null");
        Tab[] result = new Tab[tabs.size()];
        tabs.toArray(result);
        tabs.clear();
        notifyOnAllTabsRemoved(result, animation);
        selectedTab = null;
    }

    @Override
    public final boolean isSwitcherShown() {
        return switcherShown;
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
        ensureNotNull(tab, "The tab may not be null");
        int previousIndex = getSelectedTabIndex();
        int index = indexOfOrThrowException(tab);
        selectedTab = tab;
        boolean switcherHidden = setSwitcherShown(false);
        notifyOnSelectionChanged(previousIndex, index, tab, switcherHidden);
    }

    @Override
    public final Iterator<Tab> iterator() {
        return tabs.iterator();
    }

}