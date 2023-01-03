package com.example.broody_video

import android.content.Context
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TransformationOptions
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter
import com.linkedin.android.litr.io.MediaRange
import com.otaliastudios.transcoder.common.*
import com.otaliastudios.transcoder.internal.utils.Logger
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Future


/**
 * VideoCompressPlugin
 */
class BroodyVideoPlugin : MethodCallHandler, FlutterPlugin {


    private var _context: Context? = null
    private var _channel: MethodChannel? = null
    private val TAG = "VideoCompressPlugin"
    private val LOG = Logger(TAG)
    private var transcodeFuture: Future<Void>? = null
    var channelName = "broody_video"


    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val context = _context;
        val channel = _channel;

        if (context == null || channel == null) {
            Log.w(TAG, "Calling VideoCompress plugin before initialization")
            return
        }

        when (call.method) {
            "getByteThumbnail" -> {
                val path = call.argument<String>("path")
                val quality = call.argument<Int>("quality")!!
                val position = call.argument<Int>("position")!! // to long
                ThumbnailUtility(channelName).getByteThumbnail(
                    path!!,
                    quality,
                    position.toLong(),
                    result
                )
            }
            "getFileThumbnail" -> {
                val path = call.argument<String>("path")
                val quality = call.argument<Int>("quality")!!
                val position = call.argument<Int>("position")!! // to long
                ThumbnailUtility("broody_video").getFileThumbnail(
                    context, path!!, quality,
                    position.toLong(), result
                )
            }
            "getMediaInfo" -> {
                val path = call.argument<String>("path")
                result.success(Utility(channelName).getMediaInfoJson(context, path!!).toString())
            }
            "deleteAllCache" -> {
                result.success(Utility(channelName).deleteAllCache(context, result));
            }
            "setLogLevel" -> {
                val logLevel = call.argument<Int>("logLevel")!!
                Logger.setLogLevel(logLevel)
                result.success(true);
            }
            "cancelCompression" -> {
                transcodeFuture?.cancel(true)
                result.success(false);
            }
            "processClip" -> {
                val sourcePath = call.argument<String>("sourcePath")!!
                val targetWidth = call.argument<Int>("targetWidth")
                val targetHeight = call.argument<Int>("targetHeight")
                val startSeconds = call.argument<Double>("startSeconds") ?: 0.0
                val durationSeconds = call.argument<Double>("durationSeconds") ?: 0.0


                val tempDir: String = context.getExternalFilesDir("broody_video")!!.absolutePath
                val out = SimpleDateFormat("yyyy-MM-dd hh-mm-ss").format(Date())
                val destPath: String =
                    tempDir + File.separator + "VID_" + out + sourcePath.hashCode() + ".mp4"

                val mediaTransformer = MediaTransformer(context)

                val requestId = UUID.randomUUID().toString()
                val sourcePathUri = Uri.parse(sourcePath)


                val targetVideoFormat = MediaFormat().apply {
                    setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_MPEG4)
                    if (targetHeight != null) setInteger(MediaFormat.KEY_HEIGHT, targetHeight)
                    if (targetWidth != null) setInteger(MediaFormat.KEY_WIDTH, targetWidth)
                    //setInteger(MediaFormat.KEY_BIT_RATE, 2_500_500)
                    //setInteger(MediaFormat.KEY_ROTATION, 90)
                    //setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3)
//                    setInteger(
//                        MediaFormat.KEY_COLOR_FORMAT,
//                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
//                    )
                }
//                val targetAudioFormat = MediaFormat().apply {
//                    setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
//                    setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
//                    setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
//                    setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
//                }

                val frameRenderFilter = DefaultVideoFrameRenderFilter()


                val transformationOptions = TransformationOptions.Builder()
                    .setGranularity(MediaTransformer.GRANULARITY_DEFAULT)
                    .setVideoFilters(listOf(frameRenderFilter))
                    .setSourceMediaRange(
                        MediaRange(
                            (startSeconds * 1000000).toLong(),
                            ((startSeconds + durationSeconds) * 1000000).toLong()
                        )
                    )
                    .build()

                mediaTransformer.transform(
                    requestId,
                    sourcePathUri,
                    destPath,
                    targetVideoFormat,
                    null,//targetAudioFormat,
                    VideoTransformationListener(
                        channel,
                        result,
                        mediaTransformer,
                        mapOf(
                            "path" to destPath,
                            "width" to (targetWidth ?: 0),
                            "height" to (targetHeight ?: 0),
                            "duration" to durationSeconds,
                            "fileSize" to 99,
                            "author" to "BroodyVideoCutter",

                            )
                    ),
                    transformationOptions
                )


//                var videoTrackStrategy: TrackStrategy = DefaultVideoStrategy.atMost(340).build();
//                val audioTrackStrategy: TrackStrategy

//                when (quality) {
//
//                    0 -> {
//                        videoTrackStrategy = DefaultVideoStrategy.atMost(720).build()
//                    }
//
//                    1 -> {
//                        videoTrackStrategy = DefaultVideoStrategy.atMost(360).build()
//                    }
//                    2 -> {
//                        videoTrackStrategy = DefaultVideoStrategy.atMost(640).build()
//                    }
//                    3 -> {
//
//                        assert(value = frameRate != null)
//                        videoTrackStrategy = DefaultVideoStrategy.Builder()
//                            .keyFrameInterval(3f)
//                            .bitRate(1280 * 720 * 4.toLong())
//                            .frameRate(frameRate!!) // will be capped to the input frameRate
//                            .build()
//                    }
//                    4 -> {
//                        videoTrackStrategy = DefaultVideoStrategy.atMost(480, 640).build()
//                    }
//                    5 -> {
//                        videoTrackStrategy = DefaultVideoStrategy.atMost(540, 960).build()
//                    }
//                    6 -> {
//                        videoTrackStrategy = DefaultVideoStrategy.atMost(720, 1280).build()
//                    }
//                    7 -> {
//                        videoTrackStrategy = DefaultVideoStrategy.atMost(1080, 1920).build()
//                    }
//                }
//
//                audioTrackStrategy = if (includeAudio) {
//                    val sampleRate = DefaultAudioStrategy.SAMPLE_RATE_AS_INPUT
//                    val channels = DefaultAudioStrategy.CHANNELS_AS_INPUT
//
//                    DefaultAudioStrategy.builder()
//                        .channels(channels)
//                        .sampleRate(sampleRate)
//                        .build()
//                } else {
//                    RemoveTrackStrategy()
//                }
//
//                val dataSource = if (startSeconds != null || durationSeconds != null) {
//                    val source = UriDataSource(context, Uri.parse(sourcePath))
//                    TrimDataSource(
//                        source, (1000 * 1000 * (startSeconds
//                            ?: 0)).toLong(), (1000 * 1000 * (durationSeconds ?: 0)).toLong()
//                    )
//                } else {
//                    UriDataSource(context, Uri.parse(sourcePath))
//                }
//
//
//                transcodeFuture = Transcoder.into(destPath!!)
//                    .addDataSource(dataSource)
//                    .setAudioTrackStrategy(audioTrackStrategy)
//                    .setVideoTrackStrategy(videoTrackStrategy)
//                    .setListener(object : TranscoderListener {
//                        override fun onTranscodeProgress(progress: Double) {
//                            channel.invokeMethod("updateProgress", progress * 100.00)
//                        }
//
//                        override fun onTranscodeCompleted(successCode: Int) {
//                            channel.invokeMethod("updateProgress", 100.00)
//                            val json = Utility(channelName).getMediaInfoJson(context, destPath)
//                            json.put("isCancel", false)
//                            result.success(json.toString())
//                            if (deleteOrigin) {
//                                File(sourcePath).delete()
//                            }
//                        }
//
//                        override fun onTranscodeCanceled() {
//                            result.success(null)
//                        }
//
//                        override fun onTranscodeFailed(exception: Throwable) {
//                            result.success(null)
//                        }
//                    }).transcode()
//            }
//            else -> {
//                result.notImplemented()
            }
        }
    }


    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        init(binding.applicationContext, binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        _channel?.setMethodCallHandler(null)
        _context = null
        _channel = null
    }

    private fun init(context: Context, messenger: BinaryMessenger) {
        val channel = MethodChannel(messenger, channelName)
        channel.setMethodCallHandler(this)
        _context = context
        _channel = channel
    }

    companion object {
        private const val TAG = "broody_video"

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val instance = BroodyVideoPlugin()
            instance.init(registrar.context(), registrar.messenger())
        }
    }

}
