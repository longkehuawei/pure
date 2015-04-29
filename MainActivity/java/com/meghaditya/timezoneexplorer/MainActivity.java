package com.meghaditya.timezoneexplorer;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity implements AdapterView.OnItemSelectedListener {
    private Spinner mTimeZoneSpinner;
    private TextView mTimeZoneInformationTextView;
    private TextView mTimeZoneRawDataTextView;
    private TextView mTimeZoneDSTTextView;

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // TODO: Add options for selecting DST transition range
            return true;
        }

        return super.onOptionsItemSelected(item);
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