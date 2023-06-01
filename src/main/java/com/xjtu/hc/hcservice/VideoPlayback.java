package com.xjtu.hc.hcservice;

import com.github.misterchangray.core.util.ConverterUtil;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.xjtu.hc.config.HcConfig;
import com.xjtu.hc.entities.HcAuthorization;
import com.xjtu.hc.utils.CommonUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VideoPlayback {
    private final HCNetSDK sdk= HcConfig.hCNetSDK;
    private int userId;
    private int key;
    static VideoPlayback.PlaybackDataCallBack playbackDataCallBack;//预览回调函数实现




    public int videoPlayBack(HcAuthorization authorization, Date start, Date end) throws FileNotFoundException {
        long lUserId = HcConfig.register(authorization);


        if(lUserId==-1) {
            System.err.println("hksdk(视频)-海康sdk登录失败!");
            return -1;
        }
        userId=(int)lUserId;

        //回调函数定义必须是全局的
        if (playbackDataCallBack == null) {
            playbackDataCallBack = new VideoPlayback.PlaybackDataCallBack();
        }


        int iPlayBack=sdk.NET_DVR_PlayBackByTime(userId,authorization.getChannel(),CommonUtil.getHkTime(start),CommonUtil.getHkTime(end),null);
        if (iPlayBack == -1) {
            int iErr = sdk.NET_DVR_GetLastError();
            System.err.println("回放失败" + iErr);
            return -1;
        }
        this.key=iPlayBack;

        sdk.NET_DVR_PlayBackControl(iPlayBack, HCNetSDK.NET_DVR_PLAYSTART, 0, null);
        sdk.NET_DVR_PlayBackControl(iPlayBack, HCNetSDK.NET_DVR_SETSPEED,
                256, null);
        System.out.println(123);
        sdk.NET_DVR_SetPlayDataCallBack_V40(iPlayBack,playbackDataCallBack,Pointer.NULL);
        System.out.println(456);


        return userId;
    }

    public void logoutHIK(){
        System.out.println("退出...");
        sdk.NET_DVR_StopPlayBack(key);
        sdk.NET_DVR_Logout(userId);
    }



    class PlaybackDataCallBack implements HCNetSDK.FPlayDataCallBack {
        //多路视频的pes数据进行缓存，知道某一路视频的RTP包开头进入时进行取出返给前端
        Map<String, byte[]> EsBytesMap = new HashMap<>();
        PrintStream ps = new PrintStream("C:\\Users\\ASUS\\Desktop\\log.txt");

        PlaybackDataCallBack() throws FileNotFoundException {
        }


        @Override
        public void invoke(int lPlayHandle, int dwDataType, Pointer pBuffer, int dwBufSize, int dwUser) throws IOException {
            switch (dwDataType) {
                case HCNetSDK.NET_DVR_SYSHEAD: //系统头
                case HCNetSDK.NET_DVR_STREAMDATA: //码流数据
                    if ((dwBufSize > 0)) {

                        byte[] outputData = pBuffer.getByteArray(0, dwBufSize);
//                        System.setOut(ps);
//                        System.out.println("***************************************\n");
//                        System.out.println(ConverterUtil.byteToHexString(outputData));
//
//                        System.out.println("***************************************\n");

                        try {
                            if (PlaybackSocket.webSocketSet.size() > 0) {  //提取H264的裸流
//                                System.out.println(dwBufSize);
                                writeESH264(outputData, lPlayHandle,userId);//将流写入对应的实体
                            }
                        } catch (IOException e) {
                            System.err.println("推流失败");
                            e.printStackTrace();
                        }
                    }
            }
        }

        /** 提取H264的裸流写入文件 */
        public void writeESH264(final byte[] outputData,int lRealHandle,int userId) throws IOException {
            byte[] allEsBytes = EsBytesMap.get(userId+"");
            if (outputData.length <= 0) { return ; }
            int start=0;
            int end=0;
            for(int i=0;i<outputData.length-3;i++){
                if ((outputData[i] & 0xff) == 0x00 && (outputData[i+1] & 0xff) == 0x00 && (outputData[i+2] & 0xff) == 0x01 && (outputData[i+3] & 0xff) == 0xBA){
                    //找到帧开头
                    //先在该帧之前的部分找pes包
                    end=i-3;
                    //包的长度

                    for(int j=start;j<end;j++){
                        if ((outputData[j] & 0xff) == 0x00 && (outputData[j+1] & 0xff) == 0x00 && (outputData[j+2] & 0xff) == 0x01 && (outputData[j+3] & 0xff) == 0xE0) {
                            long length=(outputData[j+4]) *256 + outputData[j+5]*16 +6;
                            // 去掉包头后的起始位置
                            int from = 9 + outputData[j+8] & 0xff;
                            int len =(int) (length - 9 - (outputData[j+8] & 0xff));
                            // 获取es裸流
                            System.out.println("ESSSSSSSSSSSSSSSS");
                            byte[] esBytes = new byte[len];
                            System.arraycopy(outputData, from, esBytes, 0, len);
                            if (allEsBytes == null) {
                                allEsBytes = esBytes;
                            } else {
                                byte[] newEsBytes = new byte[allEsBytes.length + esBytes.length];
                                System.arraycopy(allEsBytes, 0, newEsBytes, 0, allEsBytes.length);
                                System.arraycopy(esBytes, 0, newEsBytes, allEsBytes.length, esBytes.length);
                                allEsBytes = newEsBytes;
                            }
                            EsBytesMap.put(userId+"",allEsBytes);
                        }
                    }
                    EsBytesMap.put(userId+"",null);
                    System.out.println(112233);
                    //一帧数据找到后就提交
                    for (PlaybackSocket webSocket : PlaybackSocket.webSocketSet) {
                        if (webSocket.isTrue) {
                            if (webSocket.session.isOpen()) {
                                //开始推流解码
                                System.out.println("推流");
                                webSocket.session.getBasicRemote().sendBinary(ByteBuffer.wrap(allEsBytes));
                            }
                        }
                    }
                    if(i+4<outputData.length){
                        start=i+4;
                    }else{
                        return ;
                    }



                }
            }
            //获取
            for(int i=end;i<outputData.length;i++){
                if ((outputData[i] & 0xff) == 0x00 && (outputData[i+1] & 0xff) == 0x00 && (outputData[i+2] & 0xff) == 0x01 && (outputData[i+3] & 0xff) == 0xE0) {
                    long length=(outputData[i+4]) *256 + outputData[i+5]*16 +6;
                    // 去掉包头后的起始位置
                    int from = 9 + outputData[i+8];
                    int len =(int) (length - 9 - (outputData[i+8]));
                    System.out.println(len);
                    // 获取es裸流
                    byte[] esBytes = new byte[len];
                    System.arraycopy(outputData, from, esBytes, 0, len);
                    if (allEsBytes == null) {
                        allEsBytes = esBytes;
                    } else {
                        byte[] newEsBytes = new byte[allEsBytes.length + esBytes.length];
                        System.arraycopy(allEsBytes, 0, newEsBytes, 0, allEsBytes.length);
                        System.arraycopy(esBytes, 0, newEsBytes, allEsBytes.length, esBytes.length);
                        allEsBytes = newEsBytes;
                    }
                    EsBytesMap.put(userId+"",allEsBytes);
                }
            }

        }

    }


}
