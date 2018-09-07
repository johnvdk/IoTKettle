package com.example.johnvdk.morningmadness;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

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

        btnPaired = (Button) findViewById(R.id.BT_button);
        deviceList = (ListView) findViewById(R.id.device_list_view);

        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPairedDevices();
            }
        });

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
            i.putExtra("init", true);
            startActivity(i);
        }
    };
}
