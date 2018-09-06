package com.example.johnvdk.morningmadness;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.support.v4.app.JobIntentService;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;

public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "ALARM!! ALARM!!", Toast.LENGTH_SHORT).show();

        context.startService(new Intent(context, AlarmSoundService.class));

        //This will send a notification message and show notification in notification tray
        //Something is messed up with startWakefulService, the notifications won't pop up.
        ComponentName comp = new ComponentName(context.getPackageName(),
                AlarmNotificationService.class.getName());
        startWakefulService(context, (intent.setComponent(comp)));

    }


}
