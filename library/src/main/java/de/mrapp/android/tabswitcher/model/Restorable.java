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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Defines the interface, a class, whose state should be stored and restored, must implement.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public interface Restorable {

    /**
     * Saves the current state.
     *
     * @param outState
     *         The bundle, which should be used to store the saved state, as an instance of the
     *         class {@link Bundle}. The bundle may not be null
     */
    void saveInstanceState(@NonNull Bundle outState);

    /**
     * Restores a previously saved state.
     *
     * @param savedInstanceState
     *         The saved state as an instance of the class {@link Bundle} or null, if no saved state
     *         is available
     */
    void restoreInstanceState(@Nullable Bundle savedInstanceState);

}