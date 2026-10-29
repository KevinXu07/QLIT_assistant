package com.schedule.qlit.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSURL
import platform.Foundation.stringWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
private class PickerDelegate(
    private val onText: (String?) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        onText(url?.let { readUrlText(it) })
        activeDelegate = null
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onText(null)
        activeDelegate = null
    }

    private fun readUrlText(url: NSURL): String? {
        val accessed = url.startAccessingSecurityScopedResource()
        return try {
            NSString.stringWithContentsOfURL(url, encoding = NSUTF8StringEncoding, error = null)
        } finally {
            if (accessed) url.stopAccessingSecurityScopedResource()
        }
    }
}

/** picker.delegate 是弱引用，picker 展示期间强引用保住 delegate。 */
private var activeDelegate: PickerDelegate? = null

/**
 * iOS 文件选择：UIDocumentPickerViewController。
 */
@Suppress("DEPRECATION")
@Composable
internal actual fun rememberTextFilePicker(onText: (String?) -> Unit): () -> Unit {
    val currentOnText = rememberUpdatedState(onText)
    return {
        val delegate = PickerDelegate { currentOnText.value(it) }
        activeDelegate = delegate
        val picker = UIDocumentPickerViewController(
            documentTypes = listOf("public.json", "public.text", "public.data"),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeOpen,
        )
        picker.delegate = delegate
        val presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
        presenter?.presentViewController(picker, animated = true, completion = null)
            ?: run {
                activeDelegate = null
                currentOnText.value(null)
            }
    }
}

internal actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? =
    org.jetbrains.skia.Image.makeFromEncoded(bytes)?.toComposeImageBitmap()
