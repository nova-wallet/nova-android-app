package jp.co.soramitsu.feature_staking_impl.domain.validations.setup

import jp.co.soramitsu.feature_wallet_api.domain.validation.EnoughToPayFeesValidation

typealias SetupStakingFeeValidation = EnoughToPayFeesValidation<SetupStakingPayload, SetupStakingValidationFailure>