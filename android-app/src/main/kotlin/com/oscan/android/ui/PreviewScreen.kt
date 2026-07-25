package com.oscan.android.ui

import com.oscan.android.R

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.oscan.android.ui.theme.OScanTheme
import com.oscan.core.model.FilterType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    croppedBitmap: Bitmap,
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    @Suppress("UNUSED_PARAMETER") onBackToCrop: (() -> Unit)? = null,
    onExportPdfRequested: () -> Unit,
    primaryActionLabel: String? = null
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(OScanTheme.colors.workspace)
    ) {
        val twoPane = maxWidth >= 600.dp
        val image: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                Image(
                    bitmap = croppedBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.preview_processed_page),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        val controls: @Composable (Modifier) -> Unit = { modifier ->
            Surface(tonalElevation = 2.dp, modifier = modifier) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.preview_treatment), style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        FilterType.entries.forEachIndexed { index, filter ->
                            SegmentedButton(
                                selected = selectedFilter == filter,
                                onClick = { onFilterSelected(filter) },
                                shape = SegmentedButtonDefaults.itemShape(index, FilterType.entries.size),
                                label = { Text(filter.displayName()) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onExportPdfRequested) {
                            Text(primaryActionLabel ?: stringResource(R.string.scanner_accept_page))
                        }
                    }
                }
            }
        }

        if (twoPane) {
            Row(Modifier.fillMaxSize()) {
                image(Modifier.weight(1f).fillMaxHeight())
                controls(
                    Modifier.widthIn(min = 300.dp, max = 360.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                image(Modifier.weight(1f).fillMaxWidth())
                controls(Modifier.fillMaxWidth())
            }
        }
    }
}
