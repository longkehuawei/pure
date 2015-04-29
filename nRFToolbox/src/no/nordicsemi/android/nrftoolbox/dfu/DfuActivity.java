/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA. Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.nrftoolbox.dfu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import no.nordicsemi.android.nrftoolbox.AppHelpFragment;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.dfu.adapter.FileBrowserAppsAdapter;
import no.nordicsemi.android.nrftoolbox.dfu.fragment.ExitConfirmationFragment;
import no.nordicsemi.android.nrftoolbox.dfu.fragment.UploadCancelFragment;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;
import no.nordicsemi.android.nrftoolbox.utility.Logger;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * DfuActivity is the main DFU activity It implements DFUManagerCallbacks to receive callbacks from DFUManager class It implements DeviceScannerFragment.OnDeviceSelectedListener callback to receive
 * callback when device is selected from scanning dialog The activity supports portrait and landscape orientations
 */
public class DfuActivity extends Activity implements LoaderCallbacks<Cursor>, DfuManagerCallbacks, ScannerFragment.OnDeviceSelectedListener {
	private static final String TAG = "DfuActivity";

	private static final String DATA_FILE_PATH = "file_path";
	private static final String DATA_FILE_STREAM = "file_stream";
	private static final String DATA_STATUS = "status";
	private static final String CONNECTION_STATUS = "connection_status";
	private static final String FILE_TRANSFER_STATUS = "file_transfer_status";
	private static final String DFU_SERVICE_STATUS = "dfu_service_status";
	private static final String FILE_VALIDATION_STATUS = "file_validation_status";
	private static final String FILE_TRANSFER_PERCENTAGE = "file_transfer_percentage";

	private static final String EXTRA_URI = "uri";

	private static final int SELECT_FILE_REQ = 1;
	static final int REQUEST_ENABLE_BT = 2;

	private TextView mDeviceNameView;
	private TextView mFileNameView;
	private TextView mFileSizeView;
	private TextView mFileStatusView;
	private TextView mTextPercentage, mTextUploading;
	private boolean mStatusOk;

	private Button mUploadButton, mConnectButton;

	private String mFilePath;
	private Uri mFileStreamUri;

	private DfuManager mDFUManager;
	public BluetoothAdapter mBluetoothAdapter;
	private BluetoothManager mBluetoothManager;
	private BluetoothDevice mDevice;
	private boolean isDFUServiceFound = false;
	private boolean isFileValidated = false;
	private boolean isDeviceConnected = false;

	public Context mContext;

	public static final int NO_TRANSFER = 0;
	public static final int START_TRANSFER = 1;
	public static final int FINISHED_TRANSFER = 2;
	private int mFileTransferStatus = NO_TRANSFER;

