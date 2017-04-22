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

import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.ViewGroup;

import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * Defines the interface, a layout, which implements the functionality of a {@link TabSwitcher},
 * must implement.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public interface TabSwitcherLayout {

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
     * {@link ViewGroup} or null, if the view has not been laid out yet
     */
    @Nullable
    ViewGroup getTabContainer();

    /**
     * Returns the toolbars, which are shown, when the tab switcher is shown. When using the
     * smartphone layout, only one toolbar is shown. When using the tablet layout, a primary and
     * secondary toolbar is shown. In such case, the first index of the returned array corresponds
     * to the primary toolbar.
     *
     * @return An array, which contains the toolbars, which are shown, when the tab switcher is
     * shown, as an array of the type Toolbar or null, if the view has not been laid out yet
     */
    @Nullable
    Toolbar[] getToolbars();

    /**
     * Returns the menu of the toolbar, which is shown, when the tab switcher is shown. When using
     * the tablet layout, the menu corresponds to the secondary toolbar.
     *
     * @return The menu of the toolbar as an instance of the type {@link Menu} or null, if the view
     * has not been laid out yet
     */
    @Nullable
    Menu getToolbarMenu();

}