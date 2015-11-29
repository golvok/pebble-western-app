package golvok.bazooga;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.IOException;
import java.util.UUID;

public class BazoogaMainActivity extends AppCompatActivity {

    private final static String TWILLO_APP_SID = "APe8b5c9643269c95f4aa0faba695382ab";
    private final static String TWILLO_ACCOUNT_SID = "ACa397147b63109ee05368f637c3917fc5";
    private final static String TWILLO_AUTH_TOKEN = "86575c5ddbd9f46a3d6f3174c9cabd12";

    private final static UUID PEBBLE_APP_UUID = UUID.fromString("ce5606be-6fdf-43c4-9882-c4ad5bb949d2");
    private final static int MESSAGE_TYPE_KEY = 78;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bazooga_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        Log.i(getLocalClassName(), "Pebble is " + (connected ? "connected" : "not connected"));

        PebbleKit.registerPebbleConnectedReceiver(getApplicationContext(), new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(getLocalClassName(), "Pebble connected!");
            }

        });

        PebbleKit.registerPebbleDisconnectedReceiver(getApplicationContext(), new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(getLocalClassName(), "Pebble disconnected!");
            }

        });

        PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {

            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                long cycles = data.getInteger(MESSAGE_TYPE_KEY);
                Log.i(getLocalClassName(), "Received value=" + cycles + " for key: " + Integer.toString(MESSAGE_TYPE_KEY));

                PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);

                long total_seconds = cycles;
                long minutes = total_seconds/60;
                long seconds_past_the_minute = total_seconds % 60;

                TelephonyManager tMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                String phoneNumber = tMgr.getLine1Number();

                String message = "Your child has been continuously active for ";
                if (minutes > 0) {
                    message += Long.toString(minutes) + "m ";
                }
                message += Long.toString(seconds_past_the_minute) + "s";

                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(phoneNumber, null, message, null, null);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bazooga_main, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
