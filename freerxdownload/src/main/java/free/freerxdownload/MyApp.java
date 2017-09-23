package free.freerxdownload;

import android.app.Application;

import com.orhanobut.logger.Logger;

import free.freerxdownload.function.Constant;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/24 0024
 * github：
 */

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.init(Constant.TAG).methodCount(2).hideThreadInfo();

    }
}
