package com.flashsphere.privatednsqs.datastore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DnsProvider(
    val name: String,
    val hostname: String
) : Parcelable
