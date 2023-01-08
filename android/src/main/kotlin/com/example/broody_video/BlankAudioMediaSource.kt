package com.example.broody_video

import android.media.MediaExtractor
import android.media.MediaFormat
import com.linkedin.android.litr.io.MediaSource
import com.otaliastudios.transcoder.internal.media.MediaFormatConstants
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil

class BlankAudioMediaSource(private val durationUs: Long) : MediaSource {
    private var positionUs = 0L
    private val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(PERIOD_SIZE).order(
        ByteOrder.nativeOrder()
    )

    override fun getOrientationHint(): Int {
        return 0
    }

    override fun getTrackCount(): Int {
        return 1
    }

    override fun getTrackFormat(track: Int): MediaFormat {
        val audioFormat = MediaFormat()
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormatConstants.MIMETYPE_AUDIO_AAC)
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, CHANNEL_COUNT * SAMPLE_RATE * 2 * 8)
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL_COUNT)
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, PERIOD_SIZE)
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE)
        return audioFormat
    }

    override fun selectTrack(track: Int) {
        return
    }

    override fun seekTo(position: Long, mode: Int) {
        positionUs = position
    }

    override fun getSampleTrackIndex(): Int {
        return 0
    }

    override fun readSampleData(buffer: ByteBuffer, offset: Int): Int {
        if (positionUs >= durationUs) {
            return -1
        }
        val position = buffer.position()
        byteBuffer.clear()
        byteBuffer.limit(PERIOD_SIZE)
        buffer.put(byteBuffer)
        buffer.position(position)
        buffer.limit(position + PERIOD_SIZE)
        return PERIOD_SIZE
    }

    override fun getSampleTime(): Long {
        return positionUs
    }

    override fun getSampleFlags(): Int {
        return MediaExtractor.SAMPLE_FLAG_SYNC
    }

    override fun advance() {
        positionUs += bytesToUs(
            PERIOD_SIZE,
            SAMPLE_RATE,
            CHANNEL_COUNT
        )

    }

    override fun release() {
        // Nothing to do here
    }

    override fun getSize(): Long {
        return usToBytes(durationUs, SAMPLE_RATE, CHANNEL_COUNT)
    }

    companion object {
        private const val BYTES_PER_SAMPLE_PER_CHANNEL = 2 // Assuming 16bit audio, so 2
        private const val MICROSECONDS_PER_SECOND = 1000000L
        private const val CHANNEL_COUNT = 2
        private const val SAMPLE_RATE = 44100
        private const val BITS_PER_SAMPLE = 16
        private const val BIT_RATE = CHANNEL_COUNT * SAMPLE_RATE * BITS_PER_SAMPLE
        private const val SAMPLES_PER_PERIOD = 2048.0
        private const val PERIOD_TIME_SECONDS = SAMPLES_PER_PERIOD / SAMPLE_RATE
        private const val PERIOD_TIME_US = (MICROSECONDS_PER_SECOND * PERIOD_TIME_SECONDS).toLong()
        private const val PERIOD_SIZE = (PERIOD_TIME_SECONDS * BIT_RATE / 8).toInt()


        private fun bytesToUs(
            bytes: Int /* bytes */,
            sampleRate: Int /* samples/sec */,
            channels: Int /* channel */
        ): Long {
            val byteRatePerChannel = sampleRate * 2 // bytes/sec/channel
            val byteRate = byteRatePerChannel * channels // bytes/sec
            return 1_000_000L * bytes / byteRate // usec
        }

        private fun usToBytes(
            bytes: Int /* bytes */,
            sampleRate: Int /* samples/sec */,
            channels: Int /* channel */
        ): Long {
            val byteRatePerChannel = sampleRate * BYTES_PER_SAMPLE_PER_CHANNEL
            val byteRate = byteRatePerChannel * channels // bytes/sec
            return MICROSECONDS_PER_SECOND * bytes / byteRate // usec
        }

        internal fun usToBytes(us: Long, sampleRate: Int, channels: Int): Long {
            val byteRatePerChannel = sampleRate * 2 //bytes per sample per channel
            val byteRate = byteRatePerChannel * channels
            return ceil(us.toDouble() * byteRate / MICROSECONDS_PER_SECOND).toLong()
        }
    }
}