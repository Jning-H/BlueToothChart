package com.toly1994.bt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.toly1994.bt.app.Cons;
import com.toly1994.bt.helper.ILoading;
import com.toly1994.bt.helper.LoadingImpl;

import java.util.Set;

import top.toly.zutils.core.ui.common.ResUtils;

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
