package free.freerxdownload.entity;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/25 0025
 * github：
 */

public class DownloadBean extends RealmObject {

    @PrimaryKey
    private int id;
    @Required
    private String url;
    private String save_name;
    private String save_path;
    private int download_flag;
    private boolean chunked;
    private long total_size;
    private long download_size;
    private String extra1;
    private String extra2;
    private String extra3;
    private String extra4;
    private String extra5;
    @Required
    private Date date;
    // 用于一组任务 同时暂停、进行、删除等操作
    private String mission_id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSave_name() {
        return save_name;
    }

    public void setSave_name(String save_name) {
        this.save_name = save_name;
    }

    public String getSave_path() {
        return save_path;
    }

    public void setSave_path(String save_path) {
        this.save_path = save_path;
    }

    public int getDownload_flag() {
        return download_flag;
    }

    public void setDownload_flag(int download_flag) {
        this.download_flag = download_flag;
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

    public String getExtra1() {
        return extra1;
    }

    public void setExtra1(String extra1) {
        this.extra1 = extra1;
    }

    public String getExtra2() {
        return extra2;
    }

    public void setExtra2(String extra2) {
        this.extra2 = extra2;
    }

    public String getExtra3() {
        return extra3;
    }

    public void setExtra3(String extra3) {
        this.extra3 = extra3;
    }

    public String getExtra4() {
        return extra4;
    }

    public void setExtra4(String extra4) {
        this.extra4 = extra4;
    }

    public String getExtra5() {
        return extra5;
    }

    public void setExtra5(String extra5) {
        this.extra5 = extra5;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMission_id() {
        return mission_id;
    }

    public void setMission_id(String mission_id) {
        this.mission_id = mission_id;
    }

    public static class Builder {
        private String url;
        private String saveName;
        private String savePath;
        private String extra1;
        private String extra2;
        private String extra3;
        private String extra4;
        private String extra5;

        public Builder(String url) {
            this.url = url;
        }

        public Builder setSaveName(String saveName) {
            this.saveName = saveName;
            return this;
        }

        public Builder setSavePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        public DownloadBean build() {
            DownloadBean bean = new DownloadBean();
            bean.url = this.url;
            bean.save_name = this.saveName;
            bean.save_path = this.savePath;
            bean.extra1 = this.extra1;
            bean.extra2 = this.extra2;
            bean.extra3 = this.extra3;
            bean.extra4 = this.extra4;
            bean.extra5 = this.extra5;
            return bean;
        }
    }
}
