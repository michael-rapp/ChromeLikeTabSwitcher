# ChromeLikeTabSwitcher - RELEASE NOTES

## Version 0.1.5 (Nov. 25th 2017)

A bugfix release, which fixes the following issues:

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

- Fixed issues, when margins are applied to a `TabSwitcher`.

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