package com.twenk11k.bswitch


import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Checkable
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_DRAGING
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_NONE
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_PENDING_DRAG
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_PENDING_RESET
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_PENDING_SETTLE
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_SWITCH
import com.twenk11k.bswitch.utils.Utils.Companion.dpToPxInt
import com.twenk11k.bswitch.utils.Utils.Companion.typedBoolean
import com.twenk11k.bswitch.utils.Utils.Companion.typedColor
import com.twenk11k.bswitch.utils.Utils.Companion.typedInt
import com.twenk11k.bswitch.utils.Utils.Companion.typedPixelSize


class BSwitch : View, Checkable {

    private val widthDefault = dpToPxInt(66f)
    private val heightDefault: Int = dpToPxInt(34f)
    private val durationDefault = 400
    private val borderWidthDefault = 2f
    private val arcStrokeWidthDefault = 2f
    private val arcSweepAngleDefault = 360
    private val arcPaddingDefault = 18
    private val drawCircleEnabledDefault = false

    private var borderWidth = dpToPxInt(borderWidthDefault)
    private var arcStrokeWidth = dpToPxInt(arcStrokeWidthDefault)
    private var arcSweepAngle = arcSweepAngleDefault
    private var arcPadding = arcPaddingDefault
    private var drawCircleEnabled = drawCircleEnabledDefault

    private var isChecked = false

    private var uncheckedColor = 0
    private var checkedColor = 0

    private var paint: Paint? = null
    private var circlePaint: Paint? = null

    private var valueAnimator: ValueAnimator? = null

    private var viewState: ViewState? = null
    private var beforeState: ViewState? = null
    private var afterState: ViewState? = null
    private var animateState = ANIMATE_STATE_NONE

    private var buttonMinX = 0f
    private var buttonMaxX = 0f
    private var viewRadius = 0f
    private var circleCornerRadius = 0f

    private var isEventBroadcast = false

    private val argbEvaluator = ArgbEvaluator()

    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    private var height = 0f
    private var width = 0f
    private var left = 0f
    private var top = 0f
    private var right = 0f
    private var bottom = 0f
    private var centerX = 0f
    private var centerY = 0f

    private var rect = RectF()
    private var arcRect = RectF()

    private var isSizeChanged = false
    private var isTouchingDown = false

    private var touchDownTime: Long = 0

    private val postPendingDrag = Runnable {
        if (!isInAnimating()) {
            pendingStateDrag()
        }
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {

        var typedArray: TypedArray? = null
        if (attrs != null) {
            typedArray = context.obtainStyledAttributes(attrs, R.styleable.BSwitch)
        }

        uncheckedColor = typedColor(
            typedArray,
            R.styleable.BSwitch_unchecked_color,
            ContextCompat.getColor(context, R.color.uncheckedColor)
        )

        checkedColor = typedColor(
            typedArray,
            R.styleable.BSwitch_checked_color,
            ContextCompat.getColor(context, R.color.checkedColor)
        )

        borderWidth = typedPixelSize(
            typedArray,
            R.styleable.BSwitch_border_width,
            dpToPxInt(borderWidthDefault)
        )

        arcStrokeWidth = typedPixelSize(
            typedArray,
            R.styleable.BSwitch_arc_stroke_width,
            dpToPxInt(arcStrokeWidthDefault)
        )

        arcSweepAngle = typedInt(
            typedArray,
            R.styleable.BSwitch_arc_sweep_angle,
            360
        )

        arcPadding = typedInt(
            typedArray,
            R.styleable.BSwitch_arc_padding,
            arcPaddingDefault
        )

        drawCircleEnabled = typedBoolean(
            typedArray,
            R.styleable.BSwitch_draw_circle_enabled,
            drawCircleEnabledDefault
        )

        val duration: Int = typedInt(
            typedArray,
            R.styleable.BSwitch_duration,
            durationDefault
        )

        isChecked = typedBoolean(
            typedArray,
            R.styleable.BSwitch_is_checked,
            false
        )

        typedArray?.recycle()

        initPaint()
        initCirclePaint()

        viewState = ViewState()
        beforeState = ViewState()
        afterState = ViewState()

        valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator?.duration = duration.toLong()
        valueAnimator?.repeatCount = 0

        valueAnimator?.addUpdateListener(animatorUpdateListener)
        valueAnimator?.addListener(animatorListener)

        super.setClickable(true)

        setPadding(0, 0, 0, 0)

        setLayerType(LAYER_TYPE_SOFTWARE, null)

    }

    private fun initPaint() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint?.style = Paint.Style.FILL_AND_STROKE
        paint?.strokeWidth = borderWidth.toFloat()
    }

