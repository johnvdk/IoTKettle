package com.example.johnvdk.morningmadness;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    //Pending intent instance
    private PendingIntent pendingIntent;

    private RadioButton AMRadioButton, PMRadioButton;
    private BluetoothAdapter myBT;
    private Button btnPaired;
    private ListView deviceList;
    private Set <BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";

    //Alarm Request Code
    private static final int ALARM_REQUEST_CODE = 133;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BTsetUp();

        //Find id of all radio buttons
        AMRadioButton = (RadioButton) findViewById(R.id.AM_radio_button);
        PMRadioButton = (RadioButton) findViewById(R.id.PM_radio_button);

        btnPaired = (Button) findViewById(R.id.BT_button);
        deviceList = (ListView) findViewById(R.id.device_list_view);


        /* Retrieve a PendingIntent that will perform a broadcast */
        Intent alarmIntent = new Intent(MainActivity.this, MyReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, ALARM_REQUEST_CODE, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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

//                else{
//                    Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
//                }

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

        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPairedDevices();
            }
        });

    }


    //Trigger alarm manager with entered time interval
    public void triggerAlarmManager(int hr, int min) {
        if (hr < 0 || hr > 12){
            Toast.makeText(this, "Error: Please enter a number of hours between 0 and 12", Toast.LENGTH_SHORT).show();
        }
        else if (min < 0 || min > 59){
            Toast.makeText(this, "Error: Please enter a number of minutes between 0 and 59",Toast.LENGTH_SHORT).show();
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
            // add alarmTriggerTime seconds to the calendar object
            cal.set(Calendar.HOUR_OF_DAY, hr);
            cal.set(Calendar.MINUTE, min);

            AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);//get instance of alarm manager
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);//set alarm manager with entered timer by converting into milliseconds

            if (min < 10){
                Toast.makeText(this, "Alarm Set for " + timeHr + ":0" + min + AMorPM, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Alarm Set for " + timeHr + ":" + min + AMorPM, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Stop/Cancel alarm manager
    public void stopAlarmManager() {

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);//cancel the alarm manager of the pending intent


        //Stop the Media Player Service to stop sound
        stopService(new Intent(MainActivity.this, AlarmSoundService.class));

        //remove the notification from notification tray
        NotificationManager notificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(AlarmNotificationService.NOTIFICATION_ID);

        Toast.makeText(this, "Alarm Canceled/Stop by User.", Toast.LENGTH_SHORT).show();
    }

    private void BTsetUp() {
        myBT = BluetoothAdapter.getDefaultAdapter();
        if (myBT == null){
            Toast.makeText(this, "No Bluetooth capability", Toast.LENGTH_SHORT).show();
            //finish();
        }

        else {
            if (myBT.isEnabled()){ }
            else {
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, 1);
            }
        }
    }

    private void getPairedDevices() {
        pairedDevices = myBT.getBondedDevices();
        ArrayList tempList = new ArrayList();

        if (pairedDevices.size() > 0){
            for (BluetoothDevice bt : pairedDevices){
                tempList.add(bt.getName() + "\n" + bt.getAddress());
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, tempList);
        deviceList.setAdapter(adapter);
        deviceList.setOnItemClickListener(myListClickListener);
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String info = ((TextView) view).getText().toString();
            String address =info.substring(info.length() - 17);

            Intent i = new Intent(MainActivity.this, STM32control.class);

            i.putExtra(EXTRA_ADDRESS, address);
            startActivity(i);
        }
    };
}
