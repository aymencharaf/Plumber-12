package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R

@Composable
fun PlumbingIcon(
    iconType: String,
    modifier: Modifier = Modifier.size(64.dp),
    tintColor: Color = Color(0xFF0284C7), // Plumbing Blue
    backgroundColor: Color = Color(0xFFF0F9FF), // Light Blue Container
    imageUri: String? = null
) {
    val realProductImageModel: Any = when {
        !imageUri.isNullOrEmpty() -> imageUri
        iconType in listOf("pipe") -> R.drawable.img_ppr_pipes_real_1787523378846
        iconType in listOf("pipe_pvc", "elbow_pvc", "elbow_pvc_45", "tee_pvc", "tee_y_pvc", "siphon", "pipe_wc") -> R.drawable.img_pvc_fittings_real_1787523392751
        iconType in listOf("valve_brass", "robinet", "clapet", "te_filete_femelle", "te_filete_male", "coude_filete", "coude_filete_male", "raccord_femelle", "raccord_male", "union_femelle", "union_male", "bouchon_male") -> R.drawable.img_brass_fittings_real_1787523405124
        iconType in listOf("flexible", "siphon_lavabo", "siphon_evier", "bonde", "teflon", "glue", "collier", "filasse") -> R.drawable.img_pex_multilayer_real_1787523416899
        else -> R.drawable.img_ppr_fittings_real_1787523364498
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = realProductImageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
        )
    }
}
