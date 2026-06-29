package com.smartcourier.core.data.mapper

import com.smartcourier.core.data.local.entity.DeliveryEntity
import com.smartcourier.core.data.local.entity.RouteEntity
import com.smartcourier.core.data.local.entity.UserEntity
import com.smartcourier.core.data.remote.DeliveryDto
import com.smartcourier.core.data.remote.RouteDto
import com.smartcourier.core.data.remote.UserDto
import com.smartcourier.core.domain.model.*

fun UserEntity.toDomain(): User = User(
    uid = uid,
    name = name,
    email = email,
    subscriptionTier = try { UserTier.valueOf(subscriptionTier) } catch (_: Exception) { UserTier.FREE },
    totalEarningsAed = totalEarningsAed,
    syncStatus = syncStatus,
    lastModifiedTimestamp = lastModifiedTimestamp,
    countryCode = countryCode,
    versionClock = versionClock
)

fun User.toEntity(): UserEntity = UserEntity(
    uid = uid,
    name = name,
    email = email,
    subscriptionTier = subscriptionTier.name,
    totalEarningsAed = totalEarningsAed,
    syncStatus = syncStatus,
    lastModifiedTimestamp = lastModifiedTimestamp,
    countryCode = countryCode,
    versionClock = versionClock
)

fun RouteEntity.toDomain(): Route = Route(
    routeId = routeId,
    userId = userId,
    routeStatus = try { RouteStatus.valueOf(routeStatus) } catch (_: Exception) { RouteStatus.CREATED },
    totalDistanceMeters = totalDistanceMeters,
    estimatedDurationSeconds = estimatedDurationSeconds,
    syncStatus = syncStatus,
    lastModifiedTimestamp = lastModifiedTimestamp,
    countryCode = countryCode,
    versionClock = versionClock
)

fun Route.toEntity(): RouteEntity = RouteEntity(
    routeId = routeId,
    userId = userId,
    routeStatus = routeStatus.value,
    totalDistanceMeters = totalDistanceMeters,
    estimatedDurationSeconds = estimatedDurationSeconds,
    syncStatus = syncStatus,
    lastModifiedTimestamp = lastModifiedTimestamp,
    countryCode = countryCode,
    versionClock = versionClock
)

fun DeliveryEntity.toDomain(): Delivery = Delivery(
    id = id,
    routeId = routeId,
    index = index,
    recipientName = recipientName,
    recipientPhone = recipientPhone,
    address = address,
    latitude = latitude,
    longitude = longitude,
    status = status,
    trackingToken = trackingToken,
    earningsAed = earningsAed,
    tipAmountAed = tipAmountAed,
    countryCode = countryCode,
    versionClock = versionClock,
    lastModifiedTimestamp = lastModifiedTimestamp
)

fun Delivery.toEntity(): DeliveryEntity = DeliveryEntity(
    id = id,
    routeId = routeId,
    index = index,
    recipientName = recipientName,
    recipientPhone = recipientPhone,
    address = address,
    latitude = latitude,
    longitude = longitude,
    status = status,
    trackingToken = trackingToken,
    earningsAed = earningsAed,
    tipAmountAed = tipAmountAed,
    syncStatus = SYNC_CLEAN,
    lastModifiedTimestamp = lastModifiedTimestamp,
    countryCode = countryCode,
    versionClock = versionClock
)

fun DeliveryDto.toDomain(): Delivery = Delivery(
    id = id,
    routeId = routeId,
    index = index,
    recipientName = recipientName,
    recipientPhone = recipientPhone,
    address = address,
    latitude = latitude,
    longitude = longitude,
    status = status,
    trackingToken = trackingToken,
    earningsAed = earningsAed,
    tipAmountAed = tipAmountAed,
    countryCode = countryCode,
    versionClock = versionClock
)

fun Delivery.toDto(): DeliveryDto = DeliveryDto(
    id = id,
    routeId = routeId,
    index = index,
    recipientName = recipientName,
    recipientPhone = recipientPhone,
    address = address,
    latitude = latitude,
    longitude = longitude,
    status = status,
    trackingToken = trackingToken,
    earningsAed = earningsAed,
    tipAmountAed = tipAmountAed,
    lastModifiedTimestamp = System.currentTimeMillis(),
    versionClock = versionClock,
    countryCode = countryCode
)

fun RouteDto.toDomain(): Route = Route(
    routeId = routeId,
    userId = userId,
    routeStatus = try { RouteStatus.valueOf(routeStatus) } catch (_: Exception) { RouteStatus.CREATED },
    totalDistanceMeters = totalDistanceMeters,
    estimatedDurationSeconds = estimatedDurationSeconds,
    countryCode = countryCode,
    versionClock = versionClock
)

fun Route.toDto(): RouteDto = RouteDto(
    routeId = routeId,
    userId = userId,
    routeStatus = routeStatus.value,
    totalDistanceMeters = totalDistanceMeters,
    estimatedDurationSeconds = estimatedDurationSeconds,
    lastModifiedTimestamp = lastModifiedTimestamp,
    versionClock = versionClock,
    countryCode = countryCode
)

fun UserDto.toDomain(): User = User(
    uid = uid,
    name = name,
    email = email,
    subscriptionTier = try { UserTier.valueOf(subscriptionTier) } catch (_: Exception) { UserTier.FREE },
    totalEarningsAed = totalEarningsAed,
    countryCode = countryCode,
    versionClock = versionClock
)

fun User.toDto(): UserDto = UserDto(
    uid = uid,
    name = name,
    email = email,
    subscriptionTier = subscriptionTier.name,
    totalEarningsAed = totalEarningsAed,
    lastModifiedTimestamp = lastModifiedTimestamp,
    versionClock = versionClock,
    countryCode = countryCode
)
