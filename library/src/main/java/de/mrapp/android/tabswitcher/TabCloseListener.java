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

/**
 * Defines the interface, a class, which should be notified, when a tab is about to be closed by
 * clicking its close button, must implement.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public interface TabCloseListener {

    /**
     * The method, which is invoked, when a tab is about to be closed by clicking its close button.
     *
     * @param tabSwitcher
     *         The tab switcher, the tab belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param tab
     *         The tab, which is about to be closed, as an instance of the class {@link Tab}. The
     *         tab may not be null
     * @return True, if the tab should be closed, false otherwise
     */
    boolean onCloseTab(@NonNull TabSwitcher tabSwitcher, @NonNull Tab tab);

}