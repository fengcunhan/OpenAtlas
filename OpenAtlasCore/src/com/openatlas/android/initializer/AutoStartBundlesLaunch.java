package com.openatlas.android.initializer;

import android.os.Debug;

import com.openatlas.android.task.Coordinator;
import com.openatlas.android.task.Coordinator.TaggedRunnable;
import com.openatlas.boot.Globals;
import com.openatlas.framework.Atlas;
import com.openatlas.framework.AtlasConfig;
import com.openatlas.framework.BundleImpl;
import com.openatlas.log.Logger;
import com.openatlas.log.LoggerFactory;

import org.osgi.framework.BundleException;

class AutoStartBundlesLaunch {
	static Logger log=LoggerFactory.getInstance("AutoStartBundlesLaunch");
    static int startUpCount = 0;
    private final String[] AUTO = AtlasConfig.AUTO;
    private boolean autoLoad = false;
    AutoStartBundlesLaunch() {
    }

    static  int updateStartUpCount() {
        return startUpCount++;
    }

    void startAutoBundles() {
        if (!this.autoLoad) {
            startBundlesOnDemand(this.AUTO, true);

            this.autoLoad = true;
        }
    }

    void startBundlesOnDemand(final String[] bundles, final boolean notify) {
     
        for (final String location : bundles) {
        	Coordinator.postTask(new TaggedRunnable("AsyncTask for bundle:" + location) {
				
				@Override
				public void run() {
					  startBundleOnDemand(location);
			            if (notify) {
			            	updateStartUpCount();
			                if (startUpCount >= bundles.length) {
			                    Utils.notifyBundleInstalled(Globals.getApplication());
			                    startUpCount = 0;
			                }
			            }
					
				}
			});

         
        
        }
    }

    void startBundleOnDemand(String location) {
        BundleImpl bundleImpl = (BundleImpl) Atlas.getInstance().getBundleOnDemand(location);
        if (bundleImpl != null) {
            try {
                long threadCpuTimeNanos = Debug.threadCpuTimeNanos();
                long nanoTime = System.nanoTime();
                bundleImpl.startBundle();
                long realTime = (System.nanoTime() - nanoTime) / 1000000;
                log.debug("Start bundle " + location + " cost cputime:" + ((Debug.threadCpuTimeNanos() - threadCpuTimeNanos) / 1000000) + " ms." + " cost real time:" + realTime + " ms.");
            } catch (BundleException e) {
                e.printStackTrace();
            }
        }
    }


}
