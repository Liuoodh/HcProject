package com.xjtu.hc.hcservice;

import com.xjtu.hc.entities.HcAuthorization;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint("/webSocket/{ip}/{port}/{u}/{p}/{c}/{start}/{end}")
public class PlaybackSocket {
    public Session session;         //与某个客户端的连接会话，需要通过它来给客户端发送数据
    public boolean isTrue = false;  //true:表示可以开始推流  false: 停止推流
    public static CopyOnWriteArraySet<PlaybackSocket> webSocketSet = new CopyOnWriteArraySet<PlaybackSocket>(); //concurrent包的线程安全Set,用来存放每个客户端对应的MyWebSocket对象


    private final VideoPlayback videoPlayback=new VideoPlayback();

    /*** 连接建立成功调用的方法 */
    @OnOpen
    public void onOpen(@PathParam("ip") String ip , @PathParam("port") short port , @PathParam("u") String u, @PathParam("p") String p, @PathParam("c") int c,@PathParam("start") String start ,@PathParam("end") String end, Session session) throws FileNotFoundException {
        HcAuthorization hcAuthorization = new HcAuthorization(ip, port, u, p, c);

        this.session=session;
        this.isTrue = true;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date startTime =null;
        Date endTime = null;
        try {
            startTime = sdf.parse(start);   //开始时间
            endTime =   sdf.parse(end);      //结束时间
        } catch (ParseException e) {
            e.printStackTrace();
            System.err.println("时间格式错误");
            return;
        }
        int userId = videoPlayback.videoPlayBack(hcAuthorization,startTime,endTime );
        if(!(userId <= -1)){
            webSocketSet.add(this);
        }
    }

    /*** 连接关闭调用的方法  */
    @OnClose
    public void onClose(){
        this.isTrue = false;
        webSocketSet.remove(this); //从set中删除
        videoPlayback.logoutHIK();
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message ){
        System.out.println("【WebSocket消息】收到客户端发来的消息:"+message);
    }

    public void sendMessage(String message){
        for (PlaybackSocket webSocket:webSocketSet) {
            System.out.println("【PlaybackWebSocket消息】广播消息,message="+message);
            try { webSocket.session.getBasicRemote ().sendText(message); }
            catch (Exception e) { e.printStackTrace (); }
        }
    }

    /**
     * 配置错误信息处理
     * @param session
     * @param t
     */
    @OnError
    public void onError(Session session, Throwable t) {
        t.printStackTrace();
        System.err.println("【websocket消息】出现未知错误 ");
    }
}
