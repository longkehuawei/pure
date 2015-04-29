package com.meghaditya.timezoneexplorer;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;


public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private Spinner mTimeZoneSpinner;
    private TextView mTimeZoneInformationTextView;
    private TextView mTimeZoneRawDataTextView;
    private TextView mTimeZoneDSTTextView;//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTimeZoneSpinner = (Spinner) findViewById(R.id.timeZoneSpinner);
        mTimeZoneInformationTextView = (TextView) findViewById(R.id.timeZoneInformation);
        mTimeZoneRawDataTextView = (TextView) findViewById(R.id.timeZoneRawData);
        mTimeZoneDSTTextView = (TextView) findViewById(R.id.timeZoneDST);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.timezone_values, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mTimeZoneSpinner.setAdapter(adapter);
        mTimeZoneSpinner.setOnItemSelectedListener(this);
    }

   
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // An item was selected. You can retrieve the selected item using
        TimeZoneWrapper tzWrapper = new TimeZoneWrapper(parent.getItemAtPosition(position).toString());
        mTimeZoneInformationTextView.setText(tzWrapper.printInformation());
        mTimeZoneRawDataTextView.setText(tzWrapper.dump());
        mTimeZoneDSTTextView.setText(tzWrapper.printDSTInformation());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
}