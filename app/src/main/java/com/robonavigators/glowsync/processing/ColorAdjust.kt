package com.robonavigators.glowsync.processing

object ColorAdjust {
    fun apply(rgb:ByteArray, brightness:Float, saturation:Float, red:Float=1f, green:Float=1f, blue:Float=1f) {
        require(rgb.size%3==0 && brightness in 0f..2f && saturation in 0f..2f)
        for(i in rgb.indices step 3) {
            val r=(rgb[i].toInt() and 255).toFloat(); val g=(rgb[i+1].toInt() and 255).toFloat(); val b=(rgb[i+2].toInt() and 255).toFloat()
            val gray=.2126f*r+.7152f*g+.0722f*b
            rgb[i]=((gray+(r-gray)*saturation)*brightness*red).toInt().coerceIn(0,255).toByte()
            rgb[i+1]=((gray+(g-gray)*saturation)*brightness*green).toInt().coerceIn(0,255).toByte()
            rgb[i+2]=((gray+(b-gray)*saturation)*brightness*blue).toInt().coerceIn(0,255).toByte()
        }
    }
}
