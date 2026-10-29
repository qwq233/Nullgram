/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.telegram.messenger.secretmedia;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.media3.common.C;
import androidx.media3.common.util.BackgroundExecutor;
import androidx.media3.common.util.NetworkTypeObserver;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.exoplayer.upstream.SlidingPercentile;

/** Combines native download samples with Media3's real transfer samples. */
public final class ExtendedBandwidthMeter implements BandwidthMeter {
    private static ExtendedBandwidthMeter instance;

    public static synchronized ExtendedBandwidthMeter getSingletonInstance(Context context) {
        if (instance == null) {
            instance = new ExtendedBandwidthMeter(context.getApplicationContext());
        }
        return instance;
    }

    private final DefaultBandwidthMeter delegate;
    private final NetworkTypeObserver network;
    private final Handler sampleHandler = new Handler(Looper.getMainLooper());
    private final NetworkTypeObserver.Listener networkListener =
            type -> sampleHandler.post(() -> onNetworkTypeChanged(type));
    private final SlidingPercentile samples = new SlidingPercentile(2000);
    private final EventListener.EventDispatcher events = new EventListener.EventDispatcher();
    private int networkType;
    private long totalElapsedMs;
    private long totalBytes;
    private long lastSampleTimeMs;
    private long estimate;
    private boolean hasEstimate;

    private ExtendedBandwidthMeter(Context context) {
        delegate = DefaultBandwidthMeter.getSingletonInstance(context);
        network = NetworkTypeObserver.getInstance(context);
        networkType = network.getNetworkType();
        lastSampleTimeMs = SystemClock.elapsedRealtime();
        delegate.addEventListener(sampleHandler, this::onMediaSample);
        // Queue the reset after the stock meter's old-network tail sample.
        network.register(networkListener, BackgroundExecutor.get());
    }

    private synchronized void onNetworkTypeChanged(int type) {
        if (networkType == type) return;
        networkType = type;
        if (type == C.NETWORK_TYPE_OFFLINE || type == C.NETWORK_TYPE_UNKNOWN || type == C.NETWORK_TYPE_OTHER) return;
        samples.reset();
        totalElapsedMs = totalBytes = 0;
        hasEstimate = false;
        lastSampleTimeMs = SystemClock.elapsedRealtime();
        events.bandwidthSample(0, 0, delegate.getBitrateEstimate());
    }

    private void addSample(long bytes, long durationMs, long elapsedMs) {
        totalElapsedMs += elapsedMs;
        totalBytes += bytes;
        samples.addSample((int) Math.sqrt(bytes), bytes * 8000f / durationMs);
        if (totalElapsedMs >= 2000 || totalBytes >= 512 * 1024) {
            estimate = (long) samples.getPercentile(0.5f);
            hasEstimate = true;
        }
        lastSampleTimeMs = SystemClock.elapsedRealtime();
        events.bandwidthSample((int) Math.min(durationMs, Integer.MAX_VALUE), bytes, getBitrateEstimate());
    }

    public synchronized void onTransfer(long bytes, long durationMs) {
        if (network.getNetworkType() == networkType && bytes > 0 && durationMs > 0) {
            addSample(bytes, durationMs, Math.max(0, SystemClock.elapsedRealtime() - lastSampleTimeMs));
        }
    }

    private synchronized void onMediaSample(int elapsedMs, long bytes, long ignoredEstimate) {
        if (elapsedMs > 0 && bytes > 0) {
            addSample(bytes, elapsedMs, elapsedMs);
        } else {
            events.bandwidthSample(elapsedMs, bytes, getBitrateEstimate());
        }
    }

    @Override
    public synchronized long getBitrateEstimate() {
        return hasEstimate && network.getNetworkType() == networkType ? estimate : delegate.getBitrateEstimate();
    }

    @Override
    public long getTimeToFirstByteEstimateUs() {
        return delegate.getTimeToFirstByteEstimateUs();
    }

    @Override
    public TransferListener getTransferListener() {
        return delegate.getTransferListener();
    }

    @Override
    public void addEventListener(Handler handler, EventListener listener) {
        events.addListener(handler, listener);
    }

    @Override
    public void removeEventListener(EventListener listener) {
        events.removeListener(listener);
    }
}
