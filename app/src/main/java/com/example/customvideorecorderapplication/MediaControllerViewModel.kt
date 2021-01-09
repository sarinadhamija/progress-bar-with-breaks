package com.example.customvideorecorderapplication

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class MediaControllerViewModel : ViewModel() {

    private val recorderState = MutableLiveData<RecorderStateManager.RecorderState>()
    private val isDurationShort = MutableLiveData<Boolean>()

    fun isDurationShort(): MutableLiveData<Boolean> {
        return isDurationShort
    }

    fun updateShortDuration(isDurationShort: Boolean) {
        this.isDurationShort.value = isDurationShort
    }

    fun getRecorderState(): RecorderStateManager.RecorderState {
        return recorderState.value!!
    }

    fun updateRecordStatus(newState: RecorderStateManager.RecorderState) {
        recorderState.value = newState
    }

    fun getVideoRecorderCurrentState(): MutableLiveData<RecorderStateManager.RecorderState> {
        return recorderState
    }
}

