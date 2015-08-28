///**
// * OpenAtlasForAndroid Project
// * The MIT License (MIT) Copyright (OpenAtlasForAndroid) 2015 Bunny Blue,achellies
// * <p>
// * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
// * and associated documentation files (the "Software"), to deal in the Software
// * without restriction, including without limitation the rights to use, copy, modify,
// * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
// * permit persons to whom the Software is furnished to do so, subject to the following conditions:
// * <p>
// * The above copyright notice and this permission notice shall be included in all copies
// * or substantial portions of the Software.
// * <p>
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
// * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
// * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
// * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
// * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// *
// * @author BunnyBlue
// **/
//package com.openatlas.android.lifecycle;
//
//import android.annotation.TargetApi;
//import android.app.Activity;
//import android.os.Build.VERSION;
//import android.os.Bundle;
//import android.os.Handler;
//
//import com.openatlas.android.compat.OpenAtlasApp;
//
//import java.lang.ref.WeakReference;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class OpenAtlasCompat extends OpenAtlasApp {
//    private static final Handler mAppHandler=new Handler();
//    private final ArrayList<ActivityLifecycleCallbacksCompat> mActivityLifecycleCallbacks;
//    private final List<CrossActivityLifecycleCallback> mCrossActivityLifecycleCallbacks;
//    private final AtomicInteger mCreationCount;
//
//
//
//
//    // private final List<CrossActivityLifecycleCallback> mCrossActivityLifecycleCallbacks;
//    private final AtomicInteger mStartCount;
//    private WeakReference<Activity> mWeakActivity;
//    public static  interface ActivityLifecycleCallbacksCompat {
//        void onActivityCreated(Activity activity, Bundle bundle);
//
//        void onActivityDestroyed(Activity activity);
//
//        void onActivityPaused(Activity activity);
//
//        void onActivityResumed(Activity activity);
//
//        void onActivitySaveInstanceState(Activity activity, Bundle bundle);
//
//        void onActivityStarted(Activity activity);
//
//        void onActivityStopped(Activity activity);
//    }
//
//    public static class AbstractActivityLifecycleCallbacks implements ActivityLifecycleCallbacksCompat {
//        @Override
//        public void onActivityCreated(Activity activity, Bundle bundle) {
//        }
//
//        @Override
//        public void onActivityStarted(Activity activity) {
//        }
//
//        @Override
//        public void onActivityResumed(Activity activity) {
//        }
//
//        @Override
//        public void onActivityPaused(Activity activity) {
//        }
//
//        @Override
//        public void onActivityStopped(Activity activity) {
//        }
//
//        @Override
//        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
//        }
//
//        @Override
//        public void onActivityDestroyed(Activity activity) {
//        }
//    }
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//
//
//        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksCompatImpl(this));
//    }
//
//    public OpenAtlasCompat() {
//        this.mActivityLifecycleCallbacks = new ArrayList<ActivityLifecycleCallbacksCompat>();
//        this.mCrossActivityLifecycleCallbacks = new CopyOnWriteArrayList<CrossActivityLifecycleCallback>();
//        this.mCreationCount = new AtomicInteger();
//        this.mStartCount = new AtomicInteger();
//
//
//    }
//    public void registerCrossActivityLifecycleCallback(CrossActivityLifecycleCallback crossActivityLifecycleCallback) {
//        if (crossActivityLifecycleCallback == null) {
//            new RuntimeException("registerCrossActivityLifecycleCallback must not be null").fillInStackTrace();
//
//            return;
//        }
//        this.mCrossActivityLifecycleCallbacks.add(crossActivityLifecycleCallback);
//        if (this.mCreationCount.get() > 0) {
//            mAppHandler.post(new CallbackRunable(this, crossActivityLifecycleCallback, "onCreated"));
//        }
//        if (this.mStartCount.get() > 0) {
//            mAppHandler.post(new CallbackRunable(this, crossActivityLifecycleCallback, "onStarted"));
//        }
//    }
//
//    public void unregisterCrossActivityLifecycleCallback(CrossActivityLifecycleCallback crossActivityLifecycleCallback) {
//        this.mCrossActivityLifecycleCallbacks.remove(crossActivityLifecycleCallback);
//    }
//
//    @TargetApi(14)
//    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacksCompat activityLifecycleCallbacksCompat) {
//        if (VERSION.SDK_INT >= 14) {
//            super.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksImpl(activityLifecycleCallbacksCompat));
//            return;
//        }
//        synchronized (this.mActivityLifecycleCallbacks) {
//            this.mActivityLifecycleCallbacks.add(activityLifecycleCallbacksCompat);
//        }
//    }
//
//    @TargetApi(14)
//    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacksCompat activityLifecycleCallbacksCompat) {
//        if (VERSION.SDK_INT >= 14) {
//            super.unregisterActivityLifecycleCallbacks(new ActivityLifecycleCallbacksImpl(activityLifecycleCallbacksCompat));
//            return;
//        }
//        synchronized (this.mActivityLifecycleCallbacks) {
//            this.mActivityLifecycleCallbacks.remove(activityLifecycleCallbacksCompat);
//        }
//    }
//
//    void dispatchActivityCreatedCompat(Activity activity, Bundle bundle) {
//        ActivityLifecycleCallbacksCompat[] collectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
//        if (collectActivityLifecycleCallbacks != null) {
//            for (ActivityLifecycleCallbacksCompat onActivityCreated : collectActivityLifecycleCallbacks) {
//                onActivityCreated.onActivityCreated(activity, bundle);
//            }
//        }
//    }
//
//    void dispatchActivityStartedCompat(Activity activity) {
//        ActivityLifecycleCallbacksCompat[] collectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
//        if (collectActivityLifecycleCallbacks != null) {
//            for (ActivityLifecycleCallbacksCompat onActivityStarted : collectActivityLifecycleCallbacks) {
//                onActivityStarted.onActivityStarted(activity);
//            }
//        }
//    }
//
//    void dispatchActivityResumedCompat(Activity activity) {
//        ActivityLifecycleCallbacksCompat[] collectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
//        if (collectActivityLifecycleCallbacks != null) {
//            for (ActivityLifecycleCallbacksCompat onActivityResumed : collectActivityLifecycleCallbacks) {
//                onActivityResumed.onActivityResumed(activity);
//            }
//        }
//    }
//
//    void dispatchActivityPausedCompat(Activity activity) {
//        ActivityLifecycleCallbacksCompat[] collectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
//        if (collectActivityLifecycleCallbacks != null) {
//            for (ActivityLifecycleCallbacksCompat onActivityPaused : collectActivityLifecycleCallbacks) {
//                onActivityPaused.onActivityPaused(activity);
//            }
//        }
//    }
//
//    void dispatchActivityStoppedCompat(Activity activity) {
//        ActivityLifecycleCallbacksCompat[] collectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
//        if (collectActivityLifecycleCallbacks != null) {
//            for (ActivityLifecycleCallbacksCompat onActivityStopped : collectActivityLifecycleCallbacks) {
//                onActivityStopped.onActivityStopped(activity);
//            }
//        }
//    }
//
//    void dispatchActivitySaveInstanceStateCompat(Activity activity, Bundle bundle) {
//        ActivityLifecycleCallbacksCompat[] collectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
//        if (collectActivityLifecycleCallbacks != null) {
//            for (ActivityLifecycleCallbacksCompat onActivitySaveInstanceState : collectActivityLifecycleCallbacks) {
//                onActivitySaveInstanceState.onActivitySaveInstanceState(activity, bundle);
//            }
//        }
//    }
//
//    void dispatchActivityDestroyedCompat(Activity activity) {
//        ActivityLifecycleCallbacksCompat[] collectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
//        if (collectActivityLifecycleCallbacks != null) {
//            for (ActivityLifecycleCallbacksCompat onActivityDestroyed : collectActivityLifecycleCallbacks) {
//                onActivityDestroyed.onActivityDestroyed(activity);
//            }
//        }
//    }
//
//
//    private ActivityLifecycleCallbacksCompat[] collectActivityLifecycleCallbacks() {
//        ActivityLifecycleCallbacksCompat[] activityLifecycleCallbacksCompatArr = null;
//        synchronized (this.mActivityLifecycleCallbacks) {
//            if (this.mActivityLifecycleCallbacks.size() > 0) {
//                activityLifecycleCallbacksCompatArr = this.mActivityLifecycleCallbacks.toArray(new ActivityLifecycleCallbacksCompat[this.mActivityLifecycleCallbacks.size()]);
//            }
//        }
//        return activityLifecycleCallbacksCompatArr;
//    }
//
//
//    class ActivityLifecycleCallbacksImpl implements ActivityLifecycleCallbacks {
//        private final ActivityLifecycleCallbacksCompat mActivityLifecycleCallbacksCompat;
//
//        @Override
//        public void onActivityCreated(Activity activity, Bundle bundle) {
//            this.mActivityLifecycleCallbacksCompat.onActivityCreated(activity, bundle);
//        }
//
//        @Override
//        public void onActivityStarted(Activity activity) {
//            this.mActivityLifecycleCallbacksCompat.onActivityStarted(activity);
//        }
//
//        @Override
//        public void onActivityResumed(Activity activity) {
//            this.mActivityLifecycleCallbacksCompat.onActivityResumed(activity);
//        }
//
//        @Override
//        public void onActivityPaused(Activity activity) {
//            this.mActivityLifecycleCallbacksCompat.onActivityPaused(activity);
//        }
//
//        @Override
//        public void onActivityStopped(Activity activity) {
//            this.mActivityLifecycleCallbacksCompat.onActivityStopped(activity);
//        }
//
//        @Override
//        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
//            this.mActivityLifecycleCallbacksCompat.onActivitySaveInstanceState(activity, bundle);
//        }
//
//        @Override
//        public void onActivityDestroyed(Activity activity) {
//            this.mActivityLifecycleCallbacksCompat.onActivityDestroyed(activity);
//        }
//
//        @Override
//        public int hashCode() {
//            return this.mActivityLifecycleCallbacksCompat.hashCode();
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj) {
//                return true;
//            }
//            if (obj instanceof ActivityLifecycleCallbacksImpl) {
//                return this.mActivityLifecycleCallbacksCompat.equals(((ActivityLifecycleCallbacksImpl) obj).mActivityLifecycleCallbacksCompat);
//            }
//            return false;
//        }
//
//        ActivityLifecycleCallbacksImpl(ActivityLifecycleCallbacksCompat activityLifecycleCallbacksCompat) {
//            this.mActivityLifecycleCallbacksCompat = activityLifecycleCallbacksCompat;
//        }
//    }
//
//    public interface CrossActivityLifecycleCallback {
//        void onCreated(Activity activity);
//
//        void onDestroyed(Activity activity);
//
//        void onStarted(Activity activity);
//
//        void onStopped(Activity activity);
//    }
//
//
//    class ActivityLifecycleCallbacksCompatImpl implements OpenAtlasCompat.ActivityLifecycleCallbacksCompat {
//        final OpenAtlasCompat mApplication;
//
//        ActivityLifecycleCallbacksCompatImpl(OpenAtlasCompat openAtlasCompat) {
//            this.mApplication = openAtlasCompat;
//        }
//
//        @Override
//        public void onActivityCreated(Activity activity, Bundle bundle) {
//            this.mApplication.mWeakActivity = new WeakReference<Activity>(activity);
//            if (this.mApplication.mCreationCount.getAndIncrement() == 0 && !this.mApplication.mCrossActivityLifecycleCallbacks.isEmpty()) {
//                for (CrossActivityLifecycleCallback onCreated : this.mApplication.mCrossActivityLifecycleCallbacks) {
//                    onCreated.onCreated(activity);
//                }
//            }
//        }
//
//        @Override
//        public void onActivityStarted(Activity activity) {
//            if (this.mApplication.mStartCount.getAndIncrement() == 0 && !this.mApplication.mCrossActivityLifecycleCallbacks.isEmpty()) {
//                for (CrossActivityLifecycleCallback onStarted : this.mApplication.mCrossActivityLifecycleCallbacks) {
//                    onStarted.onStarted(activity);
//                }
//            }
//        }
//
//        @Override
//        public void onActivityStopped(Activity activity) {
//            if (this.mApplication.mStartCount.decrementAndGet() == 0 && !this.mApplication.mCrossActivityLifecycleCallbacks.isEmpty()) {
//                for (CrossActivityLifecycleCallback onStopped : this.mApplication.mCrossActivityLifecycleCallbacks) {
//                    onStopped.onStopped(activity);
//                }
//            }
//        }
//
//        @Override
//        public void onActivityDestroyed(Activity activity) {
//            if (this.mApplication.mCreationCount.decrementAndGet() == 0 && !this.mApplication.mCrossActivityLifecycleCallbacks.isEmpty()) {
//                for (CrossActivityLifecycleCallback onDestroyed : this.mApplication.mCrossActivityLifecycleCallbacks) {
//                    onDestroyed.onDestroyed(activity);
//                }
//            }
//        }
//
//        @Override
//        public void onActivityResumed(Activity activity) {
//        }
//
//        @Override
//        public void onActivityPaused(Activity activity) {
//        }
//
//        @Override
//        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
//        }
//    }
//
//
//
//    class CallbackRunable implements Runnable {
//        final OpenAtlasCompat mApplication;
//        private CrossActivityLifecycleCallback mCrossActivityLifecycleCallback;
//        private String name;
//
//        public CallbackRunable(OpenAtlasCompat openAtlasApp, CrossActivityLifecycleCallback crossActivityLifecycleCallback, String name) {
//            this.mApplication = openAtlasApp;
//            this.mCrossActivityLifecycleCallback = crossActivityLifecycleCallback;
//            this.name = name;
//        }
//
//        @Override
//        public void run() {
//            if (this.mApplication.mWeakActivity != null) {
//                Activity activity = this.mApplication.mWeakActivity.get();
//                if (!(activity == null || this.mCrossActivityLifecycleCallback == null)) {
//                    if ("onCreated".equals(this.name)) {
//                        this.mCrossActivityLifecycleCallback.onCreated(activity);
//                    } else if ("onStarted".equals(this.name)) {
//                        this.mCrossActivityLifecycleCallback.onStarted(activity);
//                    }
//                }
//            }
//            this.mCrossActivityLifecycleCallback = null;
//            this.name = null;
//        }
//    }
//
//
//
//
//
//}