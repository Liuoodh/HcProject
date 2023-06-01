package com.xjtu.hc.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.util.WebAppRootListener;

import javax.servlet.ServletContext;

/**
 * WebSocket缓冲
 */
@Configuration
public class WebSocketConfig implements ServletContextInitializer {
    @Bean
    public ServerEndpointExporter serverEndpointExporter(){
        return new ServerEndpointExporter ();
    }

    //设置websocket发送内容长度
    @Override
    public void onStartup(ServletContext servletContext){
        servletContext.addListener(WebAppRootListener.class);
        //这里设置了30兆的缓冲区
        //Tomcat每次请求过来时在创建session时都会把这个webSocketContainer作为参数传进去所以对所有的session都生效了
        servletContext.setInitParameter("org.apache.tomcat.websocket.textBufferSize","30000000");
        servletContext.setInitParameter("org.apache.tomcat.websocket.binaryBufferSize","30000000");
    }
}
