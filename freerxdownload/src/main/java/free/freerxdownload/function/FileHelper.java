package free.freerxdownload.function;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import free.freerxdownload.entity.DownloadRange;
import free.freerxdownload.entity.DownloadStatus;
import io.reactivex.FlowableEmitter;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static free.freerxdownload.function.Constant.CHUNKED_DOWNLOAD_HINT;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static okhttp3.internal.Util.closeQuietly;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/25 0025
 * github：
 */

public class FileHelper {
    private static final int EACH_RECORD_SIZE = 16; //long + long = 8 + 8

    //    表示以只读方式("r")，以读写方式("rw")打开文件
    private static final String ACCESS = "rw";

    private int RECORD_FILE_TOTAL_SIZE;

    private int maxThreads;

    public FileHelper(int maxThreads) {
        this.maxThreads = maxThreads;
        RECORD_FILE_TOTAL_SIZE = EACH_RECORD_SIZE * maxThreads;
    }

    public void prepareDownload(File lastModifyFile, File tempFile, File saveFile, long fileLength, String lastModify) throws IOException, ParseException {
        //记录修改时间
        writeLastModify(lastModifyFile, lastModify);
        prepareFile(tempFile, saveFile, fileLength);
    }

    public void prepareDownload(File lastModifyFile, File saveFile, long fileLength, String lastModify)
            throws IOException, ParseException {

        writeLastModify(lastModifyFile, lastModify);
        prepareFile(saveFile, fileLength);
    }

