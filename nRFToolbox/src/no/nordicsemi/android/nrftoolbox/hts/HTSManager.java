/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.nrftoolbox.hts;

import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.nrftoolbox.utility.Logger;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * HTSManager class performs BluetoothGatt operations for connection, service discovery, enabling indication and reading characteristics. All operations required to connect to device with BLE HT
 * Service and reading health thermometer values are performed here. HTSActivity implements HTSManagerCallbacks in order to receive callbacks of BluetoothGatt operations
 */
public class HTSManager {
	private final String TAG = "HTSManager";
	private HTSManagerCallbacks mCallbacks;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothDevice mDevice;
	private Context mContext;

	public final static UUID HT_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");

	private static final UUID HT_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");
	private static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	private final static UUID BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
	private final static UUID BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

	private final static String ERROR_CONNECTION_STATE_CHANGE = "Error on connection state change";
	private final static String ERROR_DISCOVERY_SERVICE = "Error on discovering services";
	private final static String ERROR_WRITE_CHARACTERISTIC = "Error on writing characteristic";
	private final static String ERROR_WRITE_DESCRIPTOR = "Error on writing descriptor";

	private BluetoothGattCharacteristic mHTCharacteristic, mBatteryCharacteritsic;
	private BluetoothGattService mHTService, mBatteryService;

	private boolean isHTServiceFound = false;
	private boolean isBatteryServiceFound = false;

	private final int HIDE_MSB_8BITS_OUT_OF_32BITS = 0x00FFFFFF;
	private final int HIDE_MSB_8BITS_OUT_OF_16BITS = 0x00FF;
	private final int SHIFT_LEFT_8BITS = 8;
	private final int SHIFT_LEFT_16BITS = 16;
	private final int GET_BIT24 = 0x00400000;
	private static final int FIRST_BIT_MASK = 0x01;

	private static HTSManager managerInstance = null;

	/**
	 * singleton implementation of HTSManager class
	 */
	public static synchronized HTSManager getHTSManager() {
		if (managerInstance == null) {
			managerInstance = new HTSManager();
		}
		return managerInstance;
	}

	/**
	 * callbacks for activity {HTSActivity} that implements HTSManagerCallbacks interface activity use this method to register itself for receiving callbacks
	 */
	public void setGattCallbacks(HTSManagerCallbacks callbacks) {
		mCallbacks = callbacks;
	}

	public BluetoothDevice getDevice() {
		return mDevice;
	}

	public void connect(Context context, BluetoothDevice device) {
		mDevice = device;
		mBluetoothGatt = mDevice.connectGatt(context, false, mGattCallback);
		mContext = context;
	}

