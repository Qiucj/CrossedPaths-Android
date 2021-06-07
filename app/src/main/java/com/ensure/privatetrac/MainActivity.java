package com.ensure.privatetrac;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.os.Bundle;

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
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
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

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, SendDialog.SendDialogListener, CancelDialog.CancelDialogListener, AboutDialog.AboutDialogListener, AdapterView.OnItemSelectedListener {
    public static final String SEND_FILE_NAME = "send.txt";
    public static final String GROUP_PASSWORD_NAME = "groupPassword.txt";
    private LocationService mSerivce = null;
    private boolean mBound;
    private int numOverlapped = 0;
    private int numDaysChecked = 0;
    private int numSurfaceExposed = 0;
    private String messageFromServer = "";
    private GroupService groups;
    String[] arraySpinner;
    String password = "1234";

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

    Button requestLocation, removeLocation, checkDataBase, enterPassword;
    TextView alertManagerView, passwordText;
    Dialog infoDialog, infoDialog2, infoDialog3;
    ProgressBar checkDatabaseInProgress, downloadGroupsInProgress;
    ScrollView scrollView;
    Spinner spinner;
    ArrayAdapter<String> adapter;
    EditText groupPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        groups = new GroupService(this);
        alertManagerView = (TextView)findViewById(R.id.alertTextManager);

        View dialogView = getLayoutInflater().inflate(R.layout.infopage, null);
        infoDialog = new Dialog(this,android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        infoDialog.setContentView(dialogView);

        View dialogView2 = getLayoutInflater().inflate(R.layout.infopage2, null);
        infoDialog2 = new Dialog(this,android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        infoDialog2.setContentView(dialogView2);

        View dialogView3 = getLayoutInflater().inflate(R.layout.infopage3, null);
        infoDialog3 = new Dialog(this,android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        infoDialog3.setContentView(dialogView3);

        spinner = findViewById(R.id.spinner);
        arraySpinner = new String[groups.groupArr.size()];
        for (int x = 0; x < arraySpinner.length; x++) arraySpinner[x] = groups.groupArr.get(x).getGroupName();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        checkDatabaseInProgress = (ProgressBar) findViewById(R.id.progress1);
        checkDataBase = (Button) findViewById(R.id.check);
        checkDataBase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadDataAndCheck();
            }
        });

        downloadGroupsInProgress = (ProgressBar) findViewById(R.id.progressLoadGroups);

        groupPassword = (EditText) findViewById(R.id.editTextGroup);
        enterPassword = (Button) findViewById(R.id.enterPassword);
        passwordText = (TextView) findViewById(R.id.passwordExplain);
        scrollView = (ScrollView) findViewById(R.id.scroll_id);

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
                        requestLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mSerivce.requestLocatonUpdates(groups);
                            }
                        });
                        removeLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mSerivce.removeLocationUpdates();
                            }
                        });

                        setButtonState(Common.requestingLocationUpdates(MainActivity.this));
                        bindService(new Intent(MainActivity.this, LocationService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token){}
                }).check();

        setCheckDatabaseState(Common.requestingCheckDatabseProgressing(this));
        setGroupDownloadProgressingState(Common.requestingDownloadGroupsProgressing(this));
        setAlertManagerState(Common.requestingAlertManagerState(this));
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
            saveData("", Common.SURFACELITERAL  +i+".txt");
        }
        if(mSerivce != null)mSerivce.clearLocationArray();
    }

    @Override
    public void displayInfo(int userChoice){
        if (userChoice == 0) infoDialog.show();
        else if (userChoice == 1) infoDialog3.show();
        else if (userChoice == 2)infoDialog2.show();
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
        char selectChar = groups.getSelectedGroupChar();
        Boolean global = groups.getSelectedGroupNum() == 0;
        String sendText = "";
        for (int i = 1; i<=7; i++){
            if (global) sendText += getTextNot1stChar(Common.CONFIRMEDLITERAL+i+".txt");
            else sendText += getSelectTextNot1stChar(Common.CONFIRMEDLITERAL+i+".txt", selectChar);
        }
        saveData(sendText, SEND_FILE_NAME);
        File newFile = new File(this.getFilesDir(), SEND_FILE_NAME);
        Uri contentUri = FileProvider.getUriForFile(this, "com.ensure.PrivateTrac", newFile);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "was there");
        sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{groups.getSelectedGroup().email});
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent,null);
        startActivity(shareIntent);
    }

    @Override
    public void showLink(){
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(groups.getSelectedGroup().policy));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.android.chrome");
        try{this.startActivity(intent);}
        catch (ActivityNotFoundException ex){
            intent.setPackage(null);
            this.startActivity(intent);
        }
    }

    private String getSelectTextNot1stChar(String name, char selectChar) {
        String returnStr = "";
        try{
            String content = readTextFile(name);
            String[] lines = content.split("\n");
            returnStr = lines[0] + "\n";
            for (int i = 1; i < lines.length; i++) {
                if (lines[i].charAt(0) == selectChar) returnStr += lines[i].substring(1) + "\n";
            }
        } catch (Exception e) { /*Log.e("My log", "File did not load");*/ } //IS THERE ANYTHING ELSE WE SHOULD DO IN THIS CATCH
        return returnStr;
    }

    private String getTextNot1stChar(String name) {
        DataArray sortArr = new DataArray();
        try{
            String content = readTextFile(name);
            String[] lines = content.split("\n");
            for (int i = 1; i < lines.length; i++) sortArr.sort(lines[i].substring(1));
            return lines[0] + "\n" + sortArr.translate();
        } catch (Exception e) {
            //Log.e("My log", "File did not load"); IS THERE ANYTHING ELSE WE SHOULD DO IN THIS CATCH?
            return "";
        }
    }

    public void emailGovernor(View view){
        infoDialog2.dismiss();
        Intent emailIntent = new Intent();
        emailIntent.setAction(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing Location History Securely in Michigan");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"governorsoffice@michigan.gov"});
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Dear Governor Whitmer,\n\nI live in Michigan and recently tried out the anonymous location sharing app, CrossedPaths. I am very satisfied with the app's overall structure and data privacy model and feel comfortable with the app\'s safe location tracking methods. I believe that this type of location sharing app would be very useful in numerous public applications and am writing to urge you to consider using CrossedPaths in public cases where location sharing is prevalent. \n\n Thank you. \n");
        emailIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(emailIntent, null);
        startActivity(shareIntent);
    }

    public void sendData(View view){
        if (groups.getSelectedGroupNum() != 0) {
            SendDialog dialog = new SendDialog();
            dialog.show(getSupportFragmentManager(), "Send Data Dialog");
        }
        else {
            password = "1234";
            Common.setAlertManagerString(this, "groupPassword");
            Common.setPasswordString(this, "send");//new mierda
            Common.setAlertManagerState(this, true);
        }
    }

    public void contactUs(View view) {
        Intent emailIntent = new Intent();
        emailIntent.setAction(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Contact Us");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"havewemetapp@gmail.com"});
        emailIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(emailIntent, null);
        startActivity(shareIntent);
    }

    public void displayInfo(View view){
        AboutDialog dialog = new AboutDialog();
        dialog.show(getSupportFragmentManager(), "About Dialog");
    }

    public void dismissInfo(View view){ infoDialog.dismiss(); }

    public void dismissInfo2(View view){ infoDialog2.dismiss(); }

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

    public void login(View view) {
        if (groupPassword.getText().toString().equals(password)) {
            spinner.setSelection(3);
            Toast.makeText(view.getContext(), "Joined Group Successfully", Toast.LENGTH_SHORT).show();
            saveData("correct", GROUP_PASSWORD_NAME);
        }
        else Toast.makeText(view.getContext(), "Incorrect Password", Toast.LENGTH_SHORT).show();
        Common.setAlertManagerState(this, false);
    }

    public void enterSendPassword(View view) {
        if (groupPassword.getText().toString().equals(password)) {
            SendDialog dialog = new SendDialog();
            dialog.show(getSupportFragmentManager(), "Send Data Dialog");
        }
        else Toast.makeText(view.getContext(), "Incorrect Password", Toast.LENGTH_SHORT).show();
        Common.setAlertManagerState(this, false);
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
            removeLocation.setEnabled(true);
        }else{
            requestLocation.setEnabled(true);
            removeLocation.setEnabled(false);
        }
    }

    private void setCheckDatabaseState(Boolean check) {
        if (check) {
            checkDataBase.setVisibility(View.INVISIBLE);
            checkDatabaseInProgress.setVisibility(View.VISIBLE);
        }
        else {
            checkDataBase.setVisibility(View.VISIBLE);
            checkDatabaseInProgress.setVisibility(View.INVISIBLE);
        }
    }

    private void setGroupDownloadProgressingState(Boolean download) {
        if (download) {
            spinner.setVisibility(View.INVISIBLE);
            downloadGroupsInProgress.setVisibility(View.VISIBLE);
        }
        else {
            downloadGroupsInProgress.setVisibility(View.INVISIBLE);
            spinner.setVisibility(View.VISIBLE);
        }
    }

    private void setAlertManagerState(Boolean display) {
        String command = Common.requestingAlertManagerString(MainActivity.this);
        if(!display){
            if (command.equals(getString(R.string.downloadOthers)))downloadGroup();
            scrollView.setVisibility(View.INVISIBLE);
            alertManagerView.setVisibility(View.INVISIBLE);
            groupPassword.setVisibility(View.INVISIBLE);
            enterPassword.setVisibility(View.INVISIBLE);
            passwordText.setVisibility(View.INVISIBLE);
        } else if (command.equals("groupPassword")) {
            alertManagerView.setVisibility(View.INVISIBLE);
            scrollView.setVisibility(View.INVISIBLE);
            groupPassword.setVisibility(View.VISIBLE);
            enterPassword.setVisibility(View.VISIBLE);
            passwordText.setVisibility(View.VISIBLE);
            setPasswordState();
        } else {
            groupPassword.setVisibility(View.INVISIBLE);
            enterPassword.setVisibility(View.INVISIBLE);
            passwordText.setVisibility(View.INVISIBLE);
            scrollView.setVisibility(View.VISIBLE);
            alertManagerView.setBackgroundColor(Common.requestingAlertColor(this));
            alertManagerView.setText(command);
            alertManagerView.setVisibility(View.VISIBLE);
        }
    }

    public void setPasswordState() {
        if (Common.requestingPasswordString(MainActivity.this).equals("group")) {
            passwordText.setText(R.string.groupPassword);
            enterPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    login(view);
                }
            });
        } else {
            passwordText.setText(R.string.sendPassword);
            enterPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    enterSendPassword(view);
                }
            });
        }
    }

    public void dismissAlert(View view) {
        Common.setAlertManagerState(MainActivity.this, false);
    }

    private void downloadDataAndCheck(){
        Common.setCheckDatabaseProgressing(MainActivity.this, true);
        setCheckDatabaseState(true);
        numOverlapped = 0;
        numSurfaceExposed = 0;
        numDaysChecked = 0;
        for (int i = 0; i <= 7; i++) {
            final String fileNmae = Common.CONCERNEDLITERAL + i + ".txt";
            final String surfaceName = Common.SURFACELITERAL + i + ".txt";
            final int queueNumber = i;
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = Common.LINKS[i];
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if (queueNumber == 0) {
                        messageFromServer = response;
                    }else {
                        compareRecords(response, readTextFile(fileNmae), false);
                        compareRecords(response, readTextFile(surfaceName),true);
                    }
                    numDaysChecked++;
                    if (numDaysChecked >= 8) {
                        String groupMessage = "";
                        Scanner sc = new Scanner(messageFromServer);
                        if (sc.hasNextLine()) {
                            String thisLine = (sc.nextLine());
                            if (Integer.parseInt(thisLine) <= numOverlapped + numSurfaceExposed) {
                                while (sc.hasNextLine()) {
                                    groupMessage = groupMessage + sc.nextLine() + "\n";
                                }
                            }
                        }

                        String displayText = groupMessage;
                        if (numOverlapped != 0 || numSurfaceExposed !=0) {
                            Common.setAlertColor(MainActivity.this, getResources().getColor(R.color.RedAlert));
                            displayText +="\n" + numOverlapped + " Potential Overlaps\n";
                            displayText +="\n" + numSurfaceExposed + "Potential Overlap through touching common surfaces.";
                        } else {
                            Common.setAlertColor(MainActivity.this, getResources().getColor(R.color.NoConcern));
                            displayText += "\nNo Potential Overlaps.\n";
                        }
                        displayText += "Rotate phone or tap message to dismiss";
                        Common.setAlertManagerString(MainActivity.this, displayText);
                        Common.setAlertManagerState(MainActivity.this,true);
                        Common.setCheckDatabaseProgressing(MainActivity.this, false);
                        setCheckDatabaseState(false);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Common.setAlertManagerString(MainActivity.this, error.toString() + "\nTurn off wifi, try with data");
                    Common.setAlertColor(MainActivity.this, getResources().getColor(R.color.DowGold));
                    Common.setAlertManagerState(MainActivity.this, true);
                    Common.setCheckDatabaseProgressing(MainActivity.this, false);
                    setCheckDatabaseState(false);
                }
            });
            queue.add(stringRequest);
       }
    }

    public void downloadGroup(){
        Common.setDownloadGroupsProgressing(MainActivity.this, true);
        setGroupDownloadProgressingState(true);
        groups.downloadOK = false;
        String url =  "https://drive.google.com/uc?id=1gVxmvR2NipnzwRHszJVU5yq_eD4Xa21Q&export=download";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                groups.downloadOK = true;
                groups.downloadStr = response;
                groups.writeGroup();
                groups.readGroup();
                Common.setAlertManagerString(MainActivity.this,"download successful");
                Common.setAlertColor(MainActivity.this,R.color.NoConcern);
                Common.setAlertManagerState(MainActivity.this, true);
                Common.setDownloadGroupsProgressing(MainActivity.this, false);
                setGroupDownloadProgressingState(false);
                arraySpinner = new String[groups.groupArr.size()];
                for (int x = 0; x < arraySpinner.length; x++) {
                    arraySpinner[x] = groups.groupArr.get(x).getGroupName();
                }
                adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, arraySpinner);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Common.setAlertManagerString(MainActivity.this,error.toString()+ "\nTurn off wifi, try with data");
                Common.setAlertColor(MainActivity.this, R.color.DowGold);
                Common.setAlertManagerState(MainActivity.this, true);
                Common.setDownloadGroupsProgressing(MainActivity.this, false);
                setGroupDownloadProgressingState(false);
            }
        });
        queue.add(stringRequest);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(Common.KEY_REQUESTING_LOCATION_UPDATES)){
            setButtonState(sharedPreferences.getBoolean(Common.KEY_REQUESTING_LOCATION_UPDATES,false));
        }else if (key.equals(Common.KEY_ALERT_MANAGER) || key.equals(Common.KEY_ALERT_MANAGER_STRING)){
            setAlertManagerState(Common.requestingAlertManagerState(this));
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onListenLocation(SendLocationToActivity event){
        if(event!= null) Toast.makeText(this, "Location and time encrypted and stored", Toast.LENGTH_SHORT).show();
    }

    private void compareRecords(String record1, String record2, Boolean surface){
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
                            if(surface) numSurfaceExposed++;
                            else numOverlapped++;
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
                        if(strCompareRes == 0){
                            if (surface) numSurfaceExposed++;
                            else numOverlapped++;
                        }
                        else if(strCompareRes < 0)break;
                        line1 = sc1.nextLine();
                    }
                    while(sc2.hasNextLine()){
                        int strCompareRes = Common.strCompare(line1,line2);
                        if(strCompareRes == 0){
                            if (surface) numSurfaceExposed++;
                            else numOverlapped++;
                        }
                        else if(strCompareRes > 0 )break;
                        line2 = sc2.nextLine();
                    }
                }
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (i == 3 && readTextFile(GROUP_PASSWORD_NAME).equals("")) {
            password = "shiawassee";
            Common.setAlertManagerString(this, "groupPassword");
            Common.setPasswordString(MainActivity.this, "group");//new mierda
            Common.setAlertManagerState(this, true);
            spinner.setSelection(0);
        }
        groups.setSelectedGroup(i);
        if (groups.getSelectedGroup().getGroupName().equals("other...")) {
            Common.setAlertManagerState(this,true);
            Common.setAlertColor(this,R.color.Grey);
            Common.setAlertManagerString(this,getString(R.string.downloadOthers));
            Common.setAlertManagerState(this, true);
            setAlertManagerState(true);
        }else if (Common.requestingAlertManagerString(this).equals(getString(R.string.downloadOthers))){
            //Log.e("My Log", "previous selection is others");
            Common.setAlertManagerString(this, "");
            Common.setAlertManagerState(this,false);
        }
        String text = adapterView.getItemAtPosition(i).toString();
        Toast.makeText(adapterView.getContext(), text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}