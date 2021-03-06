package com.timetablecarpenters.pocketcalendar;

import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import java.util.Calendar;

/**
 * This activity shows the user information about their event
 * @author Deniz Mert Dilaverler
 * @version 03.05.2021
 */
public class EventActivity extends BaseActivity {
    private static final String TAG = "EventActivity";
    public final static String EVENT_VIEW_INTENT_KEY ="get_event";
    private CalendarEvent event;
    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        toolbar = (Toolbar) findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);


        // set colors
        ( (TextView) findViewById( R.id.event_name)).setTextColor( CustomizableScreen.getBackGColor());
        ( (TextView) findViewById( R.id.event_type)).setTextColor( CustomizableScreen.getBackGColor());
        ( (TextView) findViewById( R.id.date_text_view)).setTextColor( CustomizableScreen.getBackGColor());
        ( (TextView) findViewById( R.id.event_time)).setTextColor( CustomizableScreen.getBackGColor());
        ( (TextView) findViewById( R.id.event_notifications)).setTextColor( CustomizableScreen.getBackGColor());
        ( (TextView) findViewById( R.id.event_notes)).setTextColor( CustomizableScreen.getBackGColor());
        ( (TextView) findViewById( R.id.event_location)).setTextColor( CustomizableScreen.getBackGColor());
        ( (TextView) findViewById( R.id.notes_contents)).setTextColor( CustomizableScreen.getBackGColor());
        ( findViewById( R.id.notes_contents)).setBackgroundColor( CustomizableScreen.getButtonColor());
        ( findViewById( R.id.event_view_back)).setBackgroundColor( CustomizableScreen.backgroundColor);


        Bundle extras = getIntent().getExtras();
        if(extras != null)
            event = (CalendarEvent) extras.get(EVENT_VIEW_INTENT_KEY);

        if(event != null) {
            initEventView();
        }
    }

    /**
     * initializes eventView elements
     */
    public void initEventView() {
        View content = findViewById(R.id.event_view_content);
        TextView eventName = content.findViewById(R.id.event_name);
        TextView eventType = content.findViewById(R.id.event_type);
        TextView notificationText = content.findViewById(R.id.event_notifications);
        TextView noteContents = content.findViewById(R.id.notes_contents);
        Button buttonDelete = (Button) content.findViewById(R.id.button_delete);
        Button buttonEdit = (Button) content.findViewById(R.id.button_edit);
        TextView dateText = (TextView) content.findViewById(R.id.date_text_view);
        TextView timeText = (TextView) content.findViewById(R.id.event_time);

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            /**
             * Deletes the activity from db and opens dayActivity
             * @param v
             */
            @Override
            public void onClick(View v) {
                DBHelper dbHelper = new DBHelper(EventActivity.this, DBHelper.DB_NAME, null);
                dbHelper.deleteEvent(event);

                Intent intent = new Intent(EventActivity.this, DayActivity.class);
                intent.putExtra(DayActivity.INTENT_KEY, event.eventStart);
                Toast.makeText(EventActivity.this, "Event Deleted", Toast.LENGTH_SHORT).show();
                startActivity(intent);
            }
        });
        buttonEdit.setOnClickListener(new EventActivity.ViewChangeClickListener());


        //set name and type text
        eventName.setText(event.getName());
        eventType.setText(event.getType());
        eventName.setTextColor(event.getColor());
        eventType.setTextColor(event.getColor());
        // set date text
        Calendar eventStart = event.getEventStart();
        String dateTextString = String.format("%d.%d.%d", eventStart.get(Calendar.DAY_OF_MONTH), eventStart.get(Calendar.MONTH ) + 1,  eventStart.get(Calendar.YEAR));
        dateText.setText(dateTextString);
        // set time text
        String timeTextString = event.getEventStartTime();
        if (!timeTextString.equalsIgnoreCase(event.getEventEndTime()))
            timeTextString += "-" + event.getEventEndTime();
        timeText.setText(timeTextString);
        // set notes text
        String notesText = event.getNotes();
        if(notesText != null) {
            noteContents.setText(notesText);
        }
        // set notifications text
        String notifTime = event.getNotifTime();
        if(notifTime != null) {
            notificationText.setText("Notifications: " + notifTime);
        } else {
            Log.d(TAG, "initEventView: notification not found ");
        }
        // set mapview
        MapFragment mapFragment = new MapFragment();
        mapFragment.addEvent(event);

        getSupportFragmentManager().beginTransaction().add(R.id.event_map, mapFragment).commit();

    }

    public class ViewChangeClickListener implements View.OnClickListener {
        /**
         * when clicked creates an intent of the desired activity and starts the activity
         * @param v
         */
        @Override
        public void onClick(View v) {
            Intent intent;
            switch (v.getId()) {
                case R.id.button_edit:
                    Log.d("EventActivity", "onClick: edit event opening");
                    intent = new Intent(EventActivity.this, AddEvent.class);
                    intent.putExtra(AddEvent.EDIT_EVENT_KEY, event);
                    startActivity(intent);
                    break;
            }
        }
    }

    /**
     * goes to the DayActivity when the back button is pressed
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, DayActivity.class);
        intent.putExtra(DayActivity.INTENT_KEY, event.getEventStart());
        startActivity(intent);
    }

}