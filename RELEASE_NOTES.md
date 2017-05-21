# ChromeLikeTabSwitcher - RELEASE NOTES

## Version 0.1.2 (May 22th 2017)

A bugfix release, which fixes the following issues:

- Resolved issues when restoring the positions of tabs after orientation changes or when resuming the app. 

## Version 0.1.1 (May 11th 2017)

A bugfix release, which fixes the following issues:

- Improved detection of click events

## Version 0.1.0 (Apr. 22th 2017)

The first unstable release of the library, which provides the following features:

- Provides a layout optimized for smartphones. The layout is adapted depending on whether it is displayed in landscape or portrait mode 
- Tabs can dynamically be added and removed in an animated manner using a `SwipeAnimation`, `RevealAnimation` or `PeekAnimation`
- The tab switcher's state is automatically restored on configuration changes
- Views are recycled and previews are rendered as bitmaps in order to increase the performance