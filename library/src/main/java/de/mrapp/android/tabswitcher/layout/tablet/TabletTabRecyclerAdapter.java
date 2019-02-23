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
package de.mrapp.android.tabswitcher.layout.tablet;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.iterator.ItemIterator;
import de.mrapp.android.tabswitcher.layout.AbstractTabRecyclerAdapter;
import de.mrapp.android.tabswitcher.layout.AbstractTabViewHolder;
import de.mrapp.android.tabswitcher.model.AbstractItem;
import de.mrapp.android.tabswitcher.model.AddTabItem;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.tabswitcher.model.TabSwitcherStyle;
import de.mrapp.android.util.ViewUtil;

/**
 * A view recycler adapter, which allows to inflate the views, which are used to visualize the tabs
 * of a {@link TabSwitcher}, when using the tablet layout.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletTabRecyclerAdapter extends AbstractTabRecyclerAdapter {

    /**
     * The view type of a button, which allows to add a new tab.
     */
    private static final int ADD_TAB_BUTTON_VIEW_TYPE = 1;

    /**
     * Returns the item, which corresponds to the button, which allows to add a new tab.
     *
     * @return The item, which corresponds to the button, which allows to add a new tab, as an
     * instance of the class {@link AddTabItem} or null, if the button is not shown
     */
    @Nullable
    private AddTabItem getAddTabItem() {
        if (!getModel().isEmpty()) {
            ItemIterator itemIterator =
                    new ItemIterator.Builder(getModel(), getViewRecyclerOrThrowException())
                            .create();
            AbstractItem firstItem = itemIterator.getItem(0);

            if (firstItem instanceof AddTabItem) {
                return (AddTabItem) firstItem;
            }
        }

        return null;
    }

    /**
     * Adapts the color of a button, which allows to add a new tab.
     *
     * @param addTabItem
     *         The add tab item, which corresponds to the button, whose color should be adapted, as
     *         an instance of the class {@link AddTabItem}. The add tab item may not be null
     */
    private void adaptAddTabButtonColor(@NonNull final AddTabItem addTabItem) {
        ColorStateList colorStateList = getStyle().getAddTabButtonColor();
        int[] stateSet = new int[]{};
        int color = colorStateList.getColorForState(stateSet, colorStateList.getDefaultColor());
        View view = addTabItem.getView();
        Drawable background = view.getBackground();
        background.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }

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
     * @param style
     *         The style, which allows to retrieve style attributes of the tab switcher, as an
     *         instance of the class {@link TabSwitcherStyle}. The style may not be null
     */
    public TabletTabRecyclerAdapter(@NonNull final TabSwitcher tabSwitcher,
                                    @NonNull final TabSwitcherModel model,
                                    @NonNull final TabSwitcherStyle style) {
        super(tabSwitcher, model, style);
    }

    @Override
    public final void onTabBackgroundColorChanged(@Nullable final ColorStateList colorStateList) {
        super.onTabBackgroundColorChanged(colorStateList);
        AddTabItem addTabItem = getAddTabItem();

        if (addTabItem != null) {
            adaptAddTabButtonColor(addTabItem);
        }
    }

    @Override
    public final void onAddTabButtonColorChanged(@Nullable final ColorStateList colorStateList) {
        AddTabItem addTabItem = getAddTabItem();

        if (addTabItem != null) {
            adaptAddTabButtonColor(addTabItem);
        }
    }

    @Override
    public final int getViewTypeCount() {
        return super.getViewTypeCount() + 1;
    }

    @Override
    public final int getViewType(@NonNull final AbstractItem item) {
        if (item instanceof AddTabItem) {
            return ADD_TAB_BUTTON_VIEW_TYPE;
        } else {
            return super.getViewType(item);
        }
    }

    @NonNull
    @Override
    public final View onInflateView(@NonNull final LayoutInflater inflater,
                                    @Nullable final ViewGroup parent,
                                    @NonNull final AbstractItem item, final int viewType,
                                    @NonNull final Integer... params) {
        if (viewType == ADD_TAB_BUTTON_VIEW_TYPE) {
            View view = inflater.inflate(R.layout.tablet_add_tab_button, parent, false);
            item.setView(view);
            view.setTag(R.id.tag_properties, item.getTag());
            return view;
        } else {
            return super.onInflateView(inflater, parent, item, viewType, params);
        }
    }

    @Override
    public final void onShowView(@NonNull final Context context, @NonNull final View view,
                                 @NonNull final AbstractItem item, final boolean inflated,
                                 @NonNull final Integer... params) {
        if (item instanceof AddTabItem) {
            adaptAddTabButtonColor((AddTabItem) item);
        } else {
            super.onShowView(context, view, item, inflated, params);
        }
    }

    @CallSuper
    @Override
    public final void onRemoveView(@NonNull final View view, @NonNull final AbstractItem item) {
        if (item instanceof AddTabItem) {
            view.setTag(R.id.tag_properties, null);
        } else {
            super.onRemoveView(view, item);
        }
    }

    @NonNull
    @Override
    protected final View onInflateTabView(@NonNull final LayoutInflater inflater,
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
    protected final void onShowTabView(@NonNull final View view, @NonNull final TabItem tabItem,
                                       @NonNull final Integer... params) {

    }

    @NonNull
    @Override
    protected final AbstractTabViewHolder onCreateTabViewHolder() {
        return new TabletTabViewHolder();
    }

    @NonNull
    @Override
    protected final Layout getLayout() {
        return Layout.TABLET;
    }

}