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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * The example app's main activity.
 *
 * @author Michael Rapp
 */
public class MainActivity extends AppCompatActivity implements TabSwitcher.Listener {

    public final class Decorator extends TabSwitcher.Decorator {

        @NonNull
        @Override
        public View onInflateView(@NonNull final LayoutInflater inflater,
                                  @NonNull final ViewGroup parent, final int viewType) {
            return inflater.inflate(R.layout.tab, parent, false);
        }

        @Override
        public void onShowTab(@NonNull final Context context,
                              @NonNull final TabSwitcher tabSwitcher, @NonNull final View view,
                              @NonNull final Tab tab, final int viewType) {
            TextView textView = (TextView) view.findViewById(android.R.id.title);
            textView.setText(tab.getTitle());
        }

    }

    /**
     * The number of tabs, which are contained by the example app's tab switcher.
     */
    private static final int TAB_COUNT = 12;

    /**
     * The activity's tab switcher.
     */
    private TabSwitcher tabSwitcher;

    private OnClickListener createAddTabListener() {
        return new OnClickListener() {

            @Override
            public void onClick(View view) {

            }

        };
    }

    private OnMenuItemClickListener createToolbarMenuListener() {
        return new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toggle_tab_switcher_menu_item:
                        tabSwitcher.toggleSwitcherVisibility();
                        return true;
                    case R.id.clear_tabs_menu_item:
                        tabSwitcher.clear();
                        return true;
                    default:
                        return false;
                }
            }

        };
    }

    private OnClickListener createUndoSnackbarListener() {
        return new OnClickListener() {

            @Override
            public void onClick(final View view) {

            }

        };
    }

    @Override
    public final void onSwitcherShown(@NonNull final TabSwitcher tabSwitcher) {

    }

    @Override
    public final void onSwitcherHidden(@NonNull final TabSwitcher tabSwitcher) {

    }

    @Override
    public final void onSelectionChanged(@NonNull final TabSwitcher tabSwitcher,
                                         final int selectedTabIndex,
                                         @Nullable final Tab selectedTab) {
        if (selectedTab != null) {
            CharSequence text = getString(R.string.selection_changed_toast, selectedTab.getTitle());
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public final void onTabAdded(@NonNull final TabSwitcher tabSwitcher, final int index,
                                 @NonNull final Tab tab) {

    }

    @Override
    public final void onTabRemoved(@NonNull final TabSwitcher tabSwitcher, final int index,
                                   @NonNull final Tab tab) {
        CharSequence text = getString(R.string.removed_tab_snackbar, tab.getTitle());
        Snackbar.make(tabSwitcher, text, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, createUndoSnackbarListener()).show();
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final TabSwitcher tabSwitcher) {
        CharSequence text = getString(R.string.cleared_tabs_snackbar);
        Snackbar.make(tabSwitcher, text, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, createUndoSnackbarListener()).show();
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tab, menu);
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
        tabSwitcher.addListener(this);
        tabSwitcher.showToolbar(true);
        tabSwitcher.inflateToolbarMenu(R.menu.tab_switcher, createToolbarMenuListener());
        tabSwitcher
                .setToolbarNavigationIcon(R.drawable.ic_add_box_white_24dp, createAddTabListener());

        for (int i = 1; i <= TAB_COUNT; i++) {
            CharSequence title = getString(R.string.tab_title, i);
            Tab tab = new Tab(title);
            tab.setIcon(R.drawable.ic_file_outline_18dp);
            tabSwitcher.addTab(tab);
        }
    }

}