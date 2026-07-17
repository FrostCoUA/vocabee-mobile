package com.vocabee.android.feature.vocabulary.presentation.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIWindow
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosShareController : ShareController {
    override fun shareInvite(text: String, qrCodePng: ByteArray) {
        dispatch_async(dispatch_get_main_queue()) {
            val windows = UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>()
            val root = (windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull())
                ?.rootViewController ?: return@dispatch_async
            var top = root
            while (top.presentedViewController != null) {
                top = top.presentedViewController!!
            }
            val qrCodeData: NSData = qrCodePng.usePinned { bytes ->
                NSData.create(bytes = bytes.addressOf(0), length = qrCodePng.size.toULong())
            }
            val qrCodeImage = UIImage.imageWithData(qrCodeData)
            val controller = UIActivityViewController(
                activityItems = listOfNotNull(text, qrCodeImage),
                applicationActivities = null,
            )
            top.presentViewController(controller, animated = true, completion = null)
        }
    }
}
