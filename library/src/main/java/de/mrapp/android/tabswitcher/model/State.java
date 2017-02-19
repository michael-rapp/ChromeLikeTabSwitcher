package de.mrapp.android.tabswitcher.model;

/**
 * Contains all possible states of a tab, while the switcher is shown.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public enum State {

    /**
     * When the tab is part of the stack, which is located at the start of the switcher.
     */
    STACKED_START,

    /**
     * When the tab is displayed atop of the stack, which is located at the start of the switcher.
     */
    STACKED_START_ATOP,

    /**
     * When the tab is floating and freely movable.
     */
    FLOATING,

    /**
     * When the tab is part of the stack, which is located at the end of the switcher.
     */
    STACKED_END,

    /**
     * When the tab is currently not visible, i.e. if no view is inflated to visualize it.
     */
    HIDDEN

}