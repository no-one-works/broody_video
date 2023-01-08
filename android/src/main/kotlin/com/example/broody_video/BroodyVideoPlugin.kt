package com.example.broody_video

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.common.*
import com.otaliastudios.transcoder.source.*
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.otaliastudios.transcoder.strategy.PassThroughTrackStrategy
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Future


class BroodyVideoPlugin : MethodCallHandler, FlutterPlugin {
    private var _context: Context? = null
    private var _channel: MethodChannel? = null
    private var transcodeFuture: Future<Void>? = null


    @SuppressLint("SimpleDateFormat")
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val context = _context;
        val channel = _channel;

        if (context == null || channel == null) {
            return
        }

        when (call.method) {
            "processClip" -> {
                val sourcePath = call.argument<String>("sourcePath")!!
                val targetWidth = call.argument<Int>("targetWidth")
                val targetHeight = call.argument<Int>("targetHeight")
                val startSeconds = call.argument<Double>("startSeconds")
                val durationSeconds = call.argument<Double>("durationSeconds")

                val tempDir: String = context.getExternalFilesDir("broody_video")!!.absolutePath
                val out = SimpleDateFormat("yyyy-MM-dd hh-mm-ss").format(Date())
                val destPath: String =
                    tempDir + File.separator + "VID_" + out + sourcePath.hashCode() + ".mp4"

                val sourcePathUri = Uri.parse(sourcePath)
                val strategy = if (targetWidth != null && targetHeight != null) {
                    DefaultVideoStrategy.Builder()
                        .addResizer(MyExactResizer(targetWidth, targetHeight))
                        .build()
                } else {
                    PassThroughTrackStrategy()
                }
                val audioStrategy = DefaultAudioStrategy.builder().build()
                val source = UriDataSource(context, sourcePathUri)
                val videoDataSource = if (durationSeconds != null) {
                    val startMicroSeconds = ((startSeconds ?: 0.0) * 1_000_000)
                        .toLong()
                    val durationMicroSeconds = durationSeconds.times(1_000_000).toLong()
                    TrimDataSource(
                        source, startMicroSeconds,
                        startMicroSeconds + durationMicroSeconds
                    )
                } else if (startSeconds != null) {
                    TrimDataSource(
                        source, (startSeconds * 1_000_000).toLong()
                    )
                } else {
                    source
                }

                val audioDataSource = if (videoDataSource.getTrackFormat(TrackType.AUDIO) == null) {
                    videoDataSource.initialize()
                    BlankAudioDataSource(videoDataSource.durationUs)
                } else {
                    videoDataSource
                }

                transcodeFuture = Transcoder.into(destPath)
                    .addDataSource(TrackType.VIDEO, videoDataSource)
                    .addDataSource(TrackType.AUDIO, audioDataSource)
                    .setVideoTrackStrategy(strategy)
                    .setAudioTrackStrategy(audioStrategy)
                    .setListener(VideoTransformationListener(channel, result, destPath, context))
                    .transcode()
            }
            "concatVideos" -> {
                val srcPaths = call.argument<List<String>>("sourcePaths")!!
                val destPath = call.argument<String>("destinationPath")!!
                val transcoder = Transcoder.into(destPath)
                for (source in srcPaths) {
                    val inputStream = FileInputStream(source)
                    val datasource = FileDescriptorDataSource(inputStream.fd)
                    transcoder.addDataSource(datasource)
                }
                //FIXME this will fail if videos with audio are mixed with videos without audio. We were so close
                transcodeFuture = transcoder.setListener(
                    VideoTransformationListener(
                        channel,
                        result,
                        destPath,
                        context
                    )
                ).transcode()
            }
            "getThumbnail" -> {
                val path = call.argument<String>("sourcePath")
                val positionSeconds = call.argument<Double>("positionSeconds")!! // to long
                val quality = call.argument<Int>("quality")!!
                ThumbnailUtility(channelName).getByteThumbnail(
                    path!!,
                    quality,
                    positionSeconds,
                    result
                )
            }
            "clearCache" -> {
                result.success(Utility(channelName).deleteAllCache(context, result));
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
