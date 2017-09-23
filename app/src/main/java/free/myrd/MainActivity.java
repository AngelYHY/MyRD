package free.myrd;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import free.freerxdownload.RxDownload;
import free.freerxdownload.entity.DownloadStatus;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    public final static String URL = "http://dldir1.qq.com/weixin/android/weixin6330android920.apk";

    @BindView(R.id.btn)
    Button mBtn;

    private int flag = -1; // -1 未开始 0 暂停  1 进行中  2 结束

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

    }

    @OnClick(R.id.btn)
    public void onViewClicked() {
        switch (flag) {
            case -1:
                mBtn.setText("开始");
                RxDownload.getInstance(this)
                        .download(URL)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<DownloadStatus>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(DownloadStatus status) {

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
                break;
            case 0:
                break;
            case 1:
                break;
        }
    }
}
