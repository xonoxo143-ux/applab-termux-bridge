package com.applab.termuxbridge.clipboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import com.applab.termuxbridge.storage.SafBridgeFolder

class ClipboardBridge(
    private val context: Context,
    private val folder: SafBridgeFolder
) {
    fun writeClipboardSave(treeUri: Uri?): ClipboardResult {
        if (treeUri == null) return ClipboardResult(false, "Pick bridge folder first.")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return ClipboardResult(false, "Clipboard is empty.")
        val description = clip.description
        val supported = description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
            description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
        if (!supported) return ClipboardResult(false, "Clipboard does not contain text.")
        val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim().orEmpty()
        if (text.length < 8) return ClipboardResult(false, "Clipboard text is too short to look like a save code.")
        val ok = folder.writeText(treeUri, "inbox", "save_clipboard.txt", text)
        return if (ok) {
            ClipboardResult(true, "Clipboard save written. Now run Decode Clipboard Save.")
        } else {
            ClipboardResult(false, "Could not write inbox/save_clipboard.txt.")
        }
    }
}

data class ClipboardResult(val success: Boolean, val message: String)
