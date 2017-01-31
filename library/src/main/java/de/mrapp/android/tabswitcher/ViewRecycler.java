package de.mrapp.android.tabswitcher;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * @author Michael Rapp
 */
class ViewRecycler<Type> {

    interface Adapter<Type> {

        @NonNull
        View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                           @NonNull Type item);

        void onShowView(@NonNull Context context, @NonNull View view, @NonNull Type item);

    }

    private final Adapter<Type> adapter;

    private final Context context;

    private final LayoutInflater inflater;

    private final ViewGroup parent;

    private final Comparator<Type> comparator;

    private final Map<Type, View> activeViews;

    private final List<Type> items;

    public ViewRecycler(@NonNull final ViewGroup parent, @NonNull final Adapter<Type> adapter) {
        this(parent, adapter, LayoutInflater.from(parent.getContext()));
    }

    public ViewRecycler(@NonNull final ViewGroup parent, @NonNull final Adapter<Type> adapter,
                        @Nullable final Comparator<Type> comparator) {
        this(parent, adapter, LayoutInflater.from(parent.getContext()), comparator);
    }

    public ViewRecycler(@NonNull final ViewGroup parent, @NonNull final Adapter<Type> adapter,
                        @NonNull final LayoutInflater inflater) {
        this(parent, adapter, inflater, null);
    }

    public ViewRecycler(@NonNull final ViewGroup parent, @NonNull final Adapter<Type> adapter,
                        @NonNull final LayoutInflater inflater,
                        @Nullable final Comparator<Type> comparator) {
        ensureNotNull(parent, "The parent may not be null");
        ensureNotNull(adapter, "The adapter may not be null");
        ensureNotNull(inflater, "The layout inflater may not be null");
        this.context = inflater.getContext();
        this.inflater = inflater;
        this.parent = parent;
        this.adapter = adapter;
        this.comparator = comparator;
        this.activeViews = new HashMap<>();
        this.items = comparator != null ? new ArrayList<Type>() : null;
    }

    public View inflate(@NonNull final Type item) {
        // TODO: Reuse view, if possible
        View view = activeViews.get(item);

        if (view == null) {
            view = adapter.onInflateView(inflater, parent, item);
            activeViews.put(item, view);
            adapter.onShowView(context, view, item);

            if (comparator != null) {
                int index = Collections.binarySearch(items, item, comparator);

                if (index < 0) {
                    index = ~index;
                }

                items.add(index, item);
                parent.addView(view, index);
            } else {
                parent.addView(view);
            }
        }

        return view;
    }

}