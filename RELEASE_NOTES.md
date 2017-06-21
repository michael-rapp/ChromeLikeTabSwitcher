# ChromeLikeTabSwitcher - RELEASE NOTES

## Version 1.0.0 (May 11th 2017)

The first stable release of the library. It introduces the following features:

- A layout, which is optimized for tablets, is now provided
- Added predefined dark and light themes
- Added support for drag gestures. So far, the drag gestures `SwipeGesture` and `PullDownGesture` are provided
- Added the `tabContentBackgroundColor` XML attribute and according setter methods for customizing the background color of a tab's content
- The background color of tabs is now adapted when pressed, if a `ColorStateList` with state `android:state_pressed` is set
- Added the layout policy `TABLET_LANDSCAPE`. It allows to use the tablet layout on smartphones, when in landscape mode
- Added the possibility to show a certain placeholder view, when a `TabSwitcher` is empty
- Fixed various minor issues

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