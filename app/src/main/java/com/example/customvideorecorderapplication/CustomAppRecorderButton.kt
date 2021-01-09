package com.example.customvideorecorderapplication

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import kotlin.collections.ArrayList

class CustomAppRecorderButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mCurrentPlayTime: Long = 0L
    private val pauseAngleList: ArrayList<Float> = ArrayList()
    private val pauseTimeList: ArrayList<Long> = ArrayList()
    private val resumeTimeList: ArrayList<Long> = ArrayList()

    private var pausedAngle : Float = 0f
    private var resumedAngle : Float = 0f
    private var startAngle: Float = 0f
    private var sweepAngle: Float = 0f

    private val RECORD_STATUS_START = "start"
    private val RECORD_STATUS_PAUSE = "pause"
    private val RECORD_STATUS_RESUME = "resume"
    private val RECORD_STATUS_STOP = "stop"

    private var mCurrentRecordStatus: String = ""
    private var mLastREcordStatus: String = RECORD_STATUS_START

    private var actionListener: IRecorderActions? = null
    private var recordingColor: Int = Color.CYAN
    private var innerCircleFillColor: Int = Color.CYAN
    private var outerCircleFillColor: Int = Color.CYAN
    private var borderWidth: Float = context.resources.getDimension(R.dimen.cvb_border_width)

    private var startRecordTime: Long = 0
    private var resumeRecordTime: Long = 0
    private var endRecordTime: Long = 0
    private var lastPausedTime: Long = 0L

    private var innerCircleMaxSize: Float = 0f
    private var innerCircleMinSize: Float = 0f
    private var innerCircleCurrentSize: Float = 0f
    private var outerCircleMaxSize: Float = 0f
    private var outerCircleMinSize: Float = 0f
    private var outerCircleCurrentSize: Float = 0f
    private var enableVideoRecording: Boolean = true
    private var enablePhotoTaking: Boolean = true
    private var videoDurationInMillis: Long = VIDEO_DURATION

    private var innerCirclePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        innerCircleFillColor = ContextCompat.getColor(getContext(), R.color.color_recorder_button)
        color = innerCircleFillColor
    }

    private var outerCirclePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        outerCircleFillColor = ContextCompat.getColor(getContext(), R.color.color_recorder_button)
        color = outerCircleFillColor
        alpha = 100
    }

    private var outerCircleBorderPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = recordingColor
        strokeWidth = borderWidth
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = CornerPathEffect(30f)
    }

    private var outerCircleBorderRect = RectF()

    private lateinit var innerCircleSingleTapValueAnimator: ValueAnimator

    private lateinit var innerCircleLongPressValueAnimator: ValueAnimator

    private lateinit var outerCircleBorderValueAnimator: ValueAnimator

    //This method accepts duration of video in milliseconds
    fun setTotalVideoDuration(duration: Long) {
        videoDurationInMillis = duration

        initializeInnerCircleLongPressAnimation()
        initializeInnerCircleSingleTapAnimation()
        initializeOuterCircleBorderAnimation()
    }

    private fun initializeInnerCircleSingleTapAnimation() {
        innerCircleSingleTapValueAnimator = ValueAnimator.ofFloat().apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 300L
            addUpdateListener {
                innerCircleCurrentSize = it.animatedValue as Float
                postInvalidate()
            }
        }
    }

    private fun initializeInnerCircleLongPressAnimation() {
        innerCircleLongPressValueAnimator = ValueAnimator.ofFloat().apply {
            interpolator = LinearOutSlowInInterpolator()
            duration = MINIMUM_VIDEO_DURATION_MILLIS
            addUpdateListener {
                innerCircleCurrentSize = it.animatedValue as Float
                postInvalidate()
            }
        }
    }

    private fun initializeOuterCircleBorderAnimation() {
        outerCircleBorderValueAnimator =
            ValueAnimator.ofInt(0, videoDurationInMillis.toInt()).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = videoDurationInMillis
                addUpdateListener {
                    if ((it.animatedValue as Int) == videoDurationInMillis.toInt()) {
                        onClickEnd()
                    }
                    postInvalidate()
                }
            }
    }

    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomRecorderButton,
            defStyleAttr,
            defStyleAttr
        )
        recordingColor = typedArray.getColor(
            R.styleable.CustomRecorderButton_cvb_recording_color,
            Color.WHITE
        )
        innerCircleFillColor = typedArray.getColor(
            R.styleable.CustomRecorderButton_cvb_inner_circle_color,
            Color.WHITE
        )
        outerCircleFillColor = typedArray.getColor(
            R.styleable.CustomRecorderButton_cvb_outer_circle_color,
            Color.WHITE
        )
        outerCircleBorderPaint.color = recordingColor
        typedArray.recycle()
    }

    override fun performClick(): Boolean {
        handleRecordButtonClicked()
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val detectedUp = event!!.action == MotionEvent.ACTION_DOWN
        if (detectedUp) {
            performClick()
        }
        return true

    }

    private fun handleRecordButtonClicked() {
        actionListener?.apply {
            when (getCurrentRecorderState()) {
                RecorderStateManager.RecorderState.RECORDING -> {
                    onClickPause()
                }
                RecorderStateManager.RecorderState.PAUSED -> {
                    onClickResume()
                }
                RecorderStateManager.RecorderState.RESUMED -> {
                    onClickPause()
                }

                RecorderStateManager.RecorderState.INIT -> {
                    onClickStart()
                }
            }
        }
    }

    private fun onClickStart() {
        resetRecordingValues()
        mLastREcordStatus = mCurrentRecordStatus
        mCurrentRecordStatus = RECORD_STATUS_START

        startRecordTime = System.currentTimeMillis()
        pauseAngleList.add(-90f)
        resumeTimeList.add(startRecordTime)

        innerCircleLongPressValueAnimator.setFloatValues(innerCircleCurrentSize, innerCircleMinSize)
        innerCircleLongPressValueAnimator.start()

        outerCircleBorderValueAnimator.start()
        actionListener?.onStartRecord()
    }

    private fun onClickPause() {
        mLastREcordStatus = mCurrentRecordStatus
        mCurrentRecordStatus = RECORD_STATUS_PAUSE
        mCurrentPlayTime = outerCircleBorderValueAnimator.currentPlayTime
        outerCircleBorderValueAnimator.cancel()
        pausedAngle = startAngle + sweepAngle
        pauseAngleList.add(pausedAngle)
        pauseTimeList.add(System.currentTimeMillis())
        lastPausedTime = System.currentTimeMillis()
        actionListener?.onPauseRecord()
    }

    private fun onClickResume() {
        mLastREcordStatus = mCurrentRecordStatus
        mCurrentRecordStatus = RECORD_STATUS_RESUME
        actionListener?.onResumeRecord()
        resumeRecordTime = System.currentTimeMillis()
        resumeTimeList.add(resumeRecordTime)
        resumedAngle = startAngle + sweepAngle

        outerCircleBorderValueAnimator.setIntValues(0, videoDurationInMillis.toInt())
        outerCircleBorderValueAnimator.start()
        outerCircleBorderValueAnimator.currentPlayTime = mCurrentPlayTime
    }

    private fun onClickEnd() {
        mLastREcordStatus = mCurrentRecordStatus
        mCurrentRecordStatus = RECORD_STATUS_STOP
        endRecordTime = System.currentTimeMillis()

        innerCircleLongPressValueAnimator.setFloatValues(innerCircleCurrentSize, innerCircleMaxSize)
        innerCircleLongPressValueAnimator.start()

        outerCircleBorderValueAnimator.cancel()

        if (isRecordTooShort(startRecordTime, endRecordTime, MINIMUM_VIDEO_DURATION_MILLIS)) {
            actionListener?.onDurationTooShortError()
        } else if (actionListener?.getCurrentRecorderState() == RecorderStateManager.RecorderState.RECORDING
            || actionListener?.getCurrentRecorderState() == RecorderStateManager.RecorderState.PAUSED
            || actionListener?.getCurrentRecorderState() == RecorderStateManager.RecorderState.RESUMED
        ) {
            actionListener?.onEndRecord()
        }
    }

    private fun isRecordTooShort(startMillis: Long, endMillis: Long, minimumMillisRange: Long) = endMillis - startMillis < minimumMillisRange

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val minSide = Math.min(w, h)

        innerCircleMaxSize = minSide.toFloat()
        innerCircleMinSize = minSide.toFloat() / 1.5f
        innerCircleCurrentSize = innerCircleMaxSize

        outerCircleMaxSize = minSide.toFloat()
        outerCircleMinSize = minSide.toFloat() / 1.5f
        outerCircleCurrentSize = outerCircleMaxSize

        outerCircleBorderRect.set(
            borderWidth / 2,
            borderWidth / 2,
            outerCircleMaxSize - borderWidth / 2f,
            outerCircleMaxSize - borderWidth / 2
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null) {
            return
        }

        canvas.drawCircle(
            outerCircleMaxSize / 2,
            outerCircleMaxSize / 2,
            innerCircleCurrentSize / 2,
            innerCirclePaint
        )
        canvas.drawCircle(
            outerCircleMaxSize / 2,
            outerCircleMaxSize / 2,
            outerCircleCurrentSize / 2,
            outerCirclePaint
        )


        when (actionListener?.getCurrentRecorderState()) {
            RecorderStateManager.RecorderState.INIT -> {
                canvas.drawArc(
                    outerCircleBorderRect,
                    -90f,
                    calculateAngle(startRecordTime, videoDurationInMillis),
                    false,
                    outerCircleBorderPaint
                )
            }

            else -> {
                when (mCurrentRecordStatus) {
                    RECORD_STATUS_START -> {
                        canvas.drawArc(
                            outerCircleBorderRect,
                            -90f,
                            calculateAngle(startRecordTime, System.currentTimeMillis()),
                            false,
                            outerCircleBorderPaint
                        )
                    }

                    RECORD_STATUS_PAUSE -> {
                        startAngle = pauseAngleList[0]
                        sweepAngle = 0f
                        for ((i, pausedTime) in pauseTimeList.withIndex()) {
                            startAngle += sweepAngle
                            sweepAngle = calculateAngle(resumeTimeList[i], pausedTime)
                            outerCircleBorderPaint.color = Color.WHITE
                            canvas.drawArc(
                                outerCircleBorderRect,
                                startAngle,
                                sweepAngle,
                                false,
                                outerCircleBorderPaint
                            )
                        }
                    }
                    RECORD_STATUS_RESUME -> {
                        startAngle = pauseAngleList[0]
                        sweepAngle = 0f
                        var last = 0
                        for ((i, pausedTime) in pauseTimeList.withIndex()) {
                            startAngle += sweepAngle
                            sweepAngle = calculateAngle(resumeTimeList[i], pausedTime)
                            outerCircleBorderPaint.color = Color.WHITE
                            canvas.drawArc(
                                outerCircleBorderRect,
                                startAngle,
                                sweepAngle,
                                false,
                                outerCircleBorderPaint
                            )
                            last = i
                        }

                        outerCircleBorderPaint.color = Color.WHITE
                        startAngle += sweepAngle
                        sweepAngle =
                            calculateAngle(resumeTimeList[last + 1], System.currentTimeMillis())
                        canvas.drawArc(
                            outerCircleBorderRect,
                            startAngle,
                            sweepAngle,
                            false,
                            outerCircleBorderPaint
                        )
                    }
                    RECORD_STATUS_STOP -> {
                        startAngle = pauseAngleList[0]
                        sweepAngle = 0f
                        var last = 0
                        for ((i, pausedTime) in pauseTimeList.withIndex()) {
                            startAngle += sweepAngle
                            sweepAngle = calculateAngle(resumeTimeList[i], pausedTime)
                            outerCircleBorderPaint.color = Color.WHITE
                            canvas.drawArc(
                                outerCircleBorderRect,
                                startAngle,
                                sweepAngle,
                                false,
                                outerCircleBorderPaint
                            )
                            last = i
                        }

                        if (mLastREcordStatus == RECORD_STATUS_RESUME) {
                            outerCircleBorderPaint.color = Color.WHITE
                            startAngle += sweepAngle
                            sweepAngle =
                                calculateAngle(resumeTimeList[last + 1], System.currentTimeMillis())
                            canvas.drawArc(
                                outerCircleBorderRect,
                                startAngle,
                                sweepAngle,
                                false,
                                outerCircleBorderPaint
                            )
                        }
                    }
                }

            }
        }


    }

    private fun resetRecordingValues() {
        startRecordTime = 0
        endRecordTime = 0
        resumeRecordTime = 0
        pauseTimeList.clear()
        resumeTimeList.clear()
        pauseAngleList.clear()
        mCurrentPlayTime = 0
        lastPausedTime = 0
    }

    private fun calculateAngle(startTime: Long, stopTime: Long): Float {
        val millisPassed = stopTime - startTime
        return millisPassed * 360f / videoDurationInMillis
    }

    companion object {
        private const val MINIMUM_VIDEO_DURATION_MILLIS = 300L
        private const val VIDEO_DURATION = 10000L
    }

    fun setListener(listener: IRecorderActions) {
        this.actionListener = listener
    }
}


