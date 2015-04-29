/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.nrftoolbox.scanner;

import android.bluetooth.BluetoothDevice;

public class ExtendedBluetoothDevice {
	public BluetoothDevice device;
	public int rssi;

	public ExtendedBluetoothDevice(BluetoothDevice device, int rssi) {
		this.device = device;
		this.rssi = rssi;
	}
}
