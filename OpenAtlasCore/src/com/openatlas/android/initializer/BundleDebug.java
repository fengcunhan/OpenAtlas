package com.openatlas.android.initializer;

import android.os.Environment;
import android.util.Log;

import com.openatlas.framework.Atlas;
import com.openatlas.log.Logger;
import com.openatlas.log.LoggerFactory;

import java.io.File;
import java.util.ArrayList;


class BundleDebug {
	Logger  log=LoggerFactory.getInstance("Debug");
    boolean isDebugable;
    private boolean isDebug;
    private ArrayList<String> debugBundles;
    private final String debugFolder;

    public BundleDebug() {
        this.isDebug = false;
        this.isDebugable = false;
        this.debugBundles = new ArrayList();
        this.debugFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/bundle-debug";
        this.isDebug = true;
    }

    public boolean isDebugable() {

        if (!this.isDebug) {
            return false;
        }
        File file = new File(this.debugFolder);
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File mFile:listFiles){
                if (mFile.isFile() && mFile.getName().endsWith(".so")) {
                    this.debugBundles.add(mFile.getAbsolutePath());
                    log.debug( "found external bundle " + mFile.getAbsolutePath());
                    this.isDebugable = true;
                }
            }

        }
        return this.isDebugable;
    }

    public boolean installExternalBundle(String location) {

        if (!this.isDebug || this.debugBundles.size() <= 0) {
            return false;
        }
        for (String bundle:debugBundles){

            log.debug( "processLibsBundle filePath " + bundle);
            if (bundle.contains(Utils.getFileNameFromEntryName(location).substring(3))) {
                File file = new File(bundle);
                String replace = Utils.getBaseFileName(file.getName()).replace("_", ".");
                if (Atlas.getInstance().getBundle(replace) == null) {
                    try {
                        Atlas.getInstance().installBundle(replace, file);
                    } catch (Throwable th) {
                        Log.e("BundleDebug", "Could not install external bundle.", th);
                    }
                    log.debug( "Succeed to install external bundle " + replace);
                }
                file.delete();
                return true;
            }
        }
        return false;
    }
}
