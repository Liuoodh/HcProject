package com.xjtu.hc.hcservice;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.xjtu.hc.config.HcConfig;
import com.xjtu.hc.entities.HcAuthorization;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@Service
public class VideoPreview {
    private final HCNetSDK sdk= HcConfig.hCNetSDK;
    private int userId;
    private int key;
    static FRealDataCallBack fRealDataCallBack;//预览回调函数实现



    public int videoPreview(HcAuthorization authorization){
        long lUserId = HcConfig.register(authorization);


        if(lUserId==-1) {
            System.err.println("hksdk(视频)-海康sdk登录失败!");
            return -1;
        }
        userId=(int)lUserId;
        HCNetSDK.NET_DVR_PREVIEWINFO strClientInfo = new HCNetSDK.NET_DVR_PREVIEWINFO();
        strClientInfo.read();
        strClientInfo.hPlayWnd = 0;  //窗口句柄，从回调取流不显示一般设置为空
        strClientInfo.lChannel = authorization.getChannel();  //通道号
        strClientInfo.dwStreamType=0; //0-主码流，1-子码流，2-三码流，3-虚拟码流，以此类推
        strClientInfo.dwLinkMode=0; //连接方式：0- TCP方式，1- UDP方式，2- 多播方式，3- RTP方式，4- RTP/RTSP，5- RTP/HTTP，6- HRUDP（可靠传输） ，7- RTSP/HTTPS，8- NPQ
        strClientInfo.bBlocked=1;
        strClientInfo.write();

        //回调函数定义必须是全局的
        if (fRealDataCallBack == null) {
            fRealDataCallBack = new FRealDataCallBack();
        }

        //开启预览
        int lPlay = sdk.NET_DVR_RealPlay_V40(userId, strClientInfo, fRealDataCallBack , null);
        if (lPlay == -1) {
            int iErr = sdk.NET_DVR_GetLastError();
            System.err.println("取流失败" + iErr);
            return -1;
        }
        this.key=lPlay;
        System.out.println("取流成功");

        return userId;
    }

    public void logoutHIK(){
        System.out.println("退出...");
        sdk.NET_DVR_StopRealPlay(key);
        sdk.NET_DVR_Logout(userId);
    }



    class FRealDataCallBack implements HCNetSDK.FRealDataCallBack_V30 {
        //多路视频的pes数据进行缓存，知道某一路视频的RTP包开头进入时进行取出返给前端
        Map<String, byte[]> EsBytesMap = new HashMap<>();

        //预览回调
        @Override
        public void invoke(int lRealHandle, int dwDataType, ByteByReference pBuffer, int dwBufSize, Pointer pUser) {
            switch (dwDataType) {
                case HCNetSDK.NET_DVR_SYSHEAD: //系统头
                case HCNetSDK.NET_DVR_STREAMDATA: //码流数据
                    if ((dwBufSize > 0)) {
                        byte[] outputData = pBuffer.getPointer().getByteArray(0, dwBufSize);
                        try {
                            if (HcWebSocket.webSocketSet.size() > 0) {  //提取H264的裸流
                                byte[] bytes = writeESH264(outputData, lRealHandle,userId);//将流写入对应的实体
                                if (bytes != null) {
                                    for (HcWebSocket webSocket : HcWebSocket.webSocketSet) {
                                        if (webSocket.isTrue) {
                                            if (webSocket.session.isOpen()) {
                                                //开始推流解码
                                                webSocket.session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        /**
                         * 录像可以在这里保存
                         */
                        //outputStreamMap.get(lRealHandle).write(outputData);
                    }
            }

        }

        /** 提取H264的裸流写入文件 */
        public byte[] writeESH264(final byte[] outputData,int lRealHandle,int userId) throws IOException {
            byte[] allEsBytes = EsBytesMap.get(userId+"");
            if (outputData.length <= 0) { return null; }
            if ((outputData[0] & 0xff) == 0x00 && (outputData[1] & 0xff) == 0x00 && (outputData[2] & 0xff) == 0x01 && (outputData[3] & 0xff) == 0xBA) {// RTP包开头
                // 一个完整的帧解析完成后将解析的数据放入BlockingQueue,websocket获取后发生给前端
                EsBytesMap.put(userId+"",null);
                return allEsBytes ;
            }
            // 是00 00 01 eo开头的就是视频的pes包
            if ((outputData[0] & 0xff) == 0x00 && (outputData[1] & 0xff) == 0x00 && (outputData[2] & 0xff) == 0x01 && (outputData[3] & 0xff) == 0xE0) {
                // 去掉包头后的起始位置
                int from = 9 + outputData[8] & 0xff;
                int len = outputData.length - 9 - (outputData[8] & 0xff);
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
            return null;
        }

    }


























}
