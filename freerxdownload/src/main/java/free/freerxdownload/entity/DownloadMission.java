package free.freerxdownload.entity;

import java.util.Map;
import java.util.concurrent.Semaphore;

import free.freerxdownload.DataBaseHelper;
import free.freerxdownload.RxDownload;
import io.reactivex.processors.FlowableProcessor;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/25 0025
 * github：
 * 下载任务
 */

public abstract class DownloadMission {
    private RxDownload rxdownload;
    private boolean canceled, completed;
    FlowableProcessor<DownloadEvent> processor;

    public DownloadMission(RxDownload rxdownload) {
        this.rxdownload = rxdownload;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public abstract String getUrl();

    public abstract void init(Map<String, DownloadMission> missionMap,
                              Map<String, FlowableProcessor<DownloadEvent>> processorMap);

    public abstract void insertOrUpdate(DataBaseHelper dataBaseHelper);

    public abstract void start(final Semaphore semaphore) throws InterruptedException;

    public abstract void pause(DataBaseHelper dataBaseHelper);

    public abstract void delete(DataBaseHelper dataBaseHelper, boolean deleteFile);

    public abstract void sendWaitingEvent(DataBaseHelper dataBaseHelper);
}
