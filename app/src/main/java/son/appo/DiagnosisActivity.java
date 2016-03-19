package son.appo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import services.HotspotService;

// Son du kannst jetzt die einzelen Sensoren mit getter Methoden holen
//Das geht einfach durch hotspotService.getSensorBottomLeft(); für den Sensor unten Links
// hotspoService.getCurrenInstruction funktioniert jetzt 0x00 = turn left, 0x01 = go straight, 0x02 = turn right, park = 0x03, stop = 0x04
// aktuell bekommst du immer 2.5 für jeden Sensor aber wenn die App zu meiner VirtualCAr verbunden ist kommen andere Werte
public class DiagnosisActivity extends AppCompatActivity {

    //for the drawer
    private String[] mPlanetTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    boolean mBound;//shows if the service is bound to a activity
    HotspotService hotspotService;// thhis Handels the connection
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

        //here comes the drawer stuff
        mPlanetTitles = getResources().getStringArray(R.array.draweritems_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        //finished with drawer stuff

    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.i("DiagnosisActivity", "start Service was called");
        // binding the Service to the activity
        Intent intent = new Intent(this, HotspotService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        if(mBound){
            // start the to receive and request data form the car
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


    //onclicklistener for the drawer

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
    private void selectItem(int position) {
        //what happens when item is selected
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

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }
}
