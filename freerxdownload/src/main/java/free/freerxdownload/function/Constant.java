package free.freerxdownload.function;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/25 0025
 * github：
 */

public class Constant {

    public static final String TAG = "FreeStar";

    public static final String TMP_SUFFIX = ".tmp";  //temp file
    public static final String LMF_SUFFIX = ".lmf";  //last modify file
    public static final String CACHE = ".cache";    //cache directory

    public static final String URL_ILLEGAL
            = "The url [%s] is illegal.";

    public static final String DOWNLOAD_URL_EXISTS
            = "The url [%s] already exists.";

    public static final String DOWNLOAD_RECORD_FILE_DAMAGED
            = "Record file may be damaged, so we will re-download";

    /**
     * test
     */
    public static final String TEST_RANGE_SUPPORT = "bytes=0-";


    //Normal download hint
    public static final String CHUNKED_DOWNLOAD_HINT = "Aha, chunked download!";


    //Continue download hint

    //Dir hint
    public static final String DIR_EXISTS_HINT = "Path [%s] exists.";
    public static final String DIR_NOT_EXISTS_HINT =
            "Path [%s] not exists, so create.";
    public static final String DIR_CREATE_SUCCESS = "Path [%s] create success.";
    public static final String DIR_CREATE_FAILED = "Path [%s] create failed.";
    public static final String FILE_DELETE_SUCCESS = "File [%s] delete success.";
    public static final String FILE_DELETE_FAILED = "File [%s] delete failed.";

    //Range download hint
    public static final String RANGE_DOWNLOAD_STARTED =
            "Range %d start download from [%d] to [%d]";

    public static final String RANGE_DOWNLOAD_COMPLETED =
            "[%s] download completed!";

    public static final String RANGE_DOWNLOAD_CANCELED =
            "[%s] download canceled!";

    public static final String RANGE_DOWNLOAD_FAILED =
            "[%s] download failed or cancel!";

    public static final String REQUEST_RETRY_HINT = "Request";
    public static final String NORMAL_RETRY_HINT = "Normal download";
    public static final String RANGE_RETRY_HINT = "Range %d";
    public static final String RETRY_HINT =
            "%s get [%s] error, now retry [%d] times";

    public static final String COLUMN_MISSION_ID = "mission_id";
    public static final String COLUMN_DOWNLOAD_FLAG = "download_flag";
}
