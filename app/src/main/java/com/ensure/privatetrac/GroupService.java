package com.ensure.privatetrac;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import static android.content.Context.DEVICE_POLICY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

public class GroupService {
    public boolean downloadOK = false;
    public ArrayList<GroupItem> groupArr = new ArrayList<>();
    private int groupSelected = 0;
    private Context l_Context;
    private String defaultStr = "0NAon Earth \n0EMhavewemetapp@gmail.com\n0LKhttps://drive.google.com/uc?id=1ICW6_uRRfLDw-04LM5Ode0RSRNKrLR6g&export=download\n0LKhttps://drive.google.com/uc?id=1No0ApuDL9njzxefPNKguH9kp7OHmibV6&export=download\n0LKhttps://drive.google.com/uc?id=1xE7yEZAOkABnRNQhSek25EQoFZp_v2f_&export=download\n0LKhttps://drive.google.com/uc?id=1NYNo5nD9e0suubkSCa7d61Yvz_cBLSF2&export=download\n0LKhttps://drive.google.com/uc?id=1tgWgIKD_jP3dtyAjpbEmb-ANiI6ZqXCu&export=download\n0LKhttps://drive.google.com/uc?id=1xhPMgVWhMMlU7gL4Oy7WCkl7fzEu2cmq&export=download\n0LKhttps://drive.google.com/uc?id=1LeqsHl6P5jqpHI0UyHlKQz5bHAxOmF0s&export=download\n0LKhttps://drive.google.com/uc?id=1uLFMlbKLO3-eZPla6BDkhV-B4woxYeLj&export=download\n0LI 0, 0, 25000000\n0LX 0, 0, 0\n0TI 0, 0, 0, 23, 59, 59\n0PPhttps://drive.google.com/file/d/1L7fi7zBiGUO2YhqpHYJeP3I6KEYOQes7/view?usp=sharing\n";
    public String downloadStr = "";
    private static String FILENAME = "Group.txt";

    public GroupService(Context fromMain){
        l_Context = fromMain;
        String strFromFile = readTextFile(FILENAME);
        if (strFromFile == ""){
            writeGroup();
        }
        readGroup();
    }

    public void setSelectedGroup(int numSelected){
        if (numSelected < groupArr.size()){
            groupSelected = numSelected;
        }else {
            groupSelected = 0;
        }
    }

    public GroupItem getSelectedGroup() {
        if (groupArr.size() > 0) {
            return groupArr.get(groupSelected);
        }else{return null;}
    }

    public char getSelectedGroupChar() {
        return intToChar(groupSelected);
    }

    public int getSelectedGroupNum() {
        return groupSelected;
    }

    public void writeGroup(){
        if(downloadOK){
            saveFile(defaultStr+downloadStr);
        }else{
            saveFile(defaultStr);
        }
    }

    private char intToChar(int value){
        return (char)(value+48);
    }

