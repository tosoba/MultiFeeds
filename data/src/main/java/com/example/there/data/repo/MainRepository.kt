package com.example.there.data.repo

import com.example.there.data.mapper.PlaylistItemMapper
import com.example.there.data.mapper.SubscriptionMapper
import com.example.there.data.mapper.VideoCategoryMapper
import com.example.there.data.model.ChannelPlaylistIdData
import com.example.there.data.model.PlaylistData
import com.example.there.data.model.PlaylistItemData
import com.example.there.data.repo.store.IYoutubeCache
import com.example.there.data.repo.store.IYoutubeRemote
import com.example.there.domain.model.PlaylistItem
import com.example.there.domain.model.Subscription
import com.example.there.domain.model.VideoCategory
import com.example.there.domain.repo.IMainRepository
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

class MainRepository @Inject constructor(
        private val youtubeRemoteDataStore: IYoutubeRemote,
        private val youtubeCachedDataStore: IYoutubeCache
) : IMainRepository {

    private fun concatSavedAndRemoteHomeItems(
            remote: Single<Pair<List<PlaylistItemData>, String?>>,
            savedVideos: List<PlaylistItemData>
    ): Single<Pair<ArrayList<PlaylistItemData>, String?>> = remote.map { (remoteVideos, nextPageToken) ->
        ArrayList<PlaylistItemData>(remoteVideos.size + savedVideos.size).apply {
            addAll(savedVideos)
            addAll(remoteVideos)
        } to nextPageToken
    }

    private fun handleHomeItems(
            shouldReturnAll: Boolean,
            savedVideos: List<PlaylistItemData>,
            nextPageToken: String?,
            remote: Single<Pair<List<PlaylistItemData>, String?>>
    ): Single<out Pair<List<PlaylistItemData>, String?>> = if (savedVideos.isEmpty() || nextPageToken != null) {
        if (shouldReturnAll) concatSavedAndRemoteHomeItems(remote, savedVideos)
        else remote
    } else if (shouldReturnAll) {
        Single.just(savedVideos to nextPageToken)
    } else {
        Single.error { IllegalStateException("No more home items to retrieve.") }
    }

    override fun getGeneralHomeItems(
            accessToken: String,
            shouldReturnAll: Boolean
    ): Single<List<PlaylistItem>> = youtubeCachedDataStore.getSavedHomeItems(IYoutubeCache.CATEGORY_GENERAL)
            .flatMap { (savedVideos, nextPageToken) ->
                handleHomeItems(shouldReturnAll, savedVideos, nextPageToken, retrieveAndSaveGeneralHomeItems(accessToken, nextPageToken))
            }
            .map { (videos, _) -> videos.map(PlaylistItemMapper::toDomain) }

    private fun retrieveAndSaveGeneralHomeItems(
            accessToken: String,
            pageToken: String?
    ): Single<Pair<List<PlaylistItemData>, String?>> = youtubeRemoteDataStore.getGeneralHomeItems(accessToken, pageToken)
            .doOnSuccess { (videos, nextPageToken) ->
                youtubeCachedDataStore.saveHomeItems(IYoutubeCache.CATEGORY_GENERAL, videos, nextPageToken)
            }

    override fun getHomeItemsByCategory(
            categoryId: String,
            shouldReturnAll: Boolean
    ): Single<List<PlaylistItem>> = youtubeCachedDataStore.getSavedHomeItems(categoryId)
            .flatMap { (savedVideos, nextPageToken) ->
                handleHomeItems(shouldReturnAll, savedVideos, nextPageToken, retrieveAndSaveHomeItemsFromCategory(categoryId, nextPageToken))
            }
            .map { (videos, _) -> videos.map(PlaylistItemMapper::toDomain) }

    private fun retrieveAndSaveHomeItemsFromCategory(
            categoryId: String,
            pageToken: String?
    ): Single<Pair<List<PlaylistItemData>, String?>> = youtubeRemoteDataStore.getHomeItemsByCategory(categoryId, pageToken)
            .doOnSuccess { (videos, nextPageToken) ->
                youtubeCachedDataStore.saveHomeItems(categoryId, videos, nextPageToken)
            }

    override fun getSubs(
            accessToken: String,
            accountName: String
    ): Observable<List<Subscription>> = youtubeCachedDataStore.saveUser(accountName)
            .andThen(youtubeRemoteDataStore.getUserSubscriptions(accessToken)
                    .doOnNext { youtubeCachedDataStore.saveUserSubscriptions(it, accountName) }
                    .map { it.map(SubscriptionMapper::toDomain) })

    override fun updateSavedSubscriptions(
            subs: List<Subscription>, accountName: String
    ): Completable = youtubeCachedDataStore.updateSavedSubscriptions(subs.map(SubscriptionMapper::toData), accountName)

    override fun getVideoCategories(): Single<List<VideoCategory>> = youtubeRemoteDataStore.getVideoCategories()
            .map { it.map(VideoCategoryMapper::toDomain) }

    private fun getChannelPlaylistIds(
            channelIds: List<String>
    ): Observable<ChannelPlaylistIdData> = Observable.just(channelIds)
            .flatMapIterable { it }
            .buffer(50)
            .flatMap { youtubeRemoteDataStore.getChannelsPlaylistIds(it).toObservable() }
            .flatMapIterable { it }

    override fun getVideos(
            channelIds: List<String>
    ): Observable<List<PlaylistItem>> = getChannelPlaylistIds(channelIds)
            .flatMap { cp ->
                youtubeCachedDataStore.savePlaylist(cp.playlistId, cp.channelId)
                        .andThen(getAndSaveRemoteVideos(cp.playlistId))
            }
            .map { videos -> videos.map(PlaylistItemMapper::toDomain) }

    override fun getMoreVideos(
            channelIds: List<String>
    ): Observable<List<PlaylistItem>> = getPlaylistData(channelIds)
            .toObservable()
            .flatMapIterable { it.toList() }
            .flatMap { getAndSaveRemoteVideos(it.id, it.nextPageToken) }
            .map { videos -> videos.map(PlaylistItemMapper::toDomain) }

    private fun getAndSaveRemoteVideos(
            playlistId: String,
            pageToken: String? = null
    ): Observable<List<PlaylistItemData>> = youtubeRemoteDataStore.getPlaylistItems(playlistId, pageToken)
            .toObservable()
            .flatMap { (videos, nextPageToken) ->
                youtubeCachedDataStore.saveRetrievedVideos(playlistId, videos, nextPageToken)
                        .andThen(Observable.just(videos))
            }

    private fun getPlaylistData(
            channelIds: List<String>
    ): Single<List<PlaylistData>> = Single.zip(channelIds.map { youtubeCachedDataStore.getPlaylistByChannelId(it) }) {
        it.map { it as PlaylistData }
    }

    override fun getSavedVideos(
            channelIds: List<String>
    ): Flowable<List<PlaylistItem>> = getPlaylistData(channelIds)
            .toFlowable()
            .flatMapIterable { it.toList() }
            .flatMap { youtubeCachedDataStore.getSavedVideos(it.id) }
            .map { videos -> videos.map(PlaylistItemMapper::toDomain) }
}
