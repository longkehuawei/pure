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
 * When back key is pressed during uploading this fragment shows exit confirmation dialog
 */
public class ExitConfirmationFragment extends DialogFragment {
	private static DfuManager mDFUManager;

	public static ExitConfirmationFragment getInstance(DfuManager manager) {
		ExitConfirmationFragment fragment = new ExitConfirmationFragment();
		mDFUManager = manager;
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity()).setTitle(R.string.dfu_confirmation_dialog_title).setMessage(R.string.dfu_confirmation_dialog_exit_message)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						mDFUManager.systemReset();
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
						mDFUManager.close();
						getActivity().finish();
					}
				}).setNegativeButton(android.R.string.no, null).create();
	}
}
