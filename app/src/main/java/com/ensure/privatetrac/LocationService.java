package com.ensure.privatetrac;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class LocationService extends Service {
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = "com.ensure.sixfeet" + ".started_from_notification";
    private static final int NOTI_ID = 1223;
    private static final String CHANNEL_ID = "my_channel";
    private static final long UPDATE_INTERVAL_IN_MIL = 100000;
    private FusedLocationProviderClient client;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private final IBinder mBinder = new LocalBinder();
    private boolean mChangingConfiguration = false;
    private Handler mServiceHandler;
    private Location mLocation;
    private NotificationManager mNotificantionManager;
    private DataArray dataArr = new DataArray();
    private DataArray confirmedArr = new DataArray();
    private DataArray surfaceArr = new DataArray();
    private GroupService myGroup = null;

    public LocationService(){}

    public class LocalBinder extends Binder {
        LocationService getService() {return LocationService.this;}
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent){
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent){
        if(!mChangingConfiguration && Common.requestingLocationUpdates(this))
            startForeground(NOTI_ID, getNotification());
        return true;
    }

    @Override
    public void onCreate(){
        super.onCreate();

        client = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mLocation = locationResult.getLastLocation();
                String prefix = "0";
                double d_latitude = mLocation.getLatitude();
                double d_longitude = mLocation.getLongitude();
                if (myGroup.getSelectedGroup().inRange(d_latitude, d_longitude, new Date())){
                    prefix = Character.toString(myGroup.getSelectedGroupChar());
                }
                int latitude = (int) (10000. * (d_latitude + 191.));
                int longitude = (int) (10000. * (d_longitude + 281.));
                int timeHundredSec = (int) (System.currentTimeMillis() / 100000 + 23579);
                EventBus.getDefault().postSticky(new SendLocationToActivity(mLocation));
                mNotificantionManager.notify(NOTI_ID, getNotification());
                int todayDate = Common.getDayOfWeek();
                if (todayDate != dataArr.getDay()) clearLocationArray();
                String hashedText = todayDate + hashCombine(timeHundredSec, latitude, longitude);
                confirmedArr.sort(prefix+hashedText);
                saveArray(confirmedArr.translate(),Common.CONFIRMEDLITERAL + todayDate + ".txt");
                dataArr.sort(hashedText);
                saveArray(dataArr.translate(),Common.CONCERNEDLITERAL + todayDate + ".txt");
                for (int i = 1; i < 10; i++) {
                    surfaceArr.sort(todayDate + hashCombine(timeHundredSec - i, latitude, longitude));
                }
                saveArray(surfaceArr.translate(),Common.SURFACELITERAL + todayDate + ".txt");
            }
        };
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MIL);
        locationRequest.setFastestInterval(UPDATE_INTERVAL_IN_MIL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        HandlerThread handlerThread = new HandlerThread("My log");   //WHAT IS THIS FOR WHY IS IT CALLED MY LOG?
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificantionManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O){
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "sixFeet", NotificationManager.IMPORTANCE_DEFAULT);
            mNotificantionManager.createNotificationChannel(mChannel);
        }
        int today = Common.getDayOfWeek();
        loadArray(Common.CONCERNEDLITERAL+today+".txt", dataArr);
        loadArray(Common.CONFIRMEDLITERAL+today+".txt", confirmedArr);
        loadArray(Common.SURFACELITERAL+today+".txt", surfaceArr);
        //Log.e("My log", "|||" + hash256Print("ree") + "|||");
        //Log.e("My log", "Normal: " + hash256("ree"));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false)){
            removeLocationUpdates();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
        Common.setRequestingLocationUpdates(this, false);
        mServiceHandler.removeCallbacks(null);
        super.onDestroy();
    }

    /**
     * Build notification.
     * @return  notification
     */
    private Notification getNotification(){
        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .addAction(R.drawable.ic_launch_black_24dp, "Launch", activityPendingIntent)
                .addAction(R.drawable.ic_cancel_black_24dp, "Remove", servicePendingIntent)
                .setContentTitle("Location updated at " + new Date())
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.notification)
                .setWhen(System.currentTimeMillis());
        return builder.build();
    }

    /**
     * Start location service: load today's data into memory, set request status to true
     */
    public void requestLocatonUpdates(GroupService inputGroup){
        myGroup = inputGroup;
        clearLocationArray();
        int today = Common.getDayOfWeek();
        loadArray(Common.CONCERNEDLITERAL+today+".txt", dataArr);
        loadArray(Common.SURFACELITERAL+today+".txt", surfaceArr);
        loadArray(Common.CONFIRMEDLITERAL+today+".txt", confirmedArr);
        startService(new Intent(getApplicationContext(), LocationService.class));
        try{
            client.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            Common.setRequestingLocationUpdates(this, true);
        }catch (SecurityException e){
            //Log.e("My log", "Lost location permission.  Could not request" + e);    IS THERE ANYTHING ELSE WE SHOULD DO IN THIS CATCH
        }
    }

    /**
     * Stop location service: set request status to false; cancel notification,
     *  clear memory, stop service.
     */
    public void removeLocationUpdates(){
        clearLocationArray();
        try{
            client.removeLocationUpdates(locationCallback);
            Common.setRequestingLocationUpdates(this, false);
            mNotificantionManager.cancel(NOTI_ID);
            stopSelf();
        }catch(SecurityException e){
            //Log.e("My Log", "Lost Location Permission. Could not remove updates. " +e); IS THERE ANYTHING ELSE WE SHOULD DO IN THIS CATCH
        }
    }

    /**
     * Combine time, latitude, and longitude information into one string
     * @param time  epoch time in 100 seconds scale
     * @param lati  latitude mutltiplied by 1000
     * @param longi longitude multiplied by 1000
     * @return      combined string
     */
    private String hashCombine(int time, int lati, int longi){
        int lastDigLati = (lati%10)/2;
        int lastDigLongi = (longi%10)/2;
        lati = (lati/10)*10 + lastDigLati;
        longi = (longi/10)*10 + lastDigLongi;
        return hash256Print(""+lati+longi+lati%100000+longi%100000+lati%10000+
                longi%10000+time+lati%1000+longi%1000+lati%100+longi%100+lastDigLati+lastDigLongi);
    }

    /**
     * Use SHA-256 to encode a string into another string of 64 hex characters.
     * @param input the string to be encoded
     * @return      encoded string
     */
    public static String hash256(String input){
        MessageDigest md = null;
        try{ md = MessageDigest.getInstance("SHA-256");}
        catch(NoSuchAlgorithmException e){e.printStackTrace();}
        BigInteger number = new BigInteger(1,md.digest(input.getBytes(StandardCharsets.UTF_8)));
        StringBuilder hexString = new StringBuilder(number.toString(16));
        while(hexString.length()<32) hexString.insert(0,'0');
        return hexString.toString();
    }

    public static String hash256Print(String input){
        String printAble = "";
        MessageDigest md = null;
        try{ md = MessageDigest.getInstance("SHA-256");}
        catch(NoSuchAlgorithmException e){e.printStackTrace();}
        byte[] byteHash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < byteHash.length/3; i++){
            byte rByte = byteHash[3*i];
            int wByte = rByte >> 2;
            wByte = wByte & 0x3f;
            printAble += (char)(wByte+48);
            //wByte = ((rByte & 0x3) << 6) >> 2;
            wByte = (rByte << 4) & 0x30;
            rByte = byteHash[3*i+1];
            wByte +=(rByte >> 4) & 0x0f;
            //wByte = wByte & 0x3f;
            printAble += (char)(wByte+48);
            //wByte = (((rByte & 0xf) << 4) >> 2);
            wByte = (rByte << 2) & 0x3c;
            rByte = byteHash[3*i+2];
            wByte += (rByte >> 6) & 0x3;
            //wByte = wByte & 0x3f;
            printAble += (char)(wByte+48);
            wByte = rByte & 0x3f;
            printAble += (char)(wByte+48);
        }
        byte rByte = byteHash[byteHash.length-2];
        int wByte = rByte >> 2;
        wByte = wByte & 0x3f;
        printAble += (char)(wByte+48);
        //wByte = ((rByte & 0x3) << 6) >> 2;
        wByte = (rByte << 4) & 0x30;
        rByte = byteHash[byteHash.length-1];
        wByte += (rByte >> 4) & 0xf;
        //wByte = wByte & 0x3f;
        printAble += (char)(wByte+48);
        wByte = rByte & 0xf;
        //wByte = wByte & 0x3f;
        printAble += (char)(wByte+48);
        return printAble;
    }

    /**
     * Write date and a string into a file.
     * @param text      context to be written
     * @param toFileName  name of the file to write to
     */
    private void saveArray(String text, String toFileName){
        FileOutputStream fos = null;
        try{
            fos = openFileOutput(toFileName, MODE_PRIVATE);
            String writeText = Common.getFormatteDate()+"\n"+text;
            fos.write(writeText.getBytes());
        }catch(FileNotFoundException e){ e.printStackTrace();
        }catch(IOException e){ e.printStackTrace();
        } finally{
            if(fos!=null){try{fos.close();} catch(IOException e){e.printStackTrace();}}
        }
    }

    /**
     * Load data from file into array only when the date matched first.
     * @param fromFile  the file to load from
     * @param toArray   the array to store the data
     */
    private void loadArray(String fromFile, DataArray toArray){
        FileInputStream fis = null;
        try{
            fis=openFileInput(fromFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String text = br.readLine();
            if(text!= null && text.equals(Common.getFormatteDate())) {
                while((text = br.readLine()) != null)toArray.sort(text);
            }
        }catch(FileNotFoundException e){e.printStackTrace();
        }catch(IOException e) { e.printStackTrace();
        }finally{if(fis!=null){try{fis.close();} catch(IOException e){e.printStackTrace();}} }
    }

    /**
     * Delete all data held in memory.
     */
    public void clearLocationArray(){
        dataArr.clear();
        confirmedArr.clear();
        surfaceArr.clear();
    }
}