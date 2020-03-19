package com.llw.music;

import android.Manifest;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.llw.music.adapter.MusicListAdapter;
import com.llw.music.model.Song;
import com.llw.music.utils.Constant;
import com.llw.music.utils.MusicUtils;
import com.llw.music.utils.ObjectUtils;
import com.llw.music.utils.SPUtils;
import com.llw.music.utils.StatusBarUtil;
import com.llw.music.utils.ToastUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.llw.music.utils.DateUtil.parseTime;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    @BindView(R.id.rv_music)
    RecyclerView rvMusic;
    @BindView(R.id.btn_scan)
    Button btnScan;
    @BindView(R.id.scan_lay)
    LinearLayout scanLay;
    @BindView(R.id.tv_clear_list)
    TextView tvClearList;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tv_play_time)
    TextView tvPlayTime;
    @BindView(R.id.time_seekBar)
    SeekBar timeSeekBar;
    @BindView(R.id.tv_total_time)
    TextView tvTotalTime;
    @BindView(R.id.btn_previous)
    ImageView btnPrevious;
    @BindView(R.id.btn_play_or_pause)
    ImageView btnPlayOrPause;
    @BindView(R.id.btn_next)
    ImageView btnNext;
    @BindView(R.id.tv_play_song_info)
    TextView tvPlaySongInfo;
    @BindView(R.id.song_image)
    CircleImageView songImage;
    @BindView(R.id.play_state_img)
    ImageView playStateImg;
    @BindView(R.id.play_state_lay)
    LinearLayout playStateLay;

    private MusicListAdapter mAdapter;//歌曲适配器
    private List<Song> mList;//歌曲列表
    private RxPermissions rxPermissions;//权限请求
    private MediaPlayer mediaPlayer;//音频播放器
    private String musicData = null;
    // 记录当前播放歌曲的位置
    public int mCurrentPosition;
    private static final int INTERNAL_TIME = 1000;// 音乐进度间隔时间

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            // 展示给进度条和当前时间
            int progress = mediaPlayer.getCurrentPosition();
            timeSeekBar.setProgress(progress);
            tvPlayTime.setText(parseTime(progress));
            // 继续定时发送数据
            updateProgress();
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        StatusBarUtil.StatusBarLightMode(this);
        rxPermissions = new RxPermissions(this);//使用前先实例化
        timeSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);//滑动条监听
        musicData = SPUtils.getString(Constant.MUSIC_DATA_FIRST, "yes", this);

        if (musicData.equals("no")) {//说明是第一次打开APP，未进行扫描
            scanLay.setVisibility(View.GONE);
            initMusic();
        } else {
            scanLay.setVisibility(View.VISIBLE);
        }
    }

    private void permissionRequest() {//使用这个框架需要制定JDK版本，建议用1.8
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (granted) {//请求成功之后开始扫描
                        initMusic();
                    } else {//失败时给一个提示
                        ToastUtils.showShortToast(MainActivity.this, "未授权");
                    }
                });
    }

    //获取音乐列表
    private void initMusic() {
        mList = new ArrayList<>();//实例化

        //数据赋值
        mList = MusicUtils.getMusicData(this);//将扫描到的音乐赋值给音乐列表
        if (!ObjectUtils.isEmpty(mList) && mList != null) {
            scanLay.setVisibility(View.GONE);
            SPUtils.putString(Constant.MUSIC_DATA_FIRST, "no", this);
        }
        mAdapter = new MusicListAdapter(R.layout.item_music_rv_list, mList, MainActivity.this);//指定适配器的布局和数据源
        //线性布局管理器，可以设置横向还是纵向，RecyclerView默认是纵向的，所以不用处理,如果不需要设置方向，代码还可以更加的精简如下
        rvMusic.setLayoutManager(new LinearLayoutManager(this));
        //如果需要设置方向显示，则将下面代码注释去掉即可
//        LinearLayoutManager manager = new LinearLayoutManager(this);
//        manager.setOrientation(RecyclerView.HORIZONTAL);
//        rvMusic.setLayoutManager(manager);

        //设置适配器
        rvMusic.setAdapter(mAdapter);

        //item的点击事件
        mAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                if (view.getId() == R.id.item_music) {
                    mCurrentPosition = position;
                    changeMusic(mCurrentPosition);
//                    mAdapter.notifyItemRangeChanged(0, mList.size());

                }
            }
        });

        //设置背景样式
        initStyle();

    }


    private void initStyle() {
        //toolbar背景变透明
        toolbar.setBackgroundColor(getResources().getColor(R.color.half_transparent));
        //文字变白色
        tvTitle.setTextColor(getResources().getColor(R.color.white));
        tvClearList.setTextColor(getResources().getColor(R.color.white));
        StatusBarUtil.transparencyBar(this);
    }

    @OnClick({R.id.tv_clear_list, R.id.btn_scan, R.id.btn_previous, R.id.btn_play_or_pause, R.id.btn_next})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_clear_list://清空数据
                mList.clear();
                mAdapter.notifyDataSetChanged();
                SPUtils.putString(Constant.MUSIC_DATA_FIRST, "yes", this);
                scanLay.setVisibility(View.VISIBLE);
                toolbar.setBackgroundColor(getResources().getColor(R.color.white));
                StatusBarUtil.StatusBarLightMode(this);
                tvTitle.setTextColor(getResources().getColor(R.color.black));
                tvClearList.setTextColor(getResources().getColor(R.color.black));
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    mediaPlayer.reset();
                }
                break;
            case R.id.btn_scan://扫描本地歌曲
                permissionRequest();
                break;
            case R.id.btn_previous://上一曲
                changeMusic(--mCurrentPosition);//当前歌曲位置减1
                break;
            case R.id.btn_play_or_pause://播放或者暂停
                // 首次点击播放按钮，默认播放第0首，下标从0开始
                if (mediaPlayer == null) {
                    changeMusic(0);
                } else {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        btnPlayOrPause.setBackground(getResources().getDrawable(R.mipmap.icon_pause));
                        playStateImg.setBackground(getResources().getDrawable(R.mipmap.list_play_state));
                        //如果你是用TextView的leftDrawable设置的图片，在代码里面就可以通过下面代码来动态更换
//                        Drawable leftImg = getResources().getDrawable(R.mipmap.list_play_state);
//                        leftImg.setBounds(0, 0, leftImg.getMinimumWidth(), leftImg.getMinimumHeight());
//                        tvPlaySongInfo.setCompoundDrawables(leftImg, null, null, null);
                    } else {
                        mediaPlayer.start();
                        btnPlayOrPause.setBackground(getResources().getDrawable(R.mipmap.icon_play));
                        playStateImg.setBackground(getResources().getDrawable(R.mipmap.list_pause_state));
                    }
                }
                break;
            case R.id.btn_next://下一曲
                changeMusic(++mCurrentPosition);//当前歌曲位置加1
                break;
        }
    }

    //切歌
    private void changeMusic(int position) {
        Log.e("MainActivity", "position:" + position);
        if (position < 0) {
            mCurrentPosition = position = mList.size() - 1;
            Log.e("MainActivity", "mList.size:" + mList.size());
        } else if (position > mList.size() - 1) {
            mCurrentPosition = position = 0;
        }
        Log.e("MainActivity", "position:" + position);
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }

        try {
            // 切歌之前先重置，释放掉之前的资源
            mediaPlayer.reset();
            // 设置播放源
            Log.d("Music", mList.get(position).path);
            mediaPlayer.setDataSource(mList.get(position).path);
            tvPlaySongInfo.setText("歌名： " + mList.get(position).song +
                    "  歌手： " + mList.get(position).singer);

//            Glide.with(this).load(mList.get(position).album_art).into(songImage);
            tvPlaySongInfo.setSelected(true);//跑马灯效果
            playStateLay.setVisibility(View.VISIBLE);

            // 开始播放前的准备工作，加载多媒体资源，获取相关信息
            mediaPlayer.prepare();
            // 开始播放
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 切歌时重置进度条并展示歌曲时长
        timeSeekBar.setProgress(0);
        timeSeekBar.setMax(mediaPlayer.getDuration());
        tvTotalTime.setText(parseTime(mediaPlayer.getDuration()));

        updateProgress();
        if (mediaPlayer.isPlaying()) {
            btnPlayOrPause.setBackground(getResources().getDrawable(R.mipmap.icon_play));
            playStateImg.setBackground(getResources().getDrawable(R.mipmap.list_pause_state));
        } else {
            btnPlayOrPause.setBackground(getResources().getDrawable(R.mipmap.icon_pause));
            playStateImg.setBackground(getResources().getDrawable(R.mipmap.list_play_state));
        }
    }

    private void updateProgress() {
        // 使用Handler每间隔1s发送一次空消息，通知进度条更新
        Message msg = Message.obtain();// 获取一个现成的消息
        // 使用MediaPlayer获取当前播放时间除以总时间的进度
        int progress = mediaPlayer.getCurrentPosition();
        msg.arg1 = progress;
        mHandler.sendMessageDelayed(msg, INTERNAL_TIME);
    }

    //滑动条监听
    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        // 当手停止拖拽进度条时执行该方法
        // 获取拖拽进度
        // 将进度对应设置给MediaPlayer
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            mediaPlayer.seekTo(progress);
        }
    };

    //播放完成之后自动下一曲
    @Override
    public void onCompletion(MediaPlayer mp) {
        changeMusic(++mCurrentPosition);
    }
}
