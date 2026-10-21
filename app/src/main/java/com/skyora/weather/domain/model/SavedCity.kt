package com.skyora.weather.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedCity(
    val location: Location,
    val savedAtEpochMillis: Long,
)

enum class MoveDirection { UP, DOWN }
