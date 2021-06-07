package com.ensure.privatetrac;

import android.location.Location;

public class SendLocationToActivity {
    private Location location;
    public SendLocationToActivity(Location mLocation) {this.location = mLocation; }

    public Location getLocation(){return location;}
}
