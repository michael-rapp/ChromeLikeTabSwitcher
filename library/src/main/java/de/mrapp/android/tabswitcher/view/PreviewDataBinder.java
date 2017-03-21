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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.support.v4.util.Pair;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;

import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.util.DataBinder;
import de.mrapp.android.tabswitcher.util.ViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A data binder, which allows to asynchronously render preview images of tabs and display them
 * afterwards.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class PreviewDataBinder extends DataBinder<Bitmap, Tab, ImageView, TabItem> {

    /**
     * The tab switcher, the tabs belong to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The view recycler, which is used to inflate child views.
     */
    private final ViewRecycler<Tab, Void> childViewRecycler;

    /**
     * Creates a new data binder, which allows to asynchronously render preview images of tabs and
     * display them afterwards.
     *
     * @param tabSwitcher
     *         The tab switcher, the tabs belong to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param childViewRecycler
     *         The view recycler, which should be used to inflate child views, as an instance of the
     *         class {@link ViewRecycler}. The view recycler may not be null
     */
    public PreviewDataBinder(@NonNull final TabSwitcher tabSwitcher,
                             @NonNull final ViewRecycler<Tab, Void> childViewRecycler) {
        super(tabSwitcher.getContext(), new LruCache<Tab, Bitmap>(7));
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(childViewRecycler, "The child view recycler may not be null");
        this.tabSwitcher = tabSwitcher;
        this.childViewRecycler = childViewRecycler;
    }

    @Override
    protected final void onPreExecute(@NonNull final ImageView view,
                                      @NonNull final TabItem... params) {
        TabItem tabItem = params[0];
        TabViewHolder viewHolder = tabItem.getViewHolder();
        View child = viewHolder.child;
        Tab tab = tabItem.getTab();

        if (child == null) {
            Pair<View, ?> pair = childViewRecycler.inflate(tab, viewHolder.childContainer);
            child = pair.first;
        }

        tabSwitcher.getDecorator().applyDecorator(getContext(), tabSwitcher, child, tab);
        viewHolder.child = child;
    }

    @Nullable
    @Override
    protected final Bitmap doInBackground(@NonNull final Tab key,
                                          @NonNull final TabItem... params) {
        TabItem tabItem = params[0];
        TabViewHolder viewHolder = tabItem.getViewHolder();
        View child = viewHolder.child;
        viewHolder.child = null;
        int width = tabSwitcher.getWidth();
        int height = tabSwitcher.getHeight();
        child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        child.draw(canvas);

        // TODO: This is only for debugging purposes
        Paint paint = new Paint();
        paint.setTextSize(48);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setColor(Color.RED);
        canvas.drawText(Integer.toString(params[0].getIndex()), 50, 50, paint);

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