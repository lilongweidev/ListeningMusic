package com.llw.music.adapter;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.llw.music.MainActivity;
import com.llw.music.R;
import com.llw.music.model.Song;
import com.llw.music.utils.MusicUtils;

import java.util.List;

public class MusicListAdapter extends BaseQuickAdapter<Song, BaseViewHolder>{
    MainActivity mainActivity;

    public MusicListAdapter(int layoutResId, @Nullable List<Song> data,MainActivity activity) {
        super(layoutResId, data);
        mainActivity =activity;
    }


    @Override
    protected void convert(BaseViewHolder helper, Song item) {
        //给控件赋值
        int duration = item.duration;
        String time = MusicUtils.formatTime(duration);

        helper.setText(R.id.tv_song_name,item.getSong().trim())//歌曲名称
                .setText(R.id.tv_singer,item.getSinger()+" - "+item.getAlbum())//歌手 - 专辑
                .setText(R.id.tv_duration_time,time)//歌曲时间
                //歌曲序号，因为getAdapterPosition得到的位置是从0开始，故而加1，
                //是因为位置和1都是整数类型，直接赋值给TextView会报错，故而拼接了""
                .setText(R.id.tv_position,helper.getAdapterPosition()+1+"");

        helper.addOnClickListener(R.id.item_music);//给item添加点击事件，点击之后传递数据到播放页面或者在本页面进行音乐播放

        /*if (helper.getAdapterPosition() == mainActivity.mCurrentPosition) {
            helper.setTextColor(R.id.tv_song_name,mContext.getResources().getColor(R.color.greens));
            helper.setTextColor(R.id.tv_singer,mContext.getResources().getColor(R.color.greens));
            helper.setTextColor(R.id.tv_duration_time,mContext.getResources().getColor(R.color.greens));
            helper.setVisible(R.id.view,true);
//            iconPlay.setVisibility(View.VISIBLE);
        } else {
            helper.setTextColor(R.id.tv_song_name,mContext.getResources().getColor(R.color.white));
            helper.setTextColor(R.id.tv_singer,mContext.getResources().getColor(R.color.white));
            helper.setTextColor(R.id.tv_duration_time,mContext.getResources().getColor(R.color.white));
            helper.setVisible(R.id.view,false);
        }*/

    }
}
