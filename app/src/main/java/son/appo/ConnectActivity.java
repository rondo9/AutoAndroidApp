package son.appo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import services.HotspotService;

public class ConnectActivity extends AppCompatActivity {
    final String defaultHotspotName = "android.hotspot.default.name";
    final String defaultHotspotPW = "default.password";
    final int defaultHotspotP1 = 8080;
    final int defaultHotspotP2 = 8080;
    boolean mBound;
    HotspotService hotspotService;

    //for the drawer
    private String[] mPlanetTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Intent intent = new Intent(this, HotspotService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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


        ((EditText) findViewById(R.id.etHotspotName)).setText(defaultHotspotName);
        ((EditText) findViewById(R.id.etHotspotPW)).setText(defaultHotspotPW);
        ((EditText) findViewById(R.id.etHotspotP1)).setText(defaultHotspotP1 + "");
        ((EditText) findViewById(R.id.etHotspotP2)).setText(defaultHotspotP2 + "");

        final Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
        buttonConnect.setText("CONNECT");
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonConnect.setText("CONNECT");
                Toast.makeText(getApplication().getBaseContext(), "Connected", Toast.LENGTH_SHORT).show();
                Intent myIntent = new Intent(ConnectActivity.this, CommandActivity.class);
                myIntent.putExtra("key", new String("")); //Optional parameters
                hotspotService.startHotspot();
                ConnectActivity.this.startActivity(myIntent);
            }
        });

        final Button buttonDefault = (Button) findViewById(R.id.buttonDefault);
        buttonDefault.setText("DEFAULT");
        buttonDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonDefault.setText("DEFAULT");
                ((EditText) findViewById(R.id.etHotspotName)).setText(defaultHotspotName);
                ((EditText) findViewById(R.id.etHotspotPW)).setText(defaultHotspotPW);
                ((EditText) findViewById(R.id.etHotspotP1)).setText(defaultHotspotP1+"");
                ((EditText) findViewById(R.id.etHotspotP2)).setText(defaultHotspotP2+"");
                //Toast.makeText(getApplication().getBaseContext(), "Reset done", Toast.LENGTH_SHORT).show();
            }
        });
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
}
