package free.freerxdownload.entity;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/25 0025
 * github：
 */

public class DownloadRange {
    public long start, end, size;

    public DownloadRange(long start, long end) {
        this.start = start;
        this.end = end;
        this.size = end - start + 1;
    }

    public boolean legal() {
        return start <= end;
    }
}
