/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.nrftoolbox.hrs;

import no.nordicsemi.android.nrftoolbox.AppHelpFragment;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;
import no.nordicsemi.android.nrftoolbox.utility.Logger;

import org.achartengine.GraphicalView;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * HRSActivity is the main Heart rate activity. It implements HRSManagerCallbacks to receive callbacks from HRSManager class. It implements {@link ScannerFragment.OnDeviceSelectedListener} callback to
 * receive callback when device is selected from scanning dialog. The activity supports portrait and landscape orientations The activity also uses external library AChartEngine to show real time graph
 * of HR values
 */
public class HRSActivity extends Activity implements HRSManagerCallbacks, ScannerFragment.OnDeviceSelectedListener {
	private final String TAG = "HRSActivity";

	private static final String CONNECTION_STATUS = "connection_status";
	private static final String GRAPH_STATUS = "graph_status";
	private static final String GRAPH_COUNTER = "graph_counter";
	private static final String HR_VALUE = "hr_value";

	private static final int REQUEST_ENABLE_BT = 2;

	private HRSManager mHRSManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothManager mBluetoothManager;
	private BluetoothDevice mDevice;
	private Context mContext;
	private Handler mHandler = new Handler();

	private boolean isDeviceConnected = false;
	private boolean isGraphInProgress = false;
	private boolean isBackKeyPressed = false;

