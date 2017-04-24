package app.iot.com.ioteggble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.IBinder;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Jihoon Yang <j.yang@surrey.ac.uk> <jihoon.yang@gmail.com> on 20/04/2017.
 *
 * This example demonstrates how to use the Bluetooth LE Generic Attribute Profile (GATT)
 * to transmit sensor data between ICS IoT Egg and Android.
 *
 * Temperature and humidity values are receiving from IoTEgg Device via BLE.
 * And it can change the RGB LED color by transmitting LED value to IoTEgg Device via BLE.
 *
 * ICS IoT Egg BLE Breakout Board has been designed by William Headley <w.headley@surrey.ac.uk>
 * - BLE RST pin: P0_5
 * - BLE TX pin: P0_0
 * - BLE RX pin: P0_1
 *
 * Copyright (c) 2017 by Institute for Communication Systems (ICS), University of Surrey
 * Klaus Moessner <k.moessner@surrey.ac.uk>
 * William Headley <w.headley@surrey.ac.uk>
 *
 */

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

    private TextView mConnectionState;
    private TextView sensor_temperature_value, sensor_humidity_value;
    private SeekBar redBar, greenBar, blueBar;
    private int seekR, seekG, seekB;
    private ImageView rgbcolor;
    private Button btnColorUpdate;

    static final byte START_TEMPHUM = 12;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle(mDeviceName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mConnectionState = (TextView)findViewById(R.id.iotegg_connection_state);
        sensor_temperature_value = (TextView) findViewById(R.id.sensor_temperature_value);
        sensor_humidity_value = (TextView) findViewById(R.id.sensor_humidity_value);

        redBar = (SeekBar) findViewById(R.id.seekBarR);
        greenBar = (SeekBar) findViewById(R.id.seekBarG);
        blueBar = (SeekBar) findViewById(R.id.seekBarB);
        redBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        greenBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        blueBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        rgbcolor = (ImageView) findViewById(R.id.rgbcolor);

        redBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        greenBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
        blueBar.getProgressDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);

        btnColorUpdate = (Button)findViewById(R.id.btnColorUpdate);
        btnColorUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commandRGBLEDUpdate();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);

        if(mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
            mBluetoothLeService = null;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        if(mBluetoothLeService != null)
            mBluetoothLeService = null;

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                onBackPressed();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected_state);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected_state);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                parseIoTEggData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null) return;

        BluetoothGattCharacteristic characteristic = gattService
                .getCharacteristic(BluetoothLeService.UUID_IoTEgg_BLE_TX);
        map.put(characteristic.getUuid(), characteristic);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(BluetoothLeService.UUID_IoTEgg_BLE_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            switch (seekBar.getId()) {
                case R.id.seekBarR:
                    seekR = progress;
                    break;
                case R.id.seekBarG:
                    seekG = progress;
                    break;
                case R.id.seekBarB:
                    seekB = progress;
                    break;
            }
            rgbcolor.setBackgroundColor(Color.rgb(seekR, seekG, seekB));
        }
    };

    private void commandRGBLEDUpdate() {
        if(mConnected == true && mBluetoothLeService != null ) {
            BluetoothGattCharacteristic characteristic = map.get(BluetoothLeService.UUID_IoTEgg_BLE_TX);
            byte[] buf= new byte[7];
            buf[0]='@';
            buf[1]='L'; // LED
            buf[2]=3; // data length
            buf[3]=(byte)seekR;
            buf[4]=(byte)seekG;
            buf[5]=(byte)seekB;
            buf[6]='$';

            characteristic.setValue(buf);
            mBluetoothLeService.writeCharacteristic(characteristic);
        }else{
            Toast.makeText(this, R.string.error_ble_service, Toast.LENGTH_SHORT).show();
        }
    }

    private void parseIoTEggData(byte[] byteArray) {

        if(byteArray != null && byteArray[0]=='@' && byteArray[byteArray.length-1]=='$'){
            if(byteArray[1]=='T') { // Temperature & Humidity
                float temp = Float.intBitsToFloat(((int) byteArray[3] & 0xFF) ^ ((int) byteArray[4] & 0xFF) << 8 ^ ((int) byteArray[5] & 0xFF) << 16 ^ ((int) byteArray[6] & 0xFF) << 24);
                float humd = Float.intBitsToFloat(((int) byteArray[7] & 0xFF) ^ ((int) byteArray[8] & 0xFF) << 8 ^ ((int) byteArray[9] & 0xFF) << 16 ^ ((int) byteArray[10] & 0xFF) << 24);
                updateSensorTemphumValues(String.format("%.2f", (float) temp), String.format("%.2f", (float) humd));
            }

        }else{

        }
    }

    private void updateSensorTemphumValues(String temperature, String humidity){
        sensor_temperature_value.setText(temperature + (char) 0x00B0 + 'C');
        sensor_humidity_value.setText(humidity + '%');
    }
}
