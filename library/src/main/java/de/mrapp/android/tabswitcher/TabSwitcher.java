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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import de.mrapp.android.tabswitcher.gesture.AbstractTouchEventHandler;
import de.mrapp.android.tabswitcher.gesture.DragGestureEventHandlerFactory;
import de.mrapp.android.tabswitcher.gesture.TouchEventDispatcher;
import de.mrapp.android.tabswitcher.layout.AbstractTabSwitcherLayout;
import de.mrapp.android.tabswitcher.layout.AbstractTabSwitcherLayout.LayoutListenerWrapper;
import de.mrapp.android.tabswitcher.layout.ContentRecyclerAdapter;
import de.mrapp.android.tabswitcher.layout.TabSwitcherLayout;
import de.mrapp.android.tabswitcher.layout.phone.PhoneArithmetics;
import de.mrapp.android.tabswitcher.layout.phone.PhoneTabSwitcherLayout;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.tabswitcher.model.TabSwitcherStyle;
import de.mrapp.android.tabswitcher.util.ThemeHelper;
import de.mrapp.android.tabswitcher.view.TabSwitcherButton;
import de.mrapp.android.util.DisplayUtil.Orientation;
import de.mrapp.android.util.ViewUtil;
import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.android.util.view.AbstractSavedState;
import de.mrapp.android.util.view.AbstractViewRecycler;
import de.mrapp.util.Condition;

import static de.mrapp.android.util.DisplayUtil.getOrientation;

