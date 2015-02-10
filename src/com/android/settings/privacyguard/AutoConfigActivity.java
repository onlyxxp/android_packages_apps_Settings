package com.android.settings.privacyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoConfigActivity extends Activity {
    final boolean[] mSelectedIgnore = new boolean[]{
            true,
            true,
            true,
    };
    final String[] items = new String[]{
            "禁止开机自启",
            "禁止保持唤醒状态",
            "禁止定时唤醒",
    };
    final Integer[] mSelectedOpsFromDialog = new Integer[]{
            AppOpsManager.OP_BOOT_COMPLETED,
            AppOpsManager.OP_WAKE_LOCK,
            AppOpsManager.OP_ALARM_WAKEUP,
    };

    public static void start(Context context, List<? extends Parcelable> infos) {
        ArrayList<Parcelable> list = new ArrayList<Parcelable>(infos);
        Intent intent = new Intent(context, AutoConfigActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putParcelableArrayListExtra("data", list);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ArrayList<PrivacyGuardManager.AppInfo> infos = getIntent().getParcelableArrayListExtra("data");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle("自动配置");
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setMultiChoiceItems(items, mSelectedIgnore, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                mSelectedIgnore[which] = isChecked;
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new AutoConfigTask(infos, AutoConfigActivity.this).execute();
            }
        });
        builder.show();
    }


    private class AutoConfigTask extends AsyncTask<Void, Integer, Boolean> {
        private final ArrayList<Integer> sIgnoreOps = new ArrayList<Integer>(Arrays.asList(
                AppOpsManager.OP_READ_CONTACTS, AppOpsManager.OP_WRITE_CONTACTS,
                AppOpsManager.OP_READ_CALL_LOG, AppOpsManager.OP_WRITE_CALL_LOG,
                AppOpsManager.OP_READ_CALENDAR, AppOpsManager.OP_WRITE_CALENDAR,
                AppOpsManager.OP_READ_SMS, AppOpsManager.OP_WRITE_SMS,
                AppOpsManager.OP_RECEIVE_SMS, AppOpsManager.OP_RECEIVE_MMS,
                AppOpsManager.OP_RECEIVE_WAP_PUSH, AppOpsManager.OP_SEND_SMS,
                AppOpsManager.OP_SEND_MMS,
                AppOpsManager.OP_READ_MMS, AppOpsManager.OP_WRITE_MMS,
                AppOpsManager.OP_DELETE_SMS, AppOpsManager.OP_DELETE_SMS,
                AppOpsManager.OP_DELETE_MMS, AppOpsManager.OP_DELETE_CONTACTS,
                AppOpsManager.OP_DELETE_CALL_LOG
        ));

        private final int writeOpInterval = 5;
        private PrivacyGuardManager.AppInfo mCurrentAppInfo;
        private final List<PrivacyGuardManager.AppInfo> mAppInfos;
        private final AppOpsManager mAppOpsManager;
        private final WeakReference<Context> mContextRef;

        public AutoConfigTask(List<PrivacyGuardManager.AppInfo> appInfos, Context context) {
            for (int i = 0; i < mSelectedIgnore.length; i++) {
                if (mSelectedIgnore[i]) {
                    sIgnoreOps.add(mSelectedOpsFromDialog[i]);
                }
            }

            mContextRef = new WeakReference<Context>(context);
            mAppInfos = appInfos;
            mAppOpsManager = (AppOpsManager) context
                    .getSystemService(APP_OPS_SERVICE);

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMax(mAppInfos.size() * (sIgnoreOps.size()));
            mProgressDialog.setTitle("Auto Config");
        }

        private int mWorkedCount = 0;
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            mWorkedCount = 0;
            if (mAppInfos == null || mAppInfos.size() == 0) {
                return false;
            }

            try {
                for (PrivacyGuardManager.AppInfo appInfo : mAppInfos) {
                    System.out.println("xxx autoconfig " + appInfo.packageName);
                    mCurrentAppInfo = appInfo;

//                    AppOpsManager.PackageOps ignorePackageOps = mAppOpsManager.getOpsForPackage(
//                            appInfo.uid, appInfo.packageName, sIgnoreOps).get(0);
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

        private boolean isNeedSetOp(AppOpsManager.PackageOps packageOps, int ignoreOp, int setMode) {
            boolean needSetMode = true;
            for (AppOpsManager.OpEntry opEntry : packageOps.getOps()) {
                if (opEntry.getOp() == ignoreOp && opEntry.getMode() == setMode) {
                    needSetMode = false;
                    break;
                }
            }
            return needSetMode;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mProgressDialog.dismiss();

            if (mContextRef.get() != null) {
                Toast.makeText(mContextRef.get(), result ? "success" : "failed",
                        Toast.LENGTH_LONG).show();
            }

            AutoConfigActivity.this.finish();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mProgressDialog.setProgress(values[0]);
            if (mCurrentAppInfo != null) {
                mProgressDialog.setTitle("Auto Config  "
                        + mCurrentAppInfo.packageName);
            }
        }

        private void setMode(PrivacyGuardManager.AppInfo appInfo, int op, int mode) {
            mAppOpsManager.setMode(op, appInfo.uid, appInfo.packageName, mode);
            SystemClock.sleep(writeOpInterval);
        }
    }
}

