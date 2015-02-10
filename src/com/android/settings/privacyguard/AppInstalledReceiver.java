package com.android.settings.privacyguard;

import java.util.ArrayList;

import com.android.settings.privacyguard.PrivacyGuardManager.AppInfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AppInstalledReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AppInfo appInfo = new AppInfo();
        appInfo.uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
        appInfo.packageName = intent.getData().getSchemeSpecificPart();

        final String text = "AppInstalledReceiver install " + appInfo.uid + "  " + appInfo.packageName;
        System.out.println("AppInstalledReceiver onReceive " + text);
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

        ArrayList<AppInfo> infos = new ArrayList<AppInfo>();
        infos.add(appInfo);
        AutoConfigActivity.start(context, infos);
    }

}
