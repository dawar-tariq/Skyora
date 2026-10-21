package com.skyora.weather.ui.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.skyora.weather.R
import com.skyora.weather.core.util.SkyoraLinks
import com.skyora.weather.core.util.openUrl
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraRadius
import com.skyora.weather.ui.theme.SkyoraSpacing

data class DialogLink(val label: String, val url: String)

/**
 * One dialog component for confirmations and legal copy. The body scrolls so
 * long attribution text never clips on small phones or at large font scales.
 */
@Composable
fun SkyoraDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = null,
    destructive: Boolean = false,
    links: List<DialogLink> = emptyList(),
    extraContent: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(SkyoraRadius.card),
        containerColor = SkyoraColors.DeepNavy,
        tonalElevation = 0.dp,
        titleContentColor = SkyoraColors.TextPrimary,
        textContentColor = SkyoraColors.TextSecondary,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = SkyoraColors.TextPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyoraColors.TextSecondary,
                )
                if (extraContent != null) {
                    Spacer(Modifier.height(SkyoraSpacing.md))
                    extraContent()
                }
                if (links.isNotEmpty()) {
                    Spacer(Modifier.height(SkyoraSpacing.md))
                    links.forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable { context.openUrl(link.url) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = link.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyoraColors.CyanSoft,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = SkyoraColors.CyanSoft,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (confirmLabel != null && onConfirm != null) {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = confirmLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (destructive) SkyoraColors.Error else SkyoraColors.Cyan,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissLabel ?: stringResource(R.string.action_close),
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyoraColors.TextSecondary,
                )
            }
        },
    )
}

@Composable
fun SkyoraInfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    links: List<DialogLink> = emptyList(),
    extraContent: (@Composable () -> Unit)? = null,
) {
    SkyoraDialog(
        title = title,
        body = body,
        onDismiss = onDismiss,
        links = links,
        extraContent = extraContent,
    )
}

/** Shared attribution copy for Home, city weather, saved cities and Settings. */
@Composable
fun AttributionDialog(onDismiss: () -> Unit) {
    SkyoraInfoDialog(
        title = stringResource(R.string.attribution_dialog_title),
        body = stringResource(R.string.attribution_dialog_body),
        onDismiss = onDismiss,
        links = listOf(
            DialogLink(stringResource(R.string.link_open_meteo), SkyoraLinks.OPEN_METEO),
            DialogLink(stringResource(R.string.link_cc_by), SkyoraLinks.CC_BY_4),
            DialogLink(stringResource(R.string.link_cc_by_nc), SkyoraLinks.CC_BY_NC_4),
            DialogLink(stringResource(R.string.link_terms), SkyoraLinks.OPEN_METEO_TERMS),
        ),
    )
}

@Composable
fun SkyoraConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    SkyoraDialog(
        title = title,
        body = body,
        onDismiss = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.action_cancel),
        destructive = destructive,
    )
}
