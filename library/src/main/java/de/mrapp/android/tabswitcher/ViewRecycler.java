package de.mrapp.android.tabswitcher;

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
 * @author Michael Rapp
 */
public class ViewRecycler<ItemType, ParamType> {

    public static abstract class Adapter<ItemType, ParamType> {

        @SuppressWarnings("unchecked")
        @NonNull
        public abstract View onInflateView(@NonNull final LayoutInflater inflater,
                                           @Nullable final ViewGroup parent,
                                           @NonNull final ItemType item, final int viewType,
                                           @NonNull final ParamType... params);

        @SuppressWarnings("unchecked")
        public abstract void onShowView(@NonNull final Context context, @NonNull final View view,
                                        @NonNull final ItemType item,
                                        @NonNull final ParamType... params);

        public void onRemoveView(@NonNull final View view, @NonNull final ItemType item) {

        }

        public int getViewTypeCount() {
            return 1;
        }

        public int getViewType(@NonNull final ItemType item) {
            return 0;
        }

    }

    private final Adapter<ItemType, ParamType> adapter;

    private final Context context;

    private final LayoutInflater inflater;

    private final ViewGroup parent;

    private final Comparator<ItemType> comparator;

    private final Map<ItemType, View> activeViews;

    private final SparseArray<Queue<View>> unusedViews;

    private final List<ItemType> items;

    private final Logger logger;

    private boolean useCache;

    private void addUnusedView(@NonNull final View view, final int viewType) {
        if (useCache) {
            Queue<View> queue = unusedViews.get(viewType);

            if (queue == null) {
                queue = new LinkedList<>();
                unusedViews.put(viewType, queue);
            }

            queue.add(view);
        }
    }

    @Nullable
    private View pollUnusedView(final int viewType) {
        if (useCache) {
            Queue<View> queue = unusedViews.get(viewType);

            if (queue != null) {
                return queue.poll();
            }
        }

        return null;
    }

    public ViewRecycler(@NonNull final ViewGroup parent,
                        @NonNull final Adapter<ItemType, ParamType> adapter) {
        this(parent, adapter, LayoutInflater.from(parent.getContext()));
    }

    public ViewRecycler(@NonNull final ViewGroup parent,
                        @NonNull final Adapter<ItemType, ParamType> adapter,
                        @Nullable final Comparator<ItemType> comparator) {
        this(parent, adapter, LayoutInflater.from(parent.getContext()), comparator);
    }

    public ViewRecycler(@NonNull final ViewGroup parent,
                        @NonNull final Adapter<ItemType, ParamType> adapter,
                        @NonNull final LayoutInflater inflater) {
        this(parent, adapter, inflater, null);
    }

    public ViewRecycler(@NonNull final ViewGroup parent,
                        @NonNull final Adapter<ItemType, ParamType> adapter,
                        @NonNull final LayoutInflater inflater,
                        @Nullable final Comparator<ItemType> comparator) {
        ensureNotNull(parent, "The parent may not be null");
        ensureNotNull(adapter, "The adapter may not be null");
        ensureNotNull(inflater, "The layout inflater may not be null");
        this.context = inflater.getContext();
        this.inflater = inflater;
        this.parent = parent;
        this.adapter = adapter;
        this.comparator = comparator;
        this.activeViews = new HashMap<>();
        this.unusedViews = new SparseArray<>(adapter.getViewTypeCount());
        this.items = new ArrayList<>();
        this.logger = new Logger(LogLevel.INFO);
        this.useCache = true;
    }

    public final LogLevel getLogLevel() {
        return logger.getLogLevel();
    }

    public final void setLogLevel(@NonNull final LogLevel logLevel) {
        logger.setLogLevel(logLevel);
    }

    @SafeVarargs
    public final boolean inflate(@NonNull final ItemType item, @NonNull final ParamType... params) {
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

    public final void remove(@NonNull final ItemType item) {
        ensureNotNull(item, "The item may not be null");
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

    public final void removeAll() {
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

    @Nullable
    public final View getView(@NonNull final ItemType item) {
        ensureNotNull(item, "The item may not be null");
        return activeViews.get(item);
    }

    public final boolean isInflated(@NonNull final ItemType item) {
        return getView(item) != null;
    }

    public final void clearCache() {
        unusedViews.clear();
        logger.logDebug(getClass(), "Cleared cache");
    }

    public final boolean isCacheUsed() {
        return useCache;
    }

    public final void useCache(final boolean useCache) {
        this.useCache = useCache;

        if (!useCache) {
            clearCache();
        }
    }

}