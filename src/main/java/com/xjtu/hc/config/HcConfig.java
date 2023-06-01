package com.xjtu.hc.config;


import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.xjtu.hc.entities.HcAuthorization;
import com.xjtu.hc.hcservice.HCNetSDK;

import com.xjtu.hc.hcservice.PlayCtrl;
import com.xjtu.hc.utils.osSelect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.swing.*;

@Configuration
public class HcConfig {


    public static HCNetSDK hCNetSDK = null;
    public static PlayCtrl playControl = null;

    static HcConfig.FExceptionCallBack_Imp fExceptionCallBack;

    static class FExceptionCallBack_Imp implements HCNetSDK.FExceptionCallBack {
        public void invoke(int dwType, int lUserID, int lHandle, Pointer pUser) {
            System.out.println("异常事件类型:"+dwType);
            return;
        }
    }


    /**
     * 动态库加载
     *
     * @return
     */
    private static boolean createSDKInstance() {
        if (hCNetSDK == null) {
            synchronized (HCNetSDK.class) {
                String strDllPath = "";
                try {
                    if (osSelect.isWindows())
                        //win系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "\\lib1\\windows\\HCNetSDK.dll";

                    else if (osSelect.isLinux())
                        //Linux系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "/lib1/linux/libhcnetsdk.so";
                    hCNetSDK = (HCNetSDK) Native.loadLibrary(strDllPath, HCNetSDK.class);
                } catch (Exception ex) {
                    System.out.println("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 播放库加载
     *
     * @return
     */
    private static boolean createPlayInstance() {
        if (playControl == null) {
            synchronized (PlayCtrl.class) {
                String strPlayPath = "";
                try {
                    if (osSelect.isWindows())
                        //win系统加载库路径
                        strPlayPath = System.getProperty("user.dir") + "\\lib1\\windows\\PlayCtrl.dll";
                    else if (osSelect.isLinux())
                        //Linux系统加载库路径
                        strPlayPath = System.getProperty("user.dir") + "/lib1/linux/libPlayCtrl.so";
                    playControl=(PlayCtrl) Native.loadLibrary(strPlayPath,PlayCtrl.class);

                } catch (Exception ex) {
                    System.out.println("loadLibrary: " + strPlayPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }




    @PostConstruct
    public void init(){
        if (hCNetSDK == null) {
            if (!createSDKInstance()) {
                System.err.println("海康sdk加载失败");
                return;
            }
        }
        if( playControl == null){
            if (!createPlayInstance()) {
                System.err.println("视频组件库加载失败");
                return;
            }
        }

        //linux系统建议调用以下接口加载组件库
        if (osSelect.isLinux()) {
            HCNetSDK.BYTE_ARRAY ptrByteArray1 = new HCNetSDK.BYTE_ARRAY(256);
            HCNetSDK.BYTE_ARRAY ptrByteArray2 = new HCNetSDK.BYTE_ARRAY(256);
            //这里是库的绝对路径，请根据实际情况修改，注意改路径必须有访问权限
            String strPath1 = System.getProperty("user.dir") + "/lib1/linux/libcrypto.so.1.1";
            String strPath2 = System.getProperty("user.dir") + "/lib1/linux/libssl.so.1.1";

            System.arraycopy(strPath1.getBytes(), 0, ptrByteArray1.byValue, 0, strPath1.length());
            ptrByteArray1.write();
            hCNetSDK.NET_DVR_SetSDKInitCfg(3, ptrByteArray1.getPointer());

            System.arraycopy(strPath2.getBytes(), 0, ptrByteArray2.byValue, 0, strPath2.length());
            ptrByteArray2.write();
            hCNetSDK.NET_DVR_SetSDKInitCfg(4, ptrByteArray2.getPointer());

            String strPathCom = System.getProperty("user.dir") + "/lib1/linux";
            HCNetSDK.NET_DVR_LOCAL_SDK_PATH struComPath = new HCNetSDK.NET_DVR_LOCAL_SDK_PATH();
            System.arraycopy(strPathCom.getBytes(), 0, struComPath.sPath, 0, strPathCom.length());
            struComPath.write();
            hCNetSDK.NET_DVR_SetSDKInitCfg(2, struComPath.getPointer());
        }

        //SDK初始化，一个程序只需要调用一次
        boolean initSuc = hCNetSDK.NET_DVR_Init();

        //异常消息回调
        if(fExceptionCallBack == null)
        {
            fExceptionCallBack = new HcConfig.FExceptionCallBack_Imp();
        }
        Pointer pUser = null;
        if (!hCNetSDK.NET_DVR_SetExceptionCallBack_V30(0, 0, fExceptionCallBack, pUser)) {
            return ;
        }
        System.out.println("设置异常消息回调成功");


        if (!initSuc) {
            System.err.println("初始化失败........................");
        }



    }

    public static long register(HcAuthorization authorization){
        HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        long userID = hCNetSDK.NET_DVR_Login_V30(authorization.getIp(),(short) authorization.getPort(),authorization.getUserName(),authorization.getPassword(),m_strDeviceInfo);
        if (userID == -1) {
            System.err.println("注册失败");
            System.err.println("错误码："+hCNetSDK.NET_DVR_GetLastError());
        }else{
            System.out.println("注册成功");
        }
        return userID;
    }





}
