package com.example.customvideorecorderapplication

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator

class CustomRecorderButtonWithPauseBreaks @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mCurrentPlayTime: Long = 0L

    private val pauseAngleList: ArrayList<Float> = ArrayList()
    private val pauseTimeList: ArrayList<Long> = ArrayList()
    private val resumeTimeList: ArrayList<Long> = ArrayList()

    private val LOG = "CustomViderRecorderView"
    private var pausedAngle: Float = 0f
    private var resumedAngle: Float = 0f

    private val RECORD_STATUS_START = "record_start"
    private val RECORD_STATUS_PAUSE = "record_pause"
    private val RECORD_STATUS_RESUME = "record_resume"
    private val RECORD_STATUS_STOP = "record_stop"

    private var mCurrentRecordStatus: String = ""
    private var mLastREcordStatus: String = RECORD_STATUS_START

    private var actionListener: IRecorderActions? = null
    private var recordingColor: Int = Color.CYAN
    private var innerCircleFillColor: Int = Color.CYAN
    private var outerCircleFillColor: Int = Color.CYAN

    private var borderWidth: Float = context.resources.getDimension(R.dimen.cvb_border_width)
    private var isRecording: Boolean = false

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
    private var videoDurationInMillis: Long = VIDEO_DURATION

    private var startAngle: Float = 0f
    private var sweepAngle: Float = 0f

    private var innerCirclePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        innerCircleFillColor = ContextCompat.getColor(getContext(), R.color.color_recorder_button)
        color = innerCircleFillColor
    }

    fun setActionListener(actionListener: IRecorderActions){
        this.actionListener = actionListener
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

    private lateinit var innerCircleValueAnimator: ValueAnimator

    private lateinit var outerCircleBorderValueAnimator: ValueAnimator

    //This method accepts duration of video in milliseconds
    fun setTotalVideoDuration(duration: Long) {
        videoDurationInMillis = duration

        initializeInnerCircleLongPressAnimation()
        initializeOuterCircleBorderAnimation()
    }

    private fun initializeInnerCircleLongPressAnimation() {
        innerCircleValueAnimator = ValueAnimator.ofFloat().apply {
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

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val detectedUp = event!!.action == MotionEvent.ACTION_DOWN
        if (detectedUp) {
            performClick()
        }
        return true
    }

    private fun onClickStart() {
        resetRecordingValues()
        mLastREcordStatus = mCurrentRecordStatus
        mCurrentRecordStatus = RECORD_STATUS_START
        isRecording = true
        actionListener?.onStartRecord()
        startRecordTime = System.currentTimeMillis()
        pauseAngleList.add(-90f)
        resumeTimeList.add(startRecordTime)

        innerCircleValueAnimator.setFloatValues(innerCircleCurrentSize, innerCircleMinSize)
        innerCircleValueAnimator.start()

        outerCircleBorderValueAnimator.start()
        actionListener?.onStartRecord()
    }

    private fun onClickPause() {
        if (isRecording.not()) {
            return
        }
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

        isRecording = true
        actionListener?.onResumeRecord()
        resumeRecordTime = System.currentTimeMillis()
        resumeTimeList.add(resumeRecordTime)
        resumedAngle = startAngle + sweepAngle

        outerCircleBorderValueAnimator.setIntValues(0, videoDurationInMillis.toInt())
        outerCircleBorderValueAnimator.start()
        outerCircleBorderValueAnimator.currentPlayTime = mCurrentPlayTime
    }

    private fun onClickEnd() {
        if (isRecording.not()) {
            return
        }
        mLastREcordStatus = mCurrentRecordStatus
        mCurrentRecordStatus = RECORD_STATUS_STOP
        endRecordTime = System.currentTimeMillis()

        innerCircleValueAnimator.setFloatValues(innerCircleCurrentSize, innerCircleMaxSize)
        innerCircleValueAnimator.start()

        outerCircleBorderValueAnimator.cancel()
        actionListener?.onEndRecord()
    }

    private fun isRecordTooShort(
        startMillis: Long,
        endMillis: Long,
        minimumMillisRange: Long
    ): Boolean {
        return endMillis - startMillis < minimumMillisRange
    }

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

    private fun drawInnerCircle(canvas: Canvas) {
        canvas.drawCircle(
            outerCircleMaxSize / 2,
            outerCircleMaxSize / 2,
            innerCircleCurrentSize / 2,
            innerCirclePaint
        )
    }

    private fun drawOuterCircle(canvas: Canvas) {
        canvas.drawCircle(
            outerCircleMaxSize / 2,
            outerCircleMaxSize / 2,
            outerCircleCurrentSize / 2,
            outerCirclePaint
        )
    }

    private fun drawArc(canvas: Canvas) {
        canvas.drawArc(
            outerCircleBorderRect,
            startAngle,
            sweepAngle,
            false,
            outerCircleBorderPaint
        )
    }

    private fun drawArcForInitialState(canvas: Canvas) {
        canvas.drawArc(
            outerCircleBorderRect,
            -90f,
            calculateAngle(startRecordTime, videoDurationInMillis),
            false,
            outerCircleBorderPaint
        )
    }

    private fun drawArcForStartState(canvas: Canvas) {
        canvas.drawArc(
            outerCircleBorderRect,
            -90f,
            calculateAngle(startRecordTime, System.currentTimeMillis()),
            false,
            outerCircleBorderPaint
        )
    }

    private fun setParamsForResumeState(resumeTime: Long, pausedTime: Long) {
        startAngle += sweepAngle
        sweepAngle = calculateAngle(resumeTime, pausedTime)
        outerCircleBorderPaint.color = Color.WHITE
    }

    private fun setParamsForPauseState() {
        outerCircleBorderPaint.color = Color.TRANSPARENT
        startAngle += sweepAngle
        sweepAngle = 10f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null) {
            return
        }
        drawInnerCircle(canvas)
        drawOuterCircle(canvas)


        if (isRecording) {

            when (actionListener?.getCurrentRecorderState()) {
                RecorderStateManager.RecorderState.INIT -> {
                    drawArcForInitialState(canvas)
                }

                else -> {
                    when (mCurrentRecordStatus) {
                        RECORD_STATUS_START -> {
                            drawArcForStartState(canvas)
                        }

                        RECORD_STATUS_PAUSE -> {
                            startAngle = pauseAngleList[0]
                            sweepAngle = 0f
                            for ((i, pausedTime) in pauseTimeList.withIndex()) {
                                setParamsForResumeState(resumeTimeList[i], pausedTime)
                                drawArc(canvas)
                                setParamsForPauseState()
                                drawArc(canvas)
                            }
                        }
                        RECORD_STATUS_RESUME -> {
                            startAngle = pauseAngleList[0]
                            sweepAngle = 0f
                            var last = 0
                            for ((i, pausedTime) in pauseTimeList.withIndex()) {
                                setParamsForResumeState(resumeTimeList[i], pausedTime)
                                drawArc(canvas)
                                setParamsForPauseState()
                                drawArc(canvas)
                                last = i
                            }

                            setParamsForResumeState(resumeTimeList[last + 1], System.currentTimeMillis())
                            drawArc(canvas)
                        }
                        RECORD_STATUS_STOP -> {
                            startAngle = pauseAngleList[0]
                            sweepAngle = 0f
                            var last = 0
                            for ((i, pausedTime) in pauseTimeList.withIndex()) {
                                setParamsForResumeState(resumeTimeList[i], pausedTime)
                                drawArc(canvas)
                                setParamsForPauseState()
                                drawArc(canvas)
                                last = i
                            }

                            if (mLastREcordStatus == RECORD_STATUS_RESUME) {
                                setParamsForResumeState(resumeTimeList[last + 1], System.currentTimeMillis())
                                drawArc(canvas)
                            }
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

    fun cancelRecording() {
        if (isRecording.not()) {
            return
        }

        isRecording = false
        endRecordTime = System.currentTimeMillis()

        innerCircleValueAnimator.setFloatValues(innerCircleCurrentSize, innerCircleMaxSize)
        innerCircleValueAnimator.start()

        outerCircleBorderValueAnimator.cancel()
        actionListener?.onCancelled()

        resetRecordingValues()
    }

    fun enableVideoRecording(enableVideoRecording: Boolean) {
        this.enableVideoRecording = enableVideoRecording
    }

    fun setVideoDuration(durationInMillis: Long) {
        this.videoDurationInMillis = durationInMillis
        with(outerCircleBorderValueAnimator) {
            setIntValues(0, durationInMillis.toInt())
            duration = durationInMillis
        }
    }

}