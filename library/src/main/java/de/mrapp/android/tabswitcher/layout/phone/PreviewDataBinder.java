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
package de.mrapp.android.tabswitcher.layout.phone;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.support.v4.util.Pair;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.ImageView;

import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.multithreading.AbstractDataBinder;
import de.mrapp.android.util.view.ViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

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
     * The view recycler, which is used to inflate child views.
     */
    private final ViewRecycler<Tab, Void> childViewRecycler;

    /**
     * Creates a new data binder, which allows to asynchronously render preview images of tabs and
     * display them afterwards.
     *
     * @param parent
     *         The parent view of the tab switcher, the tabs belong to, as an instance of the class
     *         {@link ViewGroup}. The parent may not be null
     * @param childViewRecycler
     *         The view recycler, which should be used to inflate child views, as an instance of the
     *         class ViewRecycler. The view recycler may not be null
     */
    public PreviewDataBinder(@NonNull final ViewGroup parent,
                             @NonNull final ViewRecycler<Tab, Void> childViewRecycler) {
        super(parent.getContext(), new LruCache<Tab, Bitmap>(7));
        ensureNotNull(parent, "The parent may not be null");
        ensureNotNull(childViewRecycler, "The child view recycler may not be null");
        this.parent = parent;
        this.childViewRecycler = childViewRecycler;
    }

    @Override
    protected final void onPreExecute(@NonNull final ImageView view,
                                      @NonNull final TabItem... params) {
        TabItem tabItem = params[0];
        PhoneTabViewHolder viewHolder = tabItem.getViewHolder();
        View child = viewHolder.child;
        Tab tab = tabItem.getTab();

        if (child == null) {
            Pair<View, ?> pair = childViewRecycler.inflate(tab, viewHolder.childContainer);
            child = pair.first;
        } else {
            childViewRecycler.getAdapter().onShowView(getContext(), child, tab, false);
        }

        viewHolder.child = child;
    }

    @Nullable
    @Override
    protected final Bitmap doInBackground(@NonNull final Tab key,
                                          @NonNull final TabItem... params) {
        TabItem tabItem = params[0];
        PhoneTabViewHolder viewHolder = tabItem.getViewHolder();
        View child = viewHolder.child;
        viewHolder.child = null;
        int width = parent.getWidth();
        int height = parent.getHeight();
        child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        child.draw(canvas);
        return bitmap;
    }

    @Override
    protected final void onPostExecute(@NonNull final ImageView view, @Nullable final Bitmap data,
                                       @NonNull final TabItem... params) {
        view.setImageBitmap(data);
        view.setVisibility(data != null ? View.VISIBLE : View.GONE);
        TabItem tabItem = params[0];
        childViewRecycler.remove(tabItem.getTab());
    }

}