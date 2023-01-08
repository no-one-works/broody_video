package com.example.broody_video

import android.content.Context
import android.net.Uri
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TrackTransform
import com.linkedin.android.litr.TransformationOptions
import com.linkedin.android.litr.filter.GlFilter
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter
import com.linkedin.android.litr.io.*
import java.io.File
import java.util.*

class BroodyTranscoder(private val context: Context) {
    private var _mediaTransformer: MediaTransformer = MediaTransformer(context)

    fun transcodeClip(
        context: Context,
        sourcePath: String,
        destPath: String,
        targetSize: Pair<Int, Int>?,
        startSeconds: Double?,
        durationSeconds: Double?,
    ) {
        val mediaRange = getMediaRange(startSeconds, durationSeconds)
        val sourcePathUri = Uri.parse(sourcePath)
        val destPathUri = Uri.fromFile(File(destPath))
        val mediaSource: MediaSource =
            MediaExtractorMediaSource(context, sourcePathUri, mediaRange)
        //TODO wip
        val mediaTarget: MediaTarget = MediaMuxerMediaTarget(
            context,
            destPathUri,
            targetTrackCount,
            mediaSource.orientationHint,
            outputFormat
        )

        val trackCount = mediaSource.trackCount
        val trackTransforms: MutableList<TrackTransform> = ArrayList(trackCount)



        _mediaTransformer!!.transform(
            UUID.randomUUID().toString(),
            sourcePathUri,
            destPath,
            null,
            null,
            VideoTransformationListener(channel, result, destPath, context),
            TransformationOptions.Builder()
                .setSourceMediaRange(
                    MediaRange(
                        startSeconds?.times(1_000_000)?.toLong() ?: 0L,
                        startSeconds?.plus(durationSeconds ?: 0.0)
                            ?.times(1_000_000)?.toLong()
                            ?: Long.MAX_VALUE
                    )
                )
                .setVideoFilters(listOf(DefaultVideoFrameRenderFilter() as GlFilter))
                .build()
        )
    }

    private fun getMediaRange(startSeconds: Double?, durationSeconds: Double?):MediaRange {
        val startUs = (startSeconds ?: 0.0).times(1_000_000L).toLong()
        val endUs = if (durationSeconds == null){
            Long.MAX_VALUE
        } else {
            startUs + durationSeconds.times(1_000_000).toLong()
        }
        return MediaRange(startUs, endUs)
    }

    fun release() {
        _mediaTransformer.release()
    }
}