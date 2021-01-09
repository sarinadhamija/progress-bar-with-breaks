package com.example.customvideorecorderapplication


interface IRecorderActions {

    fun onStartRecord()

    fun onPauseRecord()

    fun onResumeRecord()

    fun onEndRecord()

    fun onDurationTooShortError()

    fun onSingleTap()

    fun onCancelled()

    fun getCurrentRecorderState() : RecorderStateManager.RecorderState
}
