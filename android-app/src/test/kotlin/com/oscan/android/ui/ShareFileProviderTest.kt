package com.oscan.android.ui

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShareFileProviderTest {
    @Test
    fun generatedExportsCanBeSharedThroughFileProvider() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val export = File(context.cacheDir, "exports/share-provider-test.pdf").apply {
            parentFile?.mkdirs()
            writeText("test")
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            export
        )

        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.fileprovider", uri.authority)
    }
}
