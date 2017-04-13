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
package de.mrapp.android.tabswitcher.layout.phone;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractTabViewHolder;

/**
 * A view holder, which allows to store references to the views, a tab of a {@link TabSwitcher}
 * consists of, when using the smartphone layout.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class PhoneTabViewHolder extends AbstractTabViewHolder {

    /**
     * The view group, which contains the title and close button of a tab.
     */
    public ViewGroup titleContainer;

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