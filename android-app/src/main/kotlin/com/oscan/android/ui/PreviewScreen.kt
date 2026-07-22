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
import com.oscan.android.ui.theme.OScanTheme

@OptIn(ExperimentalMaterial3Api::class)
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
            .background(OScanTheme.colors.workspace)
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

        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
            ) {
                FilterType.values().forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = FilterType.values().size),
                        label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
        }

        // Bottom navigation & export row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBackToCrop) {
                Text("Adjust edges")
            }
            Button(onClick = onExportPdfRequested) {
                Text("Accept page")
            }
        }
    }
}
