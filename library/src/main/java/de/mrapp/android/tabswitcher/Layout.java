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

/**
 * Contains all possible layouts of a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public enum Layout {

    /**
     * The layout, which is used on smartphones and phablet devices, when in portrait mode.
     */
    PHONE_PORTRAIT,

    /**
     * The layout, which is used on smartphones and phablet devices, when in landscape mode.
     */
    PHONE_LANDSCAPE,

    /**
     * The layout, which is used on tablets.
     */
    TABLET

}