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

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.Clock;
import androidx.media3.exoplayer.source.chunk.MediaChunk;
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.exoplayer.upstream.BandwidthMeter;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Retains Telegram's cached-video preference while Media3 manages adaptation and buffering. */
public final class CachedTrackSelection extends AdaptiveTrackSelection {
    public static final class Factory extends AdaptiveTrackSelection.Factory {
        private final Supplier<Set<String>> cachedVariants;

        public Factory(Supplier<Set<String>> cachedVariants) {
            this.cachedVariants = cachedVariants;
        }

        @Override
        protected AdaptiveTrackSelection createAdaptiveTrackSelection(TrackGroup group, int[] tracks, int type,
                BandwidthMeter meter, ImmutableList<AdaptationCheckpoint> checkpoints) {
            if (group.type != C.TRACK_TYPE_VIDEO) {
                return super.createAdaptiveTrackSelection(group, tracks, type, meter, checkpoints);
            }
            return new CachedTrackSelection(group, tracks, type, meter, checkpoints, cachedVariants.get());
        }
    }

    private final Set<String> cachedVariants;
    private boolean initialSelection;
    private long selectionTimeMs;
    private Format idealFormat;

    private CachedTrackSelection(TrackGroup group, int[] tracks, int type, BandwidthMeter meter,
            List<AdaptationCheckpoint> checkpoints, Set<String> cachedVariants) {
        super(group, tracks, type, meter, DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
                DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS, DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
                DEFAULT_MAX_WIDTH_TO_DISCARD, DEFAULT_MAX_HEIGHT_TO_DISCARD, DEFAULT_BANDWIDTH_FRACTION,
                DEFAULT_BUFFERED_FRACTION_TO_LIVE_EDGE_FOR_QUALITY_INCREASE, checkpoints, Clock.DEFAULT);
        this.cachedVariants = cachedVariants;
    }

    @Override
    public void updateSelectedTrack(long positionUs, long bufferedUs, long availableUs,
            List<? extends MediaChunk> queue, MediaChunkIterator[] iterators) {
        initialSelection = getSelectionReason() == C.SELECTION_REASON_UNKNOWN;
        selectionTimeMs = Clock.DEFAULT.elapsedRealtime();
        idealFormat = null;
        try {
            super.updateSelectedTrack(positionUs, bufferedUs, availableUs, queue, iterators);
        } finally {
            initialSelection = false;
        }
    }

    @Override
    public int evaluateQueueSize(long positionUs, List<? extends MediaChunk> queue) {
        selectionTimeMs = Clock.DEFAULT.elapsedRealtime();
        idealFormat = null;
        return super.evaluateQueueSize(positionUs, queue);
    }

    @Override
    protected boolean canSelectFormat(Format format, int bitrate, long effectiveBitrate) {
        if (idealFormat == null) {
            HashMap<Integer, Integer> resolutions = new HashMap<>();
            ArrayList<Integer> candidates = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                if (isTrackExcluded(i, selectionTimeMs)) continue;
                Format candidate = getFormat(i);
                int resolution = Math.max(candidate.width, candidate.height);
                Integer previousIndex = resolutions.get(resolution);
                if (previousIndex != null) {
                    Format previous = getFormat(previousIndex);
                    boolean previousCached = cachedVariants.contains(previous.id);
                    boolean candidateCached = cachedVariants.contains(candidate.id);
                    if (previousCached && !candidateCached) continue;
                    if (previousCached == candidateCached && candidate.bitrate >= previous.bitrate) continue;
                    candidates.remove(previousIndex);
                }
                resolutions.put(resolution, i);
                candidates.add(i);
            }
            if (initialSelection) {
                for (int index : candidates) {
                    if (cachedVariants.contains(getFormat(index).id)) {
                        idealFormat = getFormat(index);
                        break;
                    }
                }
            }
            if (idealFormat == null) {
                for (int index : candidates) {
                    idealFormat = getFormat(index);
                    if (cachedVariants.contains(idealFormat.id) || idealFormat.bitrate <= effectiveBitrate) break;
                }
            }
        }
        return format == idealFormat;
    }
}
