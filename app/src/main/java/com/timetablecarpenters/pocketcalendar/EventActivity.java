package com.timetablecarpenters.pocketcalendar;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Text;

import java.util.Calendar;

/**
 * This activity shows the user information about their event
 */
public class EventActivity extends BaseActivity {
    public final static String EVENT_VIEW_INTENT_KEY ="get_event";
    private CalendarEvent event;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

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
        MapFragment mapFragment =  (MapFragment) (getSupportFragmentManager().findFragmentById(R.id.event_map));
        Button buttonDelete = (Button) content.findViewById(R.id.button_delete);
        Button buttonEdit = (Button) content.findViewById(R.id.button_edit);
        Button buttonOpenMaps = (Button) content.findViewById(R.id.to_maps_button);
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
                startActivity(intent);
            }
        });
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo: edit view open
            }
        });


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
        }
        // set mapview
        LatLng location = event.getLocation();
        if (location != null) {
            mapFragment.moveToLocation(location);
        }

        buttonOpenMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (event.getLocation() == null) {
                    Toast.makeText(EventActivity.this, "Event has no registered location", Toast.LENGTH_SHORT).show();
                } else {
                    // opens the location in google maps
                    // Create a Uri from an intent string. Use the result to create an Intent.
                    Uri gmmIntentUri = Uri.parse(String.format("geo:%f,%f", event.location.latitude, event.location.longitude));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }
            }
        });





    }
}