/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.nrftoolbox.scanner;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.UUID;

import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.utility.Logger;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

/**
 * ScannerFragment class scan required BLE devices and shows them in a list. This class scans and filter devices with standard BLE Service UUID and devices with custom BLE Service UUID It contains a
 * list and a button to scan/cancel. There is a interface {@link OnDeviceSelectedListener} which is implemented by activity in order to receive selected device. The scanning will continue for 5
 * seconds and then stop
 */
public class ScannerFragment extends DialogFragment {
	private final String TAG = "ScannerFragment";

	private final long SCAN_DURATION = 5000;

	private static BluetoothAdapter mBluetoothAdapter;
	private static Context mContext;
	private static UUID mUuid;
	private static boolean mIsCustomUUID;

	private ArrayList<ExtendedBluetoothDevice> mBluetoothDevices = new ArrayList<ExtendedBluetoothDevice>();
	private ArrayAdapter<ExtendedBluetoothDevice> mListAdapter;
	private ListIterator<ExtendedBluetoothDevice> mIterator;
	private OnDeviceSelectedListener mListener;
	private Handler mHandler = new Handler();
	private Button mScanButton;
	private boolean mIsScanning = false;

	/**
	 * Static implementation of fragment so that it keeps data when phone orientation is changed For standard BLE Service UUID, we can filter devices using normal android provided command
	 * startScanLe() with required BLE Service UUID For custom BLE Service UUID, we will use class ScannerServiceParser to filter out required device
	 */
	public static ScannerFragment getInstance(Context context, BluetoothAdapter adapter, UUID uuid, boolean isCustomUUID) {
		ScannerFragment fragment = new ScannerFragment();
		mContext = context;
		mBluetoothAdapter = adapter;
		mUuid = uuid;
		mIsCustomUUID = isCustomUUID;
		return fragment;
	}

	/**
	 * Interface required to be implemented by activity
	 */
	public static interface OnDeviceSelectedListener {
		/**
		 * Fired when user selected the device
		 * 
		 * @param device
		 *            the device to connect to
		 */
		public void onDeviceSelected(BluetoothDevice device);
	}

	/**
	 * This will make sure that {@link OnDeviceSelectedListener} interface is implemented by activity
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			this.mListener = (OnDeviceSelectedListener) activity;
		} catch (final ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnDeviceSelectedListener");
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		stopScan();
	}

	/**
	 * When dialog is created then set AlertDialog with list and button views
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_device_selection, null);
		final ListView listview = (ListView) dialogView.findViewById(R.id.listViewScanner);
		listview.setEmptyView(dialogView.findViewById(R.id.empty));
		listview.setAdapter(mListAdapter = new DeviceListAdapter(mContext, mBluetoothDevices));

		final AlertDialog dialog = builder.setView(dialogView).create();
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				stopScan();
				dialog.cancel();
				mListener.onDeviceSelected(mBluetoothDevices.get((int) id).device);
			}
		});

		mScanButton = (Button) dialogView.findViewById(R.id.buttonCancel);
		mScanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.buttonCancel) {
					if (mIsScanning) {
						dialog.cancel();
					} else {
						startScan();
					}
				}
			}
		});
		startScan();
		return dialog;
	}

	/**
	 * Scan for 5 seconds and then stop scanning when a BluetoothLE device is found then mLEScanCallback is activated This will perform regular scan for custom BLE Service UUID and then filter out
	 * using class ScannerServiceParser
	 */
	private void startScan() {
		mBluetoothDevices.clear();
		mScanButton.setText(R.string.dfu_button_label_cancel);
		updateSensorsListView();
		final UUID[] uuids = new UUID[1];
		uuids[0] = mUuid;
		if (mIsCustomUUID) {
			mBluetoothAdapter.startLeScan(mLEScanCallback);
		} else {
			mBluetoothAdapter.startLeScan(uuids, mLEScanCallback);
		}

		mIsScanning = true;
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mIsScanning) {
					stopScan();
				}
			}
		}, SCAN_DURATION);
	}

	/**
	 * Stop scan if user tap Cancel button
	 */
	private void stopScan() {
		if (mIsScanning) {
			mScanButton.setText(R.string.dfu_button_label_scan);
			mBluetoothAdapter.stopLeScan(mLEScanCallback);
			mIsScanning = false;
		}
	}

	/**
	 * if scanned device already in the list then update it otherwise add as a new device
	 */
	private void addScannedDevice(final BluetoothDevice device, final int rssi) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!deviceAlreadyExist(device)) {
					addDevice(device, rssi);
				} else {
					updateDevice(device, rssi);
				}
				updateSensorsListView();
			}
		});
	}

	/**
	 * add device in the list of type ExtendedBluetoothDevice ExtendedBluetoothDevice contains both Bluetooth device and rssi
	 */
	private void addDevice(BluetoothDevice device, int rssi) {
		mBluetoothDevices.add(convertDevice(device, rssi));
	}

	/**
	 * if scanned device is already present then update its RSSI value
	 */
	private void updateDevice(BluetoothDevice device, int rssi) {
		mIterator = mBluetoothDevices.listIterator();
		ExtendedBluetoothDevice eDevice;
		while (mIterator.hasNext()) {
			eDevice = mIterator.next();
			if (eDevice.device.getAddress().equals(device.getAddress())) {
				eDevice.rssi = rssi;
				mIterator.set(eDevice);
				return;
			}
		}
	}

	/**
	 * convert BluetoothDevice and RSSI to single type ExtendedBluetoothDevice
	 */
	private ExtendedBluetoothDevice convertDevice(BluetoothDevice device, int rssi) {
		return new ExtendedBluetoothDevice(device, rssi);
	}

	private boolean deviceAlreadyExist(BluetoothDevice device) {
		if (!mBluetoothDevices.isEmpty()) {
			for (ExtendedBluetoothDevice bDevice : mBluetoothDevices) {
				if (bDevice.device.getAddress().equals(device.getAddress())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * show scanned devices list on screen after change
	 */
	private void updateSensorsListView() {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mListAdapter.notifyDataSetChanged();
			}
		});
	}

	/**
	 * Callback for scanned devices class {@link ScannerServiceParser} will be used to filter devices with custom BLE service UUID then the device will be added in a list
	 */
	private BluetoothAdapter.LeScanCallback mLEScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			if (device != null) {
				if (mIsCustomUUID) {
					ScannerServiceParser parser = ScannerServiceParser.getParser();
					try {
						parser.decodeDeviceAdvData(scanRecord, mUuid);

						if (parser.isValidSensor()) {
							addScannedDevice(device, rssi);
						}
					} catch (Exception e) {
						Logger.e(TAG, "Invalid data in Advertisement packet " + e.toString());
					}
				} else {
					addScannedDevice(device, rssi);
				}
			}
		}
	};
}
