package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.postprocessors

import io.novafoundation.nova.feature_staking_api.domain.model.ChildIdentity
import io.novafoundation.nova.feature_staking_api.domain.model.Identity
import io.novafoundation.nova.feature_staking_api.domain.model.RootIdentity
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationPostProcessor

private const val MAX_PER_CLUSTER = 2

object RemoveClusteringPostprocessor : RecommendationPostProcessor {

    override fun invoke(original: List<Validator>): List<Validator> {
        val clusterCounter = mutableMapOf<Identity, Int>()

        return original.filter { validator ->
            validator.clusterIdentity()?.let {
                val currentCounter = clusterCounter.getOrDefault(it, 0)

                clusterCounter[it] = currentCounter + 1

                currentCounter < MAX_PER_CLUSTER
            } ?: true
        }
    }

    private fun Validator.clusterIdentity(): Identity? {
        return when (val validatorIdentity = identity) {
            is RootIdentity -> validatorIdentity
            is ChildIdentity -> validatorIdentity.parentIdentity
            else -> null
        }
    }
}
