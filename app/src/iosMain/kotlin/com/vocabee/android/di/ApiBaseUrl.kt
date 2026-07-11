package com.vocabee.android.di

/**
 * Gateway base URL, split per Apple target:
 *  - simulator shares the host network → localhost
 *  - a physical device reaches the Mac over LAN → the Mac's Wi-Fi IP
 */
internal expect val vocabeeIosBaseUrl: String
