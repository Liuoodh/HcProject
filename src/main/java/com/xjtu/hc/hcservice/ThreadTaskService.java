package com.xjtu.hc.hcservice;


import com.xjtu.hc.entities.HcAuthorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ThreadTaskService {

    private ConcurrentHashMap<String,Boolean>recordControl=new ConcurrentHashMap<>();

    @Autowired
    VideoRecord videoRecord;


    @Async("recordThreadPool") //录像
    public void task(HcAuthorization auth, int lengthOfTime, String localSaveFilePath) {

        if(recordControl.get(auth.toString())!=null){
            if(recordControl.get(auth.toString())){
                return;
            }
        }
        recordControl.put(auth.toString(),true);
        int temp=5;//捕捉到5次异常则结束录制
        while (true){
            if(recordControl.get(auth.toString())){
                try {
                    DateTimeFormatter dfDate = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
                    String s = dfDate.format(LocalDateTime.now());
                    String e = dfDate.format(LocalDateTime.now().plusSeconds(lengthOfTime));
                    String fileName=s+"---"+e;
                    videoRecord.videoRecord(auth,lengthOfTime,localSaveFilePath,fileName);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    temp--;
                    if(temp<0){
                        recordControl.put(auth.toString(),false);
                    }
                }
            }else{
                System.out.println(auth.getIp()+":"+auth.getPort()+"/"+auth.getChannel()+"结束录像\n");
                System.out.println("文件保存于："+localSaveFilePath);
                break;
            }
        }
    }

    public ConcurrentHashMap<String, Boolean> getRecordControl() {
        return recordControl;
    }
}
