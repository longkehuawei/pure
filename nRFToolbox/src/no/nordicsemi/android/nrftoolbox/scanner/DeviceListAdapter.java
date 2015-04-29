/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA. Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.nrftoolbox.scanner;

import java.util.ArrayList;

import no.nordicsemi.android.nrftoolbox.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * DeviceListAdapter class is list adapter for showing scanned Devices name, address and RSSI image based on RSSI values.
 */
public class DeviceListAdapter extends ArrayAdapter<ExtendedBluetoothDevice> {
	private final Context mContext;
	private final ArrayList<ExtendedBluetoothDevice> mListValues;

	public DeviceListAdapter(Context context, ArrayList<ExtendedBluetoothDevice> devices) {
		super(context, R.layout.device_list_row, devices);
		this.mContext = context;
		this.mListValues = devices;
	}

	@Override
	public View getView(int position, View oldView, ViewGroup parent) {
		View view = oldView;
		if (view == null) {
			final LayoutInflater inflater = LayoutInflater.from(mContext);
			view = inflater.inflate(R.layout.device_list_row, parent, false);
			final ViewHolder holder = new ViewHolder();
			holder.name = (TextView) view.findViewById(R.id.name);
			holder.address = (TextView) view.findViewById(R.id.address);
			holder.rssi = (ImageView) view.findViewById(R.id.rssi);
			view.setTag(holder);
		}

		final ViewHolder holder = (ViewHolder) view.getTag();
		holder.name.setText(mListValues.get(position).device.getName());
		holder.address.setText(mListValues.get(position).device.getAddress());
		final int rssiPercent = (int) (100.0f * (127.0f + mListValues.get(position).rssi) / (127.0f + 20.0f));
		holder.rssi.setImageLevel(rssiPercent);
		return view;
	}

	private class ViewHolder {
		private TextView name;
		private TextView address;
		private ImageView rssi;
	}
}
