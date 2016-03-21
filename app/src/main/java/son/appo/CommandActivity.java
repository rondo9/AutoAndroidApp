package son.appo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import services.HotspotService;

//ToDO hotspotService.sendInstructions[instructions] und instructions = byte Array
//instructions use  0x00 = turn left, 0x01 = go straight, 0x02 = turn right, park = 0x03, stop = 0x04
public class CommandActivity extends AppCompatActivity {

    String[] strings = {"STRAIGHT", "LEFT", "RIGHT", "STOP", "PARK"};

    String[] subs = {"Go straight next", "Turn left next", "Turn right next", "Stop next", "Park vehicle"};

    int arr_images[] = {R.drawable.straight_sign, R.drawable.left_sign, R.drawable.right_sign, R.drawable.stop_sign, R.drawable.park_sign};

    byte[] instruction_bytes = {0x01, 0x00, 0x02, 0x04, 0x03};
    byte[] instructions = null;

    //for the drawer
    private String[] mPlanetTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    //for the Service

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
        setContentView(R.layout.activity_command);

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

        final Button buttonConnect = (Button) findViewById(R.id.commandButton);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getApplication().getBaseContext(), "Connected", Toast.LENGTH_SHORT).show();

                // send instructions to HotspotService
                if (instructions == null) {
                    Toast.makeText(getApplication().getBaseContext(), "Please select commands", Toast.LENGTH_SHORT).show();
                } else {
                    if(mBound)
                    hotspotService.sendInstructions(instructions);

                    Intent myIntent = new Intent(CommandActivity.this, ShowActivity.class);
                    myIntent.putExtra("key", new String("")); //Optional parameters
                    CommandActivity.this.startActivity(myIntent);
                }
            }
        });

        Spinner mySpinner0 = (Spinner) findViewById(R.id.spinner0);
        mySpinner0.setAdapter(new MyAdapter(CommandActivity.this, R.layout.row, strings));

        Spinner mySpinner1 = (Spinner) findViewById(R.id.spinner1);
        mySpinner1.setAdapter(new MyAdapter(CommandActivity.this, R.layout.row, strings));

        Spinner mySpinner2 = (Spinner) findViewById(R.id.spinner2);
        mySpinner2.setAdapter(new MyAdapter(CommandActivity.this, R.layout.row, strings));

        Spinner mySpinner3 = (Spinner) findViewById(R.id.spinner3);
        mySpinner3.setAdapter(new MyAdapter(CommandActivity.this, R.layout.row, strings));

        // build instructions to send to HotspotService
        instructions = new byte[4];
        instructions[0] = instruction_bytes[mySpinner0.getSelectedItemPosition()];
        instructions[1] = instruction_bytes[mySpinner1.getSelectedItemPosition()];
        instructions[2] = instruction_bytes[mySpinner2.getSelectedItemPosition()];
        instructions[3] = instruction_bytes[mySpinner3.getSelectedItemPosition()];

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("CommandActivity", "start Service was called");
        // binding the Service to the activity
        Intent intent = new Intent(this, HotspotService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.i("Command Activity", "unbindService");
    }

    @Override
    protected void onStop() {
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
            Log.i("CommandActivity", "unbindService");
        }

        super.onStop();
    }

    public class MyAdapter extends ArrayAdapter<String> {

        public MyAdapter(Context context, int textViewResourceId, String[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomDropDownView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate(R.layout.row, parent, false);
            TextView label = (TextView) row.findViewById(R.id.direction);
            label.setText(strings[position]);

            TextView sub = (TextView) row.findViewById(R.id.sub);
            sub.setText(subs[position]);

            ImageView icon = (ImageView) row.findViewById(R.id.image);
            icon.setImageResource(arr_images[position]);

            return row;
        }

        public View getCustomDropDownView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate(R.layout.row, parent, false);
            TextView label = (TextView) row.findViewById(R.id.direction);
            label.setText(strings[position]);

            TextView sub = (TextView) row.findViewById(R.id.sub);
            sub.setText(subs[position]);

            ImageView icon = (ImageView) row.findViewById(R.id.image);
            icon.setImageResource(arr_images[position]);

            return row;
        }
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
        if (position == 0) {
            Intent myIntent = new Intent(this, ConnectActivity.class);
            startActivity(myIntent);
        }
        if (position == 1) {
            Intent myIntent = new Intent(this, ShowActivity.class);
            startActivity(myIntent);
        }
        if (position == 2) {
            Intent myIntent = new Intent(this, DiagnosisActivity.class);
            startActivity(myIntent);
        }
        if (position == 3) {
            Intent myIntent = new Intent(this, RemoteActivity.class);
            startActivity(myIntent);
        }
        if (position == 4) {
            Intent myIntent = new Intent(this, CommandActivity.class);
            startActivity(myIntent);
        }

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

}
