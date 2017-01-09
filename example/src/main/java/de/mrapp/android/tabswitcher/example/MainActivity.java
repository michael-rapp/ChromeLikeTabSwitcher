/*
 * Copyright 2016 Michael Rapp
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
package de.mrapp.android.tabswitcher.example;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * The example app's main activity.
 *
 * @author Michael Rapp
 */
public class MainActivity extends AppCompatActivity {

    public final class Decorator implements TabSwitcher.Decorator {

        @NonNull
        @Override
        public final View inflateLayout(@NonNull final LayoutInflater inflater,
                                        @NonNull final ViewGroup parent, @NonNull final Tab tab) {
            View view = inflater.inflate(R.layout.tab, parent, false);
            TextView textView = (TextView) view.findViewById(android.R.id.title);
            textView.setText(tab.getTitle());
            return view;
        }

    }

    /**
     * The activity's tab switcher.
     */
    private TabSwitcher tabSwitcher;

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_tab_menu_item:
                tabSwitcher.showSwitcher();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tabSwitcher = (TabSwitcher) findViewById(R.id.tab_switcher);
        tabSwitcher.setDecorator(new Decorator());
        String tabTitle1 = "Tab 1";
        Tab tab1 = new Tab(tabTitle1);
        tab1.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab1);
        String tabTitle2 = "Tab 2";
        Tab tab2 = new Tab(tabTitle2);
        tab2.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab2);
        String tabTitle3 = "Tab 3";
        Tab tab3 = new Tab(tabTitle3);
        tab3.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab3);
        String tabTitle4 = "Tab 4";
        Tab tab4 = new Tab(tabTitle4);
        tab4.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab4);
        String tabTitle5 = "Tab 5";
        Tab tab5 = new Tab(tabTitle5);
        tab5.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab5);
        String tabTitle6 = "Tab 6";
        Tab tab6 = new Tab(tabTitle6);
        tab6.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab6);
        String tabTitle7 = "Tab 7";
        Tab tab7 = new Tab(tabTitle7);
        tab7.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab7);
        String tabTitle8 = "Tab 8";
        Tab tab8 = new Tab(tabTitle8);
        tab8.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab8);
        String tabTitle9 = "Tab 9";
        Tab tab9 = new Tab(tabTitle9);
        tab9.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab9);
        String tabTitle10 = "Tab 10";
        Tab tab10 = new Tab(tabTitle10);
        tab10.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab10);
        String tabTitle11 = "Tab 11";
        Tab tab11 = new Tab(tabTitle11);
        tab11.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab11);
        String tabTitle12 = "Tab 12";
        Tab tab12 = new Tab(tabTitle12);
        tab12.setIcon(R.drawable.ic_file_outline_18dp);
        tabSwitcher.addTab(tab12);
    }

}