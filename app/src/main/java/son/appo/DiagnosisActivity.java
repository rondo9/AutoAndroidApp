package son.appo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import services.HotspotService;

/*
Sensor data can be invoked by calling eg. hotspotService.getSensorBottomLeft() for the sensor bottom left

hotspoService.getCurrenInstruction() returns
    0x00 = turn left
    0x01 = go straight
    0x02 = turn right
    0x03 = park
    0x04 = stop
*/

public class DiagnosisActivity extends AppCompatActivity {

    // for the drawer
    private String[] mPlanetTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    boolean mBound; // shows if the service is bound to a activity
    HotspotService hotspotService; // this handles the connection

    private Handler myRepeatHandler; // used to repeat after a delay

    /**
     * Does the actual binding of the Service
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            HotspotService.LocalBinder binder = (HotspotService.LocalBinder) service;
            hotspotService = binder.getService();
            // This Thread is necessary for a short break. You need the break to prevent that this activity
            // starts a Receiving or Requesting while another activity is calling stopReceiving because onStart ans Onstop
            // is always called new activity calls onStart then old calls OnStop
            Thread x = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    hotspotService.startReceive();
                    hotspotService.startRequest();
                }
            });
            x.start();
            mBound = true;
            Log.i("ShowActivity", "OnServiceConnected");

        }
        @Override
        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnosis);

        // The drawer stuff
        mPlanetTitles = getResources().getStringArray(R.array.draweritems_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        // Finished with drawer stuff

        // Update sensor data repeatedly
        myRepeatHandler = new Handler();
        updateSensors();
        startRepeatRandom();
    }

    // Update function for sensors, this will be called repeatedly EVERY 1000ms
    private void updateSensors(){
        if(mBound){
        TextView valTopLeft = (TextView) findViewById(R.id.valTopLeft);
        valTopLeft.setText(hotspotService.getSensorTopLeft() + "");
        TextView valTopMid = (TextView) findViewById(R.id.valTopMid);
        valTopMid.setText(hotspotService.getSensorTopMiddle() + "");
        TextView valTopRight = (TextView) findViewById(R.id.valTopRight);
        valTopRight.setText(hotspotService.getSensorTopRight() + "");
        TextView valMidLeft = (TextView) findViewById(R.id.valMidLeft);
        valMidLeft.setText(hotspotService.getSensorMiddleLeft() + "");
        TextView valMidRight = (TextView) findViewById(R.id.valMidRight);
        valMidRight.setText(hotspotService.getSensorMiddleRight() + "");
        TextView valBotLeft = (TextView) findViewById(R.id.valBotLeft);
        valBotLeft.setText(hotspotService.getSensorBottomLeft() + "");
        TextView valBotMid = (TextView) findViewById(R.id.valBotMid);
        valBotMid.setText(hotspotService.getSensorBottomMiddle() + "");
        TextView valBotRight = (TextView) findViewById(R.id.valBotRight);
        valBotRight.setText(hotspotService.getSensorBottomRight() + "");}
    }

    // These following 3 functions repeat/stop the process of updating sensors
    Runnable myStatus = new Runnable() {
        @Override
        public void run() {
            updateSensors();
            //new MyAsyncTask().execute();
            myRepeatHandler.postDelayed(myStatus, 1000);
        }
    };
    void startRepeatRandom() {
        myStatus.run();
    }
    void stopRepeatingTask() {
        myRepeatHandler.removeCallbacks(myStatus);
    }
    // end of repeating functions

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("DiagnosisActivity", "start Service was called");
        // Binding the Service to the activity
        Intent intent = new Intent(this, HotspotService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        if(mBound){
            // Start the to receive and request data form the car
            hotspotService.startRequest();
            hotspotService.startReceive();
            Log.i("DiagnosisActivity\"", "bindService");
        }
    }

    @Override
    protected void onStop() {
        // Unbind from the service
        if (mBound) {
            hotspotService.quitReceive();
            hotspotService.quitRequest();
            unbindService(mConnection);
            mBound = false;
            Log.i("DiagnosisActivity", "unbindService");
        }

        super.onStop();
    }


    // OnClick Listener for the drawer
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
    private void selectItem(int position) {
        // What happens when item is selected
        if(position==0){
            Intent myIntent= new Intent(this, ConnectActivity.class);
            startActivity(myIntent);}
        if(position==1){
            Intent myIntent= new Intent(this, ShowActivity.class);
            startActivity(myIntent);}
        if(position==2){
            Intent myIntent= new Intent(this, DiagnosisActivity.class);
            startActivity(myIntent);}
        if(position==3){
            Intent myIntent= new Intent(this, RemoteActivity.class);
            startActivity(myIntent);}
        if(position==4){
            Intent myIntent= new Intent(this, CommandActivity.class);
            startActivity(myIntent);}

        // Update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }
}
