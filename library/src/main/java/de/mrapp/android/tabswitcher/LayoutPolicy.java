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
package de.mrapp.android.tabswitcher;

/**
 * Contains all possible layout policies of a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public enum LayoutPolicy {

    /**
     * If the layout should automatically adapted, depending on whether the device is a smartphone
     * or tablet.
     */
    AUTO(0),

    /**
     * If the smartphone layout should be used, regardless of the device.
     */
    PHONE(1),

    /**
     * If the tablet layout should be used, regardless of the device.
     */
    TABLET(2);

    /**
     * The value of the layout policy.
     */
    private int value;

    /**
     * Creates a new layout policy.
     *
     * @param value
     *         The value of the layout policy as an {@link Integer} value
     */
    LayoutPolicy(final int value) {
        this.value = value;
    }

    /**
     * Returns the value of the layout policy.
     *
     * @return The value of the layout policy as an {@link Integer} value
     */
    public final int getValue() {
        return value;
    }

    /**
     * Returns the layout policy, which corresponds to a specific value.
     *
     * @param value
     *         The value of the layout policy, which should be returned, as an {@link Integer}
     *         value
     * @return The layout policy, which corresponds to the given value, as a value of the enum
     * {@link LayoutPolicy}
     */
    public static LayoutPolicy fromValue(final int value) {
        for (LayoutPolicy layoutPolicy : values()) {
            if (layoutPolicy.getValue() == value) {
                return layoutPolicy;
            }
        }

        throw new IllegalArgumentException("Invalid enum value: " + value);
    }

}