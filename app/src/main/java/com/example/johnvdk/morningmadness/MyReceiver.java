package com.example.johnvdk.morningmadness;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyReceiver extends BroadcastReceiver {

    String address = null;
    public static String EXTRA_ADDRESS = "device_address";
    @Override
    public void onReceive(Context context, Intent intent) {

        address = intent.getStringExtra(STM32control.EXTRA_ADDRESS);
        Intent in = new Intent(context, STM32control.class);
        in.putExtra("lock", true);
        in.putExtra("device_address", address);
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(in);

        //This will send a notification message and show notification in notification tray
        //Something is messed up with startWakefulService, the notifications won't pop up.
//        ComponentName comp = new ComponentName(context.getPackageName(),
//                AlarmNotificationService.class.getName());
//        startWakefulService(context, (intent.setComponent(comp)));

    }
}
