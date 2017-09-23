package free.freerxdownload;

import android.content.Context;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;

import java.util.List;

import free.freerxdownload.entity.DownloadBean;
import free.freerxdownload.entity.DownloadStatus;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

import static free.freerxdownload.entity.DownloadFlag.PAUSED;
import static free.freerxdownload.entity.DownloadFlag.STARTED;
import static free.freerxdownload.entity.DownloadFlag.WAITING;
import static free.freerxdownload.function.Constant.COLUMN_DOWNLOAD_FLAG;
import static free.freerxdownload.function.Constant.COLUMN_MISSION_ID;


/**
 * 描述：
 * 作者：一颗浪星
 * 日期：2017/8/2 0002
 * github：
 */

public class DataBaseHelper {

    private final Realm mRealm;

    private DataBaseHelper(Context context) {
        Realm.init(context);

        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .name("download_record")
                .schemaVersion(2)
//                .migration(new CustomMigration())//升级数据库
                //.deleteRealmIfMigrationNeeded()
                .build();
        Logger.e(Thread.currentThread().getName() + "---" + Thread.currentThread().getId());
        mRealm = Realm.getInstance(configuration);
    }

    private static DataBaseHelper singleton;

    public static DataBaseHelper getSingleton(Context context) {
        if (singleton == null) {
            synchronized (DataBaseHelper.class) {
                if (singleton == null) {
                    singleton = new DataBaseHelper(context);
                }
            }
        }
        return singleton;
    }

    public boolean recordNotExists(String url) {
        Logger.e(Thread.currentThread().getName() + "---" + Thread.currentThread().getId());
        return mRealm.where(DownloadBean.class).equalTo("url", url).findFirst() == null;
    }

    public void insertRecord(DownloadBean downloadBean) {
        mRealm.beginTransaction();
        mRealm.copyToRealm(downloadBean);
        mRealm.commitTransaction();
    }

    public void updateStatus(String url, DownloadStatus status) {
        DownloadBean bean = mRealm.where(DownloadBean.class).equalTo("url", url).findFirst();
        mRealm.beginTransaction();
        bean.setTotal_size(status.getTotal_size());
        bean.setDownload_size(status.getDownload_size());
        bean.setChunked(status.isChunked());
        mRealm.commitTransaction();
    }

    public void updateRecord(String url, int flag) {
        DownloadBean bean = mRealm.where(DownloadBean.class).equalTo("url", url).findFirst();
        mRealm.beginTransaction();
        bean.setDownload_flag(flag);
        mRealm.commitTransaction();
    }

    public void updateRecord(String url, int flag, String missionId) {
        DownloadBean bean = mRealm.where(DownloadBean.class).equalTo("url", url).findFirst();
        mRealm.beginTransaction();
        bean.setDownload_flag(flag);
        bean.setMission_id(missionId);
        mRealm.commitTransaction();
    }

    public void updateRecord(String url, String saveName, String savePath, int flag) {
        DownloadBean bean = mRealm.where(DownloadBean.class).equalTo("url", url).findFirst();
        mRealm.beginTransaction();
        bean.setDownload_flag(flag);
        bean.setSave_name(saveName);
        bean.setSave_path(savePath);
        mRealm.commitTransaction();
    }

    public void deleteRecord(String url) {
        DownloadBean bean = mRealm.where(DownloadBean.class).equalTo("url", url).findFirst();
        mRealm.beginTransaction();
        bean.deleteFromRealm();
//        RealmObject.deleteFromRealm(bean);//  实体类实现接口的时候只能用这种方法
        mRealm.commitTransaction();
    }

    public void repairErrorFlag() {
        RealmResults<DownloadBean> list = mRealm.where(DownloadBean.class)
                .equalTo(COLUMN_DOWNLOAD_FLAG, WAITING)
                .or().equalTo(COLUMN_DOWNLOAD_FLAG, STARTED)
                .findAll();

        mRealm.beginTransaction();
        for (DownloadBean bean : list) {
            bean.setDownload_flag(PAUSED);
        }
        mRealm.commitTransaction();
    }

    @Nullable
    public DownloadBean readSingleRecord(String url) {
        return mRealm.where(DownloadBean.class).equalTo("url", url).findFirst();
    }

    public List<DownloadBean> readMissionsRecord(String missionId) {
        RealmResults<DownloadBean> results = mRealm.where(DownloadBean.class).equalTo(COLUMN_MISSION_ID, missionId).findAll();
        return mRealm.copyFromRealm(results);
    }

    public DownloadStatus readStatus(String url) {
        DownloadBean bean = mRealm.where(DownloadBean.class).equalTo("url", url).findFirst();
        return new DownloadStatus(bean.isChunked(), bean.getDownload_size(), bean.getTotal_size());
    }

    public Observable<List<DownloadBean>> readAllRecords() {
        return Observable
                .create(new ObservableOnSubscribe<List<DownloadBean>>() {
                    @Override
                    public void subscribe(ObservableEmitter<List<DownloadBean>> emitter)
                            throws Exception {
                        emitter.onNext(mRealm.copyFromRealm(mRealm.where(DownloadBean.class).findAll()));
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<DownloadBean> readRecord(final String url) {
        return Observable
                .create(new ObservableOnSubscribe<DownloadBean>() {
                    @Override
                    public void subscribe(ObservableEmitter<DownloadBean> emitter) throws Exception {
                        emitter.onNext(mRealm.where(DownloadBean.class).equalTo("url", url).findFirst());
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
