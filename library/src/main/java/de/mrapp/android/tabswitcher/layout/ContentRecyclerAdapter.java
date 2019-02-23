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
package de.mrapp.android.tabswitcher.layout;

import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.StatefulTabSwitcherDecorator;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.TabSwitcherListener;
import de.mrapp.android.tabswitcher.model.Restorable;
import de.mrapp.android.util.view.AbstractViewRecycler;
import de.mrapp.util.Condition;

/**
 * A view recycler adapter, which allows to inflate the views, which are associated with the tabs of
 * a {@link TabSwitcher}, by encapsulating a {@link TabSwitcherDecorator}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class ContentRecyclerAdapter extends AbstractViewRecycler.Adapter<Tab, Void>
        implements Restorable, TabSwitcherListener {

    /**
     * The name of the extra, which is used to store the saved instance states of previously removed
     * associated views within a bundle.
     */
    private static final String SAVED_INSTANCE_STATES_EXTRA =
            ContentRecyclerAdapter.class.getName() + "::SavedInstanceStates";

    /**
     * The tab switcher, which contains the tabs, the associated views, which are inflated by the
     * adapter, correspond to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The decorator, which is used to inflate the associated views.
     */
    private final TabSwitcherDecorator decorator;

    /**
     * A sparse array, which manages the saved instance states of previously removed associated
     * views.
     */
    private SparseArray<Bundle> savedInstanceStates;

    /**
     * Puts the parameter {@link Tab#WAS_SHOWN_PARAMETER} into a specific bundle. If the bundle is
     * null, a new bundle is created.
     *
     * @param parameters
     *         The bundle, the parameter should be put into, as an instance of the class {@link
     *         Bundle} or null
     * @return The bundle, the parameter has been put into, as an instance of the clas {@link
     * Bundle}. The bundle may not be null
     */
    @NonNull
    private Bundle setWasShownParameter(@Nullable final Bundle parameters) {
        Bundle result = parameters;

        if (result == null) {
            result = new Bundle();
        }

        result.putBoolean(Tab.WAS_SHOWN_PARAMETER, true);
        return result;
    }

    /**
     * Creates a new view recycler adapter, which allows to inflate views, which are associated with
     * the tabs of a {@link TabSwitcher}, by encapsulating a {@link TabSwitcherDecorator}.
     *
     * @param tabSwitcher
     *         The tab switcher, which contains the tabs, whose associated views are inflated by the
     *         adapter, correspond to, as an instance of the class {@link TabSwitcher}. The tab
     *         switcher may not be null
     * @param decorator
     *         The decorator, which should be used to inflate the associated views, as an instance
     *         of the class {@link TabSwitcherDecorator}. The decorator may not be null
     */
    public ContentRecyclerAdapter(@NonNull final TabSwitcher tabSwitcher,
                                  @NonNull final TabSwitcherDecorator decorator) {
        Condition.INSTANCE.ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        Condition.INSTANCE.ensureNotNull(decorator, "The decorator may not be null");
        this.tabSwitcher = tabSwitcher;
        tabSwitcher.addListener(this);
        this.decorator = decorator;
        this.savedInstanceStates = new SparseArray<>();
    }

    /**
     * Clears the saved state of a specific tab.
     *
     * @param tab
     *         The tab, whose saved state should be cleared, as an instance of the class {@link
     *         Tab}. The tab may not be null
     */
    public void clearSavedState(@NonNull final Tab tab) {
        Condition.INSTANCE.ensureNotNull(tab, "The tab may not be null");
        savedInstanceStates.remove(tab.hashCode());
    }

    /**
     * Clears the saved states of all tabs.
     */
    public void clearAllSavedStates() {
        savedInstanceStates.clear();
    }

    @NonNull
    @Override
    public final View onInflateView(@NonNull final LayoutInflater inflater,
                                    @Nullable final ViewGroup parent, @NonNull final Tab item,
                                    final int viewType, @NonNull final Void... params) {
        int index = tabSwitcher.indexOf(item);
        View view = decorator.inflateView(inflater, parent, item, index);
        view.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        return view;
    }

    @Override
    public final void onShowView(@NonNull final Context context, @NonNull final View view,
                                 @NonNull final Tab item, final boolean inflated,
                                 @NonNull final Void... params) {
        int index = tabSwitcher.indexOf(item);
        Bundle savedInstanceState = null;
        Bundle parameters = item.getParameters();

        if (parameters != null && parameters.getBoolean(Tab.WAS_SHOWN_PARAMETER, false)) {
            savedInstanceState = savedInstanceStates.get(item.hashCode());
        }

        item.setParameters(setWasShownParameter(parameters));
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

    @Override
    public final void onSwitcherShown(@NonNull final TabSwitcher tabSwitcher) {

    }

    @Override
    public final void onSwitcherHidden(@NonNull final TabSwitcher tabSwitcher) {

    }

    @Override
    public final void onSelectionChanged(@NonNull final TabSwitcher tabSwitcher,
                                         final int selectedTabIndex,
                                         @Nullable final Tab selectedTab) {

    }

    @Override
    public final void onTabAdded(@NonNull final TabSwitcher tabSwitcher, final int index,
                                 @NonNull final Tab tab, @NonNull final Animation animation) {

    }

    @Override
    public final void onTabRemoved(@NonNull final TabSwitcher tabSwitcher, final int index,
                                   @NonNull final Tab tab, @NonNull final Animation animation) {
        if (tabSwitcher.areSavedStatesClearedWhenRemovingTabs()) {
            clearSavedState(tab);
            TabSwitcherDecorator decorator = tabSwitcher.getDecorator();

            if (decorator instanceof StatefulTabSwitcherDecorator) {
                ((StatefulTabSwitcherDecorator) decorator).clearState(tab);
            }
        }
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final TabSwitcher tabSwitcher,
                                       @NonNull final Tab[] tabs,
                                       @NonNull final Animation animation) {
        if (tabSwitcher.areSavedStatesClearedWhenRemovingTabs()) {
            clearAllSavedStates();

            TabSwitcherDecorator decorator = tabSwitcher.getDecorator();

            if (decorator instanceof StatefulTabSwitcherDecorator) {
                ((StatefulTabSwitcherDecorator) decorator).clearAllStates();
            }
        }
    }

}