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

import android.support.annotation.NonNull;
import android.view.View;

import de.mrapp.android.tabswitcher.TabSwitcher;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An abstract base class for all classes, which provide methods, which allow to calculate the
 * position, size and rotation of a {@link TabSwitcher}'s tabs.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class AbstractArithmetics implements Arithmetics {

    /**
     * The tab switcher, the arithmetics are calculated for.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * Returns the tab switcher, the arithmetics are calculated for.
     *
     * @return The tab switcher, the arithmetics are calculated for, as an instance of the class
     * {@link TabSwitcher}. The tab switcher may not be null
     */
    @NonNull
    protected final TabSwitcher getTabSwitcher() {
        return tabSwitcher;
    }

    /**
     * Creates a new class, which provides methods, which allow to calculate the position, size and
     * rotation of a {@link TabSwitcher}'s tabs.
     *
     * @param tabSwitcher
     *         The tab switcher, the arithmetics should be calculated for, as an instance of the
     *         class {@link TabSwitcher}. The tab switcher may not be null
     */
    public AbstractArithmetics(@NonNull final TabSwitcher tabSwitcher) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        this.tabSwitcher = tabSwitcher;
    }

    @Override
    public final float getTabScale(@NonNull final View view) {
        return getTabScale(view, false);
    }

    @Override
    public final float getTabContainerSize(@NonNull final Axis axis) {
        return getTabContainerSize(axis, true);
    }

}