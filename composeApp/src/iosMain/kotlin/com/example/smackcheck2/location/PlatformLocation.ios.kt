package com.example.smackcheck2.location

/**
 * iOS implementation: placeholder for CLLocationManager integration.
 * Currently sets an error state — will be implemented when iOS support is added.
 */
actual fun requestCurrentLocationDetection() {
    SharedLocationState.setError("Location detection not yet implemented on iOS. Please select a city manually.")
}
