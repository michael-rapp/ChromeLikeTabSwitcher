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
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.android.util.logging.Logger;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A recycler, which allows to cache views in order to be able to reuse them later instead of
 * inflating new instances. Such a recycler can for example be used to efficiently implement widgets
 * similar to ListViews, whose children can be scrolled out of the visible screen area and therefore
 * can be recycled.
 *
 * A recycler is bound to a {@link ViewGroup}, which acts as the parent of all inflated views. Each
 * time a view is inflated using the <code>inflate</code>-method, it is added the the parent. By
 * calling the <code>remove</code>- or <code>removeAll</code>-method, previously inflated views can
 * be removed from the parent. They will be kept in a cache in order to reuse them later.
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
public class AttachedViewRecycler<ItemType, ParamType> {

    /**
     * An abstract base class for all adapters, which are responsible for inflating views, which
     * should be used to visualize the items of a {@link AttachedViewRecycler}.
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
     * The parent, the recycler is bound to.
     */
    private final ViewGroup parent;

    /**
     * The comparator, which is used to determine the order, which is used to add views to the
     * parent.
     */
    private final Comparator<ItemType> comparator;

    /**
     * A map, which manages the views, which are currently used to visualize specific items.
     */
    private final Map<ItemType, View> activeViews;

    /**
     * A list, which contains the items, which are currently visualized by the active views. The
     * order of the items corresponds to the hierarchical order of the corresponding views in their
     * parent.
     */
    private final List<ItemType> items;

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
    private void addUnusedView(@NonNull final View view, final int viewType) {
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
    private View pollUnusedView(final int viewType) {
        if (useCache && unusedViews != null) {
            Queue<View> queue = unusedViews.get(viewType);

            if (queue != null) {
                return queue.poll();
            }
        }

        return null;
    }

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later
     * instead of inflating new instances. By default, views are added to the parent in the order of
     * their inflation.
     *
     * @param parent
     *         The parent, the recycler should be bound to, as an instance of the class {@link
     *         ViewGroup}. The parent may not be null
     */
    public AttachedViewRecycler(@NonNull final ViewGroup parent) {
        this(parent, LayoutInflater.from(parent.getContext()));
    }

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later
     * instead of inflating new instances. This constructor allows to specify a comparator, which
     * allows to determine the order, which should be used to add views to the parent.
     *
     * @param parent
     *         The parent, the recycler should be bound to, as an instance of the class {@link
     *         ViewGroup}. The parent may not be null
     * @param comparator
     *         The comparator, which allows to determine the order, which should be used to add
     *         views to the parent, as an instance of the type {@link Comparator} or null, if the
     *         views should be added in the order of their inflation
     */
    public AttachedViewRecycler(@NonNull final ViewGroup parent,
                                @Nullable final Comparator<ItemType> comparator) {
        this(parent, LayoutInflater.from(parent.getContext()), comparator);
    }

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later
     * instead of inflating new instances. By default, views are added to the parent in the order of
     * their inflation.
     *
     * @param parent
     *         The parent, the recycler should be bound to, as an instance of the class {@link
     *         ViewGroup}. The parent may not be null
     * @param inflater
     *         The layout inflater, which should be used to inflate views, as an instance of the
     *         class {@link LayoutInflater}. The layout inflater may not be null
     */
    public AttachedViewRecycler(@NonNull final ViewGroup parent,
                                @NonNull final LayoutInflater inflater) {
        this(parent, inflater, null);
    }

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later
     * instead of inflating new instances. This constructor allows to specify a comparator, which
     * allows to determine the order, which should be used to add views to the parent.
     *
     * @param parent
     *         The parent, the recycler should be bound to, as an instance of the class {@link
     *         ViewGroup}. The parent may not be null
     * @param inflater
     *         The layout inflater, which should be used to inflate views, as an instance of the
     *         class {@link LayoutInflater}. The layout inflater may not be null
     * @param comparator
     *         The comparator, which allows to determine the order, which should be used to add
     *         views to the parent, as an instance of the type {@link Comparator} or null, if the
     *         views should be added in the order of their inflation
     */
    public AttachedViewRecycler(@NonNull final ViewGroup parent,
                                @NonNull final LayoutInflater inflater,
                                @Nullable final Comparator<ItemType> comparator) {
        ensureNotNull(parent, "The parent may not be null");
        ensureNotNull(inflater, "The layout inflater may not be null");
        this.context = inflater.getContext();
        this.inflater = inflater;
        this.parent = parent;
        this.comparator = comparator;
        this.activeViews = new HashMap<>();
        this.items = new ArrayList<>();
        this.logger = new Logger(LogLevel.INFO);
        this.adapter = null;
        this.unusedViews = null;
        this.useCache = true;
    }

    /**
     * Returns the adapter, which is used to inflate and adapt the appearance of views.
     *
     * @return The adapter, which is used to inflate and adapt the appearance of views, as an
     * instance of the class {@link Adapter} or null, if no adapter is set
     */
    @Nullable
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
     * Inflates the view, which is used to visualize a specific item, and adds it to the parent. If
     * possible an unused view will be retrieved from the cache instead of inflating a new
     * instance.
     *
     * @param item
     *         The item, which should be visualized by the inflated view, as an instance of the
     *         generic type ItemType. The item may not be null
     * @param params
     *         An array, which may contain optional parameters, as an array of the generic type
     *         ParamType or an empty array, if no optional parameters are available
     * @return True, if a new view has been inflated, false, if an unused view has been retrieved
     * from the cache
     */
    @SafeVarargs
    public final boolean inflate(@NonNull final ItemType item, @NonNull final ParamType... params) {
        ensureNotNull(params, "The array may not be null");
        ensureNotNull(adapter, "No adapter has been set", IllegalStateException.class);

        View view = getView(item);
        boolean inflated = false;

        if (view == null) {
            int viewType = adapter.getViewType(item);
            view = pollUnusedView(viewType);

            if (view == null) {
                view = adapter.onInflateView(inflater, parent, item, viewType, params);
                inflated = true;
                logger.logInfo(getClass(),
                        "Inflated view to visualize item " + item + " using view type " + viewType);
            } else {
                logger.logInfo(getClass(),
                        "Reusing view to visualize item " + item + " using view type " + viewType);
            }

            activeViews.put(item, view);
            int index;

            if (comparator != null) {
                index = Collections.binarySearch(items, item, comparator);

                if (index < 0) {
                    index = ~index;
                }
            } else {
                index = items.size();
            }

            items.add(index, item);
            parent.addView(view, index);
            logger.logDebug(getClass(), "Added view of item " + item + " at index " + index);
        }

        adapter.onShowView(context, view, item, params);
        logger.logDebug(getClass(), "Updated view of item " + item);
        return inflated;
    }

    /**
     * Removes a previously inflated view, which is used to visualize a specific item, from the
     * parent. If caching is enabled, the view will be put into a cache in order to be able to reuse
     * it later.
     *
     * @param item
     *         The item, which is visualized by the view, which should be removed, as an instance of
     *         the generic type ItemType. The item may not be null
     */
    public final void remove(@NonNull final ItemType item) {
        ensureNotNull(item, "The item may not be null");
        ensureNotNull(adapter, "No adapter has been set", IllegalStateException.class);
        int index = items.indexOf(item);

        if (index != -1) {
            items.remove(index);
            View view = activeViews.remove(item);
            adapter.onRemoveView(view, item);
            parent.removeViewAt(index);
            int viewType = adapter.getViewType(item);
            addUnusedView(view, viewType);
            logger.logInfo(getClass(), "Removed view of item " + item);
        } else {
            logger.logDebug(getClass(),
                    "Did not remove view of item " + item + ". View is not inflated");
        }
    }

    /**
     * Removes all previously inflated views. If caching is enabled, all of these views will be
     * added to a cache in order to be able to reuse them later.
     */
    public final void removeAll() {
        ensureNotNull(adapter, "No adapter has been set", IllegalStateException.class);

        for (int i = items.size() - 1; i >= 0; i--) {
            ItemType item = items.remove(i);
            View view = activeViews.remove(item);
            adapter.onRemoveView(view, item);
            parent.removeViewAt(i);
            int viewType = adapter.getViewType(item);
            addUnusedView(view, viewType);
        }

        logger.logInfo(getClass(), "Removed all views");
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
                "Cleared all unused views of view type " + viewType + " from cache");
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