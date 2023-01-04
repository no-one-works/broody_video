package com.example.broody_video

import android.content.Context
import android.media.MediaFormat
import android.provider.MediaStore
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TransformationListener
import com.linkedin.android.litr.analytics.TrackTransformationInfo
import com.otaliastudios.transcoder.TranscoderListener
import io.flutter.plugin.common.FlutterException
import io.flutter.plugin.common.MethodChannel

class VideoTransformationListener(
    private val channel: MethodChannel,
    private val result: MethodChannel.Result,
    private val destPath: String,
    private val context: Context

) :
    TranscoderListener {

    override fun onTranscodeProgress(progress: Double) {
        channel.invokeMethod("updateProgress", progress.toString())
    }

    override fun onTranscodeCompleted(successCode: Int) {
        channel.invokeMethod("updateProgress", 100.00)
        val resultMap = Utility(BroodyVideoPlugin.channelName).getMediaInfoJson(context, destPath)
        result.success(resultMap)

    }

    override fun onTranscodeCanceled() {
        result.success(null)
    }

    override fun onTranscodeFailed(exception: Throwable) {
        result.error(
            "transcode failed",
            exception.localizedMessage,
            exception.stackTrace.toString()
        )
    }
}