package com.oscan.android.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ExportSuccessScreen(
    pdfUri: Uri,
    onShareClicked: () -> Unit,
    onScanAnotherClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "PDF Exported Successfully",
            tint = Color(0xFF00E676),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PDF Created Successfully!",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = pdfUri.lastPathSegment ?: pdfUri.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))


        Button(
            onClick = onShareClicked,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share PDF")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onScanAnotherClicked,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            Text("Scan Another Document")
        }
    }
}
