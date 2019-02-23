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

import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.SoftReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.mrapp.android.tabswitcher.model.Restorable;
import de.mrapp.util.Condition;

/**
 * A {@link TabSwitcherDecorator}, which allows to store any arbitrary state for each tab of the
 * associated {@link TabSwitcher}. The state is kept even if the tab is currently not shown.
 * However, it is possible that states are vanished, if few memory is available, because they are
 * stored using {@link SoftReference}s. Unlike the parameters, which can be set for an individual
 * {@link Tab} using the method {@link Tab#setParameters(Bundle)}, these states are not restored
 * when restoring the state of a {@link TabSwitcher}, e.g. after orientation changes. Instead the
 * state is vanished and will be created from scratch when the tab is shown for the next time.
 * <p>
 * If it is necessary to store some data from a state, it can be put into the bundle, which is
 * passed to the {@link #onSaveInstanceState(View, Tab, int, int, Object, Bundle)} method. The data
 * can later be retrieved from the bundle when the method
 * {@link #onCreateState(Context, TabSwitcher, View, Tab, int, int, Bundle)} is invoked for the next
 * time.
 * <p>
 * It is recommended to use the class {@link AbstractState} as a base class for implementing states.
 * This is not necessary though. Said class stores a reference to the tab, it corresponds to and
 * implements the interface {@link Restorable} to be able to store and restore its state. By
 * implementing the methods of this interface and calling them in the decorator's {@link
 * #onSaveInstanceState(View, Tab, int, int, Object, Bundle)}, respectively {@link
 * #onCreateState(Context, TabSwitcher, View, Tab, int, int, Bundle)} method, the selected
 * properties of the state can be stored and restored.
 * <p>
 * If {@link TabSwitcher#areSavedStatesClearedWhenRemovingTabs()} returns true, the state of tabs is
 * cleared when a tab has been removed from the {@link TabSwitcher}. This can be prevented by using
 * the method {@link TabSwitcher#clearSavedStatesWhenRemovingTabs(boolean)}. To manually remove the
 * state of a specific tab, the method {@link #clearState(Tab)} can be used. The method {@link
 * #clearAllStates()} allows to remove the states of all tabs accordingly.
 * <p>
 * IMPORTANT: States must not store references to views, which have been inflated in the decorator's
 * {@link #onInflateView(LayoutInflater, ViewGroup, int)} method, because these views can be reused
 * across all tabs of the {@link TabSwitcher} and therefore are not guaranteed to belong to the same
 * tab for the whole lifetime of the decorator.
 *
 * @param <StateType>
 *         The type of the states, which are stored by the decorator
 * @author Michael Rapp
 * @since 0.2.4
 */
public abstract class StatefulTabSwitcherDecorator<StateType> extends TabSwitcherDecorator {

    /**
     * A sparse array, which is used to store the states of tabs.
     */
    private SparseArray<SoftReference<StateType>> states;

    /**
     * The method, which is invoked on subclasses in order to create the state for a specific tab.
     * This method is invoked, when a tab is shown or refreshed and no corresponding state exists
     * yet.
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
     *         The tab, for which a state should be created, as an instance of the class {@link
     *         Tab}. The tab may not be null
     * @param index
     *         The index of the tab, for which a state should be created, as an {@link Integer}
     *         value
     * @param viewType
     *         The view type of the tab, for which a state should be created, as an {@link Integer}
     *         value
     * @param savedInstanceState
     *         The bundle, which has previously been used to save the state of the tab as an
     *         instance of the class {@link Bundle} or null, if no saved state is available
     * @return The state, which has been created, as an instance of the generic type {@link
     * StateType} or null, if no state has been created
     */
    @Nullable
    protected abstract StateType onCreateState(@NonNull final Context context,
                                               @NonNull final TabSwitcher tabSwitcher,
                                               @NonNull final View view, @NonNull final Tab tab,
                                               final int index, final int viewType,
                                               @Nullable final Bundle savedInstanceState);

