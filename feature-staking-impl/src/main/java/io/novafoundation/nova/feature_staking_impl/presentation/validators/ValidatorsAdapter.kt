package io.novafoundation.nova.feature_staking_impl.presentation.validators

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_validator.view.itemValidationCheck
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorActionIcon
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorIcon
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorInfo
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorName
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorScoringPrimary
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorScoringSecondary

class ValidatorsAdapter(
    private val itemHandler: ItemHandler,
    initialMode: Mode = Mode.VIEW
) : ListAdapter<ValidatorModel, ValidatorViewHolder>(ValidatorDiffCallback) {

    private var mode = initialMode

    interface ItemHandler {

        fun validatorInfoClicked(validatorModel: ValidatorModel)

        fun validatorClicked(validatorModel: ValidatorModel) {
            // default empty
        }

        fun removeClicked(validatorModel: ValidatorModel) {
            // default empty
        }
    }

    enum class Mode {
        VIEW, EDIT
    }

    fun modeChanged(newMode: Mode) {
        mode = newMode

        notifyItemRangeChanged(0, itemCount, mode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValidatorViewHolder {
        val view = parent.inflateChild(R.layout.item_validator)

        return ValidatorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ValidatorViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item, itemHandler, mode)
    }

    override fun onBindViewHolder(holder: ValidatorViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(
            holder, position, payloads,
            onUnknownPayload = { holder.bindIcon(mode, item, itemHandler) },
            onDiffCheck = {
                when (it) {
                    ValidatorModel::isChecked -> holder.bindIcon(mode, item, itemHandler)
                    ValidatorModel::scoring -> holder.bindScoring(item)
                }
            }
        )
    }
}

class ValidatorViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(
        validator: ValidatorModel,
        itemHandler: ValidatorsAdapter.ItemHandler,
        mode: ValidatorsAdapter.Mode
    ) = with(containerView) {
        itemValidatorName.text = validator.title
        itemValidatorIcon.setImageDrawable(validator.image)

        itemValidatorInfo.setOnClickListener {
            itemHandler.validatorInfoClicked(validator)
        }

        setOnClickListener {
            itemHandler.validatorClicked(validator)
        }

        bindIcon(mode, validator, itemHandler)

        bindScoring(validator)
    }

    fun bindIcon(
        mode: ValidatorsAdapter.Mode,
        validatorModel: ValidatorModel,
        handler: ValidatorsAdapter.ItemHandler
    ) = with(containerView) {
        when {
            mode == ValidatorsAdapter.Mode.EDIT -> {
                itemValidatorActionIcon.makeVisible()
                itemValidationCheck.makeGone()

                itemValidatorActionIcon.setOnClickListener { handler.removeClicked(validatorModel) }
            }
            validatorModel.isChecked == null -> {
                itemValidatorActionIcon.makeGone()
                itemValidationCheck.makeGone()
            }
            else -> {
                itemValidatorActionIcon.makeGone()
                itemValidationCheck.makeVisible()

                itemValidationCheck.isChecked = validatorModel.isChecked
            }
        }
    }

    fun bindScoring(validatorModel: ValidatorModel) = with(containerView) {
        when (val scoring = validatorModel.scoring) {
            null -> {
                itemValidatorScoringPrimary.makeGone()
                itemValidatorScoringSecondary.makeGone()
            }

            is ValidatorModel.Scoring.OneField -> {
                itemValidatorScoringPrimary.setTextColorRes(R.color.white_64)
                itemValidatorScoringPrimary.makeVisible()
                itemValidatorScoringSecondary.makeGone()
                itemValidatorScoringPrimary.text = scoring.field
            }

            is ValidatorModel.Scoring.TwoFields -> {
                itemValidatorScoringPrimary.setTextColorRes(R.color.white)
                itemValidatorScoringPrimary.makeVisible()
                itemValidatorScoringSecondary.makeVisible()
                itemValidatorScoringPrimary.text = scoring.primary
                itemValidatorScoringSecondary.text = scoring.secondary
            }
        }
    }
}

object ValidatorDiffCallback : DiffUtil.ItemCallback<ValidatorModel>() {

    override fun areItemsTheSame(oldItem: ValidatorModel, newItem: ValidatorModel): Boolean {
        return oldItem.address == newItem.address
    }

    override fun areContentsTheSame(oldItem: ValidatorModel, newItem: ValidatorModel): Boolean {
        return oldItem.scoring == newItem.scoring && oldItem.title == newItem.title && oldItem.isChecked == newItem.isChecked
    }

    override fun getChangePayload(oldItem: ValidatorModel, newItem: ValidatorModel): Any? {
        return ValidatorPayloadGenerator.diff(oldItem, newItem)
    }
}

private object ValidatorPayloadGenerator : PayloadGenerator<ValidatorModel>(
    ValidatorModel::isChecked, ValidatorModel::scoring
)
