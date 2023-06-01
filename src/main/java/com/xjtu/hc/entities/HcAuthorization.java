package com.xjtu.hc.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HcAuthorization {
    private String ip;
    private int port;
    private String userName;
    private String password;
    private int channel;

    @Override
    public String toString() {
        return "HcAuthorization{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", channel=" + channel +
                '}';
    }
}
