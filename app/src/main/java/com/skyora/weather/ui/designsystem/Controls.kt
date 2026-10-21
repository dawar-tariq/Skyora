package com.skyora.weather.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.skyora.weather.R
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraRadius
import com.skyora.weather.ui.theme.SkyoraSizes
import com.skyora.weather.ui.theme.SkyoraSpacing

@Composable
fun SkyoraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SkyoraSizes.buttonHeight),
        shape = RoundedCornerShape(SkyoraRadius.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = SkyoraColors.Cyan,
            contentColor = SkyoraColors.OnAccent,
            disabledContainerColor = SkyoraColors.Cyan.copy(alpha = 0.25f),
            disabledContentColor = SkyoraColors.OnAccent.copy(alpha = 0.6f),
        ),
        contentPadding = PaddingValues(horizontal = SkyoraSpacing.xl, vertical = SkyoraSpacing.sm),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = SkyoraColors.OnAccent,
                strokeWidth = 2.dp,
            )
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        if (loading || icon != null) {
            Spacer(Modifier.width(10.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SkyoraSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SkyoraSizes.buttonHeight),
        shape = RoundedCornerShape(SkyoraRadius.button),
        border = BorderStroke(1.dp, SkyoraColors.BorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SkyoraColors.Navy.copy(alpha = 0.45f),
            contentColor = SkyoraColors.TextPrimary,
            disabledContentColor = SkyoraColors.TextTertiary,
        ),
        contentPadding = PaddingValues(horizontal = SkyoraSpacing.xl, vertical = SkyoraSpacing.sm),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SkyoraTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = SkyoraColors.CyanSoft,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SkyoraIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SkyoraColors.TextPrimary,
    containerColor: Color = SkyoraColors.Navy.copy(alpha = 0.55f),
    showBorder: Boolean = true,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(SkyoraSizes.iconButton)
            .clip(CircleShape)
            .background(containerColor)
            .then(
                if (showBorder) Modifier.border(1.dp, SkyoraColors.Border, CircleShape) else Modifier,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Two-or-more option toggle. Each segment has a 40dp visual height inside a
 * 4dp padded track, giving a 48dp touch target.
 */
@Composable
fun <T> SkyoraSegmentedControl(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SkyoraColors.Navy.copy(alpha = 0.6f))
            .border(1.dp, SkyoraColors.Border, RoundedCornerShape(50))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) SkyoraColors.Cyan else Color.Transparent,
                label = "segmentBackground",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(background)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelected(option) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) SkyoraColors.OnAccent else SkyoraColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
fun SaveCityChip(
    saved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionLabel = stringResource(if (saved) R.string.city_saved else R.string.city_save)
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                onClickLabel = interactionLabel,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (saved) {
                        SkyoraColors.Cyan.copy(alpha = 0.16f)
                    } else {
                        SkyoraColors.Navy.copy(alpha = 0.5f)
                    },
                )
                .border(
                    1.dp,
                    if (saved) SkyoraColors.Cyan.copy(alpha = 0.4f) else SkyoraColors.Border,
                    RoundedCornerShape(50),
                )
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = if (saved) SkyoraColors.Cyan else SkyoraColors.TextSecondary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(if (saved) R.string.city_saved else R.string.city_save),
                style = MaterialTheme.typography.labelMedium,
                color = if (saved) SkyoraColors.CyanSoft else SkyoraColors.TextSecondary,
            )
        }
    }
}

@Composable
fun SkyoraLoadingRow(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = SkyoraColors.Cyan,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = SkyoraColors.TextSecondary,
        )
    }
}
