package com.otaliastudios.transcoder.source

import android.media.MediaFormat
import com.otaliastudios.transcoder.common.TrackType
import com.otaliastudios.transcoder.internal.media.MediaFormatConstants
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A [DataSource] that provides a silent audio track of the a specific duration.
 * This class can be used to concatenate a DataSources that has a video track only with another
 * that has both video and audio track.
 */
class BroodyBlankAudioDataSource(private val durationUs: Long) : DataSource {
    private var byteBuffer: ByteBuffer? = null
    private var audioFormat: MediaFormat? = null
    private var positionUs = 0L
    private var initialized = false
    override fun initialize() {
        byteBuffer = ByteBuffer.allocateDirect(PERIOD_SIZE).order(ByteOrder.nativeOrder())
        audioFormat = MediaFormat()
        audioFormat!!.setString(MediaFormat.KEY_MIME, MediaFormatConstants.MIMETYPE_AUDIO_RAW)
        audioFormat!!.setInteger(MediaFormat.KEY_BIT_RATE, CHANNEL_COUNT * SAMPLE_RATE * 2 * 8)
        audioFormat!!.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL_COUNT)
        audioFormat!!.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, PERIOD_SIZE)
        audioFormat!!.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE)
        initialized = true
    }

    override fun selectTrack(type: TrackType) {
        // Nothing to do
    }

    override fun releaseTrack(type: TrackType) {
        // Nothing to do
    }

    override fun deinitialize() {
        positionUs = 0
        initialized = false
    }

    override fun isInitialized(): Boolean {
        return initialized
    }

    override fun getOrientation(): Int {
        return 0
    }

    override fun getLocation(): DoubleArray? {
        return null
    }

    override fun getDurationUs(): Long {
        return durationUs
    }

    override fun seekTo(desiredPositionUs: Long): Long {
        positionUs = desiredPositionUs
        return desiredPositionUs
    }

    override fun getTrackFormat(type: TrackType): MediaFormat? {
        return if (type == TrackType.AUDIO) audioFormat else null
    }

    override fun canReadTrack(type: TrackType): Boolean {
        return type == TrackType.AUDIO
    }

    override fun readTrack(chunk: DataSource.Chunk) {
        byteBuffer!!.clear()
        chunk.buffer.put(byteBuffer)
        chunk.keyframe = true
        chunk.timeUs = positionUs
        chunk.render = true

        positionUs += PERIOD_TIME_US
    }

    override fun getPositionUs(): Long {
        return positionUs
    }

    override fun isDrained(): Boolean {
        return positionUs >= getDurationUs()
    }

    companion object {
        private const val CHANNEL_COUNT = 2
        private const val SAMPLE_RATE = 44100
        private const val BITS_PER_SAMPLE = 16
        private const val BIT_RATE = CHANNEL_COUNT * SAMPLE_RATE * BITS_PER_SAMPLE
        private const val SAMPLES_PER_PERIOD = 2048.0
        private const val PERIOD_TIME_SECONDS = SAMPLES_PER_PERIOD / SAMPLE_RATE
        private const val PERIOD_TIME_US = (1000000 * PERIOD_TIME_SECONDS).toLong()
        private const val PERIOD_SIZE = (PERIOD_TIME_SECONDS * BIT_RATE / 8).toInt()
    }
}