	public void disconnect() {
		Logger.d(TAG, "Disconnecting device");
		if (mBluetoothGatt != null) {
			mBluetoothGatt.disconnect();
		}
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving indication, etc
	 */
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					Logger.d(TAG, "Device connected");
					mBluetoothGatt.discoverServices();
					//This will send callback to HTSActivity when device get connected
					mCallbacks.onDeviceConnected();
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					Logger.d(TAG, "Device disconnected");
					//This will send callback to HTSActivity when device get disconnected
					mCallbacks.onDeviceDisconnected();
				}
			} else {
				mCallbacks.onError(ERROR_CONNECTION_STATE_CHANGE, status);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			isHTServiceFound = false;
			if (status == BluetoothGatt.GATT_SUCCESS) {
				List<BluetoothGattService> services = gatt.getServices();
				for (BluetoothGattService service : services) {
					if (service.getUuid().equals(HT_SERVICE_UUID)) {
						Logger.e(TAG, "BP service is found");
						isHTServiceFound = true;
						mHTService = service;
						mCallbacks.onHTServiceFound();
					}
					if (service.getUuid().equals(BATTERY_SERVICE)) {
						Logger.e(TAG, "Battery service is found");
						mBatteryService = service;
						isBatteryServiceFound = true;
						mCallbacks.onBatteryServiceFound();
					}
				}
				if (isBatteryServiceFound) {
					readBatteryLevel();
				} else if (isHTServiceFound) {
					enableHTIndication();
				}
			} else {
				mCallbacks.onError(ERROR_DISCOVERY_SERVICE, status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Logger.e(TAG, "successfully read characteristic " + characteristic.getUuid());
				if (characteristic.getUuid().equals(BATTERY_LEVEL_CHARACTERISTIC)) {
					int batteryValue = characteristic.getValue()[0];
					Logger.d(TAG, "Battery value: " + batteryValue);
					mCallbacks.onBatteryValueReceived(batteryValue);
					if (isHTServiceFound) {
						enableHTIndication();
					}
				}

			} else {
				Logger.e(TAG, "Error on read characteristic " + " Error code: " + status);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			double tempValue = 0.0;
			Logger.e(TAG, "onCharacteristicChanged() " + characteristic.getUuid());
			Logger.e(TAG, "value: " + characteristic.getValue());
			if (characteristic.getUuid().equals(HT_MEASUREMENT_CHARACTERISTIC_UUID)) {
				try {
					tempValue = decodeTemperature(characteristic.getValue());
					Logger.e(TAG, "Temp value: " + tempValue);
					mCallbacks.onHTValueReceived(tempValue);
				} catch (Exception e) {
					Logger.e(TAG, "invalid temperature value");
				}
			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Logger.e(TAG, "onDescriptorWrite()");

			} else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
				mCallbacks.onBondingRequired();

				final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
				mContext.registerReceiver(mBondingBroadcastReceiver, filter);
			} else {
				Logger.e(TAG, ERROR_WRITE_DESCRIPTOR + " (" + status + ")");
				mCallbacks.onError(ERROR_WRITE_DESCRIPTOR, status);
			}
		}
	};

	private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
			final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

			Logger.d(TAG, "Bond state changed for: " + device.getAddress() + " new state: " + bondState + " previous: " + previousBondState);

			// skip other devices
			if (!device.getAddress().equals(mBluetoothGatt.getDevice().getAddress()))
				return;

			if (bondState == BluetoothDevice.BOND_BONDED) {
				// We've read Battery Level, now enabling HT indications 
				if (mHTCharacteristic != null) {
					enableHTIndication();
				}
				mContext.unregisterReceiver(this);
				mCallbacks.onBonded();
			}
		}
	};

	private void readBatteryLevel() {
		mBatteryCharacteritsic = mBatteryService.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC);
		if (mBatteryCharacteritsic != null) {
			Logger.e(TAG, "reading battery characteristic");
			mBluetoothGatt.readCharacteristic(mBatteryCharacteritsic);
		} else {
			Logger.e(TAG, "Battery Level Characteristic is null");
		}
	}

	/**
	 * enable Health Thermometer indication on Health Thermometer Measurement characteristic
	 */
	private void enableHTIndication() {
		Logger.e(TAG, "enableHTIndication()");
		mHTCharacteristic = mHTService.getCharacteristic(HT_MEASUREMENT_CHARACTERISTIC_UUID);
		mBluetoothGatt.setCharacteristicNotification(mHTCharacteristic, true);
		BluetoothGattDescriptor descriptor = mHTCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
		mBluetoothGatt.writeDescriptor(descriptor);
	}

	/**
	 * This method decode temperature value received from Health Thermometer device First byte {0} of data is flag and first bit of flag shows unit information of temperature. if bit 0 has value 1
	 * then unit is Fahrenheit and Celsius otherwise Four bytes {1 to 4} after Flag bytes represent the temperature value in IEEE-11073 32-bit Float format
	 */
	private double decodeTemperature(byte[] data) throws Exception {
		double temperatureValue = 0.0;
		byte flag = data[0];
		byte exponential = data[4];
		short firstOctet = convertNegativeByteToPositiveShort(data[1]);
		short secondOctet = convertNegativeByteToPositiveShort(data[2]);
		short thirdOctet = convertNegativeByteToPositiveShort(data[3]);
		int mantissa = ((thirdOctet << SHIFT_LEFT_16BITS) | (secondOctet << SHIFT_LEFT_8BITS) | (firstOctet)) & HIDE_MSB_8BITS_OUT_OF_32BITS;
		mantissa = getTwosComplimentOfNegativeMantissa(mantissa);
		temperatureValue = (mantissa * Math.pow(10, exponential));
		/*
		 * Conversion of temperature unit from Fahrenheit to Celsius if unit is in Fahrenheit
		 * Celsius = (98.6*Fahrenheit -32) 5/9
		 */
		if ((flag & FIRST_BIT_MASK) != 0) {
			temperatureValue = (float) ((98.6 * temperatureValue - 32) * (5 / 9.0));
		}
		return temperatureValue;

	}

	private short convertNegativeByteToPositiveShort(byte octet) {
		if (octet < 0) {
			return (short) (octet & HIDE_MSB_8BITS_OUT_OF_16BITS);
		} else {
			return octet;
		}
	}

	private int getTwosComplimentOfNegativeMantissa(int mantissa) {
		if ((mantissa & GET_BIT24) != 0) {
			return ((((~mantissa) & HIDE_MSB_8BITS_OUT_OF_32BITS) + 1) * (-1));
		} else {
			return mantissa;
		}
	}

	public void close() {
		closeBluetoothGatt();
		managerInstance = null;
	}

	public void closeBluetoothGatt() {
		try {
			mContext.unregisterReceiver(mBondingBroadcastReceiver);
		} catch (Exception e) {
			// the receiver must have been not registered or unregistered before
		}
		if (mBluetoothGatt != null) {
			mBluetoothGatt.close();
			mBluetoothGatt = null;
		}
	}

	public void resetStatus() {
		isHTServiceFound = false;
		isBatteryServiceFound = false;
	}

}
