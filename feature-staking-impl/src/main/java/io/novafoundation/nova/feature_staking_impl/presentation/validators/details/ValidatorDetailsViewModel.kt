package io.novafoundation.nova.feature_staking_impl.presentation.validators.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorDetailsParcelToValidatorDetailsModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorDetailsToErrors
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.NominatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorStakeParcelModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ValidatorDetailsViewModel(
    private val interactor: StakingInteractor,
    private val router: StakingRouter,
    private val validator: ValidatorDetailsParcelModel,
    private val iconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val appLinksProvider: AppLinksProvider,
    private val resourceManager: ResourceManager,
    private val selectedAssetState: SingleAssetSharedState,
) : BaseViewModel(), ExternalActions.Presentation by externalActions {

    private val assetFlow = interactor.currentAssetFlow()
        .share()

    private val maxNominators = flowOf { interactor.maxRewardedNominators() }
        .inBackground()

    val validatorDetails = maxNominators.combine(assetFlow) { maxNominators, asset ->
        val chain = selectedAssetState.chain()

        mapValidatorDetailsParcelToValidatorDetailsModel(chain, validator, asset, maxNominators, iconGenerator, resourceManager)
    }
        .inBackground()
        .share()

    val errorFlow = flowOf { mapValidatorDetailsToErrors(validator) }
        .inBackground()
        .share()

    private val _openEmailEvent = MutableLiveData<Event<String>>()
    val openEmailEvent: LiveData<Event<String>> = _openEmailEvent

    private val _totalStakeEvent = MutableLiveData<Event<ValidatorStakeBottomSheet.Payload>>()
    val totalStakeEvent: LiveData<Event<ValidatorStakeBottomSheet.Payload>> = _totalStakeEvent

    fun backClicked() {
        router.back()
    }

    fun totalStakeClicked() = launch {
        val validatorStake = validator.stake
        val asset = assetFlow.first()
        val payload = calculatePayload(asset, validatorStake)

        _totalStakeEvent.value = Event(payload)
    }

    private suspend fun calculatePayload(asset: Asset, validatorStake: ValidatorStakeParcelModel) = withContext(Dispatchers.Default) {
        require(validatorStake is ValidatorStakeParcelModel.Active)

        val nominatorsStake = validatorStake.nominators.sumByBigInteger(NominatorParcelModel::value)

        ValidatorStakeBottomSheet.Payload(
            own = mapAmountToAmountModel(validatorStake.ownStake, asset),
            nominators = mapAmountToAmountModel(nominatorsStake, asset),
            total = mapAmountToAmountModel(validatorStake.totalStake, asset)
        )
    }

    fun webClicked() {
        validator.identity?.web?.let {
            showBrowser(it)
        }
    }

    fun emailClicked() {
        validator.identity?.email?.let {
            _openEmailEvent.value = Event(it)
        }
    }

    fun twitterClicked() {
        validator.identity?.twitter?.let {
            showBrowser(appLinksProvider.getTwitterAccountUrl(it))
        }
    }

    fun accountActionsClicked() = launch {
        val address = validatorDetails.first().addressModel.address
        val chain = selectedAssetState.chain()

        externalActions.showExternalActions(ExternalActions.Type.Address(address), chain)
    }
}
