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

    Button btnOn, btnOff, btnDis;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static String SELECT_ADDRESS = "device_address";
    private boolean originMain = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stm32control);

        new ConnectBT().execute();//Sets up bluetooth connectivity

        address = getIntent().getStringExtra(MainActivity.SELECT_ADDRESS);//get the device address passed from MainActivity
        if(getIntent().getBooleanExtra("init", true)){
            originMain = true;//flag for ensuring kettle is off first time activity called
        }

        if(getIntent().getBooleanExtra("lock", false)){
            originMain = false;

            //Turns screen on if in sleep state
            Window win = this.getWindow();
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            address = getIntent().getStringExtra(MyReceiver.SELECT_ADDRESS);//get the device address passed from MyReceiver
            msg("ALARM!!! Turning on Kettle Now");
            this.startService(new Intent(this, AlarmSoundService.class));//Turn on a sound alarm
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
                    triggerAlarmManager(hrs, mins);
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

        btnOn = (Button) findViewById(R.id.on_button);
        btnOff = (Button) findViewById(R.id.off_button);
        btnDis = (Button) findViewById(R.id.disconnect_button);


        //By-pass alarm and turn on kettle via button
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnKettle();
            }
        });

        //By-pass kettle and turn on kettle via button
        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOffKettle();
            }
        });

        //Disconnect the bluetooth
        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect();
            }
        });


    }

    //Turns on the kettle by sending a command over the bluetooth connection
    public void turnOnKettle() {
        if (btSocket!= null){
            try{
                btSocket.getOutputStream().write("TO".toString().getBytes()); //send Turn On kettle command over bluetooth
            } catch (IOException e){msg("Error turning kettle on"); }
        }
    }

    //Turns off the kettle by sending a command over the bluetooth connection
    public void turnOffKettle() {
        if (btSocket!= null){
            try{
                btSocket.getOutputStream().write("TF".toString().getBytes()); //send Turn Off kettle command over bluetooth
            } catch (IOException e){msg("Error turning kettle off"); }
        }
    }

    //Disconnects the current bluetooth connection
    public void Disconnect(){
        if (btSocket !=null){
            try{
                btSocket.close();
            } catch (IOException e){msg("Error closing socket");}
        }
    }

    //Main code structure obtained from instructable written by Deyson on how to connect bluetooth
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(STM32control.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }

            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();

            //If STM32control called by MainActivity first time, turn the kettle off
            if(originMain){
                turnOffKettle();
            }
            //If STM32control called by MyReceiver (Alarm) turn the kettle on
            else{
                turnOnKettle();
            }
        }

    }

    //Function to call a toast
    private void msg(String str){
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
    }

    //Trigger alarm with user-inputted time
    public void triggerAlarmManager(int hr, int min) {
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
            Disconnect();

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