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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.core.util.Pair;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.multithreading.AbstractDataBinder;
import de.mrapp.android.util.view.ViewRecycler;
import de.mrapp.util.Condition;

/**
 * A data binder, which allows to asynchronously render preview images of tabs and display them
 * afterwards.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class PreviewDataBinder extends AbstractDataBinder<Bitmap, Tab, ImageView, TabItem> {

    /**
     * The parent view of the tab switcher, the tabs belong to.
     */
    private final ViewGroup parent;

    /**
     * The view recycler, which is used to inflate the views, which are associated with tabs.
     */
    private final ViewRecycler<Tab, Void> contentViewRecycler;

    /**
     * The model of the tab switcher, the tabs belong to.
     */
    private final Model model;

    /**
     * Creates a new data binder, which allows to asynchronously render preview images of tabs and
     * display them afterwards.
     *
     * @param parent
     *         The parent view of the tab switcher, the tabs belong to, as an instance of the class
     *         {@link ViewGroup}. The parent may not be null
     * @param contentViewRecycler
     *         The view recycler, which should be used to inflate the views, which are associated
     *         with tabs, as an instance of the class ViewRecycler. The view recycler may not be
     *         null
     * @param model
     *         The model of the tab switcher, the tabs belong to, as an instance of the type {@link
     *         Model}. The model may not be null
     */
    public PreviewDataBinder(@NonNull final ViewGroup parent,
                             @NonNull final ViewRecycler<Tab, Void> contentViewRecycler,
                             @NonNull final Model model) {
        super(parent.getContext().getApplicationContext(), new LruCache<Tab, Bitmap>(7));
        Condition.INSTANCE.ensureNotNull(parent, "The parent may not be null");
        Condition.INSTANCE
                .ensureNotNull(contentViewRecycler, "The content view recycler may not be null");
        this.parent = parent;
        this.contentViewRecycler = contentViewRecycler;
        this.model = model;
    }

    @Override
    protected final void onPreExecute(@NonNull final ImageView view,
                                      @NonNull final TabItem... params) {
        TabItem tabItem = params[0];
        PhoneTabViewHolder viewHolder = (PhoneTabViewHolder) tabItem.getViewHolder();
        View content = viewHolder.content;
        Tab tab = tabItem.getTab();

        if (content == null) {
            Pair<View, ?> pair = contentViewRecycler.inflate(tab, viewHolder.contentContainer);
            content = pair.first;
        } else {
            contentViewRecycler.getAdapter().onShowView(getContext(), content, tab, false);
        }

        viewHolder.content = content;
    }

    @NonNull
    @Override
    protected final Bitmap doInBackground(@NonNull final Tab key,
                                          @NonNull final TabItem... params) {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        TabItem tabItem = params[0];
        PhoneTabViewHolder viewHolder = (PhoneTabViewHolder) tabItem.getViewHolder();
        View content = viewHolder.content;
        viewHolder.content = null;
        int width = parent.getWidth();
        int height = parent.getHeight();
        content.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        content.layout(0, 0, content.getMeasuredWidth(), content.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        content.draw(canvas);
        return bitmap;
    }

    @Override
    protected final void onPostExecute(@NonNull final ImageView view, @Nullable final Bitmap data,
                                       final long duration, @NonNull final TabItem... params) {
        view.setImageBitmap(data);

        if (data != null) {
            boolean useFadeAnimation = duration > model.getTabPreviewFadeThreshold();
            view.setAlpha(useFadeAnimation ? 0f : 1f);
            view.setVisibility(View.VISIBLE);

            if (useFadeAnimation) {
                view.animate().alpha(1f).setDuration(model.getTabPreviewFadeDuration())
                        .setInterpolator(new AccelerateDecelerateInterpolator()).start();
            }
        } else {
            view.setVisibility(View.INVISIBLE);
        }

        view.setVisibility(data != null ? View.VISIBLE : View.GONE);
        TabItem tabItem = params[0];
        contentViewRecycler.remove(tabItem.getTab());
    }

}