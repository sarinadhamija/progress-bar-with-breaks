package com.example.customvideorecorderapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MediaControllerViewModel
    private lateinit var btnPausePlay: AppCompatImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MediaControllerViewModel::class.java)
        viewModel.updateRecordStatus(RecorderStateManager.RecorderState.INIT)
        setContentView(R.layout.layout_video_recorder_button)
        btnPausePlay = findViewById(R.id.ib_play_pause)
        val btn = findViewById<CustomRecorderButtonWithPauseBreaks>(R.id.btn_video_recorder)
        btn.setTotalVideoDuration(10000)
        btn.setActionListener(recorderActionListener)
    }

    private fun setPausePlayCta(isVisible: Boolean, isPaused: Boolean) {
        when (isVisible) {
            true -> {
                btnPausePlay.visibility = View.VISIBLE
                btnPausePlay.setImageResource(
                    when (isPaused) {
                        true -> R.drawable.ic_pause
                        false -> R.drawable.ic_play
                    }
                )
            }
            false -> btnPausePlay.visibility = View.GONE
        }
    }

    private val recorderActionListener = object : IRecorderActions {
        override fun onStartRecord() {
            viewModel.updateRecordStatus(RecorderStateManager.RecorderState.RECORDING)
            setPausePlayCta(true, false)
        }

        override fun onPauseRecord() {
            btnPausePlay.setImageResource(R.drawable.ic_play)
            viewModel.updateRecordStatus(RecorderStateManager.RecorderState.PAUSED)
        }

        override fun onResumeRecord() {
            btnPausePlay.setImageResource(R.drawable.ic_pause)
            viewModel.updateRecordStatus(RecorderStateManager.RecorderState.RESUMED)
        }

        override fun onEndRecord() {
            btnPausePlay.visibility = View.GONE
            viewModel.updateRecordStatus(RecorderStateManager.RecorderState.INIT)
        }

        override fun onDurationTooShortError() {
            //Handle action when recording is short
        }

        override fun onSingleTap() {
            //Handle action on single tap
        }

        override fun onCancelled() {
            btnPausePlay.visibility = View.GONE
            viewModel.updateRecordStatus(RecorderStateManager.RecorderState.INIT)
        }

        override fun getCurrentRecorderState(): RecorderStateManager.RecorderState =
            viewModel.getRecorderState()

    }
}