package io.novafoundation.nova.feature_account_api.presenatation.account.add

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ImportAccountPayload(
    val type: SecretType,
    val addAccountPayload: AddAccountPayload,
) : Parcelable
