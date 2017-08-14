package com.android.systemui.statusbar.phoneleather.model;

public class LeatherPlayerInfo {

	private String iconUrl; // 在线歌曲封面
	private String filePath; // 本地歌曲路径
	private boolean isPlaying; // 是否正在播放
	private String musicName; // 音乐名称
	private String artist; // 歌手名称
	private int position; // 播放进度
	private int duration; //歌曲时长
	private boolean isLoading; //是否正在加载

	public LeatherPlayerInfo(String iconUrl, String filePath, boolean isPlaying, String musicName, String artist,int position, int duration, boolean isLoading) {
		super();
		this.iconUrl = iconUrl;
		this.filePath = filePath;
		this.isPlaying = isPlaying;
		this.musicName = musicName;
		this.artist = artist;
		this.position = position;
		this.duration = duration;
		this.isLoading = isLoading;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	public void setPlaying(boolean isPlaying) {
		this.isPlaying = isPlaying;
	}

	public String getMusicName() {
		return musicName;
	}

	public void setMusicName(String musicName) {
		this.musicName = musicName;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public boolean isLoading() {
		return isLoading;
	}

	public void setLoading(boolean loading) {
		isLoading = loading;
	}
}
