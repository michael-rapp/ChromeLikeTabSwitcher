/*
 * Copyright 2016 - 2019 Michael Rapp
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package de.mrapp.android.tabswitcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import de.mrapp.util.Condition;
import de.mrapp.util.datastructure.ListenerList;

/**
 * A tab, which can be added to a {@link TabSwitcher} widget. It has a title, as well as an optional
 * icon. Furthermore, it is possible to set a custom color and to specify, whether the tab should be
 * closeable, or not.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class Tab implements Parcelable {

    /**
     * The name of the parameter, which specifies, whether the tab was already shown in a {@link
     * TabSwitcher}, or not. This parameter should be not be set or modified manually.
     */
    public static final String WAS_SHOWN_PARAMETER = Tab.class.getName() + "::wasShown";

    /**
     * A creator, which allows to create instances of the class {@link Tab} from parcels.
     */
    public static final Creator<Tab> CREATOR = new Creator<Tab>() {

        @Override
        public Tab createFromParcel(final Parcel source) {
            return new Tab(source);
        }

        @Override
        public Tab[] newArray(final int size) {
            return new Tab[size];
        }

    };

    /**
     * Defines the interface, a class, which should be notified, when a tab's properties have been
     * changed, must implement.
     */
    public interface Callback {

        /**
         * The method, which is invoked, when the tab's title has been changed.
         *
         * @param tab
         *         The observed tab as an instance of the class {@link Tab}. The tab may not be
         *         null
         */
        void onTitleChanged(@NonNull Tab tab);

        /**
         * The method, which is invoked, when the tab's icon has been changed.
         *
         * @param tab
         *         The observed tab as an instance of the class {@link Tab}. The tab may not be
         *         null
         */
        void onIconChanged(@NonNull Tab tab);

        /**
         * The method, which is invoked, when it has been changed, whether the tab is closeable, or
         * not.
         *
         * @param tab
         *         The observed tab as an instance of the class {@link Tab}. The tab may not be
         *         null
         */
        void onCloseableChanged(@NonNull Tab tab);

        /**
         * The method, which is invoked, when the icon of the tab's close button has been changed.
         *
         * @param tab
         *         The observed tab as an instance of the class {@link Tab}. The tab may not be
         *         null
         */
        void onCloseButtonIconChanged(@NonNull Tab tab);

        /**
         * The method, which is invoked, when the tab's background color has been changed.
         *
         * @param tab
         *         The observed tab as an instance of the class {@link Tab}. The tab may not be
         *         null
         */
        void onBackgroundColorChanged(@NonNull Tab tab);

        /**
         * The method, which is invoked, when the background color of the tab's content has been
         * changed.
         *
         * @param tab
         *         The observed tab as an instance of the class {@link Tab}. The tab may not be
         *         null
         */
        void onContentBackgroundColorChanged(@NonNull Tab tab);

        /**
         * The method, which is invoked, when the text color of the tab's title has been changed.
         *
         * @param tab
         *         The observed tab as an instance of the class {@link Tab}. The tab may not be
         *         null
         */
        void onTitleTextColorChanged(@NonNull Tab tab);

        /**
         * The method, which is invoked, when the visibility of the tab's progress bar has been
         * changed.
         *
         * @param tab
         *         The observed tab as an instance of the class {@link Tab}. The tab may not be
         *         null
         */
        void onProgressBarVisibilityChanged(@NonNull Tab tab);

        /**
         * The method, which is invoked, when the color of the tab's progress bar has been changed.
         *
         * @param tab
         *         The observed tab as an instance of the class {@link Tab}. The tab may not be
         *         null
         */
        void onProgressBarColorChanged(@NonNull Tab tab);

    }

    /**
     * A set, which contains the callbacks, which have been registered to be notified, when the
     * tab's properties have been changed.
     */
    private final ListenerList<Callback> callbacks = new ListenerList<>();

    /**
     * The tab's title.
     */
    private CharSequence title;

    /**
     * The resource id of the tab's icon.
     */
    private int iconId;

    /**
     * The tab's icon as a bitmap.
     */
    private Bitmap iconBitmap;

    /**
     * The color state list, which is used to tint the tab's icon.
     */
    private ColorStateList iconTintList;

    /**
     * The mode, which is used to tint the tab's icon.
     */
    private PorterDuff.Mode iconTintMode;

    /**
     * True, if the tab is closeable, false otherwise.
     */
    private boolean closeable;

    /**
     * The resource id of the icon of the tab's close button.
     */
    private int closeButtonIconId;

    /**
     * The bitmap of the icon of the tab's close button.
     */
    private Bitmap closeButtonIconBitmap;

    /**
     * The color state list, which is used to tint the tab's close button.
     */
    private ColorStateList closeButtonIconTintList;

    /**
     * The mode, which is used to tint the tab's close button.
     */
    private PorterDuff.Mode closeButtonIconTintMode;

    /**
     * The background color of the tab.
     */
    private ColorStateList backgroundColor;

    /**
     * The background color of the tab's content.
     */
    private int contentBackgroundColor;

    /**
     * The text color of the tab's title.
     */
    private ColorStateList titleTextColor;

    /**
     * True, if the tab's progress bar is shown, false otherwise.
     */
    private boolean progressBarShown;

    /**
     * The color of the tab's progress bar.
     */
    private int progressBarColor;

    /**
     * Optional parameters, which are associated with the tab.
     */
    private Bundle parameters;

    /**
     * Notifies all callbacks, that the tab's title has been changed.
     */
    private void notifyOnTitleChanged() {
        for (Callback callback : callbacks) {
            callback.onTitleChanged(this);
        }
    }

    /**
     * Notifies all callbacks, that the tab's icon has been changed.
     */
    private void notifyOnIconChanged() {
        for (Callback callback : callbacks) {
            callback.onIconChanged(this);
        }
    }

    /**
     * Notifies all callbacks, that it has been changed, whether the tab is closeable, or not.
     */
    private void notifyOnCloseableChanged() {
        for (Callback callback : callbacks) {
            callback.onCloseableChanged(this);
        }
    }

    /**
     * Notifies all callbacks, that the icon of the tab's close button has been changed.
     */
    private void notifyOnCloseButtonIconChanged() {
        for (Callback callback : callbacks) {
            callback.onCloseButtonIconChanged(this);
        }
    }

    /**
     * Notifies all callbacks, that the background color of the tab has been changed.
     */
    private void notifyOnBackgroundColorChanged() {
        for (Callback callback : callbacks) {
            callback.onBackgroundColorChanged(this);
        }
    }

    /**
     * Notifies all callbacks, that the background color of the tab's content has been changed.
     */
    private void notifyOnContentBackgroundColorChanged() {
        for (Callback callback : callbacks) {
            callback.onContentBackgroundColorChanged(this);
        }
    }

    /**
     * Notifies all callbacks, that the text color of the tab has been changed.
     */
    private void notifyOnTitleTextColorChanged() {
        for (Callback callback : callbacks) {
            callback.onTitleTextColorChanged(this);
        }
    }

    /**
     * Notifies all callbacks, that the visibility of the tab's progress bar has been changed.
     */
    private void notifyOnProgressBarVisibilityChanged() {
        for (Callback callback : callbacks) {
            callback.onProgressBarVisibilityChanged(this);
        }
    }

    /**
     * Notifies all callbacks, that the color of the tab's progress bar has been changed.
     */
    private void notifyOnProgressBarColorChanged() {
        for (Callback callback : callbacks) {
            callback.onProgressBarColorChanged(this);
        }
    }

    /**
     * Creates a new tab, which can be added to a {@link TabSwitcher} widget.
     *
     * @param source
     *         The parcel, the tab should be created from, as an instance of the class {@link
     *         Parcel}. The parcel may not be null
     */
    private Tab(@NonNull final Parcel source) {
        this.title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        this.iconId = source.readInt();
        this.iconBitmap = source.readParcelable(getClass().getClassLoader());
        this.iconTintList = source.readParcelable(getClass().getClassLoader());
        this.iconTintMode = (PorterDuff.Mode) source.readSerializable();
        this.closeable = source.readInt() > 0;
        this.closeButtonIconId = source.readInt();
        this.closeButtonIconBitmap = source.readParcelable(getClass().getClassLoader());
        this.closeButtonIconTintList = source.readParcelable(getClass().getClassLoader());
        this.closeButtonIconTintMode = (PorterDuff.Mode) source.readSerializable();
        this.backgroundColor = source.readParcelable(getClass().getClassLoader());
        this.contentBackgroundColor = source.readInt();
        this.titleTextColor = source.readParcelable(getClass().getClassLoader());
        this.progressBarShown = source.readInt() > 0;
        this.progressBarColor = source.readInt();
        this.parameters = source.readBundle(getClass().getClassLoader());
    }

    /**
     * Creates a new tab, which can be added to a {@link TabSwitcher} widget.
     *
     * @param title
     *         The tab's title as an instance of the type {@link CharSequence}. The title may not be
     *         neither be null, nor empty
     */
    public Tab(@NonNull final CharSequence title) {
        setTitle(title);
        this.closeable = true;
        this.closeButtonIconId = -1;
        this.closeButtonIconBitmap = null;
        this.closeButtonIconTintList = null;
        this.closeButtonIconTintMode = null;
        this.iconId = -1;
        this.iconBitmap = null;
        this.iconTintList = null;
        this.iconTintMode = null;
        this.backgroundColor = null;
        this.contentBackgroundColor = -1;
        this.titleTextColor = null;
        this.progressBarShown = false;
        this.progressBarColor = -1;
        this.parameters = null;
    }

    /**
     * Creates a new tab, which can be added to a {@link TabSwitcher} widget.
     *
     * @param context
     *         The context, which should be used, as an instance of the class {@link Context}. The
     *         context may not be null
     * @param resourceId
     *         The resource id of the tab's title as an {@link Integer} value. The resource id must
     *         correspond to a valid string resource
     */
    public Tab(@NonNull final Context context, @StringRes final int resourceId) {
        this(context.getString(resourceId));
    }

    /**
     * Returns the tab's title.
     *
     * @return The tab's title as an instance of the type {@link CharSequence}. The title may
     * neither be null, nor empty
     */
    @NonNull
    public final CharSequence getTitle() {
        return title;
    }

    /**
     * Sets the tab's title.
     *
     * @param title
     *         The title, which should be set, as an instance of the type {@link CharSequence}. The
     *         title may neither be null, nor empty
     */
    public final void setTitle(@NonNull final CharSequence title) {
        Condition.INSTANCE.ensureNotNull(title, "The title may not be null");
        Condition.INSTANCE.ensureNotEmpty(title, "The title may not be empty");
        this.title = title;
        notifyOnTitleChanged();
    }

    /**
     * Sets the tab's title.
     *
     * @param context
     *         The context, which should be used, as an instance of the class {@link Context}. The
     *         context may not be null
     * @param resourceId
     *         The resource id of the title, which should be set, as an {@link Integer} value. The
     *         resource id must correspond to a valid string resource
     */
    public final void setTitle(@NonNull final Context context, @StringRes final int resourceId) {
        setTitle(context.getText(resourceId));
    }

    /**
     * Returns the tab's icon.
     *
     * @param context
     *         The context, which should be used, as an instance of the class {@link Context}. The
     *         context may not be null
     * @return The tab's icon as an instance of the class {@link Drawable} or null, if no custom
     * icon is set
     */
    @Nullable
    public final Drawable getIcon(@NonNull final Context context) {
        Condition.INSTANCE.ensureNotNull(context, "The context may not be null");

        if (iconId != -1) {
            return AppCompatResources.getDrawable(context, iconId);
        } else {
            return iconBitmap != null ? new BitmapDrawable(context.getResources(), iconBitmap) :
                    null;
        }
    }

    /**
     * Sets the tab's icon.
     *
     * @param resourceId
     *         The resource id of the icon, which should be set, as an {@link Integer} value. The
     *         resource id must correspond to a valid drawable resource
     */
    public final void setIcon(@DrawableRes final int resourceId) {
        this.iconId = resourceId;
        this.iconBitmap = null;
        notifyOnIconChanged();
    }

    /**
     * Sets the tab's icon.
     *
     * @param icon
     *         The icon, which should be set, as an instance of the class {@link Bitmap} or null, if
     *         no custom icon should be set
     */
    public final void setIcon(@Nullable final Bitmap icon) {
        this.iconId = -1;
        this.iconBitmap = icon;
        notifyOnIconChanged();
    }

    /**
     * Returns the color state list, which is used to tint the tab's icon.
     *
     * @return The color state list, which is used to tint the tab's icon, as an instance of the
     * class {@link ColorStateList} or null, if the icon is not tinted
     */
    public final ColorStateList getIconTintList() {
        return iconTintList;
    }

    /**
     * Sets the color, which should be used to tint the tab's icon.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value
     */
    public final void setIconTint(@ColorInt final int color) {
        setIconTintList(ColorStateList.valueOf(color));
    }

    /**
     * Sets the color state list, which should be used to tint the tab's icon.
     *
     * @param tintList
     *         The color state list, which should be set, as an instance of the class {@link
     *         ColorStateList} or null, if the icon should not be tinted
     */
    public final void setIconTintList(@Nullable final ColorStateList tintList) {
        this.iconTintList = tintList;
        notifyOnIconChanged();
    }

    /**
     * Returns the mode, which is used to tint the tab's icon.
     *
     * @return The mode, which is used to tint the tab's icon, as a value of the enum {@link
     * PorterDuff.Mode} or null, if the default mode is used
     */
    public final PorterDuff.Mode getIconTintMode() {
        return iconTintMode;
    }

    /**
     * Sets the mode, which should be used to tint the tab's icon.
     *
     * @param mode
     *         The mode, which should be set, as a value of enum {@link PorterDuff.Mode} or null, if
     *         the default mode should be used
     */
    public final void setIconTintMode(@Nullable final PorterDuff.Mode mode) {
        this.iconTintMode = mode;
        notifyOnIconChanged();
    }

    /**
     * Returns, whether the tab is closeable, or not.
     *
     * @return True, if the tab is closeable, false otherwise
     */
    public final boolean isCloseable() {
        return closeable;
    }

    /**
     * Sets, whether the tab should be closeable, or not.
     *
     * @param closeable
     *         True, if the tab should be closeable, false otherwise
     */
    public final void setCloseable(final boolean closeable) {
        this.closeable = closeable;
        notifyOnCloseableChanged();
    }

    /**
     * Returns the icon of the tab's close button.
     *
     * @param context
     *         The context, which should be used to retrieve the icon, as an instance of the class
     *         {@link Context}. The context may not be null
     * @return The icon of the tab's close button as an instance of the class {@link Drawable} or
     * null, if no custom icon is set
     */
    @Nullable
    public final Drawable getCloseButtonIcon(@NonNull final Context context) {
        Condition.INSTANCE.ensureNotNull(context, "The context may not be null");

        if (closeButtonIconId != -1) {
            return AppCompatResources.getDrawable(context, closeButtonIconId);
        } else {
            return closeButtonIconBitmap != null ?
                    new BitmapDrawable(context.getResources(), closeButtonIconBitmap) : null;
        }
    }

    /**
     * Sets the icon of the tab's close button.
     *
     * @param resourceId
     *         The resource id of the icon, which should be set, as an {@link Integer} value. The
     *         resource id must correspond to a valid drawable resource
     */
    public final void setCloseButtonIcon(@DrawableRes final int resourceId) {
        this.closeButtonIconId = resourceId;
        this.closeButtonIconBitmap = null;
        notifyOnCloseButtonIconChanged();
    }

    /**
     * Sets the icon of the tab's close button.
     *
     * @param icon
     *         The icon, which should be set, as an instance of the class {@link Bitmap} or null, if
     *         no custom icon should be set
     */
    public final void setCloseButtonIcon(@Nullable final Bitmap icon) {
        this.closeButtonIconId = -1;
        this.closeButtonIconBitmap = icon;
        notifyOnCloseButtonIconChanged();
    }

    /**
     * Returns the color state list, which is used to tint the tab's close button.
     *
     * @return The color state list, which is used to tint the tab's close button, as an instance of
     * the class {@link ColorStateList} or null, if the close button is not tinted
     */
    public final ColorStateList getCloseButtonIconTintList() {
        return closeButtonIconTintList;
    }

    /**
     * Sets the color, which should be used to tint the tab's close button.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value
     */
    public final void setCloseButtonIconTint(@ColorInt final int color) {
        setCloseButtonIconTintList(ColorStateList.valueOf(color));
    }

    /**
     * Sets the color state list, which should be used to tint the tab's close button.
     *
     * @param tintList
     *         The color state list, which should be set, as an instance of the class {@link
     *         ColorStateList} or null, if the close button should not be tinted
     */
    public final void setCloseButtonIconTintList(@Nullable final ColorStateList tintList) {
        this.closeButtonIconTintList = tintList;
        notifyOnCloseButtonIconChanged();
    }

    /**
     * Returns the mode, which is used to tint the tab's close button.
     *
     * @return The mode, which is used to tint the tab's close button, as a value of the enum {@link
     * PorterDuff.Mode} or null, if the default mode is used
     */
    public final PorterDuff.Mode getCloseButtonIconTintMode() {
        return closeButtonIconTintMode;
    }

    /**
     * Sets the mode, which should be used to tint the tab's close button.
     *
     * @param mode
     *         The mode, which should be set, as a value of enum {@link PorterDuff.Mode} or null, if
     *         the default mode should be used
     */
    public final void setCloseButtonIconTintMode(@Nullable final PorterDuff.Mode mode) {
        this.closeButtonIconTintMode = mode;
        notifyOnCloseButtonIconChanged();
    }

    /**
     * Returns the background color of the tab.
     *
     * @return The background color of the tab as an instance of the class {@link ColorStateList} or
     * -1, if no custom color is set
     */
    @Nullable
    public final ColorStateList getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the tab's background color.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value or -1, if no custom color
     *         should be set
     */
    public final void setBackgroundColor(@ColorInt final int color) {
        setBackgroundColor(color != -1 ? ColorStateList.valueOf(color) : null);
    }

    /**
     * Sets the tab's background color.
     *
     * @param colorStateList
     *         The color state list, which should be set, as an instance of the class {@link
     *         ColorStateList} or null, if no custom color should be set
     */
    public final void setBackgroundColor(@Nullable final ColorStateList colorStateList) {
        this.backgroundColor = colorStateList;
        notifyOnBackgroundColorChanged();
    }

    /**
     * Returns the background color of the tab's content.
     *
     * @return The background color of the tab's content as an {@link Integer} value or -1, if no
     * custom color is set
     */
    @ColorInt
    public final int getContentBackgroundColor() {
        return contentBackgroundColor;
    }

    /**
     * Sets the background color of the tab's content.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value or -1, if no custom color
     *         should be set
     */
    public final void setContentBackgroundColor(@ColorInt final int color) {
        this.contentBackgroundColor = color;
        notifyOnContentBackgroundColorChanged();
    }

    /**
     * Returns the text color of the tab's title.
     *
     * @return The text color of the tab's title as an instance of the class {@link ColorStateList}
     * or null, if no custom color is set
     */
    @Nullable
    public final ColorStateList getTitleTextColor() {
        return titleTextColor;
    }

    /**
     * Sets the text color of the tab's title.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value or -1, if no custom color
     *         should be set
     */
    public final void setTitleTextColor(@ColorInt final int color) {
        setTitleTextColor(color != -1 ? ColorStateList.valueOf(color) : null);
    }

    /**
     * Sets the text color of the tab's title.
     *
     * @param colorStateList
     *         The color state list, which should be set, as an instance of the class {@link
     *         ColorStateList} or null, if no custom color should be set
     */
    public final void setTitleTextColor(@Nullable final ColorStateList colorStateList) {
        this.titleTextColor = colorStateList;
        notifyOnTitleTextColorChanged();
    }

    /**
     * Returns, whether the tab's progress bar is shown, or not.
     *
     * @return True, if the tab's progress bar is shown, false otherwise
     */
    public final boolean isProgressBarShown() {
        return progressBarShown;
    }

    /**
     * Sets, whether the tab's progress bar should be shown, or not.
     *
     * @param show
     *         True, if the progress bar should be shown, false otherwise
     */
    public final void showProgressBar(final boolean show) {
        this.progressBarShown = show;
        notifyOnProgressBarVisibilityChanged();
    }

    /**
     * Returns the color of the tab's progress bar.
     *
     * @return The color of the tab's progress bar as an {@link Integer} value or -1, if the default
     * color should be used
     */
    @ColorInt
    public final int getProgressBarColor() {
        return progressBarColor;
    }

    /**
     * Sets the color of the tab's progress bar.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value or -1, if the default
     *         color should be used
     */
    public final void setProgressBarColor(@ColorInt final int color) {
        this.progressBarColor = color;
        notifyOnProgressBarColorChanged();
    }

    /**
     * Returns a bundle, which contains the optional parameters, which are associated with the tab.
     *
     * @return A bundle, which contains the optional parameters, which are associated with the tab,
     * as an instance of the class {@link Bundle} or null, if no parameters are associated with the
     * tab
     */
    @Nullable
    public final Bundle getParameters() {
        return parameters;
    }

    /**
     * Sets a bundle, which contains the optional parameters, which should be associated with the
     * tab.
     *
     * @param parameters
     *         The bundle, which should be set, as an instance of the class {@link Bundle} or null,
     *         if no parameters should be associated with the tab
     */
    public final void setParameters(@Nullable final Bundle parameters) {
        this.parameters = parameters;
    }

    /**
     * Adds a new callback, which should be notified, when the tab's properties have been changed.
     *
     * @param callback
     *         The callback, which should be added, as an instance of the type {@link Callback}. The
     *         callback may not be null
     */
    public final void addCallback(@NonNull final Callback callback) {
        Condition.INSTANCE.ensureNotNull(callback, "The callback may not be null");
        this.callbacks.add(callback);
    }

    /**
     * Removes a specific callback, which should not be notified, when the tab's properties have
     * been changed, anymore.
     *
     * @param callback
     *         The callback, which should be removed, as an instance of the type {@link Callback}.
     *         The callback may not be null
     */
    public final void removeCallback(@NonNull final Callback callback) {
        Condition.INSTANCE.ensureNotNull(callback, "The callback may not be null");
        this.callbacks.remove(callback);
    }

    @Override
    public final int describeContents() {
        return 0;
    }

    @Override
    public final void writeToParcel(final Parcel parcel, final int flags) {
        TextUtils.writeToParcel(title, parcel, flags);
        parcel.writeInt(iconId);
        parcel.writeParcelable(iconBitmap, flags);
        parcel.writeParcelable(iconTintList, flags);
        parcel.writeSerializable(iconTintMode);
        parcel.writeInt(closeable ? 1 : 0);
        parcel.writeInt(closeButtonIconId);
        parcel.writeParcelable(closeButtonIconBitmap, flags);
        parcel.writeParcelable(closeButtonIconTintList, flags);
        parcel.writeSerializable(closeButtonIconTintMode);
        parcel.writeParcelable(backgroundColor, flags);
        parcel.writeInt(contentBackgroundColor);
        parcel.writeParcelable(titleTextColor, flags);
        parcel.writeInt(progressBarShown ? 1 : 0);
        parcel.writeInt(progressBarColor);
        parcel.writeBundle(parameters);
    }

}