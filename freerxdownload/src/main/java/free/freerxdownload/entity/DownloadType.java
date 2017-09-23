package free.freerxdownload.entity;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import free.freerxdownload.function.Utils;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static free.freerxdownload.function.Constant.NORMAL_RETRY_HINT;
import static free.freerxdownload.function.Constant.RANGE_RETRY_HINT;
import static free.freerxdownload.function.Utils.formatStr;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/28 0028
 * github：
 */

public abstract class DownloadType {
    protected TemporaryRecord record;

    private DownloadType(TemporaryRecord record) {
        this.record = record;
    }

    public void prepareDownload() throws IOException, ParseException {
        Utils.log(prepareLog());
    }

    protected String prepareLog() {
        return "";
    }

    public ObservableSource<DownloadStatus> startDownload() {
        return Flowable.just(1)
                .doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(@NonNull Subscription subscription) throws Exception {
                        record.start();
                    }
                })
                .flatMap(new Function<Integer, Publisher<DownloadStatus>>() {
                    @Override
                    public Publisher<DownloadStatus> apply(@NonNull Integer integer) throws Exception {
                        return download();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()) //由于 上面将线程切换到了非 主线程 然后 Realm 数据库操作需要在主线程
                .doOnNext(new Consumer<DownloadStatus>() {
                    @Override
                    public void accept(@NonNull DownloadStatus status) throws Exception {
                        record.update(status);
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        record.error();
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        record.complete();
                    }
                })
                .doOnCancel(new Action() {
                    @Override
                    public void run() throws Exception {
                        record.cancel();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())  //让订阅发生在主线程 这样 doOnSubscribe 就在主线程执行
                .observeOn(Schedulers.io())
                .toObservable();
    }

    protected abstract Publisher<DownloadStatus> download();

    public static class NormalDownload extends DownloadType {

        public NormalDownload(TemporaryRecord record) {
            super(record);
        }

        @Override
        public void prepareDownload() throws IOException, ParseException {
            super.prepareDownload();
            record.prepareNormalDownload();
        }

        @Override
        protected Publisher<DownloadStatus> download() {
            return record.download()
                    .flatMap(new Function<Response<ResponseBody>, Publisher<DownloadStatus>>() {
                        @Override
                        public Publisher<DownloadStatus> apply(@NonNull Response<ResponseBody> response) throws Exception {
                            return save(response);
                        }
                    })
                    .compose(Utils.<DownloadStatus>retry2(NORMAL_RETRY_HINT, record.getMaxRetryCount()));
        }

        private Publisher<DownloadStatus> save(final Response<ResponseBody> response) {
            return Flowable.create(new FlowableOnSubscribe<DownloadStatus>() {
                @Override
                public void subscribe(FlowableEmitter<DownloadStatus> e) throws Exception {
                    record.save(e, response);
                }
            }, BackpressureStrategy.LATEST);
        }
    }

    public static class MultiThreadDownload extends ContinueDownload {
        public MultiThreadDownload(TemporaryRecord record) {
            super(record);
        }

        @Override
        public void prepareDownload() throws IOException, ParseException {
            super.prepareDownload();
            record.prepareRangeDownload();
        }

    }

    public static class ContinueDownload extends DownloadType {
        public ContinueDownload(TemporaryRecord record) {
            super(record);
        }

        @Override
        protected Publisher<DownloadStatus> download() {
            List<Publisher<DownloadStatus>> tasks = new ArrayList<>();
            for (int i = 0; i < record.getMaxThreads(); i++) {
                tasks.add(rangeDownload(i));
            }
            return null;
        }

        /**
         * 分段下载任务
         *
         * @param index 下载编号
         * @return
         */
        private Publisher<DownloadStatus> rangeDownload(final int index) {
            return record.rangeDownload(index)
                    .subscribeOn(Schedulers.io())
                    .flatMap(new Function<Response<ResponseBody>, Publisher<DownloadStatus>>() {
                        @Override
                        public Publisher<DownloadStatus> apply(@NonNull Response<ResponseBody> bodyResponse) throws Exception {
                            return save(index, bodyResponse.body());
                        }
                    })
                    .compose(Utils.<DownloadStatus>retry2(formatStr(RANGE_RETRY_HINT, index), record.getMaxRetryCount()));
        }

        private Publisher<DownloadStatus> save(final int index, final ResponseBody response) {
            return Flowable.create(new FlowableOnSubscribe<DownloadStatus>() {
                @Override
                public void subscribe(FlowableEmitter<DownloadStatus> e) throws Exception {
                    record.save(e, index, response);
                }
            }, BackpressureStrategy.LATEST);
        }
    }

    public static class AlreadyDownloaded extends DownloadType {
        public AlreadyDownloaded(TemporaryRecord record) {
            super(record);
        }

        @Override
        protected Publisher<DownloadStatus> download() {
            return Flowable.just(new DownloadStatus(record.getContentLength(), record.getContentLength()));
        }
    }

}
