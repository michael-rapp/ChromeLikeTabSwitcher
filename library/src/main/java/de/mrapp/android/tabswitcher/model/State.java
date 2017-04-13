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

/**
 * Contains all possible states of a tab, while the switcher is shown.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public enum State {

    /**
     * When the tab is part of the stack, which is located at the start of the switcher.
     */
    STACKED_START,

    /**
     * When the tab is displayed atop of the stack, which is located at the start of the switcher.
     */
    STACKED_START_ATOP,

    /**
     * When the tab is floating and freely movable.
     */
    FLOATING,

    /**
     * When the tab is part of the stack, which is located at the end of the switcher.
     */
    STACKED_END,

    /**
     * When the tab is currently not visible, i.e. if no view is inflated to visualize it.
     */
    HIDDEN

}