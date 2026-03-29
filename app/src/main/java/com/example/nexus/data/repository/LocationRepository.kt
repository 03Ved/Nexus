package com.example.nexus.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import android.Manifest

/**
 * LocationRepository
 * Manages device location updates and provides current location via Flow
 * Uses FusedLocationProviderClient for efficient location retrieval
 */
@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {

    /**
     * Get current device location as a Flow
     * Emits Location objects as location updates are received
     * 
     * Caution: Ensure location permissions (FINE_LOCATION) are granted before collecting
     * Returns empty flow if permissions are not granted
     */
    fun currentLocation(): Flow<Location> = callbackFlow {
        // Check if location permissions are granted
        if (!hasLocationPermissions()) {
            close()
            return@callbackFlow
        }

        // Create location request with high accuracy and 5-second updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(2000)  // Throttle minimum updates to 2 seconds
            .build()

        // Callback for receiving location updates
        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                if (location != null) {
                    // Emit the location to the flow
                    trySend(location)
                }
            }
        }

        // Request location updates
        try {
            @SuppressLint("MissingPermission")  // Permissions checked above
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null  // Use default looper
            )
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }

        // Cleanup: remove location updates when flow is closed
        awaitClose {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Get the current location as a one-time request
     * Emits single location value and completes
     */
    fun getLastKnownLocation(): Flow<Location?> = callbackFlow {
        if (!hasLocationPermissions()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        try {
            @SuppressLint("MissingPermission")  // Permissions checked above
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                trySend(location)
                close()
            }.addOnFailureListener { exception ->
                close(exception)
            }
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose()
    }

    /**
     * Request a single current location update with timeout
     * More efficient than continuous updates when only one location is needed
     */
    fun getCurrentLocation(): Flow<Location> = callbackFlow {
        if (!hasLocationPermissions()) {
            close()
            return@callbackFlow
        }

        try {
            @SuppressLint("MissingPermission")  // Permissions checked above
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationProviderClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    trySend(location)
                }
                close()
            }.addOnFailureListener { exception ->
                close(exception)
            }
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose()
    }

    /**
     * Calculate distance between two locations in meters
     */
    fun calculateDistance(location1: Location, location2: Location): Float {
        return location1.distanceTo(location2)
    }

    /**
     * Check if a location is within a certain radius (in meters)
     */
    fun isLocationWithinRadius(
        targetLocation: Location,
        centerLocation: Location,
        radiusInMeters: Float
    ): Boolean {
        val distance = calculateDistance(targetLocation, centerLocation)
        return distance <= radiusInMeters
    }

    /**
     * Filter locations by distance from center point
     */
    fun filterLocationsByDistance(
        locations: List<Location>,
        centerLocation: Location,
        radiusInMeters: Float
    ): List<Location> {
        return locations.filter { location ->
            isLocationWithinRadius(location, centerLocation, radiusInMeters)
        }
    }

    /**
     * Check if location permissions are granted
     * Returns true only if both FINE_LOCATION and COARSE_LOCATION are granted
     */
    @Suppress("NewApi")  // Runtime check is performed
    private fun hasLocationPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return fineLocationGranted && coarseLocationGranted
    }

    companion object {
        private const val TAG = "LocationRepository"
    }
}
