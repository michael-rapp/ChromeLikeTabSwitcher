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

import de.mrapp.android.tabswitcher.TabSwitcher;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A tag, which allows to store the properties of the tabs of a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class Tag implements Cloneable {

    /**
     * The position of the tab on the dragging axis.
     */
    private float position;

    /**
     * The state of the tab.
     */
    private State state;

    /**
     * True, if the tab is currently being closed, false otherwise
     */
    private boolean closing;

    /**
     * Creates a new tag, which allows to store the properties of the tabs of a {@link
     * TabSwitcher}.
     */
    public Tag() {
        setPosition(Float.NaN);
        setState(State.HIDDEN);
        setClosing(false);
    }

    /**
     * Returns the position of the tab on the dragging axis.
     *
     * @return The position of the tab as a {@link Float} value
     */
    public final float getPosition() {
        return position;
    }

    /**
     * Sets the position of the tab on the dragging axis.
     *
     * @param position
     *         The position, which should be set, as a {@link Float} value
     */
    public final void setPosition(final float position) {
        this.position = position;
    }

    /**
     * Returns the state of the tab.
     *
     * @return The state of the tab as a value of the enum {@link State}. The state may not be null
     */
    @NonNull
    public final State getState() {
        return state;
    }

    /**
     * Sets the state of the tab.
     *
     * @param state
     *         The state, which should be set, as a value of the enum {@link State}. The state may
     *         not be null
     */
    public final void setState(@NonNull final State state) {
        ensureNotNull(state, "The state may not be null");
        this.state = state;
    }

    /**
     * Returns, whether the tab is currently being closed, or not.
     *
     * @return True, if the tab is currently being closed, false otherwise
     */
    public final boolean isClosing() {
        return closing;
    }

    /**
     * Sets, whether the tab is currently being closed, or not.
     *
     * @param closing
     *         True, if the tab is currently being closed, false otherwise
     */
    public final void setClosing(final boolean closing) {
        this.closing = closing;
    }

    @Override
    public final Tag clone() {
        Tag clone;

        try {
            clone = (Tag) super.clone();
        } catch (ClassCastException | CloneNotSupportedException e) {
            clone = new Tag();
        }

        clone.position = position;
        clone.state = state;
        clone.closing = closing;
        return clone;
    }

}