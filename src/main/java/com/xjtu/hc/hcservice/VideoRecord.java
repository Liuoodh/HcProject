package com.xjtu.hc.hcservice;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.xjtu.hc.config.HcConfig;
import com.xjtu.hc.entities.HcAuthorization;
import com.xjtu.hc.utils.osSelect;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;


@Service
public class VideoRecord {

    private final HCNetSDK sdk= HcConfig.hCNetSDK;
    private int userId;
    private int key;


    public void videoRecord(HcAuthorization authorization,int lengthOfTime,String localSaveFilePath,String fileName) throws Exception {


        long lUserId = HcConfig.register(authorization);

        if(lUserId==-1) {
            System.err.println("hksdk(视频)-海康sdk登录失败!");
            throw new RuntimeException();
        }
        userId=(int)lUserId;


        //启动实时预览功能  创建clientInfo对象赋值预览参数
        HCNetSDK.NET_DVR_CLIENTINFO clientInfo = new HCNetSDK.NET_DVR_CLIENTINFO();

        clientInfo.lChannel = authorization.getChannel();   //设置通道号
        clientInfo.lLinkMode = 0;  //TCP取流
        clientInfo.sMultiCastIP = null;                   //不启动多播模式

        //创建窗口句柄
        clientInfo.hPlayWnd = null;

        FRealDataCallBack fRealDataCallBack = new FRealDataCallBack();
        //开启实时预览
        key = sdk.NET_DVR_RealPlay_V30(userId, clientInfo, fRealDataCallBack, null, true);

        //判断是否预览成功
        if (key==-1) {
            System.err.println("预览失败   错误代码为:  " + sdk.NET_DVR_GetLastError());
            logoutHIK();
            throw new RuntimeException();
        }
        saveRealData(authorization,lengthOfTime,localSaveFilePath,fileName);

    }



    public void saveRealData(HcAuthorization authorization, int lengthOfTime,String localSaveFilePath,String fileName) throws Exception {

            System.out.println("设备"+authorization.getIp()+":"+authorization.getPort()+"开始录制当前段文件："+fileName);
            PTZ(authorization,lengthOfTime,localSaveFilePath,fileName);
            System.out.println("设备"+authorization.getIp()+":"+authorization.getPort()+"结束录制当前段文件："+fileName);


    }

    public void logoutHIK(){
        System.out.println("退出...");
        sdk.NET_DVR_StopRealPlay(key);
        sdk.NET_DVR_Logout(userId);
    }


    private void PTZ(HcAuthorization authorization, int lengthOfTime ,String localSaveFilePath,String fileName) throws Exception {
        // 查看文件夹是否存在,如果不存在则创建
        File file = new File(localSaveFilePath);
        if (!file.exists()) {
            file.mkdir();
        }
        HCNetSDK.NET_DVR_CLIENTINFO clientInfo = new HCNetSDK.NET_DVR_CLIENTINFO();
        clientInfo.lChannel = authorization.getChannel();   //设置通道号
        clientInfo.lLinkMode = 0;  //TCP取流
        clientInfo.sMultiCastIP = null;                   //不启动多播模式
        //创建窗口句柄
        clientInfo.hPlayWnd = null;
        HCNetSDK.NET_DVR_JPEGPARA netDvrJpegpara = new HCNetSDK.NET_DVR_JPEGPARA();
        netDvrJpegpara.wPicQuality = 2;
        netDvrJpegpara.wPicSize =2;

        String tr;
        if(osSelect.isLinux()){
            tr="/";
        }else{
            tr="\\";
        }
//        System.out.println((file.getPath() + tr + fileName + ".jpg"));
////        file.getPath() + tr+ fileName + ".jpg"
//        if(!sdk.NET_DVR_CaptureJPEGPicture(userId,authorization.getChannel(),netDvrJpegpara , ("1.jpg").getBytes(StandardCharsets.UTF_8))){
//            System.err.println("保存预览图到文件夹失败 错误码为:  " + sdk.NET_DVR_GetLastError());
//            throw new RuntimeException();
//        }

        HCNetSDK.NET_DVR_I_FRAME netDvrIFrame = new HCNetSDK.NET_DVR_I_FRAME();
        netDvrIFrame.read();
        netDvrIFrame.dwChannel = 1;
        netDvrIFrame.byStreamType = 0;
        netDvrIFrame.dwSize = netDvrIFrame.size();
        netDvrIFrame.write();

        if(!sdk.NET_DVR_RemoteControl(userId,3402,netDvrIFrame.getPointer(),netDvrIFrame.dwSize)){
            System.err.println("强制I帧 错误码为:  " + sdk.NET_DVR_GetLastError());
            throw new RuntimeException();
        }
        System.out.println("保存");
        //预览成功后 调用接口使视频资源保存到文件中
        if (!sdk.NET_DVR_SaveRealData_V30(key, 2,file.getPath() + tr + fileName + ".mp4")) {
            System.err.println("保存视频文件到文件夹失败 错误码为:  " + sdk.NET_DVR_GetLastError());
            throw new RuntimeException();
        }

        Thread.sleep(lengthOfTime* 1000);
        sdk.NET_DVR_StopSaveRealData(key);



    }



    //预览回调
    class FRealDataCallBack implements HCNetSDK.FRealDataCallBack_V30 {
        @Override
        public void invoke(int lRealHandle, int dwDataType, ByteByReference pBuffer, int dwBufSize, Pointer pUser) {

        }
    }





}
