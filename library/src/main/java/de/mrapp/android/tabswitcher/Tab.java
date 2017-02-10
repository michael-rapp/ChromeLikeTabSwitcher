/*
 * Copyright 2016 Michael Rapp
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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import java.util.LinkedHashSet;
import java.util.Set;

import static de.mrapp.android.util.Condition.ensureNotEmpty;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A tab, which can be added to a {@link TabSwitcher} widget. It has a title, as well as an optional
 * icon. Furthermore, it is possible to set a custom color and to specify, whether the tab should be
 * closeable, or not.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
// TODO: When changing attributes while the switcher is shown, the corresponding tab view must be adapted
public class Tab implements Parcelable {

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
         */
        void onTitleChanged();

        /**
         * The method, which is invoked, when the tab's icon has been changed.
         */
        void onIconChanged();

        /**
         * The method, which is invoked, when it has been changed, whether the tab is closeable, or
         * not.
         */
        void onCloseableChanged();

        /**
         * The method, which is invoked, when the tab's color has been changed.
         */
        void onColorChanged();

    }

    /**
     * A set, which contains the callbacks, which have been registered to be notified, when the
     * tab's properties have been changed.
     */
    private final Set<Callback> callbacks = new LinkedHashSet<>();

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
     * True, if the tab is closeable, false otherwise.
     */
    private boolean closeable;

    /**
     * The tab's color.
     */
    private int color;

    /**
     * Optional parameters, which are associated with the tab.
     */
    private Bundle parameters;

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
        this.closeable = source.readInt() > 0;
        this.color = source.readInt();
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
        this.iconId = -1;
        this.iconBitmap = null;
        this.color = -1;
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
        ensureNotNull(title, "The title may not be null");
        ensureNotEmpty(title, "The title may not be empty");
        this.title = title;
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
     * @return The tab's icon as an instance of the class {@link Bitmap} or null, if no icon is set
     */
    @Nullable
    public final Drawable getIcon(@NonNull final Context context) {
        if (iconId != -1) {
            return ContextCompat.getDrawable(context, iconId);
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
    }

    /**
     * Sets the tab's icon.
     *
     * @param icon
     *         The icon, which should be set, as an instance of the class {@link Bitmap} or null, if
     *         no icon should be set
     */
    public final void setIcon(@Nullable final Bitmap icon) {
        this.iconId = -1;
        this.iconBitmap = icon;
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
    }

    /**
     * Returns the tab's color.
     *
     * @return The tab's color as an {@link Integer} value or -1, if no custom color is set
     */
    @ColorInt
    public final int getColor() {
        return color;
    }

    /**
     * Sets the tab's color.
     *
     * @param color
     *         The color, which should be set, as an {@link Integer} value or -1, if no custom color
     *         should be set
     */
    public final void setColor(@ColorInt final int color) {
        this.color = color;
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
        ensureNotNull(callback, "The callback may not be null");
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
        ensureNotNull(callback, "The callback may not be null");
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
        parcel.writeInt(closeable ? 1 : 0);
        parcel.writeInt(color);
        parcel.writeBundle(parameters);
    }

}