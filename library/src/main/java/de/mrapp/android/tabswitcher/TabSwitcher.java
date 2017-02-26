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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import de.mrapp.android.tabswitcher.model.AnimationType;
import de.mrapp.android.tabswitcher.model.Layout;
import de.mrapp.android.tabswitcher.view.TabSwitcherButton;
import de.mrapp.android.util.ViewUtil;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A chrome-like tab switcher.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabSwitcher extends FrameLayout implements TabSwitcherLayout {

    private AbstractTabSwitcherLayout layout;

    /**
     * Initializes the view.
     *
     * @param attributeSet
     *         The attribute set, which should be used to initialize the view, as an instance of the
     *         type {@link AttributeSet} or null, if no attributes should be obtained
     * @param defaultStyle
     *         The default style to apply to this view. If 0, no style will be applied (beyond what
     *         is included in the theme). This may either be an attribute resource, whose value will
     *         be retrieved from the current theme, or an explicit style resource
     * @param defaultStyleResource
     *         A resource identifier of a style resource that supplies default values for the view,
     *         used only if the default style is 0 or can not be found in the theme. Can be 0 to not
     *         look for defaults
     */
    private void initialize(@Nullable final AttributeSet attributeSet,
                            @AttrRes final int defaultStyle,
                            @StyleRes final int defaultStyleResource) {
        layout = new PhoneTabSwitcherLayout(this);
        layout.inflateLayout();
        getViewTreeObserver().addOnGlobalLayoutListener(layout);
        obtainStyledAttributes(attributeSet, defaultStyle, defaultStyleResource);
    }

    /**
     * Obtains all attributes from a specific attribute set.
     *
     * @param attributeSet
     *         The attribute set, the attributes should be obtained from, as an instance of the type
     *         {@link AttributeSet} or null, if no attributes should be obtained
     * @param defaultStyle
     *         The default style to apply to this view. If 0, no style will be applied (beyond what
     *         is included in the theme). This may either be an attribute resource, whose value will
     *         be retrieved from the current theme, or an explicit style resource
     * @param defaultStyleResource
     *         A resource identifier of a style resource that supplies default values for the view,
     *         used only if the default style is 0 or can not be found in the theme. Can be 0 to not
     *         look for defaults
     */
    private void obtainStyledAttributes(@Nullable final AttributeSet attributeSet,
                                        @AttrRes final int defaultStyle,
                                        @StyleRes final int defaultStyleResource) {
        TypedArray typedArray = getContext()
                .obtainStyledAttributes(attributeSet, R.styleable.TabSwitcher, defaultStyle,
                        defaultStyleResource);

        try {
            obtainBackground(typedArray);
            obtainTabBackgroundColor(typedArray);
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Obtains the view's background from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the background should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    private void obtainBackground(@NonNull final TypedArray typedArray) {
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_android_background, 0);

        if (resourceId != 0) {
            ViewUtil.setBackground(this, ContextCompat.getDrawable(getContext(), resourceId));
        } else {
            int defaultValue =
                    ContextCompat.getColor(getContext(), R.color.tab_switcher_background_color);
            int color =
                    typedArray.getColor(R.styleable.TabSwitcher_android_background, defaultValue);
            setBackgroundColor(color);
        }
    }

    /**
     * Obtains the background color of tabs from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the background color should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabBackgroundColor(@NonNull final TypedArray typedArray) {
        int defaultValue = ContextCompat.getColor(getContext(), R.color.tab_background_color);
        // TODO: Call setTabBackgroundColor method once created
        // recyclerAdapter.setTabBackgroundColor(
        //        typedArray.getColor(R.styleable.TabSwitcher_tabBackgroundColor, defaultValue));
    }

    public TabSwitcher(@NonNull final Context context) {
        this(context, null);
    }

    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize(attributeSet, 0, 0);
    }

    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet,
                       @AttrRes final int defaultStyle) {
        super(context, attributeSet, defaultStyle);
        initialize(attributeSet, defaultStyle, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet,
                       @AttrRes final int defaultStyle, @StyleRes final int defaultStyleResource) {
        super(context, attributeSet, defaultStyle, defaultStyleResource);
        initialize(attributeSet, defaultStyle, defaultStyleResource);
    }

    public static void setupWithMenu(@NonNull final TabSwitcher tabSwitcher,
                                     @NonNull final Menu menu,
                                     @Nullable final OnClickListener listener) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(menu, "The menu may not be null");

        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            View view = menuItem.getActionView();

            if (view instanceof TabSwitcherButton) {
                TabSwitcherButton tabSwitcherButton = (TabSwitcherButton) view;
                tabSwitcherButton.setOnClickListener(listener);
                tabSwitcherButton.setCount(tabSwitcher.getCount());
                tabSwitcher.addListener(tabSwitcherButton);
            }
        }
    }

    @NonNull
    @Override
    public final Layout getLayout() {
        return layout.getLayout();
    }

    @Override
    public final void addTab(@NonNull final Tab tab) {
        layout.addTab(tab);
    }

    @Override
    public final void addTab(@NonNull final Tab tab, final int index) {
        layout.addTab(tab, index);
    }

    @Override
    public final void addTab(@NonNull final Tab tab, final int index,
                             @NonNull final AnimationType animationType) {
        layout.addTab(tab, index, animationType);
    }

    @Override
    public final void removeTab(@NonNull final Tab tab) {
        layout.removeTab(tab);
    }

    @Override
    public final void removeTab(@NonNull final Tab tab,
                                @NonNull final AnimationType animationType) {
        layout.removeTab(tab, animationType);
    }

    @Override
    public final void clear() {
        layout.clear();
    }

    @Override
    public final void clear(@NonNull final AnimationType animationType) {
        layout.clear(animationType);
    }

    @Override
    public final void selectTab(@NonNull final Tab tab) {
        layout.selectTab(tab);
    }

    @Nullable
    @Override
    public final Tab getSelectedTab() {
        return layout.getSelectedTab();
    }

    @Override
    public final int getSelectedTabIndex() {
        return layout.getSelectedTabIndex();
    }

    @Override
    public final boolean isEmpty() {
        return layout.isEmpty();
    }

    @Override
    public final int getCount() {
        return layout.getCount();
    }

    @NonNull
    @Override
    public final Tab getTab(final int index) {
        return layout.getTab(index);
    }

    @Override
    public final int indexOf(@NonNull final Tab tab) {
        return layout.indexOf(tab);
    }

    @Override
    public final boolean isSwitcherShown() {
        return layout.isSwitcherShown();
    }

    @Override
    public final void showSwitcher() {
        layout.showSwitcher();
    }

    @Override
    public final void hideSwitcher() {
        layout.hideSwitcher();
    }

    @Override
    public final void toggleSwitcherVisibility() {
        layout.toggleSwitcherVisibility();
    }

    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        return layout.handleTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public final boolean isAnimationRunning() {
        return layout.isAnimationRunning();
    }

    @Override
    public final void setDecorator(@NonNull final TabSwitcherDecorator decorator) {
        layout.setDecorator(decorator);
    }

    @Override
    public final TabSwitcherDecorator getDecorator() {
        return layout.getDecorator();
    }

    @Override
    public final void addListener(@NonNull final TabSwitcherListener listener) {
        layout.addListener(listener);
    }

    @Override
    public final void removeListener(@NonNull final TabSwitcherListener listener) {
        layout.removeListener(listener);
    }

    @Override
    public final void addCloseTabListener(@NonNull final TabCloseListener listener) {
        layout.addCloseTabListener(listener);
    }

    @Override
    public final void removeCloseTabListener(@NonNull final TabCloseListener listener) {
        layout.removeCloseTabListener(listener);
    }

    @NonNull
    @Override
    public final ViewGroup getTabContainer() {
        return layout.getTabContainer();
    }

    @NonNull
    @Override
    public final Toolbar getToolbar() {
        return layout.getToolbar();
    }

    @Override
    public final void showToolbar(final boolean show) {
        layout.showToolbar(show);
    }

    @Override
    public final boolean isToolbarShown() {
        return layout.isToolbarShown();
    }

    @Override
    public final void setToolbarTitle(@Nullable final CharSequence title) {
        layout.setToolbarTitle(title);
    }

    @Override
    public final void setToolbarTitle(@StringRes final int resourceId) {
        layout.setToolbarTitle(resourceId);
    }

    @Override
    public final void inflateToolbarMenu(@MenuRes final int resourceId,
                                         @Nullable final OnMenuItemClickListener listener) {
        layout.inflateToolbarMenu(resourceId, listener);
    }

    @NonNull
    @Override
    public final Menu getToolbarMenu() {
        return layout.getToolbarMenu();
    }

    @Override
    public final void setToolbarNavigationIcon(@Nullable final Drawable icon,
                                               @Nullable final OnClickListener listener) {
        layout.setToolbarNavigationIcon(icon, listener);
    }

    @Override
    public final void setToolbarNavigationIcon(@DrawableRes final int resourceId,
                                               @Nullable final OnClickListener listener) {
        layout.setToolbarNavigationIcon(resourceId, listener);
    }

    @Override
    public final void setPadding(final int left, final int top, final int right, final int bottom) {
        layout.setPadding(left, top, right, bottom);
    }

    @Override
    public final int getPaddingLeft() {
        return layout.getPaddingLeft();
    }

    @Override
    public final int getPaddingTop() {
        return layout.getPaddingTop();
    }

    @Override
    public final int getPaddingRight() {
        return layout.getPaddingRight();
    }

    @Override
    public final int getPaddingBottom() {
        return layout.getPaddingBottom();
    }

    @Override
    public final int getPaddingStart() {
        return layout.getPaddingStart();
    }

    @Override
    public final int getPaddingEnd() {
        return layout.getPaddingEnd();
    }

}