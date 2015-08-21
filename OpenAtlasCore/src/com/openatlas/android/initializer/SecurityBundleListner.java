package com.openatlas.android.initializer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.openatlas.framework.Atlas;
import com.openatlas.runtime.RuntimeVariables;
import com.openatlas.util.ApkUtils;
import com.openatlas.util.StringUtils;

import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceEvent;

import java.io.File;


public class SecurityBundleListner implements BundleListener {
    public static final String PUBLIC_KEY = "";
    ProcessHandler mProcessHandler;
    private Handler mSecurityCheckHandler;
    private HandlerThread mHandlerThread;


    private final class SecurityCheckHandler extends Handler {


        public SecurityCheckHandler(Looper looper) {
            super(looper);


        }

        @Override
        public void handleMessage(Message message) {

            if (message != null) {
                String location = (String) message.obj;
                if (!TextUtils.isEmpty(location) && !TextUtils.isEmpty(SecurityBundleListner.PUBLIC_KEY)) {
                    File bundleFile = Atlas.getInstance().getBundleFile(location);
                    if (bundleFile != null) {
                        if (!StringUtils.contains(ApkUtils.getApkPublicKey(bundleFile.getAbsolutePath()), SecurityBundleListner.PUBLIC_KEY)) {
                            Log.e("SecurityBundleListner", "Security check failed. " + location);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(RuntimeVariables.androidApplication, "Public Key error，PLZ update your  public key", Toast.LENGTH_SHORT).show();
                                    mProcessHandler.sendEmptyMessageDelayed(0, 5000);
                                }
                            });

                        }

                    }
                }
            }
        }
    }

    public static class ProcessHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            Process.killProcess(Process.myPid());
        }
    }


    public SecurityBundleListner() {
        this.mHandlerThread = null;
        this.mProcessHandler = new ProcessHandler();

        this.mHandlerThread = new HandlerThread("Check bundle security");
        this.mHandlerThread.start();
        this.mSecurityCheckHandler = new SecurityCheckHandler(this.mHandlerThread.getLooper());
    }

    public void bundleChanged(BundleEvent bundleEvent) {

        switch (bundleEvent.getType()) {
            case ServiceEvent.REGISTERED :
            case FrameworkEvent.STARTLEVEL_CHANGED :
                Message obtain = Message.obtain();
                obtain.obj = bundleEvent.getBundle().getLocation();
                this.mSecurityCheckHandler.sendMessage(obtain);
                return;
            default:
                return;
        }
    }


}
