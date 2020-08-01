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
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Checkable
import androidx.core.content.ContextCompat
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_DRAGING
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_NONE
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_PENDING_DRAG
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_PENDING_RESET
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_PENDING_SETTLE
import com.twenk11k.bswitch.utils.Constants.ANIMATE_STATE_SWITCH
import com.twenk11k.bswitch.utils.Utils.Companion.dpToPx
import com.twenk11k.bswitch.utils.Utils.Companion.dpToPxInt
import com.twenk11k.bswitch.utils.Utils.Companion.typedBoolean
import com.twenk11k.bswitch.utils.Utils.Companion.typedColor
import com.twenk11k.bswitch.utils.Utils.Companion.typedInt
import com.twenk11k.bswitch.utils.Utils.Companion.typedPixelSize


class BSwitch : View, Checkable {

    private val widthDefault = dpToPxInt(60f)
    private val heightDefault: Int = dpToPxInt(32f)
    private val durationDefault = 400
    private var borderWidthDefault = 2f
    private val circlePadding = 8

    private var borderWidth = dpToPxInt(borderWidthDefault)

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

    private var isSizeChanged = false
    private var isTouchingDown = false

    private var touchDownTime: Long = 0

    private var checkedLineOffsetX = 0f
    private var checkedLineOffsetY = 0f

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

        checkedLineOffsetX = dpToPx(4f)
        checkedLineOffsetY = dpToPx(4f)

        uncheckedColor = typedColor(
            typedArray,
            R.styleable.BSwitch_uncheck_color,
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

        paint = Paint(Paint.ANTI_ALIAS_FLAG)
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

    private fun initCirclePaint() {
        circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        circlePaint?.style = Paint.Style.STROKE
        circlePaint?.strokeWidth = 1f
        circlePaint?.color = Color.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint?.style = Paint.Style.FILL
        paint?.color = viewState!!.checkStateColor
        paint?.strokeWidth = borderWidth.toFloat()

        drawRoundRect(
            canvas,
            left, top, right, bottom,
            viewRadius, paint!!
        )

        canvas.drawCircle(viewState!!.buttonX, centerY, circleCornerRadius, circlePaint!!)

    }

    private fun drawRoundRect(
        canvas: Canvas,
        left: Float, top: Float,
        right: Float, bottom: Float,
        backgroundRadius: Float,
        paint: Paint
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(
                left, top, right, bottom,
                backgroundRadius, backgroundRadius, paint
            )
        } else {
            rect[left, top, right] = bottom
            canvas.drawRoundRect(
                rect,
                backgroundRadius, backgroundRadius, paint
            )
        }
    }

    private fun broadcastEvent() {
        if (onCheckedChangeListener != null) {
            isEventBroadcast = true
            onCheckedChangeListener?.onCheckedChanged(this, isChecked())
        }
        isEventBroadcast = false
    }

    override fun isChecked(): Boolean {
        return isChecked
    }

    override fun toggle() {
        toggle(true, false)
    }

    private fun toggle(broadcast: Boolean, isTouch: Boolean) {
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
            setCheckedViewState(afterState, isTouch)
        } else {
            setUnCheckedViewState(afterState, isTouch)
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
        circleCornerRadius = viewRadius - borderWidth - circlePadding
        left = viewPadding
        top = viewPadding
        right = w - viewPadding
        bottom = h - viewPadding
        centerX = (left + right) * .5f
        centerY = (top + bottom) * .5f
        buttonMinX = left + viewRadius
        buttonMaxX = right - viewRadius
        Log.d("BSwitch", "minX: $buttonMinX")
        Log.d("BSwitch", "maxX: $buttonMaxX")
        isChecked = true
        if (isChecked()) {
            setUnCheckedViewState(viewState, true)
        } else {
            setCheckedViewState(viewState, true)
        }

        isSizeChanged = true
        postInvalidate()
    }

