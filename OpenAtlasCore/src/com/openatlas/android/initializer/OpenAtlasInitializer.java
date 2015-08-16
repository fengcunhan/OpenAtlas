/**
 * OpenAtlasForAndroid Project
 * <p>
 * The MIT License (MIT)
 * Copyright (c) 2015 Bunny Blue
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
 * @author BunnyBlue
 */
/**
 * @author BunnyBlue
 */
package com.openatlas.android.initializer;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.openatlas.android.task.Coordinator;
import com.openatlas.android.task.Coordinator.TaggedRunnable;
import com.openatlas.boot.Globals;
import com.openatlas.framework.Atlas;
import com.openatlas.framework.PlatformConfigure;
import com.openatlas.util.ApkUtils;

import java.util.Properties;

public class OpenAtlasInitializer {
    private static long time;
    private static boolean isAppPkg;
    private Application mApp;
    private String pkgName;

    private boolean tryInstall;

    private boolean isMiniPackage;
    private boolean init;

    static {
        time = 0;
    }

    public OpenAtlasInitializer(Application application, String packageName) {

        this.init = true;
        this.mApp = application;
        this.pkgName = packageName;

        if (application.getPackageName().equals(packageName)) {
            isAppPkg = true;
        }
    }

    public void injectApplication() {
        try {
            Atlas.getInstance().injectApplication(this.mApp, this.mApp.getPackageName());
        } catch (Exception e) {
            throw new RuntimeException("atlas inject mApplication fail" + e.getMessage());
        }
    }

    /**
     * init ,you should  call  this on  Application,hack  class  method and  field
     **/
    public void init() {
        time = System.currentTimeMillis();
        try {
            Atlas.getInstance().init(this.mApp);
            System.out.println("Atlas framework inited end " + this.pkgName + " " + (System.currentTimeMillis() - time) + " ms");
        } catch (Throwable e) {
            Log.e("AtlasInitializer", "Could not init atlas framework !!!", e);
            throw new RuntimeException("atlas initialization fail" + e.getMessage());
        }
    }

    /***
     * start update  OpenAtlas ,you should call in Application.onCreate()
     **/
    public void startUp() {
        this.init = isMatchVersion();
        if (this.init) {
            killMe();

        }
        Properties properties = new Properties();
        properties.put(PlatformConfigure.BOOT_ACTIVITY, PlatformConfigure.BOOT_ACTIVITY);
        properties.put(PlatformConfigure.COM_OPENATLAS_DEBUG_BUNDLES, "true");
        properties.put(PlatformConfigure.ATLAS_APP_DIRECTORY, this.mApp.getFilesDir().getParent());

        try {

              Globals.init(this.mApp, Atlas.getInstance().getDelegateClassLoader());
            if (this.mApp.getPackageName().equals(this.pkgName)) {
                if (verifyRuntime() || !ApkUtils.isRootSystem()) {
                    properties.put(PlatformConfigure.OPENATLAS_PUBLIC_KEY, SecurityFrameListener.PUBLIC_KEY);
                    Atlas.getInstance().addFrameworkListener(new SecurityFrameListener());
                }
                if (this.init) {
                    properties.put("osgi.init", "true");
                }
            }
            BundlesInstaller mBundlesInstaller = BundlesInstaller.getInstance();
            OptDexProcess mOptDexProcess = OptDexProcess.getInstance();
            if (this.mApp.getPackageName().equals(this.pkgName) && (this.init)) {
                mBundlesInstaller.init(this.mApp, isAppPkg);
                mOptDexProcess.init(this.mApp);
            }
            System.out.println("Atlas framework prepare starting in process " + this.pkgName + " " + (System.currentTimeMillis() - time) + " ms");
            Atlas.getInstance().setClassNotFoundInterceptorCallback(new ClassNotFoundInterceptor());
            try {
                Atlas.getInstance().startup(properties);
                installBundles(mBundlesInstaller, mOptDexProcess);
                System.out.println("Atlas framework end startUp in process " + this.pkgName + " " + (System.currentTimeMillis() - time) + " ms");
            } catch (Throwable e) {
                Log.e("AtlasInitializer", "Could not start up atlas framework !!!", e);
                throw new RuntimeException(e);
            }
        } catch (Throwable e2) {
            e2.printStackTrace();
            throw new RuntimeException("Could not set Globals !!!", e2);
        }
    }



