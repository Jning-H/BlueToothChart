package com.toly1994.bt.manage;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.toly1994.bt.app.Cons;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

//蓝牙连接管理器
public class BTManager {
    /**
     * 连接状态的枚举
     */
    public enum BTState {
        NONE,
        LISTENING,//正在监听连接
        CONNECTING,//正在连接
        CONNECTED,// 已连接
    }

    //UUID--按格式随便写，保证两个手机安装的是一个安装包即可
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c8a66");
    // 成员变量
    private final BluetoothAdapter btAdapter;
    private final Handler myHandler;

    /**
     * 蓝牙服务线程
     */
    private ServiceThread mServiceThread;
    /**
     * 用于连接的线程
     */
    private ConnectThread mConnThread;
    /**
     * 连接后移数据传输的线程
     */
    private IOThread mIOThread;

    /**
     * 当前连接状态
     */
    private BTState mCurState;


    // 构造器
    public BTManager(Handler handler) {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        mCurState = BTState.NONE;
        myHandler = handler;
    }

    /**
     * 发送消息方法
     *
     * @param msg
     */
    public void sendMsg(String msg) {
        // 先检查是否已经连接到设备
        if (getState() != BTManager.BTState.CONNECTED) {
            return;
        }
        if (msg.length() > 0) {// 如果消息不为空再发送消息
            byte[] send = msg.getBytes();// 获取发送消息的字节数组，并发送
            write(send);
        }
    }

    /**
     * 设置当前连接状态
     *
     * @param state 连接状态
     */
    private synchronized void setState(BTState state) {
        mCurState = state;
    }

    /**
     * 获取当前连接状态
     *
     * @return 连接状态
     */
    public synchronized BTState getState() {
        return mCurState;
    }


    /**
     * 开启连接
     */
    public synchronized void start() {
        // 关闭不必要的线程
        cancelThread(mConnThread, mIOThread);

        // 开启蓝牙服务线程用于监听设备
        if (mServiceThread == null) {
            mServiceThread = new ServiceThread();
            mServiceThread.start();
        }
        //将状态改为监听中
        setState(BTState.LISTENING);
    }

    /**
     * 连接设备的方法
     *
     * @param device 蓝牙设备
     */
    public synchronized void connect(BluetoothDevice device) {
        // 如果状态已经是正在连接，取消连接线程(如果存在)
        if (mCurState == BTState.CONNECTING) {
            cancelThread(mConnThread);
        }
        //取消数据传输线程(如果存在)
        cancelThread(mIOThread);

        // 开启线程连接设备
        mConnThread = new ConnectThread(device);
        mConnThread.start();
        //将状态改为连接中
        setState(BTState.CONNECTING);
    }

    //开启管理和已连接的设备间通话的线程的方法

    /**
     * 已连接完成后发送消息
     *
     * @param socket 蓝牙的套接字
     * @param device 蓝牙设备
     */
    private synchronized void sendMsgForConnected(BluetoothSocket socket, BluetoothDevice device) {
        // 关闭不必要的线程
        cancelThread(mConnThread, mServiceThread, mIOThread);
        // 创建并启动ConnectedThread
        mIOThread = new IOThread(socket);
        mIOThread.start();
        // 发送已连接的设备名称到主界面Activity
        Message msg = myHandler.obtainMessage(Cons.MSG_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Cons.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        myHandler.sendMessage(msg);
        //将状态改为已连接
        setState(BTState.CONNECTED);
    }

    /**
     * 停止所有线程的方法
     */
    public synchronized void cancel() {
        cancelThread(mConnThread, mServiceThread, mIOThread);
        setState(BTState.NONE);
    }

    /**
     * 关闭线程方法
     *
     * @param threads 线程
     */
    private void cancelThread(Cancelable... threads) {
        for (Cancelable thread : threads) {
            if (thread != null) {
                thread.cancel();
                thread = null;
            }
        }
    }

    /**
     * 数据传输线程写入数据
     *
     * @param out 输出的字节
     */
    private void write(byte[] out) {//向ConnectedThread
        IOThread tmpCt;// 创建临时对象引用
        synchronized (this) {// 锁定ConnectedThread
            if (mCurState != BTState.CONNECTED) {
                return;
            }
            tmpCt = mIOThread;
        }
        tmpCt.write(out);// 写入数据
    }

    /**
     * 用于监听连接的线程
     */
    private class ServiceThread extends Thread implements Cancelable {
        /**
         * 本地蓝牙服务器端
         */
        private final BluetoothServerSocket mServerSocket;

        private ServiceThread() {
            BluetoothServerSocket tmpSS = null;
            try {// 创建用于监听的服务器端ServerSocket
                tmpSS = btAdapter.listenUsingRfcommWithServiceRecord("BluetoothChat", MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mServerSocket = tmpSS;
        }

        public void run() {
            setName("ServiceThread");
            BluetoothSocket socket = null;
            while (mCurState != BTState.CONNECTED) {//如果没有连接到设备
                try {
                    socket = mServerSocket.accept();//获取连接的Sock
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                if (socket != null) {// 如果连接成功
                    synchronized (BTManager.this) {
                        switch (mCurState) {
                            case LISTENING:
                            case CONNECTING:
                                // 开启管理连接后数据交流的线程
                                sendMsgForConnected(socket, socket.getRemoteDevice());
                                break;
                            case NONE:
                            case CONNECTED:
                                try {// 关闭新Socket
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }
            }
        }

        @Override
        public void cancel() {

            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 用于连接其他设备的线程
     */
    private class ConnectThread extends Thread implements Cancelable {
        /**
         * 蓝牙套接字
         */
        private final BluetoothSocket myBtSocket;
        /**
         * 蓝牙设备
         */
        private final BluetoothDevice mDevice;

        private ConnectThread(BluetoothDevice device) {
            mDevice = device;
            BluetoothSocket tmpBS = null;
            // 通过正在连接的设备获取BluetoothSocket
            try {
                tmpBS = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            myBtSocket = tmpBS;
        }

        public void run() {
            setName("ConnectThread");
            btAdapter.cancelDiscovery();// 取消搜索设备
            try {// 连接到BluetoothSocket
                myBtSocket.connect();//尝试连接
            } catch (IOException e) {
                setState(BTState.LISTENING);//连接断开后设置状态为正在监听
                try {// 关闭socket
                    myBtSocket.close();
                } catch (IOException e2) {
                    e.printStackTrace();
                }
                BTManager.this.start();//如果连接不成功，重新开启service
                return;
            }
            synchronized (BTManager.this) {// 将ConnectThread线程置空
                mConnThread = null;
            }
            sendMsgForConnected(myBtSocket, mDevice);// 开启管理连接后数据交流的线程
        }

        @Override
        public void cancel() {
            try {
                myBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 用于管理连接后数据交流的线程
     */
    private class IOThread extends Thread implements Cancelable {
        private final BluetoothSocket myBtSocket;
        private final InputStream mmInStream;
        private final OutputStream myOs;

        private IOThread(BluetoothSocket socket) {
            myBtSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // 获取BluetoothSocket的输入输出流
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            myOs = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {// 一直监听输入流
                try {
                    bytes = mmInStream.read(buffer);// 从输入流中读入数据
                    //将读入的数据发送到主Activity
                    myHandler.obtainMessage(Cons.MSG_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    setState(BTState.LISTENING);//连接断开后设置状态为正在监听
                    break;
                }
            }
        }

        //向输出流中写入数据的方法
        private void write(byte[] buffer) {
            try {
                myOs.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void cancel() {
            try {
                myBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
