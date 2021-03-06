package io.novafoundation.nova.feature_account_impl.presentation.node.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.map
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.utils.setValueIfNew
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.mappers.mapNodeToNodeModel
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.node.NodeDetailsRootViewModel
import io.novafoundation.nova.feature_account_impl.presentation.node.model.NodeModel
import kotlinx.coroutines.launch

class NodeDetailsViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val nodeId: Int,
    private val clipboardManager: ClipboardManager,
    private val resourceManager: ResourceManager
) : NodeDetailsRootViewModel(resourceManager) {

    val nodeModelLiveData = liveData {
        emit(getNode(nodeId))
    }

    val nameEditEnabled = nodeModelLiveData.map(::mapNodeNameEditState)
    val hostEditEnabled = nodeModelLiveData.map(::mapNodeHostEditState)

    private val _updateButtonEnabled = MutableLiveData<Boolean>()
    val updateButtonEnabled: LiveData<Boolean> = _updateButtonEnabled

    fun backClicked() {
        router.back()
    }

    fun nodeDetailsEdited() {
        _updateButtonEnabled.setValueIfNew(true)
    }

    fun copyNodeHostClicked() {
        nodeModelLiveData.value?.let {
            clipboardManager.addToClipboard(it.link)

            showMessage(resourceManager.getString(R.string.common_copied))
        }
    }

    fun updateClicked(name: String, hostUrl: String) {
        viewModelScope.launch {
            val result = interactor.updateNode(nodeId, name, hostUrl)

            if (result.isSuccess) {
                router.back()
            } else {
                handleNodeException(result.requireException())
            }
        }
    }

    private suspend fun getNode(nodeId: Int): NodeModel {
        val node = interactor.getNode(nodeId)

        return mapNodeToNodeModel(node)
    }

    private fun mapNodeNameEditState(node: NodeModel): Boolean {
        return !node.isDefault
    }

    private fun mapNodeHostEditState(node: NodeModel): Boolean {
        return !node.isDefault && !node.isActive
    }
}
