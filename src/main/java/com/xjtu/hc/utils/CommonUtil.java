package com.xjtu.hc.utils;

import com.xjtu.hc.hcservice.HCNetSDK;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtil {

    //SDK时间解析
    public static String parseTime(int time)
    {
        int dwYear=(time>>26)+2000;
        int dwMonth=(time>>22)&15;
        int dwDay=(time>>17)&31;
        int dwHour=(time>>12)&31;
        int dwMinute=(time>>6)&63;
        int dwSecond=(time>>0)&63;
        
        String sTime = String.format("%04d", dwYear) +
                String.format("%02d", dwMonth) +
                String.format("%02d", dwDay) +
                String.format("%02d", dwHour) +
                String.format("%02d", dwMinute) +
                String.format("%02d", dwSecond);
        return sTime;
    }

    /**
     * 获取海康录像机格式的时间
     * @param time
     * @return
     */
    public static HCNetSDK.NET_DVR_TIME getHkTime(Date time) {
        HCNetSDK.NET_DVR_TIME structTime = new HCNetSDK.NET_DVR_TIME();
        String str = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(time);
        String[] times = str.split("-");
        structTime.dwYear = Integer.parseInt(times[0]);
        structTime.dwMonth = Integer.parseInt(times[1]);
        structTime.dwDay = Integer.parseInt(times[2]);
        structTime.dwHour = Integer.parseInt(times[3]);
        structTime.dwMinute =Integer.parseInt(times[4]);
        structTime.dwSecond = Integer.parseInt(times[5]);
        return structTime;
    }


}
