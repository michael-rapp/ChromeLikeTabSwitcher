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

import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * Contains all types of animations, which can be used to add or remove tabs to a {@link
 * TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public enum AnimationType {

    /**
     * When the tab should be swiped in/out from/to the left.
     */
    SWIPE_LEFT,

    /**
     * When the tab should be swiped in/out from/to the right.
     */
    SWIPE_RIGHT

}