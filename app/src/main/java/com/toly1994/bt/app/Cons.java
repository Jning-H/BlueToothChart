package com.toly1994.bt.app;

public class Cons {
    //自定义请求码：卡开蓝牙
    public static final int OPEN_BLUETOOTH = 0x01;
    //自定义请求码：连接蓝牙
    public static final int CONNECT_BLUETOOTH = 0x02;

    // 由管理器中的Handler发送的消息类型
    public static final int MSG_READ = 2;
    public static final int MSG_DEVICE_NAME = 4;

    // 从管理器中的Handler发来的主键名
    public static final String DEVICE_NAME = "device_name";
    //单击连接时intent携带数据的键值
    public static final String EXTRA_DEVICE_ADDR = "device_address";
}
