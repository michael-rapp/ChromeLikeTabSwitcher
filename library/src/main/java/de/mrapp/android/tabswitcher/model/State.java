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
package de.mrapp.android.tabswitcher.model;

import android.support.annotation.NonNull;
import android.util.SparseArray;

/**
 * The state of a tab, while the tab switcher is shown.
 *
 * This class provides factory methods for creating different states. The states, which are returned
 * by these methods, are singletons.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public abstract class State {

    /**
     * An abstract base class for all states, which correspond to tabs, which are part of a stack.
     */
    private abstract static class AbstractStackedState extends State {

        /**
         * The index of the stack, the tab belongs to.
         */
        private final int stackIndex;

        /**
         * Creates a new state, which corresponds to a tab, which is part of a stack.
         *
         * @param stackIndex
         *         The index of the stack, the tab belongs to, as an {@link Integer} value
         */
        AbstractStackedState(final int stackIndex) {
            this.stackIndex = stackIndex;
        }

        /**
         * Returns the index of the stack, the tab belongs to.
         *
         * @return The index of the stack, the tab belongs to, as an {@link Integer} value
         */
        public final int getStackIndex() {
            return stackIndex;
        }

    }

    /**
     * A state, which corresponds to a tab, which is located at the top of a stack.
     */
    public static class StackedAtopState extends AbstractStackedState {

        /**
         * Creates a new state, which corresponds to a tab, which is located at the top of a stack.
         *
         * @param stackIndex
         *         The index of the stack, the tab belongs to, as an {@link Integer} value
         */
        private StackedAtopState(final int stackIndex) {
            super(stackIndex);
        }

        @Override
        public String toString() {
            return "STACKED_ATOP{stackIndex=" + getStackIndex() + "}";
        }

    }

    /**
     * A state, which corresponds to a tab, which is located at the start of a stack.
     */
    public static class StackedStartState extends AbstractStackedState {

        /**
         * Creates a new state, which corresponds to a tab, which is located at the start of a
         * stack.
         *
         * @param stackIndex
         *         The index of the stack, the tab belongs to, as an {@link Integer} value
         */
        private StackedStartState(final int stackIndex) {
            super(stackIndex);
        }

        @Override
        public String toString() {
            return "STACKED_START{stackIndex=" + getStackIndex() + "}";
        }

    }

    /**
     * A state, which corresponds to a tab, which is located at the end of a stack.
     */
    public static class StackedEndState extends AbstractStackedState {

        /**
         * Creates a new state, which corresponds to a tab, which is located at the end of a stack.
         *
         * @param stackIndex
         *         The index of the stack, the tab belongs to, as an {@link Integer} value
         */
        private StackedEndState(final int stackIndex) {
            super(stackIndex);
        }

        @Override
        public String toString() {
            return "STACKED_END{stackIndex=" + getStackIndex() + "}";
        }

    }

    /**
     * A state, which corresponds to a floating tab.
     */
    public static class FloatingState extends State {

        @Override
        public final String toString() {
            return "FLOATING";
        }

    }

    /**
     * A state, which corresponds to a hidden tab.
     */
    public static class HiddenState extends State {

        @Override
        public final String toString() {
            return "HIDDEN";
        }

    }

    /**
     * A sparse array, which contains all states of the type {@link StackedAtopState}, indexed by
     * the index of the stack, they belong to.
     */
    private static SparseArray<StackedAtopState> stackedAtopStates;

    /**
     * A sparse array, which contains all states of the type {@link StackedStartState}, indexed by
     * the index of the stack, they belong to.
     */
    private static SparseArray<StackedStartState> stackedStartStates;

    /**
     * A sparse array, which contains all states of the type {@link StackedEndState}, indexed by the
     * index of the stack, they belong to.
     */
    private static SparseArray<StackedEndState> stackedEndStates;

    /**
     * The state of the type {@link FloatingState}.
     */
    private static FloatingState floatingState;

    /**
     * The state of the type {@link HiddenState}.
     */
    private static HiddenState hiddenState;

    /**
     * Creates and returns a state, which corresponds to a tab, which is located at the top of the
     * first stack.
     *
     * @return The state, which has been created, as an instance of the class {@link State}. The
     * state may not be null
     */
    @NonNull
    public static State stackedAtop() {
        return stackedAtop(0);
    }

    /**
     * Creates and returns a state, which corresponds to a tab, which is located at the top of a
     * stack.
     *
     * @param stackIndex
     *         The index of the stack, the tab belongs to, as an {@link Integer} value
     * @return The state, which has been created, as an instance of the class {@link State}. The
     * state may not be null
     */
    @NonNull
    public static State stackedAtop(final int stackIndex) {
        if (stackedAtopStates == null) {
            stackedAtopStates = new SparseArray<>();
        }

        StackedAtopState state = stackedAtopStates.get(stackIndex);

        if (state == null) {
            state = new StackedAtopState(stackIndex);
            stackedAtopStates.put(stackIndex, state);
        }

        return state;
    }

    /**
     * Creates and returns a state, which corresponds to a tab, which is located at the start of the
     * first stack.
     *
     * @return The state, which has been created, as an instance of the class {@link State}. The
     * state may not be null
     */
    @NonNull
    public static State stackedStart() {
        return stackedStart(0);
    }

    /**
     * Creates and returns a state, which corresponds to a tab, which is located at the start of a
     * stack.
     *
     * @param stackIndex
     *         The index of the stack, the tab belongs to, as an {@link Integer} value
     * @return The state, which has been created, as an instance of the class {@link State}. The
     * state may not be null
     */
    @NonNull
    public static State stackedStart(final int stackIndex) {
        if (stackedStartStates == null) {
            stackedStartStates = new SparseArray<>();
        }

        StackedStartState state = stackedStartStates.get(stackIndex);

        if (state == null) {
            state = new StackedStartState(stackIndex);
            stackedStartStates.put(stackIndex, state);
        }

        return state;
    }

    /**
     * Creates and returns a state, which corresponds to a tab, which is located at the end of the
     * first stack.
     *
     * @return The state, which has been created, as an instance of the class {@link State}. The
     * state may not be null
     */
    @NonNull
    public static State stackedEnd() {
        return stackedEnd(0);
    }

    /**
     * Creates and returns a state, which corresponds to a tab, which is located at the end of a
     * stack.
     *
     * @param stackIndex
     *         The index of the stack, the tab belongs to, as an {@link Integer} value
     * @return The tab, which has been created, as an instance of the class {@link State}. The state
     * may not be null
     */
    @NonNull
    public static State stackedEnd(final int stackIndex) {
        if (stackedEndStates == null) {
            stackedEndStates = new SparseArray<>();
        }

        StackedEndState state = stackedEndStates.get(stackIndex);

        if (state == null) {
            state = new StackedEndState(stackIndex);
            stackedEndStates.put(stackIndex, state);
        }

        return state;
    }

    /**
     * Creates and returns a state, which corresponds to a floating tab.
     *
     * @return The state, which has been created, as an instance of the class {@link State}. The
     * state may not be null
     */
    @NonNull
    public static State floating() {
        if (floatingState == null) {
            floatingState = new FloatingState();
        }

        return floatingState;
    }

    /**
     * Creates and returns a state, which corresponds to a hidden tab.
     *
     * @return The state, which has been created, as an instance of the class {@link State}. The
     * state may not be null
     */
    @NonNull
    public static State hidden() {
        if (hiddenState == null) {
            hiddenState = new HiddenState();
        }

        return hiddenState;
    }

}