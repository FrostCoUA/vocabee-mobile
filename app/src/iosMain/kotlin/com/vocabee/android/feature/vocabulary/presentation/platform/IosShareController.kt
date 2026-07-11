package com.vocabee.android.feature.vocabulary.presentation.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

class IosShareController : ShareController {
    override fun shareText(text: String) {
        dispatch_async(dispatch_get_main_queue()) {
            val windows = UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>()
            val root = (windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull())
                ?.rootViewController ?: return@dispatch_async
            var top = root
            while (top.presentedViewController != null) {
                top = top.presentedViewController!!
            }
            val controller = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null,
            )
            top.presentViewController(controller, animated = true, completion = null)
        }
    }
}
