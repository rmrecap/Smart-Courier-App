package com.smartcourier.core.domain.usecase

import com.smartcourier.core.domain.model.Coordinate
import com.smartcourier.core.domain.model.Delivery
import java.util.UUID

class ParseAddressesUseCase {

    companion object {
        private val ADDRESS_REGEX = Regex(
            """([A-Za-z0-9\s,.-]+(?:Street|St|Road|Rd|Avenue|Ave|Lane|Ln|Boulevard|Blvd|Drive|Dr|Way|Square|Sq|Building|Bldg|Block|Area|Zone|Near|Opposite)[A-Za-z0-9\s,.-]*)""",
            RegexOption.IGNORE_CASE
        )
    }

    fun invoke(rawText: String, routeId: String): List<ParsedAddress> {
        val lines = rawText.lines().filter { it.isNotBlank() }
        val seen = mutableSetOf<String>()
        val results = mutableListOf<ParsedAddress>()
        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            val address = extractAddress(trimmed) ?: continue
            if (seen.add(address.lowercase())) {
                results.add(ParsedAddress(index = index, addressRaw = address, deliveryId = UUID.randomUUID().toString(), routeId = routeId))
            }
        }
        return results
    }

    private fun extractAddress(line: String): String? {
        val match = ADDRESS_REGEX.find(line)
        return match?.value?.trim()
    }

    fun estimateCoordinates(address: String): Coordinate? {
        val parts = address.split(",").map { it.trim() }
        if (parts.size >= 2) {
            val lat = parts[parts.size - 2].toDoubleOrNull()
            val lng = parts[parts.size - 1].toDoubleOrNull()
            if (lat != null && lng != null) return Coordinate(lat, lng)
        }
        return null
    }

    fun toDelivery(parsed: ParsedAddress, sequenceOrder: Int, coordinate: Coordinate, countryCode: String = "ae"): Delivery {
        return Delivery(
            id = parsed.deliveryId,
            routeId = parsed.routeId,
            index = sequenceOrder,
            address = parsed.addressRaw,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude,
            trackingToken = UUID.randomUUID().toString(),
            countryCode = countryCode
        )
    }

    data class ParsedAddress(
        val index: Int,
        val addressRaw: String,
        val deliveryId: String,
        val routeId: String
    )
}
