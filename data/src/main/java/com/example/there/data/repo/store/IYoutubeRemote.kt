package com.example.there.data.repo.store

import com.example.there.data.model.ChannelPlaylistIdData
import com.example.there.data.model.PlaylistItemData
import com.example.there.data.model.SubscriptionData
import com.example.there.data.model.VideoCategoryData
import io.reactivex.Observable
import io.reactivex.Single

interface IYoutubeRemote {
    fun getGeneralHomeItems(accessToken: String, pageToken: String? = null): Single<Pair<List<PlaylistItemData>, String?>>
    fun getHomeItemsByCategory(categoryId: String, pageToken: String? = null): Single<Pair<List<PlaylistItemData>, String?>>
    fun getChannelsPlaylistIds(channelIds: List<String>): Single<List<ChannelPlaylistIdData>>
    fun getPlaylistItems(channelId: String, pageToken: String? = null): Single<Pair<List<PlaylistItemData>, String?>>
    fun getUserSubscriptions(accessToken: String): Observable<List<SubscriptionData>>
    fun getVideoCategories(): Single<List<VideoCategoryData>>
    fun getRelatedVideos(videoId: String, pageToken: String? = null): Single<Pair<List<PlaylistItemData>, String?>>
    fun searchForVideos(query: String, pageToken: String? = null): Single<Pair<List<PlaylistItemData>, String?>>
}