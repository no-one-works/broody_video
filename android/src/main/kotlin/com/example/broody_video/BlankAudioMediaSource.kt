import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log

import com.linkedin.android.litr.io.MediaSource
import java.nio.ByteBuffer

/**
 * An AudioMediaSource that just provides empty raw audio.
 *
 * Use together with MediaCodecDecoder to add empty audio tracks
 */
class BlankAudioMediaSource(
    private val durationUs: Long,
) : MediaSource {

    private var selectedTrack: Int = -1
    private var currentPositionUs = 0L
    private var lastReadBytes = 0

    private val trackFormat = MediaFormat.createAudioFormat(
        MediaFormat.MIMETYPE_AUDIO_RAW,
        SAMPLE_RATE,
        CHANNEL_COUNT
    ).apply {
        setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
    }

    override fun getOrientationHint(): Int {
        return 0
    }

    override fun getTrackCount(): Int {
        return 1
    }

    override fun getTrackFormat(track: Int): MediaFormat {
        return trackFormat
    }

    override fun selectTrack(track: Int) {
        selectedTrack = track
    }

    override fun seekTo(position: Long, mode: Int) {
        currentPositionUs = position
    }

    override fun getSampleTrackIndex(): Int {
        return selectedTrack
    }

    override fun readSampleData(buffer: ByteBuffer, offset: Int): Int {
        Log.v(
            TAG,
            "Reading ${buffer.limit()} bytes at $currentPositionUs of $durationUs with $SAMPLE_RATE"
        )
        // We just trust the decoder here and pretend we read the full buffer
        lastReadBytes = buffer.limit()
        return if (currentPositionUs < durationUs) lastReadBytes else -1
    }

    override fun getSampleTime(): Long {
        return currentPositionUs
    }

    override fun getSampleFlags(): Int {
        return if (currentPositionUs < durationUs) 0 else MediaCodec.BUFFER_FLAG_END_OF_STREAM
    }

    override fun advance() {
        Log.v(TAG, "Advance $lastReadBytes bytes")
        currentPositionUs += bytesToUs(lastReadBytes)
    }

    override fun release() {
        // Nothing to do here
    }

    override fun getSize(): Long {
        return -1
    }

    private fun bytesToUs(
        bytes: Int
    ): Long {
        val byteRatePerChannel = SAMPLE_RATE * 2
        val byteRate = byteRatePerChannel * CHANNEL_COUNT
        return MICROSECONDS_PER_SECOND * bytes / byteRate
    }

    companion object {
        private const val TAG = "BroodyAudioMediaSource"
        private const val MICROSECONDS_PER_SECOND = 1000000L
        private const val CHANNEL_COUNT = 2
        private const val SAMPLE_RATE = 44100
        private const val BITS_PER_SAMPLE = 16
        private const val BIT_RATE = CHANNEL_COUNT * SAMPLE_RATE * BITS_PER_SAMPLE
    }
}