package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.novafoundation.nova.core.model.Node

@Entity(tableName = "users")
data class AccountLocal(
    @PrimaryKey val address: String,
    val username: String,
    val publicKey: String,
    val cryptoType: Int,
    val position: Int,
    val networkType: Node.NetworkType
)
