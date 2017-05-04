# ChromeLikeTabSwitcher - RELEASE NOTES

## Version 1.0.0 (May 2nd 2017)

The first stable release of the library. It introduces the following features:

- A layout optimized for tablets is now provided
- Added predefined dark and light themes
- Added the `tabContentBackgroundColor` XML attribute and according setter methods for customizing the background color of a tab's content
- Added the layout policy `TABLET_LANDSCAPE`. It allows to use the tablet layout on smartphones, when in landscape mode
- Fixed various minor issues

## Version 0.1.0 (Apr. 22th 2017)

The first unstable release of the library, which provides the following features:

- Provides a layout optimized for smartphones. The layout is adapted depending on whether it is displayed in landscape or portrait mode 
- Tabs can dynamically be added and removed in an animated manner using a `SwipeAnimation`, `RevealAnimation` or `PeekAnimation`
- The tab switcher's state is automatically restored on configuration changes
- Views are recycled and previews are rendered as bitmaps in order to increase the performance