	ProgressBar mProgressBar;
	private int mPercentage = 0;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feature_dfu);
		mContext = getApplicationContext();
		setBluetoothAdapter();
		isBLESupported();
		if (!isBLEEnabled()) {
			showBLEDialog();
		}
		initializeDFUManager();
		setGUI();

		/*
		 *  copy example HEX files to the external storage. 
		 *  Files will be copied if the DFU Applications folder is missing
		 */
		final File folder = new File(Environment.getExternalStorageDirectory(), "Nordic Semiconductor");
		if (!folder.exists()) {
			folder.mkdir();

			copyRawResource(R.raw.ble_app_hrs, new File(folder, "ble_app_hrs.hex"));
			copyRawResource(R.raw.ble_app_rscs, new File(folder, "ble_app_rscs.hex"));

			Toast.makeText(this, R.string.dfu_example_files_created, Toast.LENGTH_LONG).show();
		}

		// restore saved state
		if (savedInstanceState != null) {
			mFilePath = savedInstanceState.getString(DATA_FILE_PATH);
			mFileStreamUri = savedInstanceState.getParcelable(DATA_FILE_STREAM);
			mStatusOk = savedInstanceState.getBoolean(DATA_STATUS);
			mUploadButton.setEnabled(mStatusOk);
			isDeviceConnected = savedInstanceState.getBoolean(CONNECTION_STATUS);
			isFileValidated = savedInstanceState.getBoolean(FILE_VALIDATION_STATUS);
			isDFUServiceFound = savedInstanceState.getBoolean(DFU_SERVICE_STATUS);
			mPercentage = savedInstanceState.getInt(FILE_TRANSFER_PERCENTAGE);
			mFileTransferStatus = savedInstanceState.getInt(FILE_TRANSFER_STATUS);

			if (isDeviceConnected) {
				mConnectButton.setText(R.string.action_disconnect);
			} else {
				mConnectButton.setText(R.string.action_connect);
			}
			if (mFileTransferStatus == START_TRANSFER) {
				setUploadButtonText(R.string.dfu_action_upload_cancel);
				disableConnectButton();
				showProgressBar();
				updateProgressBar();
			}
			if (mDFUManager != null) {
				mDevice = mDFUManager.getDevice();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(DATA_FILE_PATH, mFilePath);
		outState.putParcelable(DATA_FILE_STREAM, mFileStreamUri);
		outState.putBoolean(DATA_STATUS, mStatusOk);

		outState.putBoolean(CONNECTION_STATUS, isDeviceConnected);
		outState.putBoolean(FILE_VALIDATION_STATUS, isFileValidated);
		outState.putBoolean(DFU_SERVICE_STATUS, isDFUServiceFound);
		outState.putInt(FILE_TRANSFER_PERCENTAGE, mPercentage);
		outState.putInt(FILE_TRANSFER_STATUS, mFileTransferStatus);
	}

	private void setGUI() {
		setupActionBar();
		mDeviceNameView = (TextView) findViewById(R.id.device_name);
		mFileNameView = (TextView) findViewById(R.id.file_name);
		mFileSizeView = (TextView) findViewById(R.id.file_size);
		mFileStatusView = (TextView) findViewById(R.id.file_status);

		mUploadButton = (Button) findViewById(R.id.action_upload);
		mConnectButton = (Button) findViewById(R.id.action_connect);
		mTextPercentage = (TextView) findViewById(R.id.textviewProgress);
		mTextUploading = (TextView) findViewById(R.id.textviewUploading);
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar_file);
		hideProgressBar();
	}

	private void setupActionBar() {
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	private void setDFUNameOnView(final String name) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (name != null) {
					mDeviceNameView.setText(name);
				} else {
					mDeviceNameView.setText(R.string.dfu_default_name);
				}
			}
		});
	}

	private void setDefaultDFUNameOnView() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceNameView.setText(R.string.dfu_default_name);
			}
		});
	}

	@Override
	public void onBackPressed() {
		if (mFileTransferStatus == START_TRANSFER) {
			showExitConfirmationDialogue("Application transferring", "Are you sure to exit?");
		} else {
			mDFUManager.close();
			super.onBackPressed();
		}
	}

	private void initializeDFUManager() {
		mDFUManager = DfuManager.getDFUManager();
		mDFUManager.setGattCallbacks(this);
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
				FragmentManager fm = getFragmentManager();
				ScannerFragment dialog = ScannerFragment.getInstance(mContext, mBluetoothAdapter, DfuManager.DFU_SERVICE_UUID, true);
				dialog.show(fm, "scan_fragment");
			}
		});

	}

	private void showProgressBar() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mProgressBar.setVisibility(View.VISIBLE);
				mTextPercentage.setVisibility(View.VISIBLE);
				mTextUploading.setVisibility(View.VISIBLE);
			}
		});
	}

	private void hideProgressBar() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mProgressBar.setVisibility(View.INVISIBLE);
				mTextPercentage.setVisibility(View.INVISIBLE);
				mTextUploading.setVisibility(View.INVISIBLE);
			}
		});
	}

	private void updateProgressBar() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mProgressBar.setProgress(mPercentage);
				mTextPercentage.setText(getString(R.string.battery, mPercentage));
			}
		});
	}

	/**
	 * Copies the file from res/raw with given id to given destination file. If dest does not exist it will be created.
	 * 
	 * @param rawResId
	 *            the resource id
	 * @param dest
	 *            destination file
	 */
	private void copyRawResource(final int rawResId, final File dest) {
		try {
			final InputStream is = getResources().openRawResource(rawResId);
			final FileOutputStream fos = new FileOutputStream(dest);

			final byte[] buf = new byte[1024];
			int read = 0;
			try {
				while ((read = is.read(buf)) > 0)
					fos.write(buf, 0, read);
			} finally {
				is.close();
				fos.close();
			}
		} catch (final IOException e) {
			Logger.e(TAG, "Error while copying HEX file " + e.toString());
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
			final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.dfu_about_text);
			fragment.show(getFragmentManager(), "help_fragment");
			break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (resultCode != RESULT_OK)
			return;

		switch (requestCode) {
		case SELECT_FILE_REQ:
			// clear previous data
			mFilePath = null;
			mFileStreamUri = null;

			// and read new one
			final Uri uri = data.getData();
			/*
			 * The URI returned from application may be in 'file' or 'content' schema.
			 * 'File' schema allows us to create a File object and read details from if directly.
			 * 
			 * Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
			 */
			if (uri.getScheme().equals("file")) {
				// the direct path to the file has been returned
				final String path = uri.getPath();
				final File file = new File(path);
				mFilePath = path;

				mFileNameView.setText(file.getName());
				mFileSizeView.setText(getString(R.string.dfu_file_size_text, file.length()));
				final boolean isHexFile = mStatusOk = MimeTypeMap.getFileExtensionFromUrl(path).equalsIgnoreCase("bin");
				mFileStatusView.setText(isHexFile ? R.string.dfu_file_status_ok : R.string.dfu_file_status_invalid);
				mUploadButton.setEnabled(isHexFile);
			} else if (uri.getScheme().equals("content")) {
				// an Uri has been returned
				mFileStreamUri = uri;
				// if application returned Uri for streaming, let's us it. Does it works?
				// FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
				final Bundle extras = data.getExtras();
				if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
					mFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);

				// file name and size must be obtained from Content Provider
				final Bundle bundle = new Bundle();
				bundle.putParcelable(EXTRA_URI, uri);
				getLoaderManager().restartLoader(0, bundle, this);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		final Uri uri = args.getParcelable(EXTRA_URI);
		final String[] projection = new String[] { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA };
		return new CursorLoader(this, uri, projection, null, null, null);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mFileNameView.setText(null);
		mFileSizeView.setText(null);
		mFilePath = null;
		mFileStreamUri = null;
		mStatusOk = false;
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		if (data.moveToNext()) {
			final String fileName = data.getString(0 /* DISPLAY_NAME */);
			final int fileSize = data.getInt(1 /* SIZE */);
			final String filePath = data.getString(2 /* DATA */);
			if (!TextUtils.isEmpty(filePath))
				mFilePath = filePath;

			mFileNameView.setText(fileName);
			mFileSizeView.setText(getString(R.string.dfu_file_size_text, fileSize));
			final boolean isHexFile = mStatusOk = MimeTypeMap.getFileExtensionFromUrl(fileName).equalsIgnoreCase("HEX");
			mFileStatusView.setText(isHexFile ? R.string.dfu_file_status_ok : R.string.dfu_file_status_invalid);
			mUploadButton.setEnabled(isHexFile);
		}
	}

	/**
	 * Called when the question mark was pressed
	 * 
	 * @param view
	 *            a button that was pressed
	 */
	public void onSelectFileHelpClicked(final View view) {
		new AlertDialog.Builder(this).setTitle(R.string.dfu_help_title).setMessage(R.string.dfu_help_message).setPositiveButton(android.R.string.ok, null).show();
	}

	/**
	 * Called when Select File was pressed
	 * 
	 * @param view
	 *            a button that was pressed
	 */
	public void onSelectFileClicked(final View view) {
		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("file/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		if (intent.resolveActivity(getPackageManager()) != null) {
			// file browser has been found on the device
			startActivityForResult(intent, SELECT_FILE_REQ);
		} else {
			// there is no any file browser app, let's try to download one
			final View customView = getLayoutInflater().inflate(R.layout.app_file_browser, null);
			final ListView appsList = (ListView) customView.findViewById(android.R.id.list);
			appsList.setAdapter(new FileBrowserAppsAdapter(this));
			appsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			appsList.setItemChecked(0, true);
			new AlertDialog.Builder(this).setTitle(R.string.dfu_alert_no_filebrowser_title).setView(customView).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					dialog.dismiss();
				}
			}).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					final int pos = appsList.getCheckedItemPosition();
					if (pos >= 0) {
						final String query = getResources().getStringArray(R.array.dfu_app_file_browser_action)[pos];
						final Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(query));
						startActivity(storeIntent);
					}
				}
			}).show();
		}

	}

	/**
	 * Callback of UPDATE/CANCEL button on DfuActivity
	 */
	public void onUploadClicked(final View view) {
		// check whether the selected file is a HEX file (we are just checking the extension)
		if (!mStatusOk) {
			Toast.makeText(this, R.string.dfu_file_status_invalid_message, Toast.LENGTH_LONG).show();
			return;
		}
		if (mFileTransferStatus == START_TRANSFER) {
			mDFUManager.stopSendingPacket();
			showUploadCancelDialogue();
		} else {
			try {
				if (isDFUServiceFound) {
					InputStream hexStream = mFileStreamUri != null ? getContentResolver().openInputStream(mFileStreamUri) : new FileInputStream(mFilePath);
					mDFUManager.openFile(hexStream);
					isFileValidated = false;
					mDFUManager.enableNotification();
					setUploadButtonText(R.string.dfu_action_upload_cancel);
				} else {
					showToast("DFU device is not connected with phone");
				}
			} catch (FileNotFoundException e) {
				Logger.e(TAG, "An exception occured while opening file" + " " + e);
			}
		}
	}

	private void showUploadCancelDialogue() {
		UploadCancelFragment fragment = UploadCancelFragment.getInstance(mDFUManager);
		fragment.show(getFragmentManager(), TAG);
	}

	private void showExitConfirmationDialogue(String title, String message) {
		ExitConfirmationFragment fragment = ExitConfirmationFragment.getInstance(mDFUManager);
		fragment.show(getFragmentManager(), TAG);
	}

	private void setUploadButtonText(final int label) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mUploadButton.setText(label);
			}
		});
	}

	/**
	 * Callback of CONNECT/DISCONNECT button on DfuActivity
	 */
	public void onConnectClicked(final View view) {
		if (isBLEEnabled()) {
			if (mDevice == null) {
				showDeviceScanningDialog();
			} else if (!isDeviceConnected) {
				mDFUManager.connect(mContext, mDevice);
			} else {
				mDFUManager.disconnect();
			}
		} else {
			showBLEDialog();
		}
	}

	private void showFileTransferSuccessMessage() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showToast("Application has been transfered successfully!");
			}
		});
	}

	private void showFileTransferUnSuccessMessage() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showToast("Uploading of Application has been interrupted!");
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

	private void showDisconnectedButton() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectButton.setEnabled(true);
				mConnectButton.setText(R.string.action_connect);
			}
		});
	}

	private void disableConnectButton() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectButton.setEnabled(false);
			}
		});
	}

	private void reset() {
		hideProgressBar();
		showDisconnectedButton();
		setUploadButtonText(R.string.dfu_action_upload);
		setDefaultDFUNameOnView();
		mDevice = null;
		mFileTransferStatus = NO_TRANSFER;
		isDFUServiceFound = false;
		mDFUManager.closeFile();
		mDFUManager.closeBluetoothGatt();
		mDFUManager.resetStatus();
	}

	@Override
	public void onDeviceConnected() {
		Logger.d(TAG, "onDeviceConnected()");
		isDeviceConnected = true;
	}

	@Override
	public void onDFUServiceFound() {
		Logger.d(TAG, "onDFUServiceFound");
		isDFUServiceFound = true;
		showConnectedButton();
		setDFUNameOnView(mDevice.getName());
	}

	@Override
	public void onDeviceDisconnected() {
		Logger.d(TAG, "onDeviceDisconnected()");
		isDeviceConnected = false;
		if (mFileTransferStatus == START_TRANSFER) {
			showFileTransferUnSuccessMessage();
		}
		if (isFileValidated) {
			showFileTransferSuccessMessage();
			isFileValidated = false;
		}
		reset();
	}

	@Override
	public void onFileTransferStarted() {
		Logger.d(TAG, "onFileTransferStarted()");
		showProgressBar();
		mFileTransferStatus = START_TRANSFER;
		disableConnectButton();
	}

	@Override
	public void onFileTranfering(long sizeTransfered) {
		Logger.d(TAG, "onFileTransfering(): " + sizeTransfered);
		mPercentage = (int) ((sizeTransfered * 100) / mDFUManager.getFileSize());
		updateProgressBar();
	}

	@Override
	public void onFileTransferCompleted() {
		Logger.d(TAG, "onFileTransferCompleted()");
		mFileTransferStatus = FINISHED_TRANSFER;
		hideProgressBar();
		setUploadButtonText(R.string.dfu_action_upload);
	}

	@Override
	public void onFileTransferValidation() {
		Logger.d(TAG, "onFileTransferValidation()");
		isFileValidated = true;
	}

	@Override
	public void onError(final String message, final int errorCode) {
		Logger.e(TAG, "onError() " + message + " ErrorCode: " + errorCode);
		dealWithError(message, errorCode);
	}

	private void dealWithError(final String message, final int errorCode) {
		if (isDeviceConnected) {
			mDFUManager.systemReset();
		}
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
	public void onDeviceSelected(BluetoothDevice device) {
		Logger.d(TAG, "onDeviceSelected: " + device.getName());
		mDevice = device;
		mDFUManager.connect(mContext, mDevice);
	}

}
