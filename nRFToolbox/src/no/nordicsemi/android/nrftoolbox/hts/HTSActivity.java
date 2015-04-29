/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.nrftoolbox.hts;

import java.text.DecimalFormat;

import no.nordicsemi.android.nrftoolbox.AppHelpFragment;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;
import no.nordicsemi.android.nrftoolbox.utility.Logger;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
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

/**
 * HTSActivity is the main Health Thermometer activity. It implements HTSManagerCallbacks to receive callbacks from HTSManager class. It implements {@link ScannerFragment.OnDeviceSelectedListener}
 * callback to receive callback when device is selected from scanning dialog. The activity supports portrait and landscape orientations.
 */
public class HTSActivity extends Activity implements HTSManagerCallbacks, ScannerFragment.OnDeviceSelectedListener {
	private final String TAG = "HTSActivity";

	private static final String CONNECTION_STATUS = "connection_status";
	private static final int REQUEST_ENABLE_BT = 2;

	private HTSManager mHTSManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothManager mBluetoothManager;
	private BluetoothDevice mDevice;
	private Context mContext;

	private boolean isDeviceConnected = false;
	private boolean isBackKeyPressed = false;

	private Button mConnectButton;
	private TextView mHTSName, mHTSValue, mHTSBatteryValue;
	private final int MAX_BATTERY_VALUE = 100;
	private final int MIN_POSITIVE_VALUE = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feature_hts);
		mContext = getApplicationContext();
		setBluetoothAdapter();
		isBLESupported();
		if (!isBLEEnabled()) {
			showBLEDialog();
		}
		initializeHTSManager();
		setGUI();
		restoreSavedState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(CONNECTION_STATUS, isDeviceConnected);

	}

	private void restoreSavedState(Bundle inState) {
		if (inState != null) {
			isDeviceConnected = inState.getBoolean(CONNECTION_STATUS);
			if (mHTSManager != null) {
				mDevice = mHTSManager.getDevice();
			}
			if (isDeviceConnected) {
				mConnectButton.setText(R.string.action_disconnect);
			} else {
				mConnectButton.setText(R.string.action_connect);
			}

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.help, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (isDeviceConnected) {
				isBackKeyPressed = true;
				mHTSManager.disconnect();

			} else {
				mHTSManager.close();
				onBackPressed();
			}
			break;
		case R.id.action_about:
			final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.hts_about_text);
			fragment.show(getFragmentManager(), "help_fragment");
			break;
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		if (isDeviceConnected) {
			isBackKeyPressed = true;
			mHTSManager.disconnect();
		} else {
			mHTSManager.close();
			super.onBackPressed();
		}
	}

	private void setGUI() {
		setupActionBar();
		mConnectButton = (Button) findViewById(R.id.action_connect);
		mHTSName = (TextView) findViewById(R.id.text_hts_device_name);
		mHTSValue = (TextView) findViewById(R.id.text_hts_value);
		mHTSBatteryValue = (TextView) findViewById(R.id.text_hts_battery);
	}

	private void setupActionBar() {
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	private void initializeHTSManager() {
		mHTSManager = HTSManager.getHTSManager();
		mHTSManager.setGattCallbacks(this);
	}

	private void isBLESupported() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			showToast("Device don't have BLE support");
			finish();
		}
	}

	private void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	private boolean isBLEEnabled() {
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			return false;
		} else {
			return true;
		}
	}

	private void showBLEDialog() {
		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
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
				FragmentManager fm = getFragmentManager();
				ScannerFragment dialog = ScannerFragment.getInstance(mContext, mBluetoothAdapter, HTSManager.HT_SERVICE_UUID, false);
				dialog.show(fm, "scan_fragment");
			}
		});
	}

	private void showConnectedButton() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectButton.setText(R.string.action_disconnect);
			}
		});
	}

	/**
	 * Callback of CONNECT/DISCONNECT button on HTSActivity
	 */
	public void onConnectClicked(final View view) {
		if (isBLEEnabled()) {
			if (mDevice == null) {
				showDeviceScanningDialog();
			} else if (!isDeviceConnected) {
				mHTSManager.connect(mContext, mDevice);
			} else {
				mHTSManager.disconnect();
			}
		} else {
			showBLEDialog();
		}
	}

	@Override
	public void onDeviceSelected(BluetoothDevice device) {
		mDevice = device;
		mHTSManager.connect(mContext, mDevice);
	}

	@Override
	public void onDeviceConnected() {
		Logger.d(TAG, "Device connected");
		isDeviceConnected = true;
		showConnectedButton();

	}

	@Override
	public void onDeviceDisconnected() {
		Logger.d(TAG, "Device disconnected");
		reset();
		if (isBackKeyPressed) {
			Logger.e(TAG, "Back key pressed so close activity");
			mHTSManager.close();
			finish();
		}
	}

	@Override
	public void onHTServiceFound() {
		setHTSNameOnView(mDevice.getName());
	}

	@Override
	public void onBatteryServiceFound() {
		// do nothing here
	}

	@Override
	public void onBatteryValueReceived(int value) {
		setHTSBatteryValueOnView(value);
	}

	@Override
	public void onHTValueReceived(double value) {
		setHTSValueOnView(value);
	}

	@Override
	public void onError(String message, int errorCode) {
		Logger.e(TAG, message + " Error Code: " + errorCode);
		showErrorMessage(message, errorCode);
	}

	@Override
	public void onHTIndicationEnabled() {
		// nothing to do here
	}

	private void showErrorMessage(final String message, final int code) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showToast(message + " Error code: " + Integer.toString(code));
			}
		});
	}

	private void setHTSNameOnView(final String name) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (name != null) {
					mHTSName.setText(name);
				} else {
					mHTSName.setText(R.string.hts_default_name);
				}
			}
		});
	}

	private void setHTSValueOnView(final double value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				DecimalFormat formatedTemp = new DecimalFormat("#0.00");
				mHTSValue.setText(formatedTemp.format(value));

			}
		});
	}

	private void setHTSBatteryValueOnView(final int value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (value >= MIN_POSITIVE_VALUE && value <= MAX_BATTERY_VALUE) {
					mHTSBatteryValue.setText(getString(R.string.battery, value));
				} else {
					mHTSBatteryValue.setText(R.string.not_available);
				}
			}
		});
	}

	private void reset() {
		mDevice = null;
		isDeviceConnected = false;
		setDefaultUI();
		mHTSManager.closeBluetoothGatt();
		mHTSManager.resetStatus();
	}

	private void setDefaultUI() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectButton.setText(R.string.action_connect);
				mHTSName.setText(R.string.hts_default_name);
				mHTSValue.setText(R.string.not_available_value);
				mHTSBatteryValue.setText(R.string.not_available);
			}
		});
	}

	@Override
	public void onBondingRequired() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(HTSActivity.this, R.string.bonding, Toast.LENGTH_LONG).show();
			}
		});

	}

	@Override
	public void onBonded() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(HTSActivity.this, R.string.bonded, Toast.LENGTH_SHORT).show();
			}
		});

	}

}