    /**
     * The method, which is invoked on subclasses, when a state is cleared using the {@link
     * #clearState(Tab)} or {@link #clearAllStates()} method.
     *
     * @param state
     *         The state, which is cleared, as an instance of the generic type {@link StateType}.
     *         The state may not be null
     */
    protected void onClearState(@NonNull final StateType state) {

    }

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
     * @param state
     *         The state of the tab, which should be visualized, as an instance of the generic type
     *         {@link StateType} or null, if no state has been created yet
     * @param savedInstanceState
     *         The bundle, which has previously been used to save the state of the view as an
     *         instance of the class {@link Bundle} or null, if no saved state is available
     * @see #onShowTab(Context, TabSwitcher, View, Tab, int, int, Bundle)
     */
    protected abstract void onShowTab(@NonNull final Context context,
                                      @NonNull final TabSwitcher tabSwitcher,
                                      @NonNull final View view, @NonNull final Tab tab,
                                      final int index, final int viewType,
                                      @Nullable final StateType state,
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
     * @param state
     *         The state of tab, whose state should be saved, as an instance of the generic type
     *         {@link StateType} or null, if no state has been created yet
     * @param outState
     *         The bundle, the state of the tab should be saved to, as an instance of the class
     *         {@link Bundle}. The bundle may not be null
     * @see #onSaveInstanceState(View, Tab, int, int, Bundle)
     */
    protected void onSaveInstanceState(@NonNull final View view, @NonNull final Tab tab,
                                       final int index, final int viewType,
                                       @Nullable final StateType state,
                                       @NonNull final Bundle outState) {

    }

    /**
     * Returns the state of a specific tab.
     *
     * @param tab
     *         The tab, whose state should be returned, as an instance of the class {@link Tab}. The
     *         tab may not be null
     * @return The state of the given tab as an instance of the generic type {@link StateType} or
     * null, if no state has been created yet or if it was removed
     */
    @Nullable
    public final StateType getState(@NonNull final Tab tab) {
        Condition.INSTANCE.ensureNotNull(tab, "The tab may not be null");

        if (states != null) {
            SoftReference<StateType> reference = states.get(tab.hashCode());

            if (reference != null) {
                return reference.get();
            }
        }

        return null;
    }

    /**
     * Removes the state of a specific tab.
     *
     * @param tab
     *         The tab, whose state should be removed, as an instance of the class {@link Tab}. The
     *         tab may not be null
     */
    public final void clearState(@NonNull final Tab tab) {
        Condition.INSTANCE.ensureNotNull(tab, "The tab may not be null");

        if (states != null) {
            SoftReference<StateType> reference = states.get(tab.hashCode());

            if (reference != null) {
                StateType state = reference.get();

                if (state != null) {
                    onClearState(state);
                }

                states.remove(tab.hashCode());

                if (states.size() == 0) {
                    states = null;
                }
            }
        }
    }

    /**
     * Removes the states of all tabs.
     */
    public final void clearAllStates() {
        if (states != null) {
            for (int i = 0; i < states.size(); i++) {
                SoftReference<StateType> reference = states.valueAt(i);
                StateType state = reference.get();

                if (state != null) {
                    onClearState(state);
                }
            }

            states.clear();
            states = null;
        }
    }

    @Override
    public final void onShowTab(@NonNull final Context context,
                                @NonNull final TabSwitcher tabSwitcher, @NonNull final View view,
                                @NonNull final Tab tab, final int index, final int viewType,
                                @Nullable final Bundle savedInstanceState) {
        if (states == null) {
            states = new SparseArray<>();
        }

        StateType state = getState(tab);

        if (state == null) {
            state = onCreateState(context, tabSwitcher, view, tab, index, viewType,
                    savedInstanceState);

            if (state != null) {
                states.put(tab.hashCode(), new SoftReference<>(state));
            }
        }

        onShowTab(context, tabSwitcher, view, tab, index, viewType, state, savedInstanceState);
    }

    @Override
    public final void onSaveInstanceState(@NonNull final View view, @NonNull final Tab tab,
                                          final int index, final int viewType,
                                          @NonNull final Bundle outState) {
        StateType state = getState(tab);
        onSaveInstanceState(view, tab, index, viewType, state, outState);
    }

}