/**
 * A tab switcher, which allows to switch between multiple tabs. It it is designed similar to the
 * tab switcher of the Google Chrome Android app.
 * <p>
 * In order to specify the appearance of individual tabs, a class, which extends from the abstract
 * class {@link TabSwitcherDecorator}, must be implemented and set to the tab switcher via the
 * <code>setDecorator</code>-method.
 * <p>
 * The currently selected tab is shown fullscreen by default. When displaying the switcher via the
 * <code>showSwitcher-method</code>, an overview of all tabs is shown, allowing to select an other
 * tab by clicking it. By swiping a tab or by clicking its close button, it can be removed,
 * resulting in the selected tab to be altered automatically. The switcher can programmatically be
 * hidden by calling the <code>hideSwitcher</code>-method. By calling the
 * <code>setSelectedTab</code>-method programmatically, a tab is selected and shown fullscreen.
 * <p>
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
     * The index of the primary toolbar as returned by the method {@link
     * TabSwitcher#getToolbars()}.
     */
    public static final int PRIMARY_TOOLBAR_INDEX = 0;

    /**
     * The index of the secondary toolbar as returned by the method {@link
     * TabSwitcher#getToolbars()}.
     */
    public static final int SECONDARY_TOOLBAR_INDEX = 1;

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
     * The style, which allows to retrieve the style attributes of the tab switcher.
     */
    private TabSwitcherStyle style;

    /**
     * The theme helper, which allows to retrieve resources, depending on the tab switcher's theme.
     */
    private ThemeHelper themeHelper;

    /**
     * The dispatcher, which is used to dispatch touch events.
     */
    private TouchEventDispatcher touchEventDispatcher;

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
        listeners = new CopyOnWriteArraySet<>();
        model = new TabSwitcherModel(this);
        model.addListener(createModelListener());
        touchEventDispatcher = new TouchEventDispatcher();
        setPadding(super.getPaddingLeft(), super.getPaddingTop(), super.getPaddingRight(),
                super.getPaddingBottom());
        obtainStyledAttributes(attributeSet, defaultStyle, defaultStyleResource);
        getViewTreeObserver().addOnGlobalLayoutListener(
                new LayoutListenerWrapper(this, createGlobalLayoutListener(false)));
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
            this.layout = new PhoneTabSwitcherLayout(TabSwitcher.this, model,
                    new PhoneArithmetics(TabSwitcher.this), style, touchEventDispatcher);
        } else {
            this.layout = new PhoneTabSwitcherLayout(TabSwitcher.this, model,
                    new PhoneArithmetics(TabSwitcher.this), style, touchEventDispatcher);
        }

        this.layout.setCallback(createLayoutCallback());
        this.model.addListener(this.layout);
        this.layout.inflateLayout(inflatedTabsOnly);
        this.touchEventDispatcher.addEventHandler(this.layout.getDragHandler());
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
            int globalTheme = typedArray.getResourceId(R.styleable.TabSwitcher_themeGlobal, 0);
            int phoneTheme = typedArray.getResourceId(R.styleable.TabSwitcher_themePhone, 0);
            int tabletTheme = typedArray.getResourceId(R.styleable.TabSwitcher_themeTablet, 0);
            themeHelper = new ThemeHelper(getContext(), globalTheme, phoneTheme, tabletTheme);
            style = new TabSwitcherStyle(this, model, themeHelper);
            obtainLayoutPolicy(typedArray);
            obtainBackground(typedArray);
            obtainTabIcon(typedArray);
            obtainTabIconTint(typedArray);
            obtainTabBackgroundColor(typedArray);
            obtainTabContentBackgroundColor(typedArray);
            obtainAddTabButtonColor(typedArray);
            obtainTabTitleTextColor(typedArray);
            obtainTabCloseButtonIcon(typedArray);
            obtainTabCloseButtonIconTint(typedArray);
            obtainShowToolbars(typedArray);
            obtainToolbarTitle(typedArray);
            obtainToolbarNavigationIcon(typedArray);
            obtainToolbarNavigationIconTint(typedArray);
            obtainToolbarMenu(typedArray);
            obtainTabPreviewFadeThreshold(typedArray);
            obtainTabPreviewFadeDuration(typedArray);
            obtainEmptyView(typedArray);
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
        int value = typedArray.getInt(R.styleable.TabSwitcher_layoutPolicy, 0);

        if (value == 0) {
            value = themeHelper.getInteger(getLayout(), R.attr.tabSwitcherLayoutPolicy, 0);
        }

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
        Drawable background = typedArray.getDrawable(R.styleable.TabSwitcher_android_background);

        if (background == null) {
            try {
                background = themeHelper.getDrawable(getLayout(), R.attr.tabSwitcherBackground);
            } catch (NotFoundException e) {
                // There's nothing we can do
            }
        }

        if (background != null) {
            ViewUtil.setBackground(this, background);
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
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_tabIcon, 0);

        if (resourceId != 0) {
            setTabIcon(resourceId);
        }
    }

    /**
     * Obtains the color state list, which should be used to tint the icon of a tab, from a specific
     * typed array.
     *
     * @param typedArray
     *         The typed array, the color state list should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabIconTint(@NonNull final TypedArray typedArray) {
        setTabIconTintList(typedArray.getColorStateList(R.styleable.TabSwitcher_tabIconTint));
    }

    /**
     * Obtains the background color of a tab from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the background color should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabBackgroundColor(@NonNull final TypedArray typedArray) {
        setTabBackgroundColor(
                typedArray.getColorStateList(R.styleable.TabSwitcher_tabBackgroundColor));
    }

    /**
     * Obtains the background color of a tab's content from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the background color should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabContentBackgroundColor(@NonNull final TypedArray typedArray) {
        setTabContentBackgroundColor(
                typedArray.getColor(R.styleable.TabSwitcher_tabContentBackgroundColor, -1));
    }

    /**
     * Obtains the background color of the button, which allows to add a new tab, from a specific
     * typed array.
     *
     * @param typedArray
     *         The typed array, the color should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    private void obtainAddTabButtonColor(@NonNull final TypedArray typedArray) {
        setAddTabButtonColor(
                typedArray.getColorStateList(R.styleable.TabSwitcher_addTabButtonColor));
    }

    /**
     * Obtains the text color of a tab's title from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the text color should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabTitleTextColor(@NonNull final TypedArray typedArray) {
        setTabTitleTextColor(
                typedArray.getColorStateList(R.styleable.TabSwitcher_tabTitleTextColor));
    }

    /**
     * Obtains the icon of a tab's close button from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the icon should be obtained from, as an instance of the class {@link
     *         TypedArray}. The typed array may not be null
     */
    private void obtainTabCloseButtonIcon(@NonNull final TypedArray typedArray) {
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_tabCloseButtonIcon, 0);

        if (resourceId != 0) {
            setTabCloseButtonIcon(resourceId);
        }
    }

    /**
     * Obtains the color state list, which should be used to tint the close button of a tab, from a
     * specific typed array.
     *
     * @param typedArray
     *         The typed array, the color state list should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabCloseButtonIconTint(@NonNull final TypedArray typedArray) {
        setTabIconTintList(
                typedArray.getColorStateList(R.styleable.TabSwitcher_tabCloseButtonIconTint));
    }

    /**
     * Obtains, whether the tab switcher's toolbars should be shown, or not, from a specific typed
     * array.
     *
     * @param typedArray
     *         The typed array, it should be obtained from, whether the toolbars should be shown, as
     *         an instance of the class {@link TypedArray}. The typed array may not be null
     */
    private void obtainShowToolbars(@NonNull final TypedArray typedArray) {
        showToolbars(typedArray.getBoolean(R.styleable.TabSwitcher_showToolbars, false));
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
        setToolbarTitle(typedArray.getText(R.styleable.TabSwitcher_toolbarTitle));
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
        int resourceId =
                typedArray.getResourceId(R.styleable.TabSwitcher_toolbarNavigationIcon, -1);
        Drawable navigationIcon = null;

        if (resourceId != -1) {
            navigationIcon = AppCompatResources.getDrawable(getContext(), resourceId);
        }

        setToolbarNavigationIcon(navigationIcon, null);
    }

    /**
     * Obtains the color state list, which should be used to tint the navigation icon of the
     * toolbar, which is shown, when the tab switcher is shown, from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the color state list should be obtained from, as an instance of the
     *         class {@link TypedArray}. The typed array may not be null
     */
    private void obtainToolbarNavigationIconTint(@NonNull final TypedArray typedArray) {
        setToolbarNavigationIconTintList(
                typedArray.getColorStateList(R.styleable.TabSwitcher_toolbarNavigationIconTint));
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
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_toolbarMenu, 0);

        if (resourceId == 0) {
            resourceId = themeHelper.getResourceId(getLayout(), R.attr.tabSwitcherToolbarMenu, 0);
        }

        if (resourceId != 0) {
            inflateToolbarMenu(resourceId, null);
        }
    }

    /**
     * Obtains the duration, which must be reached when loading the preview of tabs to use a fade
     * duration, from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the duration should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabPreviewFadeThreshold(@NonNull final TypedArray typedArray) {
        int threshold = typedArray.getInteger(R.styleable.TabSwitcher_tabPreviewFadeThreshold, -1);

        if (threshold == -1) {
            threshold = themeHelper
                    .getInteger(getLayout(), R.attr.tabSwitcherTabToolbarPreviewFadeThreshold, -1);
        }

        if (threshold != -1) {
            setTabPreviewFadeThreshold(threshold);
        }
    }

    /**
     * Sets the duration of the fade animation, which is used to show the previews of tabs.
     *
     * @param typedArray
     *         The typed array, the duration should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    private void obtainTabPreviewFadeDuration(@NonNull final TypedArray typedArray) {
        int duration = typedArray.getInteger(R.styleable.TabSwitcher_tabPreviewFadeDuration, -1);

        if (duration == -1) {
            duration = themeHelper
                    .getInteger(getLayout(), R.attr.tabSwitcherTabToolbarPreviewFadeDuration, -1);
        }

        if (duration != -1) {
            setTabPreviewFadeDuration(duration);
        }
    }

    /**
     * Obtains the view, which should be shown, when the tab switcher is empty, from a specific
     * typed array.
     *
     * @param typedArray
     *         The typed array, trhe view should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    private void obtainEmptyView(@NonNull final TypedArray typedArray) {
        int resourceId = typedArray.getResourceId(R.styleable.TabSwitcher_emptyView, 0);

        if (resourceId == 0) {
            resourceId = themeHelper.getResourceId(getLayout(), R.attr.tabSwitcherEmptyView, 0);
        }

        if (resourceId != 0) {
            long animationDuration =
                    typedArray.getInteger(R.styleable.TabSwitcher_emptyViewAnimationDuration, -2);

            if (animationDuration < -1) {
                animationDuration = themeHelper
                        .getInteger(getLayout(), R.attr.tabSwitcherEmptyViewAnimationDuration, -1);
            }

            setEmptyView(resourceId, animationDuration);
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
        Condition.INSTANCE.ensureNotNull(action, "The action may not be null");
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
                                   final boolean selectionChanged,
                                   final boolean switcherVisibilityChanged,
                                   @NonNull final Animation animation) {
                notifyOnTabAdded(index, tab, animation);

                if (selectionChanged) {
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
                                       final int selectedTabIndex, final boolean selectionChanged,
                                       @NonNull final Animation animation) {
                for (Tab tab : tabs) {
                    notifyOnTabAdded(index, tab, animation);
                }

                if (selectionChanged) {
                    notifyOnSelectionChanged(selectedTabIndex,
                            selectedTabIndex != -1 ? getTab(selectedTabIndex) : null);
                }
            }

            @Override
            public void onTabRemoved(final int index, @NonNull final Tab tab,
                                     final int previousSelectedTabIndex, final int selectedTabIndex,
                                     final boolean selectionChanged,
                                     @NonNull final Animation animation) {
                notifyOnTabRemoved(index, tab, animation);

                if (selectionChanged) {
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
            public void onApplyPaddingToTabsChanged(final boolean applyPaddingToTabs) {

            }

            @Override
            public void onTabIconChanged(@Nullable final Drawable icon) {

            }

            @Override
            public void onTabBackgroundColorChanged(@Nullable final ColorStateList colorStateList) {

            }

            @Override
            public void onTabContentBackgroundColorChanged(@ColorInt final int color) {

            }

            @Override
            public void onTabTitleColorChanged(@Nullable final ColorStateList colorStateList) {

            }

            @Override
            public void onTabCloseButtonIconChanged(@Nullable final Drawable icon) {

            }

            @Override
            public void onTabProgressBarColorChanged(@ColorInt final int color) {

            }

            @Override
            public void onAddTabButtonVisibilityChanged(final boolean visible) {

            }

            @Override
            public void onAddTabButtonColorChanged(@Nullable final ColorStateList colorStateList) {

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

            @Override
            public void onEmptyViewChanged(@Nullable final View view,
                                           final long animationDuration) {

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
                Condition.INSTANCE.ensureNotNull(getDecorator(), "No decorator has been set",
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
     * Setups the tab switcher to be associated with those menu items of its own toolbar menu, which
     * use a {@link TabSwitcherButton} as their action view. The icon of such menu items will
     * automatically be updated, when the number of tabs, which are contained by the tab switcher,
     * changes.
     * <p>
     * Calling this method is basically the same as calling <code>setupWithMenu(tabSwitcher,
     * tabSwitcher.getToolbarMenu(), listener)</code>. However, if the {@link Menu}, which is
     * returned by <code>tabSwitcher.getToolbarMenu()</code> is null, a {@link
     * OnGlobalLayoutListener} is registered at the given tab switcher to setup the tab switcher as
     * soon as the menu is initialized.
     *
     * @param tabSwitcher
     *         The tab switcher, which should become associated with the menu items, as an instance
     *         of the class {@link TabSwitcher}. The tab switcher may not be null
     * @param listener
     *         The listener, which should be set to the menu items, which use a {@link
     *         TabSwitcherButton} as their action view, as an instance of the type {@link
     *         OnClickListener} or null, if no listener should be set
     */
    public static void setupWithMenu(@NonNull final TabSwitcher tabSwitcher,
                                     @Nullable final OnClickListener listener) {
        Condition.INSTANCE.ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        Menu menu = tabSwitcher.getToolbarMenu();

        if (menu != null) {
            setupWithMenu(tabSwitcher, menu, listener);
        } else {
            tabSwitcher.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

                        @Override
                        public void onGlobalLayout() {
                            ViewUtil.removeOnGlobalLayoutListener(tabSwitcher.getViewTreeObserver(),
                                    this);
                            Menu menu = tabSwitcher.getToolbarMenu();

                            if (menu != null) {
                                TabSwitcher.setupWithMenu(tabSwitcher, menu, listener);
                            }
                        }

                    });
        }
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
        Condition.INSTANCE.ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        Condition.INSTANCE.ensureNotNull(menu, "The menu may not be null");

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
        Condition.INSTANCE.ensureNotNull(listener, "The listener may not be null");
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
        Condition.INSTANCE.ensureNotNull(listener, "The listener may not be null");
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
     * <p>
     * Changing the layout policy after the view has been laid out does not have any effect.
     *
     * @param layoutPolicy
     *         The layout policy, which should be set, as a value of the enum {@link LayoutPolicy}.
     *         The layout policy may either be {@link LayoutPolicy#AUTO}, {@link LayoutPolicy#PHONE}
     *         or {@link LayoutPolicy#TABLET}
     */
    public final void setLayoutPolicy(@NonNull final LayoutPolicy layoutPolicy) {
        Condition.INSTANCE.ensureNotNull(layoutPolicy, "The layout policy may not be null");

        if (this.layoutPolicy != layoutPolicy) {
            Layout previousLayout = getLayout();
            this.layoutPolicy = layoutPolicy;

            if (layout != null) {
                Layout newLayout = getLayout();

                if (previousLayout != newLayout) {
                    layout.detachLayout(false);
                    model.removeListener(layout);
                    touchEventDispatcher.removeEventHandler(layout.getDragHandler());
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
        // TODO: Return Layout.TABLET once supported
        /*
        if (layoutPolicy == LayoutPolicy.TABLET || (layoutPolicy == LayoutPolicy.AUTO &&
                getDeviceType(getContext()) == DeviceType.TABLET)) {
            return Layout.TABLET;
        } else {
        */
        return getOrientation(getContext()) == Orientation.LANDSCAPE ? Layout.PHONE_LANDSCAPE :
                Layout.PHONE_PORTRAIT;
        //}
    }

    /**
     * Adds a specific drag gesture to the tab switcher.
     *
     * @param dragGesture
     *         The drag gesture, which should be added, as an instance of the class {@link
     *         DragGesture}. The drag gesture may not be null
     */
    public final void addDragGesture(@NonNull final DragGesture dragGesture) {
        Condition.INSTANCE.ensureNotNull(dragGesture, "The drag gesture may not be null");
        AbstractTouchEventHandler eventHandler =
                new DragGestureEventHandlerFactory(this).fromGesture(dragGesture);
        touchEventDispatcher.addEventHandler(eventHandler);
    }

    /**
     * Removes a specific drag gesture from the tab switcher.
     *
     * @param dragGesture
     *         The drag gesture, which should be removed, as an instance of the class {@link
     *         DragGesture}. The drag gesture may not be null
     */
    public final void removeDragGesture(@NonNull final DragGesture dragGesture) {
        Condition.INSTANCE.ensureNotNull(dragGesture, "The drag gesture may not be null");
        AbstractTouchEventHandler eventHandler =
                new DragGestureEventHandlerFactory(this).fromGesture(dragGesture);
        touchEventDispatcher.removeEventHandler(eventHandler);
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
        ContentRecyclerAdapter contentRecyclerAdapter = model.getContentRecyclerAdapter();

        if (contentRecyclerAdapter != null) {
            contentRecyclerAdapter.clearSavedState(tab);
        }
    }

    /**
     * Clears the saved states of all tabs.
     */
    public void clearAllSavedStates() {
        ContentRecyclerAdapter contentRecyclerAdapter = model.getContentRecyclerAdapter();

        if (contentRecyclerAdapter != null) {
            contentRecyclerAdapter.clearAllSavedStates();
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

    /**
     * Notifies the tab switcher that a specific tab has changed. This will cause the content of the
     * tab to be updated by utilizing the tab switcher's adapter.
     *
     * @param tab
     *         The tab, which has changed, as an instance of the class {@link Tab}. The tab may not
     *         be null
     */
    public final void notifyTabChanged(@NonNull final Tab tab) {
        Condition.INSTANCE.ensureNotNull(tab, "The tab may not be null");

        if (layout != null) {
            AbstractViewRecycler<Tab, Void> contentViewRecycler = layout.getContentViewRecycler();

            if (contentViewRecycler != null) {
                contentViewRecycler.notifyItemChanged(tab);
            }
        }
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

    @Override
    public final void selectTab(final int index) {
        model.selectTab(index);
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
        // The model might be null on old API levels, where the super constructor implicitly invokes
        // the setPadding-method. We can simply ignore such method call and will set the correct
        // padding later on
        if (model != null) {
            model.setPadding(left, top, right, bottom);
        }
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

    @Override
    public final void applyPaddingToTabs(final boolean applyPaddingToTabs) {
        model.applyPaddingToTabs(applyPaddingToTabs);
    }

    @Override
    public final boolean isPaddingAppliedToTabs() {
        return model.isPaddingAppliedToTabs();
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

    @Override
    public final ColorStateList getTabIconTintList() {
        return model.getTabIconTintList();
    }

    @Override
    public final void setTabIconTint(@ColorInt final int color) {
        model.setTabIconTint(color);
    }

    @Override
    public final void setTabIconTintList(@Nullable final ColorStateList tintList) {
        model.setTabIconTintList(tintList);
    }

    @Override
    public final PorterDuff.Mode getTabIconTintMode() {
        return model.getTabIconTintMode();
    }

    @Override
    public final void setTabIconTintMode(@Nullable final PorterDuff.Mode mode) {
        model.setTabIconTintMode(mode);
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

    @ColorInt
    @Override
    public final int getTabContentBackgroundColor() {
        return model.getTabContentBackgroundColor();
    }

    @Override
    public final void setTabContentBackgroundColor(@ColorInt final int color) {
        model.setTabContentBackgroundColor(color);
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
    public final int getTabProgressBarColor() {
        return model.getTabProgressBarColor();
    }

    @Override
    public final void setTabProgressBarColor(@ColorInt final int color) {
        model.setTabProgressBarColor(color);
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
    public final ColorStateList getTabCloseButtonIconTintList() {
        return model.getTabCloseButtonIconTintList();
    }

    @Override
    public final void setTabCloseButtonIconTint(@ColorInt final int color) {
        model.setTabCloseButtonIconTint(color);
    }

    @Override
    public final void setTabCloseButtonIconTintList(@Nullable final ColorStateList tintList) {
        model.setTabCloseButtonIconTintList(tintList);
    }

    @Override
    public final PorterDuff.Mode getTabCloseButtonIconTintMode() {
        return model.getTabCloseButtonIconTintMode();
    }

    @Override
    public final void setTabCloseButtonIconTintMode(@Nullable final PorterDuff.Mode mode) {
        model.setTabCloseButtonIconTintMode(mode);
    }

    @Override
    public final boolean isAddTabButtonShown() {
        return model.isAddTabButtonShown();
    }

    @Override
    public final void showAddTabButton(@Nullable final AddTabButtonListener listener) {
        model.showAddTabButton(listener);
    }

    @Nullable
    @Override
    public final ColorStateList getAddTabButtonColor() {
        return model.getAddTabButtonColor();
    }

    @Override
    public final void setAddTabButtonColor(@ColorInt final int color) {
        model.setAddTabButtonColor(color);
    }

    @Override
    public final void setAddTabButtonColor(@Nullable final ColorStateList colorStateList) {
        model.setAddTabButtonColor(colorStateList);
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
        return toolbars != null ? toolbars[PRIMARY_TOOLBAR_INDEX].getTitle() :
                model.getToolbarTitle();
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
        return toolbars != null ? toolbars[PRIMARY_TOOLBAR_INDEX].getNavigationIcon() :
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
    public final ColorStateList getToolbarNavigationIconTintList() {
        return model.getToolbarNavigationIconTintList();
    }

    @Override
    public final void setToolbarNavigationIconTint(@ColorInt final int color) {
        model.setToolbarNavigationIconTint(color);
    }

    @Override
    public final void setToolbarNavigationIconTintList(@Nullable final ColorStateList tintList) {
        model.setToolbarNavigationIconTintList(tintList);
    }

    @Override
    public final PorterDuff.Mode getToolbarNavigationIconTintMode() {
        return model.getToolbarNavigationIconTintMode();
    }

    @Override
    public final void setToolbarNavigationIconTintMode(@Nullable final PorterDuff.Mode mode) {
        model.setToolbarNavigationIconTintMode(mode);
    }

    @Override
    public final void inflateToolbarMenu(@MenuRes final int resourceId,
                                         @Nullable final OnMenuItemClickListener listener) {
        model.inflateToolbarMenu(resourceId, listener);
    }

    @Override
    public final long getTabPreviewFadeThreshold() {
        return model.getTabPreviewFadeThreshold();
    }

    @Override
    public final void setTabPreviewFadeThreshold(final long threshold) {
        model.setTabPreviewFadeThreshold(threshold);
    }

    @Override
    public final long getTabPreviewFadeDuration() {
        return model.getTabPreviewFadeDuration();
    }

    @Override
    public final void setTabPreviewFadeDuration(final long duration) {
        model.setTabPreviewFadeDuration(duration);
    }

    @Nullable
    @Override
    public final View getEmptyView() {
        return model.getEmptyView();
    }

    @Override
    public void setEmptyView(@Nullable final View view) {
        model.setEmptyView(view);
    }

    @Override
    public final void setEmptyView(@Nullable final View view, final long animationDuration) {
        model.setEmptyView(view, animationDuration);
    }

    @Override
    public final void setEmptyView(@LayoutRes final int resourceId) {
        model.setEmptyView(resourceId);
    }

    @Override
    public final void setEmptyView(@LayoutRes final int resourceId, final long animationDuration) {
        model.setEmptyView(resourceId);
    }

    @Override
    public final boolean areSavedStatesClearedWhenRemovingTabs() {
        return model.areSavedStatesClearedWhenRemovingTabs();
    }

    @Override
    public final void clearSavedStatesWhenRemovingTabs(final boolean clear) {
        model.clearSavedStatesWhenRemovingTabs(clear);
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
        return touchEventDispatcher.handleTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public final Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        if (layout != null) {
            TabSwitcherState savedState = new TabSwitcherState(superState);
            savedState.layoutPolicy = layoutPolicy;
            savedState.modelState = new Bundle();
            Pair<Integer, Float> pair = layout.detachLayout(true);

            if (pair != null) {
                savedState.modelState
                        .putInt(TabSwitcherModel.REFERENCE_TAB_INDEX_EXTRA, pair.first);
                savedState.modelState
                        .putFloat(TabSwitcherModel.REFERENCE_TAB_POSITION_EXTRA, pair.second);
                model.setReferenceTabIndex(pair.first);
                model.setReferenceTabPosition(pair.second);
            } else {
                model.setReferenceTabPosition(-1);
                model.setReferenceTabIndex(-1);
            }

            model.removeListener(layout);
            layout = null;
            executePendingAction();
            getViewTreeObserver().addOnGlobalLayoutListener(
                    new LayoutListenerWrapper(this, createGlobalLayoutListener(true)));
            model.saveInstanceState(savedState.modelState);
            return savedState;
        }

        return superState;
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