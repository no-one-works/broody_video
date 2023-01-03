package com.example.broody_video

import android.media.MediaFormat
import android.provider.MediaStore
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TransformationListener
import com.linkedin.android.litr.analytics.TrackTransformationInfo
import io.flutter.plugin.common.FlutterException
import io.flutter.plugin.common.MethodChannel

class VideoTransformationListener(
    private val channel: MethodChannel,
    private val result: MethodChannel.Result,
    private val mediaTransformer: MediaTransformer,
    private val resultMap: Map<String, Any>
) :
    TransformationListener {


    override fun onCancelled(
        id: String,
        trackTransformationInfos: MutableList<TrackTransformationInfo>?
    ) {

    }

    override fun onStarted(id: String) {
    }

    override fun onProgress(id: String, progress: Float) {
        channel.invokeMethod("updateProgress", progress.toString())
    }

    override fun onError(
        id: String,
        cause: Throwable?,
        trackTransformationInfos: MutableList<TrackTransformationInfo>?
    ) {
        result.error(id, cause?.localizedMessage, cause?.stackTrace)
    }

    override fun onCompleted(
        id: String,
        trackTransformationInfos: MutableList<TrackTransformationInfo>?
    ) {

//        val videoTrackTransformationInfo =
//            trackTransformationInfos?.filter { it.targetFormat?.getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_VIDEO_MPEG4 }
//                ?.firstOrNull()
//        if (videoTrackTransformationInfo == null) result.error(
//            "Aua das ging schief",
//            "wirklich",
//            "sehr schief"
//        )

        result.success(resultMap)
        mediaTransformer.release()
    }
}