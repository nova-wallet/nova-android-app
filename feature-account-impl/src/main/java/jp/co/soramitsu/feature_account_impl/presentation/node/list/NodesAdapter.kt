package jp.co.soramitsu.feature_account_impl.presentation.node.list

import android.view.View
import android.view.ViewGroup
import jp.co.soramitsu.common.groupedList.BaseGroupedDiffCallback
import jp.co.soramitsu.common.groupedList.GroupedListAdapter
import jp.co.soramitsu.common.groupedList.GroupedListHolder
import jp.co.soramitsu.common.utils.inflateChild
import jp.co.soramitsu.feature_account_impl.R
import jp.co.soramitsu.feature_account_impl.presentation.node.model.NodeHeaderModel
import jp.co.soramitsu.feature_account_impl.presentation.node.model.NodeModel
import kotlinx.android.synthetic.main.item_node.view.nodeCheck
import kotlinx.android.synthetic.main.item_node.view.nodeHost
import kotlinx.android.synthetic.main.item_node.view.nodeIcon
import kotlinx.android.synthetic.main.item_node.view.nodeInfo
import kotlinx.android.synthetic.main.item_node.view.nodeTitle
import kotlinx.android.synthetic.main.item_node_group.view.nodeGroupTitle

class NodesAdapter(
    private val nodeItemHandler: NodeItemHandler
) : GroupedListAdapter<NodeHeaderModel, NodeModel>(NodesDiffCallback) {

    interface NodeItemHandler {

        fun infoClicked(nodeModel: NodeModel)

        fun checkClicked(nodeModel: NodeModel)
    }

    private var selectedItem: NodeModel? = null

    fun updateSelectedNode(newSelection: NodeModel) {
        val positionToHide = selectedItem?.let { selected ->
            findIndexOfElement<NodeModel> { selected.id == it.id }
        }

        val positionToShow = findIndexOfElement<NodeModel> {
            newSelection.id == it.id
        }

        selectedItem = newSelection

        positionToHide?.let { notifyItemChanged(it) }
        notifyItemChanged(positionToShow)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return NodeGroupHolder(parent.inflateChild(R.layout.item_node_group))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return NodeHolder(parent.inflateChild(R.layout.item_node))
    }

    override fun bindGroup(holder: GroupedListHolder, group: NodeHeaderModel) {
        (holder as NodeGroupHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: NodeModel) {
        val isChecked = child.id == selectedItem?.id

        (holder as NodeHolder).bind(child, nodeItemHandler, isChecked)
    }
}

class NodeGroupHolder(view: View) : GroupedListHolder(view) {
    fun bind(nodeHeaderModel: NodeHeaderModel) = with(containerView) {
        nodeGroupTitle.text = nodeHeaderModel.title
    }
}

class NodeHolder(view: View) : GroupedListHolder(view) {

    fun bind(
        nodeModel: NodeModel,
        handler: NodesAdapter.NodeItemHandler,
        isChecked: Boolean
    ) {
        with(containerView) {
            nodeTitle.text = nodeModel.name
            nodeHost.text = nodeModel.link

            nodeCheck.visibility = if (isChecked) View.VISIBLE else View.INVISIBLE

            nodeIcon.setImageResource(nodeModel.networkModelType.icon)

            setOnClickListener { handler.checkClicked(nodeModel) }

            nodeInfo.setOnClickListener { handler.infoClicked(nodeModel) }
        }
    }
}

private object NodesDiffCallback : BaseGroupedDiffCallback<NodeHeaderModel, NodeModel>(NodeHeaderModel::class.java) {

    override fun areGroupItemsTheSame(oldItem: NodeHeaderModel, newItem: NodeHeaderModel): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areGroupContentsTheSame(oldItem: NodeHeaderModel, newItem: NodeHeaderModel): Boolean {
        return oldItem == newItem
    }

    override fun areChildItemsTheSame(oldItem: NodeModel, newItem: NodeModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areChildContentsTheSame(oldItem: NodeModel, newItem: NodeModel): Boolean {
        return oldItem == newItem
    }
}