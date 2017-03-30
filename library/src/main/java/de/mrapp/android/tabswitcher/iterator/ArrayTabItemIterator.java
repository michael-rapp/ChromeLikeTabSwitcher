package de.mrapp.android.tabswitcher.iterator;

import android.support.annotation.NonNull;

import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.view.AttachedViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An iterator, which allows to iterate the tab items, which correspond to the tabs, which are
 * contained by an array.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class ArrayTabItemIterator extends AbstractTabItemIterator {

    /**
     * A builder, which allows to configure an create instances of the class {@link
     * ArrayTabItemIterator}.
     */
    public static class Builder extends AbstractBuilder<Builder, ArrayTabItemIterator> {

        /**
         * The view recycler, which allows to inflate the views, which are used to visualize the
         * tabs, which are iterated by the iterator, which is created by the builder.
         */
        private final AttachedViewRecycler<TabItem, ?> viewRecycler;

        /**
         * The array, which contains the tabs, which are iterated by the iterator, which is created
         * by the builder.
         */
        private final Tab[] array;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * ArrayTabItemIterator}.
         *
         * @param viewRecycler
         *         The view recycler, which allows to inflate the views, which are used to visualize
         *         the tabs, which should be iterated by the iterator, as an instance of the class
         *         {@link AttachedViewRecycler}. The view recycler may not be null
         * @param array
         *         The array, which contains the tabs, which should be iterated by the iterator, as
         *         an array of the type {@link Tab}. The array may not be null
         */
        public Builder(@NonNull final AttachedViewRecycler<TabItem, ?> viewRecycler,
                       @NonNull final Tab[] array) {
            ensureNotNull(viewRecycler, "The view recycler may not be null");
            ensureNotNull(array, "The array may not be null");
            this.viewRecycler = viewRecycler;
            this.array = array;
        }

        @NonNull
        @Override
        public ArrayTabItemIterator create() {
            return new ArrayTabItemIterator(viewRecycler, array, reverse, start);
        }

    }

    /**
     * A factory, which allows to create instances of the class {@link Builder}.
     */
    public static class Factory implements AbstractTabItemIterator.Factory {

        /**
         * The view recycler, which is used by the builders, which are created by the factory.
         */
        private final AttachedViewRecycler<TabItem, ?> viewRecycler;

        /**
         * The array, which is used by the builders, which are created by the factory.
         */
        private final Tab[] array;

        /**
         * Creates a new factory, which allows to create instances of the class {@link Builder}.
         *
         * @param viewRecycler
         *         The view recycler, which should be used by the builders, which are created by the
         *         factory, as an instance of the class {@link AttachedViewRecycler}. The view
         *         recycler may not be null
         * @param array
         *         The array, which should be used by the builders, which are created by the
         *         factory, as an array of the type {@link Tab}. The array may not be null
         */
        public Factory(@NonNull final AttachedViewRecycler<TabItem, ?> viewRecycler,
                       @NonNull final Tab[] array) {
            ensureNotNull(viewRecycler, "The view recycler may not be null");
            ensureNotNull(array, "The array may not be null");
            this.viewRecycler = viewRecycler;
            this.array = array;
        }

        @NonNull
        @Override
        public AbstractBuilder<?, ?> create() {
            return new Builder(viewRecycler, array);
        }

    }

    /**
     * The view recycler, which allows to inflate the views, which are used to visualize the
     * iterated tabs.
     */
    private final AttachedViewRecycler<TabItem, ?> viewRecycler;

    /**
     * The array, which contains the tabs, which are iterated by the iterator.
     */
    private final Tab[] array;

    /**
     * Creates a new iterator, which allows to iterate the tab items, whcih correspond to the tabs,
     * which are contained by an array.
     *
     * @param viewRecycler
     *         The view recycler, which allows to inflate the views, which are used to visualize the
     *         iterated tabs, as an instance of the class {@link AttachedViewRecycler}. The view
     *         recycler may not be null
     * @param array
     *         The array, which contains the tabs, which should be iterated by the iterator, as an
     *         array of the type {@link Tab}. The array may not be null
     * @param reverse
     *         True, if the tabs should be iterated in reverse order, false otherwise
     * @param start
     *         The index of the first tab, which should be iterated, as an {@link Integer} value or
     *         -1, if all tabs should be iterated
     */
    public ArrayTabItemIterator(@NonNull final AttachedViewRecycler<TabItem, ?> viewRecycler,
                                @NonNull final Tab[] array, final boolean reverse,
                                final int start) {
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        ensureNotNull(array, "The array may not be null");
        this.viewRecycler = viewRecycler;
        this.array = array;
        initialize(reverse, start);
    }

    @Override
    public final int getCount() {
        return array.length;
    }

    @NonNull
    @Override
    public final TabItem getItem(final int index) {
        return TabItem.create(viewRecycler, index, array[index]);
    }

}
