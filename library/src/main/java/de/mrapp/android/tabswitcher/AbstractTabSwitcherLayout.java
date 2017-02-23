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
package de.mrapp.android.tabswitcher;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.mrapp.android.tabswitcher.model.AnimationType;

import static de.mrapp.android.util.Condition.ensureAtLeast;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An abstract base class for all layouts, which implement the functionality of a {@link
 * TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class AbstractTabSwitcherLayout implements TabSwitcherLayout {

    /**
     * The tab switcher, the layout belongs to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * A set, which contains the listeners, which should be notified about the tab switcher's
     * events.
     */
    private final Set<TabSwitcherListener> listeners;

    /**
     * A list, which contains the tabs, which are contained by the tab switcher.
     */
    private final List<Tab> tabs;

    /**
     * The decorator, which allows to inflate the views, which correspond to the tab switcher's
     * tabs.
     */
    private TabSwitcherDecorator decorator;

    /**
     * True, if the tab switcher is currently shown, false otherwise.
     */
    private boolean switcherShown;

    /**
     * The index of the currently selected tab.
     */
    private int selectedTabIndex;

    /**
     * An array, which contains the left, top, right and bottom padding of the tab switcher.
     */
    private int[] padding;

    /**
     * Sets, whether the tab switcher is currently shown, or not.
     *
     * @param shown
     *         True, if the tab switcher is currently shown, false otherwise
     */
    protected final void setSwitcherShown(final boolean shown) {
        this.switcherShown = shown;
    }

    /**
     * Sets the index of the currently selected tab.
     *
     * @param index
     *         The index, which should be set, as an {@link Integer} value. The index must be at
     *         least -1
     */
    protected final void setSelectedTabIndex(final int index) {
        ensureAtLeast(index, -1, "The index must be at least -1");
        this.selectedTabIndex = index;
    }

    /**
     * The method, which is invoked on implementing subclasses, when the decorator has been
     * changed.
     *
     * @param decorator
     *         The decorator, which has been set, as an instance of the class {@link
     *         TabSwitcherDecorator}. The decorator may not be null
     */
    protected abstract void onDecoratorChanged(@NonNull final TabSwitcherDecorator decorator);

    /**
     * The method, which is invoked on implementing subclasses, when the padding has been changed.
     *
     * @param left
     *         The left padding, which has been set, as an {@link Integer} value
     * @param top
     *         The top padding, which has been set, as an {@link Integer} value
     * @param right
     *         The right padding, which has been set, as an {@link Integer} value
     * @param bottom
     *         The bottom padding, which has been set, as an {@link Integer} value
     */
    protected abstract void onPaddingChanged(final int left, final int top, final int right,
                                             final int bottom);

    /**
     * Creates a new layout, which implements the functionality of a {@link TabSwitcher}.
     *
     * @param tabSwitcher
     *         The tab switcher, the layout belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     */
    public AbstractTabSwitcherLayout(@NonNull final TabSwitcher tabSwitcher) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        this.tabSwitcher = tabSwitcher;
        this.listeners = new LinkedHashSet<>();
        this.tabs = new ArrayList<>();
        this.decorator = null;
        this.switcherShown = false;
        this.selectedTabIndex = -1;
        this.padding = new int[]{0, 0, 0, 0};
    }

    @Override
    public final void setDecorator(@NonNull final TabSwitcherDecorator decorator) {
        ensureNotNull(decorator, "The decorator may not be null");
        this.decorator = decorator;
        onDecoratorChanged(decorator);
    }

    @Override
    public final TabSwitcherDecorator getDecorator() {
        ensureNotNull(decorator, "No decorator has been set", IllegalStateException.class);
        return decorator;
    }

    @Override
    public final void addListener(@NonNull final TabSwitcherListener listener) {
        ensureNotNull(listener, "The listener may not be null");
        this.listeners.add(listener);
    }

    @Override
    public final void removeListener(@NonNull final TabSwitcherListener listener) {
        ensureNotNull(listener, "The listener may not be null");
        this.listeners.remove(listener);
    }

    @Override
    public final boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public final int getCount() {
        return tabs.size();
    }

    @NonNull
    @Override
    public final Tab getTab(final int index) {
        return tabs.get(index);
    }

    @Override
    public final int indexOf(@NonNull final Tab tab) {
        ensureNotNull(tab, "The tab may not be null");
        return tabs.indexOf(tab);
    }

    @Override
    public final void addTab(@NonNull final Tab tab) {
        addTab(tab, getCount());
    }

    @Override
    public final void addTab(@NonNull final Tab tab, final int index) {
        addTab(tab, index, AnimationType.SWIPE_RIGHT);
    }

    @Override
    public final boolean isSwitcherShown() {
        return switcherShown;
    }

    @Override
    public final void toggleSwitcherVisibility() {
        if (isSwitcherShown()) {
            hideSwitcher();
        } else {
            showSwitcher();
        }
    }

    @Nullable
    @Override
    public final Tab getSelectedTab() {
        return getSelectedTabIndex() != -1 ? getTab(getSelectedTabIndex()) : null;
    }

    @Override
    public final int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    @Override
    public final void showToolbar(final boolean show) {
        getToolbar().setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public final boolean isToolbarShown() {
        return getToolbar().getVisibility() == View.VISIBLE;
    }

    @Override
    public final void setToolbarTitle(@Nullable final CharSequence title) {
        getToolbar().setTitle(title);
    }

    @Override
    public final void setToolbarTitle(@StringRes final int resourceId) {
        setToolbarTitle(tabSwitcher.getContext().getText(resourceId));
    }

    @Override
    public final void inflateToolbarMenu(@MenuRes final int resourceId,
                                         @Nullable final OnMenuItemClickListener listener) {
        getToolbar().inflateMenu(resourceId);
        getToolbar().setOnMenuItemClickListener(listener);
    }

    @NonNull
    @Override
    public final Menu getToolbarMenu() {
        return getToolbar().getMenu();
    }

    @Override
    public final void setToolbarNavigationIcon(@Nullable final Drawable icon,
                                               @Nullable final OnClickListener listener) {
        getToolbar().setNavigationIcon(icon);
        getToolbar().setNavigationOnClickListener(listener);
    }

    @Override
    public final void setToolbarNavigationIcon(@DrawableRes final int resourceId,
                                               @Nullable final OnClickListener listener) {
        setToolbarNavigationIcon(ContextCompat.getDrawable(tabSwitcher.getContext(), resourceId),
                listener);
    }

    @Override
    public final void setPadding(final int left, final int top, final int right, final int bottom) {
        padding = new int[]{left, top, right, bottom};
        onPaddingChanged(left, top, right, bottom);
    }

    @Override
    public final int getPaddingLeft() {
        return padding[0];
    }

    @Override
    public final int getPaddingTop() {
        return padding[1];
    }

    @Override
    public final int getPaddingRight() {
        return padding[2];
    }

    @Override
    public final int getPaddingBottom() {
        return padding[3];
    }

    @Override
    public final int getPaddingStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return tabSwitcher.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ?
                    getPaddingRight() : getPaddingLeft();
        }

        return getPaddingLeft();
    }

    @Override
    public final int getPaddingEnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return tabSwitcher.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ?
                    getPaddingLeft() : getPaddingRight();
        }

        return getPaddingRight();
    }

}