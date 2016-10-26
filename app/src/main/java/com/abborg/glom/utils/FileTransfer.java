package com.abborg.glom.utils;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.Const;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.CloudProvider;
import com.abborg.glom.model.DrawItem;
import com.abborg.glom.model.FileItem;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles file transfer operations to supported cloud providers:
 * Dropbox, Google Drive, and Amazon S3. All file operations must be done
 * on a seperate thread than UI thread.
 *
 * Created by jitrapon on 12/5/16.
 */
public class FileTransfer {

    private DataProvider dataProvider;

    private static final String TAG = "FileTransfer";

    private Context context;

    /* The cloud to which the operation will be done from */
    private CloudProvider cloudProvider;

    /* Main thread handler */
    private Handler handler;

    private List<String> downloadList;

    private File downloadDir;

    /********** AMAZON S3 ************/
    private TransferUtility s3Transfer;
    private AmazonS3Client s3Client;

    public FileTransfer(Context ctx, ApplicationState appState, DataProvider dp) {
        dataProvider = dp;
        context = ctx;
        handler = dataProvider.getHandler();
        downloadList = Collections.synchronizedList(new ArrayList<String>());
        downloadDir = appState.getExternalFilesDir();
    }

    public void setHandler(Handler h) {
        handler = h;
    }

