package com.ensure.privatetrac;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class GroupItem {
    String name = "";
    String email = "";
    String[] links = new String[8];
    ArrayList<Double> centerLat = new ArrayList<Double>();
    ArrayList<Double> centerLon = new ArrayList<Double>();
    ArrayList<Double> radius = new ArrayList<Double>();
    ArrayList<Double> exCenterLat = new ArrayList<Double>();
    ArrayList<Double> exCenterLon = new ArrayList<Double>();
    ArrayList<Double> exRadius = new ArrayList<Double>();
    ArrayList<Integer> startSecs = new ArrayList<Integer>();
    ArrayList<Integer> endSecs = new ArrayList<Integer>();
    String policy = "";
    int id;

    public GroupItem(String iname, String iemail, String[] ilinks, ArrayList<Double> icenterLat, ArrayList<Double> icenterLon, ArrayList<Double> iradius, ArrayList<Double> iexCenterLat, ArrayList<Double> iexCenterLon, ArrayList<Double> iexRadius, ArrayList<Integer> istartSecs, ArrayList<Integer> iendSecs, String ipolicy, int iid) {
        name = iname;
        email = iemail;
        links = ilinks;
        centerLat = icenterLat;
        centerLon = icenterLon;
        radius = iradius;
        exCenterLat = iexCenterLat;
        exCenterLon = iexCenterLon;
        exRadius = iexRadius;
        startSecs = istartSecs;
        endSecs = iendSecs;
        policy = ipolicy;
        id = iid;
    }

    public String getGroupName() {
        return name;
    }

    public double dBetween(Double latiA, Double latiB, Double longiA, Double longiB) {
        double r = 6371000.0;
        double fi1 = latiA * Math.PI / 180.0;
        double fi2 = latiB * Math.PI / 180.0;
        double deltaFi = fi2-fi1;
        double deltaLd = (longiB - longiA) * Math.PI / 180;
        double a = Math.sin(deltaFi/2) * Math.sin(deltaFi/2) + Math.cos(fi1) * Math.cos(fi2) * Math.sin(deltaLd/2) * Math.sin(deltaLd/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return r * c;
    }

    public Boolean inRange(double lati, double longi, Date time) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(time);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int secondsInDay = 3600*hour + 60*minute + second;
        boolean timeInRange = false;
        for (int c = 0; c < startSecs.size(); c++) {
            if (secondsInDay > startSecs.get(c) && secondsInDay < endSecs.get(c)) {
                timeInRange = true;
                break;
            }
        }
        if (timeInRange) {
            boolean locInRange = false;
            for (int c = 0; c < centerLat.size(); c++) {
                if (radius.get(c) > dBetween(lati, centerLat.get(c), longi, centerLon.get(c))) {
                    locInRange = true;
                    break;
                }
            }
            if(locInRange){
                for (int c = 0; c < exCenterLat.size(); c++) {
                    if (exRadius.get(c) > dBetween(lati, exCenterLat.get(c), longi, exCenterLon.get(c))) {
                        locInRange = false;
                        break;
                    }
                }
            }
            return locInRange;
        }
        return false;
    }
}
