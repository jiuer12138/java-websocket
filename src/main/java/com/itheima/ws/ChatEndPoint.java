package com.itheima.ws;


import com.alibaba.fastjson.JSON;
import com.itheima.config.GetHttpSessionConfig;
import com.itheima.utils.MessageUtils;
import com.itheima.ws.pojo.Message;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat", configurator = GetHttpSessionConfig.class)
@Component
public class ChatEndPoint {

    private static final Map<String, Session> onlineUsers = new ConcurrentHashMap<>();

    private HttpSession httpSession;

    /**
     * 建立连接之后被调用
     *
     * @param session
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        //1.保存session
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        String user = (String) this.httpSession.getAttribute("user");
        onlineUsers.put(user, session);
        //2.广播消息，需要将登陆的所有的用户推送给所有的用户
        String message = MessageUtils.getMessage(true, null, getAllUsersName());
        broadcastAllUsers(message);
    }


    public Set<String> getAllUsersName() {
        return onlineUsers.keySet();
    }

    private void broadcastAllUsers(String message) {
        for (Map.Entry<String, Session> entry : onlineUsers.entrySet()) {
            Session session = entry.getValue();
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                //记录日志
            }
        }


    }

    /**
     * 浏览器发送消息到服务端
     *
     * @param message
     */
    @OnMessage
    public void onMessage(String message) {
        try {
            Message msg = JSON.parseObject(message, Message.class);
            String toName = msg.getToName();
            String toMsg = msg.getMessage();

            Session session = onlineUsers.get(toName);

            String user = (String) this.httpSession.getAttribute("user");
            String s = MessageUtils.getMessage(false, user, toMsg);
            session.getBasicRemote().sendText(s);
        } catch (IOException e) {
            //记录日志
        }

    }

    /**
     * 断开websocket连接出发
     *
     * @param session
     */
    @OnClose
    public void onClose(Session session) {

        //1.onlineUsers删除该用户对session
        String user = (String) this.httpSession.getAttribute("user");
        onlineUsers.remove(user);
        //2.通知其他用户，该用户下线
        String message = MessageUtils.getMessage(true, null, getAllUsersName());
        broadcastAllUsers(message);
    }

}
