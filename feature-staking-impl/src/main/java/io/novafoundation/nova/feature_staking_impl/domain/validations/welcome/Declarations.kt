package io.novafoundation.nova.feature_staking_impl.domain.validations.welcome

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.MaxNominatorsReachedValidation

typealias WelcomeStakingValidationSystem = ValidationSystem<WelcomeStakingValidationPayload, WelcomeStakingValidationFailure>

typealias WelcomeStakingMaxNominatorsValidation = MaxNominatorsReachedValidation<WelcomeStakingValidationPayload, WelcomeStakingValidationFailure>
