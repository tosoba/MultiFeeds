package com.example.there.multifeeds.main

import android.util.Log
import com.example.there.domain.usecase.impl.GetRelatedVideos
import com.example.there.multifeeds.base.vm.BaseViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class MainViewModel @Inject constructor(
        private val getRelatedVideos: GetRelatedVideos
) : BaseViewModel() {

    val viewState = MainActivityViewState()

    private var previousVideoId: String? = null

    fun loadRelatedVideos(videoId: String, shouldReturnAll: Boolean, onFinally: (() -> Unit)? = null, onAfterAdd: (() -> Unit)? = null) {
        fun load() {
            if (shouldReturnAll || videoId != previousVideoId)
                viewState.relatedVideos.clear()
            previousVideoId = videoId

            viewState.loadingInProgress.set(true)

            disposables.add(getRelatedVideos.execute(params = GetRelatedVideos.Params(videoId, shouldReturnAll))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally {
                        viewState.loadingInProgress.set(false)
                        onFinally?.invoke()
                    }
                    .subscribe({
                        viewState.relatedVideos.addAll(it)
                        onAfterAdd?.invoke()
                    }, {
                        Log.e(javaClass.name, it.message ?: "getRelatedVideos error")
                    }))
        }

        if (previousVideoId == null || previousVideoId != videoId) {
            disposables.clear()
            load()
        } else if (viewState.loadingInProgress.get() == false) {
            load()
        }
    }
}