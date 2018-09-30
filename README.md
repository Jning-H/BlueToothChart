
```
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <!--6.0以上扫描蓝牙需要地理位置权限-->
    <uses-feature android:name="android.hardware.location.gps" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```

![蓝牙数据传输](https://upload-images.jianshu.io/upload_images/9414344-24d61b6af7565755.gif?imageMogr2/auto-orient/strip)

---

>常量类
```
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

```


#### 一、主页面MainActivity

```
public class MainActivity extends PermissionActivity {

    @BindView(R.id.id_tv_msg)
    EditText mIdTvMsg;
    @BindView(R.id.id_btn_send)
    Button mIdBtnSend;
    @BindView(R.id.id_tv_info)
    TextView mIdTvInfo;
    private BluetoothAdapter mBtAdapter;
    private ConnectManager mConnectManager;
    private Context mContext = this;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Cons.MSG_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // 创建要发送的信息的字符串
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    ToastUtil.showAtOnce(mContext, readMessage);
                    mIdTvInfo.append(readMessage + "\n");
                    break;
                case Cons.MSG_DEVICE_NAME:
                    // 获取已连接的设备名称，并弹出提示信息
                    String deviceName = msg.getData().getString(Cons.DEVICE_NAME);
                    ToastUtil.showAtOnce(mContext, "已连接到 " + deviceName);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        //1.获取蓝牙适配器
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        //动态权限申请
        applyPermissions(_ACCESS_COARSE_LOCATION());
        useFab(this);
    }

    @Override
    protected void permissionOk() {
        ToastUtil.showAtOnce(this,"欢迎");
    }

    @Override
    protected void onStart() {
        setTitleText();
        super.onStart();
        //2.蓝牙不可用时
        if (!mBtAdapter.isEnabled()) {
            ToastUtil.showAtOnce(this, "请先开启蓝牙");
            Intent open = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(open, Cons.OPEN_BLUETOOTH);
        } else {
            setBtInfo();
            //服务为空时，新建服务
            if (mConnectManager == null) {
                mConnectManager = new ConnectManager(mHandler);
            }
        }
    }

    @OnClick(R.id.id_btn_send)
    public void onViewClicked() {
        String msg = mIdTvMsg.getText().toString();
        sendMsg(msg);
        mIdTvMsg.setText("");
    }

    /**
     * 发送消息方法
     *
     * @param msg
     */
    private void sendMsg(String msg) {
        // 先检查是否已经连接到设备
        if (mConnectManager.getState() != ConnectManager.BTState.CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        if (msg.length() > 0) {// 如果消息不为空再发送消息
            byte[] send = msg.getBytes();// 获取发送消息的字节数组，并发送
            mConnectManager.write(send);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Cons.OPEN_BLUETOOTH:
                    ToastUtil.showAtOnce(this, "蓝牙打开成功!");
                    setBtInfo();
                    break;
                case Cons.CONNECT_BLUETOOTH://连接成功
                    // 获取设备的MAC地址
                    String address = data.getExtras().getString(Cons.EXTRA_DEVICE_ADDR);
                    mIdTvInfo.append("\n" + address);
                    // 获取BLuetoothDevice对象
                    BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
                    mConnectManager.connect(device);// 连接该设备
                    break;
            }
        }
    }

    private void setBtInfo() {
        StringBuffer sb = new StringBuffer();
        sb.append("蓝牙名称:").append(mBtAdapter.getName()).append("\n")
                .append("蓝牙地址:").append(mBtAdapter.getAddress()).append("\n");
        mIdTvInfo.setText(sb.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mConnectManager != null) {
            //Service状态为空
            if (mConnectManager.getState() == ConnectManager.BTState.NONE) {
                mConnectManager.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnectManager != null) {
            mConnectManager.cancel();
        }
    }

    /**
     * 根据手机品牌设置标题名
     */
    private void setTitleText() {
        switch (Build.MODEL) {
            case "A31u":
                setTitle("龙少");
                break;
            case "OPPO R9tm":
                setTitle("捷特");
                break;
        }
    }

    /**
     * 进入蓝牙搜索页的按钮
     * @param activity
     */
    private void useFab(final Activity activity) {
        final FloatingActionButton fab = findViewById(R.id.fab);
        //点击
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent serverIntent = new Intent(activity, DeviceListActivity.class);
                startActivityForResult(serverIntent, Cons.CONNECT_BLUETOOTH);
            }
        });
    }
}

```

#### 二、列表页

```
public class DeviceListActivity extends Activity implements OnItemClickListener {

    public Context mContext = this;
    //蓝牙适配器
    private BluetoothAdapter myBtAdapter;
    //ListView已配对设备
    private ArrayAdapter<String> myAdapterPaired;
    //ListView未配对设备
    private ArrayAdapter<String> myAdapterNew;
    //加载进度条
    private ILoading mLoading;
    //新设备最上面的标题
    private TextView mTvNewDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);
        // 设置为当结果是Activity.RESULT_CANCELED时，返回到该Activity的调用者
        setResult(Activity.RESULT_CANCELED);
        mLoading = new LoadingImpl(this);

        // 初始化搜索按钮
        Button scanBtn = findViewById(R.id.button_scan);
        scanBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                doSearch(view);//开始搜索设备
            }
        });
        handleNew();//处理扫表到的设备列表
        registeBT();//注册广播
        handlePaired();//处理已经匹配的设备列表
    }

    /**
     * 处理已经匹配的设备列表
     */
    private void handleNew() {
        mTvNewDevices = findViewById(R.id.title_new_devices);
        // 新发现的设备适配器
        myAdapterNew = new ArrayAdapter<>(this, R.layout.device_name);
        // 将新发现的设备放入列表中
        ListView lvNewDevices = findViewById(R.id.new_devices);
        lvNewDevices.setAdapter(myAdapterNew);
        lvNewDevices.setOnItemClickListener(this);
    }

    /**
     * 开始搜索设备
     *
     * @param view
     */
    private void doSearch(View view) {
        // 使用蓝牙适配器搜索设备的方法
        if (myBtAdapter.isDiscovering()) {
            // 如果正在搜索，取消本次搜索
            myBtAdapter.cancelDiscovery();
        }
        myBtAdapter.startDiscovery();// 开始搜索
        view.setVisibility(View.GONE);// 使按钮不可见
        mLoading.show(ResUtils.getString(mContext, R.string.scanning));
    }

    /**
     * 处理已经匹配的设备列表
     */
    private void handlePaired() {
        // 已配对的设备适配器
        myAdapterPaired = new ArrayAdapter<>(this, R.layout.device_name);
        // 将已配对的设备放入列表中
        ListView lvPaired = findViewById(R.id.paired_devices);
        lvPaired.setAdapter(myAdapterPaired);
        lvPaired.setOnItemClickListener(this);

        // 获取本地蓝牙适配器
        myBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // 获取已配对的设备
        Set<BluetoothDevice> pairedDevices = myBtAdapter.getBondedDevices();
        // 将所有已配对设备信息放入列表中
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                myAdapterPaired.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            myAdapterPaired.add(noDevices);
        }
    }

    /**
     * 注册广播
     */
    private void registeBT() {
        // 注册发现设备时的广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        // 注册搜索完成时的广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myBtAdapter != null) {// 确保不再搜索设备
            myBtAdapter.cancelDiscovery();
        }
        // 取消广播监听器
        this.unregisterReceiver(mReceiver);
    }

    // 监听搜索到的设备的BroadcastReceiver
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 如果找到设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                findOk(intent);

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                noFind(); //未发现新设备
            }
        }
    };

    /**
     * 未发现新设备
     */
    private void noFind() {
        mLoading.hide();
        if (myAdapterNew.getCount() == 0) {
            String noDevices = getResources().getText(
                    R.string.none_found).toString();
            myAdapterNew.add(noDevices);
        }
    }

    /**
     * 发现新设备
     * @param intent
     */
    private void findOk(Intent intent) {
        // 从Intent中获取BluetoothDevice对象
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        // 将扫描到设备加入新设备列表
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            myAdapterNew.add(device.getName() + "\n" + device.getAddress());
            mTvNewDevices.setVisibility(View.VISIBLE);
            mLoading.hide();
        }
    }

    //列表中设备按下时的监听器
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        myBtAdapter.cancelDiscovery();// 取消搜索
        // 获取设备的MAC地址
        String msg = ((TextView) view).getText().toString();
        String address = msg.substring(msg.length() - 17);
        // 创建带有MAC地址的Intent
        Intent intent = new Intent();
        intent.putExtra(Cons.EXTRA_DEVICE_ADDR, address);
        // 设备结果并退出Activity
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
```
#### 三、蓝牙连接管理器
```
//蓝牙连接管理器
public class ConnectManager {
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
    public ConnectManager(Handler handler) {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        mCurState = BTState.NONE;
        myHandler = handler;
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
    public synchronized void sendMsgForConnected(BluetoothSocket socket, BluetoothDevice device) {
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
    public void write(byte[] out) {//向ConnectedThread
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

        public ServiceThread() {
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
                    synchronized (ConnectManager.this) {
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

        public ConnectThread(BluetoothDevice device) {
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
                ConnectManager.this.start();//如果连接不成功，重新开启service
                return;
            }
            synchronized (ConnectManager.this) {// 将ConnectThread线程置空
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

        public IOThread(BluetoothSocket socket) {
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
        public void write(byte[] buffer) {
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

```

