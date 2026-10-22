package com.skyora.weather.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.skyora.weather.R
import com.skyora.weather.domain.widget.DeviceVendor

/**
 * The manufacturer-specific path to the screen that limits background work.
 *
 * Shared by onboarding and the Widget reliability page so the two can never
 * disagree about what a Xiaomi, Huawei, Oppo, Vivo or Samsung user should look
 * for. The wording names a menu, never a verdict about the device.
 */
@Composable
fun vendorGuidanceText(vendor: DeviceVendor): String = when (vendor) {
    DeviceVendor.XIAOMI -> stringResource(R.string.reliability_help_xiaomi)
    DeviceVendor.HUAWEI -> stringResource(R.string.reliability_help_huawei)
    DeviceVendor.OPPO -> stringResource(R.string.reliability_help_oppo)
    DeviceVendor.VIVO -> stringResource(R.string.reliability_help_vivo)
    DeviceVendor.SAMSUNG -> stringResource(R.string.reliability_help_samsung)
    DeviceVendor.OTHER -> stringResource(R.string.reliability_help_generic)
}
