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

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * A view holder, which allows to keep references to the views a tab of a {@link TabSwitcher}
 * consists of.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabViewHolder {

    /**
     * The view group, which contains the title and close button of a tab.
     */
    public ViewGroup titleContainer;

    /**
     * The text view, which is used to display the title of a tab.
     */
    public TextView titleTextView;

    /**
     * The close button of a tab.
     */
    public ImageButton closeButton;

    /**
     * The view group, which contains the child view of a tab.
     */
    public ViewGroup childContainer;

    /**
     * The child view, which contains the tab's content.
     */
    public View child;

    /**
     * The image view, which is used to display the preview of a tab.
     */
    public ImageView previewImageView;

    /**
     * The view, which is used to display a border around the preview of a tab.
     */
    public View borderView;

}