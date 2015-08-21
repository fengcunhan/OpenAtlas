package com.openatlas.android.initializer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.openatlas.android.task.Coordinator;
import com.openatlas.android.task.Coordinator.TaggedRunnable;



public class ReplacedReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        try {
            if (("onReceive: " + intent) != null) {
                intent.getAction();
            }
        } catch (Exception e) {
        }
        if (InstallSolutionConfig.install_when_onreceive && !InstallSolutionConfig.install_when_oncreate) {
            Coordinator.postTask(new TaggedRunnable("ProcessBundlesInReceiver") {
				
				@Override
				public void run() {
		          BundlesInstaller.getInstance().process(false, true);
		          OptDexProcess.getInstance().processPackages(false, true);
					
				}
			});

        }
    }
}