    public void readGroup(){
        groupArr.clear();
        Scanner sc = new Scanner(readTextFile(FILENAME));
        Boolean nameFound = false;
        String name = "";
        Boolean emailFound = false;
        String email = "";
        String[] links = new String[8];
        ArrayList<Double> centerLat = new ArrayList<>();
        ArrayList<Double> centerLon = new ArrayList<>();
        ArrayList<Double> radius = new ArrayList<>();
        ArrayList<Double> exLat = new ArrayList<>();
        ArrayList<Double> exLon = new ArrayList<>();
        ArrayList<Double> exRadius = new ArrayList<>();
        ArrayList<Integer> timeStart = new ArrayList<>();
        ArrayList<Integer> timeEnd = new ArrayList<>();
        Boolean policyFound = false;
        String policy = "";
        int groupsFound = 0;
        Boolean wrongFormat = false;
        int linksFound = 0;
        if(sc.hasNextLine()){ sc.nextLine();}
        while (sc.hasNextLine()){
            String thisLine = sc.nextLine();
             if (thisLine.charAt(0) == intToChar(groupsFound)){
                String indicator = thisLine.substring(1,3);
                switch (indicator){
                    case "NA":
                        nameFound = true;
                        name = thisLine.substring(3);
                        break;
                    case "PP":
                        policyFound = true;
                        policy = thisLine.substring(3);
                        break;
                    case "EM":
                        emailFound = true;
                        email = thisLine.substring(3);
                        break;
                    case "LK":
                        if (linksFound < 8){
                            links[linksFound] = thisLine.substring(3);
                            linksFound++;
                        }else wrongFormat = true;
                        break;
                    case "LI":
                        String [] numbers = (thisLine.substring(3)).split(",");
                        if (numbers.length == 3){
                            centerLat.add(Double.parseDouble(numbers[0]));
                            centerLon.add(Double.parseDouble(numbers[1]));
                            radius.add(Double.parseDouble(numbers[2]));
                        }else wrongFormat = true;
                        break;
                    case "LX":
                        numbers = (thisLine.substring(3)).split(",");
                        if (numbers.length == 3){
                            exLat.add(Double.parseDouble(numbers[0]));
                            exLon.add(Double.parseDouble(numbers[1]));
                            exRadius.add(Double.parseDouble(numbers[2]));
                        }else wrongFormat = true;
                        break;
                    case "TI":
                        numbers = (thisLine.substring(3)).split(",");
                        if (numbers.length == 6){
                            int seconds = 3600 * Integer.parseInt(numbers[0].replaceAll("\\D+",""));
                            seconds += 60 * Integer.parseInt(numbers[1].replaceAll("\\D+",""));
                            timeStart.add(seconds+Integer.parseInt(numbers[2].replaceAll("\\D+","")));
                            seconds = 3600 * Integer.parseInt(numbers[3].replaceAll("\\D+",""));
                            seconds += 60 * Integer.parseInt(numbers[4].replaceAll("\\D+",""));
                            timeEnd.add(seconds+Integer.parseInt(numbers[5].replaceAll("\\D+","")));
                        }else wrongFormat = true;
                        break;
                    default:
                        wrongFormat = true;
                        break;
                }
            }else if (!wrongFormat && nameFound && emailFound && linksFound == 8 && centerLat.size() > 0 && timeStart.size()>0 && policyFound){
                groupArr.add(new GroupItem(name,email,links,centerLat, centerLon,radius,exLat,exLon,exRadius,timeStart,timeEnd,policy,groupsFound));
                groupsFound++;
                nameFound = false;
                emailFound = false;
                policyFound = false;
                linksFound = 0;
                links = new String[8];
                centerLat = new ArrayList<Double>();
                centerLon = new ArrayList<Double>();
                radius = new ArrayList<Double>();
                exLat = new ArrayList<Double>();
                exLon = new ArrayList<Double>();
                exRadius = new ArrayList<Double>();
                timeStart = new ArrayList<Integer>();
                timeEnd = new ArrayList<Integer>();
            }else wrongFormat = true;
            if (wrongFormat){
                groupArr.clear();
                break;
            }
        }
        if(!wrongFormat && nameFound && emailFound && linksFound == 8 && centerLat.size() > 0 && timeStart.size()>0 && policyFound){
            groupArr.add(new GroupItem(name,email,links,centerLat, centerLon,radius,exLat,exLon,exRadius,timeStart,timeEnd,policy,groupsFound));
            groupsFound++;
        }

        centerLat = new ArrayList<Double>();
        centerLat.add(0.0);
        centerLon = new ArrayList<Double>();
        centerLon.add(0.0);
        radius = new ArrayList<Double>();
        radius.add(0.0);
        exLat = new ArrayList<Double>();
        exLat.add(0.0);
        exLon = new ArrayList<Double>();
        exLon.add(0.0);
        exRadius = new ArrayList<Double>();
        exRadius.add(0.0);
        timeStart = new ArrayList<Integer>();
        timeStart.add(0);
        timeEnd = new ArrayList<Integer>();
        timeEnd.add(1);

        groupArr.add(new GroupItem("other...", "", Common.LINKS, centerLat, centerLon, radius,
                exLat,exLon, exRadius, timeStart, timeEnd, "", groupsFound));
        //Log.e("My log", "Groups found: " + (groupsFound+1));
        groupSelected = 0;
    }


    private void saveFile(String text){
        FileOutputStream fos = null;
        try{
            fos = l_Context.openFileOutput(FILENAME, MODE_PRIVATE);
            String writeText = Common.getFormatteDate()+"\n"+text;
            fos.write(writeText.getBytes());
        }catch(FileNotFoundException e){ e.printStackTrace();
        }catch(IOException e){ e.printStackTrace();
        } finally{
            if(fos!=null){try{fos.close();} catch(IOException e){e.printStackTrace();}}
        }
    }

    private String readTextFile(String fileName){
        FileInputStream fis = null;
        try{
            fis=l_Context.openFileInput(fileName);
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

}
