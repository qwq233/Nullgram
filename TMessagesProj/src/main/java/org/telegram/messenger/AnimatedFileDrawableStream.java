/*
 * Copyright (C) 2019-2024 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this software.
 *  If not, see
 * <https://www.gnu.org/licenses/>
 */

package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

import java.util.concurrent.CountDownLatch;

public class AnimatedFileDrawableStream implements FileLoadOperationStream {

    private FileLoadOperation loadOperation;
    private CountDownLatch countDownLatch;
    private TLRPC.Document document;
    private ImageLocation location;
    private Object parentObject;
    private int currentAccount;
    private volatile boolean canceled;
    private final Object sync = new Object();
    private long lastOffset;
    private boolean waitingForLoad;
    private boolean preview;
    private boolean finishedLoadingFile;
    private String finishedFilePath;
    private int loadingPriority;

    private int debugCanceledCount;
    private boolean debugReportSend;

    public AnimatedFileDrawableStream(TLRPC.Document d, ImageLocation l, Object p, int a, boolean prev, int loadingPriority, int cacheType) {
        document = d;
        location = l;
        parentObject = p;
        currentAccount = a;
        preview = prev;
        this.loadingPriority = loadingPriority;
        loadOperation = FileLoader.getInstance(currentAccount).loadStreamFile(this, document, location, parentObject, 0, preview, loadingPriority, cacheType);
    }

    public boolean isFinishedLoadingFile() {
        return finishedLoadingFile;
    }

    public String getFinishedFilePath() {
        return finishedFilePath;
    }

    public int read(int offset, int readLength) {
        synchronized (sync) {
            if (canceled) {
                debugCanceledCount++;
                if (!debugReportSend && debugCanceledCount > 200) {
                    debugReportSend = true;
                    FileLog.e(new RuntimeException("infinity stream reading!!!"));
                }
                return 0;
            }
        }
        if (readLength == 0) {
            return 0;
        } else {
            long availableLength = 0;
            try {
                while (availableLength == 0) {
                    long[] result = loadOperation.getDownloadedLengthFromOffset(offset, readLength);
                    availableLength = result[0];
                    if (!finishedLoadingFile && result[1] != 0) {
                        finishedLoadingFile = true;
                        finishedFilePath = loadOperation.getCacheFileFinal().getAbsolutePath();
                    }
                    if (availableLength == 0) {
                        synchronized (sync) {
                            if (canceled) {
                                cancelLoadingInternal();
                                return 0;
                            }
                        }
                        countDownLatch = new CountDownLatch(1);
                        if (loadOperation.isPaused() || lastOffset != offset || preview) {
                            FileLoadOperation loadOperation = FileLoader.getInstance(currentAccount).loadStreamFile(this, document, location, parentObject, offset, preview, loadingPriority);
                            if (this.loadOperation != loadOperation) {
                                this.loadOperation.removeStreamListener(this);
                                this.loadOperation = loadOperation;
                            }
                            lastOffset = offset + availableLength;
                        }
                        synchronized (sync) {
                            if (canceled) {
                                countDownLatch = null;
                                cancelLoadingInternal();
                                return 0;
                            }
                        }
                        if (!preview) {
                            FileLoader.getInstance(currentAccount).setLoadingVideo(document, false, true);
                        }
                        if (countDownLatch != null) {
                            waitingForLoad = true;
                            countDownLatch.await();
                            waitingForLoad = false;
                        }
                    }
                }
                lastOffset = offset + availableLength;
            } catch (Exception e) {
                FileLog.e(e, false);
            }
            return (int) availableLength;
        }
    }

    public void cancel() {
        cancel(true);
    }

    public void cancel(boolean removeLoading) {
        if (canceled) {
            return;
        }
        synchronized (sync) {
            if (countDownLatch != null) {
                countDownLatch.countDown();
                countDownLatch = null;
                if (removeLoading && !canceled && !preview) {
                    FileLoader.getInstance(currentAccount).removeLoadingVideo(document, false, true);
                }
            }
            if (parentObject instanceof MessageObject) {
                MessageObject messageObject = (MessageObject) parentObject;
                if (DownloadController.getInstance(messageObject.currentAccount).isDownloading(messageObject.getId()))  {
                    removeLoading = false;
                }
            }
            if (removeLoading) {
                cancelLoadingInternal();
            }
            canceled = true;
        }
    }

    private void cancelLoadingInternal() {
        FileLoader.getInstance(currentAccount).cancelLoadFile(document);
        if (location != null) {
            FileLoader.getInstance(currentAccount).cancelLoadFile(location.location, "mp4");
        }
    }

    public void reset() {
        synchronized (sync) {
            canceled = false;
        }
    }

    public TLRPC.Document getDocument() {
        return document;
    }

    public ImageLocation getLocation() {
        return location;
    }

    public Object getParentObject() {
        return document;
    }

    public boolean isPreview() {
        return preview;
    }

    public int getCurrentAccount() {
        return currentAccount;
    }

    public boolean isWaitingForLoad() {
        return waitingForLoad;
    }

    @Override
    public void newDataAvailable() {
        if (countDownLatch != null) {
            countDownLatch.countDown();
            countDownLatch = null;
        }
    }

    public boolean isCanceled() {
        return canceled;
    }
}
