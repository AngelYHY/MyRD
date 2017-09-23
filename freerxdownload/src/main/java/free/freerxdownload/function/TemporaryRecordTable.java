package free.freerxdownload.function;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import free.freerxdownload.DataBaseHelper;
import free.freerxdownload.entity.DownloadType;
import free.freerxdownload.entity.DownloadType.MultiThreadDownload;
import free.freerxdownload.entity.DownloadType.NormalDownload;
import free.freerxdownload.entity.DownloadType.ContinueDownload;
import free.freerxdownload.entity.DownloadType.AlreadyDownloaded;
import free.freerxdownload.entity.TemporaryRecord;
import retrofit2.Response;

import static free.freerxdownload.function.Constant.DOWNLOAD_RECORD_FILE_DAMAGED;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/28 0028
 * github：
 */

public class TemporaryRecordTable {

    private Map<String, TemporaryRecord> map;

    public TemporaryRecordTable() {
        this.map = new HashMap<>();
    }

    public boolean contain(String url) {
        return map.get(url) != null;
    }

    public void add(String url, TemporaryRecord record) {
        map.put(url, record);
    }

    public void saveRangeInfo(String url, Response<Void> response) {
        map.get(url).setRangeSupport(Utils.isSupportRange(response));
    }

    public void init(String url, int maxThreads, int maxRetryCount, String defaultSavePath, DownloadApi downloadApi, DataBaseHelper dataBaseHelper) {
        map.get(url).init(maxThreads, maxRetryCount, defaultSavePath, downloadApi, dataBaseHelper);
    }

    public boolean fileExists(String url) {
        return map.get(url).file().exists();
    }

    public String readLastModify(String url) {
        try {
            return map.get(url).readLastModify();
        } catch (IOException e) {
            return "";
        }
    }

    public void saveFileState(String url, Response<Void> response) {
        if (response.code() == 304) {
            map.get(url).setServerFileChanged(false);
        } else if (response.code() == 200) {
            map.get(url).setServerFileChanged(true);
        }
    }

    public DownloadType generateFileExistsType(String url) {
        DownloadType type;
        if (fileChanged(url)) {
            type = getNormalType(url);
        } else {
            type = getServerFileChangeType(url);
        }
        return type;
    }

    private DownloadType getServerFileChangeType(String url) {
        if (supportRange(url)) {
            return supportRangeType(url);
        } else {
            return notSupportRangeType(url);
        }
    }

    private DownloadType notSupportRangeType(String url) {
        if (normalDownloadNotComplete(url)) {
            return new NormalDownload(map.get(url));
        } else {
            return new AlreadyDownloaded(map.get(url));
        }
    }

    private boolean normalDownloadNotComplete(String url) {
        return !map.get(url).fileComplete();
    }

    private DownloadType supportRangeType(String url) {
        if (needReDownload(url)) {
            return new MultiThreadDownload(map.get(url));
        }
        try {
            if (multiDownloadNotComplete(url)) {
                return new ContinueDownload(map.get(url));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new AlreadyDownloaded(map.get(url));
    }

    private boolean multiDownloadNotComplete(String url) throws IOException {
        return map.get(url).fileNotComplete();
    }

    private boolean needReDownload(String url) {
        return tempFileNotExists(url) || tempFileDamaged(url);
    }

    private boolean tempFileDamaged(String url) {
        try {
            return map.get(url).tempFileDamaged();
        } catch (IOException e) {
            Utils.log(DOWNLOAD_RECORD_FILE_DAMAGED);
            return true;
        }
    }

    private boolean tempFileNotExists(String url) {
        return !map.get(url).tempFile().exists();
    }

    private boolean supportRange(String url) {
        return map.get(url).isRangeSupport();
    }

    private DownloadType getNormalType(String url) {
        DownloadType type;
        if (supportRange(url)) {
            type = new MultiThreadDownload(map.get(url));
        } else {
            type = new NormalDownload(map.get(url));
        }
        return type;
    }

    private boolean fileChanged(String url) {
        return map.get(url).isServerFileChanged();
    }

    public DownloadType generateNonExistsType(String url) {
        return getNormalType(url);
    }

    public void delete(String url) {
        map.remove(url);
    }
}
