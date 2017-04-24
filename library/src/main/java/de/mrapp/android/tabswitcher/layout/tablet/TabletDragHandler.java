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
package de.mrapp.android.tabswitcher.layout.tablet;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractDragHandler;
import de.mrapp.android.tabswitcher.layout.Arithmetics;
import de.mrapp.android.tabswitcher.model.TabItem;

/**
 * A drag handler, which allows to calculate the position and state of tabs on touch events, when
 * using the tablet layout.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletDragHandler extends AbstractDragHandler<AbstractDragHandler.Callback> {

    /**
     * Creates a new drag handler, which allows to calculate the position and state of tabs on touch
     * events, when using the tablet layout.
     *
     * @param tabSwitcher
     *         The tab switcher, whose tabs' positions and states should be calculated by the drag
     *         handler, as an instance of the class {@link TabSwitcher}. The tab switcher may not be
     *         null
     * @param arithmetics
     *         The arithmetics, which should be used to calculate the position, size and rotation of
     *         tabs, as an instance of the type {@link Arithmetics}. The arithmetics may not be
     *         null
     */
    public TabletDragHandler(@NonNull final TabSwitcher tabSwitcher,
                             @NonNull final Arithmetics arithmetics) {
        super(tabSwitcher, arithmetics, true);
    }

    @Override
    @Nullable
    protected final TabItem getFocusedTab(final float position) {
        // TODO: Implement
        return null;
    }

}