    private fun setUnCheckedViewState(viewState: ViewState?, isReversed: Boolean) {
        Log.d("BSwitch", "toggle unchecked")
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
        Log.d("BSwitch", "toggle checked")
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

                    Log.d("BSwitchOnAnim", "ANIMATE_STATE_SWITCH")
                    viewState?.buttonX = (beforeState!!.buttonX
                            + (afterState!!.buttonX - beforeState!!.buttonX) * value)
                    val fraction = (viewState!!.buttonX - buttonMinX) / (buttonMaxX - buttonMinX)
                    viewState?.checkStateColor = argbEvaluator.evaluate(
                        1 - fraction,
                        uncheckedColor,
                        checkedColor
                    ) as Int
                    viewState!!.radius = fraction * viewRadius
                }
                ANIMATE_STATE_PENDING_SETTLE -> {
                    run {}
                    run {}
                    run {
                        Log.d("BSwitchOnAnim", "ANIMATE_STATE_PENDING_SETTLE")

                        viewState?.radius = (beforeState!!.radius
                                + (afterState!!.radius - beforeState!!.radius) * value)
                        if (animateState != ANIMATE_STATE_PENDING_DRAG) {
                            viewState?.buttonX = (beforeState!!.buttonX
                                    + (afterState!!.buttonX - beforeState!!.buttonX) * value)
                        }
                        viewState?.checkStateColor = argbEvaluator.evaluate(
                             value,
                            beforeState?.checkStateColor,
                            afterState?.checkStateColor
                        ) as Int
                    }
                }
                ANIMATE_STATE_PENDING_DRAG -> {
                    Log.d("BSwitchOnAnim", "ANIMATE_STATE_PENDING_DRAG")

                    viewState?.radius = (beforeState!!.radius
                            + (afterState!!.radius - beforeState!!.radius) * value)
                    if (animateState != ANIMATE_STATE_PENDING_DRAG) {
                        viewState?.buttonX = (beforeState!!.buttonX
                                + (afterState!!.buttonX - beforeState!!.buttonX) * value)
                    }
                    viewState?.checkStateColor = argbEvaluator.evaluate(
                        value,
                        beforeState?.checkStateColor,
                        afterState?.checkStateColor
                    ) as Int

                    Log.d("BSwitchOnAnim", "isChecked: $isChecked")

                }
                ANIMATE_STATE_PENDING_RESET -> {
                    run {}
                    run {
                        viewState?.radius = (beforeState!!.radius
                                + (afterState!!.radius - beforeState!!.radius) * value)
                        if (animateState != ANIMATE_STATE_PENDING_DRAG) {
                            viewState?.buttonX = (beforeState!!.buttonX
                                    + (afterState!!.buttonX - beforeState!!.buttonX) * value)
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
                Log.d("BSwitch", "ACTION_DOWN")
                isTouchingDown = true
                touchDownTime = System.currentTimeMillis()
                removeCallbacks(postPendingDrag)
                postDelayed(postPendingDrag, 100)
            }
            MotionEvent.ACTION_MOVE -> {
                Log.d("BSwitch", "ACTION_MOVE")
                val eventX = event.x
                Log.d("BSwitch", "eventX: ${event.x}")
                if (isPendingStateDrag()) {
                    var fraction = eventX / getWidth()
                    fraction = maxOf(0f, minOf(1f, fraction))
                    Log.d("BSwitch", "fraction: $fraction")

                    val buttonX = (buttonMinX
                            + (buttonMaxX - buttonMinX)
                            * fraction)

                    viewState?.buttonX = buttonX

                } else if (isStateDrag()) {
                    var fraction = eventX / getWidth()

                    fraction = maxOf(0f, minOf(1f, fraction))

                    val buttonX = (buttonMinX
                            + (buttonMaxX - buttonMinX)
                            * fraction)

                    viewState?.buttonX = buttonX
                    viewState?.checkStateColor = argbEvaluator.evaluate(
                        1-fraction,
                        uncheckedColor,
                        checkedColor
                    ) as Int
                    postInvalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                Log.d("BSwitch", "ACTION_UP")
                isTouchingDown = false
                removeCallbacks(postPendingDrag)
                if (System.currentTimeMillis() - touchDownTime <= 300) {
                    Log.d("BSwitchOnAnim", "toggle")
                    toggle(true, true)
                } else if (isStateDrag()) {
                    val eventX = event.x
                    var fraction = eventX / getWidth()
                    fraction = maxOf(0f, minOf(1f, fraction))
                    val newCheck = fraction > .5f
                    if (newCheck == isChecked()) {
                        Log.d("BSwitchOnAnim", "if")
                        pendingStateCancelDrag()
                    } else {
                        Log.d("BSwitchOnAnim", "else")
                        isChecked = newCheck
                        pendingStateSettle()
                    }
                } else if (isPendingStateDrag()) {
                    Log.d("BSwitchOnAnim", "else else if")
                    pendingStateCancelDrag()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                Log.d("BSwitch", "ACTION_CANCEL")
                isTouchingDown = false
                removeCallbacks(postPendingDrag)
                if (isPendingStateDrag() || isStateDrag()) {
                    Log.d("BSwitch123123", "cancel if")
                    pendingStateCancelDrag()
                }
            }
        }
        return true
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