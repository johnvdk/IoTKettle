package com.example.johnvdk.morningmadness;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class BluetoothOnlyControl extends AppCompatActivity {

    Button btnOn, btnOff, btnDis;
    String address = null;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_only_control);

        btnOn = (Button) findViewById(R.id.on_button);
        btnOff = (Button) findViewById(R.id.off_button);
        btnDis = (Button) findViewById(R.id.disconnect_button);
        address = getIntent().getStringExtra(MainActivity.SELECT_ADDRESS);

        final BluetoothControls Bluetooth = new BluetoothControls(address);
        new ConnectBT().execute(Bluetooth);

        //By-pass alarm and turn on kettle via button
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bluetooth.turnOnKettle();
            }
        });

        //By-pass kettle and turn on kettle via button
        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bluetooth.turnOffKettle();
            }
        });

        //Disconnect the bluetooth
        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bluetooth.Disconnect();
            }
        });
    }

    private class ConnectBT extends AsyncTask<BluetoothControls, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(BluetoothOnlyControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(BluetoothControls... devices) //while the progress dialog is shown, the connection is done in background
        {
            BluetoothControls Bluetooth = devices[0];
            Bluetooth.connectSocket();
            ConnectSuccess = Bluetooth.connectStatus();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
            }

            else
            {
                msg("Connected.");
            }
            progress.dismiss();
        }

    }

    //Function to call a toast
    private void msg(String str){
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
    }
}
