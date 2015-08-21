package com.openatlas.android.initializer;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.openatlas.framework.Atlas;
import com.openatlas.framework.AtlasConfig;
import com.openatlas.framework.BundleImpl;
import com.openatlas.framework.PlatformConfigure;
import com.openatlas.framework.bundlestorage.BundleArchiveRevision.DexLoadException;
import com.openatlas.log.Logger;
import com.openatlas.log.LoggerFactory;

import org.osgi.framework.Bundle;

public class OptDexProcess {
	Logger log=LoggerFactory.getInstance("OptDexProcess");
    private static OptDexProcess instance;
    private Application mApplication;
    private boolean isInitialized;
    private boolean notifyInstalled;

    OptDexProcess() {
    }

    public static synchronized OptDexProcess getInstance() {
        synchronized (OptDexProcess.class) {
            if (instance == null) {
                instance = new OptDexProcess();
            }
         
        }
        return instance;
    }

    void init(Application application) {
        this.mApplication = application;
        this.isInitialized = true;
    }

    public synchronized void processPackages(boolean onlyOptAutos, boolean noNeedNotifyUI) {
        if (!this.isInitialized) {
            Log.e("OptDexProcess", "Bundle Installer not initialized yet, process abort!");
        } else if (!this.notifyInstalled || noNeedNotifyUI) {
            long currentTimeMillis;
            if (onlyOptAutos) {
                currentTimeMillis = System.currentTimeMillis();
                runOptDexAuto();
                if (!noNeedNotifyUI) {
                    finishInstalled();
                }
                log.debug("dexopt auto start bundles cost time = " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            } else {
                currentTimeMillis = System.currentTimeMillis();
                runOptDexNonDelay();
                log.debug("dexopt bundles not delayed cost time = " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
                if (!noNeedNotifyUI) {
                    finishInstalled();
                }
                currentTimeMillis = System.currentTimeMillis();
                getInstance().runOptDexDelay();
                log.debug("dexopt delayed bundles cost time = " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            }
            if (!noNeedNotifyUI) {
                this.notifyInstalled = true;
            }
        }
    }

    private void finishInstalled() {
        Utils.saveAtlasInfoBySharedPreferences(this.mApplication);
        System.setProperty("BUNDLES_INSTALLED", "true");
        this.mApplication.sendBroadcast(new Intent(PlatformConfigure.ACTION_BROADCAST_BUNDLES_INSTALLED));
    }

    private void runOptDexNonDelay() {
        for (Bundle bundle : Atlas.getInstance().getBundles()) {
            if (!(bundle == null || contains(AtlasConfig.STORE, bundle.getLocation()))) {
                try {
                    ((BundleImpl) bundle).optDexFile();
                } catch (Throwable e) {
                    if (e instanceof DexLoadException) {
                        throw ((RuntimeException) e);
                    }
                    Log.e("OptDexProcess", "Error while dexopt >>>", e);
                }
            }
        }
    }

    private void runOptDexDelay() {
 
        for (String location : AtlasConfig.STORE) {
            Bundle bundle = Atlas.getInstance().getBundle(location);
            if (bundle != null) {
                try {
                    ((BundleImpl) bundle).optDexFile();
                } catch (Throwable e) {
                    if (e instanceof DexLoadException) {
                        throw ((RuntimeException) e);
                    }
                    Log.e("OptDexProcess", "Error while dexopt >>>", e);
                }
            }
        }
    }

    private void runOptDexAuto() {

        for (String location :AtlasConfig.AUTO) {
            Bundle mBundle = Atlas.getInstance().getBundle(location);
            if (mBundle != null) {
                try {
                    ((BundleImpl) mBundle).optDexFile();
                } catch (Throwable e) {
                    if (e instanceof DexLoadException) {
                        throw ((RuntimeException) e);
                    }
                    Log.e("OptDexProcess", "Error while dexopt >>>", e);
                }
            }
        }
    }

    private boolean contains(String[] bundles, String bundle) {
        if (bundles == null || bundle == null) {
            return false;
        }
        for (String tmp : bundles) {
            if (tmp != null && tmp.equals(bundle)) {
                return true;
            }
        }
        return false;
    }
}
