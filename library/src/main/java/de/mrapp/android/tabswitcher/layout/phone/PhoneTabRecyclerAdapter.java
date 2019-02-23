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
package de.mrapp.android.tabswitcher.layout.phone;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabPreviewListener;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.iterator.ItemIterator;
import de.mrapp.android.tabswitcher.layout.AbstractTabRecyclerAdapter;
import de.mrapp.android.tabswitcher.layout.AbstractTabViewHolder;
import de.mrapp.android.tabswitcher.model.AbstractItem;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.tabswitcher.model.TabSwitcherStyle;
import de.mrapp.android.util.ViewUtil;
import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.android.util.multithreading.AbstractDataBinder;
import de.mrapp.android.util.view.ViewRecycler;
import de.mrapp.util.Condition;

/**
 * A view recycler adapter, which allows to inflate the views, which are used to visualize the tabs
 * of a {@link TabSwitcher}, when using the smartphone layout.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class PhoneTabRecyclerAdapter extends AbstractTabRecyclerAdapter
        implements AbstractDataBinder.Listener<Bitmap, Tab, ImageView, TabItem> {

    /**
     * The view recycler, which allows to inflate the views, which are associated with tabs.
     */
    private final ViewRecycler<Tab, Void> tabViewRecycler;

    /**
     * The data binder, which allows to render previews of tabs.
     */
    private final AbstractDataBinder<Bitmap, Tab, ImageView, TabItem> dataBinder;

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
     * Inflates the view, which is associated with a tab, and adds it to the view hierarchy.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose associated view should be inflated,
     *         as an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void addContentView(@NonNull final TabItem tabItem) {
        PhoneTabViewHolder viewHolder = (PhoneTabViewHolder) tabItem.getViewHolder();
        View view = viewHolder.content;
        Tab tab = tabItem.getTab();

        if (view == null) {
            ViewGroup parent = viewHolder.contentContainer;
            Pair<View, ?> pair = tabViewRecycler.inflate(tab, parent);
            view = pair.first;
            LayoutParams layoutParams =
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            Rect padding = getPadding();
            layoutParams.setMargins(padding.left, padding.top, padding.right, padding.bottom);
            parent.addView(view, 0, layoutParams);
            viewHolder.content = view;
        } else {
            tabViewRecycler.getAdapter().onShowView(getModel().getContext(), view, tab, false);
        }

        viewHolder.previewImageView.setVisibility(View.GONE);
        viewHolder.previewImageView.setImageBitmap(null);
        viewHolder.borderView.setVisibility(View.GONE);
    }

    /**
     * Renders and displays the preview of a tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose preview should be rendered, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     */
    private void renderPreview(@NonNull final TabItem tabItem) {
        Tab tab = tabItem.getTab();
        PhoneTabViewHolder viewHolder = (PhoneTabViewHolder) tabItem.getViewHolder();
        viewHolder.borderView
                .setVisibility(getModel().isSwitcherShown() ? View.VISIBLE : View.GONE);

        if (viewHolder.content != null) {
            tabViewRecycler.getAdapter().onRemoveView(viewHolder.content, tab);
            dataBinder.load(tab, viewHolder.previewImageView, false, tabItem);
            removeContentView(viewHolder, tab);
        } else {
            dataBinder.load(tab, viewHolder.previewImageView, tabItem);
        }
    }

    /**
     * Removes the view, which is associated with a tab, from its parent.
     *
     * @param viewHolder
     *         The view holder, which stores references to the tab's views, as an instance of the
     *         class {@link PhoneTabViewHolder}. The view holder may not be null
     * @param tab
     *         The tab, whose associated view should be removed, as an instance of the class {@link
     *         Tab}. The tab may not be null
     */
    private void removeContentView(@NonNull final PhoneTabViewHolder viewHolder,
                                   @NonNull final Tab tab) {
        if (viewHolder.contentContainer.getChildCount() > 2) {
            viewHolder.contentContainer.removeViewAt(0);
        }

        viewHolder.content = null;
        tabViewRecycler.remove(tab);
    }

    /**
     * Adapts the visibility of a tab's background.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose backgrounds's visibility should be
     *         adapted, as an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptBackgroundVisibility(@NonNull final TabItem tabItem) {
        View view = tabItem.getView();
        Drawable background = view.getBackground();
        background.setAlpha(getModel().isSwitcherShown() ? 0xFF : 0x0);
    }

    /**
     * Adapts the background color of a tab's content.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose content's background should be
     *         adapted, as an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void adaptContentBackgroundColor(@NonNull final TabItem tabItem) {
        Tab tab = tabItem.getTab();
        int color = getStyle().getTabContentBackgroundColor(tab);
        PhoneTabViewHolder viewHolder = (PhoneTabViewHolder) tabItem.getViewHolder();
        viewHolder.contentContainer.setBackgroundColor(color);
    }

    /**
     * Adapts the log level.
     */
    private void adaptLogLevel() {
        dataBinder.setLogLevel(getModel().getLogLevel());
    }

    /**
     * Adapts the padding of a tab.
     *
     * @param viewHolder
     *         The view holder, which stores references to the tab's views, as an instance of the
     *         class {@link PhoneTabViewHolder}. The view holder may not be null
     */
    private void adaptPadding(@NonNull final PhoneTabViewHolder viewHolder) {
        Rect padding = getPadding();

        if (viewHolder.content != null) {
            LayoutParams contentLayoutParams = (LayoutParams) viewHolder.content.getLayoutParams();
            contentLayoutParams
                    .setMargins(padding.left, padding.top, padding.right, padding.bottom);
        }

        LayoutParams previewLayoutParams =
                (LayoutParams) viewHolder.previewImageView.getLayoutParams();
        previewLayoutParams.setMargins(padding.left, padding.top, padding.right, padding.bottom);
    }

    /**
     * Returns the padding of a tab.
     *
     * @return A rect, which contains the left, top, right and bottom padding of a tab, as an
     * instance of the class {@link Rect}. The rect may not be null
     */
    @NonNull
    private Rect getPadding() {
        if (getModel().isPaddingAppliedToTabs()) {
            return new Rect(getModel().getPaddingLeft(), getModel().getPaddingTop(),
                    getModel().getPaddingRight(), getModel().getPaddingBottom());
        } else {
            return new Rect(0, 0, 0, 0);
        }
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
     * @param tabViewRecycler
     *         The view recycler, which allows to inflate the views, which are associated with tabs,
     *         as an instance of the class ViewRecycler. The view recycler may not be null
     */
    public PhoneTabRecyclerAdapter(@NonNull final TabSwitcher tabSwitcher,
                                   @NonNull final TabSwitcherModel model,
                                   @NonNull final TabSwitcherStyle style,
                                   @NonNull final ViewRecycler<Tab, Void> tabViewRecycler) {
        super(tabSwitcher, model, style);
        Condition.INSTANCE.ensureNotNull(tabViewRecycler, "The tab view recycler may not be null");
        this.tabViewRecycler = tabViewRecycler;
        this.dataBinder = new PreviewDataBinder(tabSwitcher, tabViewRecycler, model);
        this.dataBinder.addListener(this);
        Resources resources = tabSwitcher.getResources();
        this.tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        this.tabBorderWidth = resources.getDimensionPixelSize(R.dimen.tab_border_width);
        this.tabTitleContainerHeight =
                resources.getDimensionPixelSize(R.dimen.tab_title_container_height);
        adaptLogLevel();
    }

    /**
     * Removes all previously rendered previews from the cache.
     */
    public final void clearCachedPreviews() {
        dataBinder.clearCache();
    }

    @Override
    protected final void onAdaptBackgroundColor(@ColorInt final int color,
                                                @NonNull final TabItem tabItem) {
        PhoneTabViewHolder viewHolder = (PhoneTabViewHolder) tabItem.getViewHolder();
        Drawable border = viewHolder.borderView.getBackground();
        border.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }

    @NonNull
    @Override
    protected final View onInflateTabView(@NonNull final LayoutInflater inflater,
                                          @Nullable final ViewGroup parent,
                                          @NonNull final AbstractTabViewHolder viewHolder) {
        View view = inflater.inflate(R.layout.phone_tab, parent, false);
        Drawable backgroundDrawable =
                ContextCompat.getDrawable(getModel().getContext(), R.drawable.phone_tab_background);
        ViewUtil.setBackground(view, backgroundDrawable);
        int padding = tabInset + tabBorderWidth;
        view.setPadding(padding, tabInset, padding, padding);
        ((PhoneTabViewHolder) viewHolder).contentContainer =
                view.findViewById(R.id.content_container);
        ((PhoneTabViewHolder) viewHolder).previewImageView =
                view.findViewById(R.id.preview_image_view);
        adaptPadding((PhoneTabViewHolder) viewHolder);
        ((PhoneTabViewHolder) viewHolder).borderView = view.findViewById(R.id.border_view);
        Drawable borderDrawable =
                ContextCompat.getDrawable(getModel().getContext(), R.drawable.phone_tab_border);
        ViewUtil.setBackground(((PhoneTabViewHolder) viewHolder).borderView, borderDrawable);
        return view;
    }

    @Override
    protected final void onShowTabView(@NonNull final View view, @NonNull final TabItem tabItem,
                                       @NonNull final Integer... params) {
        LayoutParams layoutParams =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        int borderMargin = -(tabInset + tabBorderWidth);
        int bottomMargin = params.length > 0 && params[0] != -1 ? params[0] : borderMargin;
        layoutParams.leftMargin = borderMargin;
        layoutParams.topMargin = -(tabInset + tabTitleContainerHeight);
        layoutParams.rightMargin = borderMargin;
        layoutParams.bottomMargin = bottomMargin;
        view.setLayoutParams(layoutParams);
        adaptContentBackgroundColor(tabItem);
        adaptBackgroundVisibility(tabItem);

        if (!getModel().isSwitcherShown()) {
            addContentView(tabItem);
        } else {
            renderPreview(tabItem);
        }
    }

    @NonNull
    @Override
    protected final AbstractTabViewHolder onCreateTabViewHolder() {
        return new PhoneTabViewHolder();
    }

    @NonNull
    @Override
    protected final Layout getLayout() {
        return Layout.PHONE_PORTRAIT;
    }

    @Override
    public final void onRemoveView(@NonNull final View view, @NonNull final AbstractItem item) {
        if (item instanceof TabItem) {
            TabItem tabItem = (TabItem) item;
            PhoneTabViewHolder viewHolder = (PhoneTabViewHolder) view.getTag(R.id.tag_view_holder);
            Tab tab = tabItem.getTab();
            removeContentView(viewHolder, tab);

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

            super.onRemoveView(view, tabItem);
        } else {
            throw new IllegalArgumentException("Unknown item type");
        }
    }

    @Override
    public final void onLogLevelChanged(@NonNull final LogLevel logLevel) {
        adaptLogLevel();
    }

    @Override
    public final void onPaddingChanged(final int left, final int top, final int right,
                                       final int bottom) {
        ItemIterator iterator =
                new ItemIterator.Builder(getModel(), getViewRecyclerOrThrowException()).create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            if (item.isInflated() && item instanceof TabItem) {
                AbstractTabViewHolder viewHolder = ((TabItem) item).getViewHolder();
                adaptPadding((PhoneTabViewHolder) viewHolder);
            }
        }
    }

    @Override
    public final void onApplyPaddingToTabsChanged(final boolean applyPaddingToTabs) {
        ItemIterator iterator =
                new ItemIterator.Builder(getModel(), getViewRecyclerOrThrowException()).create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            if (item.isInflated() && item instanceof TabItem) {
                AbstractTabViewHolder viewHolder = ((TabItem) item).getViewHolder();
                adaptPadding((PhoneTabViewHolder) viewHolder);
            }
        }
    }

    @Override
    public final void onTabContentBackgroundColorChanged(@ColorInt final int color) {
        ItemIterator iterator =
                new ItemIterator.Builder(getModel(), getViewRecyclerOrThrowException()).create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            if (item.isInflated() && item instanceof TabItem) {
                adaptContentBackgroundColor((TabItem) item);
            }
        }
    }

    @Override
    public final void onContentBackgroundColorChanged(@NonNull final Tab tab) {
        TabItem tabItem = getTabItem(tab);

        if (tabItem != null) {
            adaptContentBackgroundColor(tabItem);
        }
    }

    @Override
    public final boolean onLoadData(
            @NonNull final AbstractDataBinder<Bitmap, Tab, ImageView, TabItem> dataBinder,
            @NonNull final Tab key, @NonNull final TabItem... params) {
        boolean result = true;

        for (TabPreviewListener listener : getModel().getTabPreviewListeners()) {
            result &= listener.onLoadTabPreview(getTabSwitcher(), key);
        }

        return result;
    }

    @Override
    public final void onFinished(
            @NonNull final AbstractDataBinder<Bitmap, Tab, ImageView, TabItem> dataBinder,
            @NonNull final Tab key, @Nullable final Bitmap data, @NonNull final ImageView view,
            @NonNull final TabItem... params) {

    }

    @Override
    public final void onCanceled(
            @NonNull final AbstractDataBinder<Bitmap, Tab, ImageView, TabItem> dataBinder) {

    }

}