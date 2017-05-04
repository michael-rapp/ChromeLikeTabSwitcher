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
import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabCloseListener;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.iterator.AbstractTabItemIterator;
import de.mrapp.android.tabswitcher.iterator.TabItemIterator;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.tabswitcher.util.ThemeHelper;
import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.android.util.view.AbstractViewRecycler;
import de.mrapp.android.util.view.AttachedViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An abstract base class for all view recycler adapters, which allow to inflate the views, which
 * are used to visualize the tabs of a {@link TabSwitcher}.
 *
 * @param <ParamType>
 *         The type of the optional parameters, which may be passed when inflating a view
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class AbstractRecyclerAdapter<ParamType>
        extends AbstractViewRecycler.Adapter<TabItem, ParamType>
        implements Tab.Callback, Model.Listener {

    /**
     * The tab switcher, the tabs belong to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The model, which belongs to the tab switcher.
     */
    private final TabSwitcherModel model;

    /*
     * The theme helper, which allows to retrieve resources, depending on the tab switcher's theme.
     */
    private final ThemeHelper themeHelper;

    /**
     * The default text color of a tab's title.
     */
    private final ColorStateList tabTitleTextColor;

    /**
     * The default background color of tabs.
     */
    private final ColorStateList tabBackgroundColor;

    /**
     * The default icon of a tab's close button.
     */
    private final Drawable closeButtonIcon;

    /**
     * The view recycler, the adapter is bound to.
     */
    private AttachedViewRecycler<TabItem, ParamType> viewRecycler;

    /**
     * Adapts the title of a tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose title should be adapted, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptTitle(@NonNull final TabItem tabItem) {
        Tab tab = tabItem.getTab();
        AbstractTabViewHolder viewHolder = tabItem.getViewHolder();
        viewHolder.titleTextView.setText(tab.getTitle());
    }

    /**
     * Adapts the icon of a tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose icon should be adapted, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptIcon(@NonNull final TabItem tabItem) {
        Tab tab = tabItem.getTab();
        AbstractTabViewHolder viewHolder = tabItem.getViewHolder();
        Drawable icon = tab.getIcon(model.getContext());

        if (icon == null) {
            icon = model.getTabIcon();

            if (icon == null) {
                try {
                    icon = getThemeHelper().getDrawable(getLayout(), R.attr.tabSwitcherTabIcon);
                } catch (NotFoundException e) {
                    icon = null;
                }
            }
        }

        viewHolder.titleTextView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
    }

    /**
     * Adapts the visibility of a tab's close button.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose close button should be adapted, as
     *         an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptCloseButton(@NonNull final TabItem tabItem) {
        Tab tab = tabItem.getTab();
        AbstractTabViewHolder viewHolder = tabItem.getViewHolder();
        viewHolder.closeButton.setVisibility(tab.isCloseable() ? View.VISIBLE : View.GONE);
        viewHolder.closeButton.setOnClickListener(
                tab.isCloseable() ? createCloseButtonClickListener(viewHolder.closeButton, tab) :
                        null);
    }

    /**
     * Adapts the icon of a tab's close button.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose close button icon should be
     *         adapted, as an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptCloseButtonIcon(@NonNull final TabItem tabItem) {
        Tab tab = tabItem.getTab();
        AbstractTabViewHolder viewHolder = tabItem.getViewHolder();
        Drawable icon = tab.getCloseButtonIcon(model.getContext());

        if (icon == null) {
            icon = model.getTabCloseButtonIcon();
        }

        viewHolder.closeButton.setImageDrawable(icon != null ? icon : closeButtonIcon);
    }

    /**
     * Adapts the background color of a tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose background should be adapted, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptBackgroundColor(@NonNull final TabItem tabItem) {
        Tab tab = tabItem.getTab();
        ColorStateList colorStateList =
                tab.getBackgroundColor() != null ? tab.getBackgroundColor() :
                        model.getTabBackgroundColor();

        if (colorStateList == null) {
            colorStateList = tabBackgroundColor;
        }

        int[] stateSet = model.getSelectedTab() == tab ? new int[]{android.R.attr.state_selected} :
                new int[]{};
        int color = colorStateList.getColorForState(stateSet, colorStateList.getDefaultColor());
        View view = tabItem.getView();
        Drawable background = view.getBackground();
        background.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        onAdaptBackgroundColor(color, tabItem);
    }

    /**
     * Adapts the text color of a tab's title.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose title should be adapted, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptTitleTextColor(@NonNull final TabItem tabItem) {
        Tab tab = tabItem.getTab();
        AbstractTabViewHolder viewHolder = tabItem.getViewHolder();
        ColorStateList colorStateList = tab.getTitleTextColor() != null ? tab.getTitleTextColor() :
                model.getTabTitleTextColor();

        if (colorStateList != null) {
            viewHolder.titleTextView.setTextColor(colorStateList);
        } else {
            viewHolder.titleTextView.setTextColor(tabTitleTextColor);
        }
    }

    /**
     * Adapts the selection state of a tab's views.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose selection state should be adapted,
     *         as an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptSelectionState(@NonNull final TabItem tabItem) {
        boolean selected = model.getSelectedTab() == tabItem.getTab();
        tabItem.getView().setSelected(selected);
        AbstractTabViewHolder viewHolder = tabItem.getViewHolder();
        viewHolder.titleTextView.setSelected(selected);
        viewHolder.closeButton.setSelected(selected);
    }

    /**
     * Adapts the appearance of all currently inflated tabs, depending on whether they are currently
     * selected, or not.
     */
    private void adaptAllSelectionStates() {
        AbstractTabItemIterator iterator =
                new TabItemIterator.Builder(model, getViewRecyclerOrThrowException()).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.isInflated()) {
                adaptSelectionState(tabItem);
                adaptBackgroundColor(tabItem);
            }
        }
    }

    /**
     * Creates and returns a listener, which allows to close a specific tab, when its close button
     * is clicked.
     *
     * @param closeButton
     *         The tab's close button as an instance of the class {@link ImageButton}. The button
     *         may not be null
     * @param tab
     *         The tab, which should be closed, as an instance of the class {@link Tab}. The tab may
     *         not be null
     * @return The listener, which has been created, as an instance of the class {@link
     * View.OnClickListener}. The listener may not be null
     */
    @NonNull
    private View.OnClickListener createCloseButtonClickListener(
            @NonNull final ImageButton closeButton, @NonNull final Tab tab) {
        return new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                if (notifyOnCloseTab(tab)) {
                    closeButton.setOnClickListener(null);
                    tabSwitcher.removeTab(tab);
                }
            }

        };
    }

    /**
     * Notifies all listeners, that a tab is about to be closed by clicking its close button.
     *
     * @param tab
     *         The tab, which is about to be closed, as an instance of the class {@link Tab}. The
     *         tab may not be null
     * @return True, if the tab should be closed, false otherwise
     */
    private boolean notifyOnCloseTab(@NonNull final Tab tab) {
        boolean result = true;

        for (TabCloseListener listener : model.getTabCloseListeners()) {
            result &= listener.onCloseTab(tabSwitcher, tab);
        }

        return result;
    }

    /**
     * Returns the tab switcher, which contains the tabs.
     *
     * @return The tab switcher, which contains the tabs, as an instance of the class {@link
     * TabSwitcher}. The tab switcher may not be null
     */
    @NonNull
    protected final TabSwitcher getTabSwitcher() {
        return tabSwitcher;
    }

    /**
     * Returns the model of the tab switcher.
     *
     * @return The model of the tab switcher as an instance of the class {@link TabSwitcherModel}.
     * The model may not be null
     */
    @NonNull
    protected final TabSwitcherModel getModel() {
        return model;
    }

    /**
     * Returns the theme helper, which allows to retrieve resources, depending on the tab switcher's
     * theme.
     *
     * @return The theme helper, which allows to retrieve resources, depending on the tab switcher's
     * theme, as an instance of the class {@link ThemeHelper}. The theme helper may not be null
     */
    @NonNull
    protected final ThemeHelper getThemeHelper() {
        return themeHelper;
    }

    /**
     * Returns the tab item, which corresponds to a specific tab.
     *
     * @param tab
     *         The tab, whose tab item should be returned, as an instance of the class {@link Tab}.
     *         The tab may not be null
     * @return The tab item, which corresponds to the given tab, as an instance of the class {@link
     * TabItem} or null, if no view, which visualizes the tab, is currently inflated
     */
    @Nullable
    protected final TabItem getTabItem(@NonNull final Tab tab) {
        int index = model.indexOf(tab);

        if (index != -1) {
            TabItem tabItem = TabItem.create(model, getViewRecyclerOrThrowException(), index);

            if (tabItem.isInflated()) {
                return tabItem;
            }
        }

        return null;
    }

    /**
     * Returns the view recycler, the adapter is bound to, or throws an {@link
     * IllegalStateException}, if no view recycler has been set.
     *
     * @return The view recycler, the adapter is bound to, as an instance of the class {@link
     * AttachedViewRecycler}. The view recycler may not be null
     */
    @NonNull
    protected final AttachedViewRecycler<TabItem, ParamType> getViewRecyclerOrThrowException() {
        ensureNotNull(viewRecycler, "No view recycler has been set", IllegalStateException.class);
        return viewRecycler;
    }

    /**
     * The method, which is invoked on implementing subclasses, when the background color of a tab
     * has been changed.
     *
     * @param color
     *         The color, which has been set, as an {@link Integer} value
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose background color has been changed,
     *         as an instance of the class {@link TabItem}. The tab item may not be null
     */
    protected void onAdaptBackgroundColor(@ColorInt final int color,
                                          @NonNull final TabItem tabItem) {

    }

    /**
     * The method, which is invoked on implementing subclasses in order to inflate the view, which
     * is used to visualize tabs.
     *
     * @param inflater
     *         The layout inflater, which should be used, as an instance of the class {@link
     *         LayoutInflater}. The layout inflater may not be null
     * @param parent
     *         The parent of the view, which should be inflated, as an instance of the class {@link
     *         ViewGroup} or null, if no parent is available
     * @param viewHolder
     *         The view holder, which should hold references to the child views of the view, which
     *         should be inflated, as an instance of the class {@link AbstractTabViewHolder}. The
     *         view holder may not be null
     * @return The view, which has been inflated, as an instance of the class {@link View}. The view
     * may not be null
     */
    @NonNull
    protected abstract View onInflateView(@NonNull final LayoutInflater inflater,
                                          @Nullable final ViewGroup parent,
                                          @NonNull final AbstractTabViewHolder viewHolder);

    /**
     * The method, which is invoked on implementing subclasses in order to adapt the appearance of a
     * view, which is used to visualize a tab.
     *
     * @param view
     *         The view, which is used to visualize the tab, as an instance of the class {@link
     *         View}. The view may not be null
     * @param tabItem
     *         The tab item, which corresponds to the tab, which is visualized by the given view, as
     *         an instance of the class {@link TabItem}. The tab item may not be null
     * @param params
     *         An array, which may contain optional parameters, as an array of the generic type
     *         ParamType or an empty array, if no optional parameters are available
     */
    @SuppressWarnings("unchecked")
    protected abstract void onShowView(@NonNull final View view, @NonNull final TabItem tabItem,
                                       @NonNull final ParamType... params);

    /**
     * The method, which is invoked on implementing subclasses in order to create the view holder,
     * which shold be associated with an inflated view.
     *
     * @return The view holder, which has been created, as an instance of the class {@link
     * AbstractTabViewHolder}. The view holder may not be null
     */
    @NonNull
    protected abstract AbstractTabViewHolder onCreateViewHolder();

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the layout,
     * which is used by the tab switcher.
     *
     * @return The layout, which is used by the tab switcher, as a value of the enum {@link Layout}.
     * The layout may not be null
     */
    @NonNull
    protected abstract Layout getLayout();

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
     * @param themeHelper
     *         The theme helper, which allows to retrieve resources, depending on the tab switcher's
     *         theme, as an instance of the class {@link ThemeHelper}. The theme helper may not be
     *         null
     */
    public AbstractRecyclerAdapter(@NonNull final TabSwitcher tabSwitcher,
                                   @NonNull final TabSwitcherModel model,
                                   @NonNull final ThemeHelper themeHelper) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(model, "The model may not be null");
        ensureNotNull(themeHelper, "The theme helper may not be null");
        this.tabSwitcher = tabSwitcher;
        this.model = model;
        this.themeHelper = themeHelper;
        this.tabTitleTextColor =
                themeHelper.getColorStateList(getLayout(), R.attr.tabSwitcherTabTitleTextColor);
        this.tabBackgroundColor =
                themeHelper.getColorStateList(getLayout(), R.attr.tabSwitcherTabBackgroundColor);
        this.closeButtonIcon =
                themeHelper.getDrawable(getLayout(), R.attr.tabSwitcherTabCloseButtonIcon);
        this.viewRecycler = null;
    }

    /**
     * Sets the view recycler, which allows to inflate the views, which are used to visualize tabs.
     *
     * @param viewRecycler
     *         The view recycler, which should be set, as an instance of the class
     *         AttachedViewRecycler. The view recycler may not be null
     */
    public final void setViewRecycler(
            @NonNull final AttachedViewRecycler<TabItem, ParamType> viewRecycler) {
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        this.viewRecycler = viewRecycler;
    }

    @Override
    public void onLogLevelChanged(@NonNull final LogLevel logLevel) {

    }

    @Override
    public final void onDecoratorChanged(@NonNull final TabSwitcherDecorator decorator) {

    }

    @Override
    public final void onSwitcherShown() {

    }

    @Override
    public final void onSwitcherHidden() {

    }

    @Override
    public final void onSelectionChanged(final int previousIndex, final int index,
                                         @Nullable final Tab selectedTab,
                                         final boolean switcherHidden) {
        adaptAllSelectionStates();
    }

    @Override
    public final void onTabAdded(final int index, @NonNull final Tab tab,
                                 final int previousSelectedTabIndex, final int selectedTabIndex,
                                 final boolean switcherVisibilityChanged,
                                 @NonNull final Animation animation) {
        if (previousSelectedTabIndex != selectedTabIndex) {
            adaptAllSelectionStates();
        }
    }

    @Override
    public final void onAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                     final int previousSelectedTabIndex, final int selectedTabIndex,
                                     @NonNull final Animation animation) {
        if (previousSelectedTabIndex != selectedTabIndex) {
            adaptAllSelectionStates();
        }
    }

    @Override
    public final void onTabRemoved(final int index, @NonNull final Tab tab,
                                   final int previousSelectedTabIndex, final int selectedTabIndex,
                                   @NonNull final Animation animation) {
        if (previousSelectedTabIndex != selectedTabIndex) {
            adaptAllSelectionStates();
        }
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final Tab[] tabs,
                                       @NonNull final Animation animation) {

    }

    @Override
    public void onPaddingChanged(final int left, final int top, final int right, final int bottom) {

    }

    @Override
    public final void onTabIconChanged(@Nullable final Drawable icon) {
        TabItemIterator iterator =
                new TabItemIterator.Builder(model, getViewRecyclerOrThrowException()).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.isInflated()) {
                adaptIcon(tabItem);
            }
        }
    }

    @Override
    public final void onTabBackgroundColorChanged(@Nullable final ColorStateList colorStateList) {
        TabItemIterator iterator =
                new TabItemIterator.Builder(model, getViewRecyclerOrThrowException()).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.isInflated()) {
                adaptBackgroundColor(tabItem);
            }
        }
    }

    @Override
    public void onTabContentBackgroundColorChanged(@ColorInt final int color) {

    }

    @Override
    public final void onTabTitleColorChanged(@Nullable final ColorStateList colorStateList) {
        TabItemIterator iterator =
                new TabItemIterator.Builder(model, getViewRecyclerOrThrowException()).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.isInflated()) {
                adaptTitleTextColor(tabItem);
            }
        }
    }

    @Override
    public final void onTabCloseButtonIconChanged(@Nullable final Drawable icon) {
        TabItemIterator iterator =
                new TabItemIterator.Builder(model, getViewRecyclerOrThrowException()).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.isInflated()) {
                adaptCloseButtonIcon(tabItem);
            }
        }
    }

    @Override
    public final void onToolbarVisibilityChanged(final boolean visible) {

    }

    @Override
    public final void onToolbarTitleChanged(@Nullable final CharSequence title) {

    }

    @Override
    public final void onToolbarNavigationIconChanged(@Nullable final Drawable icon,
                                                     @Nullable final View.OnClickListener listener) {

    }

    @Override
    public final void onToolbarMenuInflated(@MenuRes final int resourceId,
                                            @Nullable final Toolbar.OnMenuItemClickListener listener) {

    }

    @Override
    public final void onTitleChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptTitle(tabItem);
        }
    }

    @Override
    public final void onIconChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptIcon(tabItem);
        }
    }

    @Override
    public final void onCloseableChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptCloseButton(tabItem);
        }
    }

    @Override
    public final void onCloseButtonIconChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptCloseButtonIcon(tabItem);
        }
    }

    @Override
    public final void onBackgroundColorChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptBackgroundColor(tabItem);
        }
    }

    @Override
    public void onContentBackgroundColorChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onTitleTextColorChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptTitleTextColor(tabItem);
        }
    }

    @SafeVarargs
    @NonNull
    @Override
    public final View onInflateView(@NonNull final LayoutInflater inflater,
                                    @Nullable final ViewGroup parent,
                                    @NonNull final TabItem tabItem, final int viewType,
                                    @NonNull final ParamType... params) {
        AbstractTabViewHolder viewHolder = onCreateViewHolder();
        View view = onInflateView(inflater, parent, viewHolder);
        viewHolder.titleTextView = (TextView) view.findViewById(R.id.tab_title_text_view);
        viewHolder.closeButton = (ImageButton) view.findViewById(R.id.close_tab_button);
        view.setTag(R.id.tag_view_holder, viewHolder);
        tabItem.setView(view);
        tabItem.setViewHolder(viewHolder);
        view.setTag(R.id.tag_properties, tabItem.getTag());
        return view;
    }

    @SafeVarargs
    @Override
    public final void onShowView(@NonNull final Context context, @NonNull final View view,
                                 @NonNull final TabItem tabItem, final boolean inflated,
                                 @NonNull final ParamType... params) {
        AbstractTabViewHolder viewHolder =
                (AbstractTabViewHolder) view.getTag(R.id.tag_view_holder);

        if (!tabItem.isInflated()) {
            tabItem.setView(view);
            tabItem.setViewHolder(viewHolder);
            view.setTag(R.id.tag_properties, tabItem.getTag());
        }

        Tab tab = tabItem.getTab();
        tab.addCallback(this);
        adaptTitle(tabItem);
        adaptIcon(tabItem);
        adaptCloseButton(tabItem);
        adaptCloseButtonIcon(tabItem);
        adaptBackgroundColor(tabItem);
        adaptTitleTextColor(tabItem);
        adaptSelectionState(tabItem);
        onShowView(view, tabItem, params);
    }

    @CallSuper
    @Override
    public void onRemoveView(@NonNull final View view, @NonNull final TabItem tabItem) {
        Tab tab = tabItem.getTab();
        tab.removeCallback(this);
        view.setTag(R.id.tag_properties, null);
    }

}
