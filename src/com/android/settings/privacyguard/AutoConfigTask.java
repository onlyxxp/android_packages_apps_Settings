package com.android.settings.privacyguard;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.ProgressDialog;
import android.app.AppOpsManager.OpEntry;
import android.app.AppOpsManager.PackageOps;
import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.widget.Toast;

import com.android.settings.privacyguard.PrivacyGuardManager.AppInfo;

public class AutoConfigTask extends AsyncTask<Void, Integer, Boolean> {
    private final int[] sIgnoreOps = new int[] {
            AppOpsManager.OP_COARSE_LOCATION, AppOpsManager.OP_FINE_LOCATION,
            AppOpsManager.OP_READ_CONTACTS, AppOpsManager.OP_WRITE_CONTACTS,
            AppOpsManager.OP_READ_CALL_LOG, AppOpsManager.OP_WRITE_CALL_LOG,
            AppOpsManager.OP_READ_CALENDAR, AppOpsManager.OP_WRITE_CALENDAR,
            AppOpsManager.OP_READ_SMS, AppOpsManager.OP_WRITE_SMS,
            AppOpsManager.OP_RECEIVE_SMS, AppOpsManager.OP_RECEIVE_MMS,
            AppOpsManager.OP_RECEIVE_WAP_PUSH, AppOpsManager.OP_SEND_SMS,
            AppOpsManager.OP_WAKE_LOCK, AppOpsManager.OP_SEND_MMS,
            AppOpsManager.OP_READ_MMS, AppOpsManager.OP_WRITE_MMS,
            AppOpsManager.OP_DELETE_SMS, AppOpsManager.OP_DELETE_SMS,
            AppOpsManager.OP_DELETE_MMS, AppOpsManager.OP_DELETE_CONTACTS,
            AppOpsManager.OP_DELETE_CALL_LOG, AppOpsManager.OP_ALARM_WAKEUP,
            AppOpsManager.OP_BOOT_COMPLETED, AppOpsManager.OP_RECORD_AUDIO,
            AppOpsManager.OP_WRITE_SETTINGS };

    private final int writeOpInterval = 5;
    private AppInfo mCurrentAppInfo;
    private final List<AppInfo> mAppInfos;
    private final AppOpsManager mAppOpsManager;
    private final WeakReference<Context> mContextRef;
    private final boolean shouldShowDialog;

    public AutoConfigTask(List<AppInfo> appInfos, Context context) {
        mContextRef = new WeakReference<Context>(context);
        mAppInfos = appInfos;
        mAppOpsManager = (AppOpsManager) context
                .getSystemService(Context.APP_OPS_SERVICE);

        shouldShowDialog = context instanceof Activity;

        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(mAppInfos.size() * (sIgnoreOps.length));
        mProgressDialog.setTitle("Auto Config");
    }

    private int mWorkedCount = 0;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onPreExecute() {
        if (shouldShowDialog) {
            mProgressDialog.show();
        }
    }

    @Override
    protected Boolean doInBackground(Void... arg0) {
        mWorkedCount = 0;
        if (mAppInfos == null || mAppInfos.size() == 0) {
            return false;
        }

        try {
            for (AppInfo appInfo : mAppInfos) {
                System.out.println("xxx autoconfig " + appInfo.packageName);
                mCurrentAppInfo = appInfo;

                PackageOps ignorePackageOps = mAppOpsManager.getOpsForPackage(
                        appInfo.uid, appInfo.packageName, sIgnoreOps).get(0);
                for (int ignoreOp : sIgnoreOps) {
                    // if (isNeedSetOp(ignorePackageOps, ignoreOp,
                    // AppOpsManager.MODE_IGNORED)) {
                    // setMode(appInfo, ignoreOp, AppOpsManager.MODE_IGNORED);
                    // }
                    setMode(appInfo, ignoreOp, AppOpsManager.MODE_IGNORED);
                    publishProgress(++mWorkedCount);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean isNeedSetOp(PackageOps packageOps, int ignoreOp, int setMode) {
        boolean needSetMode = true;
        for (OpEntry opEntry : packageOps.getOps()) {
            if (opEntry.getOp() == ignoreOp && opEntry.getMode() == setMode) {
                needSetMode = false;
                break;
            }
        }
        return needSetMode;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (shouldShowDialog) {
            mProgressDialog.dismiss();
        }

        if (mContextRef.get() != null) {
            Toast.makeText(mContextRef.get(), result ? "success" : "failed",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (shouldShowDialog) {
            mProgressDialog.setProgress(values[0]);
            if (mCurrentAppInfo != null) {
                mProgressDialog.setTitle("Auto Config  "
                        + mCurrentAppInfo.packageName);
            }
        }
    }

    private void setMode(AppInfo appInfo, int op, int mode) {
        mAppOpsManager.setMode(op, appInfo.uid, appInfo.packageName, mode);
        SystemClock.sleep(writeOpInterval);
    }
}