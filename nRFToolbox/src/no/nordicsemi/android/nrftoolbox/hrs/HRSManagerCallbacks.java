/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.nrftoolbox.hrs;

public interface HRSManagerCallbacks {

	public void onDeviceConnected();

	public void onDeviceDisconnected();

	public void onHRServiceFound();

	public void onBatteryServiceFound();

	public void onBatteryValueReceived(int value);

	public void onHRSensorPositionFound(String position);

	public void onHRValueReceived(int value);

	public void onError(String message, int errorCode);

	public void onHRNotificationEnabled();
}
