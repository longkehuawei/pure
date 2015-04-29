/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.nrftoolbox.dfu.fragment;

import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.dfu.DfuManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * When cancel button is pressed during uploading this fragment shows uploading cancel dialog
 */
public class UploadCancelFragment extends DialogFragment {
	private static DfuManager mDFUManager;

	public static UploadCancelFragment getInstance(DfuManager manager) {
		UploadCancelFragment fragment = new UploadCancelFragment();
		mDFUManager = manager;
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity()).setTitle(R.string.dfu_confirmation_dialog_title).setMessage(R.string.dfu_upload_dialog_cancel_message)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						mDFUManager.systemReset();
					}
				}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						mDFUManager.resumeSendingPacket();
					}
				}).create();
	}
}
