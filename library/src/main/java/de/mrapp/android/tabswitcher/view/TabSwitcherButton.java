package de.mrapp.android.tabswitcher.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.widget.ImageButton;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.drawable.TabSwitcherDrawable;
import de.mrapp.android.util.ThemeUtil;
import de.mrapp.android.util.ViewUtil;

/**
 * @author Michael Rapp
 */
public class TabSwitcherButton extends ImageButton {

    private void initialize() {
        setImageDrawable(new TabSwitcherDrawable(getContext()));
        ViewUtil.setBackground(this,
                ThemeUtil.getDrawable(getContext(), R.attr.selectableItemBackgroundBorderless));
        setContentDescription(null);
        setClickable(true);
        setFocusable(true);
    }

    public TabSwitcherButton(@NonNull final Context context) {
        this(context, null);
    }

    public TabSwitcherButton(@NonNull final Context context,
                             @Nullable final AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize();
    }

    public TabSwitcherButton(@NonNull final Context context,
                             @Nullable final AttributeSet attributeSet,
                             @AttrRes final int defaultStyleAttribute) {
        super(context, attributeSet, defaultStyleAttribute);
        initialize();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TabSwitcherButton(@NonNull final Context context,
                             @Nullable final AttributeSet attributeSet,
                             @AttrRes final int defaultStyleAttribute,
                             @StyleRes final int defaultStyleResource) {
        super(context, attributeSet, defaultStyleAttribute, defaultStyleResource);
        initialize();
    }

}