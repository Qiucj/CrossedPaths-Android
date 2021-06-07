package com.ensure.privatetrac;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, SendDialog.SendDialogListener, CancelDialog.CancelDialogListener, AboutDialog.AboutDialogListener {
    public static final String SEND_FILE_NAME = "send.txt";
    private LocationService mSerivce = null;
    private boolean mBound;
    private int numOverlapped = 0;
    private int numDaysChecked = 0;
    private String messageFromServer = "";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder)iBinder;
            mSerivce = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSerivce = null;
            mBound = false;
            Common.setRequestingLocationUpdates(MainActivity.this,false);
        }
    };

    Button requestLocation, removeLocation, checkDataBase;
    TextView alertView;
    Dialog infoDialog, infoDialog2, infoDialog3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alertView = (TextView)findViewById(R.id.warning);

        View dialogView = getLayoutInflater().inflate(R.layout.infopage, null);
        infoDialog = new Dialog(this,android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        infoDialog.setContentView(dialogView);

        View dialogView2 = getLayoutInflater().inflate(R.layout.infopage2, null);
        infoDialog2 = new Dialog(this,android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        infoDialog2.setContentView(dialogView2);

        View dialogView3 = getLayoutInflater().inflate(R.layout.infopage3, null);
        //dialogView3.setMovementMethod(new ScrollingMovementMethod());
        infoDialog3 = new Dialog(this,android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        infoDialog3.setContentView(dialogView3);

        Dexter.withContext(this)
                .withPermissions(Arrays.asList(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET
                ))
                .withListener(new MultiplePermissionsListener(){
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        requestLocation = (Button) findViewById(R.id.update);
                        removeLocation = (Button) findViewById(R.id.removeUpdates);
                        checkDataBase = (Button) findViewById(R.id.check);
                        requestLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mSerivce.requestLocatonUpdates();
                            }
                        });
                        removeLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mSerivce.removeLocationUpdates();
                            }
                        });
                        checkDataBase.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                downloadDataAndCheck();
                            }
                        });
                        setButtonState(Common.requestingLocationUpdates(MainActivity.this));
                        bindService(new Intent(MainActivity.this, LocationService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token){}
                }).check();
    }

    @Override
    protected void onStart(){
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop(){
        if(mBound){
            unbindService(mServiceConnection);
            mBound = false;
        }
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void deleteClicked(){
        for (int i = 1; i <=7; i++) {
            saveData("", Common.CONFIRMEDLITERAL+i+".txt");
            saveData("", Common.CONCERNEDLITERAL+i+".txt");
        }
        if(mSerivce != null)mSerivce.clearLocationArray();
    }

    @Override
    public void displayInfo(int userChoice){
        if (userChoice == 0) infoDialog.show();
        if (userChoice == 1) infoDialog3.show();
        else if (userChoice == 2){
            //infoDialog.dismiss();
            infoDialog2.show();
        }
    }

    /**
     * Clear all saved data, including files and memory.
     * @param view  default view
     */
    public void clearHistroy(View view) {
        CancelDialog dialog = new CancelDialog();
        dialog.show(getSupportFragmentManager(), "Cancel History Dialog");
    }

    @Override
    public void sendConfirmedClicked() {
        String sendText = "";
        for (int i = 1; i<=7; i++){
            sendText += readTextFile(Common.CONFIRMEDLITERAL+i+".txt");
        }
        saveData(sendText, SEND_FILE_NAME);
        File newFile = new File(this.getFilesDir(), SEND_FILE_NAME);
        Uri contentUri = FileProvider.getUriForFile(this, "com.ensure.PrivateTrac", newFile);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Confirmed");
        sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"sixfeetapp2020@gmail.com"});
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent,null);
        startActivity(shareIntent);
    }

    public void emailGovernor(View view){
        infoDialog2.dismiss();
        Intent emailIntent = new Intent();
        emailIntent.setAction(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Digital Contact Tracing in Michigan");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"governorsoffice@michigan.gov"});
        emailIntent.putExtra(Intent.EXTRA_TEXT,"Dear Governor Whitmer,\n\nI live in Michigan and would like to see our state adopt digital contact tracing to fight Covid-19. I have tried the contact tracing app, PrivateTrac, and am very satisfied with its overall structure and data privacy model. I am writing to urge you to establish a digital contact tracing app for Michigan by encouraging the public health department to work with app developers like PrivateTrac. \n\nThank you. \n");
        emailIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(emailIntent, null);
        startActivity(shareIntent);
    }

    public void sendData(View view){
        SendDialog dialog = new SendDialog();
        dialog.show(getSupportFragmentManager(), "Send Data Dialog");
    }

    public void contactUs(View view) {
        Intent emailIntent = new Intent();
        emailIntent.setAction(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Contact Us");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"sixfeetapp2020@gmail.com"});
        emailIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(emailIntent, null);
        startActivity(shareIntent);
    }

    public void displayInfo(View view){
        AboutDialog dialog = new AboutDialog();
        dialog.show(getSupportFragmentManager(), "About Dialog");
    }

    public void dismissInfo(View view){
        infoDialog.dismiss();
    }

    public void dismissInfo2(View view){
        infoDialog2.dismiss();
    }

    public void dismissInfo3(View view) { infoDialog3.dismiss(); }

    /**
     * Save a string to a file
     * @param text      string to be saved
     * @param fileName  name of the file
     */
    public void saveData(String text, String fileName){
        FileOutputStream fos = null;
        try{
            fos = openFileOutput(fileName, MODE_PRIVATE);
            fos.write(text.getBytes());
        }catch(FileNotFoundException e){ e.printStackTrace();
        }catch(IOException e){ e.printStackTrace();
        }finally{if(fos!=null){try {fos.close();}catch(IOException e){e.printStackTrace();}}}
    }

    /**
     * Read a text file into a string, each line is marked with \n symbol
     * @param fileName  Name for the file
     * @return          String of the context in the file.  Empty if file does not exist
     */
    private String readTextFile(String fileName){
        FileInputStream fis = null;
        try{
            fis = openFileInput(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String text;
            while((text=br.readLine()) != null)sb.append(text).append("\n");
            return sb.toString();
        }catch (FileNotFoundException e){ e.printStackTrace();
        }catch (IOException e){ e.printStackTrace();
        }finally{if(fis!=null){ try{ fis.close();}catch (IOException e){ e.printStackTrace();}}}
        return "";
    }

    /**
     * Either the "request location" or "remove location" button is enabled at any time
     * @param isRequestEnabled  whether the activity is in the request state.
     */
    private void setButtonState(boolean isRequestEnabled){
        if(isRequestEnabled){
            requestLocation.setEnabled(false);
            //requestLocation.setBackgroundColor(Color.parseColor("#004488"));
            removeLocation.setEnabled(true);
            //removeLocation.setBackgroundColor(Color.parseColor("0088FF"));
        }else{
            requestLocation.setEnabled(true);
            //requestLocation.setBackgroundColor(Color.parseColor("#0088FF"));
            removeLocation.setEnabled(false);
            //removeLocation.setBackgroundColor(Color.parseColor("#004488"));
        }
    }

    public void dismissAlert(View view) {
        numOverlapped = 0;
        numDaysChecked = 0;
        alertView.setVisibility(View.INVISIBLE);
    }

    private void downloadDataAndCheck(){
        numOverlapped = 0;
        numDaysChecked = 0;
        for (int i = 0; i <= 7; i++) {
            final String fileNmae = Common.CONCERNEDLITERAL + i + ".txt";
            final int queueNumber = i;
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = Common.LINKS[i];
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if (queueNumber == 0) {
                        messageFromServer = response;
                    }else compareRecords(response, readTextFile(fileNmae));
                    numDaysChecked++;
                    if (numDaysChecked >= 8) {
                        alertView.setVisibility(View.VISIBLE);
                        String displayText = messageFromServer;
                        if (numOverlapped != 0) {
                            alertView.setBackgroundColor(getResources().getColor(R.color.RedAlert));
                            displayText +="\n" + numOverlapped + " Suspected Exposures\n";
                            } else {
                            alertView.setBackgroundColor(getResources().getColor(R.color.DowGreen));
                            displayText += "\nNo Suspected Exposure.\n";
                        }
                        displayText += "Rotate phone or tap message to dismiss";
                        alertView.setText(displayText);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    alertView.setVisibility(View.VISIBLE);
                    alertView.setBackgroundColor(getResources().getColor(R.color.DowGold));
                    alertView.setText(error.toString() + "\nTurn off wifi, try with data");
                }
            });
            stringRequest.setRetryPolicy(new RetryPolicy() {
                @Override
                public int getCurrentTimeout() {
                    return 50000;
                }

                @Override
                public int getCurrentRetryCount() {
                    return 5;
                }

                @Override
                public void retry(VolleyError error) throws VolleyError {
                }
            });
            queue.add(stringRequest);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(Common.KEY_REQUESTING_LOCATION_UPDATES)){
            setButtonState(sharedPreferences.getBoolean(Common.KEY_REQUESTING_LOCATION_UPDATES,false));
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onListenLocation(SendLocationToActivity event){
        if(event!= null) Toast.makeText(this, "Location and time encrypted and stored", Toast.LENGTH_SHORT).show();
    }

    private void compareRecords(String record1, String record2){
        Scanner sc1 = new Scanner(record1);
        Scanner sc2 = new Scanner(record2);
        String line1 = "";
        String line2 = "";
        if(sc1.hasNextLine())line1=sc1.nextLine();
        if(sc2.hasNextLine())line2=sc2.nextLine();
        if(line1.equals(line2)) {
            if(sc1.hasNextLine()){
                line1 = sc1.nextLine();
                if(sc2.hasNextLine()) {
                    line2 = sc2.nextLine();
                    while (sc1.hasNextLine() && sc2.hasNextLine()) {
                        int strCompareRes = Common.strCompare(line1,line2);
                        if(strCompareRes == 0) {
                            numOverlapped++;
                            line1 = sc1.nextLine();
                            line2 = sc2.nextLine();
                        }else if(strCompareRes < 0){
                            line1 = sc1.nextLine();
                        }else{
                            line2 = sc2.nextLine();
                        }
                    }
                    while(sc1.hasNextLine()){
                        int strCompareRes = Common.strCompare(line1,line2);
                        if(strCompareRes == 0)numOverlapped++;
                        else if(strCompareRes < 0)break;
                        line1 = sc1.nextLine();
                    }
                    while(sc2.hasNextLine()){
                        int strCompareRes = Common.strCompare(line1,line2);
                        if(strCompareRes == 0)numOverlapped++;
                        else if(strCompareRes > 0 )break;
                        line2 = sc2.nextLine();
                    }
                }
            }
        }
    }
}