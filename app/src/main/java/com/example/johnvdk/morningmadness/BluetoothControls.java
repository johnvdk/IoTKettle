package com.example.johnvdk.morningmadness;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

public class BluetoothControls {

    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    private boolean ConnectSuccess = true;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothControls(String address){
        this.address = address;
    }

    public void connectSocket(){
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
    }

    //Turns on the kettle by sending a command over the bluetooth connection
    public void turnOnKettle() {
        if (btSocket!= null){
            try{
                btSocket.getOutputStream().write("TO".toString().getBytes()); //send Turn On kettle command over bluetooth
            } catch (IOException e){}//msg("Error turning kettle on"); }
        }
    }

    //Turns off the kettle by sending a command over the bluetooth connection
    public void turnOffKettle() {
        if (btSocket!= null){
            try{
                btSocket.getOutputStream().write("TF".toString().getBytes()); //send Turn Off kettle command over bluetooth
            } catch (IOException e){}//msg("Error turning kettle off"); }
        }
    }

    //Disconnects the current bluetooth connection
    public void Disconnect(){
        if (btSocket !=null){
            try{
                btSocket.close();
            } catch (IOException e){}//msg("Error closing socket");}
        }
    }

    public boolean connectStatus(){
        return ConnectSuccess;
    }

}
