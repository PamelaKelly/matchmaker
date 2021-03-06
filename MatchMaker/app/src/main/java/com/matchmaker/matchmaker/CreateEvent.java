package com.matchmaker.matchmaker;

/**************************************************************************************************
 CreateEvent
 Authors: Emma Byrne, Pamela Kelly
 Date: 18/11/2017
 Course: COMP 41690 Android Programming
 Usage: Code to create an event. User selects type of sport, time, day and location and clicks
 create match. This match gets created and it is added to the Firebase API database and the users'
 local database.
 **************************************************************************************************/

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class CreateEvent extends AppCompatActivity {
    private int mYear, mMonth, mDay, mHour, mMinute;
    private String userTimeChoice, userDateChoice;
    private String eventName, eventLocation, eventTime, eventOrganiser, eventActivity, eventDate;

    myDbAdapter dbAdapter;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbAdapter = new myDbAdapter(this); // start a db instance
        firebaseAuth = FirebaseAuth.getInstance(); // get the info from firebase
        setContentView(R.layout.activity_create_event);
        // Code from match preferences activity
        //######################################SPINNER########################################
        // Setting up the Spinner for sport choice
        Spinner spinner = (Spinner) findViewById(R.id.sports_spinner);

        //Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter
                .createFromResource(this, R.array.sports_list_values,
                        android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                eventActivity = parent.getItemAtPosition(pos).toString();
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    //##################################### Time Picker Set Up #################################
    // reference: https://www.journaldev.com/9976/android-date-time-picker-dialog
    public void showTimePickerDialog(View v) {
        // function for displaying the time picker
        final EditText editTextTime = (EditText) findViewById(R.id.time);
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        editTextTime.setText(hourOfDay + ":" + minute);
                        userTimeChoice = hourOfDay + ":" + minute;
                    }
                }, mHour, mMinute, false);
        timePickerDialog.show();
    }

    //##################################### Date Picker Set Up ##################################
    // reference: https://www.journaldev.com/9976/android-date-time-picker-dialog
    public void showDatePickerDialog(View v) {
        final EditText editTextDate = (EditText) findViewById(R.id.date);
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                editTextDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);
                userDateChoice = dayOfMonth + "-" + (monthOfYear + 1) + "-" + year;
            }
        }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    // Method to create the event
    public void createEvent(View view){
        // onclick function for create event
        // get the data from the activity
        // first check that all the things are filled in
        if (getEventData(view)) {
            EditText edit = (EditText)findViewById(R.id.location);
            eventLocation = edit.getText().toString();
            addToUserDB(view); // call to update the local database
            addEventToRemoteDB(view); // add event to firebase

            // store data in array to pass to send with intent
            String[] matchDetails = {eventOrganiser, eventDate, eventTime, eventLocation};
            Event eventDetails = new Event(eventDate, eventLocation, eventName, eventOrganiser, eventOrganiser, eventTime);
            String matchDetailsString = eventDetails.toString();

            // redirect to event details page
            Intent intent = new Intent(this, MatchDetailsActivity.class);
            intent.putExtra("Activity", eventActivity);
            intent.putExtra("Match Details", matchDetailsString);
            startActivity(intent);
        } else {
            Message.message(this, "Please make sure all the fields are filled in");
        }
    }

    public Boolean getEventData(View view) {
        // get info for adding to the two databases
        EditText event = (EditText) findViewById(R.id.eventName);
        EditText time = (EditText) findViewById(R.id.time);
        EditText date = (EditText) findViewById(R.id.date);
        EditText location = (EditText) findViewById(R.id.location); // This may be changed to something else

        eventName = event.getText().toString();
        eventTime = time.getText().toString();
        eventDate = date.getText().toString();

        if (eventName.isEmpty() || eventTime.isEmpty() || eventDate.isEmpty()){
            return false;
        } else {
            return true;
        }
    }

    // Method to add match to user DB
    // Do checks to make sure the form fields are filled out correctly
    public void addToUserDB(View view){
        // get user details
        FirebaseUser user = firebaseAuth.getCurrentUser();
        String email  = user.getEmail(); // get the users' email to pass into updateEvents
        String oldEvent = dbAdapter.getEventData(email);

        StringBuilder eventInfo = new StringBuilder();
        eventInfo.append(eventName + ", " + eventTime + ", " + eventDate);
        dbAdapter.updateEvents(email, oldEvent, eventInfo.toString());
    }

    public void addEventToRemoteDB(View view) {
        //############### Storing Data Remotely in Firebase ####################
        // Add the user to the Users Info Table in the Firebase Database
        EditText location = (EditText) findViewById(R.id.location);
        eventLocation = location.toString();
        // Get an instance of the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        // Get a reference for the "users" section
        DatabaseReference myRef = database.getReference("events");

        // How to get the organiser? Current logged in user?
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        eventOrganiser = firebaseUser.getEmail().toString();
        // Create an Event object and push that to the database
        Event newEvent = new Event(eventDate, eventLocation, eventName, eventOrganiser, eventOrganiser, eventTime);
        myRef.child(eventActivity.toLowerCase()).child(eventName).setValue(newEvent);
    }
}

