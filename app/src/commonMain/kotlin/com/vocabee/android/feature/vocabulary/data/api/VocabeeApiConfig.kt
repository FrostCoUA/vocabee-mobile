package com.vocabee.android.feature.vocabulary.data.api

/**
 * Tells the API client which gateway base URL to use, e.g.
 *   - emulator → http://10.0.2.2:3000
 *   - device on LAN → http://192.168.x.x:3000
 *   - production → https://api.vocabee.app
 */
data class VocabeeApiConfig(
    val baseUrl: String,
)
