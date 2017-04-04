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

import java.util.Collection;
import java.util.NoSuchElementException;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * Defines the interface, a class, which implements the model of a {@link TabSwitcher} must
 * implement.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public interface Model extends Iterable<Tab> {

    /**
     * Defines the interface, a class, which should be notified about the model's events, must
     * implement.
     */
    interface Listener {

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
         * @param switcherHidden
         *         True, if adding the tab caused the tab switcher to be hidden, false otherwise
         * @param animation
         *         The animation, which has been used to add the tab, as an instance of the class
         *         {@link Animation}. The animation may not be null
         */
        void onTabAdded(int index, @NonNull Tab tab, int previousSelectedTabIndex,
                        int selectedTabIndex, boolean switcherHidden, @NonNull Animation animation);

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

    }

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
     * Animation.SwipeAnimation} with direction {@link Animation.SwipeDirection#RIGHT} is used. If
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
     * the tab is added by using an animation. By default, a {@link Animation.SwipeAnimation} with
     * direction {@link Animation.SwipeDirection#RIGHT} is used. If an animation is currently
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
     * animation. By default, a {@link Animation.SwipeAnimation} with direction {@link
     * Animation.SwipeDirection#RIGHT} is used. If an animation is currently running, the tabs will
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
     * By default, a {@link Animation.SwipeAnimation} with direction {@link
     * Animation.SwipeDirection#RIGHT} is used. If an animation is currently running, the tabs will
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
     * animation. By default, a {@link Animation.SwipeAnimation} with direction {@link
     * Animation.SwipeDirection#RIGHT} is used. If an animation is currently running, the tabs will
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
     * default, a {@link Animation.SwipeAnimation} with direction {@link
     * Animation.SwipeDirection#RIGHT} is used. If an animation is currently running, the tabs will
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
     * removed by using an animation. By default, a {@link Animation.SwipeAnimation} with direction
     * {@link Animation.SwipeDirection#RIGHT} is used. If an animation is currently running, the tab
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
     * removed by using an animation. By default, a {@link Animation.SwipeAnimation} with direction
     * {@link Animation.SwipeDirection#RIGHT} is used. If an animation is currently running, the
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

}