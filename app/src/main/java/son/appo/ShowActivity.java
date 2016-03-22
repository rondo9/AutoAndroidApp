package son.appo;

import android.animation.ArgbEvaluator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import java.util.Random;

import services.HotspotService;

public class ShowActivity extends AppCompatActivity {


    private byte[][] picture = new byte[60][80];

    private void randomPicture() {
        Random rand = new Random();

        for (int i = 0; i < 60; i++) {
            for (int j = 0; j < 80; j++) {
                picture[i][j] = (byte) rand.nextInt(127);
            }
        }
    }

    private void makeBitmap() {
        for (int i = 0; i < 60; i++) {
            for (int m = 0; m < 8; m++) {
                for (int j = 0; j < 80; j++) {
                    for (int k = 0; k < 8; k++) {
                        if (bytetoBit(picture[i][j], k)) {
                            buffer.setPixel(8 * j + k, 8 * i + m, Color.WHITE);
                        } else {
                            buffer.setPixel(8 * j + k, 8 * i + m, Color.BLACK);
                        }
                        ;
                    }
                }
            }
        }
    }

    private boolean bytetoBit(byte data, int pos) {
        int mask = 1 << pos;

        return (data & mask) == mask;
    }


    private int rand;
    private ImageView video;
    Bitmap bm;
    Bitmap buffer = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);

    String[] strings = {"STRAIGHT", "LEFT", "RIGHT", "STOP", "PARK"};

    String[] subs = {"Go straight next", "Turn left next", "Turn right next", "Stop next", "Park vehicle"};

    int arr_images[] = {R.drawable.straight_sign, R.drawable.left_sign, R.drawable.right_sign, R.drawable.stop_sign, R.drawable.park_sign};

    // For displaying sensor data with color varying from RED (near) to GREEN (far)
    private final long refreshMiliSec = 500;
    private final double MINDISTANCE = 0;
    private final double MAXDISTANCE = 1.27;
    private final int SAFECOLOR = Color.GREEN;
    private final int NEARCOLOR = Color.RED;
    private final int[] oval_diags = {R.drawable.oval_diag_0, R.drawable.oval_diag_1, R.drawable.oval_diag_2, R.drawable.oval_diag_3, R.drawable.oval_diag_4,
            R.drawable.oval_diag_5, R.drawable.oval_diag_6, R.drawable.oval_diag_7, R.drawable.oval_diag_8, R.drawable.oval_diag_9};
    private final int[] oval_vers = {R.drawable.oval_ver_0, R.drawable.oval_ver_1, R.drawable.oval_ver_2, R.drawable.oval_ver_3, R.drawable.oval_ver_4,
            R.drawable.oval_ver_5, R.drawable.oval_ver_6, R.drawable.oval_ver_7, R.drawable.oval_ver_8, R.drawable.oval_ver_9};
    private final int[] oval_hors = {R.drawable.oval_hor_0, R.drawable.oval_hor_1, R.drawable.oval_hor_2, R.drawable.oval_hor_3, R.drawable.oval_hor_4,
            R.drawable.oval_hor_5, R.drawable.oval_hor_6, R.drawable.oval_hor_7, R.drawable.oval_hor_8, R.drawable.oval_hor_9};
    private double[] sensors;
    private TextView[] textViews;
    private TextView[] areas; // background of textViews
    private int[] saveColors;
    private int savMinPos;
    private Handler myRepeatHandler;

    // For the drawer
    private String[] mPlanetTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    // For the service
    boolean mBound;
    HotspotService hotspotService;
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
                        Thread.sleep(refreshMiliSec);
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

    // Initialize sensors
    private void initStats() {
        sensors = new double[8];
        textViews = new TextView[8];
        areas = new TextView[8];
        saveColors = new int[8];
        savMinPos = 0;
        Random r = new Random();

        for (int i = 0; i < sensors.length; i++) {
            sensors[i] = MAXDISTANCE;
            saveColors[i] = SAFECOLOR;
        }

        textViews[0] = ((TextView) findViewById(R.id.tvTopLeft));
        textViews[1] = ((TextView) findViewById(R.id.tvTopMid));
        textViews[2] = ((TextView) findViewById(R.id.tvTopRight));
        textViews[3] = ((TextView) findViewById(R.id.tvMidLeft));
        textViews[4] = ((TextView) findViewById(R.id.tvMidRight));
        textViews[5] = ((TextView) findViewById(R.id.tvBotLeft));
        textViews[6] = ((TextView) findViewById(R.id.tvBotMid));
        textViews[7] = ((TextView) findViewById(R.id.tvBotRight));

        areas[0] = ((TextView) findViewById(R.id.tvTopLeftBack));
        areas[1] = ((TextView) findViewById(R.id.tvTopMidBack));
        areas[2] = ((TextView) findViewById(R.id.tvTopRightBack));
        areas[3] = ((TextView) findViewById(R.id.tvLeftBack));
        areas[4] = ((TextView) findViewById(R.id.tvRightBack));
        areas[5] = ((TextView) findViewById(R.id.tvBotLeftBack));
        areas[6] = ((TextView) findViewById(R.id.tvBotMidBack));
        areas[7] = ((TextView) findViewById(R.id.tvBotRightBack));

        /*
        // set transparency
        areas[0].setAlpha((float) 0.5);
        areas[2].setAlpha((float) 0.5);
        areas[5].setAlpha((float) 0.5);
        areas[7].setAlpha((float) 0.5);
        */
    }

    // Update sensor data
    private void updateInfos() {
        if (mBound) {
            sensors[0] = hotspotService.getSensorTopLeft();
            sensors[1] = hotspotService.getSensorTopMiddle();
            sensors[2] = hotspotService.getSensorTopRight();
            sensors[3] = hotspotService.getSensorMiddleLeft();
            sensors[4] = hotspotService.getSensorMiddleRight();
            sensors[5] = hotspotService.getSensorBottomLeft();
            sensors[6] = hotspotService.getSensorBottomMiddle();
            sensors[7] = hotspotService.getSensorBottomRight();
        }

        // Array oval_diags (for sensor top left, top right, bot left, bot right) contains 10 levels of colors varying from GREEN (index 0) to RED (index 9)
        // Array oval_vers (for sensor top mid, bot mid) contains 10 levels of colors varying from GREEN (index 0) to RED (index 9)
        // Array oval_hors (for sensor left mid, right mid) contains 10 levels of colors varying from GREEN (index 0) to RED (index 9)
        areas[0].setBackgroundResource(oval_diags[(int) (9.99 * (1.0 - (sensors[0] - MINDISTANCE) / (MAXDISTANCE - MINDISTANCE)))]);
        areas[1].setBackgroundResource(oval_vers[(int) (9.99 * (1.0 - (sensors[1] - MINDISTANCE) / (MAXDISTANCE - MINDISTANCE)))]);
        areas[2].setBackgroundResource(oval_diags[(int) (9.99 * (1.0 - (sensors[2] - MINDISTANCE) / (MAXDISTANCE - MINDISTANCE)))]);
        areas[3].setBackgroundResource(oval_hors[(int) (9.99 * (1.0 - (sensors[3] - MINDISTANCE) / (MAXDISTANCE - MINDISTANCE)))]);
        areas[4].setBackgroundResource(oval_hors[(int) (9.99 * (1.0 - (sensors[4] - MINDISTANCE) / (MAXDISTANCE - MINDISTANCE)))]);
        areas[5].setBackgroundResource(oval_diags[(int) (9.99 * (1.0 - (sensors[5] - MINDISTANCE) / (MAXDISTANCE - MINDISTANCE)))]);
        areas[6].setBackgroundResource(oval_vers[(int) (9.99 * (1.0 - (sensors[6] - MINDISTANCE) / (MAXDISTANCE - MINDISTANCE)))]);
        areas[7].setBackgroundResource(oval_diags[(int) (9.99 * (1.0 - (sensors[7] - MINDISTANCE) / (MAXDISTANCE - MINDISTANCE)))]);

        double minD = sensors[0]; // find minimum distance to underline
        savMinPos = 0;
        for (int i = 0; i < sensors.length; i++) {
            textViews[i].setText(Math.round(sensors[i] * 100) / 100. + ""); // rounding to 2 comma-digits eg. 1.27
            if (sensors[i] < minD) {
                minD = sensors[i];
                savMinPos = i;
            }
            /*
            int newColor = getScalingColor(Double.parseDouble((textViews[i]).getText() + "")); // normalized color between GREEN and RED
            // Used to fade color smoothly between old and new color
            ObjectAnimator colorFade = ObjectAnimator.ofObject(
                    textViews[i], "backgroundColor",
                    new ArgbEvaluator(),
                    saveColors[i],
                    newColor);
            colorFade.setDuration(refreshMiliSec);
            colorFade.start();
            textViews[i].setBackgroundColor(newColor);
            saveColors[i] = newColor;
            */
        }


        // Underline the currently minimum distance
        SpannableString content = new SpannableString(Math.round(sensors[savMinPos] * 100) / 100. + "");
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        textViews[savMinPos].setText(content);

        /* Show current command
        0x00 = turn left
        0x01 = go straight
        0x02 = turn right
        0x03 = park
        0x04 = stop
        strings = {"STRAIGHT", "LEFT", "RIGHT", "STOP", "PARK"};
        */
        if (mBound) {
            Spinner sp = (Spinner) findViewById(R.id.spinner);
            switch (hotspotService.getCurrentInstruction()) {
                case 0x00:
                    sp.setSelection(1);
                    break;
                case 0x01:
                    sp.setSelection(0);
                    break;
                case 0x02:
                    sp.setSelection(2);
                    break;
                case 0x03:
                    sp.setSelection(4);
                    break;
                case 0x04:
                    sp.setSelection(3);
                    break;
            }
        }
    }

    // Normalize color to two ends : GREEN (far) and RED (near)
    private int getScalingColor(double value) {
        /*
        eg:
        value     color
        MIN         RED = -65536 (0xffff0000)
        MAX         GREEN = -16711936 (0xff00ff00)


        --> color = (value-MIN)*(GREEN-RED)/(MAX-MIN) + RED
        */

        //return (int) ((value - MINDISTANCE) * (Color.GREEN - Color.RED) / (MAXDISTANCE - MINDISTANCE) + Color.RED);

        return (int) (new ArgbEvaluator()).evaluate((float) ((value - MINDISTANCE) / (MAXDISTANCE - MINDISTANCE)), NEARCOLOR, SAFECOLOR);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("ShowActivity", "OnCreate is called");

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

        //findViewById(R.id.tvMid).setBackgroundResource(R.drawable.auto_img);

        Spinner mySpinner = (Spinner) findViewById(R.id.spinner);
        mySpinner.setAdapter(new MyAdapter(ShowActivity.this, R.layout.row, strings));

        final Button buttonInfo = (Button) findViewById(R.id.buttonInfo);
        buttonInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplication().getBaseContext(), "Diagnosis selected", Toast.LENGTH_SHORT).show();
                Intent myIntent = new Intent(ShowActivity.this, DiagnosisActivity.class);
                myIntent.putExtra("key", new String("")); //Optional parameters
                ShowActivity.this.startActivity(myIntent);
            }
        });

        // Update sensor data
        myRepeatHandler = new Handler();
        initStats();
        startRepeatRandom();


        bm = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
        //bm.eraseColor(Color.BLUE);
        video = (ImageView) findViewById(R.id.videoView);
        video.setImageBitmap(buffer);

        //thread for periodically updating the video player:

        final Handler handler = new Handler();
        Thread videoplayer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                video.setImageBitmap(buffer);
                            }
                        }, 400);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        Thread videoupdater = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(0);
                        randomPicture();
                        makeBitmap();
                        //bm=buffer;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        videoupdater.start();

        //videoplayer.start();
        //makebmBlack();

    }

    // These following 3 functions repeat/stop the process of updating sensors
    Runnable myStatus = new Runnable() {
        @Override
        public void run() {
            updateInfos();
            //new MyAsyncTask().execute();
            myRepeatHandler.postDelayed(myStatus, refreshMiliSec);
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
        Log.i("ShowActivity", "start Service was called");
        // binding the Service to the activity
        Intent intent = new Intent(this, HotspotService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        if (mBound) {
            // start the to receive and request data form the car
            hotspotService.startRequest();
            hotspotService.startReceive();
            Log.i("ShowActivity", "bindService");
        } else {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("ShowActivity", "onResume is called");
        if (mBound) {
            // start the to receive and request data form the car
            hotspotService.startRequest();
            hotspotService.startReceive();
            Log.i("ShowActivity", "bindService");
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
            Log.i("ShowActivity", "unbindService");
        }

        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reset:
                Toast.makeText(this, "Reset selected", Toast.LENGTH_SHORT).show();
                finish();
                /*
                Intent myIntent = new Intent(ShowActivity.this, ConnectActivity.class);
                myIntent.putExtra("key", new String("")); //Optional parameters
                ShowActivity.this.startActivity(myIntent);
                */
                break;
            case R.id.menu_setting:
                Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT).show();
                Intent myIntent = new Intent(ShowActivity.this, SettingsActivity.class);
                myIntent.putExtra("key", new String("")); //Optional parameters
                ShowActivity.this.startActivity(myIntent);
                break;
        }
        return true;
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
            //TextView label=(TextView)row.findViewById(R.id.company);
            //label.setText(strings[position]);

            //TextView sub=(TextView)row.findViewById(R.id.sub);
            //sub.setText(subs[position]);

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


    //für Testzwecke, ob setPixel alle Pixel erreicht
    private void makebmBlack() {
        for (int i = 0; i < 60; i++) {
            for (int m = 0; m < 8; m++) {
                for (int j = 0; j < 80; j++) {
                    for (int k = 0; k < 8; k++) {
                        bm.setPixel(8 * j + k, 8 * i + m, Color.BLUE);
                    }
                }
            }
        }
    }
}
