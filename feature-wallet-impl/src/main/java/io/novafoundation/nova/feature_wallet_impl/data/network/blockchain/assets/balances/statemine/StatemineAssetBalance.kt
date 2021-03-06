package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.utils.assets
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class StatemineAssetBalance(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val remoteStorage: StorageDataSource,
) : AssetBalance {

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        val statemineType = chainAsset.requireStatemine()

        val assetDetails = remoteStorage.query(
            chainId = chain.id,
            keyBuilder = { it.metadata.assets().storage("Asset").storageKey(it, statemineType.id) },
            binding = { scale, runtime -> bindAssetDetails(scale!!, runtime) }
        )

        return assetDetails.minimumBalance
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val statemineType = chainAsset.requireStatemine()

        val assetAccount = remoteStorage.query(
            chainId = chain.id,
            keyBuilder = { it.metadata.assets().storage("Account").storageKey(it, statemineType.id, accountId) },
            binding = { scale, runtime -> bindAssetAccountOrEmpty(scale, runtime) }
        )

        return assetAccount.balance
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash?> {
        val statemineType = chainAsset.requireStatemine()

        val runtime = chainRegistry.getRuntime(chain.id)

        val assetDetailsKey = runtime.metadata.assets().storage("Asset").storageKey(runtime, statemineType.id)
        val assetAccountKey = runtime.metadata.assets().storage("Account").storageKey(runtime, statemineType.id, accountId)

        val isFrozenFlow = subscriptionBuilder.subscribe(assetDetailsKey)
            .map { bindAssetDetails(it.value!!, runtime).isFrozen }

        return combine(
            subscriptionBuilder.subscribe(assetAccountKey),
            isFrozenFlow
        ) { balanceStorageChange, isAssetFrozen ->
            val assetAccount = bindAssetAccountOrEmpty(balanceStorageChange.value, runtime)
            val assetChanged = updateAssetBalance(metaAccount.id, chainAsset, isAssetFrozen, assetAccount)

            balanceStorageChange.block.takeIf { assetChanged }
        }
    }

    private fun bindAssetAccountOrEmpty(scale: String?, runtime: RuntimeSnapshot): AssetAccount {
        return scale?.let { bindAssetAccount(it, runtime) } ?: AssetAccount.empty()
    }

    private suspend fun updateAssetBalance(
        metaId: Long,
        chainAsset: Chain.Asset,
        isAssetFrozen: Boolean,
        assetAccount: AssetAccount
    ) = assetCache.updateAsset(metaId, chainAsset) {
        val frozenBalance = if (isAssetFrozen || assetAccount.isFrozen) {
            assetAccount.balance
        } else {
            BigInteger.ZERO
        }

        it.copy(
            frozenInPlanks = frozenBalance,
            freeInPlanks = assetAccount.balance
        )
    }
}
