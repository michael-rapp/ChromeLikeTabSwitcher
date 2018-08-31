# ChromeLikeTabSwitcher - RELEASE NOTES

## Version 0.3.4 (Sep. 1st 2018)

A bugfix release, which fixes the following issues:

- Fixed an infinite recursion when calling a `TabSwitcher`'s `clearAllSavedStates`-method.

## Version 0.3.3 (Aug. 5th 2018)

A bugfix release, which fixes the following issues:

- Fixed a crash when a `TabSwitcher`'s `onSaveInstanceState`-method is called before the view is laid out. This might happen on some devices when the app is started in landscape mode.

## Version 0.3.2 (May 20th 2018)

A bugfix release, which fixes the following issues:

- Fixed a crash on device with API level 16 (see https://github.com/michael-rapp/ChromeLikeTabSwitcher/pull/16).

## Version 0.3.1 (May 11th 2018)

A minor release, which introduces the following changes:

- Increased the size of the `TabSwitcherDrawable` to enhance consistency with other menu items.

## Version 0.3.0 (May 5th 2018)

A feature release, which introduces the following changes:

- It is now possible to use vector drawables for the navigation icon of a `TabSwitcher`'s toolbar as well as for the icon and close button icon of tabs (even on pre-Lollipop devices).
- Updated AppCompat v7 support library to version 27.1.1
- Updated AppCompat annotations support library to version 27.1.1
- Updated dependency "AndroidUtil" to version 1.20.3.
- Updated dependency "AndroidMaterialViews" to version 2.1.11.

## Version 0.2.9 (Feb. 15th 2018)

A bugfix release, which fixes the following issues:

- Fixed a crash of the example app on tablets.
- Updated the dependency "AndroidUtil" to version 1.20.1.

## Version 0.2.8 (Feb. 4th 2018)

A bugfix release, which fixes the following issues:

- Implemented an alternative fix regarding the issue of previews not being loaded, which does not come with a performance loss. Previews are now again rendered in background threads.

## Version 0.2.7 (Feb. 4th 2018)

A bugfix release, which fixes the following issues:

- Improved the fix in the last release regarding previews not being loaded. Previews are now entirely rendered on the UI thread. 

## Version 0.2.6 (Feb. 4th 2018)

A bugfix release, which fixes the following issues:

- Fixed previews of tabs not able to be loaded when certain child views (e.g. `AppBarLayout`s) are contained by the tabs.
- Fixed a crash when storing the state of a TabSwitcher, if the switcher is still shown after all tabs have been removed.

## Version 0.2.5 (Jan. 28th 2018)

A bugfix release, which fixes the following issues:

- Fixed an issue, which caused states of a `StatefulTabSwitcherDecorator` to be cleared prematurely.

## Version 0.2.4 (Jan. 27th 2018)

A minor release, which introduces the following changes:

- Added the class `StatefulTabSwitcherDecorator`.
- The saved states of tabs are now cleared when the corresponding tab is removed by default. This can be turned of by using the `clearSavedStatesWhenRemovingTabs`-method. 
- Fade animations can now be used to show the previews of tabs when using the smartphone layout.
- Updated `targetSdkVersion` to API level 27 (Android 8.1).
- Updated dependency "AndroidUtil" to version 1.19.0.
- Updated dependency "AndroidMaterialViews" to version 2.1.10.
- The data structure `ListenerList` is now used for managing event listeners.

## Version 0.2.3 (Jan. 23th 2018)

A bugfix release, which fixes the following issues:

- Added additional method parameter to the interface `Model.Listener`. It allows to reliably determine, whether the selection changed when adding or removing tabs.

## Version 0.2.2 (Jan. 14th 2018)

A bugfix release, which fixes the following issues:

- `TabSwitcherButton`s are now rendered properly in the preview of tabs.
- Added the attribute `applyPaddingToTabs`. 

## Version 0.2.1 (Jan. 10th 2018)

A bugfix release, which fixes the following issues:

- Overshooting towards the end as well as the start is now possible when using the phone layout, if only one tab is contained by a `TabSwitcher`
- Fixed the navigation icon of a `TabSwitcher`'s toolbar not being shown

## Version 0.2.0 (Dec. 30th 2017)

A major release, which introduces the following features:

- Added predefined dark and light themes
- Added support for drag gestures. So far, the drag gestures `SwipeGesture` and `PullDownGesture` are provided
- Added the `tabContentBackgroundColor` XML attribute and according setter methods for customizing the background color of a tab's content
- The background color of tabs is now adapted when pressed, if a `ColorStateList` with state `android:state_pressed` is set
- Added the possibility to show a certain placeholder view, when a `TabSwitcher` is empty
- Added the functionality to show a circular progress bar instead of an icon for individual tabs.
- Updated dependency "AndroidUtil" to version 1.18.3
- Updated AppCompat v7 support library to version 27.0.2
- Updated AppCompat annotations support library to version 27.0.2
- Added dependency "AndroidMaterialViews" with version 2.1.9

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