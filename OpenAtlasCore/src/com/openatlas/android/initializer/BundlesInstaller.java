package com.openatlas.android.initializer;

import android.app.Application;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;
import android.widget.Toast;

import com.openatlas.framework.Atlas;
import com.openatlas.framework.AtlasConfig;
import com.openatlas.log.Logger;
import com.openatlas.log.LoggerFactory;
import com.openatlas.runtime.RuntimeVariables;

import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BundlesInstaller {
	Logger log = LoggerFactory.getInstance("BundlesInstaller");
	private static boolean isTargetApp;
	private static BundlesInstaller instance;
	BundleDebug bundleDebug;
	private Application application;
	private boolean isInitialized;
	private boolean isDone;

	BundlesInstaller() {
	}

	void init(Application application, BundleDebug bundleDebug, boolean inTargetApp) {

		this.application = application;

		this.bundleDebug = bundleDebug;
		isTargetApp = inTargetApp;
		this.isInitialized = true;
	}

	static synchronized BundlesInstaller getInstance() {
		if (instance != null) {
			return instance;
		}
		synchronized (BundlesInstaller.class) {
			if (instance == null) {
				instance = new BundlesInstaller();
			}

		}
		return instance;
	}

	public synchronized void process(boolean onlyProcessAutos, boolean reInstall) {

		if (!this.isInitialized) {
			Log.e("BundlesInstaller", "Bundle Installer not initialized yet, process abort!");
		} else if (!this.isDone || reInstall) {
			ZipFile zipFile = null;
			try {
				zipFile = new ZipFile(this.application.getApplicationInfo().sourceDir);
				List<String> bundleList = fetchBundleFileList(zipFile, "lib/"+AtlasConfig.PRELOAD_DIR+"/libcom_", ".so");
				if (bundleList != null && bundleList.size() > 0 && getAvailableSize() < ((long) (((bundleList.size() * 2) * 1024) * 1024))) {
					new Handler(Looper.getMainLooper()).post(new Runnable() {
						public void run() {
							Toast.makeText(RuntimeVariables.androidApplication, "Ops  No Space ", Toast.LENGTH_SHORT).show();
						}
					});
				}
				if (onlyProcessAutos) {
					List bundles = new ArrayList();
					for (String location : bundleList) {
						for (String replace : AtlasConfig.AUTO) {
							if (location.contains(replace.replace(".", "_"))) {
								bundles.add(location);
							}
						}
					}
					processAutoStartBundles(zipFile, bundles, this.application);
				} else {
					installBundle(zipFile, (List) bundleList, this.application);
				}
				if (!reInstall) {
					Utils.UpdatePackageVersion(this.application);
				}

			} catch (IOException e) {
				Log.e("BundlesInstaller", "IOException while processLibsBundles >>>", e);
			} finally {
				if (zipFile != null) {
					try {
						zipFile.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (reInstall) {
					this.isDone = true;
				}

			}
			if (reInstall) {
				this.isDone = true;
			}
		}
	}

	private List<String> fetchBundleFileList(ZipFile zipFile, String prefix, String suffix) {
		List<String> arrayList = new ArrayList<String>();
		try {
			Enumeration<?> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				String name = ((ZipEntry) entries.nextElement()).getName();
				if (name.startsWith(prefix) && name.endsWith(suffix)) {
					arrayList.add(name);
				}
			}
		} catch (Throwable e) {
			Log.e("BundlesInstaller", "Exception while get bundles in assets or lib", e);
		}
		return arrayList;
	}

	private long getAvailableSize() {

		StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
		return ((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize());
	}

	public void processAutoStartBundles(ZipFile zipFile, List<String> bundles, Application application) {

		for (String location : bundles) {
			installBundle(zipFile, location, application);
		}
		if (isTargetApp) {
			for (String location : AtlasConfig.AUTO) {
				Bundle bundle = Atlas.getInstance().getBundle(location);
				if (bundle != null) {
					try {
						bundle.start();
					} catch (Throwable e) {
						Log.e("BundlesInstaller", "Could not auto start bundle: " + bundle.getLocation(), e);
					}
				}
			}
		}
	}

	private void installBundle(ZipFile zipFile, List<String> bundles, Application application) {
		for (String location : AtlasConfig.DELAY) {
			String mLocation = installBundle(bundles, location.replace(".", "_"));
			if (mLocation != null && mLocation.length() > 0) {
				installBundle(zipFile, mLocation, application);
				bundles.remove(mLocation);
			}
		}
		for (String location : bundles) {
			installBundle(zipFile, location, application);
		}
		if (isTargetApp) {
			String[] auto = AtlasConfig.AUTO;
			for (String location:auto){
				Bundle bundle = Atlas.getInstance().getBundle(location);
				if (bundle != null) {
					try {
						bundle.start();
					} catch (Throwable e) {
						Log.e("BundlesInstaller", "Could not auto start bundle: " + bundle.getLocation(), e);
					}
				}
			}

		}
	}

	private String installBundle(List<String> bundles, String location) {

		if (bundles == null || location == null) {
			return null;
		}
		for (String mLocation : bundles) {
			if (mLocation.contains(location)) {
				return mLocation;
			}
		}
		return null;
	}

	private boolean installBundle(ZipFile zipFile, String location, Application application) {

		log.info("processLibsBundle entryName " + location);
		this.bundleDebug.installExternalBundle(location);
		String fileNameFromEntryName = Utils.getFileNameFromEntryName(location);
		String packageNameFromEntryName = Utils.getPackageNameFromEntryName(location);
		if (packageNameFromEntryName == null || packageNameFromEntryName.length() <= 0) {
			return false;
		}
		File archvieFile = new File(new File(application.getFilesDir().getParentFile(), "lib"), fileNameFromEntryName);
		if (Atlas.getInstance().getBundle(packageNameFromEntryName) != null) {
			return false;
		}
		try {
			if (archvieFile.exists()) {
				Atlas.getInstance().installBundle(packageNameFromEntryName, archvieFile);
			} else {
				Atlas.getInstance().installBundle(packageNameFromEntryName, zipFile.getInputStream(zipFile.getEntry(location)));
			}
			log.info("Succeed to install bundle " + packageNameFromEntryName);
			return true;
		} catch (Throwable e) {
			Log.e("BundlesInstaller", "Could not install bundle.", e);
			return false;
		}
	}
}
