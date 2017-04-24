package app.iot.com.ioteggble;

import java.util.HashMap;

/**
 * Created by Jihoon Yang <j.yang@surrey.ac.uk> <jihoon.yang@gmail.com> on 20/04/2017.
 *
 * This example demonstrates how to use the Bluetooth LE Generic Attribute Profile (GATT)
 * to transmit sensor data between ICS IoT Egg and Android.
 *
 * Bluetooth LE Generic Attribute Profile (GATT) in this code is for IoTEgg BLE module.
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

public class IoTEggGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String IoTEgg_BLE_RX = "ca9dfee8-f18e-47d0-b1b1-b4f8a0465945";
    public static String IoTEgg_BLE_TX = "2507c4a3-bd07-4df6-bd62-63234461e396";
    public static String IoTEgg_BLE_SERVICE = "6be3da26-1cf5-476e-8ef6-1ee12dfc3e8a";

    static {
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put("6be3da26-1cf5-476e-8ef6-1ee12dfc3e8a", "IoTEgg BLE Service");
        attributes.put(IoTEgg_BLE_TX, "IoTEgg BLE TX");
        attributes.put(IoTEgg_BLE_RX, "IoTEgg BLE RX");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
