package free.freerxdownload.function;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import free.freerxdownload.DataBaseHelper;
import free.freerxdownload.entity.DownloadBean;
import free.freerxdownload.entity.DownloadStatus;
import free.freerxdownload.entity.DownloadType;
import free.freerxdownload.entity.TemporaryRecord;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import retrofit2.Response;
import retrofit2.Retrofit;

import static free.freerxdownload.function.Constant.DOWNLOAD_URL_EXISTS;
import static free.freerxdownload.function.Constant.REQUEST_RETRY_HINT;
import static free.freerxdownload.function.Constant.TAG;
import static free.freerxdownload.function.Constant.TEST_RANGE_SUPPORT;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/25 0025
 * github：
 */

public class DownloadHelper {
    private int maxRetryCount = 3;
    private int maxThreads = 3;

    private String defaultSavePath;
    private DownloadApi downloadApi;

    private DataBaseHelper dataBaseHelper;
    private TemporaryRecordTable recordTable;

    public DownloadHelper(Context context) {
        downloadApi = RetrofitProvider.getInstance().create(DownloadApi.class);
        defaultSavePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        recordTable = new TemporaryRecordTable();
        dataBaseHelper = DataBaseHelper.getSingleton(context);
    }

    public void setRetrofit(Retrofit retrofit) {
        downloadApi = retrofit.create(DownloadApi.class);
    }

    public void setDefaultSavePath(String defaultSavePath) {
        this.defaultSavePath = defaultSavePath;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    @Nullable
    public File[] getFiles(String url) {
        DownloadBean record = dataBaseHelper.readSingleRecord(url);
        if (record == null) {
            return null;
        } else {
            return Utils.getFiles(record.getSave_name(), record.getSave_path());
        }
    }

    public Observable<DownloadStatus> downloadDispatcher(final DownloadBean bean) {
        return Observable.just(1)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        addTempRecord(bean);
                    }
                })
                .flatMap(new Function<Integer, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(@NonNull Integer integer) throws Exception {
                        return getDownloadType(bean.getUrl());
                    }
                })
                .flatMap(new Function<DownloadType, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(@NonNull DownloadType type) throws Exception {
                        return download(type);
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        logError(throwable);
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        recordTable.delete(bean.getUrl());
                    }
                });
    }

    private void logError(Throwable throwable) {
        // 可能有多个错误。
        if (throwable instanceof CompositeException) {
            CompositeException realException = (CompositeException) throwable;
            List<Throwable> exceptions = realException.getExceptions();
            for (Throwable each : exceptions) {
                Log.e(TAG, "出错啦~~", each);
            }
        } else {
            Log.e(TAG, "出错啦~~", throwable);
        }
    }

    private ObservableSource<DownloadStatus> download(DownloadType type) throws IOException, ParseException {
        type.prepareDownload();
        return type.startDownload();
    }

    private ObservableSource<DownloadType> getDownloadType(final String url) {
        return Observable.just(1)
                .flatMap(new Function<Integer, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(@NonNull Integer integer) throws Exception {
                        return checkUrl(url);
                    }
                })
                .flatMap(new Function<Object, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(@NonNull Object o) throws Exception {
                        return checkRange(url);
                    }
                })
                .doOnNext(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        recordTable.init(url, maxThreads, maxRetryCount, defaultSavePath, downloadApi, dataBaseHelper);
                    }
                })
                .flatMap(new Function<Object, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(@NonNull Object o) throws Exception {
                        return recordTable.fileExists(url) ? existsType(url) : nonExistsType(url);
                    }
                });
    }

    private ObservableSource<DownloadType> nonExistsType(final String url) {
        return Observable.just(1)
                .flatMap(new Function<Integer, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(@NonNull Integer integer) throws Exception {
                        return Observable.just(recordTable.generateNonExistsType(url));
                    }
                });
    }

    private ObservableSource<DownloadType> existsType(final String url) {
        return Observable.just(1)
                .map(new Function<Integer, String>() {
                    @Override
                    public String apply(@NonNull Integer integer) throws Exception {
                        return recordTable.readLastModify(url);
                    }
                })
                .flatMap(new Function<String, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(@NonNull String lastModify) throws Exception {
                        return checkFile(url, lastModify);
                    }
                })
                .flatMap(new Function<Object, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(@NonNull Object o) throws Exception {
                        return Observable.just(recordTable.generateFileExistsType(url));
                    }
                });
    }

    private ObservableSource<Object> checkFile(final String url, String lastModify) {
        return downloadApi.checkFileByHead(lastModify, url)
                .doOnNext(new Consumer<Response<Void>>() {
                    @Override
                    public void accept(@NonNull Response<Void> voidResponse) throws Exception {
                        recordTable.saveFileState(url, voidResponse);
                    }
                })
                .map(new Function<Response<Void>, Object>() {
                    @Override
                    public Object apply(@NonNull Response<Void> voidResponse) throws Exception {
                        return new Object();
                    }
                })
                .compose(Utils.retry(REQUEST_RETRY_HINT, maxRetryCount));
    }

    private ObservableSource<Object> checkRange(final String url) {
        return downloadApi.checkRangeByHead(TEST_RANGE_SUPPORT, url)
                .doOnNext(new Consumer<Response<Void>>() {
                    @Override
                    public void accept(@NonNull Response<Void> voidResponse) throws Exception {
                        recordTable.saveRangeInfo(url, voidResponse);
                    }
                })
                .map(new Function<Response<Void>, Object>() {
                    @Override
                    public Object apply(@NonNull Response<Void> voidResponse) throws Exception {
                        return new Object();
                    }
                })
                .compose(Utils.retry(REQUEST_RETRY_HINT, maxRetryCount));
    }

    private ObservableSource<Object> checkUrl(final String url) {
        return downloadApi.check(url)
                .flatMap(new Function<Response<Void>, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(@NonNull Response<Void> res) throws Exception {
                        return res.isSuccessful() ? saveFileInfo(url, res) : checkUrlByGet(url);
                    }
                })
                .compose(Utils.retry(REQUEST_RETRY_HINT, maxRetryCount));
    }

    private ObservableSource<Object> saveFileInfo(String url, Response<Void> res) {
        return null;
    }

    private ObservableSource<Object> checkUrlByGet(String url) {
        return null;
    }

    private void addTempRecord(DownloadBean bean) {
        if (recordTable.contain(bean.getUrl())) {
            //抛出的异常表明向方法传递了一个不合法或不正确的参数。
            throw new IllegalArgumentException(Utils.formatStr(DOWNLOAD_URL_EXISTS, bean.getUrl()));
        }
        recordTable.add(bean.getUrl(), new TemporaryRecord(bean));
    }
}
