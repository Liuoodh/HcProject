package com.xjtu.hc.hcservice;

import com.xjtu.hc.entities.HcAuthorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint("/webSocket/{ip}/{port}/{u}/{p}/{c}")
public class HcWebSocket {
    public Session session;         //与某个客户端的连接会话，需要通过它来给客户端发送数据
    public boolean isTrue = false;  //true:表示可以开始推流  false: 停止推流
    public static CopyOnWriteArraySet<HcWebSocket> webSocketSet = new CopyOnWriteArraySet<HcWebSocket>(); //concurrent包的线程安全Set,用来存放每个客户端对应的MyWebSocket对象


    private VideoPreview videoPreview=new VideoPreview();

    /*** 连接建立成功调用的方法 */
    @OnOpen
    public void onOpen(@PathParam("ip") String ip , @PathParam("port") short port , @PathParam("u") String u, @PathParam("p") String p,@PathParam("c") int c, Session session){
        HcAuthorization hcAuthorization = new HcAuthorization(ip, port, u, p, c);
        this.session=session;
        this.isTrue = true;
        int userId = videoPreview.videoPreview(hcAuthorization);
        if(!(userId <= -1)){
            webSocketSet.add(this);
        }
    }

    /*** 连接关闭调用的方法  */
    @OnClose
    public void onClose(){
        this.isTrue = false;
        webSocketSet.remove(this); //从set中删除
        videoPreview.logoutHIK();
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
        for (HcWebSocket webSocket:webSocketSet) {
            System.out.println("【webSocket消息】广播消息,message="+message);
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
