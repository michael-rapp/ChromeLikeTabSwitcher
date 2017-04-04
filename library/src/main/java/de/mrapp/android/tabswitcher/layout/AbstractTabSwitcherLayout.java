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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.util.ViewUtil;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An abstract base class for all layouts, which implement the functionality of a {@link
 * TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class AbstractTabSwitcherLayout
        implements TabSwitcherLayout, OnGlobalLayoutListener, Model.Listener {

    /**
     * Defines the interface, a class, which should be notified about the events of a tab switcher
     * layout, must implement.
     */
    public interface Callback {

        /*
         * The method, which is invoked, when all animations have been ended.
         */
        void onAnimationsEnded();

    }

    /**
     * A layout listener, which unregisters itself from the observed view, when invoked. The
     * listener allows to encapsulate another listener, which is notified, when the listener is
     * invoked.
     */
    public static class LayoutListenerWrapper implements OnGlobalLayoutListener {

        /**
         * The observed view.
         */
        private final View view;

        /**
         * The encapsulated listener.
         */
        private final OnGlobalLayoutListener listener;

        /**
         * Creates a new layout listener, which unregisters itself from the observed view, when
         * invoked.
         *
         * @param view
         *         The observed view as an instance of the class {@link View}. The view may not be
         *         null
         * @param listener
         *         The listener, which should be encapsulated, as an instance of the type {@link
         *         OnGlobalLayoutListener} or null, if no listener should be encapsulated
         */
        public LayoutListenerWrapper(@NonNull final View view,
                                     @Nullable final OnGlobalLayoutListener listener) {
            ensureNotNull(view, "The view may not be null");
            this.view = view;
            this.listener = listener;
        }

        @Override
        public void onGlobalLayout() {
            ViewUtil.removeOnGlobalLayoutListener(view.getViewTreeObserver(), this);

            if (listener != null) {
                listener.onGlobalLayout();
            }
        }

    }

    /**
     * A animation listener, which increases the number of running animations, when the observed
     * animation is started, and decreases the number of accordingly, when the animation is
     * finished. The listener allows to encapsulate another animation listener, which is notified
     * when the animation has been started, canceled or ended.
     */
    protected class AnimationListenerWrapper extends AnimatorListenerAdapter {

        /**
         * The encapsulated listener.
         */
        private final AnimatorListener listener;

        /**
         * Decreases the number of running animations and executes the next pending action, if no
         * running animations remain.
         */
        private void endAnimation() {
            if (--runningAnimations == 0) {
                notifyOnAnimationsEnded();
            }
        }

        /**
         * Creates a new animation listener, which increases the number of running animations, when
         * the observed animation is started, and decreases the number of accordingly, when the
         * animation is finished.
         *
         * @param listener
         *         The listener, which should be encapsulated, as an instance of the type {@link
         *         AnimatorListener} or null, if no listener should be encapsulated
         */
        AnimationListenerWrapper(@Nullable final AnimatorListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAnimationStart(final Animator animation) {
            super.onAnimationStart(animation);
            runningAnimations++;

            if (listener != null) {
                listener.onAnimationStart(animation);
            }
        }

        @Override
        public void onAnimationEnd(final Animator animation) {
            super.onAnimationEnd(animation);

            if (listener != null) {
                listener.onAnimationEnd(animation);
            }

            endAnimation();
        }

        @Override
        public void onAnimationCancel(final Animator animation) {
            super.onAnimationCancel(animation);

            if (listener != null) {
                listener.onAnimationCancel(animation);
            }

            endAnimation();
        }

    }

    /**
     * The tab switcher, the layout belongs to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The model of the tab switcher, the layout belongs to.
     */
    private final Model model;

    /**
     * The callback, which is notified about the layout's events.
     */
    private Callback callback;

    /**
     * The number of animations, which are currently running.
     */
    private int runningAnimations;

    /**
     * The decorator, which allows to inflate the views, which correspond to the tab switcher's
     * tabs.
     */
    private TabSwitcherDecorator decorator;

    /**
     * An array, which contains the left, top, right and bottom padding of the tab switcher.
     */
    private int[] padding;

    /**
     * The resource id of a tab's icon.
     */
    private int tabIconId;

    /**
     * The bitmap of a tab's icon.
     */
    private Bitmap tabIconBitmap;

    /**
     * The background color of a tab;
     */
    private int tabBackgroundColor;

    /**
     * The text color of a tab's title.
     */
    private int tabTitleTextColor;

    /**
     * The resource id of the icon of a tab's close button.
     */
    private int tabCloseButtonIconId;

    /**
     * The bitmap of the icon of a tab's close button.
     */
    private Bitmap tabCloseButtonIconBitmap;

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
     * Notifies the callback, that all animations have been ended.
     */
    protected final void notifyOnAnimationsEnded() {
        if (callback != null) {
            callback.onAnimationsEnded();
        }
    }

    /**
     * Returns the tab switcher, the layout belongs to.
     *
     * @return The tab switcher, the layout belongs to, as an instance of the class {@link
     * TabSwitcher}. The tab switcher may not be null
     */
    @NonNull
    protected final TabSwitcher getTabSwitcher() {
        return tabSwitcher;
    }

    /**
     * Returns the model of the tab switcher, the layout belongs to.
     *
     * @return The model of the tab switcher, the layout belongs to, as an instance of the type
     * {@link Model}. The model may not be null
     */
    @NonNull
    protected final Model getModel() {
        return model;
    }

    /**
     * Returns the context, which is used by the layout.
     *
     * @return The context, which is used by the layout, as an instance of the class {@link
     * Context}. The context may not be null
     */
    @NonNull
    protected final Context getContext() {
        return tabSwitcher.getContext();
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
     * The method, which is invoked on implementing subclasses, when the icon of a tab has been
     * changed.
     *
     * @param icon
     *         The icon, which has been set, as an instance of the class {@link Drawable} or null,
     *         if no icon has been set
     */
    protected abstract void onTabIconChanged(@Nullable final Drawable icon);

    /**
     * The method, which is invoked on implementing subclasses, when the background color of a tab
     * has been changed.
     *
     * @param color
     *         The color, which has been set, as an {@link Integer} value
     */
    protected abstract void onTabBackgroundColorChanged(@ColorInt final int color);

    /**
     * The method, which is invoked on implementing subclasses, when the text color of a tab's title
     * has been changed.
     *
     * @param color
     *         The color, which has been set, as an {@link Integer} value
     */
    protected abstract void onTabTitleColorChanged(@ColorInt final int color);

    /**
     * The method, which is invoked on implementing subclasses, when the icon of a tab's close
     * button has been changed.
     *
     * @param icon
     *         The icon, which has been set, as an instance of the class {@link Drawable}. The icon
     *         may not be null
     */
    protected abstract void onTabCloseButtonIconChanged(@NonNull final Drawable icon);

    /**
     * Creates a new layout, which implements the functionality of a {@link TabSwitcher}.
     *
     * @param tabSwitcher
     *         The tab switcher, the layout belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param model
     *         The model of the tab switcher, the layout belongs to, as an instance of the type
     *         {@link Model}. The model may not be null
     */
    public AbstractTabSwitcherLayout(@NonNull final TabSwitcher tabSwitcher,
                                     @NonNull final Model model) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(model, "The model may not be null");
        this.tabSwitcher = tabSwitcher;
        this.model = model;
        this.callback = null;
        this.runningAnimations = 0;
        this.decorator = null;
        this.padding = new int[]{0, 0, 0, 0};
    }

    /**
     * Inflates the layout.
     */
    public abstract void inflateLayout();

    /**
     * Handles a touch event.
     *
     * @param event
     *         The touch event as an instance of the class {@link MotionEvent}. The touch event may
     *         not be null
     * @return True, if the event has been handled, false otherwise
     */
    public abstract boolean handleTouchEvent(@NonNull final MotionEvent event);

    /**
     * Obtains all of the layout's attributes from a specific typed array.
     *
     * @param typedArray
     *         The typed array, the attributes should be obtained from, as an instance of the class
     *         {@link TypedArray}. The typed array may not be null
     */
    @CallSuper
    public void obtainStyledAttributes(@NonNull final TypedArray typedArray) {
        obtainToolbarTitle(typedArray);
        obtainToolbarMenu(typedArray);
        obtainToolbarNavigationIcon(typedArray);
    }

    /**
     * Sets the callback, which should be notified about the layout's events.
     *
     * @param callback
     *         The callback, which should be set, as an instance of the type {@link Callback} or
     *         null, if no callback should be notified
     */
    public final void setCallback(@Nullable final Callback callback) {
        this.callback = callback;
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

    @CallSuper
    @Override
    public boolean isAnimationRunning() {
        return runningAnimations > 0;
    }

    @Override
    public final void showToolbars(final boolean show) {
        for (Toolbar toolbar : getToolbars()) {
            toolbar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public final boolean areToolbarsShown() {
        return getToolbars()[0].getVisibility() == View.VISIBLE;
    }

    @Override
    public final void setToolbarTitle(@Nullable final CharSequence title) {
        getToolbars()[0].setTitle(title);
    }

    @Override
    public final void setToolbarTitle(@StringRes final int resourceId) {
        setToolbarTitle(getContext().getText(resourceId));
    }

    @Override
    public final void inflateToolbarMenu(@MenuRes final int resourceId,
                                         @Nullable final OnMenuItemClickListener listener) {
        Toolbar[] toolbars = getToolbars();
        Toolbar toolbar = toolbars.length > 1 ? toolbars[1] : toolbars[0];
        toolbar.inflateMenu(resourceId);
        toolbar.setOnMenuItemClickListener(listener);
    }

    @NonNull
    @Override
    public final Menu getToolbarMenu() {
        Toolbar[] toolbars = getToolbars();
        Toolbar toolbar = toolbars.length > 1 ? toolbars[1] : toolbars[0];
        return toolbar.getMenu();
    }

    @Override
    public final void setToolbarNavigationIcon(@Nullable final Drawable icon,
                                               @Nullable final OnClickListener listener) {
        Toolbar toolbar = getToolbars()[0];
        toolbar.setNavigationIcon(icon);
        toolbar.setNavigationOnClickListener(listener);
    }

    @Override
    public final void setToolbarNavigationIcon(@DrawableRes final int resourceId,
                                               @Nullable final OnClickListener listener) {
        setToolbarNavigationIcon(ContextCompat.getDrawable(getContext(), resourceId), listener);
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

    @Nullable
    @Override
    public final Drawable getTabIcon() {
        if (tabIconId != -1) {
            return ContextCompat.getDrawable(getContext(), tabIconId);
        } else {
            return tabIconBitmap != null ?
                    new BitmapDrawable(getContext().getResources(), tabIconBitmap) : null;
        }
    }

    @Override
    public final void setTabIcon(@DrawableRes final int resourceId) {
        this.tabIconId = resourceId;
        this.tabIconBitmap = null;
        onTabIconChanged(getTabIcon());
    }

    @Override
    public final void setTabIcon(@Nullable final Bitmap icon) {
        this.tabIconId = -1;
        this.tabIconBitmap = icon;
        onTabIconChanged(getTabIcon());
    }

    @ColorInt
    @Override
    public final int getTabBackgroundColor() {
        return tabBackgroundColor;
    }

    @Override
    public final void setTabBackgroundColor(@ColorInt final int color) {
        this.tabBackgroundColor = color;
        onTabBackgroundColorChanged(color);
    }

    @Override
    public final int getTabTitleTextColor() {
        return tabTitleTextColor;
    }

    @Override
    public final void setTabTitleTextColor(@ColorInt final int color) {
        this.tabTitleTextColor = color;
        onTabTitleColorChanged(color);
    }

    @NonNull
    @Override
    public final Drawable getTabCloseButtonIcon() {
        if (tabCloseButtonIconId != -1) {
            return ContextCompat.getDrawable(getContext(), tabCloseButtonIconId);
        } else {
            return new BitmapDrawable(getContext().getResources(), tabCloseButtonIconBitmap);
        }
    }

    @Override
    public final void setTabCloseButtonIcon(@DrawableRes final int resourceId) {
        tabCloseButtonIconId = resourceId;
        tabCloseButtonIconBitmap = null;
        onTabCloseButtonIconChanged(getTabCloseButtonIcon());
    }

    @Override
    public final void setTabCloseButtonIcon(@NonNull final Bitmap icon) {
        tabCloseButtonIconId = -1;
        tabCloseButtonIconBitmap = icon;
        onTabCloseButtonIconChanged(getTabCloseButtonIcon());
    }

}