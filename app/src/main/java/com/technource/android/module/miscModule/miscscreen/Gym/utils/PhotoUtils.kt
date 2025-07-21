package com.technource.android.module.miscModule.miscscreen.Gym.utils

import android.net.Uri
import androidx.fragment.app.Fragment

object PhotoUtils {
    fun takePicture(fragment: Fragment, onPhotoTaken: (photoUri: String) -> Unit) {
        // TODO: Implement camera/gallery logic
        // For now, just call with a dummy URI
        onPhotoTaken("dummy_photo_uri")
    }
}