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

import de.mrapp.android.tabswitcher.TabSwitcher;

import static de.mrapp.android.util.Condition.ensureNotEmpty;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * The example app's main activity.
 *
 * @author Michael Rapp
 */
public class MainActivity extends AppCompatActivity {

    public final class Decorator implements TabSwitcher.Decorator {

        private CharSequence title;

        public Decorator(@NonNull final CharSequence title) {
            ensureNotNull(title, "The title may not be null");
            ensureNotEmpty(title, "The title may not be empty");
            this.title = title;
        }

        @NonNull
        @Override
        public final View inflateLayout(@NonNull final LayoutInflater inflater,
                                        @NonNull final ViewGroup parent) {
            View view = inflater.inflate(R.layout.tab, parent, false);
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(title);
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
        String tabTitle1 = "Tab 1";
        TabSwitcher.Tab tab1 = new TabSwitcher.Tab(tabTitle1, new Decorator(tabTitle1));
        tabSwitcher.addTab(tab1);
        String tabTitle2 = "Tab 2";
        TabSwitcher.Tab tab2 = new TabSwitcher.Tab(tabTitle2, new Decorator(tabTitle2));
        tabSwitcher.addTab(tab2);
        String tabTitle3 = "Tab 3";
        TabSwitcher.Tab tab3 = new TabSwitcher.Tab(tabTitle3, new Decorator(tabTitle3));
        tabSwitcher.addTab(tab3);
        String tabTitle4 = "Tab 4";
        TabSwitcher.Tab tab4 = new TabSwitcher.Tab(tabTitle4, new Decorator(tabTitle4));
        tabSwitcher.addTab(tab4);
        String tabTitle5 = "Tab 5";
        TabSwitcher.Tab tab5 = new TabSwitcher.Tab(tabTitle5, new Decorator(tabTitle5));
        tabSwitcher.addTab(tab5);
        String tabTitle6 = "Tab 6";
        TabSwitcher.Tab tab6 = new TabSwitcher.Tab(tabTitle6, new Decorator(tabTitle6));
        tabSwitcher.addTab(tab6);
        String tabTitle7 = "Tab 7";
        TabSwitcher.Tab tab7 = new TabSwitcher.Tab(tabTitle7, new Decorator(tabTitle7));
        tabSwitcher.addTab(tab7);
        String tabTitle8 = "Tab 8";
        TabSwitcher.Tab tab8 = new TabSwitcher.Tab(tabTitle6, new Decorator(tabTitle8));
        tabSwitcher.addTab(tab8);
    }

}