    private void prepareFile(File saveFile, long fileLength) throws IOException {

        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(saveFile, ACCESS);
            if (fileLength != -1) {
                file.setLength(fileLength);//设置下载文件的长度
            } else {
                Logger.e(CHUNKED_DOWNLOAD_HINT);
                //Chunked 下载, 无需设置文件大小.
            }
        } finally {
            closeQuietly(file);
        }
    }

    private void prepareFile(File tempFile, File saveFile, long fileLength) throws IOException {
        RandomAccessFile rFile = null;
        RandomAccessFile rRecord = null;
        FileChannel channel = null;

        try {
            rFile = new RandomAccessFile(saveFile, ACCESS);
            rFile.setLength(fileLength);//设置下载文件的长度

            rRecord = new RandomAccessFile(tempFile, ACCESS);
            rRecord.setLength(RECORD_FILE_TOTAL_SIZE); //设置指针记录文件的大小

            channel = rRecord.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);

            long start;
            long end;
            int eachSize = (int) (fileLength / maxThreads);

            for (int i = 0; i < maxThreads; i++) {
                if (i == maxThreads - 1) {
                    start = i * eachSize;
                    end = fileLength - 1;
                } else {
                    start = i * eachSize;
                    end = (i + 1) * eachSize - 1;
                }
                buffer.putLong(start);
                buffer.putLong(end);
            }
        } finally {
            closeQuietly(channel);
            closeQuietly(rRecord);
            closeQuietly(rFile);
        }
    }

    private void writeLastModify(File lastModifyFile, String lastModify) throws IOException, ParseException {
        RandomAccessFile record = null;
        try {
            record = new RandomAccessFile(lastModifyFile, ACCESS);
//            设置此文件的长度
            record.setLength(8);
            // 把文件指针位置设置到文件起始处
            record.seek(0);
            record.writeLong(GMTToLong(lastModify));
        } finally {
            closeQuietly(record);
        }
    }

    private long GMTToLong(String GMT) throws ParseException {
        if (GMT == null || "".equals(GMT)) {
            return new Date().getTime();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = sdf.parse(GMT);
        return date.getTime();
    }

    public void saveFile(FlowableEmitter<DownloadStatus> emitter, File saveFile, Response<ResponseBody> resp) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            int readLen;
            int downloadSize = 0;
            byte[] buffer = new byte[8192];

            DownloadStatus status = new DownloadStatus();
            inputStream = resp.body().byteStream();
            outputStream = new FileOutputStream(saveFile);

            long contentLength = resp.body().contentLength();

            boolean chunked = Utils.isChunked(resp);
            if (chunked || contentLength == -1) {
                status.setChunked(true);
            }

            status.setTotal_size(contentLength);

            while ((readLen = inputStream.read(buffer)) != -1 && !emitter.isCancelled()) {
                outputStream.write(buffer, 0, readLen);
                downloadSize += readLen;
                status.setDownload_size(downloadSize);
                emitter.onNext(status);
            }
            // 刷新此输出流并强制写出所有缓冲的输出字节
            outputStream.flush(); // This is important!!!
            emitter.onComplete();
        } catch (IOException e) {
            emitter.onError(e);
        } finally {
            closeQuietly(inputStream);
            closeQuietly(outputStream);
            closeQuietly(resp.body());
        }
    }

    public void saveFile(FlowableEmitter<DownloadStatus> emitter, int i, File tempFile, File saveFile, ResponseBody response) {
        RandomAccessFile record = null;
        FileChannel recordChannel = null;
        RandomAccessFile save = null;
        FileChannel saveChannel = null;
        InputStream inStream = null;

        try {
            int readLen;
            byte[] buffer = new byte[2048];

            DownloadStatus status = new DownloadStatus();
            record = new RandomAccessFile(tempFile, ACCESS);
//            返回与此文件关联的唯一 FileChannel 对象
            recordChannel = record.getChannel();
//            将此通道的文件区域直接映射到内存中。
            MappedByteBuffer recordBuffer = recordChannel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);

            int startIndex = i * EACH_RECORD_SIZE;

//            读取给定索引处的 8 个字节，根据当前的字节顺序将它们组成 long 值。
            long start = recordBuffer.getLong(startIndex);
            long totalSize = recordBuffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
            status.setTotal_size(totalSize);

            save = new RandomAccessFile(saveFile, ACCESS);
            saveChannel = save.getChannel();

            inStream = response.byteStream();

            while ((readLen = inStream.read(buffer)) != -1 && !emitter.isCancelled()) {
                MappedByteBuffer saveBuffer = saveChannel.map(READ_WRITE, start, readLen);
                start += readLen;
                saveBuffer.put(buffer, 0, readLen);
                recordBuffer.putLong(startIndex, start);

                status.setDownload_size(totalSize - getResidue(recordBuffer));
                emitter.onNext(status);
            }

        } catch (IOException e) {
            emitter.onError(e);
        } finally {
            closeQuietly(record);
            closeQuietly(recordChannel);
            closeQuietly(save);
            closeQuietly(saveChannel);
            closeQuietly(inStream);
            closeQuietly(response);
        }
    }

    /**
     * 还剩多少字节没有下载
     *
     * @param recordBuffer
     * @return 剩余的字节
     */
    private long getResidue(MappedByteBuffer recordBuffer) {
        long residue = 0, startTemp, endTemp, temp;
        for (int i = 0; i < maxThreads; i++) {
            startTemp = recordBuffer.getLong(i * EACH_RECORD_SIZE);
            endTemp = recordBuffer.getLong(i * EACH_RECORD_SIZE + 8);
            temp = endTemp - startTemp + 1;
            residue += temp;
        }
        return residue;
    }

    public boolean fileNotComplete(File tempFile) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(tempFile, ACCESS);
            channel = record.getChannel();

            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);

            long startByte, endByte;
            for (int i = 0; i < maxThreads; i++) {
                startByte = buffer.getLong();
                endByte = buffer.getLong();
                if (startByte <= endByte) {
                    return true;
                }
            }

            return false;

        } finally {
            closeQuietly(channel);
            closeQuietly(record);
        }
    }

    public boolean tempFileDamaged(File tempFile, long fileLength) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(tempFile, ACCESS);
            channel = record.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
            long recordTotalSize = buffer.getLong(RECORD_FILE_TOTAL_SIZE);
            return recordTotalSize != fileLength;
        } finally {
            closeQuietly(channel);
            closeQuietly(record);
        }
    }

    public DownloadRange readDownloadRange(File tempFile, int i) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(tempFile, ACCESS);
            channel = record.getChannel();

            MappedByteBuffer buffer = channel.map(READ_WRITE, i * EACH_RECORD_SIZE, (i + 1) * EACH_RECORD_SIZE);

            return new DownloadRange(buffer.getLong(), buffer.getLong());
        } finally {
            closeQuietly(channel);
            closeQuietly(record);
        }
    }

    public String readLastModify(File lastModifyFile) throws IOException {
        RandomAccessFile record = null;
        try {
            record = new RandomAccessFile(lastModifyFile, ACCESS);
            record.seek(0);
            return Utils.longToGMT(record.readLong());
        } finally {
            closeQuietly(record);
        }
    }

}
