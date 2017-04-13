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
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.mrapp.android.util.view.AbstractViewHolderAdapter;

/**
 * An abstract base class for all decorators, which  are responsible for inflating views, which
 * should be used to visualize the tabs of a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public abstract class TabSwitcherDecorator extends AbstractViewHolderAdapter {

    /**
     * The name of the extra, which is used to store the state of a view hierarchy within a bundle.
     */
    private static final String VIEW_HIERARCHY_STATE_EXTRA =
            TabSwitcherDecorator.class.getName() + "::ViewHierarchyState";

    /**
     * The method which is invoked, when a view, which is used to visualize a tab, should be
     * inflated.
     *
     * @param inflater
     *         The inflater, which should be used to inflate the view, as an instance of the class
     *         {@link LayoutInflater}. The inflater may not be null
     * @param parent
     *         The parent view of the view, which should be inflated, as an instance of the class
     *         {@link ViewGroup} or null, if no parent view is available
     * @param viewType
     *         The view type of the tab, which should be visualized, as an {@link Integer} value
     * @return The view, which has been inflated, as an instance of the class {@link View}. The view
     * may not be null
     */
    @NonNull
    public abstract View onInflateView(@NonNull final LayoutInflater inflater,
                                       @Nullable final ViewGroup parent, final int viewType);

    /**
     * The method which is invoked, when the view, which is used to visualize a tab, should be
     * shown, respectively when it should be refreshed. The purpose of this method is to customize
     * the appearance of the view, which is used to visualize the corresponding tab, depending on
     * its state and whether the tab switcher is currently shown, or not.
     *
     * @param context
     *         The context, the tab switcher belongs to, as an instance of the class {@link
     *         Context}. The context may not be null
     * @param tabSwitcher
     *         The tab switcher, whose tabs are visualized by the decorator, as an instance of the
     *         type {@link TabSwitcher}. The tab switcher may not be null
     * @param view
     *         The view, which is used to visualize the tab, as an instance of the class {@link
     *         View}. The view may not be null
     * @param tab
     *         The tab, which should be visualized, as an instance of the class {@link Tab}. The tab
     *         may not be null
     * @param index
     *         The index of the tab, which should be visualized, as an {@link Integer} value
     * @param viewType
     *         The view type of the tab, which should be visualized, as an {@link Integer} value
     * @param savedInstanceState
     *         The bundle, which has previously been used to save the state of the view as an
     *         instance of the class {@link Bundle} or null, if no saved state is available
     */
    public abstract void onShowTab(@NonNull final Context context,
                                   @NonNull final TabSwitcher tabSwitcher, @NonNull final View view,
                                   @NonNull final Tab tab, final int index, final int viewType,
                                   @Nullable final Bundle savedInstanceState);

    /**
     * The method, which is invoked, when the view, which is used to visualize a tab, is removed.
     * The purpose of this method is to save the current state of the tab in a bundle.
     *
     * @param view
     *         The view, which is used to visualize the tab, as an instance of the class {@link
     *         View}
     * @param tab
     *         The tab, whose state should be saved, as an instance of the class {@link Tab}. The
     *         tab may not be null
     * @param index
     *         The index of the tab, whose state should be saved, as an {@link Integer} value
     * @param viewType
     *         The view type of the tab, whose state should be saved, as an {@link Integer} value
     * @param outState
     *         The bundle, the state of the tab should be saved to, as an instance of the class
     *         {@link Bundle}. The bundle may not be null
     */
    public void onSaveInstanceState(@NonNull final View view, @NonNull final Tab tab,
                                    final int index, final int viewType,
                                    @NonNull final Bundle outState) {

    }

    /**
     * Returns the view type, which corresponds to a specific tab. For each layout, which is
     * inflated by the <code>onInflateView</code>-method, a distinct view type must be
     * returned.
     *
     * @param tab
     *         The tab, whose view type should be returned, as an instance of the class {@link Tab}.
     *         The tab may not be null
     * @param index
     *         The index of the tab, whose view type should be returned, as an {@link Integer}
     *         value
     * @return The view type, which corresponds to the given tab, as an {@link Integer} value
     */
    public int getViewType(@NonNull final Tab tab, final int index) {
        return 0;
    }

    /**
     * Returns the number of view types, which are used by the decorator.
     *
     * @return The number of view types, which are used by the decorator, as an {@link Integer}
     * value. The number of view types must correspond to the number of distinct values, which are
     * returned by the <code>getViewType</code>-method
     */
    public int getViewTypeCount() {
        return 1;
    }

    /**
     * The method, which is invoked by a {@link TabSwitcher} to inflate the view, which should be
     * used to visualize a specific tab.
     *
     * @param inflater
     *         The inflater, which should be used to inflate the view, as an instance of the class
     *         {@link LayoutInflater}. The inflater may not be null
     * @param parent
     *         The parent view of the view, which should be inflated, as an instance of the class
     *         {@link ViewGroup} or null, if no parent view is available
     * @param tab
     *         The tab, which should be visualized, as an instance of the class {@link Tab}. The tab
     *         may not be null
     * @param index
     *         The index of the tab, which should be visualized, as an {@link Integer} value
     * @return The view, which has been inflated, as an instance of the class {@link View}. The view
     * may not be null
     */
    @NonNull
    public final View inflateView(@NonNull final LayoutInflater inflater,
                                  @Nullable final ViewGroup parent, @NonNull final Tab tab,
                                  final int index) {
        int viewType = getViewType(tab, index);
        return onInflateView(inflater, parent, viewType);
    }

    /**
     * The method, which is invoked by a {@link TabSwitcher} to apply the decorator. It initializes
     * the view holder pattern, which is provided by the decorator and then delegates the method
     * call to the decorator's custom implementation of the method <code>onShowTab(...):void</code>.
     *
     * @param context
     *         The context, the tab switcher belongs to, as an instance of the class {@link
     *         Context}. The context may not be null
     * @param tabSwitcher
     *         The tab switcher, whose tabs are visualized by the decorator, as an instance of the
     *         class {@link TabSwitcher}. The tab switcher may not be null
     * @param view
     *         The view, which is used to visualize the tab, as an instance of the class {@link
     *         View}. The view may not be null
     * @param tab
     *         The tab, which should be visualized, as an instance of the class {@link Tab}. The tab
     *         may not be null
     * @param index
     *         The index of the tab, which should be visualized, as an {@link Integer} value
     * @param savedInstanceState
     *         The bundle, which has previously been used to save the state of the view as an
     *         instance of the class {@link Bundle} or null, if no saved state is available
     */
    public final void applyDecorator(@NonNull final Context context,
                                     @NonNull final TabSwitcher tabSwitcher,
                                     @NonNull final View view, @NonNull final Tab tab,
                                     final int index, @Nullable final Bundle savedInstanceState) {
        setCurrentParentView(view);
        int viewType = getViewType(tab, index);

        if (savedInstanceState != null) {
            SparseArray<Parcelable> viewStates =
                    savedInstanceState.getSparseParcelableArray(VIEW_HIERARCHY_STATE_EXTRA);

            if (viewStates != null) {
                view.restoreHierarchyState(viewStates);
            }
        }

        onShowTab(context, tabSwitcher, view, tab, index, viewType, savedInstanceState);
    }

    /**
     * The method, which is invoked by a {@link TabSwitcher} to save the current state of a tab. It
     * initializes the view holder pattern, which is provided by the decorator and then delegates
     * the method call to the decorator's custom implementation of the method
     * <code>onSaveInstanceState(...):void</code>.
     *
     * @param view
     *         The view, which is used to visualize the tab, as an instance of the class {@link
     *         View}
     * @param tab
     *         The tab, whose state should be saved, as an instance of the class {@link Tab}. The
     *         tab may not be null
     * @param index
     *         The index of the tab, whose state should be saved, as an {@link Integer} value
     * @return The bundle, which has been used to save the state, as an instance of the class {@link
     * Bundle}. The bundle may not be null
     */
    @NonNull
    public final Bundle saveInstanceState(@NonNull final View view, @NonNull final Tab tab,
                                          final int index) {
        setCurrentParentView(view);
        int viewType = getViewType(tab, index);
        Bundle outState = new Bundle();
        SparseArray<Parcelable> viewStates = new SparseArray<>();
        view.saveHierarchyState(viewStates);
        outState.putSparseParcelableArray(VIEW_HIERARCHY_STATE_EXTRA, viewStates);
        onSaveInstanceState(view, tab, index, viewType, outState);
        return outState;
    }

}