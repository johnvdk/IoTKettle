package com.example.johnvdk.morningmadness;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.icu.util.Calendar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import java.io.IOException;
import java.util.UUID;

public class STM32control extends AppCompatActivity {


    private PendingIntent pendingIntent;

    private RadioButton AMRadioButton, PMRadioButton;
    private static final int ALARM_REQUEST_CODE = 133;

    String address = null;
    private ProgressDialog progress;
    public static String SELECT_ADDRESS = "device_address";
    private boolean originMain = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stm32control);

        address = getAddress();
        final BluetoothControls Bluetooth = new BluetoothControls(address);

        if(!originMain){
            //Turns screen on if in sleep state
            new ConnectBT().execute(Bluetooth);
            Window win = this.getWindow();
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            msg("ALARM!!! Turning on Kettle Now");
            //this.startService(new Intent(this, AlarmSoundService.class));//Turn on a sound alarm
        }



        //Grab ID of AM and PM Radio buttons
        AMRadioButton = (RadioButton) findViewById(R.id.AM_radio_button);
        PMRadioButton = (RadioButton) findViewById(R.id.PM_radio_button);


        //Retrieve a PendingIntent that will perform a broadcast
        Intent alarmIntent = new Intent(STM32control.this, MyReceiver.class);
        alarmIntent.putExtra(SELECT_ADDRESS, address);    //Pass the bluetooth device address

        pendingIntent = PendingIntent.getBroadcast(STM32control.this, ALARM_REQUEST_CODE, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Find id of Edit Text boxes
        final EditText editTextMins = (EditText) findViewById(R.id.input_interval_time_mins);
        final EditText editTextHrs = (EditText) findViewById(R.id.input_interval_time_hours);

        //Set On CLick over start alarm button
        findViewById(R.id.start_alarm_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String MinsSelect = editTextMins.getText().toString().trim();//get interval from edittext
                String HrsSelect = editTextHrs.getText().toString().trim();//get interval from edittext

                if (!MinsSelect.equals("") && !MinsSelect.equals("")){
                    int mins = Integer.parseInt(MinsSelect);
                    int hrs = Integer.parseInt(HrsSelect);
                    triggerAlarmManager(hrs, mins, Bluetooth);
                }

                else{
                    msg("Please fill in both fields");
                }
            }
        });

        //set on click over stop alarm button
        findViewById(R.id.stop_alarm_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Stop alarm manager
                stopAlarmManager();
            }
        });

    }



    //Main code structure obtained from instructable written by Deyson on how to connect bluetooth
    private class ConnectBT extends AsyncTask<BluetoothControls, Void, BluetoothControls>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(STM32control.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected BluetoothControls doInBackground(BluetoothControls... devices) //while the progress dialog is shown, the connection is done in background
        {
            BluetoothControls Bluetooth = devices[0];
            Bluetooth.connectSocket();
            ConnectSuccess = Bluetooth.connectStatus();
            return Bluetooth;
        }
        @Override
        protected void onPostExecute(BluetoothControls Bluetooth) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(Bluetooth);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
            }

            else
            {
                msg("Connected.");
            }
            progress.dismiss();

            if(!originMain){
                Bluetooth.turnOnKettle();
            }
        }

    }

    //Function to call a toast
    private void msg(String str){
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
    }

    //Trigger alarm with user-inputted time
    public void triggerAlarmManager(int hr, int min, BluetoothControls Bluetooth) {
        if (hr < 0 || hr > 12){ //Hours must confine to the 12 hour clock
            msg("Error: Please enter a number of hours between 0 and 12");
        }
        else if (min < 0 || min > 59){ //Minutes must confine to 12 hour clock
            msg("Error: Please enter a number of minutes between 0 and 59");
        }
        else{

            //Establish AM or PM
            String AMorPM = " AM";
            int timeHr = hr;
            if (PMRadioButton.isChecked()){
                hr += 12;
                AMorPM = " PM";
            }

            // get a Calendar object with current time
            Calendar cal = Calendar.getInstance();

            String whatDay = " today";

            //If alarm set for a time earlier than current time, assume it's the next day
            if((cal.get(Calendar.HOUR) >= timeHr) && (cal.get(Calendar.MINUTE) >= min)){
                int day = cal.get(Calendar.DAY_OF_YEAR);
                cal.set(Calendar.DAY_OF_YEAR, (day + 1));
                whatDay = " tomorrow";

            }
            //Set the desired time for the alarm from the user input
            cal.set(Calendar.HOUR_OF_DAY, hr);
            cal.set(Calendar.MINUTE, min);

            //Disconnect current bluetooth connection so that the alarm can be called
            Bluetooth.Disconnect();

            AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);//Get instance of alarm manager
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);//Set the alarm by converting the desired time into milliseconds


            //Notify the user what time the alarm will be set
            if (min < 10){
                msg("Alarm Set for " + timeHr + ":0" + min + AMorPM + whatDay);
            }
            else {
                msg("Alarm Set for " + timeHr + ":" + min + AMorPM + whatDay);
            }

        }
    }

    private String getAddress(){
        String tempaddress;
        if (getIntent().getBooleanExtra("init", true)){
            tempaddress = getIntent().getStringExtra(MainActivity.SELECT_ADDRESS);
            originMain = true;
            return tempaddress;
        }

        else if (getIntent().getBooleanExtra("lock", false)){
            tempaddress = getIntent().getStringExtra(MyReceiver.SELECT_ADDRESS);
            originMain = false;
            return tempaddress;
        }

        return null;
    }

    //Stop/Cancel alarm manager
    public void stopAlarmManager() {

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);//cancel the alarm manager of the pending intent


        //Stop the Media Player Service to stop sound
        stopService(new Intent(STM32control.this, AlarmSoundService.class));

//        //remove the notification from notification tray
//        NotificationManager notificationManager = (NotificationManager) this
//                .getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.cancel(AlarmNotificationService.NOTIFICATION_ID);

        msg("Alarm Canceled/Stop by User");
    }
}