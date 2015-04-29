/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.nrftoolbox.bpm;

import java.util.Calendar;

import no.nordicsemi.android.nrftoolbox.AppHelpFragment;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;
import no.nordicsemi.android.nrftoolbox.utility.Logger;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BPMActivity extends Activity implements BPMManagerCallbacks, ScannerFragment.OnDeviceSelectedListener {
	private static final String TAG = "BPMActivity";

	private static final String CONNECTION_STATUS = "connection_status";
	private static final int REQUEST_ENABLE_BT = 2;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothManager mBluetoothManager;
	private BPMManager mBPMManager;

	private TextView mDeviceNameView;
	private TextView mBatteryLevelView;
	private TextView mSystolicView;
	private TextView mSystolicUnitView;
	private TextView mDiastolicView;
	private TextView mDiastolicUnitView;
	private TextView mMeanAPView;
	private TextView mMeanAPUnitView;
	private TextView mPulseView;
	private TextView mTimestampView;
	private Button mConnectButton;

	private boolean isDeviceConnected = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feature_bpm);

		setBluetoothAdapter();
		isBLESupported();
		if (!isBLEEnabled()) {
			showBLEDialog();
		}
		initializeBPMManager();
		setGUI();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(CONNECTION_STATUS, isDeviceConnected);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		isDeviceConnected = savedInstanceState.getBoolean(CONNECTION_STATUS);

		if (isDeviceConnected) {
			mConnectButton.setText(R.string.action_disconnect);
		} else {
			mConnectButton.setText(R.string.action_connect);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.help, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;
		case R.id.action_about:
			final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.bpm_about_text);
			fragment.show(getFragmentManager(), "help_fragment");
			break;
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		mBPMManager.disconnect();
	}

	private void initializeBPMManager() {
		mBPMManager = BPMManager.getBPMManager();
		mBPMManager.setGattCallbacks(this);
	}

	private void setGUI() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
		mConnectButton = (Button) findViewById(R.id.action_connect);
		mDeviceNameView = (TextView) findViewById(R.id.text_bpm_device_name);
		mBatteryLevelView = (TextView) findViewById(R.id.text_bpm_battery);
		mSystolicView = (TextView) findViewById(R.id.systolic);
		mSystolicUnitView = (TextView) findViewById(R.id.systolic_unit);
		mDiastolicView = (TextView) findViewById(R.id.diastolic);
		mDiastolicUnitView = (TextView) findViewById(R.id.diastolic_unit);
		mMeanAPView = (TextView) findViewById(R.id.mean_ap);
		mMeanAPUnitView = (TextView) findViewById(R.id.mean_ap_unit);
		mPulseView = (TextView) findViewById(R.id.pulse);
		mTimestampView = (TextView) findViewById(R.id.timestamp);
	}

	private void isBLESupported() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private void showToast(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(BPMActivity.this, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	private boolean isBLEEnabled() {
		return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
	}

	private void showBLEDialog() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	}

	private void setBluetoothAdapter() {
		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
	}

	private void showDeviceScanningDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final ScannerFragment dialog = ScannerFragment.getInstance(getApplicationContext(), mBluetoothAdapter, BPMManager.BP_SERVICE_UUID, false);
				dialog.show(getFragmentManager(), "scan_fragment");
			}
		});
	}

	private void setDefaultUI() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mSystolicView.setText(R.string.not_available_value);
				mSystolicUnitView.setText(null);
				mDiastolicView.setText(R.string.not_available_value);
				mDiastolicUnitView.setText(null);
				mMeanAPView.setText(R.string.not_available_value);
				mMeanAPUnitView.setText(null);
				mPulseView.setText(R.string.not_available_value);
				mTimestampView.setText(R.string.not_available);
			}
		});
	}

	/**
	 * Callback of CONNECT/DISCONNECT button on BPMActivity
	 */
	public void onConnectClicked(final View view) {
		setDefaultUI();
		if (isBLEEnabled()) {
			if (!isDeviceConnected) {
				showDeviceScanningDialog();
			} else {
				mBPMManager.disconnect();
			}
		} else {
			showBLEDialog();
		}
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device) {
		mDeviceNameView.setText(device.getName());
		mBPMManager.connect(getApplicationContext(), device);
	}

	@Override
	public void onDeviceConnected() {
		isDeviceConnected = true;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectButton.setText(R.string.action_disconnect);
			}
		});
	}

	@Override
	public void onDeviceDisconnected() {
		isDeviceConnected = false;
		mBPMManager.closeBluetoothGatt();
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectButton.setText(R.string.action_connect);
				mDeviceNameView.setText(R.string.bpm_default_name);
				mBatteryLevelView.setText(R.string.not_available);
			}
		});
	}

	@Override
	public void onServicesDiscovered(final boolean bloodPressure, final boolean cuffPressure, final boolean batteryService) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onBloodPressureMeasurementIndicationsEnabled() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onIntermediateCuffPressureNotificationEnabled() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onBloodPressureMeasurmentRead(final float systolic, final float diastolic, final float meanArterialPressure, final int unit) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mSystolicView.setText(Float.toString(systolic));
				mDiastolicView.setText(Float.toString(diastolic));
				mMeanAPView.setText(Float.toString(meanArterialPressure));

				mSystolicUnitView.setText(unit == UNIT_mmHG ? R.string.bpm_unit_mmhg : R.string.bpm_unit_kpa);
				mDiastolicUnitView.setText(unit == UNIT_mmHG ? R.string.bpm_unit_mmhg : R.string.bpm_unit_kpa);
				mMeanAPUnitView.setText(unit == UNIT_mmHG ? R.string.bpm_unit_mmhg : R.string.bpm_unit_kpa);
			}
		});
	}

	@Override
	public void onIntermediateCuffPressureRead(final float cuffPressure, final int unit) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mSystolicView.setText(Float.toString(cuffPressure));
				mDiastolicView.setText(R.string.not_available_value);
				mMeanAPView.setText(R.string.not_available_value);

				mSystolicUnitView.setText(unit == UNIT_mmHG ? R.string.bpm_unit_mmhg : R.string.bpm_unit_kpa);
				mDiastolicUnitView.setText(null);
				mMeanAPUnitView.setText(null);
			}
		});
	}

	@Override
	public void onPulseRateRead(final float pulseRate) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mPulseView.setText(Float.toString(pulseRate));
			}
		});
	}

	@Override
	public void onTimestampRead(final Calendar calendar) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mTimestampView.setText(getString(R.string.bpm_timestamp, calendar));
			}
		});
	}

	@Override
	public void onBatteryValueReceived(final int value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mBatteryLevelView.setText(getString(R.string.battery, value));
			}
		});
	}

	@Override
	public void onBondingRequired() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(BPMActivity.this, R.string.bonding, Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	public void onBonded() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(BPMActivity.this, R.string.bonded, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void onError(final String message, final int errorCode) {
		Logger.e(TAG, "onError " + message + " errodCode: " + errorCode);
		showToast(message + " (" + errorCode + ")");

		// refresh UI when connection failed
		onDeviceDisconnected();
	}
}
