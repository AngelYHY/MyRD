package free.freerxdownload.entity;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/25 0025
 * github：
 */

public class DownloadStatus {
    //    表示下载状态, 如果isChunked为true, total_size 可能不存在
    private boolean chunked;
    private long total_size;
    private long download_size;

    public DownloadStatus() {
    }

    public DownloadStatus(boolean chunked, long total_size, long download_size) {
        this.chunked = chunked;
        this.total_size = total_size;
        this.download_size = download_size;
    }

    public DownloadStatus(long download_size, long total_size) {
        this.download_size = download_size;
        this.total_size = total_size;
    }


    public boolean isChunked() {
        return chunked;
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    public long getTotal_size() {
        return total_size;
    }

    public void setTotal_size(long total_size) {
        this.total_size = total_size;
    }

    public long getDownload_size() {
        return download_size;
    }

    public void setDownload_size(long download_size) {
        this.download_size = download_size;
    }
}
