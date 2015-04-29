package com.meghaditya.timezoneexplorer;

import android.text.format.DateUtils;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by meghaditya on 19/12/14.
 */
public class TimeZoneWrapper {
    TimeZone mTimeZone;
    StringBuilder mTZInfoString;
    StringBuilder mTZDSTString;
    SimpleDateFormat mFormatter;

    // TODO: Remove Hardcoding
    private static final String FORMAT = "yyyy-MM-dd, HH:mm:ss";
    private static final String START_DATE = "2014-01-01, 02:30:00";
    private static final String END_DATE = "2016-01-01, 02:30:00";
    private static final long ONE_DAY = DateUtils.DAY_IN_MILLIS;

    private static final String TAG = "TimeZoneWrapper";

    TimeZoneWrapper(String timeZoneId) {
        mTimeZone = TimeZone.getTimeZone(timeZoneId);

        mFormatter = new SimpleDateFormat(FORMAT);
        mFormatter.setLenient(false);

        mTZInfoString = new StringBuilder();
        mTZInfoString.append(timeZoneId);

        mTZDSTString = new StringBuilder();

        init();
    }

    private void init() {
        mTZInfoString.append("\nGMT Offset : ");
        mTZInfoString.append(createGmtOffsetString(true, true, mTimeZone.getRawOffset()));

        mTZInfoString.append("\nUse Day-Light Time : ");
        mTZInfoString.append(mTimeZone.useDaylightTime() ? "Yes" : "No");

        mTZInfoString.append("\nDST Savings : ");
        mTZInfoString.append(mTimeZone.getDSTSavings());

        calculateDSTtransitions();
    }

    private void calculateDSTtransitions() {
        try {
            Date startDate = mFormatter.parse(START_DATE);
            Date endDate = mFormatter.parse(END_DATE);

            while (startDate.before(endDate)) {
                Date nextDate = new Date(startDate.getTime() + ONE_DAY);
                if (!mTimeZone.inDaylightTime(startDate) && mTimeZone.inDaylightTime(nextDate)) {
                    mTZDSTString.append("\nDST Start : ");
                    mTZDSTString.append("\nFrom : ");
                    mTZDSTString.append(startDate.toString());
                    mTZDSTString.append("\nTo : ");
                    mTZDSTString.append(nextDate.toString());
                }
                if (mTimeZone.inDaylightTime(startDate) && !mTimeZone.inDaylightTime(nextDate)) {
                    mTZDSTString.append("\nDST End : ");
                    mTZDSTString.append("\nFrom : ");
                    mTZDSTString.append(startDate.toString());
                    mTZDSTString.append("\nTo : ");
                    mTZDSTString.append(nextDate.toString());
                }
                // increment startDate
                startDate = nextDate;
                Log.d(TAG, "Current Date = " + startDate.toString());
            }
        } catch (ParseException pEx) {
            Log.d(TAG, "ParseException" + pEx);
            pEx.printStackTrace();
            // mTZDSTString will be empty :-(
        }
    }

    public String printInformation() {
        return mTZInfoString.toString();
    }

    public String dump() {
        return mTimeZone.toString();
    }

    public String printDSTInformation() {
        return mTZDSTString.toString();
    }

    // Cribbed as in from java.util.TimeZone
    private String createGmtOffsetString(boolean includeGmt,
                                         boolean includeMinuteSeparator, int offsetMillis) {
        int offsetMinutes = offsetMillis / 60000;
        char sign = '+';
        if (offsetMinutes < 0) {
            sign = '-';
            offsetMinutes = -offsetMinutes;
        }
        StringBuilder builder = new StringBuilder(9);
        if (includeGmt) {
            builder.append("GMT");
        }
        builder.append(sign);
        appendNumber(builder, 2, offsetMinutes / 60);
        if (includeMinuteSeparator) {
            builder.append(':');
        }
        appendNumber(builder, 2, offsetMinutes % 60);
        return builder.toString();
    }

    // Cribbed as in from java.util.TimeZone
    private void appendNumber(StringBuilder builder, int count, int value) {
        String string = Integer.toString(value);
        for (int i = 0; i < count - string.length(); i++) {
            builder.append('0');
        }
        builder.append(string);
    }
}
