/*
 * Copyright 2015 - 2017 Michael Rapp
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
package de.mrapp.android.tabswitcher.util;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.util.LruCache;
import android.view.View;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.android.util.logging.Logger;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An abstract base class for all data binders, which allow to asynchronously load data in order to
 * display it by using views. Such binders are meant to be used, when loading various data items, of
 * which each one should be displayed by a different view. Once loaded, the data can optionally be
 * stored in a cache (it therefore must be associated with an unique key). When attempting to reload
 * already cached data, it is retrieved from the cache and displayed immediately.
 *
 * The binder supports to use adapter views, which might be recycled while data is still loaded. In
 * such case, the recycled view is prevented from showing the data once loading has finished,
 * because it is already used for other purposes.
 *
 * @param <DataType>
 *         The type of the data, which is bound to views
 * @param <KeyType>
 *         The type of the keys, which allow to uniquely identify already loaded data
 * @param <ViewType>
 *         The type of the views, which are used to display data
 * @param <ParamType>
 *         The type of parameters, which can be passed when loading data
 * @author Michael Rapp
 * @since 1.14.0
 */
public abstract class DataBinder<DataType, KeyType, ViewType extends View, ParamType>
        extends Handler {

    /**
     * A task, which encapsulates all information, which is required to asynchronously load data and
     * display it afterwards. It also contains the data once loaded.
     *
     * @param <DataType>
     *         The type of the data, which is bound to views
     * @param <KeyType>
     *         The type of the keys, which allow to uniquely identify already loaded data
     * @param <ViewType>
     *         The type of the views, which are used to display data
     * @param <ParamType>
     *         The type of parameters, which can be passed when loading data
     */
    private static class Task<DataType, KeyType, ViewType extends View, ParamType> {

        /**
         * The view, which should be used to display the data.
         */
        private final ViewType view;

        /**
         * The key of the data, which should be loaded.
         */
        private final KeyType key;

        /**
         * An array, which contains optional parameters.
         */
        private final ParamType[] params;

        /**
         * The data, which has been loaded.
         */
        @Nullable
        private DataType result;

        /**
         * Creates a new task
         *
         * @param view
         *         The view, which should be used to display the data, as an instance of the class
         *         {@link View}. The view may not be null
         * @param key
         *         The key of the data, which should be loaded, as an instance of the generic type
         *         KeyType. The key may not be null
         * @param params
         *         An array, which contains optional parameters, as an array of the type ParamType
         *         or an empty array, if no parameters should be used
         */
        Task(@NonNull final ViewType view, @NonNull final KeyType key,
             @NonNull final ParamType[] params) {
            this.view = view;
            this.key = key;
            this.params = params;
            this.result = null;
        }

    }

    /**
     * The number of items, which are stored by a cached, by default.
     */
    public static final int CACHE_SIZE = 10;

    /**
     * The context, which is used by the data binder.
     */
    private final Context context;

    /**
     * The logger, which is used by the data binder.
     */
    private final Logger logger;

    /**
     * A LRU cache, which is used to cache already loaded data.
     */
    private final LruCache<KeyType, DataType> cache;

    /**
     * A map, which is used to manage the views, which have already been used to display data.
     */
    private final Map<ViewType, KeyType> views;

    /**
     * The thread pool, which is used to manage the threads, which are used to asynchronously load
     * data.
     */
    private final ExecutorService threadPool;

    /**
     * The object, which is used to acquire locks, when cancelling to load data.
     */
    private final Object cancelLock;

    /**
     * True, if loading the data has been canceled, false otherwise
     */
    private boolean canceled;

    /**
     * True, if data should be cached, false otherwise.
     */
    private boolean useCache;

    /**
     * Returns the data, which corresponds to a specific key, from the cache.
     *
     * @param key
     *         The key of the data, which should be retrieved, as an instance of the generic type
     *         KeyType. The key may not be null
     * @return The data, which has been retrieved, as an instance of the generic type DataType or
     * null, if no data with the given key is contained by the cache
     */
    @Nullable
    private DataType getCachedData(@NonNull final KeyType key) {
        synchronized (cache) {
            return cache.get(key);
        }
    }

    /**
     * Adds the data, which corresponds to a specific key, to the cache, if caching is enabled.
     *
     * @param key
     *         The key of the data, which should be added to the cache, as an instance of the
     *         generic type KeyType. The key may not be null
     * @param data
     *         The data, which should be added to the cache, as an instance of the generic type
     *         DataType. The data may not be null
     */
    private void cacheData(@NonNull final KeyType key, @NonNull final DataType data) {
        synchronized (cache) {
            if (useCache) {
                cache.put(key, data);
            }
        }
    }

    /**
     * Asynchronously executes a specific task in order to load data and to display it afterwards.
     *
     * @param task
     *         The task, which should be executed, as an instance of the class {@link Task}. The
     *         task may not be null
     */
    private void loadDataAsynchronously(
            @NonNull final Task<DataType, KeyType, ViewType, ParamType> task) {
        threadPool.submit(new Runnable() {

            @Override
            public void run() {
                if (!isCanceled()) {
                    task.result = loadData(task);
                    Message message = Message.obtain();
                    message.obj = task;
                    sendMessage(message);
                }
            }

        });
    }

    /**
     * Executes a specific task in order to load data.
     *
     * @param task
     *         The task, which should be executed, as an instance of the class {@link Task}. The
     *         task may not be null
     * @return The data, which has been loaded, as an instance of the generic type DataType or null,
     * if no data has been loaded
     */
    @Nullable
    private DataType loadData(@NonNull final Task<DataType, KeyType, ViewType, ParamType> task) {
        try {
            DataType data = doInBackground(task.key, task.params);

            if (data != null) {
                cacheData(task.key, data);
            }

            logger.logInfo(getClass(), "Loaded data with key " + task.key);
            return data;
        } catch (Exception e) {
            logger.logError(getClass(), "An error occurred while loading data with key " + task.key,
                    e);
            return null;
        }
    }

    /**
     * Sets, whether loading the data has been canceled, or not.
     *
     * @param canceled
     *         True, if loading the data has been canceled, false otherwise
     */
    private void setCanceled(final boolean canceled) {
        synchronized (cancelLock) {
            this.canceled = canceled;
        }
    }

    /**
     * The method, which is invoked on implementing subclasses prior to loading any data. This
     * method may be overridden to adapt the appearance of views.
     *
     * @param view
     *         The view, which should be used to display the data, as an instance of the generic
     *         type ViewType. The view may not be null
     * @param params
     *         An array, which contains optional parameters, as an array of the type ParamType or an
     *         empty array, if no parameters should be used
     */
    @UiThread
    @SuppressWarnings("unchecked")
    protected void onPreExecute(@NonNull final ViewType view, @NonNull final ParamType... params) {

    }

    /**
     * The method, which is invoked on implementing subclasses, in order to load the data, which
     * corresponds to a specific key. This method is executed in a background thread and therefore
     * no views may be modified.
     *
     * @param key
     *         The key of the data, which should be loaded, as an instance of the generic type
     *         KeyType. The key may not be null
     * @param params
     *         An array, which contains optional parameters, as an array of the type ParamType or an
     *         empty array, if no parameters should be used
     * @return The data, which has been loaded, as an instance of the generic type DataType or null,
     * if no data has been loaded
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected abstract DataType doInBackground(@NonNull final KeyType key,
                                               @NonNull final ParamType... params);

    /**
     * The method, which is invoked on implementing subclasses, in order to display data after it
     * has been loaded.
     *
     * @param view
     *         The view, which should be used to display the data, as an instance of the generic
     *         type ViewType. The view may not be null
     * @param data
     *         The data, which should be displayed, as an instance of the generic type DataType or
     *         null, if no data should be displayed
     * @param params
     *         An array, which contains optional parameters, as an array of the type ParamType or an
     *         empty array, if no parameters should be used
     */
    @UiThread
    @SuppressWarnings("unchecked")
    protected abstract void onPostExecute(@NonNull final ViewType view,
                                          @Nullable final DataType data,
                                          @NonNull final ParamType... params);

    /**
     * Creates a new data binder. Caching is enabled by default. The cache, which is used to store
     * already loaded data, caches up to <code>CACHE_SIZE</code> items. The executor service, which
     * is used to manage asynchronous tasks, is created by using the static method
     * <code>Executors.newCachedThreadPool</code>. Such executor services are meant to be used when
     * many short-living tasks are executed and reuse previously created threads.
     *
     * @param context
     *         The context, which should be used by the data binder, as an instance of the class
     *         {@link Context}. The context may not be null
     */
    public DataBinder(@NonNull final Context context) {
        this(context, Executors.newCachedThreadPool());
    }

    /**
     * Creates a new data binder, which uses a specific executor service. Caching is enabled by
     * default. The cache, which is used to store already loaded data, caches up to
     * <code>CACHE_SIZE</code> items.
     *
     * @param context
     *         The context, which should be used by the data binder, as an instance of the class
     *         {@link Context}. The context may not be null
     * @param threadPool
     *         The executor service, which should be used to manage asynchronous tasks, as an
     *         instance of the type {@link ExecutorService}. The executor service may not be null
     */
    public DataBinder(@NonNull final Context context, @NonNull final ExecutorService threadPool) {
        this(context, threadPool, new LruCache<KeyType, DataType>(CACHE_SIZE));
    }

    /**
     * Creates a new data binder, which uses a specific cache. Caching is enabled by default. The
     * executor service, which is used to manage asynchronous tasks, is created by using the static
     * method <code>Executors.newCachedThreadPool</code>. Such executor services are meant to be
     * used when many short-living tasks are executed and reuse previously created threads.
     *
     * @param context
     *         The context, which should be used by the data binder, as an instance of the class
     *         {@link Context}. The context may not be null
     * @param cache
     *         The LRU cache, which should be used to cache already loaded data, as an instance of
     *         the class {@link LruCache}. The cache may not be null
     */
    public DataBinder(@NonNull final Context context,
                      @NonNull final LruCache<KeyType, DataType> cache) {
        this(context, Executors.newCachedThreadPool(), cache);
    }

    /**
     * Creates a new data binder, which uses a specifc executor service and cache. Caching is
     * enabled by default.
     *
     * @param context
     *         The context, which should be used by the data binder, as an instance of the class
     *         {@link Context}. The context may not be null
     * @param threadPool
     *         The executor service, which should be used to manage asynchronous tasks, as an
     *         instance of the type {@link ExecutorService}. The executor service may not be null
     * @param cache
     *         The LRU cache, which should be used to cache already loaded data, as an instance of
     *         the class {@link LruCache}. The cache may not be null
     */
    public DataBinder(@NonNull final Context context, @NonNull final ExecutorService threadPool,
                      @NonNull final LruCache<KeyType, DataType> cache) {
        ensureNotNull(context, "The context may not be null");
        ensureNotNull(threadPool, "The executor service may not be null");
        ensureNotNull(cache, "The cache may not be null");
        this.context = context;
        this.logger = new Logger(LogLevel.INFO);
        this.cache = cache;
        this.views = Collections.synchronizedMap(new WeakHashMap<ViewType, KeyType>());
        this.threadPool = threadPool;
        this.cancelLock = new Object();
        this.canceled = false;
        this.useCache = true;
    }

    /**
     * Returns the context, which is used by the data binder.
     *
     * @return The context, which is used by the data binder, as an instance of the class {@link
     * Context}. The context may not be null
     */
    @NonNull
    public final Context getContext() {
        return context;
    }

    /**
     * Returns the log level, which is used for logging.
     *
     * @return The log level, which is used for logging, as a value of the enum {@link LogLevel}.
     * The log level may not be null
     */
    @NonNull
    public final LogLevel getLogLevel() {
        return logger.getLogLevel();
    }

    /**
     * Sets the log level, which should be used for logging.
     *
     * @param logLevel
     *         The log level, which should be set, as a value of the enum {@link LogLevel}. The log
     *         level may not be null
     */
    public final void setLogLevel(@NonNull final LogLevel logLevel) {
        logger.setLogLevel(logLevel);
    }

    /**
     * Loads the the data, which corresponds to a specific key, and displays it in a specific view.
     * If the data has already been loaded, it will be retrieved from the cache. By default, the
     * data is loaded in a background thread.
     *
     * @param key
     *         The key of the data, which should be loaded, as an instance of the generic type
     *         KeyType. The key may not be null
     * @param view
     *         The view, which should be used to display the data, as an instance of the generic
     *         type ViewType. The view may not be null
     * @param params
     *         An array, which contains optional parameters, as an array of the type ParamType or an
     *         empty array, if no parameters should be used
     */
    @SafeVarargs
    public final void load(@NonNull final KeyType key, @NonNull final ViewType view,
                           @NonNull final ParamType... params) {
        load(key, view, true, params);
    }

    /**
     * Loads the the data, which corresponds to a specific key, and displays it in a specific view.
     * If the data has already been loaded, it will be retrieved from the cache.
     *
     * @param key
     *         The key of the data, which should be loaded, as an instance of the generic type
     *         KeyType. The key may not be null
     * @param view
     *         The view, which should be used to display the data, as an instance of the generic
     *         type ViewType. The view may not be null
     * @param async
     *         True, if the data should be loaded in a background thread, false otherwise
     * @param params
     *         An array, which contains optional parameters, as an array of the type ParamType or an
     *         empty array, if no parameters should be used
     */
    @SafeVarargs
    public final void load(@NonNull final KeyType key, @NonNull final ViewType view,
                           final boolean async, @NonNull final ParamType... params) {
        ensureNotNull(key, "The key may not be null");
        ensureNotNull(view, "The view may not be null");
        ensureNotNull(params, "The array may not be null");
        setCanceled(false);
        views.put(view, key);
        DataType data = getCachedData(key);

        if (!isCanceled()) {
            if (data != null) {
                onPostExecute(view, data, params);
                logger.logInfo(getClass(), "Loaded data with key " + key + " from cache");
            } else {
                onPreExecute(view, params);
                Task<DataType, KeyType, ViewType, ParamType> task = new Task<>(view, key, params);

                if (async) {
                    loadDataAsynchronously(task);
                } else {
                    data = loadData(task);
                    onPostExecute(view, data, params);
                }
            }
        }
    }

    /**
     * Cancels loading the data.
     */
    public final void cancel() {
        setCanceled(true);
        logger.logInfo(getClass(), "Canceled to load data");
    }

    /**
     * Returns, whether loading the data has been canceled, or not.
     *
     * @return True, if loading the data has been canceled, false otherwise
     */
    public final boolean isCanceled() {
        synchronized (cancelLock) {
            return canceled;
        }
    }

    /**
     * Returns, whether the data, which corresponds to a specific key, is currently cached, or not.
     *
     * @param key
     *         The key, which corresponds to the data, which should be checked, as an instance of
     *         the generic type KeyType. The key may not be null
     * @return True, if the data, which corresponds to the given key, is currently cached, false
     * otherwise
     */
    public final boolean isCached(@NonNull final KeyType key) {
        ensureNotNull(key, "The key may not be null");

        synchronized (cache) {
            return cache.get(key) != null;
        }
    }

    /**
     * Returns, whether data is cached, or not.
     *
     * @return True, if data is cached, false otherwise
     */
    public final boolean isCacheUsed() {
        synchronized (cache) {
            return useCache;
        }
    }

    /**
     * Sets, whether data should be cached, or not.
     *
     * @param useCache
     *         True, if data should be cached, false otherwise.
     */
    public final void useCache(final boolean useCache) {
        synchronized (cache) {
            this.useCache = useCache;
            logger.logDebug(getClass(), useCache ? "Enabled" : "Disabled" + " caching");

            if (!useCache) {
                clearCache();
            }
        }
    }

    /**
     * Clears the cache.
     */
    public final void clearCache() {
        synchronized (cache) {
            cache.evictAll();
            logger.logDebug(getClass(), "Cleared cache");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void handleMessage(final Message msg) {
        Task<DataType, KeyType, ViewType, ParamType> task = (Task) msg.obj;

        if (!isCanceled()) {
            KeyType key = views.get(task.view);

            if (key != null && key.equals(task.key)) {
                onPostExecute(task.view, task.result, task.params);
            } else {
                logger.logVerbose(getClass(),
                        "Data with key " + task.key + " not displayed. View has been recycled");
            }
        } else {
            logger.logVerbose(getClass(),
                    "Data with key " + task.key + " not displayed. Loading data has been canceled");
        }
    }

}