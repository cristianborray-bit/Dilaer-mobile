package com.dilaer.mobile.data

enum class CallStatus { PENDING, CALLED, SKIPPED }

data class Contact(
    val id: Int,
    val name: String,
    val phone: String,
    val status: CallStatus = CallStatus.PENDING,
)
