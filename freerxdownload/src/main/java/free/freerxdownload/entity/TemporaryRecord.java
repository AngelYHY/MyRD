package free.freerxdownload.entity;

import android.text.TextUtils;

import org.reactivestreams.Publisher;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import free.freerxdownload.DataBaseHelper;
import free.freerxdownload.function.DownloadApi;
import free.freerxdownload.function.FileHelper;
import free.freerxdownload.function.Utils;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static free.freerxdownload.entity.DownloadFlag.COMPLETED;
import static free.freerxdownload.entity.DownloadFlag.FAILED;
import static free.freerxdownload.entity.DownloadFlag.PAUSED;
import static free.freerxdownload.entity.DownloadFlag.STARTED;
import static free.freerxdownload.function.Constant.CACHE;
import static free.freerxdownload.function.Constant.RANGE_DOWNLOAD_STARTED;
import static java.io.File.separator;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/28 0028
 * github：
 */

public class TemporaryRecord {

    private final DownloadBean mBean;

    private String filePath;
    private String tempPath;
    private String lmfPath;

    private int maxRetryCount;
    private int maxThreads;

    private long contentLength;
    private String lastModify;

    private boolean rangeSupport = false;
    private boolean serverFileChanged = false;

    private DataBaseHelper dataBaseHelper;
    private FileHelper fileHelper;
    private DownloadApi downloadApi;

    public TemporaryRecord(DownloadBean bean) {
        mBean = bean;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTempPath() {
        return tempPath;
    }

    public void setTempPath(String tempPath) {
        this.tempPath = tempPath;
    }

    public String getLmfPath() {
        return lmfPath;
    }

    public void setLmfPath(String lmfPath) {
        this.lmfPath = lmfPath;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public String getLastModify() {
        return lastModify;
    }

    public void setLastModify(String lastModify) {
        this.lastModify = lastModify;
    }

    public boolean isRangeSupport() {
        return rangeSupport;
    }

    public void setRangeSupport(boolean rangeSupport) {
        this.rangeSupport = rangeSupport;
    }

    public boolean isServerFileChanged() {
        return serverFileChanged;
    }

    public void setServerFileChanged(boolean serverFileChanged) {
        this.serverFileChanged = serverFileChanged;
    }

    public DataBaseHelper getDataBaseHelper() {
        return dataBaseHelper;
    }

    public void setDataBaseHelper(DataBaseHelper dataBaseHelper) {
        this.dataBaseHelper = dataBaseHelper;
    }

    public FileHelper getFileHelper() {
        return fileHelper;
    }

    public void setFileHelper(FileHelper fileHelper) {
        this.fileHelper = fileHelper;
    }

    public DownloadApi getDownloadApi() {
        return downloadApi;
    }

    public void setDownloadApi(DownloadApi downloadApi) {
        this.downloadApi = downloadApi;
    }

    public void init(int maxThreads, int maxRetryCount, String defaultSavePath, DownloadApi downloadApi, DataBaseHelper dataBaseHelper) {
        this.maxThreads = maxThreads;
        this.maxRetryCount = maxRetryCount;
        this.downloadApi = downloadApi;
        this.dataBaseHelper = dataBaseHelper;
        this.fileHelper = new FileHelper(maxThreads);

        String realSavePath;
        if (TextUtils.isEmpty(mBean.getSave_path())) {
            realSavePath = defaultSavePath;
            mBean.setSave_path(defaultSavePath);
        } else {
            realSavePath = mBean.getSave_path();
        }

        String cachePath = TextUtils.concat(realSavePath, separator, CACHE).toString();
        Utils.mkdirs(realSavePath, cachePath);

        String[] paths = Utils.getPaths(mBean.getSave_name(), realSavePath);
        filePath = paths[0];
        tempPath = paths[1];
        lmfPath = paths[2];
    }

    public File file() {
        return new File(filePath);
    }

    public String readLastModify() throws IOException {
        return fileHelper.readLastModify(lastModifyFile());
    }

    private File lastModifyFile() {
        return new File(lmfPath);
    }

    public boolean tempFileDamaged() throws IOException {
        return fileHelper.tempFileDamaged(tempFile(), contentLength);
    }

    public File tempFile() {
        return new File(tempPath);
    }

    public boolean fileNotComplete() throws IOException {
        return fileHelper.fileNotComplete(tempFile());
    }

    public boolean fileComplete() {
        return file().length() == contentLength;
    }

    public void start() {
        if (dataBaseHelper.recordNotExists(mBean.getUrl())) {
            mBean.setDownload_flag(STARTED);
            mBean.setDate(new Date());
            dataBaseHelper.insertRecord(mBean);
        } else {
            dataBaseHelper.updateRecord(mBean.getUrl(), mBean.getSave_name(), mBean.getSave_path(), STARTED);
        }
    }

    public void update(DownloadStatus status) {
        dataBaseHelper.updateStatus(mBean.getUrl(), status);
    }

    public void error() {
        dataBaseHelper.updateRecord(mBean.getUrl(), FAILED);
    }

    public void complete() {
        dataBaseHelper.updateRecord(mBean.getUrl(), COMPLETED);
    }

    public void cancel() {
        dataBaseHelper.updateRecord(mBean.getUrl(), PAUSED);
    }

    public void prepareNormalDownload() throws IOException, ParseException {
        fileHelper.prepareDownload(lastModifyFile(), file(), contentLength, lastModify);
    }

    public Flowable<Response<ResponseBody>> download() {
        return downloadApi.download(null, mBean.getUrl());
    }

    public void save(FlowableEmitter<DownloadStatus> emitter, Response<ResponseBody> response) {
        fileHelper.saveFile(emitter, file(), response);
    }

    /**
     * Range download request
     *
     * @param index
     * @return
     */
    public Flowable<Response<ResponseBody>> rangeDownload(final int index) {
        return Flowable.create(new FlowableOnSubscribe<DownloadRange>() {
            @Override
            public void subscribe(FlowableEmitter<DownloadRange> e) throws Exception {
                DownloadRange range = readDownloadRange(index);
                if (range.legal()) {
                    e.onNext(range);
                }
                e.onComplete();
            }
        }, BackpressureStrategy.ERROR)
                .flatMap(new Function<DownloadRange, Publisher<Response<ResponseBody>>>() {
                    @Override
                    public Publisher<Response<ResponseBody>> apply(@NonNull DownloadRange range) throws Exception {
                        Utils.log(RANGE_DOWNLOAD_STARTED, index, range.start, range.end);
                        String rangeStr = "bytes=" + range.start + "-" + range.end;
                        return downloadApi.download(rangeStr, mBean.getUrl());
                    }
                });
    }

    private DownloadRange readDownloadRange(int index) throws IOException {
        return fileHelper.readDownloadRange(tempFile(), index);
    }

    public void save(FlowableEmitter<DownloadStatus> e, int index, ResponseBody response) {
        fileHelper.saveFile(e, index, tempFile(), file(), response);
    }

    public void prepareRangeDownload() throws IOException, ParseException {
        fileHelper.prepareDownload(lastModifyFile(), tempFile(), contentLength, lastModify);
    }
}
