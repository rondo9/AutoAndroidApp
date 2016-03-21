package son.appo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.net.InetAddress;

import services.HotspotService;

public class RemoteActivity extends AppCompatActivity implements SensorEventListener {
    //Die sind für den Drawer
    private String[] mPlanetTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;


    //Das ist die Remote Control
    private SensorManager sensorManager;
    public int direction;
    private Button geradeaus;
    private Button links;
    private Button rechts;
    private Button switchMode;
    private int switchCount;
    private int port = 33334;
    private InetAddress ip;


    private TextView showip;
    // for the service
    boolean mBound;
    HotspotService hotspotService;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            HotspotService.LocalBinder binder = (HotspotService.LocalBinder) service;
            hotspotService = binder.getService();


            mBound = true;
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
        setContentView(R.layout.activity_remote);
        //Erstmal das Drawer-Zeug
        mPlanetTitles = getResources().getStringArray(R.array.draweritems_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


        //Die Buttons werden belegt
        geradeaus=(Button)findViewById(R.id.button);
        links=(Button)findViewById(R.id.button_left);
        rechts=(Button)findViewById(R.id.button_right);
        switchMode= (Button) findViewById(R.id.button_switch);
        //jetzt wird festgelegt, was bei drücken des switch-Buttons passiert(SensorManager pausiert)
        switchMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (switchCount % 2 == 0) {
                    onPause();
                } else onResume();
                //Zähler zum umschalten
                switchCount++;
                showip= (TextView)findViewById(R.id.button_switch);
                showip.setText(hotspotService.getTest());

            }
        });

        //jetzt wird der SensorManager aktiviert
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);

        //auf die Buttons wird ein clickListener gesetzt, und festgelegt was passiert wenn sie gedrückt werden
        geradeaus.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                direction=1;
                updateButtons();
            }
        });
        links.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                direction=2;
                updateButtons();
            }
        });
        rechts.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                direction = 0;
                updateButtons();
            }
        });
       }
    @Override
    protected void onStart() {
        super.onStart();
        // binding the Service to the activity
        Intent intent = new Intent(this, HotspotService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);


    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
            Log.i("Remote activiy", "unbindService");
        }
    }

        //zeigt zu Testzwecken die IP des verbundenen Geräts


    //Erstmal das Sensor-Zeug
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }

    }


    //Hat nichts zu tun
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

        if(x<=-5){
            direction=0;
            updateButtons();
        }
        if(x>-5 && x<5){
            direction=1;
            updateButtons();

        }
        if(x>=5) {direction=2;
            updateButtons();}



    }
    @Override
    protected void onResume() {
        super.onResume();

        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);

    }



    //updatet die Buttons, die die aktuelle Richtung anzeigen. Der ausgewählte wird grün, die anderen grau
    private void updateButtons(){
        if(mBound){
        if(direction==0){
            geradeaus.setBackgroundColor(Color.LTGRAY);
            rechts.setBackgroundColor(Color.GREEN);
            links.setBackgroundColor(Color.LTGRAY);
            byte direction = 0x33;
            hotspotService.sendDirection(direction);
        }
        if(direction==1){
            geradeaus.setBackgroundColor(Color.GREEN);
            rechts.setBackgroundColor(Color.LTGRAY);
            links.setBackgroundColor(Color.LTGRAY);
            byte direction = 0x32;
            hotspotService.sendDirection(direction);

            //hotspotService.sendRandomnumbers();
        }
        if(direction==2){
            geradeaus.setBackgroundColor(Color.LTGRAY);
            links.setBackgroundColor(Color.GREEN);
            rechts.setBackgroundColor(Color.LTGRAY);
            byte direction = 0x31;
            hotspotService.sendDirection(direction);
        }
    }}







    //Jetzt kommt wieder Drawer-Zeug. Der ClickListener für Elemente im Drawer wird implementiert.

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
    private void selectItem(int position) {
        //hier sollte stehen, was passiert, wenn auf ein Item gedrückt wird.
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
