package com.example.broody_video

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TransformationOptions
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter
import com.linkedin.android.litr.io.MediaRange
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.common.*
import com.otaliastudios.transcoder.internal.utils.Logger
import com.otaliastudios.transcoder.source.ClipDataSource
import com.otaliastudios.transcoder.source.TrimDataSource
import com.otaliastudios.transcoder.source.UriDataSource
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.otaliastudios.transcoder.strategy.PassThroughTrackStrategy
import com.otaliastudios.transcoder.strategy.TrackStrategy
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
    private var transcodeFuture: Future<Void>? = null


    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val context = _context;
        val channel = _channel;

        if (context == null || channel == null) {
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
                val utility = Utility(channelName)
                val sourcePath = call.argument<String>("sourcePath")!!
                val targetWidth = call.argument<Int>("targetWidth")
                val targetHeight = call.argument<Int>("targetHeight")
                val startSeconds = call.argument<Double>("startSeconds")
                val durationSeconds = call.argument<Double>("durationSeconds")

                val videoInfo = utility.getMediaInfoJson(context, sourcePath)


                val tempDir: String = context.getExternalFilesDir("broody_video")!!.absolutePath
                val out = SimpleDateFormat("yyyy-MM-dd hh-mm-ss").format(Date())
                val destPath: String =
                    tempDir + File.separator + "VID_" + out + sourcePath.hashCode() + ".mp4"

                val mediaTransformer = MediaTransformer(context)

                val requestId = UUID.randomUUID().toString()
                val sourcePathUri = Uri.parse(sourcePath)

                val strategy = if (targetWidth != null && targetHeight != null) {
                    DefaultVideoStrategy.exact(targetWidth, targetHeight).build()
                } else {
                    PassThroughTrackStrategy()
                }
                val audioStrategy =
                    DefaultAudioStrategy.builder()
                        .channels(DefaultAudioStrategy.CHANNELS_AS_INPUT)
                        .sampleRate(44100)
                        .build()


                val source = UriDataSource(context, sourcePathUri)
                val datasource = if (durationSeconds != null) {
                    val startMicroSeconds = ((startSeconds ?: 0.0) * 1_000_000)
                        .toLong()
                    ClipDataSource(
                        source, startMicroSeconds,
                        (startMicroSeconds + (durationSeconds * 1_000_000).toLong())
                    )
                } else if (startSeconds != null) {
                    TrimDataSource(
                        source, (startSeconds * 1_000_000).toLong()
                    )
                } else {
                    source
                }

                transcodeFuture = Transcoder.into(destPath)
                    .addDataSource(datasource)
                    .setAudioTrackStrategy(audioStrategy)
                    .setVideoTrackStrategy(strategy)
                    .setListener(VideoTransformationListener(channel, result, destPath, context))
                    .transcode()

            }
            else -> {
                result.notImplemented()
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
        internal const val channelName = "broody_video"

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val instance = BroodyVideoPlugin()
            instance.init(registrar.context(), registrar.messenger())
        }
    }

}
