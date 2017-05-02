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

import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractRecyclerAdapter;
import de.mrapp.android.tabswitcher.layout.AbstractTabViewHolder;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.util.ViewUtil;

/**
 * A view recycler adapter, which allows to inflate the views, which are used to visualize the tabs
 * of a {@link TabSwitcher}, when using the tablet layout.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletRecyclerAdapter extends AbstractRecyclerAdapter<Void>
        implements Tab.Callback, Model.Listener {

    /**
     * Creates a new view recycler adapter, which allows to inflate the views, which are used to
     * visualize the tabs of a {@link TabSwitcher}.
     *
     * @param tabSwitcher
     *         The tab switcher as an instance of the class {@link TabSwitcher}. The tab switcher
     *         may not be null
     * @param model
     *         The model, which belongs to the tab switcher, as an instance of the class {@link
     *         TabSwitcherModel}. The model may not be null
     */
    public TabletRecyclerAdapter(@NonNull final TabSwitcher tabSwitcher,
                                 @NonNull final TabSwitcherModel model) {
        super(tabSwitcher, model, R.color.tablet_tab_background_light,
                R.color.tablet_tab_background_color_light_selected,
                R.drawable.tablet_tab_close_button_icon_light);
    }

    @NonNull
    @Override
    protected final View onInflateView(@NonNull final LayoutInflater inflater,
                                       @Nullable final ViewGroup parent,
                                       @NonNull final AbstractTabViewHolder viewHolder) {
        View view = inflater.inflate(R.layout.tablet_tab, parent, false);
        StateListDrawable backgroundDrawable = new StateListDrawable();
        Drawable defaultDrawable = ContextCompat
                .getDrawable(getModel().getContext(), R.drawable.tablet_tab_background);
        Drawable selectedDrawable = ContextCompat
                .getDrawable(getModel().getContext(), R.drawable.tablet_tab_background_selected);
        backgroundDrawable.addState(new int[]{android.R.attr.state_selected}, selectedDrawable);
        backgroundDrawable.addState(StateSet.WILD_CARD, defaultDrawable);
        ViewUtil.setBackground(view, backgroundDrawable);
        return view;
    }

    @Override
    protected final void onShowView(@NonNull final View view, @NonNull final TabItem tabItem,
                                    @NonNull final Void... params) {

    }

    @NonNull
    @Override
    protected final AbstractTabViewHolder onCreateViewHolder() {
        return new TabletTabViewHolder();
    }

}