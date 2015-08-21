/**
 * OpenAtlasForAndroid Project
 * The MIT License (MIT) Copyright (OpenAtlasForAndroid) 2015 Bunny Blue,achellies
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author BunnyBlue
 **/
package com.openatlas.android.lifecycle;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.openatlas.android.compat.OpenAtlasCompat;
import com.openatlas.android.initializer.BundleParser;
import com.openatlas.android.initializer.OpenAtlasInitializer;
import com.openatlas.boot.Globals;
import com.openatlas.framework.PlatformConfigure;
import com.openatlas.runtime.ContextImplHook;
import com.openatlas.util.OpenAtlasUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

//import com.openatlas.android.initializer.OpenAtlasInitializer;

//import com.openatlas.android.initializer.OpenAtlasInitializer;


/****OpenAtlasApplication, you can  extend  this class direct****/
public class OpenAtlasApp extends OpenAtlasCompat {
    private static final Handler mAppHandler;
    private final AtomicInteger mCreationCount;
    private final List<CrossActivityLifecycleCallback> mCrossActivityLifecycleCallbacks;
    private final AtomicInteger mStartCount;
    private WeakReference<Activity> mWeakActivity;
    private Context mBaseContext;
    private String processName;
    OpenAtlasInitializer mAtlasInitializer;

    public interface CrossActivityLifecycleCallback {
        void onCreated(Activity activity);

        void onDestroyed(Activity activity);

        void onStarted(Activity activity);

        void onStopped(Activity activity);
    }

    class CallbackRunable implements Runnable {
        final OpenAtlasApp mApplication;
        private CrossActivityLifecycleCallback mCrossActivityLifecycleCallback;
        private String name;

        public CallbackRunable(OpenAtlasApp openAtlasApp, CrossActivityLifecycleCallback crossActivityLifecycleCallback, String name) {
            this.mApplication = openAtlasApp;
            this.mCrossActivityLifecycleCallback = crossActivityLifecycleCallback;
            this.name = name;
        }

        @Override
        public void run() {
            if (this.mApplication.mWeakActivity != null) {
                Activity activity = this.mApplication.mWeakActivity.get();
                if (!(activity == null || this.mCrossActivityLifecycleCallback == null)) {
                    if ("onCreated".equals(this.name)) {
                        this.mCrossActivityLifecycleCallback.onCreated(activity);
                    } else if ("onStarted".equals(this.name)) {
                        this.mCrossActivityLifecycleCallback.onStarted(activity);
                    }
                }
            }
            this.mCrossActivityLifecycleCallback = null;
            this.name = null;
        }
    }

    class ActivityLifecycleCallbacksCompatImpl implements ActivityLifecycleCallbacksCompat {
        final OpenAtlasApp mApplication;

