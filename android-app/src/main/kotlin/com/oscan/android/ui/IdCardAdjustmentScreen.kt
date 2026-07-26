package com.oscan.android.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oscan.android.R
import com.oscan.core.model.ImageDimensions
import org.opencv.core.Point

@Composable
fun IdCardAdjustmentScreen(
    state: ScannerUiState.IdCardAdjust,
    onCornerMoved: (String, Int, Point, ImageDimensions) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val side: @Composable (
            Modifier,
            String,
            android.graphics.Bitmap,
            com.oscan.android.data.session.SessionPage,
            com.oscan.core.model.CornerPoints,
            Boolean
        ) -> Unit = { modifier, label, preview, page, corners, valid ->
            Column(modifier) {
                Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                CropScreen(
                    previewBitmap = preview,
                    sourceDimensions = ImageDimensions(page.sourceWidth, page.sourceHeight),
                    corners = corners,
                    isValidGeometry = valid,
                    onCornerMoved = { handle, point, dimensions ->
                        onCornerMoved(page.id, handle, point, dimensions)
                    }
                )
            }
        }

        if (maxWidth >= 700.dp) {
            Row(Modifier.fillMaxSize()) {
                side(
                    Modifier.weight(1f),
                    stringResource(R.string.id_card_front),
                    state.frontPreview,
                    state.frontPage,
                    state.frontCorners,
                    state.isFrontValid
                )
                side(
                    Modifier.weight(1f),
                    stringResource(R.string.id_card_back),
                    state.backPreview,
                    state.backPage,
                    state.backCorners,
                    state.isBackValid
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                side(
                    Modifier.weight(1f),
                    stringResource(R.string.id_card_front),
                    state.frontPreview,
                    state.frontPage,
                    state.frontCorners,
                    state.isFrontValid
                )
                side(
                    Modifier.weight(1f),
                    stringResource(R.string.id_card_back),
                    state.backPreview,
                    state.backPage,
                    state.backCorners,
                    state.isBackValid
                )
            }
        }
    }
}
