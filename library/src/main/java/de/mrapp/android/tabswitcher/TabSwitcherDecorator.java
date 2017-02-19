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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.mrapp.android.util.view.AbstractViewHolderAdapter;

public abstract class TabSwitcherDecorator extends AbstractViewHolderAdapter {

    public int getViewType(@NonNull final Tab tab) {
        return 0;
    }

    public int getViewTypeCount() {
        return 1;
    }

    @NonNull
    public final View inflateView(@NonNull final LayoutInflater inflater,
                                  @Nullable final ViewGroup parent, @NonNull final Tab tab) {
        int viewType = getViewType(tab);
        return onInflateView(inflater, parent, viewType);
    }

    public final void applyDecorator(@NonNull final Context context,
                                     @NonNull final TabSwitcher tabSwitcher,
                                     @NonNull final View view, @NonNull final Tab tab) {
        setCurrentParentView(view);
        int viewType = getViewType(tab);
        onShowTab(context, tabSwitcher, view, tab, viewType);
    }

    @NonNull
    public abstract View onInflateView(@NonNull final LayoutInflater inflater,
                                       @Nullable final ViewGroup parent, final int viewType);

    public abstract void onShowTab(@NonNull final Context context,
                                   @NonNull final TabSwitcher tabSwitcher, @NonNull final View view,
                                   @NonNull final Tab tab, final int viewType);

}