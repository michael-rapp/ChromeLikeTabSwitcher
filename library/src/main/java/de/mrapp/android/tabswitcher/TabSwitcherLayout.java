package de.mrapp.android.tabswitcher;

import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.Menu;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import de.mrapp.android.tabswitcher.model.AnimationType;
import de.mrapp.android.tabswitcher.model.Layout;

/**
 * @author Michael Rapp
 */
public interface TabSwitcherLayout {

    void setDecorator(@NonNull TabSwitcherDecorator decorator);

    TabSwitcherDecorator getDecorator();

    void addListener(@NonNull TabSwitcherListener listener);

    void removeListener(@NonNull TabSwitcherListener listener);

    @NonNull
    Layout getLayout();

    boolean isEmpty();

    int getCount();

    @NonNull
    Tab getTab(int index);

    int indexOf(@NonNull Tab tab);

    void addTab(@NonNull Tab tab);

    void addTab(@NonNull Tab tab, int index);

    void addTab(@NonNull Tab tab, int index, @NonNull AnimationType animationType);

    void removeTab(@NonNull Tab tab);

    void clear();

    boolean isSwitcherShown();

    void showSwitcher();

    void hideSwitcher();

    void toggleSwitcherVisibility();

    @Nullable
    Tab getSelectedTab();

    int getSelectedTabIndex();

    void selectTab(@NonNull Tab tab);

    @NonNull
    ViewGroup getTabContainer();

    @NonNull
    Toolbar getToolbar();

    void showToolbar(boolean show);

    boolean isToolbarShown();

    void setToolbarTitle(@Nullable CharSequence title);

    void setToolbarTitle(@StringRes int resourceId);

    void inflateToolbarMenu(@MenuRes int resourceId, @Nullable OnMenuItemClickListener listener);

    @NonNull
    Menu getToolbarMenu();

    void setToolbarNavigationIcon(@Nullable Drawable icon, @Nullable OnClickListener listener);

    void setToolbarNavigationIcon(@DrawableRes int resourceId, @Nullable OnClickListener listener);

    void setPadding(int left, int top, int right, int bottom);

    int getPaddingLeft();

    int getPaddingTop();

    int getPaddingRight();

    int getPaddingBottom();

    int getPaddingStart();

    int getPaddingEnd();

}