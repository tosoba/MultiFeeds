package com.example.there.flextube.list

import android.support.v7.widget.LinearLayoutManager
import com.example.there.flextube.R
import com.example.there.flextube.base.list.adapter.BaseObservableListAdapter
import com.example.there.flextube.base.list.viewholder.BaseBindingViewHolder
import com.example.there.flextube.databinding.GroupItemBinding
import com.example.there.flextube.groups.list.item.GroupItemView
import com.example.there.flextube.groups.list.item.GroupItemViewState
import com.example.there.flextube.model.UiGroupWithSubscriptions
import com.example.there.flextube.util.view.ObservableSortedList
import io.reactivex.subjects.PublishSubject

class SortedGroupsAdapter(
        items: ObservableSortedList<UiGroupWithSubscriptions>,
        itemLayoutId: Int,
        itemsOffset: Int = 0
) : BaseObservableListAdapter<UiGroupWithSubscriptions, GroupItemBinding>(items, itemLayoutId, itemsOffset) {

    override fun onBindViewHolder(holder: BaseBindingViewHolder<GroupItemBinding>?, position: Int) {
        val group = items[position]
        holder?.binding?.groupItemView =
                GroupItemView(GroupItemViewState(group), SubscriptionsAdapter(group.subscriptions, R.layout.subscription_item))
        holder?.binding?.groupSubscriptionsRecyclerView?.layoutManager =
                LinearLayoutManager(holder?.binding?.root?.context, LinearLayoutManager.HORIZONTAL, false)
        holder?.binding?.root?.setOnClickListener { groupClicked.onNext(group) }
    }

    val groupClicked: PublishSubject<UiGroupWithSubscriptions> = PublishSubject.create()
}