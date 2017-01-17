/*
 * Copyright 2016 Michael Rapp
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
package de.mrapp.android.tabswitcher.drawable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * A drawable, which allows to show the number of tabs, which are currently contained by a {@link
 * TabSwitcher}. It must be registered at a {@link TabSwitcher} instance in order to keep the
 * displayed count up to date.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabSwitcherDrawable extends LayerDrawable {

    public TabSwitcherDrawable(@NonNull final Context context) {
        super(new Drawable[]{
                ContextCompat.getDrawable(context, R.drawable.tab_switcher_menu_item_background)});
    }

}