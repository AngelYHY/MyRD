package free.freerxdownload.entity;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/25 0025
 * github：
 */

public class DownloadEvent {
    private int flag = DownloadFlag.NORMAL;
    private DownloadStatus downloadStatus = new DownloadStatus();
    private Throwable mError;

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public Throwable getError() {
        return mError;
    }

    public void setError(Throwable error) {
        mError = error;
    }
}
