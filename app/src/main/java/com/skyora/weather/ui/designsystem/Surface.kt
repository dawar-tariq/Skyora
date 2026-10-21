package com.skyora.weather.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraRadius
import com.skyora.weather.ui.theme.SkyoraSizes
import com.skyora.weather.ui.theme.SkyoraSpacing
import com.skyora.weather.ui.theme.SkyoraTextStyles

/**
 * The translucent card used everywhere in SKYORA. A very subtle top-to-bottom
 * gradient plus a 1dp hairline makes cards feel like glass over the sky.
 */
@Composable
fun SkyoraCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(SkyoraSizes.cardPadding),
    shape: Shape = RoundedCornerShape(SkyoraRadius.card),
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(SkyoraColors.CardTop, SkyoraColors.CardBottom),
                ),
            )
            .border(1.dp, SkyoraColors.Border, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClickLabel = onClickLabel,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun SkyoraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = SkyoraColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Spacer(Modifier.width(SkyoraSpacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = trailing,
            )
        }
    }
}

@Composable
fun SkyoraSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = SkyoraTextStyles.SectionLabel,
        color = SkyoraColors.TextTertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Small status capsule, e.g. "Current location" or "Coming later". */
@Composable
fun SkyoraStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SkyoraColors.Cyan.copy(alpha = 0.12f))
            .border(1.dp, SkyoraColors.Cyan.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = SkyoraColors.CyanSoft,
            maxLines = 1,
        )
    }
}

/** A hairline separator. */
@Composable
fun SkyoraDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SkyoraColors.Border),
    )
}
