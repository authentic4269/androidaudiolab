package com.example.wal;

import java.util.LinkedList;

public interface ProgressUpdateListener {
	public void onProgress(int progress);
	public void onExercisesDownloaded(LinkedList<String> exercises);
	public void onLessonsDownloaded(LinkedList<String> lessons);
	public void onTracksDownloaded(LinkedList<Track> tracks);
}
