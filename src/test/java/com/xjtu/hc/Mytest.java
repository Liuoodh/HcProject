package com.xjtu.hc;

import com.xjtu.hc.entities.HcAuthorization;
import com.xjtu.hc.hcservice.VideoDownload;
import com.xjtu.hc.hcservice.VideoPlayback;
import com.xjtu.hc.hcservice.VideoPreview;
import com.xjtu.hc.hcservice.VideoRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Mytest {

    @Autowired
    VideoDownload videoDownload;

    @Autowired
    VideoRecord videoRecord;

    @Autowired
    VideoPreview videoPreview;

    @Test
    void downLoadTest(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date startTime =null;
        Date endTime = null;
        try {
            startTime = sdf.parse("20230529120000");   //开始时间
            endTime =   sdf.parse("20230529120030");      //结束时间
        } catch (ParseException e) {
            e.printStackTrace();
        }

        HcAuthorization auth = new HcAuthorization("192.168.1.64", 8000, "admin", "12345qwer",1);
        String sFileName = ".\\Download\\" + System.currentTimeMillis() + ".mp4";

        videoDownload.downloadVideo(auth,startTime,endTime,sFileName);

    }

    @Test
    void recordTest(){
        HcAuthorization auth = new HcAuthorization("192.168.1.64", 8000, "admin", "12345qwer",1);
        int lengthOfTime=100;//10秒钟
        String localSaveFilePath="E:\\JavaProject\\projects\\MyHcProject\\Record";
        String fileName="test100";
        try {
            videoRecord.videoRecord(auth,lengthOfTime,localSaveFilePath,fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void previewTest(){

    }

    @Test
    void playbackTest(){
        HcAuthorization auth = new HcAuthorization("192.168.1.64", 8000, "admin", "12345qwer",1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date startTime =null;
        Date endTime = null;
        try {
            startTime = sdf.parse("20230530120000");   //开始时间
            endTime =   sdf.parse("20230530120030");      //结束时间
        } catch (ParseException e) {
            e.printStackTrace();
        }
        VideoPlayback videoPlayback = new VideoPlayback();

        try {
            videoPlayback.videoPlayBack(auth,startTime,endTime );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }



}
