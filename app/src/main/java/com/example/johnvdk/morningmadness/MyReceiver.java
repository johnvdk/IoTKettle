package com.example.johnvdk.morningmadness;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyReceiver extends BroadcastReceiver {

    String address = null;
    public static String SELECT_ADDRESS = "device_address";
    @Override
    public void onReceive(Context context, Intent intent) {

        //Retrieve the bluetooth device address from STM32control
        address = intent.getStringExtra(STM32control.SELECT_ADDRESS);

        //Create new intent to call STM32control again with the alarm
        Intent in = new Intent(context, STM32control.class);
        in.putExtra("init", false);
        in.putExtra("lock", true); //alarm flag
        in.putExtra("device_address", address);//Pass the bluetooth device address
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//Create a new activity with the intent
        context.startActivity(in);

        //This will send a notification message and show notification in notification tray
        //Something is messed up with startWakefulService, the notifications won't pop up.
//        ComponentName comp = new ComponentName(context.getPackageName(),
//                AlarmNotificationService.class.getName());
//        startWakefulService(context, (intent.setComponent(comp)));

    }
}
