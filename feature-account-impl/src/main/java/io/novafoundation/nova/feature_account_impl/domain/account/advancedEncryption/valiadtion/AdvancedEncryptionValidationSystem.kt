package io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem

typealias AdvancedEncryptionValidationSystem = ValidationSystem<AdvancedEncryptionValidationPayload, AdvancedEncryptionValidationFailure>
typealias AdvancedEncryptionValidation = Validation<AdvancedEncryptionValidationPayload, AdvancedEncryptionValidationFailure>
