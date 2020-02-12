# ChromeLikeTabSwitcher - README

*THIS LIBRARY IS STILL WORK IN PROGRESS. THE LAYOUT OPTIMIZED FOR TABLETS IS NOT IMPLEMENTED YET.*

[![API-Level](https://img.shields.io/badge/API-14%2B-orange.svg)](https://android-arsenal.com/api?level=14) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-ChromeLikeTabSwitcher-brightgreen.svg?style=true)](https://android-arsenal.com/details/1/5654) [![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=X75YSLEJV3DWE)

"ChromeLikeTabSwitcher" is an Android-library, which provides a tab switcher similar to the one, which is used in the Android version of Google's Chrome browser. It provides layouts optimized for smartphone and tablet devices. The following GIFs illustrate the appearance of the library:

![](doc/images/smartphone_layout.gif)

The library provides the following features:

- Provides layouts optimized for smartphones and tablets.
- Tabs can dynamically be added and removed either programmatically or by using the UI. Different types of animations are available for said purpose.
- The tab switcher's state is automatically restored on configuration changes.
- In order to provide great performance, views are automatically recycled and the previews of tabs are rendered as bitmaps.
- The appearance of tabs, e.g. their background or title color, can be customized.
 
## License Agreement

This project is distributed under the Apache License version 2.0. For further information about this license agreement's content please refer to its full version, which is available at http://www.apache.org/licenses/LICENSE-2.0.txt.

## Download

The latest release of this library can be downloaded as a zip archive from the download section of the project's Github page, which is available [here](https://github.com/michael-rapp/ChromeLikeTabSwitcher/releases). Furthermore, the library's source code is available as a Git repository, which can be cloned using the URL https://github.com/michael-rapp/ChromeLikeTabSwitcher.git.

Alternatively, the library can be added to your Android app as a Gradle dependency by adding the following to the respective module's `build.gradle` file:

```groovy
dependencies {
    compile 'com.github.michael-rapp:chrome-like-tab-switcher:0.4.6'
}
```

## Examples

The library's tab switcher is implemented as a custom view `TabSwitcher`. It can be added to an activity or fragment by being declared programmatically or via a XML resource. The following XML code shows how the view can be added to a XML layout resource. A tab switcher should typically be shown fullscreen by setting the attributes `layout_width` and `layout_height` to `match_parent`. Furthermore, the view provides various custom attributes for customizing its appearance, which can as well be seen in the given example. 

```xml
<?xml version="1.0" encoding="utf-8"?>
<de.mrapp.android.tabswitcher.TabSwitcher 
        android:id="@+id/tab_switcher"
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:custom="http://schemas.android.com/apk/res-auto"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/tab_switcher_background_color"
        custom:layoutPolicy="auto"
        custom:tabIcon="@drawable/ic_file_outline_18dp"
        custom:tabIconTint="@android:color/black"
        custom:tabBackgroundColor="@color/tab_background_color"
        custom:tabContentBackgroundColor="@color/tab_content_background_color"
        custom:tabTitleTextColor="@color/tab_title_text_color"
        custom:tabCloseButtonIcon="@ic_close_18dp"
        custom:tabCloseBUttonIconTint="@android:color/black"
        custom:toolbarTitle="@string/tab_switcher_toolbar_title"
        custom:toolbarMenu="@menu/tab_switcher_toolbar_menu"
        custom:toolbarNavigationIcon="@drawable/ic_add_box_24dp"
        custom:toolbarNavigationIconTint="@android:color/white"/>
```

When instantiating a `TabSwitcher` programmatically, the following Java code can be used. For all XML attributes shown in the example above, corresponding setter methods are available. 

```java
TabSwitcher tabSwitcher = new TabSwitcher(context);
tabSwitcher.setBackground(ContextCompat.getColor(context, R.color.tab_switcher_background_color));
tabSwitcher.setLayoutPolicy(LayoutPolicy.AUTO);
tabSwitcher.setTabIcon(R.drawable.ic_file_outline_18dp);
tabSwitcher.setTabIconTint(ContextCompat.getColor(context, android.R.color.black));
tabSwitcher.setTabBackgroundColor(ContextCompat.getColor(context, R.color.tab_background_color));
tabSwitcher.setTabContentBackgroundColor(ContextCompat.getColor(context, R.color.tab_content_background_color));
tabSwitcher.setTabTitleTextColor(ContextCompat.getColor(context, R.color.tab_title_text_color));
tabSwitcher.setTabCloseButtonIcon(R.drawable.ic_close_18dp);
tabSwitcher.setTabCloseButtonIcon(ContextCompat.getColor(context, android.R.color.black));
tabSwitcher.setToolbarTitle(context.getString(R.string.tab_switcher_toolbar_title));
tabSwitcher.inflateToolbarMenu(R.menu.tab_switcher_toolbar_menu, null);
tabSwitcher.setToolbarNavigationIcon(R.drawable.ic_add_box_white_24dp, null);
tabSwitcher.setToolbarNavigationIconTint(ContextCompat.getColor(context, android.R.color.black));
```

The tabs, which are contained by a `TabSwitcher` are represented by instances of the class `Tab`. The following Java code demonstrates, how a new tab can be created and added to a tab switcher. By setting a custom icon, background color, title color etc., the defaults, which are applied to the `TabSwitcher` can be overridden for that particular tab. The `setParameters`-method allows to associate a tab with a `Bundle`, which may contain additional information about the tab. By implementing the interface `Tab.Callback` and registering an instance at a `Tab` by using its `addCallback`-method, it can be observed, when the properties of a tab has been changed. 

```java
Tab tab = new Tab("Title");
tab.setCloseable(true);
tab.setIcon(R.drawable.ic_file_outline_18dp);
tab.setIconTint(ContextCompat.getColor(context, androidR.color.black));
tab.setBackgroundColor(ContextCompat.getColor(context, R.color.tab_background_color));
tab.setContentBackgroundColor(ContextCompat.getColor(context, R.color.tab_content_background_color));
tab.setTitleTextColor(ContextCompat.getColor(context, R.color.tab_title_text_color));
tab.setCloseButtonIcon(R.drawable.ic_close_18dp);
tab.setCloseButtonIconTint(ContextCompat.getColor(context, android.R.color.black));
tab.setParameters(new Bundle());
tab.addCallback(new Tab.Callback() { /* ... */ });
tabSwitcher.addTab(tab);
```

In order to specify how the tabs of a `TabSwitcher` should look like, the abstract class `TabSwitcherDecorator` must be overridden and an instance of the implementing class must be applied to the tab switcher by using its `setDecorator`-method. This is very similar to the paradigm of adapters commonly used in Android developing for populating a `ListView`, `RecyclerView`, etc. Each custom implementation of the class `TabSwitcherDecorator` must override the `onInflateView`- and `onShowTab`-method. The first one is used to inflate the view, which should be used by a tab, the latter allows to customize the appearance of the inflated view, depending on the current state. Within the scope of the `onShowTab`-method the decorator's `findViewById`-method can be used to reference views. It uses a built-in view holder for better performance.

If different views should be inflated for different tabs, the `getViewTypeCount`- and `getViewType`-methods must be overridden as well. The first one should return the total number of different views, which are inflated by the `onInflateView`-method, the latter one must return a distinct integer value, which specifies the view type of a specific tab. The following code illustrates how the class `TabSwitcherDecorator` can be implemented.

Furthermore, the class `TabSwitcherDecorator` enables to override the `onSaveInstanceState`-method in order to store the current state of a tab within a `Bundle`. When the tab is shown again, the `Bundle` will be passed to the `onShowTab`-method in order to be able to restore the previously saved state. If you want to take care of clearing saved state by yourself, false can be passed to the `clearSavedStatesWhenRemovingTabs`-method. To manually clear the saved state of a single tab, the `clearSavedState`-method can be used. For clearing all saved states, the `clearAllSavedStates`-method is provided.


```java
class Decorator extends TabSwitcherDecorator {

    @NonNull
    @Override
    public View onInflateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup parent, int viewType) {
        if (viewType == 0) {
            return inflater.inflate(R.layout.tab_view_1, parent, false);
        } else {
            return inflater.inflate(R.layout.tab_view_2, parent, false);
        }
    }

    @Override
    public void onShowTab(@NonNull Context context, @NonNull TabSwitcher tabSwitcher, 
                          @NonNull View view, @NonNull Tab tab, int index, int viewType,
                          @Nullable Bundle savedInstanceState) {
        if (viewType == 0) {
            TextView textView = findViewById(R.id.text_view);
            textView.setText(tab.getTitle());
        } else {
            Button button = findViewById(R.id.button);
            button.setText(tab.getTitle());
        }
        
        if (savedInstanceState != null) {
            // Restore the tab's state if necessary
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getViewType(@NonNull Tab tab, int index) {
        Bundle parameters = tab.getParameters();
        return parameters != null ? parameters.getInt("view_type") : 0;
    }
    
    @Override
    public void onSaveInstanceState(@NonNull View view, @NonNull Tab tab, int index,
                                    int viewType, @NonNull Bundle outState) {
        // Store the tab's current state in the Bundle outState if necessary
    }

}
```

In order to apply a decorator to a `TabSwitcher` its `setDecorator`-method must be used as shown below. If no decorator has been set, an `IllegalStateException` will be thrown as soon as the view should become visible.

```java
tabSwitcher.setDecorator(new Decorator());
```

In order to observe the state of a `TabSwitcher`, the interface `TabSwitcherListener` listener can be implemented. The interface provides methods, which are invoked, when tabs are added to or removed from a tab switcher, or if the tab switcher has been hidden or shown (only when using the smartphone layout). Instances of the type `TabSwitcherListener` can be added by using a `TabSwitcher`'s `addListener`-method. In order to observe, when the close button of a tab has been clicked, the interface `TabCloseListener` can be implemented and added by using the `addTabCloseListener`-method accordingly.

## Stateful Decorators

As an alternative of the class `TabSwitcherDecorator`, the class `StatefulTabSwitcherDecorator` can be used. It is an extension of the class `TabSwitcherDecorator`, which allows to maintain arbitrary states for a `TabSwitcher`'s tab (it is recommended to use the class `AbstractState` as a base class though). This is for example useful, if list items are shown within a tab and a reference to the used adapter should be stored to easy the modification of the list items. The states are kept even if the corresponding tab is currently not shown. However, they are not restored after orientation changes and may be vanished when the device is low on memory.

To create states, they must be returned by the decorator's `onCreateState`-method. When a state is about to be deleted, the `onClearState`-method is invoked. 

By default, the states of tabs are automatically clear, when the corresponding tab is removed. However, when passing `false` to the `clearSavedStatesWhenRemovingTabs`-method, this mechanism is turned off. To manually remove states, the `clearState`- and `clearAllStates`-methods can be used.

The library's example app illustrates how the class `StatefulTabSwitcherDecorator` can be used for asynchronouly loading list items, which are shown in a tab using an `ArrayAdapter` and a `Listener`. Please refer to its code for further information.

## Using Animations

The class `TabSwitcher` provides various methods to add or remove one or several tabs. If the tab switcher is currently shown, tabs are added or removed in an animated manner. In order to use custom animations, an instance of the class `Animation` can be passed to the methods.

### Swipe Animation

When using the smartphone layout, a `SwipeAnimation` is used to add or remove tabs by default. It causes tabs to be swiped horizontally (or vertically in landscape mode). By specifying a value of the enum `SwipeAnimation.SwipeDirection`, it can be specified, whether the tab should be moved to/from the left or right (respectively to/from the top or bottom in landscape mode). The following code sample illustrates how instances of the class `SwipeAnimation` can be created by using a builder.
 
```java
Animation animation = new SwipeAnimation.Builder().setDuration(2000)
        .setInterpolator(new LinearInterpolator()).setDirection(SwipeAnimation.SwipeDirection.LEFT)
        .create();
```

![](doc/images/swipe_animation.gif)

### Reveal Animation

When using the smartphone layout, a `RevealAnimation` can be used to add a single tab. Starting at a specific position, the size of the tab will be animated until it is shown fullscreen. Using a `RevealAnimation` causes the tab switcher to become hidden and the added tab is selected automatically. The following code shows, how a `RevealAnimation` can be instantiated. All of the builder's setter methods are optional. If they are not called, default values are used.

```java
Animation animation = new RevealAnimation.Builder().setDuration(2000)
        .setInterpolator(new LinearInterpolator().setX(20).setY(50)
        .create();
```

![](doc/images/reveal_animation.gif)

### PeekAnimation

A `PeekAnimation` can be used to add a tab, if the smartphone layout is used and when the switcher is currently not shown. Similar to a `RevealAnimation`, the size of the added tab is animated, starting at a specific position. The tab is then shown at the bottom (or right in landscape mode) of the tab switcher for a short time. Unlike a `RevealAnimation`, a `PeekAnimation` does not cause the added tab to become selected. Its purpose is to give a preview of the added tab, while another tab is still shown fullscreen. This corresponds to the animation, which is used in the Google Chrome browser when opening a link in a new tab. A `PeekAnimation` can be created by using the builder pattern as shown below.

```java
Animation animation = new PeekAnimation.Builder().setDuration(2000)
        .setInterpolator(new LinearInterpolator().setX(20).setY(50)
        .create();
```

![](doc/images/peek_animation.gif)

# Using drag gestures

The library provides multiple drag gestures, which can be used to perform certain actions. They can be set to a `TabSwitcher` by using its `addDragGesture`-method. If a previously added drag gesture should be removed, the `removeDragGesture`-method can be used accordingly. Usually, a onscreen area is specified for each gesture in order to specify the area, where drag gestures should be detected. In the following, the available drag gestures are discussed.

## SwipeGesture

Swipe gestures allow to switch between neighboring tabs, when the tab switcher is not currently shown, by swiping horizontally. Such gestures are represented by instances of the class `SwipeGesture`. In order to create a swipe gesture the builder pattern can be used as shown below. The `setTouchableArea` method call is used to specify the onscreen area, which should be taken into account for recognizing the gesture, the `setAnimationDuration`-method allows to set the duration of the swipe animation and the `setThreshold`-method call sets the distance in pixels, the gesture must last until it is recognized. All of these method calls  are optional.

```java
DragGesture gesture = new SwipeGesture.Builder().setTouchableArea(0, 0, 200, 100)
        .setAnimationDuration(1000L).setThreshold(10)
        .create();
```

![](doc/images/swipe_gesture.gif)

## PullDownGesture

A `PullDownGesture` can be used on smartphones to show the tab switcher by pulling down from the top. It does not have any effects when using the tablet layout. Instances of the class `PullDownGesture` can be created as follows.

```java
DragGesture gesture = new PullDownGesture.Builder().setTouchableArea(0, 0, 200, 100)
        .setThreshold(10)
        .create();
```

![](doc/images/pull_down_gesture.gif)

## Toolbars and Menus

The view `TabSwitcher`, which is provided by the library, allows to show a toolbar. By default, the toolbar is always hidden. In order to show it, the `showToolbars`-method must be used. When using the smartphone layout, the toolbar is shown when the tab switcher is currently shown, or if no tabs are contained by the tab switcher. When using the tablet layout, two toolbars - one to the left and one to the right of the tabs - are always shown. The toolbars can be referenced by using the `getToolbars`-method. It returns an array, which contains the layout's toolbars. When using the smartphone layout, only one `Toolbar` is contained by the array, when using the tablet layout, the left one is contained at index 0 and the right one is contained at index 1.

The class `TabSwitcher` provides a few methods, which allow to set the toolbar's title, navigation icon and menu. The `setToolbarTitle`-method allows to set a title. When using the tablet layout, the title is applied to the left toolbar. The `setToolbarNavigationIcon`-method allows to specify the navigation icon of the toolbar as well as a listener which is invoked when the icon is clicked. When using the tablet layout, the navigation icon is applied to the left toolbar. In order to add a menu to a `TabSwitcher`'s toolbar, the `inflateToolbarMenu`-method can be used. Besides the resource id of the menu resource, which should be applied, it also allows to specify a listener, which is notified when a menu item is clicked.

In order to provide a button, similar to the one, which is used in Google's Chrome browser, which shows the total number of tabs contained by a `TabSwitcher` and allows to toggle the visibility of the tab switcher, the class `TabSwitcherButton` is exposed by the library. It implements a custom `ImageButton`, which implements the interface `TabSwitcherListener` in order to keep the displayed number of tabs up-to-date. The appearance of the button is given by the class `TabSwitcherDrawable`. If a `TabSwitcherButton` should be used as part of a toolbar menu, it must be included in a menu resource as shown in the following XML code.

```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:app="http://schemas.android.com/apk/res-auto">

    <item
        android:id="@+id/toggle_tab_switcher_menu_item"
        android:title="@string/toggle_tab_switcher_menu_item"
        app:actionLayout="@layout/tab_switcher_menu_item"
        app:showAsAction="ifRoom"/>

</menu>
```

In order to register a menu's `TabSwitcherButton` as a listener of a `TabSwitcher`, the static `setupWithMenu`-method can be used. It automatically registers all items of a `Menu`, that use a `TabSwitcherButton`, as listeners of a specific `TabSwitcher`. The `OnClickListener`, which can optionally be specified, is invoked when one of these buttons is clicked. The following code shows, how the method can be used together with an arbitrary menu.

```java
TabSwitcher.setupWithMenu(tabSwitcher, menu, new OnClickListener() { /* ... */ });
```
If the menu, which is part of the tab switcher itself, should be set up, the following method call can be used. 
```java
TabSwitcher.setupWithMenu(tabSwitcher, new OnClickListener() { /* ... */ });
```

## Using Themes

By default, a light theme is used by the library's `TabSwitcher`. However, the library comes with a predefined dark theme `TabSwitcher` in addition to the theme `TabSwitcher.Light`. The appearance of the dark theme (on a smartphone in landscape mode) is shown in the screenshot below. 

![](doc/images/dark_theme.png)

Themes can be applied globally to all tab switchers by specifying the following attributes in the app's theme as shown below. The attributes `tabSwitcherThemePhone` and `tabSwitcherThemeTablet` allow to specify different themes for the smartphone and tablet layout. These attributes take priority over the attribute `tabSwitcherThemeGlobal`, which specifies the theme regardless of the used layout.

```xml
<style name="AppTheme" parent="@style/Theme.AppCompat.Light.NoActionBar">
        <item name="tabSwitcherThemeGlobal">@style/TabSwitcher</item>
        <item name="tabSwitcherThemePhone">@style/TabSwitcher.Light</item>
        <item name="tabSwitcherThemeTablet">@style/TabSwitcher</item>
</style>
```

The same attributes are also available to be applied to a single tab switcher, when defined as part of a XML layout:

```xml
<?xml version="1.0" encoding="utf-8"?>
<de.mrapp.android.tabswitcher.TabSwitcher 
        android:id="@+id/tab_switcher"
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:custom="http://schemas.android.com/apk/res-auto"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/tab_switcher_background_color"
        custom:themeGlobal="@style/TabSwitcher"
        custom:themePhone="@style/TabSwitcher.Light"
        custom:themeTablet="@style/TabSwitcher"/>
```

The attributes, which are available when defining a tab switcher in a XML layout resource, can also be used when extending the predefined themes. All of them are shown in the following example. The attribute `tabSwitcherToolbarPopupTheme` enables to force the tab switcher's toolbar to use a dark theme for its popup menu.

```xml
<style name="MyTabSwitcher" parent="@style/TabSwitcher.Light">
        <item name="tabSwitcherBackground">@color/tab_switcher_background_color</item>
        <item name="tabSwitcherLayoutPolicy">auto</item>
        <item name="tabSwitcherTabIcon">@drawable/tab_switcher_tab_icon</item>
        <item name="tabSwitcherTabBackgroundColor">@color/tab_switcher_tab_background_color</item>
        <item name="tabSwitcherTabContentBackgroundColor">@color/tab_switcher_tab_content_background_color</item>
        <item name="tabSwitcherTabTitleTextColor">@color/tab_switcher_tab_title_text_color</item>
        <item name="tabSwitcherTabCloseButtonIcon">@drawable/tab_switcher_tab_close_button_icon</item>
        <item name="tabSwitcherToolbarTitle">@string/tab_switcher_toolbar_title</item>
        <item name="tabSwitcherToolbarMenu">@menu/tab_switcher_toolbar_menu</item>
        <item name="tabSwitcherToolbarNavigationIcon">@drawable/tab_switcher_toolbar_navigation_icon</item>
        <item name="tabSwitcherToolbarPopupTheme">@style/ThemeOverlay.AppCompat.Dark</item>
</style>
```

As an alternative to creating a custom theme, the theme attribute shown above can also be applied globally by including them in the global app theme. This causes them to take priority over the attributes of the theme, which is applied to a tab switcher. 

## Showing a Placeholder when TabSwitcher is empty

The class `TabSwitcher` provides `setEmptyView`-methods, which can be used to specify a custom view, which is shown, when the tab switcher is empty. The specified view is shown or hidden by using a fade in, respectively a fade out, animation. The duration of these animations can be specified optionally. The first possibility to use the `setEmptyView`-methods is to specify an instance of the class `android.view.View`:

```java
View view = // ... inflate view
tabSwitcher.setEmptyView(view); 
// or tabSwitcher.setEmptyView(view, 1000L) if an animation duration should be specified
```

Alternatively, the `setEmpyView`-methods can be used by specifying the layout resource id of a view:

```java
tabSwitcher.setEmptyView(R.layout.empty_view); 
// or tabSwitcher.setEmptyView(R.layout.empty_view, 1000L) if an animation duration should be specified
```

An example of a placeholder view being shown, when the tab switcher is empty, can be seen in the screenshot below.

![](doc/images/empty_view.png)

## Padding

The view `TabSwitcher` overrides the `setPadding`-methods of the class `View` in order to apply the padding to all tabs as well as to their parent view. The main purpose of this behavior is to apply window insets, when using a translucent status and/or navigation bar as it can be seen in the library's example app. The following code sample demonstrates, how the window insets of an activity can be applied to a tab switcher by using a `OnApplyWindowInsetsListener`. It is meant to be used in the activity's `onCreate`-method.

```java
ViewCompat.setOnApplyWindowInsetsListener(tabSwitcher, new OnApplyWIndowInsetsListener() {

    @Override
    public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
        tabSwitcher.setPadding(insets.getSystemWindowInsetsLeft(), 
                insets.getSystemWindowInsetsTop(),
                insets.getSystemWindowInsetsRight(),
                insets.getSystemWindowInsetsBottom());
        return insets;
    }

});
```

If the padding of a `TabSwitcher` should not be applied to the content of tabs, `false` can be passed to the `applyPaddingToTabs`-method. This prevents the content of tabs from being inset, but does not affect the position of tabs or the position of a `TabSwitcher`'s toolbar(s). This might be useful, if you want to take care of the padding, which is applied to tabs by yourself, e.g. when the content should scroll below the system's status and/or navigation bar.

## Contact information

For personal feedback or questions feel free to contact me via the mail address, which is mentioned on my [Github profile](https://github.com/michael-rapp). If you have found any bugs or want to post a feature request please use the [bugtracker](https://github.com/michael-rapp/ChromeLikeTabSwitcher/issues) to report them.
