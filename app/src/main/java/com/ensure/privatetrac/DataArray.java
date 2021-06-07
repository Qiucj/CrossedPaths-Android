package com.ensure.privatetrac;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class DataArray {
    private ArrayList<String> dataArr = new ArrayList<>();

    public void sort(String str){
        int max = dataArr.size();
        int min = 0;
        boolean done = false;
        if(max == 0){
            dataArr.add(str);
            done = true;
        }
        while(!done){
            if (min+1 >= max){
                int compareResult = Common.strCompare(str,  dataArr.get(min));
                if (compareResult > 0) min++;
                if (min < dataArr.size()) {
                    compareResult = Common.strCompare(str, dataArr.get(min));
                }else compareResult = 1;
                if(compareResult != 0) dataArr.add(min, str);
                done = true;
            }else{
                int middle = (max+min)/2;
                int compareResult = Common.strCompare(str, dataArr.get(middle));
                if(compareResult == 0) done = true;
                else{
                    if (compareResult > 0) min = middle;
                    else max = middle;
                }
            }
        }
    }

    public void clear(){
        dataArr = new ArrayList<>();
    }

    public String translate(){
        String sub="";
        int repeating = dataArr.size();
        for(int x=0; x<repeating; x++) sub += dataArr.get(x)+"\n";
        return sub;
    }

    public int getDay(){
        if (dataArr.size() == 0) return 0;
        else return Integer.parseInt((dataArr.get(0)).substring(0,1));
    }
}
