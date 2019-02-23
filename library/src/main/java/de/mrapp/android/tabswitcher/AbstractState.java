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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.mrapp.android.tabswitcher.model.Restorable;
import de.mrapp.util.Condition;

/**
 * An abstract base class for the states of tabs, which can be used together with a {@link
 * StatefulTabSwitcherDecorator}. The state stores the tab it corresponds to and implements the
 * interface {@link Restorable} to be able to store and restore its state.
 *
 * @author Michael Rapp
 * @since 0.2.4
 */
public abstract class AbstractState implements Restorable {

    /**
     * The tab, the state corresponds to.
     */
    private final Tab tab;

    /**
     * Creates a new state of a specific tab.
     *
     * @param tab
     *         The tab, the state should correspond to, as an instance of the class {@link Tab}. The
     *         tab may not be null
     */
    public AbstractState(@NonNull final Tab tab) {
        Condition.INSTANCE.ensureNotNull(tab, "The tab may not be null");
        this.tab = tab;
    }

    /**
     * Returns the tab, the state corresponds to.
     *
     * @return The tab, the state corresponds to, as an instance of the class {@link Tab}. The tab
     * may not be null
     */
    @NonNull
    public Tab getTab() {
        return tab;
    }

    @Override
    public void saveInstanceState(@NonNull final Bundle outState) {

    }

    @Override
    public void restoreInstanceState(@Nullable final Bundle savedInstanceState) {

    }

}