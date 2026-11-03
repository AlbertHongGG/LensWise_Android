package com.lenswise.app.core.camera

/**
 * 相機原始擷取成果值物件。
 *
 * 封裝拍照二進位位元組與拍攝當下 PreviewView 的實際視口寬高比 (width / height)。
 */
data class RawCameraCapture(
    val jpegBytes: ByteArray,
    val viewportAspectRatio: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawCameraCapture

        if (!jpegBytes.contentEquals(other.jpegBytes)) return false
        if (viewportAspectRatio != other.viewportAspectRatio) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jpegBytes.contentHashCode()
        result = 31 * result + viewportAspectRatio.hashCode()
        return result
    }
}
