/*
 * Copyright 2015 - 2017 Michael Rapp
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
package de.mrapp.android.tabswitcher.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.android.util.logging.Logger;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An abstract base class for all recyclers, which allow to cache views in order to be able to reuse
 * them later, instead of inflating new instances. Such a recycler can for example be used to
 * efficiently implement widgets similar to ListViews, whose children can be scrolled out of the
 * visible screen area and therefore can be recycled.
 *
 * Each view must be associated with a corresponding item of an arbitrary generic type. By
 * implementing the abstract class {@link Adapter}, the views, which should be used to visualize a
 * specific item, can be inflated and adapted in their appearance. The recycler supports to inflate
 * different layouts for individual items by overriding an adapter's <code>getViewType</code>- and
 * <code>getViewTypeCount</code>-method.
 *
 * @param <ItemType>
 *         The type of the items, which should be visualized by inflated views
 * @param <ParamType>
 *         The type of the optional parameters, which may be passed when inflating a view
 * @author Michael Rapp
 * @since 1.14.0
 */
public abstract class AbstractViewRecycler<ItemType, ParamType> {

    /**
     * An abstract base class for all adapters, which are responsible for inflating views, which
     * should be used to visualize the items of a {@link ViewRecycler}.
     *
     * @param <ItemType>
     *         The type of the items, which should be visualized by the adapter
     * @param <ParamType>
     *         The type of the optional parameters, which may be passed when inflating a view
     */
    public static abstract class Adapter<ItemType, ParamType> {

        /**
         * The method, which is invoked in order to inflate the view, which should be used to
         * visualize a specific item. This method is only called, if no cached views are available
         * to be reused. It should only inflate the appropriate layout for visualizing an item. For
         * modifying the appearance of the layout's children, the method <code>onShowItem</code> is
         * responsible.
         *
         * @param inflater
         *         The layout inflater, which should be used to inflate the view, as an instance of
         *         the class {@link LayoutInflater}. The layout inflater may not be null
         * @param parent
         *         The parent, the inflated view will be added to, as an instance of the class
         *         {@link ViewGroup} or null, if the view will not be added to a parent
         * @param item
         *         The item, which should be visualized by the inflated view, as an instance of the
         *         generic type ItemType. The item may not be null
         * @param viewType
         *         The view type, which corresponds to the item, which should be visualized, as an
         *         {@link Integer} value
         * @param params
         *         An array, which may contain optional parameters, as an array of the generic type
         *         ParamType or an empty array, if no optional parameters are available
         * @return The view, which has been inflated, as an instance of the class {@link View}. The
         * view may not be null
         */
        @SuppressWarnings("unchecked")
        @NonNull
        public abstract View onInflateView(@NonNull final LayoutInflater inflater,
                                           @Nullable final ViewGroup parent,
                                           @NonNull final ItemType item, final int viewType,
                                           @NonNull final ParamType... params);

        /**
         * The method, which is invoked in order to adapt the appearance of the view, which is used
         * to visualize a specific item. This method is called every time a view has been inflated
         * or reused.
         *
         * @param context
         *         The context, which is used by the adapter, as an instance of the class {@link
         *         Context}. The context may not be null
         * @param view
         *         The view, whose appearance should be adapted, as an instance of the class {@link
         *         View}. The view may not be null
         * @param item
         *         The item, which should be visualized, as an instance of the generic type
         *         ItemType. The item may not be null
         * @param params
         *         An array, which may contain optional parameters, as an array of the generic type
         *         ParamType or an empty array, if no optional parameters are available
         */
        @SuppressWarnings("unchecked")
        public abstract void onShowView(@NonNull final Context context, @NonNull final View view,
                                        @NonNull final ItemType item,
                                        @NonNull final ParamType... params);

        /**
         * The method, which is invoked when a previously inflated view is about to be removed from
         * its parent. It may be overridden in order to reset the view's state. This is for example
         * necessary, if children, which are reused themselves using a different lifecycle, are
         * attached to the view.
         *
         * @param view
         *         The view, which is about to be removed from its parent, as an instance of the
         *         class {@link View}. The view may not be null
         * @param item
         *         The item, which is visualized by the view, which is about to be removed, as an
         *         instance of the generic type ItemType. The item may not be null
         */
        public void onRemoveView(@NonNull final View view, @NonNull final ItemType item) {

        }

