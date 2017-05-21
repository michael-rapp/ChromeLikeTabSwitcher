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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import de.mrapp.android.tabswitcher.layout.AbstractTabSwitcherLayout;
import de.mrapp.android.tabswitcher.layout.AbstractTabSwitcherLayout.LayoutListenerWrapper;
import de.mrapp.android.tabswitcher.layout.TabSwitcherLayout;
import de.mrapp.android.tabswitcher.layout.phone.PhoneArithmetics;
import de.mrapp.android.tabswitcher.layout.phone.PhoneTabSwitcherLayout;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.tabswitcher.view.TabSwitcherButton;
import de.mrapp.android.util.DisplayUtil.DeviceType;
import de.mrapp.android.util.DisplayUtil.Orientation;
import de.mrapp.android.util.ViewUtil;
import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.android.util.view.AbstractSavedState;

import static de.mrapp.android.util.Condition.ensureNotNull;
import static de.mrapp.android.util.DisplayUtil.getDeviceType;
import static de.mrapp.android.util.DisplayUtil.getOrientation;

/**
 * A tab switcher, which allows to switch between multiple tabs. It it is designed similar to the
 * tab switcher of the Google Chrome Android app.
 *
 * In order to specify the appearance of individual tabs, a class, which extends from the abstract
 * class {@link TabSwitcherDecorator}, must be implemented and set to the tab switcher via the
 * <code>setDecorator</code>-method.
 *
 * The currently selected tab is shown fullscreen by default. When displaying the switcher via the
 * <code>showSwitcher-method</code>, an overview of all tabs is shown, allowing to select an other
 * tab by clicking it. By swiping a tab or by clicking its close button, it can be removed,
 * resulting in the selected tab to be altered automatically. The switcher can programmatically be
 * hidden by calling the <code>hideSwitcher</code>-method. By calling the
 * <code>setSelectedTab</code>-method programmatically, a tab is selected and shown fullscreen.
 *
 * Individual tabs are represented by instances of the class {@link Tab}. Such tabs can dynamically
 * be added to the tab switcher by using the <code>addTab</code>-methods. In order to remove them
 * afterwards, the <code>removeTab</code> can be used. If the switcher is currently shown, calling
 * these methods results in the tabs being added or removed in an animated manner.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class TabSwitcher extends FrameLayout implements TabSwitcherLayout, Model {

    /**
     * A saved state, which allows to store the state of a {@link TabSwitcher}.
     */
    private static class TabSwitcherState extends AbstractSavedState {

        /**
         * A creator, which allows to create instances of the class {@link TabSwitcherState}.
         */
        public static Creator<TabSwitcherState> CREATOR = new Creator<TabSwitcherState>() {

            @Override
            public TabSwitcherState createFromParcel(final Parcel source) {
                return new TabSwitcherState(source);
            }

            @Override
            public TabSwitcherState[] newArray(final int size) {
                return new TabSwitcherState[size];
            }

        };

        /**
         * The saved layout policy, which is used by the tab switcher.
         */
        private LayoutPolicy layoutPolicy;

        /**
         * The saved state of the model, which is used by the tab switcher.
         */
        private Bundle modelState;

        /**
         * Creates a new saved state, which allows to store the state of a {@link TabSwitcher}.
         *
         * @param source
         *         The parcel to read read from as a instance of the class {@link Parcel}. The
         *         parcel may not be null
         */
        private TabSwitcherState(@NonNull final Parcel source) {
            super(source);
            layoutPolicy = (LayoutPolicy) source.readSerializable();
            modelState = source.readBundle(getClass().getClassLoader());
        }

        /**
         * Creates a new saved state, which allows to store the state of a {@link TabSwitcher}.
         *
         * @param superState
         *         The state of the superclass of the view, this saved state corresponds to, as an
         *         instance of the type {@link Parcelable} or null if no state is available
         */
        TabSwitcherState(@Nullable final Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            super.writeToParcel(dest, flags);
            dest.writeSerializable(layoutPolicy);
            dest.writeBundle(modelState);
        }

    }

    /**
     * A queue, which contains all pending actions.
     */
    private Queue<Runnable> pendingActions;

    /**
     * A set, which contains the listeners, which should be notified about the tab switcher's
     * events.
     */
    private Set<TabSwitcherListener> listeners;

    /**
     * The layout policy, which is used by the tab switcher.
     */
    private LayoutPolicy layoutPolicy;

    /**
     * The model, which is used by the tab switcher.
     */
    private TabSwitcherModel model;

    /**
     * The layout, which is used by the tab switcher, depending on whether the device is a
     * smartphone or tablet and the set layout policy.
     */
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
        pendingActions = new LinkedList<>();
        listeners = new LinkedHashSet<>();
        model = new TabSwitcherModel(this);
        model.addListener(createModelListener());
        getViewTreeObserver().addOnGlobalLayoutListener(
                new LayoutListenerWrapper(this, createGlobalLayoutListener(false)));
        setPadding(super.getPaddingLeft(), super.getPaddingTop(), super.getPaddingRight(),
                super.getPaddingBottom());
        obtainStyledAttributes(attributeSet, defaultStyle, defaultStyleResource);
    }

    /**
     * Initializes a specific layout.
     *
     * @param inflatedTabsOnly
     *         True, if only the tabs should be inflated, false otherwise
     * @param layout
     *         The layout, which should be initialized, as a value of the enum {@link Layout}. The
     *         layout may not be null
     */
    private void initializeLayout(@NonNull final Layout layout, final boolean inflatedTabsOnly) {
        if (layout == Layout.TABLET) {
            // TODO: Use tablet layout once implemented
            PhoneArithmetics arithmetics = new PhoneArithmetics(TabSwitcher.this);
            this.layout = new PhoneTabSwitcherLayout(TabSwitcher.this, model, arithmetics);
        } else {
            PhoneArithmetics arithmetics = new PhoneArithmetics(TabSwitcher.this);
            this.layout = new PhoneTabSwitcherLayout(TabSwitcher.this, model, arithmetics);
        }

        this.layout.setCallback(createLayoutCallback());
        this.model.addListener(this.layout);
        this.layout.inflateLayout(inflatedTabsOnly);
        final ViewGroup tabContainer = getTabContainer();
        assert tabContainer != null;

        if (ViewCompat.isLaidOut(tabContainer)) {
            this.layout.onGlobalLayout();
        } else {
            tabContainer.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

                        @Override
                        public void onGlobalLayout() {
                            ViewUtil.removeOnGlobalLayoutListener(
                                    tabContainer.getViewTreeObserver(), this);
                            TabSwitcher.this.layout.onGlobalLayout();
                        }

                    });
        }
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
            obtainLayoutPolicy(typedArray);
            obtainBackground(typedArray);
            obtainTabIcon(typedArray);
            obtainTabBackgroundColor(typedArray);
            obtainTabTitleTextColor(typedArray);
            obtainTabCloseButtonIcon(typedArray);
            obtainToolbarTitle(typedArray);
            obtainToolbarNavigationIcon(typedArray);
            obtainToolbarMenu(typedArray);
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Obtains the layout policy from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the layout policy should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainLayoutPolicy(@NonNull final TypedArray typedArray) {
        int defaultValue = LayoutPolicy.AUTO.getValue();
        int value = typedArray.getInt(R.styleable.TabSwitcher_layoutPolicy, defaultValue);
        setLayoutPolicy(LayoutPolicy.fromValue(value));
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
     * Obtains the icon of a tab from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the icon should be obtained from, as an instance of the class {@link
     *         TypedArray}. The typed array may not be null
     */
    private void obtainTabIcon(@NonNull final TypedArray typedArray) {
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_tabIcon, -1);

        if (resourceId != -1) {
            setTabIcon(resourceId);
        }
    }

    /**
     * Obtains the background color of a tab from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the background color should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabBackgroundColor(@NonNull final TypedArray typedArray) {
        ColorStateList colorStateList =
                typedArray.getColorStateList(R.styleable.TabSwitcher_tabBackgroundColor);

        if (colorStateList != null) {
            setTabBackgroundColor(colorStateList);
        }
    }

    /**
     * Obtains the text color of a tab's title from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the text color should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabTitleTextColor(@NonNull final TypedArray typedArray) {
        ColorStateList colorStateList =
                typedArray.getColorStateList(R.styleable.TabSwitcher_tabTitleTextColor);

        if (colorStateList != null) {
            setTabTitleTextColor(colorStateList);
        }
    }

    /**
     * Obtains the icon of a tab's close button from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the icon should be obtained from, as an instance of the class {@link
     *         TypedArray}. The typed array may not be null
     */
    private void obtainTabCloseButtonIcon(@NonNull final TypedArray typedArray) {
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_tabCloseButtonIcon, -1);

        if (resourceId != -1) {
            setTabCloseButtonIcon(resourceId);
        }
    }

    /**
     * Obtains the title of the toolbar, which is shown, when the tab switcher is shown, from a
     * specific typed array.
     *
     * @param typedArray
     *         The typed array, the title should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    private void obtainToolbarTitle(@NonNull final TypedArray typedArray) {
        CharSequence title = typedArray.getText(R.styleable.TabSwitcher_toolbarTitle);

        if (!TextUtils.isEmpty(title)) {
            setToolbarTitle(title);
        }
    }

    /**
     * Obtains the navigation icon of the toolbar, which is shown, when the tab switcher is shown,
     * from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the navigation icon should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainToolbarNavigationIcon(@NonNull final TypedArray typedArray) {
        Drawable icon = typedArray.getDrawable(R.styleable.TabSwitcher_toolbarNavigationIcon);

        if (icon != null) {
            setToolbarNavigationIcon(icon, null);
        }
    }

    /**
     * Obtains the menu of the toolbar, which is shown, when the tab switcher is shown, from a
     * specific typed array.
     *
     * @param typedArray
     *         The typed array, the menu should be obtained from, as an instance of the class {@link
     *         TypedArray}. The typed array may not be null
     */
    private void obtainToolbarMenu(@NonNull final TypedArray typedArray) {
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_toolbarMenu, -1);

        if (resourceId != -1) {
            inflateToolbarMenu(resourceId, null);
        }
    }

    /**
     * Enqueues a specific action to be executed, when no animation is running.
     *
     * @param action
     *         The action, which should be enqueued as an instance of the type {@link Runnable}. The
     *         action may not be null
     */
    private void enqueuePendingAction(@NonNull final Runnable action) {
        ensureNotNull(action, "The action may not be null");
        pendingActions.add(action);
        executePendingAction();
    }

    /**
     * Executes the next pending action.
     */
    private void executePendingAction() {
        if (!isAnimationRunning()) {
            final Runnable action = pendingActions.poll();

            if (action != null) {
                new Runnable() {

                    @Override
                    public void run() {
                        action.run();
                        executePendingAction();
                    }

                }.run();
            }
        }
    }

    /**
     * Creates and returns a listener, which allows to observe, when the tab switcher's model is
     * modified.
     *
     * @return The listener, which has been created, as an instance of the type {@link
     * Model.Listener}. The listener may not be null
     */
    @NonNull
    private Model.Listener createModelListener() {
        return new Model.Listener() {

            @Override
            public void onLogLevelChanged(@NonNull final LogLevel logLevel) {

            }

            @Override
            public void onDecoratorChanged(@NonNull final TabSwitcherDecorator decorator) {

            }

            @Override
            public void onSwitcherShown() {
                notifyOnSwitcherShown();
            }

            @Override
            public void onSwitcherHidden() {
                notifyOnSwitcherHidden();
            }

            @Override
            public void onSelectionChanged(final int previousIndex, final int index,
                                           @Nullable final Tab selectedTab,
                                           final boolean switcherHidden) {
                notifyOnSelectionChanged(index, selectedTab);

                if (switcherHidden) {
                    notifyOnSwitcherHidden();
                }
            }

            @Override
            public void onTabAdded(final int index, @NonNull final Tab tab,
                                   final int previousSelectedTabIndex, final int selectedTabIndex,
                                   final boolean switcherVisibilityChanged,
                                   @NonNull final Animation animation) {
                notifyOnTabAdded(index, tab, animation);

                if (previousSelectedTabIndex != selectedTabIndex) {
                    notifyOnSelectionChanged(selectedTabIndex,
                            selectedTabIndex != -1 ? getTab(selectedTabIndex) : null);
                }

                if (switcherVisibilityChanged) {
                    notifyOnSwitcherHidden();
                }
            }

            @Override
            public void onAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                       final int previousSelectedTabIndex,
                                       final int selectedTabIndex,
                                       @NonNull final Animation animation) {
                for (Tab tab : tabs) {
                    notifyOnTabAdded(index, tab, animation);
                }

                if (previousSelectedTabIndex != selectedTabIndex) {
                    notifyOnSelectionChanged(selectedTabIndex,
                            selectedTabIndex != -1 ? getTab(selectedTabIndex) : null);
                }
            }

            @Override
            public void onTabRemoved(final int index, @NonNull final Tab tab,
                                     final int previousSelectedTabIndex, final int selectedTabIndex,
                                     @NonNull final Animation animation) {
                notifyOnTabRemoved(index, tab, animation);

                if (previousSelectedTabIndex != selectedTabIndex) {
                    notifyOnSelectionChanged(selectedTabIndex,
                            selectedTabIndex != -1 ? getTab(selectedTabIndex) : null);
                }
            }

            @Override
            public void onAllTabsRemoved(@NonNull final Tab[] tabs,
                                         @NonNull final Animation animation) {
                notifyOnAllTabsRemoved(tabs, animation);
                notifyOnSelectionChanged(-1, null);
            }

            @Override
            public void onPaddingChanged(final int left, final int top, final int right,
                                         final int bottom) {

            }

            @Override
            public void onTabIconChanged(@Nullable final Drawable icon) {

            }

            @Override
            public void onTabBackgroundColorChanged(@Nullable final ColorStateList colorStateList) {

            }

            @Override
            public void onTabTitleColorChanged(@Nullable final ColorStateList colorStateList) {

            }

            @Override
            public void onTabCloseButtonIconChanged(@Nullable final Drawable icon) {

            }

            @Override
            public void onToolbarVisibilityChanged(final boolean visible) {

            }

            @Override
            public void onToolbarTitleChanged(@Nullable final CharSequence title) {

            }

            @Override
            public void onToolbarNavigationIconChanged(@Nullable final Drawable icon,
                                                       @Nullable final OnClickListener listener) {

            }

            @Override
            public void onToolbarMenuInflated(@MenuRes final int resourceId,
                                              @Nullable final OnMenuItemClickListener listener) {

            }

        };
    }

    /**
     * Creates and returns a callback, which allows to observe, when all pending animations of a
     * layout have been ended.
     *
     * @return The callback, which has been created, as an instance of the type {@link
     * AbstractTabSwitcherLayout.Callback}. The callback may not be null
     */
    @NonNull
    private AbstractTabSwitcherLayout.Callback createLayoutCallback() {
        return new AbstractTabSwitcherLayout.Callback() {

            @Override
            public void onAnimationsEnded() {
                executePendingAction();
            }

        };
    }

    /**
     * Creates and returns a listener, which allows to inflate the view's layout once the view is
     * laid out.
     *
     * @param inflateTabsOnly
     *         True, if only the tabs should be inflated, false otherwise
     * @return The listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createGlobalLayoutListener(final boolean inflateTabsOnly) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                ensureNotNull(getDecorator(), "No decorator has been set",
                        IllegalStateException.class);
                initializeLayout(getLayout(), inflateTabsOnly);
            }

        };
    }

    /**
     * Notifies all listeners, that the tab switcher has been shown.
     */
    private void notifyOnSwitcherShown() {
        for (TabSwitcherListener listener : listeners) {
            listener.onSwitcherShown(this);
        }
    }

    /**
     * Notifies all listeners, that the tab switcher has been hidden.
     */
    private void notifyOnSwitcherHidden() {
        for (TabSwitcherListener listener : listeners) {
            listener.onSwitcherHidden(this);
        }
    }

    /**
     * Notifies all listeners, that the selected tab has been changed.
     *
     * @param selectedTabIndex
     *         The index of the currently selected tab as an {@link Integer} value or -1, if no tab
     *         is currently selected
     * @param selectedTab
     *         The currently selected tab as an instance of the class {@link Tab} or null,  if no
     *         tab is currently selected
     */
    private void notifyOnSelectionChanged(final int selectedTabIndex,
                                          @Nullable final Tab selectedTab) {
        for (TabSwitcherListener listener : listeners) {
            listener.onSelectionChanged(this, selectedTabIndex, selectedTab);
        }
    }

    /**
     * Notifies all listeners, that a specific tab has been added to the tab switcher.
     *
     * @param index
     *         The index of the tab, which has been added, as an {@link Integer} value
     * @param tab
     *         The tab, which has been added, as an instance of the class {@link Tab}. The tab may
     *         not be null
     * @param animation
     *         The animation, which has been used to add the tab, as an instance of the class {@link
     *         Animation}. The animation may not be null
     */
    private void notifyOnTabAdded(final int index, @NonNull final Tab tab,
                                  @NonNull final Animation animation) {
        for (TabSwitcherListener listener : listeners) {
            listener.onTabAdded(this, index, tab, animation);
        }
    }

    /**
     * Notifies all listeners, that a specific tab has been removed from the tab switcher.
     *
     * @param index
     *         The index of the tab, which has been removed, as an {@link Integer} value
     * @param tab
     *         The tab, which has been removed, as an instance of the class {@link Tab}. The tab may
     *         not be null
     * @param animation
     *         The animation, which has been used to remove the tab, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    private void notifyOnTabRemoved(final int index, @NonNull final Tab tab,
                                    @NonNull final Animation animation) {
        for (TabSwitcherListener listener : listeners) {
            listener.onTabRemoved(this, index, tab, animation);
        }
    }

    /**
     * Notifies all listeners, that all tabs have been removed from the tab switcher.
     *
     * @param tabs
     *         An array, which contains the tabs, which have been removed, as an array of the type
     *         {@link Tab} or an empty array, if no tabs have been removed
     * @param animation
     *         The animation, which has been used to remove the tabs, as an instance of the class
     *         {@link Animation}. The animation may not be null
     */
    private void notifyOnAllTabsRemoved(@NonNull final Tab[] tabs,
                                        @NonNull final Animation animation) {
        for (TabSwitcherListener listener : listeners) {
            listener.onAllTabsRemoved(this, tabs, animation);
        }
    }

    /**
     * Creates a new tab switcher, which allows to switch between multiple tabs.
     *
     * @param context
     *         The context, which should be used by the view, as an instance of the class {@link
     *         Context}. The context may not be null
     */
    public TabSwitcher(@NonNull final Context context) {
        this(context, null);
    }

    /**
     * Creates a new tab switcher, which allows to switch between multiple tabs.
     *
     * @param context
     *         The context, which should be used by the view, as an instance of the class {@link
     *         Context}. The context may not be null
     * @param attributeSet
     *         The attribute set, the view's attributes should be obtained from, as an instance of
     *         the type {@link AttributeSet} or null, if no attributes should be obtained
     */
    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize(attributeSet, 0, 0);
    }

    /**
     * Creates a new tab switcher, which allows to switch between multiple tabs.
     *
     * @param context
     *         The context, which should be used by the view, as an instance of the class {@link
     *         Context}. The context may not be null
     * @param attributeSet
     *         The attribute set, the view's attributes should be obtained from, as an instance of
     *         the type {@link AttributeSet} or null, if no attributes should be obtained
     * @param defaultStyle
     *         The default style to apply to this view. If 0, no style will be applied (beyond what
     *         is included in the theme). This may either be an attribute resource, whose value will
     *         be retrieved from the current theme, or an explicit style resource
     */
    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet,
                       @AttrRes final int defaultStyle) {
        super(context, attributeSet, defaultStyle);
        initialize(attributeSet, defaultStyle, 0);
    }

    /**
     * Creates a new tab switcher, which allows to switch between multiple tabs.
     *
     * @param context
     *         The context, which should be used by the view, as an instance of the class {@link
     *         Context}. The context may not be null
     * @param attributeSet
     *         The attribute set, the view's attributes should be obtained from, as an instance of
     *         the type {@link AttributeSet} or null, if no attributes should be obtained
     * @param defaultStyle
     *         The default style to apply to this view. If 0, no style will be applied (beyond what
     *         is included in the theme). This may either be an attribute resource, whose value will
     *         be retrieved from the current theme, or an explicit style resource
     * @param defaultStyleResource
     *         A resource identifier of a style resource that supplies default values for the view,
     *         used only if the default style is 0 or can not be found in the theme. Can be 0 to not
     *         look for defaults
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TabSwitcher(@NonNull final Context context, @Nullable final AttributeSet attributeSet,
                       @AttrRes final int defaultStyle, @StyleRes final int defaultStyleResource) {
        super(context, attributeSet, defaultStyle, defaultStyleResource);
        initialize(attributeSet, defaultStyle, defaultStyleResource);
    }

    /**
     * Setups the tab switcher to be associated with those menu items of a specific menu, which use
     * a {@link TabSwitcherButton} as their action view. The icon of such menu items will
     * automatically be updated, when the number of tabs, which are contained by the tab switcher,
     * changes.
     *
     * @param tabSwitcher
     *         The tab switcher, which should become associated with the menu items, as an instance
     *         of the class {@link TabSwitcher}. The tab switcher may not be null
     * @param menu
     *         The menu, whose menu items should become associated with the given tab switcher, as
     *         an instance of the type {@link Menu}. The menu may not be null
     * @param listener
     *         The listener, which should be set to the menu items, which use a {@link
     *         TabSwitcherButton} as their action view, as an instance of the type {@link
     *         OnClickListener} or null, if no listener should be set
     */
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

    /**
     * Adds a listener, which should be notified about the tab switcher's events.
     *
     * @param listener
     *         The listener, which should be added, as an instance of the type {@link
     *         TabSwitcherListener}. The listener may not be null
     */
    public final void addListener(@NonNull final TabSwitcherListener listener) {
        ensureNotNull(listener, "The listener may not be null");
        this.listeners.add(listener);
    }

    /**
     * Removes a specific listener, which should not be notified about the tab switcher's events,
     * anymore.
     *
     * @param listener
     *         The listener, which should be removed, as an instance of the type {@link
     *         TabSwitcherListener}. The listener may not be null
     */
    public final void removeListener(@NonNull final TabSwitcherListener listener) {
        ensureNotNull(listener, "The listener may not be null");
        this.listeners.remove(listener);
    }

    /**
     * Returns the layout policy, which is used by the tab switcher.
     *
     * @return The layout policy, which is used by the tab switcher, as a value of the enum {@link
     * LayoutPolicy}. The layout policy may either be {@link LayoutPolicy#AUTO}, {@link
     * LayoutPolicy#PHONE} or {@link LayoutPolicy#TABLET}
     */
    @NonNull
    public final LayoutPolicy getLayoutPolicy() {
        return layoutPolicy;
    }

    /**
     * Sets the layout policy, which should be used by the tab switcher.
     *
     * Changing the layout policy after the view has been laid out does not have any effect.
     *
     * @param layoutPolicy
     *         The layout policy, which should be set, as a value of the enum {@link LayoutPolicy}.
     *         The layout policy may either be {@link LayoutPolicy#AUTO}, {@link LayoutPolicy#PHONE}
     *         or {@link LayoutPolicy#TABLET}
     */
    public final void setLayoutPolicy(@NonNull final LayoutPolicy layoutPolicy) {
        ensureNotNull(layoutPolicy, "The layout policy may not be null");

        if (this.layoutPolicy != layoutPolicy) {
            Layout previousLayout = getLayout();
            this.layoutPolicy = layoutPolicy;

            if (layout != null) {
                Layout newLayout = getLayout();

                if (previousLayout != newLayout) {
                    layout.detachLayout(false);
                    model.removeListener(layout);
                    initializeLayout(newLayout, false);
                }
            }
        }
    }

    /**
     * Returns the layout of the tab switcher.
     *
     * @return The layout of the tab switcher as a value of the enum {@link Layout}. The layout may
     * either be {@link Layout#PHONE_PORTRAIT}, {@link Layout#PHONE_LANDSCAPE} or {@link
     * Layout#TABLET}
     */
    @NonNull
    public final Layout getLayout() {
        if (layoutPolicy == LayoutPolicy.TABLET || (layoutPolicy == LayoutPolicy.AUTO &&
                getDeviceType(getContext()) == DeviceType.TABLET)) {
            return Layout.TABLET;
        } else {
            return getOrientation(getContext()) == Orientation.LANDSCAPE ? Layout.PHONE_LANDSCAPE :
                    Layout.PHONE_PORTRAIT;
        }
    }

    @Override
    public final void addTab(@NonNull final Tab tab) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.addTab(tab);
            }

        });
    }

    @Override
    public final void addTab(@NonNull final Tab tab, final int index) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.addTab(tab, index);
            }

        });
    }

    @Override
    public final void addTab(@NonNull final Tab tab, final int index,
                             @NonNull final Animation animation) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.addTab(tab, index, animation);
            }

        });
    }

    @Override
    public final void addAllTabs(@NonNull final Collection<? extends Tab> tabs) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.addAllTabs(tabs);
            }

        });
    }

    @Override
    public final void addAllTabs(@NonNull final Collection<? extends Tab> tabs, final int index) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.addAllTabs(tabs, index);
            }

        });
    }

    @Override
    public final void addAllTabs(@NonNull final Collection<? extends Tab> tabs, final int index,
                                 @NonNull final Animation animation) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.addAllTabs(tabs, index, animation);
            }

        });
    }

    @Override
    public final void addAllTabs(@NonNull final Tab[] tabs) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.addAllTabs(tabs);
            }

        });
    }

    @Override
    public final void addAllTabs(@NonNull final Tab[] tabs, final int index) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.addAllTabs(tabs, index);
            }

        });
    }

    @Override
    public final void addAllTabs(@NonNull final Tab[] tabs, final int index,
                                 @NonNull final Animation animation) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.addAllTabs(tabs, index, animation);
            }

        });
    }

    @Override
    public final void removeTab(@NonNull final Tab tab) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.removeTab(tab);
            }

        });
    }

    @Override
    public final void removeTab(@NonNull final Tab tab, @NonNull final Animation animation) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.removeTab(tab, animation);
            }

        });
    }

    @Override
    public final void clear() {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.clear();
            }

        });
    }

    @Override
    public final void clear(@NonNull final Animation animationType) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.clear(animationType);
            }

        });
    }

    @Override
    public final void selectTab(@NonNull final Tab tab) {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.selectTab(tab);
            }

        });
    }

    @Nullable
    @Override
    public final Tab getSelectedTab() {
        return model.getSelectedTab();
    }

    @Override
    public final int getSelectedTabIndex() {
        return model.getSelectedTabIndex();
    }

    @Override
    public final Iterator<Tab> iterator() {
        return model.iterator();
    }

    @Override
    public final boolean isEmpty() {
        return model.isEmpty();
    }

    @Override
    public final int getCount() {
        return model.getCount();
    }

    @NonNull
    @Override
    public final Tab getTab(final int index) {
        return model.getTab(index);
    }

    @Override
    public final int indexOf(@NonNull final Tab tab) {
        return model.indexOf(tab);
    }

    @Override
    public final boolean isSwitcherShown() {
        return model.isSwitcherShown();
    }

    @Override
    public final void showSwitcher() {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.showSwitcher();
            }

        });
    }

    @Override
    public final void hideSwitcher() {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.hideSwitcher();
            }

        });
    }

    @Override
    public final void toggleSwitcherVisibility() {
        enqueuePendingAction(new Runnable() {

            @Override
            public void run() {
                model.toggleSwitcherVisibility();
            }

        });
    }

    @Override
    public final void setDecorator(@NonNull final TabSwitcherDecorator decorator) {
        model.setDecorator(decorator);
    }

    @Override
    public final TabSwitcherDecorator getDecorator() {
        return model.getDecorator();
    }

    @NonNull
    @Override
    public final LogLevel getLogLevel() {
        return model.getLogLevel();
    }

    @Override
    public final void setLogLevel(@NonNull final LogLevel logLevel) {
        model.setLogLevel(logLevel);
    }

    @Override
    public final void setPadding(final int left, final int top, final int right, final int bottom) {
        model.setPadding(left, top, right, bottom);
    }

    @Override
    public final int getPaddingLeft() {
        return model.getPaddingLeft();
    }

    @Override
    public final int getPaddingTop() {
        return model.getPaddingTop();
    }

    @Override
    public final int getPaddingRight() {
        return model.getPaddingRight();
    }

    @Override
    public final int getPaddingBottom() {
        return model.getPaddingBottom();
    }

    @Override
    public final int getPaddingStart() {
        return model.getPaddingStart();
    }

    @Override
    public final int getPaddingEnd() {
        return model.getPaddingEnd();
    }

    @Nullable
    @Override
    public final Drawable getTabIcon() {
        return model.getTabIcon();
    }

    @Override
    public final void setTabIcon(@DrawableRes final int resourceId) {
        model.setTabIcon(resourceId);
    }

    @Override
    public final void setTabIcon(@Nullable final Bitmap icon) {
        model.setTabIcon(icon);
    }

    @Nullable
    @Override
    public final ColorStateList getTabBackgroundColor() {
        return model.getTabBackgroundColor();
    }

    @Override
    public final void setTabBackgroundColor(@ColorInt final int color) {
        model.setTabBackgroundColor(color);
    }

    @Override
    public final void setTabBackgroundColor(@Nullable final ColorStateList colorStateList) {
        model.setTabBackgroundColor(colorStateList);
    }

    @Nullable
    @Override
    public final ColorStateList getTabTitleTextColor() {
        return model.getTabTitleTextColor();
    }

    @Override
    public final void setTabTitleTextColor(@ColorInt final int color) {
        model.setTabTitleTextColor(color);
    }

    @Override
    public final void setTabTitleTextColor(@Nullable final ColorStateList colorStateList) {
        model.setTabTitleTextColor(colorStateList);
    }

    @Nullable
    @Override
    public final Drawable getTabCloseButtonIcon() {
        return model.getTabCloseButtonIcon();
    }

    @Override
    public final void setTabCloseButtonIcon(@DrawableRes final int resourceId) {
        model.setTabCloseButtonIcon(resourceId);
    }

    @Override
    public final void setTabCloseButtonIcon(@Nullable final Bitmap icon) {
        model.setTabCloseButtonIcon(icon);
    }

    @Override
    public final boolean areToolbarsShown() {
        return model.areToolbarsShown();
    }

    @Override
    public final void showToolbars(final boolean show) {
        model.showToolbars(show);
    }

    @Nullable
    @Override
    public final CharSequence getToolbarTitle() {
        Toolbar[] toolbars = getToolbars();
        return toolbars != null ? toolbars[0].getTitle() : model.getToolbarTitle();
    }

    @Override
    public final void setToolbarTitle(@StringRes final int resourceId) {
        model.setToolbarTitle(resourceId);
    }

    @Override
    public final void setToolbarTitle(@Nullable final CharSequence title) {
        model.setToolbarTitle(title);
    }

    @Nullable
    @Override
    public final Drawable getToolbarNavigationIcon() {
        Toolbar[] toolbars = getToolbars();
        return toolbars != null ? toolbars[0].getNavigationIcon() :
                model.getToolbarNavigationIcon();
    }

    @Override
    public final void setToolbarNavigationIcon(@Nullable final Drawable icon,
                                               @Nullable final OnClickListener listener) {
        model.setToolbarNavigationIcon(icon, listener);
    }

    @Override
    public final void setToolbarNavigationIcon(@DrawableRes final int resourceId,
                                               @Nullable final OnClickListener listener) {
        model.setToolbarNavigationIcon(resourceId, listener);
    }

    @Override
    public final void inflateToolbarMenu(@MenuRes final int resourceId,
                                         @Nullable final OnMenuItemClickListener listener) {
        model.inflateToolbarMenu(resourceId, listener);
    }

    @Override
    public final void addCloseTabListener(@NonNull final TabCloseListener listener) {
        model.addCloseTabListener(listener);
    }

    @Override
    public final void removeCloseTabListener(@NonNull final TabCloseListener listener) {
        model.removeCloseTabListener(listener);
    }

    @Override
    public final void addTabPreviewListener(@NonNull final TabPreviewListener listener) {
        model.addTabPreviewListener(listener);
    }

    @Override
    public final void removeTabPreviewListener(@NonNull final TabPreviewListener listener) {
        model.removeTabPreviewListener(listener);
    }

    @Override
    public final boolean isAnimationRunning() {
        return layout != null && layout.isAnimationRunning();
    }

    @Nullable
    @Override
    public final ViewGroup getTabContainer() {
        return layout != null ? layout.getTabContainer() : null;
    }

    @Override
    public final Toolbar[] getToolbars() {
        return layout != null ? layout.getToolbars() : null;
    }

    @Nullable
    @Override
    public final Menu getToolbarMenu() {
        return layout != null ? layout.getToolbarMenu() : null;
    }

    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        return (layout != null && layout.handleTouchEvent(event)) || super.onTouchEvent(event);
    }

    @Override
    public final Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        TabSwitcherState savedState = new TabSwitcherState(superState);
        savedState.layoutPolicy = layoutPolicy;
        savedState.modelState = new Bundle();
        Pair<Integer, Float> pair = layout.detachLayout(true);

        if (pair != null) {
            savedState.modelState
                    .putInt(TabSwitcherModel.FIRST_VISIBLE_TAB_INDEX_EXTRA, pair.first);
            savedState.modelState
                    .putFloat(TabSwitcherModel.FIRST_VISIBLE_TAB_POSITION_EXTRA, pair.second);
            model.setFirstVisibleTabIndex(pair.first);
            model.setFirstVisibleTabPosition(pair.second);
        } else {
            model.setFirstVisibleTabPosition(-1);
            model.setFirstVisibleTabIndex(-1);
        }

        model.removeListener(layout);
        layout = null;
        executePendingAction();
        getViewTreeObserver().addOnGlobalLayoutListener(
                new LayoutListenerWrapper(this, createGlobalLayoutListener(true)));
        model.saveInstanceState(savedState.modelState);
        return savedState;
    }

    @Override
    public final void onRestoreInstanceState(final Parcelable state) {
        if (state instanceof TabSwitcherState) {
            TabSwitcherState savedState = (TabSwitcherState) state;
            this.layoutPolicy = savedState.layoutPolicy;
            model.restoreInstanceState(savedState.modelState);
            super.onRestoreInstanceState(savedState.getSuperState());
        } else {
            super.onRestoreInstanceState(state);
        }
    }

}