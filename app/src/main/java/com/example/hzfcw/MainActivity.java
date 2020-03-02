package com.example.hzfcw;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;

import androidx.appcompat.app.AppCompatActivity;

import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.github.lzyzsd.jsbridge.DefaultHandler;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String version = "1.0.1";
//    private String REQUEST_URL = "http://192.168.1.9:3001/index_android";
    private String REQUEST_URL = "https://cewen.pinwen.wang";
//    private String REQUEST_URL = "https://cewen.pinwen.wang";
    private String filePath ="file:////android_asset/index_android.html";
    private static final int REQUEST_ENABLE_BT = 0xa01;
    private final int PERMISSION_REQUEST_COARSE_LOCATION = 0xb01;
    private BroadcastReceiver mReceiver;

    private BluetoothAdapter mbluetoothAdapter;
    private BluetoothDevice currentDevice;
    private BluetoothGatt mBluetoothGatt;

    private String FORMAT_CONNECTED = "FORMAT_CONNECTED";
    private String FORMAT_DISCONNECTED = "FORMAT_DISCONNECTED";
    private String FORMAT_FOUND_DEVICE = "FORMAT_FOUND_DEVICE";
    private String FORMAT_SCAN_START = "FORMAT_SCAN_START";
    private String FORMAT_SCAN_FINISH = "FORMAT_SCAN_FINISH";

    private int STATE_CONNECTED = 1;
    private int STATE_DISCONNECTED = 0;

    private int SELECT_DEVICE = 1;
    private int NOT_SELECT_DEVICE = 0;

    private String temp;
    private List<String> listTemp = new ArrayList<>();
    private Map<String, Object> map;
    private HashMap<String, String> hashMap = new HashMap<>();//key-mac address val-name

    private Vibrator mVibrator;

    private BridgeWebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.goBack).setOnClickListener(this);
        findViewById(R.id.goForward).setOnClickListener(this);
        findViewById(R.id.reload).setOnClickListener(this);

        webview = findViewById(R.id.webview);

        mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);

        initConfigWebView();

        registerBroadcastReceiver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }


        webview.registerHandler("getVersion", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                function.onCallBack(version);
            }
        });

        webview.registerHandler("startDiscovery", new

                BridgeHandler() {
                    @Override
                    public void handler(String data, CallBackFunction callBackFn) {
                        hashMap = new HashMap<>();
                        System.out.println("js调用了startDiscovery");
                        startDiscovery();
                    }
                });

        webview.registerHandler("stopDiscovery", new

                BridgeHandler() {
                    @Override
                    public void handler(String data, CallBackFunction callBackFn) {
                        hashMap = new HashMap<>();
                        System.out.println("js调用了stopDiscovery");
                        stopDiscovery();
                    }
                });

        webview.registerHandler("connectBLE", new

                BridgeHandler() {
                    @Override
                    public void handler(String data, CallBackFunction function) {
                        System.out.println("js调用了connectBle方法,接收参数:" + data);
                        connectBLE(data);
                    }
                });

        webview.registerHandler("disconnectBLE", new

                BridgeHandler() {
                    @Override
                    public void handler(String data, CallBackFunction function) {
                        System.out.println("js调用了disconnectBLE方法,接收参数:" + data);
                        disconnectBLE();
                    }
                });

        webview.registerHandler("remind", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                int action = Integer.parseInt(data);
                remind(getApplicationContext(), action, mVibrator);
            }
        });
        webview.registerHandler("sound", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                System.out.println("sound type:" + data);
//                if (data == "connected") {
//                    rawPlay(R.raw.connected);
//                } else if (data == "disconnected") {
//                    rawPlay(R.raw.disconnected);
//                } else if (data == "water") {
//                    rawPlay(R.raw.water);
//                } else if (data == "message") {
//                    rawPlay(R.raw.message);
//                }
//                rawPlay(R.raw.connected);
                switch (data) {
                    case "connected":
                        rawPlay(R.raw.connected);
                        break;
                    case "disconnected":
                        rawPlay(R.raw.disconnected);
                        break;
                    case "water":
                        rawPlay(R.raw.water);
                        break;
                    case "ququ":
                        rawPlay(R.raw.ququ);
                        break;
                    case "message":
                        rawPlay(R.raw.message);
                        break;
                    default:
                        rawPlay(R.raw.water);
                        break;
                }
            }
        });


        webview.registerHandler("cewen", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction callBackFn) {
                map = new LinkedHashMap<>();
                if (currentDevice == null) {
                    map.put("state", NOT_SELECT_DEVICE);
                } else {
                    if (listTemp.size() < 5) {
                        listTemp.add(temp);
                    } else if (listTemp.size() == 5) {
                        listTemp.remove(listTemp.get(0));
                        listTemp.add(temp);
                    } else if (listTemp.size() > 5) {
                        listTemp.clear();
                    }
                    listTemp = duplicateRemoval(listTemp);
                    map.put("state", SELECT_DEVICE);
                    map.put("temp", listTemp);
                }
                JSONObject jsonObject = new JSONObject(map);
                callBackFn.onCallBack(jsonObject.toString());
            }
        });

    }

    @SuppressLint("NewApi")
    private void disconnectBLE() {
        System.out.println("disconnectBLE mBluetoothGatt:" + mBluetoothGatt);
        if (mBluetoothGatt == null) {
            System.out.println("没有设备连接,无需断开");
            return;
        }
//        mBluetoothGatt.connect();
        mBluetoothGatt.close();
    }

    @SuppressLint("NewApi")
    private void connectBLE(String _address) {
//        String[] d = _d.split("&");
//        String address = d[1].toString();
        System.out.println("准备连接的设备地址--->" + _address);
        if (mbluetoothAdapter == null || _address == null) {
            System.out.println("蓝牙适配器未初始化或地址无效");
            return;
        }
        currentDevice = mbluetoothAdapter.getRemoteDevice(_address);
        if (currentDevice == null) {
            System.out.println("设备未选择，请选择蓝牙连接");
            return;
        }
        System.out.println("当前准备连接设备:" + currentDevice);

        mBluetoothGatt = currentDevice.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    //释放BluetootheGatt的所有资源
                    gatt.close();
                    System.out.println("释放BluetootheGatt");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status != BluetoothGatt.GATT_SUCCESS) return;
                List<BluetoothGattService> list = gatt.getServices();
                for (BluetoothGattService bluetoothGattService : list) {
                    String str = bluetoothGattService.getUuid().toString();
                    List<BluetoothGattCharacteristic> gattCharacteristics = bluetoothGattService.getCharacteristics();
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        if ("0000fff1-0000-1000-8000-00805f9b34fb".equals(gattCharacteristic.getUuid().toString())) {
                            System.out.println("onCharacteristic中" + gattCharacteristic.getUuid().toString());

                            enableNotification(gatt, bluetoothGattService.getUuid(), gattCharacteristic.getUuid());
                            System.out.println("开启监听特征值服务:" + gattCharacteristic.getUuid().toString());
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                System.out.println("onCharacteristicRead:" + characteristic.getValue());
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                byte[] b = characteristic.getValue();
//                8200F274
                String s = bytes_String16(b);
                BigInteger num = new BigInteger(s.substring(2, 6), 16);
                String t = new BigInteger(s.substring(2, 6), 16).toString();
                Integer a = Integer.parseInt(t);
//                DecimalFormat df = new DecimalFormat("0.0");
//                String c = df.format((float) a / 10);
                System.out.println("=======a:" + a);
                String timespan = String.valueOf(System.currentTimeMillis());
                temp = timespan + "@" + a;
                System.out.println("监听变化温度temp:" + temp);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
            }
        });
    }

    private BluetoothAdapter initBlueAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    private void startDiscovery() {
        if (mbluetoothAdapter == null) {
            mbluetoothAdapter = initBlueAdapter();
        }
        if (mbluetoothAdapter != null) {
            if (mbluetoothAdapter.isEnabled()) {
                mbluetoothAdapter.startDiscovery();
            } else {
                System.out.println("系统蓝牙没有打开");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void stopDiscovery() {
        if (mbluetoothAdapter == null) {
            mbluetoothAdapter = initBlueAdapter();
        }
        if (mbluetoothAdapter != null) {
            if (mbluetoothAdapter.isEnabled()) {
                System.out.println("准备...取消扫描");
                mbluetoothAdapter.cancelDiscovery();
            } else {
                System.out.println("系统蓝牙没有打开");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.goBack:
                if (webview.canGoBack()) {
                    webview.goBack();
                }
                break;
            case R.id.goForward:
                if (webview.canGoForward()) {
                    webview.goForward();
                }
                break;
            case R.id.reload:
                webview.reload();
        }
    }

    private void registerBroadcastReceiver() {
        mReceiver = new BroadcastReceiver() {
            HashMap<String, String> mapNotifyState = new HashMap<>();
            Gson gson = new Gson();

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    mapNotifyState = new HashMap<>();
                    mapNotifyState.put("type", FORMAT_SCAN_START);
                    mapNotifyState.put("data", "开始扫描");
                    JSONObject jsonObject = new JSONObject(mapNotifyState);
                    System.out.println("开始扫描");
                    webview.callHandler("onBLENotify", jsonObject.toString(), null);
                }
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && !hashMap.containsKey(device.getAddress())) {
                        hashMap.put(device.getAddress(), String.format("%s@%s", device.getName(), device.getName()));
                        mapNotifyState = new HashMap<>();
                        mapNotifyState.put("type", FORMAT_FOUND_DEVICE);
                        FormatDevice formatDevice = new FormatDevice(device);
                        String jsonDevice = gson.toJson(formatDevice);
                        System.out.println("发现新设备:" + jsonDevice);
                        mapNotifyState.put("data", jsonDevice);
                        JSONObject jsonObject = new JSONObject(mapNotifyState);
                        webview.callHandler("onBLENotify", jsonObject.toString(), null);
                    }
                }
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    mapNotifyState = new HashMap<>();
                    mapNotifyState.put("type", FORMAT_SCAN_FINISH);
                    mapNotifyState.put("data", "扫描结束");
                    JSONObject jsonObject = new JSONObject(mapNotifyState);
                    System.out.println("扫描结束");
                    webview.callHandler("onBLENotify", jsonObject.toString(), null);
                }

                if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
                    mapNotifyState = new HashMap<>();
                    mapNotifyState.put("type", FORMAT_CONNECTED);
                    FormatDevice formatDevice = new FormatDevice(currentDevice);
                    formatDevice.status = STATE_CONNECTED;
                    String deviceJson = gson.toJson(formatDevice);
                    mapNotifyState.put("data", deviceJson);
                    String j = gson.toJson(mapNotifyState);
                    webview.callHandler("onBLENotify", j, null);

                    //播放成功连接音效
//                    rawPlay(R.raw.connected);
                    //震动
//                    remind(getApplicationContext(), 1, mVibrator);
                    System.out.println("ACTION_ACL_CONNECTED:" + j);
                }
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                    mapNotifyState = new HashMap<>();
                    mapNotifyState.put("type", FORMAT_DISCONNECTED);
                    FormatDevice formatDevice = new FormatDevice(currentDevice);
                    formatDevice.status = STATE_DISCONNECTED;
                    String deviceJson = gson.toJson(formatDevice);
                    mapNotifyState.put("data", deviceJson);
                    String j = gson.toJson(mapNotifyState);
                    webview.callHandler("onBLENotify", j, null);
                    //播放连接失败音效
//                    rawPlay(R.raw.disconnected);
                    //震动
//                    remind(getApplicationContext(), 0, mVibrator);
                    System.out.println("ACTION_ACL_DISCONNECTED:" + j);
                }
                if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
