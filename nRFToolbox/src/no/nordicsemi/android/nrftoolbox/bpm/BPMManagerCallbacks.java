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

public interface BPMManagerCallbacks {
	public static final int UNIT_mmHG = 0;
	public static final int UNIT_kPa = 1;

	public void onDeviceConnected();

	public void onDeviceDisconnected();

	public void onServicesDiscovered(final boolean bloodPressure, final boolean cuffPressure, final boolean batteryService);

	public void onBloodPressureMeasurementIndicationsEnabled();

	public void onIntermediateCuffPressureNotificationEnabled();

	public void onBloodPressureMeasurmentRead(final float systolic, final float diastolic, final float meanArterialPressure, final int unit);

	public void onIntermediateCuffPressureRead(final float cuffPressure, final int unit);

	public void onPulseRateRead(final float pulseRate);

	public void onTimestampRead(final Calendar calendar);

	public void onBatteryValueReceived(final int value);

	public void onBondingRequired();

	public void onBonded();

	public void onError(final String message, final int errorCode);
}
