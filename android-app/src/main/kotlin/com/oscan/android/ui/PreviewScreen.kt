package com.oscan.android.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.oscan.core.model.FilterType

@Composable
fun PreviewScreen(
    croppedBitmap: Bitmap,
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    onBackToCrop: () -> Unit,
    onExportPdfRequested: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = croppedBitmap.asImageBitmap(),
                contentDescription = "Cropped Preview",
                modifier = Modifier.fillMaxSize()
            )
        }

        // Filter selection segment
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterType.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Bottom navigation & export row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBackToCrop) {
                Text("Back to Crop")
            }
            Button(onClick = onExportPdfRequested) {
                Text("Export PDF")
            }
        }
    }
}
