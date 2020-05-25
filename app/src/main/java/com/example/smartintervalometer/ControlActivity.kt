package com.example.smartintervalometer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.control_activity.*
import kotlinx.coroutines.*
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext


class ControlActivity : AppCompatActivity(), CoroutineScope {

    private val job = Job()
    private val uIScope = MainScope() // scope of the ui class


    companion object {
        var mBluetoothSocket: BluetoothSocket? = null
        lateinit var mBluetoothAdapter: BluetoothAdapter
        var mIsConnected: Boolean = false
        lateinit var mAddress: String
        lateinit var mName: String
        const val SEPARATOR_BYTE = ","
        const val END_BYTE = "\n"
        const val ON_BYTE = "S"
        const val PAUSE_BYTE = "P"
        const val RESET_BYTE = "R"
        //lateinit var bluetoothThread : BluetoothThread;
    }

    override val coroutineContext: CoroutineContext
        get() = job

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_activity)

        //seekbar event listener for value display
        seekBar_photo_count.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val progressLocal = progress + 1;
                seekBar_photo_count_value.text = "$progressLocal"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                return
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                return
            }
        })
        seekBar_photo_duration.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val progressLocal = progress + 1;
                seekBar_photo_duration_value.text = "$progressLocal s";
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                return
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                return
            }
        })
        seekBar_photo_pause.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val progressLocal = progress + 1;
                seekBar_photo_pause_value.text = "$progressLocal s";
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                return
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                return
            }
        })


        //get address and name of the device from intent
        mAddress = intent.getStringExtra(SelectDevice.EXTRA_ADDRESS)
        mName = intent.getStringExtra(SelectDevice.EXTRA_NAME)
        device_name.text = mName

        //init connection to setlected device
        ConnectToDevice(this).execute()

        //start and stop command binding
        start_button.setOnClickListener{sendCommand( ""+ (seekBar_photo_count.progress+1) + SEPARATOR_BYTE
                + (seekBar_photo_duration.progress+1) + SEPARATOR_BYTE
                + (seekBar_photo_pause.progress+1) + SEPARATOR_BYTE
                + ON_BYTE
                + END_BYTE)}
        pause_button.setOnClickListener{sendCommand(PAUSE_BYTE + END_BYTE)}
        reset_button.setOnClickListener{sendCommand(RESET_BYTE + END_BYTE)}

        disconnect_button.setOnClickListener{disconnect()}


        uIScope.launch() {
            while (true) {
                if (mBluetoothSocket != null) {
                    readBluetooth();
                }
                delay(200);
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uIScope.cancel()
    }

    private suspend fun readBluetooth() {
        val buffer = ByteArray(1024)
        var bytes: Int? = 0
        var res = ""
        withContext(Dispatchers.IO){
            bytes = mBluetoothSocket?.inputStream?.read(buffer)
        }

        for (i:Int  in 0 until bytes!!) {
            res += buffer[i].toChar()
        }
        val parts = res.split(",")
        val countValue = parts[0].toInt()
        val duration_value = parts[1]
        val delay_value = parts[2]
        val photoTaken = parts[3].toInt()


        progressBar3.progress = (photoTaken * 100) / countValue
    }



    private fun sendCommand(input: String) {
        if(mBluetoothSocket != null){
            try {
                mBluetoothSocket!!.outputStream.write(input.toByteArray())
            }catch (e: IOException){
                e.printStackTrace()
            }
        }
    }

    private fun disconnect () {
        try {
            if(mBluetoothSocket != null){
                mBluetoothSocket?.close()
                mBluetoothSocket = null
                mIsConnected = false
            }
            Toast.makeText(this, "disconnected from : $mName",Toast.LENGTH_SHORT).show()
        } catch (e: IOException){
            e.printStackTrace()
        }
        finish()
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {

        private var connectSuccess: Boolean = true
        private val context: Context = c

        override fun onPreExecute() {
            super.onPreExecute()
            Toast.makeText(context, "connexion to : $mName",Toast.LENGTH_SHORT).show()
        }

        override fun doInBackground(vararg params: Void?): String? {
            try{
                if (mBluetoothSocket == null || !mIsConnected) {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    Log.i("bluetooth", "get Default adapter")
                    val device : BluetoothDevice = mBluetoothAdapter.getRemoteDevice((mAddress))
                    Log.i("bluetooth", "get Device")
                    Log.i("bluetooth","device type : "+device.type)
                    mBluetoothSocket = device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
                    Log.i("bluetooth", "get Socket")
                    mBluetoothSocket!!.connect()
                    Log.i("bluetooth", "connected")

                }
            }catch (e: IOException){
                connectSuccess = false
                e.printStackTrace()
                Log.e("bluetooth", e.toString())
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess){
                Log.e("bluetooth", "error couldn't connect to device : $mName")
                Toast.makeText(context, "error couldn't connect to device : $mName",Toast.LENGTH_LONG).show()
                //context.
            } else {
                Toast.makeText(context, "connected to : $mName",Toast.LENGTH_SHORT).show()
                mIsConnected = true
            }
        }
    }

}
