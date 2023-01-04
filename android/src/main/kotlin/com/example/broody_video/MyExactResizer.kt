package com.example.broody_video

import com.otaliastudios.transcoder.common.ExactSize
import com.otaliastudios.transcoder.common.Size
import com.otaliastudios.transcoder.resize.Resizer

class MyExactResizer(private val width: Int, private val height: Int): Resizer {
    override fun getOutputSize(inputSize: Size): Size {
        return ExactSize(width, height)
    }
}