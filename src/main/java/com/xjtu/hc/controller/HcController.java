package com.xjtu.hc.controller;


import com.xjtu.hc.entities.HcAuthorization;
import com.xjtu.hc.hcservice.ThreadTaskService;
import com.xjtu.hc.hcservice.VideoDownload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/HcVision")
public class HcController {

    @Autowired
    VideoDownload videoDownload;

    @Autowired
    ThreadTaskService threadTaskService;
    //下载视频
    @RequestMapping(value = "/download",method = RequestMethod.POST)
    public String downLoad(@RequestBody HcAuthorization authorization, @RequestParam("start") String start,@RequestParam("end") String end,@RequestParam("path") String path){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date startTime =null;
        Date endTime = null;
        try {
            startTime = sdf.parse(start);   //开始时间
            endTime =   sdf.parse(end);      //结束时间
        } catch (ParseException e) {
            e.printStackTrace();
            return "时间格式不对";
        }
        String fileName=path+start+"---"+end+".mp4";
        if(!videoDownload.downloadVideo(authorization,startTime,endTime,fileName)){
            return "下载失败";
        }
        return "下载成功";
    }

    //开启录像
    @RequestMapping(value = "/openRecord",method = RequestMethod.POST)
    public String openRecord(@RequestBody HcAuthorization authorization,@RequestParam("length") int lengthOfTime,@RequestParam("path") String path ){
        threadTaskService.task(authorization,lengthOfTime,path);
        return "开始录像,存储于："+path;
    }

    //关闭录像
    @RequestMapping(value = "/closeRecord",method = RequestMethod.POST)
    public String closeRecord(@RequestBody HcAuthorization authorization){
        threadTaskService.getRecordControl().put(authorization.toString(),Boolean.FALSE);
        return "结束录像";
    }

}
