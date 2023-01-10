package com.example.broody_video

import BlankAudioMediaSource
import android.content.Context
import android.graphics.PointF
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TrackTransform
import com.linkedin.android.litr.TransformationOptions
import com.linkedin.android.litr.codec.MediaCodecDecoder
import com.linkedin.android.litr.codec.MediaCodecEncoder
import com.linkedin.android.litr.exception.MediaTargetException
import com.linkedin.android.litr.filter.Transform
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter
import com.linkedin.android.litr.io.*
import com.linkedin.android.litr.render.AudioRenderer
import com.linkedin.android.litr.render.GlVideoRenderer
import java.io.File
import java.util.*
import kotlin.math.min

class BroodyTranscoder(private val context: Context) {
    private var _mediaTransformer: MediaTransformer = MediaTransformer(context)

    fun transcodeClip(
        sourcePath: String,
        destPath: String,
        listener: VideoTransformationListener,
        startSeconds: Double? = null,
        durationSeconds: Double? = null,
        mute: Boolean = false,
        ensureAudioTrack: Boolean = false,
        removeMetadata: Boolean = false,
        size: Pair<Int, Int>? = null,
    ) {
        val sourcePathUri = Uri.parse(sourcePath)
        val destPathUri = Uri.fromFile(File(destPath))
        val range = getMediaRange(startSeconds, durationSeconds)

        transcodeClip(
            sourcePathUri,
            destPathUri,
            listener,
            mute = mute,
            ensureAudioTrack = ensureAudioTrack,
            removeMetadata = removeMetadata,
            size = size,
            mediaRange = range,
        )
    }

    fun transcodeClip(
        sourcePathUri: Uri,
        destPathUri: Uri,
        listener: VideoTransformationListener,
        mute: Boolean = false,
        ensureAudioTrack: Boolean = false,
        removeMetadata: Boolean = false,
        size: Pair<Int, Int>? = null,
        mediaRange: MediaRange? = null,
    ) {
        val range = mediaRange ?: MediaRange(0, Long.MAX_VALUE)

        val mediaSource =
            MediaExtractorMediaSource(context, sourcePathUri, range)
        val trackFormats: MutableList<MediaFormat> = mutableListOf()
        for (track in 0 until mediaSource.trackCount) {
            val format = mediaSource.getTrackFormat(track);
            if (shouldIncludeTrack(format, mute, removeMetadata)) {
                trackFormats.add(format)
            }
        }
        val outputFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4

        // Extract source formats
        val sourceVideoFormat = trackFormats.firstOrNull {
            getMimeType(it)?.startsWith("video") ?: false
        } ?: throw MediaTargetException(
            MediaTargetException.Error.NO_OUTPUT_TRACKS,
            destPathUri.path ?: "",
            outputFormat,
            IllegalArgumentException("Clip source didn't include any video tracks")
        )
        val sourceAudioFormat = trackFormats.firstOrNull {
            getMimeType(it)?.startsWith("audio") ?: false
        }
        val addBlankAudio = sourceAudioFormat == null && ensureAudioTrack
        val mediaTarget: MediaTarget = MediaMuxerMediaTarget(
            context,
            destPathUri,
            if (addBlankAudio) trackFormats.size + 1 else trackFormats.size,
            mediaSource.orientationHint,
            outputFormat
        )
        val targetVideoFormat = buildTargetVideoFormat(sourceVideoFormat, size)
        val targetAudioFormat =
            MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                41000,
                2,
            ).apply {
                //TODO check if this value makes sense
                setInteger(MediaFormat.KEY_BIT_RATE, 256_000)
            }

        val options =
            TransformationOptions.Builder().setSourceMediaRange(range).setVideoFilters(
                listOf(
                    DefaultVideoFrameRenderFilter(
                        Transform(
                            getScale(sourceVideoFormat, size),
                            PointF(0.5f, 0.5f),
                            0f
                        )
                    )
                )
            ).build()

        val trackTransforms: MutableList<TrackTransform> = mutableListOf()
        // Build TrackTransforms for original tracks from mediaSource that we want to include
        for (trackIndex in trackFormats.indices) {
            val format = trackFormats[trackIndex]
            val mimeType = getMimeType(format)!!
            val trackTransformBuilder = TrackTransform.Builder(
                mediaSource,
                trackIndex,
                mediaTarget,
            ).setTargetTrack(trackIndex)
            if (mimeType.startsWith("video")) {
                trackTransformBuilder.setDecoder(MediaCodecDecoder())
                    .setEncoder(MediaCodecEncoder())
                    .setRenderer(GlVideoRenderer(options.videoFilters))
                    .setTargetFormat(targetVideoFormat)
            } else if (mimeType.startsWith("audio")) {
                val encoder = MediaCodecEncoder()
                trackTransformBuilder.setDecoder(MediaCodecDecoder()).setEncoder(encoder)
                    .setRenderer(AudioRenderer(encoder, options.audioFilters))
                    .setTargetFormat(targetAudioFormat)
            } else {
                trackTransformBuilder.setTargetFormat(null)
            }
            trackTransforms.add(trackTransformBuilder.build())
        }

