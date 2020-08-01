package com.twenk11k.bswitch.utils

import android.content.res.Resources
import android.content.res.TypedArray
import android.util.TypedValue

class Utils {

    companion object {
        fun typedInt(
            typedArray: TypedArray?,
            index: Int,
            def: Int
        ): Int {
            return typedArray?.getInt(index, def) ?: def
        }

        fun typedPixelSize(
            typedArray: TypedArray?,
            index: Int,
            def: Int
        ): Int {
            return typedArray?.getDimensionPixelOffset(index, def) ?: def
        }

        fun typedColor(
            typedArray: TypedArray?,
            index: Int,
            def: Int
        ): Int {
            return typedArray?.getColor(index, def) ?: def
        }

        fun typedBoolean(
            typedArray: TypedArray?,
            index: Int,
            def: Boolean
        ): Boolean {
            return typedArray?.getBoolean(index, def) ?: def
        }

        fun dpToPxInt(dp: Float): Int {
            return dpToPx(dp).toInt()
        }

        fun dpToPx(dp: Float): Float {
            val r = Resources.getSystem()
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.displayMetrics)
        }

    }

}