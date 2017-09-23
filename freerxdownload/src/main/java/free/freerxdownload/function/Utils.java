package free.freerxdownload.function;

import android.text.TextUtils;

import com.orhanobut.logger.Logger;

import org.reactivestreams.Publisher;

import java.io.File;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiPredicate;
import okhttp3.internal.http.HttpHeaders;
import retrofit2.HttpException;
import retrofit2.Response;

import static free.freerxdownload.function.Constant.CACHE;
import static free.freerxdownload.function.Constant.DIR_CREATE_FAILED;
import static free.freerxdownload.function.Constant.DIR_CREATE_SUCCESS;
import static free.freerxdownload.function.Constant.DIR_EXISTS_HINT;
import static free.freerxdownload.function.Constant.DIR_NOT_EXISTS_HINT;
import static free.freerxdownload.function.Constant.LMF_SUFFIX;
import static free.freerxdownload.function.Constant.RETRY_HINT;
import static free.freerxdownload.function.Constant.TMP_SUFFIX;
import static java.util.Locale.getDefault;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/25 0025
 * github：
 */

public class Utils {

    public static boolean isChunked(Response<?> response) {
        return "chunked".equals(transferEncoding(response));
    }

    public static String transferEncoding(Response<?> response) {
        return response.headers().get("Transfer-Encoding");
    }

    /**
     * 将 long 转为 GMT
     *
     * @param lastModify
     * @return
     */
    public static String longToGMT(long lastModify) {
        Date d = new Date(lastModify);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(d);
    }

    // 获取 文件路径
    public static File[] getFiles(String save_name, String save_path) {
        String[] paths = getPaths(save_name, save_path);
        return new File[]{new File(paths[0]), new File(paths[1]), new File(paths[2])};
    }

    // 获取 字符串路径
    public static String[] getPaths(String save_name, String save_path) {
        //字符串拼接
        // File.separator 与系统有关的默认名称分隔符，为了方便，它被表示为一个字符串。
//        在 UNIX 系统上，此字段的值为 '/'；在 Microsoft Windows 系统上，它为 '\\'。
        String cachePath = TextUtils.concat(save_path, File.separator, CACHE).toString();

        String filePath = TextUtils.concat(save_path, File.separator, save_name).toString();
        String tempPath = TextUtils.concat(cachePath, File.separator, save_name, TMP_SUFFIX).toString();
        String lmfPath = TextUtils.concat(cachePath, File.separator, save_name, LMF_SUFFIX).toString();
        return new String[]{filePath, tempPath, lmfPath};
    }

    public static String formatStr(String str, Object... args) {
        return String.format(getDefault(), str, args);
    }

    public static <U> ObservableTransformer<U, U> retry(final String hint, final int retryCount) {
        return new ObservableTransformer<U, U>() {
            @Override
            public ObservableSource<U> apply(Observable<U> upstream) {
                return upstream.retry(new BiPredicate<Integer, Throwable>() {
                    @Override
                    public boolean test(@NonNull Integer integer, @NonNull Throwable throwable) throws Exception {
                        return retry(hint, retryCount, integer, throwable);
                    }
                });
            }
        };
    }

    private static boolean retry(String hint, int maxRetryCount, Integer integer, Throwable throwable) {
        if (throwable instanceof ProtocolException) {
            if (integer < maxRetryCount + 1) {
                log(RETRY_HINT, hint, "ProtocolException", integer);
                return true;
            }
            return false;
        } else if (throwable instanceof UnknownHostException) {
            if (integer < maxRetryCount + 1) {
                log(RETRY_HINT, hint, "UnknownHostException", integer);
                return true;
            }
            return false;
        } else if (throwable instanceof HttpException) {
            if (integer < maxRetryCount + 1) {
                log(RETRY_HINT, hint, "HttpException", integer);
                return true;
            }
            return false;
        } else if (throwable instanceof SocketTimeoutException) {
            if (integer < maxRetryCount + 1) {
                log(RETRY_HINT, hint, "SocketTimeoutException", integer);
                return true;
            }
            return false;
        } else if (throwable instanceof ConnectException) {
            if (integer < maxRetryCount + 1) {
                log(RETRY_HINT, hint, "ConnectException", integer);
                return true;
            }
            return false;
        } else if (throwable instanceof SocketException) {
            if (integer < maxRetryCount + 1) {
                log(RETRY_HINT, hint, "SocketException", integer);
                return true;
            }
            return false;
        } else {
            return false;
        }

    }

    public static void log(String message, Object... args) {
        Logger.e(String.format(getDefault(), message, args));
    }

    public static boolean isSupportRange(Response<?> response) {
        return !((TextUtils.isEmpty(contentRange(response)) && !TextUtils.equals(acceptRanges(response), "bytes")) ||
                contentLength(response) == -1 || isChunked(response));
    }

    private static long contentLength(Response<?> response) {
        return HttpHeaders.contentLength(response.headers());
    }

    private static CharSequence acceptRanges(Response<?> response) {
        return response.headers().get("Accept-Ranges");
    }

    private static CharSequence contentRange(Response<?> response) {
        return response.headers().get("Content-Range");
    }

    public static void mkdirs(String... paths) {
        for (String each : paths) {
            File file = new File(each);
            if (file.exists() && file.isDirectory()) {
                log(DIR_EXISTS_HINT, each);
            } else {
                log(DIR_NOT_EXISTS_HINT, each);
                log(file.mkdirs() ? DIR_CREATE_SUCCESS : DIR_CREATE_FAILED, each);
            }
        }
    }

    public static <U> FlowableTransformer<U, U> retry2(final String retryHint, final int maxRetryCount) {
        return new FlowableTransformer<U, U>() {
            @Override
            public Publisher<U> apply(Flowable<U> upstream) {
                return upstream.retry(new BiPredicate<Integer, Throwable>() {
                    @Override
                    public boolean test(@NonNull Integer integer, @NonNull Throwable throwable) throws Exception {
                        return retry(retryHint, maxRetryCount, integer, throwable);
                    }
                });
            }
        };
    }
}