        if (addBlankAudio) {
            trackTransforms.add(
                buildBlankAudioMediaTransform(
                    forMediaSource = mediaSource,
                    mediaTarget = mediaTarget,
                    atTrackIndex = trackTransforms.size,
                    targetAudioFormat,
                )
            )
        }
        _mediaTransformer.transform(
            UUID.randomUUID().toString(),
            trackTransforms,
            listener,
            options.granularity
        )
    }

    private fun buildTargetVideoFormat(
        sourceVideoFormat: MediaFormat,
        size: Pair<Int, Int>?,
    ): MediaFormat {
        return MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            size?.first ?: sourceVideoFormat.getInteger(MediaFormat.KEY_WIDTH),
            size?.second ?: sourceVideoFormat.getInteger(MediaFormat.KEY_HEIGHT),
        ).apply {
            //TODO do not hardcode this
            setInteger(MediaFormat.KEY_BIT_RATE, 5_000_000)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3)
            setInteger(
                MediaFormat.KEY_FRAME_RATE,
                sourceVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
            )
        }
    }

    private fun buildBlankAudioMediaTransform(
        forMediaSource: MediaSource,
        mediaTarget: MediaTarget,
        atTrackIndex: Int,
        targetFormat: MediaFormat,
    ): TrackTransform {
        val durationUs = min(
            forMediaSource.getTrackFormat(0).getLong(MediaFormat.KEY_DURATION),
            forMediaSource.selection.end - forMediaSource.selection.start,
        )
        Log.i(TAG, "Adding blank audio source with duration $durationUs")
        val trackTransformBuilder = TrackTransform.Builder(
            BlankAudioMediaSource(durationUs), 0, mediaTarget
        )
        val encoder = MediaCodecEncoder()
        trackTransformBuilder
            .setTargetTrack(atTrackIndex)
            .setTargetFormat(targetFormat)
            .setDecoder(MediaCodecDecoder())
            .setEncoder(encoder)
            .setRenderer(AudioRenderer(encoder, mutableListOf()))

        return trackTransformBuilder.build()
    }

    private fun getScale(
        mediaFormat: MediaFormat,
        targetSize: Pair<Int, Int>? = null
    ): PointF {
        if (!mediaFormat.containsKey(MediaFormat.KEY_WIDTH) || !mediaFormat.containsKey(MediaFormat.KEY_HEIGHT) || targetSize == null) {
            return PointF(1f, 1f)
        }
        val width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
        val height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val sourceAspect = width.toFloat() / height.toFloat()
        val targetAspect = targetSize.first.toFloat() / targetSize.second.toFloat()
        return if (sourceAspect > targetAspect) {
            // Source is wider than targetFrame so we scale up width
            PointF(sourceAspect / targetAspect, 1f)
        } else {
            // Source is taller than target frame so we scale up height
            PointF(1f, targetAspect / sourceAspect)
        }
    }

    private fun getMimeType(mediaFormat: MediaFormat): String? {
        if (mediaFormat.containsKey(MediaFormat.KEY_MIME)) {
            return mediaFormat.getString(MediaFormat.KEY_MIME)
        }
        return null
    }

    private fun shouldIncludeTrack(
        sourceMediaFormat: MediaFormat, removeAudio: Boolean, removeMetadata: Boolean
    ): Boolean {
        val mimeType = getMimeType(sourceMediaFormat)
        return shouldIncludeTrack(mimeType, removeAudio, removeMetadata)
    }

    private fun shouldIncludeTrack(
        mimeType: String?, removeAudio: Boolean, removeMetadata: Boolean
    ): Boolean {
        if (mimeType == null) {
            Log.e(TAG, "Mime type is null for track ")
            return false
        }
        return !(removeAudio && mimeType.startsWith("audio") || removeMetadata && !mimeType.startsWith(
            "video"
        ) && !mimeType.startsWith("audio"))
    }

    private fun getMediaRange(startSeconds: Double?, durationSeconds: Double?): MediaRange {
        val startUs = (startSeconds ?: 0.0).times(1_000_000L).toLong()
        val endUs = if (durationSeconds == null) {
            Long.MAX_VALUE
        } else {
            startUs + durationSeconds.times(1_000_000).toLong()
        }
        return MediaRange(startUs, endUs)
    }

    fun release() {
        _mediaTransformer.release()
    }

    companion object {
        private val TAG = "BroodyTranscoder"
    }
}