        ActivityLifecycleCallbacksCompatImpl(OpenAtlasApp panguApplication) {
            this.mApplication = panguApplication;
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
            this.mApplication.mWeakActivity = new WeakReference<Activity>(activity);
            if (this.mApplication.mCreationCount.getAndIncrement() == 0 && !this.mApplication.mCrossActivityLifecycleCallbacks.isEmpty()) {
                for (CrossActivityLifecycleCallback onCreated : this.mApplication.mCrossActivityLifecycleCallbacks) {
                    onCreated.onCreated(activity);
                }
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (this.mApplication.mStartCount.getAndIncrement() == 0 && !this.mApplication.mCrossActivityLifecycleCallbacks.isEmpty()) {
                for (CrossActivityLifecycleCallback onStarted : this.mApplication.mCrossActivityLifecycleCallbacks) {
                    onStarted.onStarted(activity);
                }
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (this.mApplication.mStartCount.decrementAndGet() == 0 && !this.mApplication.mCrossActivityLifecycleCallbacks.isEmpty()) {
                for (CrossActivityLifecycleCallback onStopped : this.mApplication.mCrossActivityLifecycleCallbacks) {
                    onStopped.onStopped(activity);
                }
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (this.mApplication.mCreationCount.decrementAndGet() == 0 && !this.mApplication.mCrossActivityLifecycleCallbacks.isEmpty()) {
                for (CrossActivityLifecycleCallback onDestroyed : this.mApplication.mCrossActivityLifecycleCallbacks) {
                    onDestroyed.onDestroyed(activity);
                }
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }
    }

    public OpenAtlasApp() {
        this.mCrossActivityLifecycleCallbacks = new CopyOnWriteArrayList<CrossActivityLifecycleCallback>();
        this.mCreationCount = new AtomicInteger();
        this.mStartCount = new AtomicInteger();
    }

    public void registerCrossActivityLifecycleCallback(CrossActivityLifecycleCallback crossActivityLifecycleCallback) {
        if (crossActivityLifecycleCallback == null) {
            new RuntimeException("registerCrossActivityLifecycleCallback must not be null").fillInStackTrace();

            return;
        }
        this.mCrossActivityLifecycleCallbacks.add(crossActivityLifecycleCallback);
        if (this.mCreationCount.get() > 0) {
            mAppHandler.post(new CallbackRunable(this, crossActivityLifecycleCallback, "onCreated"));
        }
        if (this.mStartCount.get() > 0) {
            mAppHandler.post(new CallbackRunable(this, crossActivityLifecycleCallback, "onStarted"));
        }
    }

    public void unregisterCrossActivityLifecycleCallback(CrossActivityLifecycleCallback crossActivityLifecycleCallback) {
        this.mCrossActivityLifecycleCallbacks.remove(crossActivityLifecycleCallback);
    }

    public static void runOnUiThread(Runnable runnable) {
        mAppHandler.post(runnable);
    }

    /* (non-Javadoc)
     * @see android.content.ContextWrapper#attachBaseContext(android.content.Context)
     */
    @Override
    protected void attachBaseContext(Context base) {
        // TODO Auto-generated method stub
        super.attachBaseContext(base);
        this.mBaseContext = base;
        BundleParser.parser(getBaseContext());

        try {
            Field declaredField = Globals.class
                    .getDeclaredField("sInstalledVersionName");
            declaredField.setAccessible(true);
            declaredField.set(null, this.mBaseContext.getPackageManager()
                    .getPackageInfo(base.getPackageName(), 0).versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int myPid = Process.myPid();
        for (RunningAppProcessInfo runningAppProcessInfo : ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE))
                .getRunningAppProcesses()) {
            if (runningAppProcessInfo.pid == myPid) {
                this.processName = runningAppProcessInfo.processName;
                break;
            }
        }
        this.mAtlasInitializer = new OpenAtlasInitializer(this, getPackageName(),isUpdate());
        //this.mAtlasInitializer.injectApplication();
        this.mAtlasInitializer.init();
    }
    private boolean isUpdate() {
        try {
            PackageInfo packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            SharedPreferences sharedPreferences =getSharedPreferences(PlatformConfigure.OPENATLAS_CONFIGURE, 0);
            int last_version_code = sharedPreferences.getInt("last_version_code", 0);
            CharSequence last_version_name = sharedPreferences.getString("last_version_name", "");
//return true;
         return packageInfo.versionCode > last_version_code || ((packageInfo.versionCode == last_version_code && !TextUtils.equals(Globals.getInstalledVersionName(), last_version_name)) );
        } catch (Throwable e) {
            Log.e("OpenAtlasInitializer", "Error to get PackageInfo >>>", e);
            throw new RuntimeException(e);
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksCompatImpl(this));
        this.mAtlasInitializer.startUp();

    }

    @Override
    public boolean bindService(Intent intent,
                               ServiceConnection serviceConnection, int i) {
        return new ContextImplHook(getBaseContext(), null).bindService(intent,
                serviceConnection, i);
    }

    @Override
    public void startActivity(Intent intent) {
        // TODO Auto-generated method stub
        // super.startActivity(intent);
        new ContextImplHook(getBaseContext(), getClassLoader())
                .startActivity(intent);
    }


    @Override
    public ComponentName startService(Intent intent) {
        return new ContextImplHook(getBaseContext(), null).startService(intent);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory cursorFactory) {
        String processName = OpenAtlasUtils.getProcessNameByPID(Process.myPid());
        if (!TextUtils.isEmpty(processName)) {
            Log.i("SQLiteDatabase", processName);
            if (!processName.equals(getPackageName())) {
                String[] split = processName.split(":");
                if (split != null && split.length > 1) {
                    processName = split[1] + "_" + name;
                    Log.i("SQLiteDatabase", "openOrCreateDatabase:" + processName);
                    return hookDatabase(processName, mode, cursorFactory);
                }
            }
        }
        return hookDatabase(name, mode, cursorFactory);
    }

    public SQLiteDatabase hookDatabase(String name, int mode, CursorFactory cursorFactory) {
        if (VERSION.SDK_INT >= 11) {
            return super.openOrCreateDatabase(name, mode, cursorFactory);
        }
        SQLiteDatabase sQLiteDatabase = null;
        try {
            return super.openOrCreateDatabase(name, mode, cursorFactory);
        } catch (SQLiteException e) {
            e.printStackTrace();
            if (Globals.getApplication().deleteDatabase(name)) {
                return super.openOrCreateDatabase(name, mode, cursorFactory);
            }
            return sQLiteDatabase;
        }
    }

    static {
        mAppHandler = new Handler();
    }
}