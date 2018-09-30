package top.toly.zutils.phone.media.song;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

import top.toly.zutils.core.domain.song.Album;
import top.toly.zutils.core.domain.song.Artist;
import top.toly.zutils.core.domain.song.ArtistMusicInfo;
import top.toly.zutils.core.domain.song.Song;


/**
 * 作者：张风捷特烈
 * 时间：2018/7/14:5:49
 * 邮箱：1981462002@qq.com
 * 说明：歌曲扫描器
 */
public class MusicScanner extends MusicDao {
    private Activity context;
    private static List<Song> songs; // 包含为整理的所有音乐


    public MusicScanner(Activity context) {
        super(context);
        this.context = context;
        songs = new ArrayList<>();
    }

    @Override
    public Loader<Cursor> onLoaderCreate(Uri contentUri, int id, Bundle args) {

        String[] projection = new String[]{
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.IS_MUSIC};
        return new CursorLoader(context, contentUri, projection,
                null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onFinishedLoader(Loader<Cursor> loader, Cursor data, LoaderManager manager) {
        if (data != null) {
            // 清除旧数据
            songs.clear();
            // 获取所需列的索引
            int albumIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int artistIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int titleIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int durationIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int songIdIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int albumIdIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int artistIdIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
            int dataUrlIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int isMusicIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC);

            while (data.moveToNext()) {
                int isMusic = data.getInt(isMusicIdx);
                if (isMusic != 0) {
                    String album = data.getString(albumIdx);
                    String artist = data.getString(artistIdx);
                    String title = data.getString(titleIdx);
                    String dataUrl = data.getString(dataUrlIdx);
                    long duration = data.getLong(durationIdx);
                    long songId = data.getLong(songIdIdx);
                    long albumId = data.getLong(albumIdIdx);
                    long artistId = data.getLong(artistIdIdx);
                    Song item = new Song(new Album(albumId, album,
                            new Artist(artistId, artist,
                                    new ArtistMusicInfo()), null),
                            title, songId, dataUrl, duration);
                    songs.add(item);
                }
            }
            // 歌曲检索完毕
            manager.destroyLoader(SONG_LOADER_ID); // 销毁loader
            if (mOnFinish != null) {
                mOnFinish.scanOver(songs);
            }
        }
    }

    @Override
    public void onResetLoader(Loader<Cursor> loader) {
        songs.clear();
    }

    //////////////////////////////////扫描歌曲结束监听------------------
    public interface OnFinish {
        void scanOver(List<Song> songs);
    }

    private OnFinish mOnFinish;

        public MusicScanner setOnFinish(OnFinish onFinish) {
        mOnFinish = onFinish;
        return this;
    }

    public static List<Song> getSongs() {
        return songs;
    }
}
