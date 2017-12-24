# ChromeLikeTabSwitcher - RELEASE NOTES

## Version 1.0.0 (Nov. 26th 2017)

The first stable release of the library. It introduces the following features:

- A layout, which is optimized for tablets, is now provided
- Added predefined dark and light themes
- Added support for drag gestures. So far, the drag gestures `SwipeGesture` and `PullDownGesture` are provided
- Added the `tabContentBackgroundColor` XML attribute and according setter methods for customizing the background color of a tab's content
- The background color of tabs is now adapted when pressed, if a `ColorStateList` with state `android:state_pressed` is set
- Added the layout policy `TABLET_LANDSCAPE`. It allows to use the tablet layout on smartphones, when in landscape mode
- Added the possibility to show a certain placeholder view, when a `TabSwitcher` is empty
- Added the functionality to show a circular progress bar instead of an icon for individual tabs.
- Fixed various minor issues

## Version 0.1.7 (Dec. 24th 2017)

A minor release, which introduces the following changes:

- Added an additional `setupWithMenu`-method for setting up the menu of a `TabSwitcher`. If necessary, it uses a `OnGlobalLayoutListener` internally.

## Version 0.1.6 (Dec. 23th 2017)

A bugfix release, which fixes the following issues:

- If a `Tab`, which is added to a `TabSwitcher`, happens to have the same hash code as a previously removed `Tab`, the state of the removed tab is not restored anymore.

## Version 0.1.5 (Nov. 25th 2017)

A bugfix release, which fixes the following issues:

- Fixed a crash which might occurred when resuming the activity after it was in the background for a long time (see https://github.com/michael-rapp/ChromeLikeTabSwitcher/issues/7).
- Updated dependency "AndroidUtil" to version 1.18.2.
- Updated AppCompat v7 support library to version 27.0.1.
- Updated AppCompat annotations support library to version 27.0.1.

## Version 0.1.4 (Oct. 2nd 2017)

A bugfix release, which fixes the following issues:

- Fixed an issue in the example app, which caused the contents of `EditText` widgets to be shown in the wrong tabs.
- Updated `targetSdkVersion` to API level 26.
- Updated dependency "AndroidUtil" to version 1.18.0.
- Updated AppCompat v7 support library to version 26.1.0.
- Updated AppCompat annotations support library to version 26.1.0.

## Version 0.1.3 (May 23th 2017)

A bugfix release, which fixes the following issues:

- Fixed issues, when margins are applied to a `TabSwitcher`

## Version 0.1.2 (May 22th 2017)

A bugfix release, which fixes the following issues:

- Resolved issues when restoring the positions of tabs after orientation changes or when resuming the app

## Version 0.1.1 (May 11th 2017)

A bugfix release, which fixes the following issues:

- Improved detection of click events

## Version 0.1.0 (Apr. 22th 2017)

The first unstable release of the library, which provides the following features:

- Provides a layout optimized for smartphones. The layout is adapted depending on whether it is displayed in landscape or portrait mode 
- Tabs can dynamically be added and removed in an animated manner using a `SwipeAnimation`, `RevealAnimation` or `PeekAnimation`
- The tab switcher's state is automatically restored on configuration changes
- Views are recycled and previews are rendered as bitmaps in order to increase the performance