//                    System.out.println("ACTION_STATE_CHANGED");
                }
            }

        };

        IntentFilter filterFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filterStart = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        IntentFilter filterFinish = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        IntentFilter stateChangeFilter = new IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter connectedFilter = new IntentFilter(
                BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter disConnectedFilter = new IntentFilter(
                BluetoothDevice.ACTION_ACL_DISCONNECTED);

        registerReceiver(mReceiver, filterFound);
        registerReceiver(mReceiver, filterStart);
        registerReceiver(mReceiver, filterFinish);
        registerReceiver(mReceiver, stateChangeFilter);
        registerReceiver(mReceiver, connectedFilter);
        registerReceiver(mReceiver, disConnectedFilter);
    }

    private void initConfigWebView() {
        webview.loadUrl(REQUEST_URL);//本地测试
//        webview.loadUrl(filePath);//本地测试
        webview.setDefaultHandler(new DefaultHandler());
        webview.setWebChromeClient(new WebChromeClient());
        WebSettings webSettings = webview.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        String appCachePath = getApplication().getCacheDir().getAbsolutePath();
        webSettings.setAppCachePath(appCachePath);
        webSettings.setDatabaseEnabled(true);
    }

    @SuppressLint("NewApi")
    public boolean enableNotification(BluetoothGatt gatt, UUID serviceUUID, UUID
            characteristicUUID) {
        boolean success = false;
        BluetoothGattService service = gatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = findNotifyCharacteristic(service, characteristicUUID);
            if (characteristic != null) {
                success = gatt.setCharacteristicNotification(characteristic, true);
                gatt.readCharacteristic(characteristic);
                if (success) {
                    for (BluetoothGattDescriptor dp : characteristic.getDescriptors()) {
                        if (dp != null) {
                            if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            }
                            int writeType = characteristic.getWriteType();
                            System.out.println("enableNotification: " + writeType);
                            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            mBluetoothGatt.writeDescriptor(dp);
                            characteristic.setWriteType(writeType);
                        }
                    }
                }
            }
        }
        return success;
    }

    @SuppressLint("NewApi")
    private BluetoothGattCharacteristic findNotifyCharacteristic(BluetoothGattService
                                                                         service, UUID characteristicUUID) {
        BluetoothGattCharacteristic characteristic = null;
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }
        if (characteristic != null) {
            return characteristic;
        }
        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }
        return characteristic;
    }

    public static String bytes_String16(byte[] b) {
        char[] _16 = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            sb.append(_16[b[i] >> 4 & 0xf])
                    .append(_16[b[i] & 0xf]);
        }
        return sb.toString();
    }

    private List<String> duplicateRemoval(List<String> ioList) {
        LinkedHashSet<String> tmpSet = new LinkedHashSet<String>(ioList.size());
        tmpSet.addAll(ioList);
        ioList.clear();
        ioList.addAll(tmpSet);
        return ioList;
    }

    class FormatDevice {
        String name;
        String address;
        int status = STATE_DISCONNECTED;

        public FormatDevice(BluetoothDevice device) {
            if (device.getName() == null) {
                this.name = "";
            } else {
                this.name = device.getName();
            }
            this.address = device.getAddress();
        }
    }

    private void rawPlay(int raw) {
        //实例化播放内核
        final android.media.MediaPlayer mediaPlayer = new android.media.MediaPlayer();
        //获得播放源访问入口
        AssetFileDescriptor afd = getResources().openRawResourceFd(raw); // 注意这里的区别
        //给MediaPlayer设置播放源
        try {
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //设置准备就绪状态监听
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // 开始播放
                mediaPlayer.start();
            }
        });
        //准备播放
        mediaPlayer.prepareAsync();
    }

    public void remind(Context context, int action, Vibrator mVibrator) {
        try {
            Uri uri;
            Ringtone rt;
            switch (action) {
                case 0:
                    //失败提示效果
                    mVibrator.vibrate(new long[]{500, 300, 500, 300}, -1);
                    //停止500毫秒，开启震动300毫秒，然后又停止500毫秒，又开启震动300毫秒，不重复.
//                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//                    rt = RingtoneManager.getRingtone(context, uri);
//                    rt.play();
                    break;
                case 1:
                    //成功提示效果
//                    mVibrator.vibrate(new long[]{0, 200},-1);
                    mVibrator.vibrate(new long[]{0, 200,}, -1);
                    //开启震动200毫秒，不重复.

                    //用于获取手机默认提示音的Uri
//                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//                    rt = RingtoneManager.getRingtone(context, uri);
//                    rt.play();
                    break;
                case 2:
                    break;
            }
        } catch (Exception e) {

        }


    }
}
