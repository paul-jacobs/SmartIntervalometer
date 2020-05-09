package com.example.smartintervalometer

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_select_device.*

class SelectDevice : AppCompatActivity() {

    private lateinit var mBluetoothAdapter : BluetoothAdapter
    private lateinit var mPairedDevices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    companion object{
        const val EXTRA_ADDRESS: String = "Device_address"
        const val EXTRA_NAME: String = "Device_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device)

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null){
            Toast.makeText(applicationContext,"this device doesn't support bluetooth",Toast.LENGTH_SHORT).show()
            return
        }

        if (!mBluetoothAdapter!!.isEnabled){
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        refresh_button.setOnClickListener{ pairedDeviceList() }
        pairedDeviceList()
    }

    private fun pairedDeviceList(){
        mPairedDevices = mBluetoothAdapter!!.bondedDevices
        val list : ArrayList<BluetoothDevice> = ArrayList()
        val nameList : ArrayList<String> = ArrayList()

        if (mPairedDevices.isNotEmpty()){
            for (device: BluetoothDevice in mPairedDevices) {
                list.add(device)
                nameList.add(device.name)
                Log.i("device : ", device.address+":"+device.name)
            }
        }else {
            Toast.makeText(applicationContext,"no paired devices",Toast.LENGTH_SHORT).show()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nameList)
        device_select_list.adapter = adapter;
        device_select_list.onItemClickListener = AdapterView.OnItemClickListener{_,_,position, _ ->
            val device: BluetoothDevice = list[position]
            val address: String = device.address
            val name: String = device.name

            val intent = Intent(this, ControlActivity::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            intent.putExtra(EXTRA_NAME, name)
            startActivity(intent)

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_ENABLE_BLUETOOTH){
            if (resultCode == Activity.RESULT_OK) {
                if (mBluetoothAdapter!!.isEnabled){
                    Toast.makeText(applicationContext,"bluetooth as been enabled",Toast.LENGTH_SHORT).show()
                    pairedDeviceList()
                } else {
                    Toast.makeText(applicationContext,"bluetooth as been disabled",Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
}