    private void killMe() {
        if (!this.mApp.getPackageName().equals(this.pkgName)) {
            Process.killProcess(Process.myPid());
        }
    }

    private void installBundles(final BundlesInstaller mBundlesInstaller, final OptDexProcess mOptDexProcess) {
        if (this.mApp.getPackageName().equals(this.pkgName)) {
            if (!Utils.searchFile(this.mApp.getFilesDir().getParentFile() + "/lib", "libcom_")) {
                InstallSolutionConfig.install_when_oncreate = true;
            }
            if (InstallSolutionConfig.install_when_findclass ) {
                InstallSolutionConfig.install_when_oncreate = true;
                this.tryInstall = true;
            }
            if (this.init) {

                if (InstallSolutionConfig.install_when_oncreate_auto) {
                    Coordinator.postTask(new TaggedRunnable("AtlasStartup") {

                        @Override
                        public void run() {
                            mBundlesInstaller.process(true, false);
                            mOptDexProcess.processPackages(false, false);

                        }
                    });
                }
                if (InstallSolutionConfig.install_when_oncreate) {
                    Coordinator.postTask(new TaggedRunnable("AtlasStartup") {

                        @Override
                        public void run() {

                            mBundlesInstaller.process(true, false);
                            mOptDexProcess.processPackages(true, false);


                        }
                    });
                } else {
                    Utils.notifyBundleInstalled(this.mApp);
                    Utils.UpdatePackageVersion(this.mApp);
                    Utils.saveAtlasInfoBySharedPreferences(this.mApp);
                }
            } else if (!this.init) {
                if (this.tryInstall) {
                    Coordinator.postTask(new TaggedRunnable("AtlasStartup") {

                        @Override
                        public void run() {
                            mBundlesInstaller.process(false, false);
                            mOptDexProcess.processPackages(false, false);

                        }
                    });
                } else {
                    Utils.notifyBundleInstalled(this.mApp);
                }
            }

        }
    }






    @SuppressLint({"DefaultLocale"})
    private boolean verifyRuntime() {
        return !((Build.BRAND == null || !Build.BRAND.toLowerCase().contains("xiaomi") || Build.HARDWARE == null || !Build.HARDWARE.toLowerCase().contains("mt65")) && VERSION.SDK_INT >= 14);
    }

    private boolean isMatchVersion() {
        try {
            PackageInfo packageInfo = this.mApp.getPackageManager().getPackageInfo(this.mApp.getPackageName(), 0);
            SharedPreferences sharedPreferences = this.mApp.getSharedPreferences(PlatformConfigure.OPENATLAS_CONFIGURE, 0);
            int last_version_code = sharedPreferences.getInt("last_version_code", 0);
            CharSequence last_version_name = sharedPreferences.getString("last_version_name", "");
            CharSequence miniPackage = sharedPreferences.getString("isMiniPackage", "");
            this.isMiniPackage = false;
            System.out.println("resetForOverrideInstall = " + this.isMiniPackage);
            if (TextUtils.isEmpty(miniPackage) || this.isMiniPackage) {
                Editor edit = sharedPreferences.edit();
                edit.clear();
                edit.putString("isMiniPackage", "false");
                edit.commit();
            }
            return packageInfo.versionCode > last_version_code || ((packageInfo.versionCode == last_version_code && !TextUtils.equals(Globals.getInstalledVersionName(), last_version_name)) || this.isMiniPackage);
        } catch (Throwable e) {
            Log.e("AtlasInitializer", "Error to get PackageInfo >>>", e);
            throw new RuntimeException(e);
        }
    }
}