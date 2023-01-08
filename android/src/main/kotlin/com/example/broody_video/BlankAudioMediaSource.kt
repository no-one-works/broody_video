import android.media.MediaCodec
import android.media.MediaFormat

import com.linkedin.android.litr.io.MediaSource
import java.nio.ByteBuffer

class BlankAudioMediaSource(
    private val durationUs: Long,
    private val trackFormat: MediaFormat
) : MediaSource {


    private var selectedTrack: Int = -1
    private var currentPosition = 0L
    private val periodSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
    private val sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)



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
        currentPosition = position
    }

    override fun getSampleTrackIndex(): Int {
        return selectedTrack
    }

    override fun readSampleData(buffer: ByteBuffer, offset: Int): Int {
        return periodSize
    }

    override fun getSampleTime(): Long {
        return currentPosition
    }

    override fun getSampleFlags(): Int {
        return if (currentPosition < durationUs) 0 else MediaCodec.BUFFER_FLAG_END_OF_STREAM
    }

    override fun advance() {
        currentPosition +=  1_000_000L / sampleRate
    }

    override fun release() {
    }

    override fun getSize(): Long {
        return -1
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
        const val PERIOD_SIZE = (PERIOD_TIME_SECONDS * BIT_RATE / 8).toInt()

    }

}