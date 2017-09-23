package free.freerxdownload;

import android.content.Context;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.concurrent.Semaphore;

import free.freerxdownload.entity.DownloadBean;
import free.freerxdownload.entity.DownloadStatus;
import free.freerxdownload.function.DownloadHelper;
import free.freerxdownload.function.DownloadService;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/25 0025
 * github：
 */

public class RxDownload {

    private volatile static RxDownload instance;

    // 是否绑定服务
    private volatile static boolean bound = false;

    static {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                if (throwable instanceof InterruptedException) {
                    Logger.e("Thread interrupted");
                } else if (throwable instanceof InterruptedIOException) {
                    Logger.e("Io interrupted");
                } else if (throwable instanceof SocketException) {
                    Logger.e("Socket error");
                }
            }
        });
    }

    private int maxDownloadNumber = 5;

    private Context context;
    private Semaphore semaphore;

    private DownloadService downloadService;
    private DownloadHelper downloadHelper;

    private RxDownload(Context context) {
        this.context = context.getApplicationContext();
        downloadHelper = new DownloadHelper(context);
        semaphore = new Semaphore(1);
    }

    public static RxDownload getInstance(Context context) {
        if (instance == null) {
            synchronized (RxDownload.class) {
                if (instance == null) {
                    instance = new RxDownload(context);
                }
            }
        }
        return instance;
    }

    @Nullable
    public File[] getRealFiles(String url) {
        return downloadHelper.getFiles(url);
    }

    public <Upstream> ObservableTransformer<Upstream, DownloadStatus> transform(String url) {
        return transform(url, null);
    }

    private <Upstream> ObservableTransformer<Upstream, DownloadStatus> transform(String url, String saveName) {
        return transform(url, saveName, null);
    }

    private <Upstream> ObservableTransformer<Upstream, DownloadStatus> transform(String url, String saveName, String savePath) {
        return transform(new DownloadBean.Builder(url)
                .setSaveName(saveName).setSavePath(savePath).build());
    }

    public <Upstream> ObservableTransformer<Upstream, DownloadStatus> transform(
            final DownloadBean downloadBean) {
        return new ObservableTransformer<Upstream, DownloadStatus>() {
            @Override
            public ObservableSource<DownloadStatus> apply(Observable<Upstream> upstream) {
                return upstream.flatMap(new Function<Upstream, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(Upstream upstream) throws Exception {
                        return download(downloadBean);
                    }
                });
            }
        };
    }

    private Observable<DownloadStatus> download(DownloadBean downloadBean) {
        return downloadHelper.downloadDispatcher(downloadBean);
    }

    public Observable<DownloadStatus> download(String url) {
        return download(url, null);
    }

    private Observable<DownloadStatus> download(String url, String saveName) {
        return download(url, saveName, null);
    }

    private Observable<DownloadStatus> download(String url, String saveName, String savePath) {
        return download(new DownloadBean.Builder(url).setSaveName(saveName)
                .setSavePath(savePath).build());
    }
}
