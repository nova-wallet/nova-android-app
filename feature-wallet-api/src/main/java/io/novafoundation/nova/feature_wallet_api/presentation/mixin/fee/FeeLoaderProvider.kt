package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class FeeLoaderProvider(
    private val resourceManager: ResourceManager,
    private val tokenUseCase: TokenUseCase,
) : FeeLoaderMixin.Presentation {

    override val feeLiveData = MutableLiveData<FeeStatus>()

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    override suspend fun loadFeeSuspending(
        coroutineScope: CoroutineScope,
        feeConstructor: suspend (Token) -> BigInteger,
        onRetryCancelled: () -> Unit,
    ): Unit = withContext(Dispatchers.Default) {
        feeLiveData.postValue(FeeStatus.Loading)

        val token = tokenUseCase.currentToken()

        val feeResult = runCatching {
            feeConstructor(token)
        }

        val value = if (feeResult.isSuccess) {
            val feeInPlanks = feeResult.getOrThrow()
            val fee = token.amountFromPlanks(feeInPlanks)
            val feeModel = mapFeeToFeeModel(fee, token)

            FeeStatus.Loaded(feeModel)
        } else {
            val exception = feeResult.exceptionOrNull()

            if (exception is CancellationException) {
                null
            } else {
                retryEvent.postValue(
                    Event(
                        RetryPayload(
                            title = resourceManager.getString(R.string.choose_amount_network_error),
                            message = resourceManager.getString(R.string.choose_amount_error_fee),
                            onRetry = { loadFee(coroutineScope, feeConstructor, onRetryCancelled) },
                            onCancel = onRetryCancelled
                        )
                    )
                )

                exception?.printStackTrace()

                FeeStatus.Error
            }
        }

        value?.run { feeLiveData.postValue(this) }
    }

    override fun loadFee(
        coroutineScope: CoroutineScope,
        feeConstructor: suspend (Token) -> BigInteger,
        onRetryCancelled: () -> Unit,
    ) {
        coroutineScope.launch {
            loadFeeSuspending(coroutineScope, feeConstructor, onRetryCancelled)
        }
    }

    override fun requireFee(
        block: (BigDecimal) -> Unit,
        onError: (title: String, message: String) -> Unit
    ) {
        val feeStatus = feeLiveData.value

        if (feeStatus is FeeStatus.Loaded) {
            block(feeStatus.feeModel.fee)
        } else {
            onError(
                resourceManager.getString(R.string.fee_not_yet_loaded_title),
                resourceManager.getString(R.string.fee_not_yet_loaded_message)
            )
        }
    }
}