    public void upload(final CloudProvider provider, final Circle circle, final DrawItem item) {
        try {
            switch (provider) {
                case AMAZON_S3: {
                    if (s3Transfer == null) createAmazonS3Client();

                    Log.d(TAG, "Begin uploading " + item.getLocalCache().getName() + " to Amazon S3...");
                    s3Transfer.upload(
                            Const.AWS_S3_BUCKET,
                            circle.getId() + "/" + item.getLocalCache().getName(),
                            item.getLocalCache()
                    ).setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            Log.d(TAG, "Amazon S3 upload id " + id + " state changed to " + state.name());

                            if (handler != null) {
                                switch (state) {
                                    case CANCELED:
                                    case FAILED:
                                        dataProvider.setSyncStatus(item, Const.MSG_DRAWING_POST_FAILED, BoardItem.SYNC_ERROR);
                                        break;
                                    case COMPLETED:
                                        dataProvider.requestUpdateDrawing(circle, DateTime.now(), item);
                                        break;
                                }
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            if (bytesTotal != 0) {
                                int progress = (int) (bytesCurrent / bytesTotal * 100);
                                Log.d(TAG, "Amazon S3 upload id " + id + ", uploading at progress " + progress);
                                if (handler != null)
                                    handler.sendMessage(handler.obtainMessage(
                                            Const.MSG_DRAWING_POST_IN_PROGRESS, BoardItem.SYNC_IN_PROGRESS, progress, item));
                            }
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            Log.e(TAG, "Amazon S3 upload id " + id + " has encountered an error due to " + ex.getMessage());
                            dataProvider.setSyncStatus(item, Const.MSG_DRAWING_POST_FAILED, BoardItem.SYNC_ERROR);
                        }
                    });
                    break;
                }
                case DROPBOX: {

                    break;
                }
                case GOOGLE_DRIVE: {

                    break;
                }
                default:
                    break;
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            dataProvider.setSyncStatus(item, Const.MSG_DRAWING_POST_FAILED, BoardItem.SYNC_ERROR);
        }
    }

    public void upload(final CloudProvider provider, final Circle circle, final FileItem item) {
        try {
            switch (provider) {
                case AMAZON_S3: {
                    if (s3Transfer == null) createAmazonS3Client();

                    Log.d(TAG, "Begin uploading " + item.getName() + " to Amazon S3...");
                    s3Transfer.upload(
                            Const.AWS_S3_BUCKET,
                            circle.getId() + "/" + item.getName(),
                            item.getLocalCache()
                    ).setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            Log.d(TAG, "Amazon S3 upload id " + id + " state changed to " + state.name());

                            if (handler != null) {
                                switch (state) {
                                    case CANCELED:
                                    case FAILED:
                                        dataProvider.setSyncStatus(item, Const.MSG_FILE_POST_FAILED, BoardItem.SYNC_ERROR);
                                        break;
                                    case COMPLETED:
                                        dataProvider.requestPostFile(circle, item, provider);
                                        break;
                                }
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            int progress = (int) (bytesCurrent/bytesTotal * 100);
                            Log.d(TAG, "Amazon S3 upload id " + id + ", uploading at progress " + progress);
                            if (handler != null)
                                handler.sendMessage(handler.obtainMessage(
                                        Const.MSG_FILE_POST_IN_PROGRESS, BoardItem.SYNC_IN_PROGRESS, progress, item));
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            Log.e(TAG, "Amazon S3 upload id " + id + " has encountered an error due to " + ex.getMessage());
                            dataProvider.setSyncStatus(item, Const.MSG_FILE_POST_FAILED, BoardItem.SYNC_ERROR);
                        }
                    });
                    break;
                }
                case DROPBOX: {

                    break;
                }
                case GOOGLE_DRIVE: {

                    break;
                }
                default:
                    break;
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            dataProvider.setSyncStatus(item, Const.MSG_FILE_POST_FAILED, BoardItem.SYNC_ERROR);
        }
    }

    public void download(final CloudProvider provider, final Circle circle, final DrawItem item) {
        try {
            if (downloadList.contains(item.getId())) {
                Log.d(TAG, "File " + item.getName() + " is already being downloaded!");
                return;
            }

            switch (provider) {
                case AMAZON_S3: {
                    if (s3Transfer == null) createAmazonS3Client();
                    String name = TextUtils.isEmpty(item.getName()) ? item.getId() : item.getName();
                    final File tempFile = new File(downloadDir.getPath() + "/" + name + ".png");

                    Log.d(TAG, "Begin downloading " + name + " from Amazon S3...to " + tempFile.getPath());
                    downloadList.add(item.getId());
                    s3Transfer.download(
                            Const.AWS_S3_BUCKET,
                            circle.getId() + "/" + name + ".png",
                            tempFile
                    ).setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            Log.d(TAG, "Amazon S3 download id " + id + " state changed to " + state.name());

                            if (handler != null) {
                                switch (state) {
                                    case CANCELED:
                                    case FAILED:
                                        downloadList.remove(item.getId());
                                        handler.sendMessage(handler.obtainMessage(Const.MSG_DRAWING_DOWNLOAD_FAILED, item));
                                        break;
                                    case COMPLETED:
                                        downloadList.remove(item.getId());
                                        dataProvider.updateDrawingPath(circle, item.getId(), tempFile.getPath());
                                        break;
                                }
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            int progress = (int) (bytesCurrent/bytesTotal * 100);
                            Log.d(TAG, "Amazon S3 download id " + id + ", download at progress " + progress);
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            Log.e(TAG, "Amazon S3 download id " + id + " has encountered an error due to " + ex.getMessage());
                            downloadList.remove(item.getId());
                            if (handler != null)
                                handler.sendMessage(handler.obtainMessage(
                                        Const.MSG_DRAWING_DOWNLOAD_FAILED, item));
                        }
                    });

                    break;
                }
                default: break;
            }
        }
        catch (Exception ex) {
            downloadList.remove(item.getId());
            Log.e(TAG, ex.getMessage());
        }
    }

    public void download(final CloudProvider provider, final Circle circle, final FileItem item) {
        try {
            if (downloadList.contains(item.getId())) {
                Log.d(TAG, "File " + item.getName() + " is already being downloaded!");
                return;
            }

            switch (provider) {
                case AMAZON_S3: {
                    if (s3Transfer == null) createAmazonS3Client();
                    final File tempFile = new File(downloadDir.getPath() + "/" + item.getName());

                    Log.d(TAG, "Begin downloading " + item.getName() + " from Amazon S3...to " + tempFile.getPath());
                    downloadList.add(item.getId());
                    s3Transfer.download(
                            Const.AWS_S3_BUCKET,
                            circle.getId() + "/" + item.getName(),
                            tempFile
                    ).setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            Log.d(TAG, "Amazon S3 download id " + id + " state changed to " + state.name());

                            if (handler != null) {
                                switch (state) {
                                    case CANCELED:
                                    case FAILED:
                                        downloadList.remove(item.getId());
                                        handler.sendMessage(handler.obtainMessage(Const.MSG_FILE_DOWNLOAD_FAILED, item));
                                        break;
                                    case COMPLETED:
                                        downloadList.remove(item.getId());
                                        dataProvider.updateFilePath(circle, item.getId(), tempFile.getPath());
                                        break;
                                }
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            int progress = (int) (bytesCurrent/bytesTotal * 100);
                            Log.d(TAG, "Amazon S3 download id " + id + ", download at progress " + progress);
                            if (handler != null)
                                handler.sendMessage(handler.obtainMessage(
                                        Const.MSG_FILE_DOWNLOAD_IN_PROGRESS, progress, -1, item));
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            Log.e(TAG, "Amazon S3 download id " + id + " has encountered an error due to " + ex.getMessage());
                            downloadList.remove(item.getId());
                            if (handler != null)
                                handler.sendMessage(handler.obtainMessage(
                                        Const.MSG_FILE_DOWNLOAD_FAILED, item));
                        }
                    });

                    break;
                }
                default: break;
            }
        }
        catch (Exception ex) {
            downloadList.remove(item.getId());
            Log.e(TAG, ex.getMessage());
        }
    }

    public void delete(CloudProvider provider, Circle circle, FileItem item) {
        try {
            switch (provider) {
                case AMAZON_S3: {
                    if (s3Client == null) createAmazonS3Client();

                    Log.d(TAG, "Attempting to delete " + item.getName() + " from Amazon S3...");
                    s3Client.deleteObject(Const.AWS_S3_BUCKET, circle.getId() + "/" + item.getName());
                    dataProvider.requestDeleteItem(circle, item);

                    break;
                }
                case DROPBOX: {

                    break;
                }
                case GOOGLE_DRIVE: {

                    break;
                }
                default:
                    break;
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            if (handler != null) {
                handler.sendMessage(handler.obtainMessage(Const.MSG_ITEM_DELETED_FAILED, item));
            }
        }
    }

    public void delete(CloudProvider provider, Circle circle, DrawItem item) {
        try {
            switch (provider) {
                case AMAZON_S3: {
                    if (s3Client == null) createAmazonS3Client();

                    String name = TextUtils.isEmpty(item.getName()) ? item.getId() : item.getName();
                    Log.d(TAG, "Attempting to delete " + name + " from Amazon S3...");
                    s3Client.deleteObject(Const.AWS_S3_BUCKET, circle.getId() + "/" + name + ".png");
                    dataProvider.requestDeleteItem(circle, item);

                    break;
                }
                case DROPBOX: {

                    break;
                }
                case GOOGLE_DRIVE: {

                    break;
                }
                default:
                    break;
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            if (handler != null) {
                handler.sendMessage(handler.obtainMessage(Const.MSG_ITEM_DELETED_FAILED, item));
            }
        }
    }

    /******************************************************
     * VENDOR-SPECIFIC OPERATIONS
     ******************************************************/
    private void createAmazonS3Client() {
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                Const.AWS_IDENTIY_POOL_ID,  // Identity Pool ID
                Regions.US_EAST_1           // Region
        );
        s3Client = new AmazonS3Client(credentialsProvider);
        s3Transfer = new TransferUtility(s3Client, context.getApplicationContext());
    }
}
