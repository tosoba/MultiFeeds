package com.example.there.domain.repo

import com.example.there.domain.model.PlaylistItem
import com.example.there.domain.model.Subscription
import com.example.there.domain.model.VideoCategory
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single

interface IMainRepository {
    fun getSubs(accessToken: String, accountName: String): Observable<List<Subscription>>
    fun updateSavedSubscriptions(subs: List<Subscription>, accountName: String): Completable

    fun loadVideos(channelIds: List<String>): Observable<List<PlaylistItem>>
    fun loadMoreVideos(channelIds: List<String>): Completable
    fun getSavedVideos(channelIds: List<String>): Flowable<List<PlaylistItem>>

    fun getGeneralHomeItems(accessToken: String, shouldReturnAll: Boolean = false): Single<List<PlaylistItem>>
    fun getHomeItemsByCategory(categoryId: String, shouldReturnAll: Boolean = false): Single<List<PlaylistItem>>

    fun getVideoCategories(): Single<List<VideoCategory>>
}