package de.mrapp.android.tabswitcher;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
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

    private final LayoutInflater inflater;

    private final Map<Type, View> activeViews;

    public ViewRecycler(@NonNull final LayoutInflater inflater,
                        @NonNull final Adapter<Type> adapter) {
        ensureNotNull(inflater, "The layout inflater may not be null");
        ensureNotNull(adapter, "The adapter may not be null");
        this.inflater = inflater;
        this.adapter = adapter;
        this.activeViews = new HashMap<>();

    }

    public View inflate(@NonNull final Type item, @Nullable final ViewGroup parent) {
        // TODO: Reuse view, if possible
        View view = activeViews.get(item);

        if (view == null) {
            view = adapter.onInflateView(inflater, parent, item);
            adapter.onShowView(inflater.getContext(), view, item);
            activeViews.put(item, view);
        }

        return view;
    }

}