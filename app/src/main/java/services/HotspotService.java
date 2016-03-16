package services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import wifihotspotutils.ClientScanResult;
import wifihotspotutils.FinishScanListener;
import wifihotspotutils.WifiApManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 *
 * Created by philipp on 01.03.16.
 */
public class HotspotService extends Service {
    // THE IDs for sended and received Messages
    private final byte ID_REMOTE_DIRECTION = 0x15;
    private final byte ID_DATA_REQUEST = 0x19;
    private final byte ID_INSTRUCTIONS = 0x18;
    private final byte ID_SENSORS = 0x0D;
    private final byte ID_CURRENTINSTRUCTION= 0x0F;
    // IDs for requests
    private final byte ID_Request_Sensors =0x02;
    private final byte ID_Request_CurrentInstruction = 0x05;

    // Data for the acitivites
    private byte currentInstruction;
    private double[] currentSensors = new double []{2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5};
    //Data for the service itself
    private boolean hotspotStarted = false;
    private boolean ipSet;
    private String password;
    private String name;
    private InetAddress ip;
    private WifiApManager hotspot;
    private String ipAdress= null;
    private final IBinder mBinder = new LocalBinder();
    private int port =33334;
    private int AppPort= 5000;
    boolean receive = false;
    boolean request = false;
    private final Semaphore send_sem = new Semaphore(1, true);
    private final Semaphore sem_sensor = new Semaphore(1, true);

    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public void onDestroy() {
        Log.i("HotspotService", "destroyed");
        super.onDestroy();
        quitReceive();
    }
    public class LocalBinder extends Binder {
        public HotspotService getService() {
            // Return this instance of Connection so clients can call public methods
            return HotspotService.this;
        }

    }
    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;
    }

    public void startHotspot(){
        if(!hotspotStarted){
        hotspot = new WifiApManager(this);
        hotspot.setWifiApEnabled(null, true);
        hotspot.setWifiApConfiguration(null);
        hotspotStarted=true;}
    }
    public String getIp(){
        if(!ipSet){
        hotspot.getClientList(false, new FinishScanListener() {
            @Override
            public void onFinishScan(final ArrayList<ClientScanResult> clients) {
                for (ClientScanResult clientScanResult : clients) {
                    ipAdress = clientScanResult.getIpAddr();
                    if(ipAdress!=null){
                        ipSet=true;
                    }
                }
            }
        });}
    return ipAdress;}

    public void startReceive(){
        if(receive!=true){
            receive=true;
            Thread connection = new Thread( new Recieving());
            connection.start();
        }
    }
    public void startRequest(){
        if(request!=true){
            request=true;
            Thread requester = new Thread( new DataRequester());
            requester.start();
        }
    }
    public void quitReceive(){
        receive= false;
    }
    public void quitRequest(){
        request= false;
    }
    public byte getCurrentInstruction(){
        return currentInstruction;
    }
    public double[] getSensors(){
        return currentSensors;
    }
    public String getTest(){
        return ""+currentSensors[0]+currentSensors[1]
                +currentSensors[2]+currentSensors[3]+currentSensors[4]+currentSensors[5]+currentSensors[6]+currentSensors[7];
    }
    /**
     * This Method is used in the Remotecontrol to send the directions.
     * @param direction use 0 for left, 50 for straight, 51 for right
     */
    public void sendDirection(byte direction){
        byte message [] = new byte[2];
        message[0]= ID_REMOTE_DIRECTION;
        message[1]= direction;
        Thread send = new Thread(new Sending(port,message));
        send.start();
    }


    /**
     * This method is used in the CommandActivity. It is used to send the array of instructions to the car
     * @param instructions use  0x00 = turn left, 0x01 = go straight, 0x02 = turn right, park = 0x03, stop = 0x04
     */
    public void sendInstructions(byte instructions []){
        byte message [] = addIDwithLength(ID_INSTRUCTIONS, instructions);
        Thread send = new Thread(new Sending(port,message));
        send.start();
    }
    public void sendStop(){
        byte message [] = {0x16};
        Thread send = new Thread(new Sending(port,message));
        send.start();
    }
    private byte [] addID(byte id,byte[]data){
        byte []message = new byte[data.length+1];
        message[0]=id;
        for(int i=0;i<data.length;i++){
            message[i+1]=data[i];
        }
        return message;
    }
    private byte [] addIDwithLength (byte id,byte[]data){
        byte []message = new byte[data.length+2];
        message[0]=id;
        message[1]= (byte) data.length;
        for(int i=0;i<data.length;i++){
            message[i+2]=data[i];
        }
        return message;
    }


 class Sending implements Runnable{
     boolean SendingSocketSet;
     int port;
     byte [] msg;
     DatagramSocket sender;
      Sending(int port, byte[] msg){
         this.port = port;
          this.msg = msg;
          try {
              try {
                  send_sem.acquire();
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
              sender = new DatagramSocket(port);
              Log.i("ConstructorSending", "sucessful");
          } catch (SocketException e) {
              Log.i("ConstructorSending", e.toString());
              e.printStackTrace();
          }
      }
     @Override
     public void run(){
         byte []message = msg;
         // getIp() must be called to set the IpAdress
         if(getIp()!=null){
         try {
             ip = InetAddress.getByName(ipAdress);
         } catch (UnknownHostException e) {
             Log.i("InetAdresss",e.toString());
             e.printStackTrace();
         }
         DatagramPacket packet = new DatagramPacket(message,message.length,ip,port);
         try {
             sender.send(packet);
             Log.i("HotspotService", "package send to "+ip+" port: "+port);
         } catch (IOException e) {
             e.printStackTrace();
             Log.i("HotspotService", e.toString());
         }
     }
         closeSocket();
     }
     public void closeSocket(){
         sender.close();
         send_sem.release();
     }
 }

    class Recieving implements Runnable{
        DatagramSocket receiver;
        public Recieving (){
            super();
            try {
                receiver = new DatagramSocket(AppPort);
                Log.i("UDP Socket","is listening on "+AppPort);
            } catch (SocketException e) {
                e.printStackTrace();
                Log.i("UDP Service",e.toString());
            }
        }

        @Override
        public void run() {
            while(receive){
                byte[] data = new byte[64];
                DatagramPacket packet = new DatagramPacket(data,
                        data.length);
                try {
                    receiver.receive(packet);
                } catch (IOException e) {
                    Log.i("UDP Socket",e.toString());
                    e.printStackTrace();
                }
                byte msg [] = packet.getData();
                decodeData(msg, packet.getLength());
                }
            receiver.close();
            }
        public void decodeData(byte msg [], int length){
            Log.i("decode Data was called ", "Package with length "+length);
            for(int i=0; i<length;i++) {
                if (msg[i] == ID_SENSORS) {
                    byte[] sensors = new byte[8];
                    for(int j=0;j<8;j++){
                        sensors[j]=msg[i+j+1];// copy the sensors data in a new byte array i=current postion in the msg[] array
                        // j is for moving on to get all 8 sensor data in sensors[] and +1 is to skip the ID
                    }
                    receivedSensor(sensors);
                    i = i + 8;// The Sensor Data is 8 byte long so we must skip them to get to next id
                }
                if (msg[i]==ID_CURRENTINSTRUCTION){
                    receivedCurrentInstruction(msg[i+1]);
                    i=i+1;
                }
            }
        }

        public void receivedSensor(byte []sensors){

            for(int i =0;i<=7;i++){
                currentSensors[i]=(double) sensors[i]/100;
            }
        }
        public void receivedCurrentInstruction(byte instruction){
            if (currentInstruction!=instruction){
                currentInstruction=instruction;
            }
        }

        }
        class DataRequester implements Runnable{

            @Override
            public void run() {
                Log.i("HotspotService","Data Requester is running");
                while(request){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    byte message[]= new byte[]{ID_DATA_REQUEST,ID_Request_Sensors};
                    sendDataRequest(message);
                }
                Log.i("HotspotService","Data Requester is stopped request has the value"+ request);
            }
            public void sendDataRequest(byte request []){
                byte message [] = addID(ID_DATA_REQUEST,request);
                Thread send = new Thread(new Sending(port,message));
                send.start();
            }
        }
    public void sendRandomnumbers(){
        Log.i("Random numbers","called");
        Random rand = new Random();
        byte value;
        byte[] sensors = new byte[9];
        sensors[0]= ID_SENSORS;
        for (int i=1;i<sensors.length;i++){
            value= (byte)rand.nextInt(124);
            sensors[i]=value;}
            Thread send = new Thread(new Sending(port,sensors));
            send.start();
        }


}