        /**
         * Returns the view type, which corresponds to a specific item. For each layout, which is
         * inflated by the <code>onInflateView</code>-method, a distinct view type must be
         * returned.
         *
         * @param item
         *         The item, whose view type should be returned, as an instance of the generic type
         *         ItemType. The item may not be null
         * @return The view type, which corresponds to the given item, as an {@link Integer} value
         */
        public int getViewType(@NonNull final ItemType item) {
            return 0;
        }

        /**
         * Returns the number of view types, which are used by the adapter.
         *
         * @return The number of view types, which are used by the adapter, as an {@link Integer}
         * value. The number of view types must correspond to the number of distinct values, which
         * are returned by the <code>getViewType</code>-method
         */
        public int getViewTypeCount() {
            return 1;
        }

    }

    /**
     * The context, which is used by the recycler.
     */
    private final Context context;

    /**
     * The layout inflater, which is used to inflate views.
     */
    private final LayoutInflater inflater;

    /**
     * A map, which manages the views, which are currently used to visualize specific items.
     */
    private final Map<ItemType, View> activeViews;

    /**
     * The logger, which is used by the recycler.
     */
    private final Logger logger;

    /**
     * The adapter, which is used to inflate and adapt the appearance of views.
     */
    private Adapter<ItemType, ParamType> adapter;

    /**
     * A sparse array, which manages the views, which are currently unused. The views are associated
     * with the view type the correspond to.
     */
    private SparseArray<Queue<View>> unusedViews;

    /**
     * True, if unused views are cached, false otherwise.
     */
    private boolean useCache;

    /**
     * Adds an unused view to the cache.
     *
     * @param view
     *         The unused view, which should be added to the cache, as an instance of the class
     *         {@link View}. The view may not be null
     * @param viewType
     *         The view type, the unused view corresponds to, as an {@link Integer} value
     */
    protected final void addUnusedView(@NonNull final View view, final int viewType) {
        if (useCache) {
            if (unusedViews == null) {
                unusedViews = new SparseArray<>(adapter.getViewTypeCount());
            }

            Queue<View> queue = unusedViews.get(viewType);

            if (queue == null) {
                queue = new LinkedList<>();
                unusedViews.put(viewType, queue);
            }

            queue.add(view);
        }
    }

    /**
     * Retrieves an unused view, which corresponds to a specific view type, from the cache, if any
     * is available.
     *
     * @param viewType
     *         The view type of the unused view, which should be retrieved, as an {@link Integer}
     *         value
     * @return An unused view, which corresponds to the given view type, as an instance of the class
     * {@link View} or null, if no such view is available in the cache
     */
    @Nullable
    protected final View pollUnusedView(final int viewType) {
        if (useCache && unusedViews != null) {
            Queue<View> queue = unusedViews.get(viewType);

            if (queue != null) {
                return queue.poll();
            }
        }

        return null;
    }

    /**
     * Returns the logger, which is used by the recycler.
     *
     * @return The logger, which is used by the recycler, as an instance of the class {@link
     * Logger}. The logger may not be null
     */
    @NonNull
    protected final Logger getLogger() {
        return logger;
    }

    /**
     * Returns the layout inflater, which is used to inflate views.
     *
     * @return The layout inflater, which is used to inflate views, as an instance of the class
     * {@link LayoutInflater}. The layout inflater may not be null
     */
    @NonNull
    protected final LayoutInflater getLayoutInflater() {
        return inflater;
    }

    /**
     * Returns the map, which manages the views, which are currently used to visualize items.
     *
     * @return The map, which manages the views, which are currently used to visualize items, as an
     * instance of the type {@link Map}. The map may not be null
     */
    @NonNull
    protected Map<ItemType, View> getActiveViews() {
        return activeViews;
    }

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later,
     * instead of inflating new instances.
     *
     * @param inflater
     *         The layout inflater, which should be used to inflate views, as an instance of the
     *         class {@link LayoutInflater}. The layout inflater may not be null
     */
    public AbstractViewRecycler(@NonNull final LayoutInflater inflater) {
        ensureNotNull(inflater, "The layout inflater may not be null");
        this.context = inflater.getContext();
        this.inflater = inflater;
        this.activeViews = new HashMap<>();
        this.logger = new Logger(LogLevel.INFO);
        this.adapter = null;
        this.unusedViews = null;
        this.useCache = true;
    }

