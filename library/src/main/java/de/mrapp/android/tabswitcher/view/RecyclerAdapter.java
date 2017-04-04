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
package de.mrapp.android.tabswitcher.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedHashSet;
import java.util.Set;

import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabCloseListener;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.ViewUtil;
import de.mrapp.android.util.view.AbstractViewRecycler;
import de.mrapp.android.util.view.AttachedViewRecycler;
import de.mrapp.android.util.view.ViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An adapter, which allows to inflate the views, which are used to visualize the tabs of a {@link
 * TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class RecyclerAdapter extends AbstractViewRecycler.Adapter<TabItem, Integer>
        implements Tab.Callback {

    /**
     * The tab switcher, the tabs belong to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The view recycler, which allows to inflate the child views of tabs.
     */
    private final ViewRecycler<Tab, Void> childViewRecycler;

    /**
     * The data binder, which allows to render previews of tabs.
     */
    private final PreviewDataBinder dataBinder;

    /**
     * A set, which contains the listeners, which should be notified, when a tab is about to be
     * closed by clicking its close button.
     */
    private final Set<TabCloseListener> tabCloseListeners;

    /**
     * The inset of tabs in pixels.
     */
    private final int tabInset;

    /**
     * The width of the border, which is shown around the preview of tabs, in pixels.
     */
    private final int tabBorderWidth;

    /**
     * The height of the view group, which contains a tab's title and close button, in pixels.
     */
    private final int tabTitleContainerHeight;

    /**
     * The view recycler, the adapter is bound to.
     */
    private AttachedViewRecycler<TabItem, Integer> viewRecycler;

    /**
     * Inflates the child view of a tab and adds it to the view hierarchy.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose child view should be inflated, as
     *         an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void addChildView(@NonNull final TabItem tabItem) {
        TabViewHolder viewHolder = tabItem.getViewHolder();
        View view = viewHolder.child;
        Tab tab = tabItem.getTab();

        if (view == null) {
            ViewGroup parent = viewHolder.childContainer;
            Pair<View, ?> pair = childViewRecycler.inflate(tab, parent);
            view = pair.first;
            LayoutParams layoutParams =
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            layoutParams.setMargins(tabSwitcher.getPaddingLeft(), tabSwitcher.getPaddingTop(),
                    tabSwitcher.getPaddingRight(), tabSwitcher.getPaddingBottom());
            parent.addView(view, 0, layoutParams);
            viewHolder.child = view;
        } else {
            childViewRecycler.getAdapter().onShowView(tabSwitcher.getContext(), view, tab, false);
        }

        viewHolder.previewImageView.setVisibility(View.GONE);
        viewHolder.previewImageView.setImageBitmap(null);
        viewHolder.borderView.setVisibility(View.GONE);
    }

    /**
     * Renders and displays the child view of a tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose preview should be rendered, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     */
    private void renderChildView(@NonNull final TabItem tabItem) {
        Tab tab = tabItem.getTab();
        TabViewHolder viewHolder = tabItem.getViewHolder();
        viewHolder.borderView.setVisibility(View.VISIBLE);

        if (viewHolder.child != null) {
            childViewRecycler.getAdapter().onRemoveView(viewHolder.child, tab);
            dataBinder.load(tab, viewHolder.previewImageView, false, tabItem);
            removeChildView(viewHolder, tab);
        } else {
            dataBinder.load(tab, viewHolder.previewImageView, tabItem);
        }
    }

    /**
     * Removes the child of a tab from its parent.
     *
     * @param viewHolder
     *         The view holder, which stores references to the tab's views, as an instance of the
     *         class {@link TabViewHolder}. The view holder may not be null
     * @param tab
     *         The tab, whose child should be removed, as an instance of the class {@link Tab}. The
     *         tab may not be null
     */
    private void removeChildView(@NonNull final TabViewHolder viewHolder, @NonNull final Tab tab) {
        if (viewHolder.childContainer.getChildCount() > 2) {
            viewHolder.childContainer.removeViewAt(0);
        }

        viewHolder.child = null;
        childViewRecycler.remove(tab);
    }

    /**
     * Adapts the title of a tab.
     *
     * @param viewHolder
     *         The view holder, which stores references to the tab's views, as an instance of the
     *         class {@link TabViewHolder}. The view holder may not be null
     * @param tab
     *         The tab, whose title should be adapted, as an instance of the class {@link Tab}. The
     *         tab may not be null
     */
    private void adaptTitle(@NonNull final TabViewHolder viewHolder, @NonNull final Tab tab) {
        viewHolder.titleTextView.setText(tab.getTitle());
    }

    /**
     * Adapts the icon of a tab.
     *
     * @param viewHolder
     *         The view holder, which stores references to the tab's views, as an instance of the
     *         class {@link TabViewHolder}. The view holder may not be null
     * @param tab
     *         The icon, whose icon should be adapted, as an instance of the class {@link Tab}. The
     *         tab may not be null
     */
    private void adaptIcon(@NonNull final TabViewHolder viewHolder, @NonNull final Tab tab) {
        Drawable icon = tab.getIcon(tabSwitcher.getContext());
        viewHolder.titleTextView.setCompoundDrawablesWithIntrinsicBounds(
                icon != null ? icon : tabSwitcher.getTabIcon(), null, null, null);
    }

    /**
     * Adapts the visibility of a tab's close button.
     *
     * @param viewHolder
     *         The view holder, which stores references to the tab's views, as an instance of the
     *         class {@link TabViewHolder}. The view holder may not be null
     * @param tab
     *         The icon, whose close button should be adapted, as an instance of the class {@link
     *         Tab}. The tab may not be null
     */
    private void adaptCloseButton(@NonNull final TabViewHolder viewHolder, @NonNull final Tab tab) {
        viewHolder.closeButton.setVisibility(tab.isCloseable() ? View.VISIBLE : View.GONE);
        viewHolder.closeButton.setOnClickListener(
                tab.isCloseable() ? createCloseButtonClickListener(viewHolder.closeButton, tab) :
                        null);
    }

    /**
     * Adapts the icon of a tab's close button.
     *
     * @param viewHolder
     *         The view holder, which stores references to the tab's views, as an instance of the
     *         class {@link TabViewHolder}. The view holder may not be null
     * @param tab
     *         The icon, whose icon hould be adapted, as an instance of the class {@link Tab}. The
     *         tab may not be null
     */
    private void adaptCloseButtonIcon(@NonNull final TabViewHolder viewHolder,
                                      @NonNull final Tab tab) {
        Drawable icon = tab.getCloseButtonIcon(tabSwitcher.getContext());
        viewHolder.closeButton
                .setImageDrawable(icon != null ? icon : tabSwitcher.getTabCloseButtonIcon());
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

        for (TabCloseListener listener : tabCloseListeners) {
            result &= listener.onCloseTab(tabSwitcher, tab);
        }

        return result;
    }

    /**
     * Adapts the background color of a tab.
     *
     * @param view
     *         The view, which is used to visualize the tab, as an instance of the class {@link
     *         View}. The view may not be null
     * @param viewHolder
     *         The view holder, which stores references to the tab's views, as an instance of the
     *         class {@link TabViewHolder}. The view holder may not be null
     * @param tab
     *         The tab, whose background color should be adapted, as an instance of the class {@link
     *         Tab}. The tab may not be null
     */
    private void adaptBackgroundColor(@NonNull final View view,
                                      @NonNull final TabViewHolder viewHolder,
                                      @NonNull final Tab tab) {
        int color = tab.getBackgroundColor();
        Drawable background = view.getBackground();
        background.setColorFilter(color != -1 ? color : tabSwitcher.getTabBackgroundColor(),
                PorterDuff.Mode.MULTIPLY);
        Drawable border = viewHolder.borderView.getBackground();
        border.setColorFilter(color != -1 ? color : tabSwitcher.getTabBackgroundColor(),
                PorterDuff.Mode.MULTIPLY);
    }

    /**
     * Adapts the text color of a tab's title.
     *
     * @param viewHolder
     *         The view holder, which stores references to the tab's views, as an instance of the
     *         class {@link TabViewHolder}. The view holder may not be null
     * @param tab
     *         The tab, whose text color should be adapted, as an instance of the class {@link Tab}.
     *         The tab may not be null
     */
    private void adaptTitleTextColor(@NonNull final TabViewHolder viewHolder,
                                     @NonNull final Tab tab) {
        int color = tab.getTitleTextColor();
        viewHolder.titleTextView
                .setTextColor(color != -1 ? color : tabSwitcher.getTabTitleTextColor());
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
    private TabItem getTabItem(@NonNull final Tab tab) {
        ensureNotNull(viewRecycler, "No view recycler has been set", IllegalStateException.class);
        int index = tabSwitcher.indexOf(tab);

        if (index != -1) {
            TabItem tabItem = TabItem.create(tabSwitcher, viewRecycler, index);

            if (tabItem.isInflated()) {
                return tabItem;
            }
        }

        return null;
    }

    /**
     * Creates a new adapter, which allows to inflate the views, which are used to visualize the
     * tabs of a {@link TabSwitcher}.
     */
    public RecyclerAdapter(@NonNull final TabSwitcher tabSwitcher,
                           @NonNull final ViewRecycler<Tab, Void> childViewRecycler) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(childViewRecycler, "The child view recycler may not be null");
        this.tabSwitcher = tabSwitcher;
        this.childViewRecycler = childViewRecycler;
        this.dataBinder = new PreviewDataBinder(tabSwitcher, childViewRecycler);
        this.tabCloseListeners = new LinkedHashSet<>();
        Resources resources = tabSwitcher.getResources();
        this.tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        this.tabBorderWidth = resources.getDimensionPixelSize(R.dimen.tab_border_width);
        this.tabTitleContainerHeight =
                resources.getDimensionPixelSize(R.dimen.tab_title_container_height);
        this.viewRecycler = null;
    }

    /**
     * Adds a new listener, which should be notified, when a tab is about to be closed by clicking
     * its close button.
     *
     * @param listener
     *         The listener, which should be added, as an instance of the type {@link
     *         TabCloseListener}. The listener may not be null
     */
    public final void addCloseTabListener(@NonNull final TabCloseListener listener) {
        ensureNotNull(listener, "The listener may not be null");
        tabCloseListeners.add(listener);
    }

    /**
     * Removes a specific listener, which should not be notified, when a tab is about to be closed
     * by clicking its close button, anymore.
     *
     * @param listener
     *         The listener, which should be removed, as an instance of the type {@link
     *         TabCloseListener}. The listener may not be null
     */
    public final void removeCloseTabListener(@NonNull final TabCloseListener listener) {
        ensureNotNull(listener, "The listener may not be null");
        tabCloseListeners.remove(listener);
    }

    /**
     * Sets the view recycler, which allows to inflate the views, which are used to visualize tabs.
     *
     * @param viewRecycler
     *         The view recycler, which should be set, as an instance of the class {@link
     *         AttachedViewRecycler}. The view recycler may not be null
     */
    public final void setViewRecycler(
            @NonNull final AttachedViewRecycler<TabItem, Integer> viewRecycler) {
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        this.viewRecycler = viewRecycler;
    }

    /**
     * Adapts the padding of a tab.
     *
     * @param viewHolder
     *         The view holder, which stores references to the tab's views, as an instance of the
     *         class {@link TabViewHolder}. The view holder may not be null
     */
    public final void adaptPadding(@NonNull final TabViewHolder viewHolder) {
        if (viewHolder.child != null) {
            LayoutParams childLayoutParams = (LayoutParams) viewHolder.child.getLayoutParams();
            childLayoutParams.setMargins(tabSwitcher.getPaddingLeft(), tabSwitcher.getPaddingTop(),
                    tabSwitcher.getPaddingRight(), tabSwitcher.getPaddingBottom());
        }

        LayoutParams previewLayoutParams =
                (LayoutParams) viewHolder.previewImageView.getLayoutParams();
        previewLayoutParams.setMargins(tabSwitcher.getPaddingLeft(), tabSwitcher.getPaddingTop(),
                tabSwitcher.getPaddingRight(), tabSwitcher.getPaddingBottom());
    }

    /**
     * Removes all previously rendered previews from the cache.
     */
    public final void clearCachedPreviews() {
        dataBinder.clearCache();
    }

    @NonNull
    @Override
    public final View onInflateView(@NonNull final LayoutInflater inflater,
                                    @Nullable final ViewGroup parent,
                                    @NonNull final TabItem tabItem, final int viewType,
                                    @NonNull final Integer... params) {
        TabViewHolder viewHolder = new TabViewHolder();
        View view = inflater.inflate(
                tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE ? R.layout.tab_view_horizontally :
                        R.layout.tab_view, tabSwitcher.getTabContainer(), false);
        Drawable backgroundDrawable =
                ContextCompat.getDrawable(tabSwitcher.getContext(), R.drawable.tab_background);
        ViewUtil.setBackground(view, backgroundDrawable);
        int padding = tabInset + tabBorderWidth;
        view.setPadding(padding, tabInset, padding, padding);
        viewHolder.titleContainer = (ViewGroup) view.findViewById(R.id.tab_title_container);
        viewHolder.titleTextView = (TextView) view.findViewById(R.id.tab_title_text_view);
        viewHolder.closeButton = (ImageButton) view.findViewById(R.id.close_tab_button);
        viewHolder.childContainer = (ViewGroup) view.findViewById(R.id.child_container);
        viewHolder.previewImageView = (ImageView) view.findViewById(R.id.preview_image_view);
        adaptPadding(viewHolder);
        viewHolder.borderView = view.findViewById(R.id.border_view);
        Drawable borderDrawable =
                ContextCompat.getDrawable(tabSwitcher.getContext(), R.drawable.tab_border);
        ViewUtil.setBackground(viewHolder.borderView, borderDrawable);
        view.setTag(R.id.tag_view_holder, viewHolder);
        tabItem.setView(view);
        tabItem.setViewHolder(viewHolder);
        view.setTag(R.id.tag_properties, tabItem.getTag());
        return view;
    }

    @Override
    public final void onShowView(@NonNull final Context context, @NonNull final View view,
                                 @NonNull final TabItem tabItem, final boolean inflated,
                                 @NonNull final Integer... params) {
        TabViewHolder viewHolder = (TabViewHolder) view.getTag(R.id.tag_view_holder);

        if (!tabItem.isInflated()) {
            tabItem.setView(view);
            tabItem.setViewHolder(viewHolder);
            view.setTag(R.id.tag_properties, tabItem.getTag());
        }

        LayoutParams layoutParams =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        int borderMargin = -(tabInset + tabBorderWidth);
        int bottomMargin = params.length > 0 && params[0] != -1 ? params[0] : borderMargin;
        layoutParams.leftMargin = borderMargin;
        layoutParams.topMargin = -(tabInset + tabTitleContainerHeight);
        layoutParams.rightMargin = borderMargin;
        layoutParams.bottomMargin = bottomMargin;
        view.setLayoutParams(layoutParams);
        Tab tab = tabItem.getTab();
        tab.addCallback(this);
        adaptTitle(viewHolder, tab);
        adaptIcon(viewHolder, tab);
        adaptCloseButton(viewHolder, tab);
        adaptCloseButtonIcon(viewHolder, tab);
        adaptBackgroundColor(view, viewHolder, tab);
        adaptTitleTextColor(viewHolder, tab);

        if (!tabSwitcher.isSwitcherShown()) {
            if (tab == tabSwitcher.getSelectedTab()) {
                addChildView(tabItem);
            }
        } else {
            renderChildView(tabItem);
        }
    }

    @Override
    public final void onRemoveView(@NonNull final View view, @NonNull final TabItem tabItem) {
        TabViewHolder viewHolder = (TabViewHolder) view.getTag(R.id.tag_view_holder);
        Tab tab = tabItem.getTab();
        tab.removeCallback(this);
        removeChildView(viewHolder, tab);

        if (!dataBinder.isCached(tab)) {
            Drawable drawable = viewHolder.previewImageView.getDrawable();
            viewHolder.previewImageView.setImageBitmap(null);

            if (drawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        } else {
            viewHolder.previewImageView.setImageBitmap(null);
        }

        view.setTag(R.id.tag_properties, null);
    }

    @Override
    public final void onTitleChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptTitle(tabItem.getViewHolder(), tabItem.getTab());
        }
    }

    @Override
    public final void onIconChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptIcon(tabItem.getViewHolder(), tabItem.getTab());
        }
    }

    @Override
    public final void onCloseableChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptCloseButton(tabItem.getViewHolder(), tabItem.getTab());
        }
    }

    @Override
    public final void onCloseButtonIconChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptCloseButtonIcon(tabItem.getViewHolder(), tabItem.getTab());
        }
    }

    @Override
    public final void onBackgroundColorChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptBackgroundColor(tabItem.getView(), tabItem.getViewHolder(), tabItem.getTab());
        }
    }

    @Override
    public final void onTitleTextColorChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptTitleTextColor(tabItem.getViewHolder(), tabItem.getTab());
        }
    }

}