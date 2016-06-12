package com.abborg.glom.interfaces;

/**
 * Created by jitrapon on 7/6/16.
 */
public interface FileDownloadListener {

    void onDownloadStarted(String path);

    void onDownloadInProgress(String path, int progress);

    void onDownloadCompleted(String path);

    void onDownloadFailed(String error);
}
