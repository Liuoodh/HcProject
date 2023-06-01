package com.xjtu.hc.hcservice;

import com.sun.jna.ptr.IntByReference;
import com.xjtu.hc.config.HcConfig;
import com.xjtu.hc.entities.HcAuthorization;
import com.xjtu.hc.utils.CommonUtil;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;
import java.util.Date;


@Service
public class VideoDownload {


        private final HCNetSDK hcNetSDK=HcConfig.hCNetSDK;


        private int userId;//用户句柄
        private int loadHandle;//下载句柄

        /**
         *按时间下载视频
         *
         * @return
         */
        public boolean downloadVideo(HcAuthorization authorization, Date startTime, Date endTime, String filePath) {

            long lUserId = HcConfig.register(authorization);

            if(lUserId==-1) {
                System.err.println("hksdk(视频)-海康sdk登录失败!");
                return false;
            }
            userId=(int) lUserId;
            loadHandle =-1;
            if(loadHandle==-1) {
                loadHandle = hcNetSDK.NET_DVR_GetFileByTime(userId,authorization.getChannel(), CommonUtil.getHkTime(startTime),CommonUtil.getHkTime(endTime),filePath);
                System.out.println("hksdk(视频)-获取播放句柄信息,状态值:"+hcNetSDK.NET_DVR_GetLastError());

                if(loadHandle>=0) {
                    boolean downloadFlag = hcNetSDK.NET_DVR_PlayBackControl(loadHandle,hcNetSDK.NET_DVR_PLAYSTART,0,null);
                    int tmp = -1;
                    IntByReference pos = new IntByReference();
                    while(true) {
                        boolean backFlag = hcNetSDK.NET_DVR_PlayBackControl(loadHandle,hcNetSDK.NET_DVR_PLAYGETPOS,0,pos);
                        if(!backFlag) {
                            return downloadFlag;
                        }
                        int produce =pos.getValue();
                        if((produce%10)==0&&tmp!=produce) {//输出进度
                            tmp = produce;
                            System.out.println("hksdk(视频)-视频下载进度:"+"=="+produce+"%");
                        }
                        if(produce ==100) {//下载成功
                            System.out.println("下载成功！");
                            hcNetSDK.NET_DVR_StopGetFile(loadHandle);
                            loadHandle=-1;
                            hcNetSDK.NET_DVR_Logout(userId);//退出录像机
                            System.out.println("hksdk(视频)-退出状态"+hcNetSDK.NET_DVR_GetLastError());
                            //hcNetSDK.NET_DVR_Cleanup();
                            return true;
                        }
                        if(produce>100) {//下载失败
                            hcNetSDK.NET_DVR_StopGetFile(loadHandle);
                            loadHandle=-1;
                            System.err.println("hksdk(视频)-海康sdk由于网络原因或DVR忙,下载异常终止!错误原因:"+ hcNetSDK.NET_DVR_GetLastError());
                            hcNetSDK.NET_DVR_Logout(userId);//退出录像机
                            //logger.info("hksdk(视频)-退出状态"+hcNetSDK.NET_DVR_GetLastError());
                            return false;
                        }
                    }
                }else{
                    System.out.println("hksdk(视频)-下载失败" + hcNetSDK.NET_DVR_GetLastError());
                    return false;
                }
            }
            return false;
        }




}