	private static GraphicalView mGraphView;
	private LineGraphView mLineGraph;
	private Button mConnectButton;
	private TextView mHRSName, mHRSValue, mHRSPosition, mHRSBatteryValue;
	private int mInterval = 1000; // 1 second interval
	private int mHrmValue = 0;
	private int mCounter = 0;
	private final int MAX_BATTERY_VALUE = 100;
	private final int MAX_HR_VALUE = 65535;
	private final int MIN_POSITIVE_VALUE = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feature_hrs);

		mContext = getApplicationContext();
		setBluetoothAdapter();
		isBLESupported();
		if (!isBLEEnabled()) {
			showBLEDialog();
		}
		initializeHRSManager();
		setGUI();
		restoreSavedState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(CONNECTION_STATUS, isDeviceConnected);
		outState.putBoolean(GRAPH_STATUS, isGraphInProgress);
		outState.putInt(GRAPH_COUNTER, mCounter);
		outState.putInt(HR_VALUE, mHrmValue);
		stopShowGraph();
	}

	private void restoreSavedState(Bundle inState) {
		if (inState != null) {
			isDeviceConnected = inState.getBoolean(CONNECTION_STATUS);
			isGraphInProgress = inState.getBoolean(GRAPH_STATUS);
			mCounter = inState.getInt(GRAPH_COUNTER);
			mHrmValue = inState.getInt(HR_VALUE);
			if (mHRSManager != null) {
				mDevice = mHRSManager.getDevice();
			}
			if (isDeviceConnected) {
				mConnectButton.setText(R.string.action_disconnect);
			} else {
				mConnectButton.setText(R.string.action_connect);
			}
			if (isGraphInProgress) {
				startShowGraph();
			}
		}
	}

	private void showGraph() {
		mGraphView = mLineGraph.getView(this);
		ViewGroup layout = (ViewGroup) findViewById(R.id.graph_hrs);
		layout.addView(mGraphView);
	}

	private void updateGraph(final int hrmValue) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mCounter++;
				Point point = new Point(mCounter, hrmValue);
				mLineGraph.addValue(point);
				mGraphView.repaint();
			}
		});
	}

	private Runnable mRepeatTask = new Runnable() {
		@Override
		public void run() {
			updateGraph(mHrmValue);
			mHandler.postDelayed(mRepeatTask, mInterval);
		}
	};

	void startShowGraph() {
		mRepeatTask.run();
	}

	void stopShowGraph() {
		mHandler.removeCallbacks(mRepeatTask);
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
			if (isDeviceConnected) {
				mHRSManager.disconnect();
				isBackKeyPressed = true;
			} else {
				clearGraph();
				mHRSManager.close();
				onBackPressed();
			}
			break;
		case R.id.action_about:
			final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.hrs_about_text);
			fragment.show(getFragmentManager(), "help_fragment");
			break;
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		if (isDeviceConnected) {
			mHRSManager.disconnect();
			isBackKeyPressed = true;
		} else {
			clearGraph();
			mHRSManager.close();
			super.onBackPressed();
		}
	}

	private void setGUI() {
		setupActionBar();
		mLineGraph = LineGraphView.getLineGraphView();
		mConnectButton = (Button) findViewById(R.id.action_connect);
		mHRSName = (TextView) findViewById(R.id.text_hrs_device_name);
		mHRSValue = (TextView) findViewById(R.id.text_hrs_value);
		mHRSPosition = (TextView) findViewById(R.id.text_hrs_position);
		mHRSBatteryValue = (TextView) findViewById(R.id.text_hrs_battery);
		showGraph();
	}

	private void setupActionBar() {
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	private void initializeHRSManager() {
		mHRSManager = HRSManager.getHRSManager();
		mHRSManager.setGattCallbacks(this);
	}

	private void isBLESupported() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			showToast(R.string.no_ble);
			finish();
		}
	}

	private void showToast(final int messageResId) {
		Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show();
	}

	private void showToast(final String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
				final ScannerFragment dialog = ScannerFragment.getInstance(mContext, mBluetoothAdapter, HRSManager.HR_SERVICE_UUID, false);
				dialog.show(getFragmentManager(), "scan_fragment");
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

	private void setHRSNameOnView(final String name) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (name != null) {
					mHRSName.setText(name);
				} else {
					mHRSName.setText(R.string.hrs_default_name);
				}
			}
		});
	}

	private void setHRSValueOnView(final int value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (value >= MIN_POSITIVE_VALUE && value <= MAX_HR_VALUE) {
					mHRSValue.setText(Integer.toString(value));
				} else {
					mHRSValue.setText(R.string.not_available_value);
				}
			}
		});
	}

	private void setHRSPositionOnView(final String position) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (position != null) {
					mHRSPosition.setText(position);
				} else {
					mHRSPosition.setText(R.string.not_available);
				}
			}
		});
	}

	private void setHRSBatteryValueOnView(final int value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (value >= MIN_POSITIVE_VALUE && value <= MAX_BATTERY_VALUE) {
					mHRSBatteryValue.setText(getString(R.string.battery, value));
				} else {
					mHRSBatteryValue.setText(R.string.not_available);
				}
			}
		});
	}

	/**
	 * Callback of CONNECT/DISCONNECT button on HRSActivity
	 */
	public void onConnectClicked(final View view) {
		if (isBLEEnabled()) {
			if (mDevice == null) {
				showDeviceScanningDialog();
			} else if (!isDeviceConnected) {
				mHRSManager.connect(mContext, mDevice);
			} else {
				mHRSManager.disconnect();
			}
		} else {
			showBLEDialog();
		}
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
			clearGraph();
			mHRSManager.close();
			finish();
		}
	}

	@Override
	public void onHRServiceFound() {
		setHRSNameOnView(mDevice.getName());
	}

	@Override
	public void onHRSensorPositionFound(String position) {
		setHRSPositionOnView(position);
	}

	@Override
	public void onHRNotificationEnabled() {
		Logger.d(TAG, "onHRNotificationEnabled()");
		clearGraph();
		startShowGraph();
		isGraphInProgress = true;
	}

	@Override
	public void onHRValueReceived(int value) {
		Logger.d(TAG, "HRMValue: " + value);
		mHrmValue = value;
		setHRSValueOnView(mHrmValue);
	}

	@Override
	public void onError(String message, int errorCode) {
		Logger.e(TAG, "onError() " + message + " ErrorCode: " + errorCode);
		showErrorMessage(message, errorCode);
	}

	private void showErrorMessage(final String message, final int code) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showToast(message + " Error code: " + Integer.toString(code));
			}
		});
	}

	@Override
	public void onBatteryServiceFound() {
		// do nothing
	}

	@Override
	public void onBatteryValueReceived(final int value) {
		setHRSBatteryValueOnView(value);
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device) {
		mDevice = device;
		mHRSManager.connect(mContext, mDevice);
	}

	private void reset() {
		mDevice = null;
		mCounter = 0;
		isDeviceConnected = false;
		isGraphInProgress = false;
		setDefaultUI();
		stopShowGraph();
		mHRSManager.closeBluetoothGatt();
		mHRSManager.resetStatus();
	}

	private void setDefaultUI() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectButton.setText(R.string.action_connect);
				mHRSName.setText(R.string.hrs_default_name);
				mHRSValue.setText(R.string.not_available_value);
				mHRSPosition.setText(R.string.not_available);
				mHRSBatteryValue.setText(R.string.not_available);
			}
		});
	}

	private void clearGraph() {
		mLineGraph.clearGraph();
		mGraphView.repaint();
	}

}
