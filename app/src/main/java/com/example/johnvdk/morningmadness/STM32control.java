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
    public static String EXTRA_ADDRESS = "device_address";
    private boolean originMain = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stm32control);

        new ConnectBT().execute();

        address = getIntent().getStringExtra(MainActivity.EXTRA_ADDRESS);
        if(getIntent().getBooleanExtra("init", true)){
            originMain = true;
        }

        if(getIntent().getBooleanExtra("lock", false)){
            originMain = false;
            Window win = this.getWindow();
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            address = getIntent().getStringExtra(MyReceiver.EXTRA_ADDRESS);
            Log.d("Trying this", address);
            msg("ALARM!!! Turning on Kettle Now");
            this.startService(new Intent(this, AlarmSoundService.class));
        }



        //Find id of all radio buttons
        AMRadioButton = (RadioButton) findViewById(R.id.AM_radio_button);
        PMRadioButton = (RadioButton) findViewById(R.id.PM_radio_button);


        /* Retrieve a PendingIntent that will perform a broadcast */
        Intent alarmIntent = new Intent(STM32control.this, MyReceiver.class);
        alarmIntent.putExtra(EXTRA_ADDRESS, address);
        pendingIntent = PendingIntent.getBroadcast(STM32control.this, ALARM_REQUEST_CODE, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Find id of Edit Text
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

        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnKettle();
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOffKettle();
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect();
            }
        });


    }
    public void turnOnKettle() {
        if (btSocket!= null){
            try{
                btSocket.getOutputStream().write("TO".toString().getBytes());
            } catch (IOException e){msg("Error turning kettle on"); }
        }
    }

    public void turnOffKettle() {
        if (btSocket!= null){
            try{
                btSocket.getOutputStream().write("TF".toString().getBytes());
            } catch (IOException e){msg("Error turning kettle off"); }
        }
    }
    public void Disconnect(){
        if (btSocket !=null){
            try{
                btSocket.close();
            } catch (IOException e){msg("Error closing socket");}
        }
    }


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
            if(originMain){
                turnOffKettle();
                Disconnect();
            }
            else{
                turnOnKettle();
            }
        }

    }

    private void msg(String s){
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    //Trigger alarm manager with entered time interval
    public void triggerAlarmManager(int hr, int min) {
        if (hr < 0 || hr > 12){
            msg("Error: Please enter a number of hours between 0 and 12");
        }
        else if (min < 0 || min > 59){
            msg("Error: Please enter a number of minutes between 0 and 59");
        }
        else{

            String AMorPM = " AM";
            int timeHr = hr;
            if (PMRadioButton.isChecked()){
                hr += 12;
                AMorPM = " PM";
            }
            // get a Calendar object with current time
            Calendar cal = Calendar.getInstance();
            if((cal.get(Calendar.HOUR) >= hr) && (cal.get(Calendar.MINUTE) >= min)){
                int day = cal.get(Calendar.DAY_OF_YEAR);
                cal.set(Calendar.DAY_OF_YEAR, day + 1);
            }
            cal.set(Calendar.HOUR_OF_DAY, hr);
            cal.set(Calendar.MINUTE, min);

            AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);//get instance of alarm manager
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);//set alarm manager with entered timer by converting into milliseconds

            if (min < 10){
                msg("Alarm Set for " + timeHr + ":0" + min + AMorPM);
            }
            else {
                msg("Alarm Set for " + timeHr + ":0" + min + AMorPM);
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