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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.model.Restorable;
import de.mrapp.android.util.view.AbstractViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A view recycler adapter, which allows to inflate the views, which are used to visualize the child
 * views of the tabs of a {@link TabSwitcher}, by encapsulating a {@link TabSwitcherDecorator}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class ChildRecyclerAdapter extends AbstractViewRecycler.Adapter<Tab, Void>
        implements Restorable {

    /**
     * The name of the extra, which is used to store the saved instance states of previously removed
     * child views within a bundle.
     */
    private static final String SAVED_INSTANCE_STATES_EXTRA =
            ChildRecyclerAdapter.class.getName() + "::SavedInstanceStates";

    /**
     * The tab switcher, which contains the tabs, the child views, which are inflated by the
     * adapter, correspond to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The decorator, which is used to inflate the child views.
     */
    private final TabSwitcherDecorator decorator;

    /**
     * A sparse array, which manages the saved instance states of previously removed child views.
     */
    private SparseArray<Bundle> savedInstanceStates;

    /**
     * Creates a new view recycler adapter, which allows to inflate the views, which are used to
     * visualize the child views of the tabs of a {@link TabSwitcher}, by encapsulating a {@link
     * TabSwitcherDecorator}.
     *
     * @param tabSwitcher
     *         The tab switcher, which contains the tabs, the child views, which are inflated by the
     *         adapter, correspond to, as an instance of the class {@link TabSwitcher}. The tab
     *         switcher may not be null
     * @param decorator
     *         The decorator, which should be used to inflate the child views, as an instance of the
     *         class {@link TabSwitcherDecorator}. The decorator may not be null
     */
    public ChildRecyclerAdapter(@NonNull final TabSwitcher tabSwitcher,
                                @NonNull final TabSwitcherDecorator decorator) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(decorator, "The decorator may not be null");
        this.tabSwitcher = tabSwitcher;
        this.decorator = decorator;
        this.savedInstanceStates = new SparseArray<>();
    }

    @NonNull
    @Override
    public final View onInflateView(@NonNull final LayoutInflater inflater,
                                    @Nullable final ViewGroup parent, @NonNull final Tab item,
                                    final int viewType, @NonNull final Void... params) {
        int index = tabSwitcher.indexOf(item);
        return decorator.inflateView(inflater, parent, item, index);
    }

    @Override
    public final void onShowView(@NonNull final Context context, @NonNull final View view,
                                 @NonNull final Tab item, final boolean inflated,
                                 @NonNull final Void... params) {
        int index = tabSwitcher.indexOf(item);
        Bundle savedInstanceState = savedInstanceStates.get(item.hashCode());
        decorator.applyDecorator(context, tabSwitcher, view, item, index, savedInstanceState);
    }

    @Override
    public final void onRemoveView(@NonNull final View view, @NonNull final Tab item) {
        int index = tabSwitcher.indexOf(item);
        Bundle outState = decorator.saveInstanceState(view, item, index);
        savedInstanceStates.put(item.hashCode(), outState);
    }

    @Override
    public final int getViewTypeCount() {
        return decorator.getViewTypeCount();
    }

    @Override
    public final int getViewType(@NonNull final Tab item) {
        int index = tabSwitcher.indexOf(item);
        return decorator.getViewType(item, index);
    }

    @Override
    public final void saveInstanceState(@NonNull final Bundle outState) {
        outState.putSparseParcelableArray(SAVED_INSTANCE_STATES_EXTRA, savedInstanceStates);
    }

    @Override
    public final void restoreInstanceState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            savedInstanceStates =
                    savedInstanceState.getSparseParcelableArray(SAVED_INSTANCE_STATES_EXTRA);
        }
    }

}