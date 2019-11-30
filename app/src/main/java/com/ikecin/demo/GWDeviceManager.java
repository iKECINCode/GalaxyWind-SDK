package com.ikecin.demo;


import android.content.Context;
import android.util.Log;

import com.galaxywind.wukit.kitapis.KitConfigApi;
import com.galaxywind.wukit.kits.WukitEventHandler;
import com.galaxywind.wukit.kits.clibevent.BaseEventMapper;
import com.galaxywind.wukit.support_devs.xinyuan.XinYuanKit;
import com.galaxywind.wukit.user.KitLanDev;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;

public class GWDeviceManager implements WukitEventHandler {
    private static class Holder {
        private static final GWDeviceManager sInstance = new GWDeviceManager();
    }

    static GWDeviceManager getInstance() {
        return Holder.sInstance;
    }

    private String TAG = this.getClass().getSimpleName();
    private XinYuanKit mSdk = XinYuanKit.getInstance();
    private PublishSubject<EventData> mPublishSubject = PublishSubject.create();

    private GWDeviceManager() {

    }

    /**
     * 初始化
     */
    void init(Context context) {
        mSdk.init(context.getApplicationContext());
        mSdk.registerEvent(BaseEventMapper.SC_BEGIN, BaseEventMapper.SC_END, WukitEventHandler.HANDLE_ALL, this);
        mSdk.setDebugEnable(BuildConfig.DEBUG);
    }

    /**
     * 销毁
     */
    void deinit() {
        mSdk.unRegisterEvent(this);
        mSdk.release();
    }

    /**
     * 开始局域网扫描
     */
    void startProbe() {
        mSdk.startProbe();
    }

    /**
     * 结束局域网扫描
     */
    void stopProbe() {
        mSdk.stopProbe();
    }

    /**
     * 获取扫描的局域网设备
     */
    ArrayList<KitLanDev> getLanDevInfo() {
        return mSdk.getLanDevInfo();
    }

    /**
     * 开始一键配置
     * 配网成功一个后就自动停止了（根据现象推测）
     * 配网超时后会返回配网失败
     * @param ssid ssid
     * @param password 密码
     * @param timeout 超时时间，单位秒
     */
    void startSmartConfig(String ssid, String password, int timeout) {
        Log.d(TAG, String.format("开始配网:ssid=%s, pwd=%s, timeout=%d", ssid, password, timeout));
        mSdk.startSmartConfig(ssid, password, KitConfigApi.CONF_MODE_MUT, timeout);
    }

    /**
     * 停止一键配置
     */
    void stopSmartConfig() {
        Log.d(TAG, "停止配网");
        mSdk.stopSmartConfig();
    }

    /**
     * 回调数据
     */
    private static class EventData {
        int wukitEvent;
        int objHandle;
        int errNo;


        EventData(int wukitEvent, int objHandle, int errNo) {
            this.wukitEvent = wukitEvent;
            this.objHandle = objHandle;
            this.errNo = errNo;
        }
    }

    @Override
    public void callback(int wukitEvent, int objHandle, int errNo) {
        Log.d(TAG, String.format("回调：type=%d, objHandle=%d, errNo=%d", wukitEvent, objHandle, errNo));
        mPublishSubject.onNext(new EventData(wukitEvent, objHandle, errNo));
    }

    /**
     * 监听配网事件
     */
    Observable<String> observeConfig() {
        return mPublishSubject
            .filter(data -> data.wukitEvent == BaseEventMapper.SC_CONFIG_OK || data.wukitEvent == BaseEventMapper.SC_CONFIG_FAIL)
            .map(eventData -> {
                if (eventData.wukitEvent == BaseEventMapper.SC_CONFIG_FAIL) {
                    Log.d(TAG, "配网失败");
                    return "";
                }

                ArrayList<KitLanDev> devices = mSdk.getLanDevInfo();
                if (devices == null) {
                    Log.d(TAG, "配网成功，但没有找到设备");
                    return "";
                }

                for (KitLanDev lanDev : devices) {
                    if (lanDev.handle == eventData.objHandle) {
                        Log.d(TAG, String.format("配网成功：sn=%d", lanDev.dev_sn));
                        return String.valueOf(lanDev.dev_sn);
                    }
                }

                return "";
            })
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 监听局域网列表变化
     */
    Observable<ArrayList<KitLanDev>> observeProbe() {
        return mPublishSubject
            .filter(data -> data.wukitEvent == BaseEventMapper.PE_DEV_CHANGED)
            .map(eventData -> {
                ArrayList<KitLanDev> devices = mSdk.getLanDevInfo();
                if (devices == null) {
                    Log.d(TAG, "局域网设备列表变化：空");
                    return new ArrayList<KitLanDev>();
                } else {
                    Log.d(TAG, "局域网设备列表变化：" + devices.toString());
                    return devices;
                }
            })
            .observeOn(AndroidSchedulers.mainThread());
    }

}
