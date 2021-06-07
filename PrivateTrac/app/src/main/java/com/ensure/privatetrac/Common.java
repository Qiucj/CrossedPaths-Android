package com.ensure.privatetrac;

import android.content.Context;
import android.preference.PreferenceManager;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;

import java.text.SimpleDateFormat;

public class Common {
    public static final String KEY_REQUESTING_LOCATION_UPDATES = "LocationUpdateEnable";
    public static final String KEY_REQUESTING_EXPOSURES = "Exposures";

    public static void setRequestingLocationUpdates(Context context, boolean value){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_REQUESTING_LOCATION_UPDATES, value).apply();
    }

    public static void setExposure(Context context, int value){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(KEY_REQUESTING_EXPOSURES, value).apply();
    }

    public static int getExposure(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_REQUESTING_EXPOSURES,0);
    }

    public static boolean requestingLocationUpdates(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_REQUESTING_LOCATION_UPDATES,false);
    }

    public static final String CONCERNEDLITERAL = "Concerned";
    public static final String CONFIRMEDLITERAL = "Confirmed";

    public static final String[] LINKS = {
            "https://drive.google.com/uc?id=1ICW6_uRRfLDw-04LM5Ode0RSRNKrLR6g&export=download",
            "https://drive.google.com/uc?id=1No0ApuDL9njzxefPNKguH9kp7OHmibV6&export=download",
            "https://drive.google.com/uc?id=1xE7yEZAOkABnRNQhSek25EQoFZp_v2f_&export=download",
            "https://drive.google.com/uc?id=1NYNo5nD9e0suubkSCa7d61Yvz_cBLSF2&export=download",
            "https://drive.google.com/uc?id=1tgWgIKD_jP3dtyAjpbEmb-ANiI6ZqXCu&export=download",
            "https://drive.google.com/uc?id=1xhPMgVWhMMlU7gL4Oy7WCkl7fzEu2cmq&export=download",
            "https://drive.google.com/uc?id=1LeqsHl6P5jqpHI0UyHlKQz5bHAxOmF0s&export=download",
            "https://drive.google.com/uc?id=1uLFMlbKLO3-eZPla6BDkhV-B4woxYeLj&export=download"
    };

    public static String getFormatteDate(){
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return "#"+dateFormat.format(date);
    }

    public static int getDayOfWeek(){
        Calendar cal = Calendar.getInstance();
        cal.setTime(cal.getTime());
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    public static int strCompare(String str1, String str2){
        int l1 = str1.length();
        int l2 = str2.length();
        int lmin = Math.min(l1,l2);
        for (int i = 0; i < lmin; i++){
            int str1_ch = (int)str1.charAt(i);
            int str2_ch = (int)str2.charAt(i);
            if(str1_ch != str2_ch) return str1_ch - str2_ch;
        }
        if(l1 != l2) return l1-l2;
        else return 0;
    }

    public static String info = "is a contact tracing app focused on data privacy.\\n\\nThe app is in beta testing to build user trust in its data privacy. \\\"Check Database\\\" button works, but the database is hypothetical. All warnings should be ignored. After the official launch with healthcare partners, \\\"Check Database\\\" will prompt you to update your app.\\n\\nWhen you are satisfied with its data privacy and would like to see digital contact tracing adopted in Michigan, Please contact Governer Whitmer to help its official launch.";
    public static String info2 = "Data privacy is a natural concern for contact tracing apps.  PrivateTrac prioritizes data privacy employing multiple strategies:\\n\\n1. All data is stored locally on your phone. No one, not even other apps on your phone, can access it. Sharing data with healthcare officials after Covid confirmation is voluntary. Further data privacy protocols will be provided before you decide to share.\\n\\n2. You have complete control over when to collect data. Data is automatically deleted after a week. You can delete all data at any time.\\n\\n3. Data is encrypted before stored locally on your phone. The encryption algorithm, SHA-256, is irreversible.  Stored data cannot be traced back to a location or time.\\n\\n4. Decentralized model lets your phone download the database and compare on the phone. No one knows your exposure results.\\n";
    public static String info2_2 = "During beta testing, use \"SEND DATA\" button to email me any questions or suggestions.  Once satisfied with its data privacy, please share with friends and email health officials and Governor Whitmer to advocate for the public health department's adoption of this app.";
    public static String letter = "Dear Governor Whitmer,\n\nI am a Michigander and would like to see Michigan adopt digital contact tracing to fight Covid-19.  I tried PrivateTrac app and is satisfied with its data privacy.  I would appreaciate your effort in establishing a digital contact tracing app for Michigan by encouraging pulic health department to work with such app developers.\n\n Regards, \n";
}
