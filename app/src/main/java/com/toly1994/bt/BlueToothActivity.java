package com.toly1994.bt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.toly1994.bt.app.Cons;
import com.toly1994.bt.manage.BTManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import top.toly.zutils.core.base.PermissionActivity;
import top.toly.zutils.core.shortUtils.ToastUtil;

public class BlueToothActivity extends PermissionActivity {

    @BindView(R.id.id_tv_msg)
    EditText mIdTvMsg;
    @BindView(R.id.id_btn_send)
    Button mIdBtnSend;
    @BindView(R.id.id_tv_info)
    TextView mIdTvInfo;
    private BluetoothAdapter mBtAdapter;

    private BTManager mBTManager;
    private Context mContext = this;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Cons.MSG_READ:
                    // 创建要发送的信息的字符串
                    String readMessage = new String((byte[]) msg.obj, 0, msg.arg1);
                    handleMsg(readMessage);
                    break;
                case Cons.MSG_DEVICE_NAME:
                    // 获取已连接的设备名称，并弹出提示信息
                    String deviceName = msg.getData().getString(Cons.DEVICE_NAME);
                    ToastUtil.showAtOnce(mContext, "已连接到 " + deviceName);
                    break;
            }
        }
    };

    /**
     * 处理另一个客户端传来的数据
     * @param readMessage
     */
    private void handleMsg(String readMessage) {
        ToastUtil.showAtOnce(mContext, readMessage);
        mIdTvInfo.append(readMessage + "\n");
    }

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
        super.onStart();
        //2.蓝牙不可用时
        if (!mBtAdapter.isEnabled()) {
            ToastUtil.showAtOnce(this, "请先开启蓝牙");
            Intent open = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(open, Cons.OPEN_BLUETOOTH);
        } else {
            //服务为空时，新建服务
            if (mBTManager == null) {
                mBTManager = new BTManager(mHandler);
            }
        }
    }

    @OnClick(R.id.id_btn_send)
    public void onViewClicked() {
        String msg = mIdTvMsg.getText().toString();
        mBTManager.sendMsg(msg);
        mIdTvMsg.setText("");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Cons.OPEN_BLUETOOTH:
                    ToastUtil.showAtOnce(this, "蓝牙打开成功!");
                    break;
                case Cons.CONNECT_BLUETOOTH://连接成功
                    // 获取设备的MAC地址
                    String address = data.getExtras().getString(Cons.EXTRA_DEVICE_ADDR);
                    // 获取BLuetoothDevice对象
                    BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
                    mBTManager.connect(device);// 连接该设备
                    break;
            }
        }
    }

    @Override
    protected synchronized void onResume() {
        super.onResume();
        if (mBTManager != null) {
            //Service状态为空
            if (mBTManager.getState() == BTManager.BTState.NONE) {
                mBTManager.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBTManager != null) {
            mBTManager.cancel();
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