    private fun initCirclePaint() {
        circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        circlePaint?.style = Paint.Style.STROKE
        circlePaint?.strokeWidth = arcStrokeWidth.toFloat()
        circlePaint?.color = Color.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint?.color = viewState!!.checkStateColor

        drawRoundRect(
            canvas,
            left, top, right, bottom,
            viewRadius, paint!!
        )

        if (drawCircleEnabled)
            canvas.drawCircle(viewState!!.buttonX, centerY, circleCornerRadius, circlePaint!!)
        else
            canvas.drawArc(arcRect, 0f, arcSweepAngle.toFloat(), true, circlePaint!!)

    }

    private fun drawRoundRect(
        canvas: Canvas,
        left: Float, top: Float,
        right: Float, bottom: Float,
        viewRadius: Float,
        paint: Paint
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(
                left, top, right, bottom,
                viewRadius, viewRadius, paint
            )
        } else {
            rect[left, top, right] = bottom
            canvas.drawRoundRect(
                rect,
                viewRadius, viewRadius, paint
            )
        }
    }

    private fun broadcastEvent() {
        if (onCheckedChangeListener != null) {
            isEventBroadcast = true
            onCheckedChangeListener?.onCheckedChanged(this@BSwitch, !isChecked())
        }
        isEventBroadcast = false
    }

    override fun isChecked(): Boolean {
        return isChecked
    }

    override fun toggle() {
        toggle(true, false)
    }

    private fun toggle(broadcast: Boolean, isReversed: Boolean) {
        if (!isEnabled) {
            return
        }
        if (!isSizeChanged) {
            isChecked = !isChecked
            if (broadcast)
                broadcastEvent()
            return
        }
        if (valueAnimator!!.isRunning)
            valueAnimator?.cancel()

        animateState = ANIMATE_STATE_SWITCH
        beforeState?.update(viewState!!)
        if (isChecked()) {
            setCheckedViewState(afterState, isReversed)
        } else {
            setUnCheckedViewState(afterState, isReversed)
        }
        valueAnimator?.start()
    }

    override fun setChecked(checked: Boolean) {
        if (checked == isChecked()) {
            postInvalidate()
            return
        }
        toggle(false, false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val viewPadding = borderWidth.toFloat()
        height = h - viewPadding - viewPadding
        width = w - viewPadding - viewPadding
        viewRadius = height * .5f
        circleCornerRadius = viewRadius - borderWidth - arcPadding
        left = viewPadding
        top = viewPadding
        right = w - viewPadding
        bottom = h - viewPadding
        centerX = (left + right) * .5f
        centerY = (top + bottom) * .5f
        buttonMinX = left + viewRadius
        buttonMaxX = right - viewRadius
        arcRect.top = top + arcPadding
        arcRect.bottom = bottom - arcPadding

        isChecked = true
        if (isChecked()) {
            arcRect.right = getArcRightEnd()
            arcRect.left = arcRect.right - 1f
            setUnCheckedViewState(viewState, true)
        } else {
            arcRect.left = getArcLeftEnd()
            arcRect.right = arcRect.left + arcRect.height()
            setCheckedViewState(viewState, true)
        }

        isSizeChanged = true
        postInvalidate()
    }

    private fun setUnCheckedViewState(viewState: ViewState?, isReversed: Boolean) {
        viewState?.radius = viewRadius
        if (isReversed) {
            viewState?.checkStateColor = uncheckedColor
            viewState?.buttonX = buttonMaxX
        } else {
            viewState?.checkStateColor = checkedColor
            viewState?.buttonX = buttonMinX
        }
    }

    private fun setCheckedViewState(viewState: ViewState?, isReversed: Boolean) {
        viewState?.radius = 1f
        if (isReversed) {
            viewState?.checkStateColor = checkedColor
            viewState?.buttonX = buttonMinX
        } else {
            viewState?.checkStateColor = uncheckedColor
            viewState?.buttonX = buttonMaxX
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec1 = widthMeasureSpec
        var heightMeasureSpec1 = heightMeasureSpec
        val widthMode = MeasureSpec.getMode(widthMeasureSpec1)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec1)

        if (widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST)
            widthMeasureSpec1 = MeasureSpec.makeMeasureSpec(widthDefault, MeasureSpec.EXACTLY)

        if (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST)
            heightMeasureSpec1 = MeasureSpec.makeMeasureSpec(heightDefault, MeasureSpec.EXACTLY)

        super.onMeasure(widthMeasureSpec1, heightMeasureSpec1)
    }

    private val animatorUpdateListener: ValueAnimator.AnimatorUpdateListener = object :
        ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val value = animation.animatedValue as Float

            when (animateState) {
                ANIMATE_STATE_SWITCH -> {

                    viewState?.buttonX = (beforeState!!.buttonX
                            + (afterState!!.buttonX - beforeState!!.buttonX) * value)

                    val fraction = (viewState!!.buttonX - buttonMinX) / (buttonMaxX - buttonMinX)

                    viewState?.checkStateColor = argbEvaluator.evaluate(
                        1 - fraction,
                        uncheckedColor,
                        checkedColor
                    ) as Int

                    viewState?.radius = fraction * viewRadius

                    val buttonX = (buttonMinX
                            + (buttonMaxX - buttonMinX + arcRect.height())
                            * fraction)

                    val arcLeft = getClampValue(
                        buttonX-arcPadding,
                        getArcLeftEnd(),
                        getArcRightEnd()
                    )

                    arcRect.left = arcLeft
                    arcRect.right = arcLeft + measureArcWidth(arcLeft)

                }
                ANIMATE_STATE_PENDING_SETTLE -> {
                    run {}
                    run {}
                    run {
                        viewState?.radius = (beforeState!!.radius
                                + (afterState!!.radius - beforeState!!.radius) * value)
                        if (animateState != ANIMATE_STATE_PENDING_DRAG) {
                            viewState?.buttonX = (beforeState!!.buttonX
                                    + (afterState!!.buttonX - beforeState!!.buttonX) * value)

                            val fraction =
                                (viewState!!.buttonX - buttonMinX) / (buttonMaxX - buttonMinX)

                            val buttonX = (buttonMinX
                                    + (buttonMaxX - buttonMinX + arcRect.height())
                                    * fraction)

                            val arcLeft = getClampValue(
                                buttonX-arcPadding,
                                getArcLeftEnd(),
                                getArcRightEnd()
                            )

                            arcRect.left = arcLeft
                            arcRect.right = arcLeft + measureArcWidth(arcLeft)

                        }
                        viewState?.checkStateColor = argbEvaluator.evaluate(
                            value,
                            beforeState?.checkStateColor,
                            afterState?.checkStateColor
                        ) as Int
                    }
                }
                ANIMATE_STATE_PENDING_DRAG -> {
                    viewState?.radius = (beforeState!!.radius
                            + (afterState!!.radius - beforeState!!.radius) * value)
                    if (animateState != ANIMATE_STATE_PENDING_DRAG) {
                        viewState?.buttonX = (beforeState!!.buttonX
                                + (afterState!!.buttonX - beforeState!!.buttonX) * value)

                        val fraction =
                            (viewState!!.buttonX - buttonMinX) / (buttonMaxX - buttonMinX)

                        val buttonX = (buttonMinX
                                + (buttonMaxX - buttonMinX + arcRect.height())
                                * fraction)

                        val arcLeft = getClampValue(
                            buttonX-arcPadding,
                            getArcLeftEnd(),
                            getArcRightEnd()
                        )

                        arcRect.left = arcLeft
                        arcRect.right = arcLeft + measureArcWidth(arcLeft)

                    }
                    viewState?.checkStateColor = argbEvaluator.evaluate(
                        value,
                        beforeState?.checkStateColor,
                        afterState?.checkStateColor
                    ) as Int

                }
                ANIMATE_STATE_PENDING_RESET -> {
                    run {}
                    run {
                        viewState?.radius = (beforeState!!.radius
                                + (afterState!!.radius - beforeState!!.radius) * value)
                        if (animateState != ANIMATE_STATE_PENDING_DRAG) {
                            viewState?.buttonX = (beforeState!!.buttonX
                                    + (afterState!!.buttonX - beforeState!!.buttonX) * value)

                            val fraction =
                                (viewState!!.buttonX - buttonMinX) / (buttonMaxX - buttonMinX)

                            val buttonX = (buttonMinX
                                    + (buttonMaxX - buttonMinX + arcRect.height())
                                    * fraction)

                            val arcLeft = getClampValue(
                                buttonX-arcPadding,
                                getArcLeftEnd(),
                                getArcRightEnd()
                            )

                            arcRect.left = arcLeft
                            arcRect.right = arcLeft + measureArcWidth(arcLeft)

                        }
                        viewState?.checkStateColor = argbEvaluator.evaluate(
                            value,
                            beforeState?.checkStateColor,
                            afterState?.checkStateColor
                        ) as Int
                    }
                }
                else -> {
                    run {}
                    run {}
                }
            }
            postInvalidate()
        }
    }

    private val animatorListener: Animator.AnimatorListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {}
        override fun onAnimationEnd(animation: Animator) {
            when (animateState) {
                ANIMATE_STATE_DRAGING -> {
                }
                ANIMATE_STATE_PENDING_DRAG -> {
                    animateState = ANIMATE_STATE_DRAGING
                    viewState?.radius = viewRadius
                    postInvalidate()
                }
                ANIMATE_STATE_PENDING_RESET -> {
                    animateState = ANIMATE_STATE_NONE
                    postInvalidate()
                }
                ANIMATE_STATE_PENDING_SETTLE -> {
                    animateState = ANIMATE_STATE_NONE
                    postInvalidate()
                    broadcastEvent()
                }
                ANIMATE_STATE_SWITCH -> {
                    isChecked = !isChecked
                    animateState = ANIMATE_STATE_NONE
                    postInvalidate()
                    broadcastEvent()
                }
                ANIMATE_STATE_NONE -> {
                }
                else -> {
                }
            }
        }

        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
    }

    override fun setOnClickListener(l: OnClickListener?) {}

    override fun setOnLongClickListener(l: OnLongClickListener?) {}

    fun setOnCheckedChangeListener(onCheckedChangeListener: OnCheckedChangeListener) {
        this.onCheckedChangeListener = onCheckedChangeListener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        val actionMasked = event.actionMasked
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isTouchingDown = true
                touchDownTime = System.currentTimeMillis()
                removeCallbacks(postPendingDrag)
                postDelayed(postPendingDrag, 100)
            }
            MotionEvent.ACTION_MOVE -> {
                val eventX = event.x
                if (isPendingStateDrag()) {
                    var fraction = eventX / getWidth()
                    fraction = maxOf(0f, minOf(1f, fraction))
                    var arcHeight = 0f
                    if (!drawCircleEnabled)
                        arcHeight = arcRect.height()

                    val buttonX = (buttonMinX
                            + (buttonMaxX - buttonMinX + arcHeight)
                            * fraction)

                    viewState?.buttonX = buttonX

                    val arcLeft = getClampValue(
                        buttonX-arcPadding,
                        getArcLeftEnd(),
                        getArcRightEnd()
                    )

                    arcRect.left = arcLeft
                    arcRect.right = arcLeft + measureArcWidth(arcLeft)

                } else if (isStateDrag()) {
                    var fraction = eventX / getWidth()

                    fraction = maxOf(0f, minOf(1f, fraction))

                    var arcHeight = 0f
                    if (!drawCircleEnabled)
                        arcHeight = arcRect.height()

                    val buttonX = (buttonMinX
                            + (buttonMaxX - buttonMinX + arcHeight)
                            * fraction)

                    viewState?.buttonX = buttonX
                    viewState?.checkStateColor = argbEvaluator.evaluate(
                        1 - fraction,
                        uncheckedColor,
                        checkedColor
                    ) as Int

                    val arcLeft = getClampValue(
                        buttonX-arcPadding,
                        getArcLeftEnd(),
                        getArcRightEnd()
                    )

                    arcRect.left = arcLeft
                    arcRect.right = arcLeft + measureArcWidth(arcLeft)

                    postInvalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                isTouchingDown = false
                removeCallbacks(postPendingDrag)
                if (System.currentTimeMillis() - touchDownTime <= 300) {
                    toggle(true, true)
                } else if (isStateDrag()) {
                    val eventX = event.x
                    var fraction = eventX / getWidth()
                    fraction = maxOf(0f, minOf(1f, fraction))
                    val newCheck = fraction > .5f
                    if (newCheck == isChecked()) {
                        pendingStateCancelDrag()
                    } else {
                        isChecked = newCheck
                        pendingStateSettle()
                    }
                } else if (isPendingStateDrag()) {
                    pendingStateCancelDrag()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isTouchingDown = false
                removeCallbacks(postPendingDrag)
                if (isPendingStateDrag() || isStateDrag()) {
                    pendingStateCancelDrag()
                }
            }
        }
        return true
    }

    private fun getArcLeftEnd(): Float {
        return left + arcPadding
    }

    private fun getArcRightEnd(): Float {
        return right - arcPadding - arcRect.height() / 2
    }

    private fun measureArcWidth(arcLeft: Float): Float {
        val leftEnd = getArcLeftEnd()
        val rightEnd = getArcRightEnd()
        val tilt = (leftEnd - rightEnd) / (arcRect.height() - 1f)
        val value = (arcLeft - (-1 * tilt + rightEnd)) / tilt
        return getClampValue(value, 1f, arcRect.height())
    }

    private fun getClampValue(value: Float, min: Float, max: Float): Float {
        return MathUtils.clamp(value, min, max)
    }

    private fun pendingStateCancelDrag() {
        if (isStateDrag() || isPendingStateDrag()) {
            if (valueAnimator!!.isRunning) {
                valueAnimator?.cancel()
            }
            animateState = ANIMATE_STATE_PENDING_RESET
            beforeState?.update(viewState!!)
            if (isChecked()) {
                setUnCheckedViewState(afterState, true)
            } else {
                setCheckedViewState(afterState, true)
            }
            valueAnimator?.start()
        }
    }

    private fun isInAnimating(): Boolean {
        return animateState != ANIMATE_STATE_NONE
    }

    private fun isStateDrag(): Boolean {
        return animateState == ANIMATE_STATE_DRAGING
    }

    private fun isPendingStateDrag(): Boolean {
        return (animateState == ANIMATE_STATE_PENDING_DRAG
                || animateState == ANIMATE_STATE_PENDING_RESET)
    }

    private fun pendingStateDrag() {
        if (isInAnimating()) {
            return
        }
        if (!isTouchingDown) {
            return
        }
        if (valueAnimator!!.isRunning) {
            valueAnimator?.cancel()
        }
        animateState = ANIMATE_STATE_PENDING_DRAG
        beforeState?.update(viewState!!)
        afterState?.update(viewState!!)
        if (isChecked()) {
            afterState?.checkStateColor = uncheckedColor
            afterState?.buttonX = buttonMinX
        } else {
            afterState?.checkStateColor = checkedColor
            afterState?.buttonX = buttonMaxX
            afterState?.radius = viewRadius
        }
        valueAnimator?.start()
    }

    private fun pendingStateSettle() {
        if (valueAnimator!!.isRunning) {
            valueAnimator?.cancel()
        }
        animateState = ANIMATE_STATE_PENDING_SETTLE
        beforeState?.update(viewState!!)
        if (isChecked()) {
            setUnCheckedViewState(afterState, true)
        } else {
            setCheckedViewState(afterState, true)
        }
        valueAnimator?.start()
    }

    private inner class ViewState {

        var buttonX = 0f
        var checkStateColor = 0
        var radius = 0f

        fun update(source: ViewState) {
            buttonX = source.buttonX
            checkStateColor = source.checkStateColor
            radius = source.radius
        }

    }

}