    /**
     * Inflates the view, which is used to visualize a specific item. If possible, an unused view
     * will be retrieved from the cache, instead of inflating a new instance.
     *
     * @param item
     *         The item, which should be visualized by the inflated view, as an instance of the
     *         generic type ItemType. The item may not be null
     * @param params
     *         An array, which may contain optional parameters, as an array of the generic type
     *         ParamType or an empty array, if no optional parameters are available
     * @return A pair, which contains the view, which is used to visualize the given item, as well
     * as a boolean value, which indicates, whether a new view has been inflated, or if an unused
     * view has been reused from the cache, as an instance of the class {@link Pair}. The pair may
     * not be null
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public abstract Pair<View, Boolean> inflate(@NonNull final ItemType item,
                                                @NonNull final ParamType... params);

    /**
     * Removes a previously inflated view, which is used to visualize a specific item. If caching is
     * enabled, the view will be put into a cache in order to be able to reuse it later.
     *
     * @param item
     *         The item, which is visualized by the view, which should be removed, as an instance of
     *         the generic type ItemType. The item may not be null
     */
    public abstract void remove(@NonNull final ItemType item);

    /**
     * Removes all previously inflated views. If caching is enabled, all of these views will be
     * added to a cache in order to be able to reuse them later.
     */
    public abstract void removeAll();

    /**
     * Returns the context, which is used by the view recycler.
     *
     * @return The context, which is used by the view recycler, as an instance of the class {@link
     * Context}. The context may not be null
     */
    @NonNull
    public final Context getContext() {
        return context;
    }

    /**
     * Returns the adapter, which is used to inflate and adapt the appearance of views.
     *
     * @return The adapter, which is used to inflate and adapt the appearance of views, as an
     * instance of the class {@link Adapter} or null, if no adapter is set
     */
    public final Adapter<ItemType, ParamType> getAdapter() {
        return adapter;
    }

    /**
     * Sets the adapter, which should be used to inflate and adapt the appearance of views. Calling
     * this method causes the cache to be cleared.
     *
     * @param adapter
     *         The adapter, which should be set, as an instance of the class {@link Adapter} or
     *         null, if no adapter should be set
     */
    public final void setAdapter(@Nullable final Adapter<ItemType, ParamType> adapter) {
        this.adapter = adapter;
        clearCache();
    }

    /**
     * Returns the log level, which is used for logging.
     *
     * @return The log level, which is used for logging, as a value of the enum {@link LogLevel}.
     * The log level may not be null
     */
    public final LogLevel getLogLevel() {
        return logger.getLogLevel();
    }

    /**
     * Sets the log level, which should be used for logging.
     *
     * @param logLevel
     *         The log level, which should be set, as a value of the enum {@link LogLevel}. The log
     *         level may not be null
     */
    public final void setLogLevel(@NonNull final LogLevel logLevel) {
        logger.setLogLevel(logLevel);
    }

    /**
     * Returns the view, which is currently used to visualize a specific item.
     *
     * @param item
     *         The item, whose view should be returned, as an instance of the generic type ItemType.
     *         The item may not be null
     * @return The view, which is currently used to visualize the given item, as an instance of the
     * type ItemType or null, if no such view is currently inflated
     */
    @Nullable
    public final View getView(@NonNull final ItemType item) {
        ensureNotNull(item, "The item may not be null");
        return activeViews.get(item);
    }

    /**
     * Returns, whether a view is currently inflated to visualize a specific item.
     *
     * @param item
     *         The item, which should be checked, as an instance of the generic type ItemType. The
     *         item may not be null
     * @return True, if a view is currently inflated to visualize the given item, false otherwise
     */
    public final boolean isInflated(@NonNull final ItemType item) {
        return getView(item) != null;
    }

    /**
     * Removes all unused views from the cache.
     */
    public final void clearCache() {
        if (unusedViews != null) {
            unusedViews.clear();
            unusedViews = null;
        }

        logger.logDebug(getClass(), "Removed all unused views from cache");
    }

    /**
     * Removes all unused views, which correspond to a specific view type, from the cache.
     *
     * @param viewType
     *         The view type of the unused views, which should be removed from the cache, as an
     *         {@link Integer} value
     */
    public final void clearCache(final int viewType) {
        if (unusedViews != null) {
            unusedViews.remove(viewType);
        }

        logger.logDebug(getClass(),
                "Removed all unused views of view type " + viewType + " from cache");
    }

    /**
     * Returns, whether unused views are cached, or not.
     *
     * @return True, if unused views are cached, false otherwise
     */
    public final boolean isCacheUsed() {
        return useCache;
    }

    /**
     * Sets, whether unused views should be cached, or not.
     *
     * @param useCache
     *         True, if unused views should be cached, false otherwise
     */
    public final void useCache(final boolean useCache) {
        this.useCache = useCache;

        if (!useCache) {
            clearCache();
        }
    }

}