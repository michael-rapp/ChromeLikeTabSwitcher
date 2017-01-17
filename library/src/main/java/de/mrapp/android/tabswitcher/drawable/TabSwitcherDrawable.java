package de.mrapp.android.tabswitcher.drawable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import de.mrapp.android.tabswitcher.R;

/**
 * @author Michael Rapp
 */
public class TabSwitcherDrawable extends LayerDrawable {

    public TabSwitcherDrawable(@NonNull final Context context) {
        super(new Drawable[]{
                ContextCompat.getDrawable(context, R.drawable.tab_switcher_menu_item_background)});
    }

}