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
package de.mrapp.android.tabswitcher;

import androidx.annotation.NonNull;

/**
 * Defines the interface of a listener, which should be notified, when a button, which allows to add
 * a tab to a {@link TabSwitcher}, has been clicked. The listener is responsible for instantiating
 * and adding the tab.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public interface AddTabButtonListener {

    /**
     * The method, which is invoked, when a button, which allows to add a tab to a tab switcher, has
     * been clicked.
     *
     * @param tabSwitcher
     *         The tab switcher, the tab should be added to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     */
    void onAddTab(@NonNull TabSwitcher tabSwitcher);

}