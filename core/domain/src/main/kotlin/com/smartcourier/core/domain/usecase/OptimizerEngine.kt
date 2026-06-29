package com.smartcourier.core.domain.usecase

import com.smartcourier.core.domain.model.Coordinate
import kotlin.math.*

class OptimizerEngine {

    fun compute2Opt(coordinates: List<Coordinate>): List<Coordinate> {
        if (coordinates.size <= 3) return coordinates

        val distanceMatrix = buildDistanceMatrix(coordinates)
        var route = nearestNeighbor(coordinates, distanceMatrix)
        var improved = true

        while (improved) {
            improved = false
            for (i in 1 until route.size - 1) {
                for (k in i + 1 until route.size) {
                    val delta = calculateDelta(route, i, k, distanceMatrix)
                    if (delta < 0) {
                        route = twoOptSwap(route, i, k)
                        improved = true
                    }
                }
            }
        }

        return route.map { coordinates[it] }
    }

    private fun buildDistanceMatrix(coords: List<Coordinate>): Array<DoubleArray> {
        val n = coords.size
        return Array(n) { i ->
            DoubleArray(n) { j ->
                if (i == j) 0.0 else haversine(coords[i], coords[j])
            }
        }
    }

    private fun haversine(a: Coordinate, b: Coordinate): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val sinDLat = sin(dLat / 2)
        val sinDLon = sin(dLon / 2)
        val x = sinDLat.pow(2) + sinDLon.pow(2) * cos(lat1) * cos(lat2)
        return R * 2 * asin(sqrt(x))
    }

    private fun nearestNeighbor(coords: List<Coordinate>, matrix: Array<DoubleArray>): List<Int> {
        val n = coords.size
        val visited = BooleanArray(n)
        visited[0] = true
        val route = mutableListOf(0)
        for (count in 1 until n) {
            var bestNext = -1
            var bestDist = Double.MAX_VALUE
            val current = route.last()
            for (next in 0 until n) {
                if (!visited[next] && matrix[current][next] < bestDist) {
                    bestDist = matrix[current][next]
                    bestNext = next
                }
            }
            visited[bestNext] = true
            route.add(bestNext)
        }
        return route
    }

    private fun calculateDelta(route: List<Int>, i: Int, k: Int, matrix: Array<DoubleArray>): Double {
        val a = route[i - 1]; val b = route[i]; val c = route[k]; val d = if (k + 1 < route.size) route[k + 1] else route[i]
        return matrix[a][b] + matrix[c][d] - (matrix[a][c] + matrix[b][d])
    }

    private fun twoOptSwap(route: List<Int>, i: Int, k: Int): MutableList<Int> {
        val newRoute = route.toMutableList()
        val segment = newRoute.subList(i, k + 1).reversed()
        for (idx in segment.indices) newRoute[i + idx] = segment[idx]
        return newRoute
    }

    fun computeTotalDistance(coordinates: List<Coordinate>, indices: List<Int>): Double {
        var total = 0.0
        for (i in 0 until indices.size - 1) {
            total += haversine(coordinates[indices[i]], coordinates[indices[i + 1]])
        }
        return total
    }
}
