package com.coolApps.MultipleAlarmClock.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Elderly
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R

data class ProfileOption(
    val stringResId: Int,
    val ageRangeStringResId: Int,
    val analyticsString: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserProfileScreen(
    onButtonStateChange: (ButtonState) -> Unit,
    onProfileSelected: (String?) -> Unit
) {
	onButtonStateChange(ButtonState.Enabled)
    val profiles = listOf(
        ProfileOption(R.string.onboarding_profile_school, R.string.onboarding_profile_school_age, "School student", Icons.AutoMirrored.Rounded.MenuBook),
        ProfileOption(R.string.onboarding_profile_college, R.string.onboarding_profile_college_age, "College student", Icons.Rounded.School),
        ProfileOption(R.string.onboarding_profile_work, R.string.onboarding_profile_work_age, "Working professional", Icons.Rounded.Work),
        ProfileOption(R.string.onboarding_profile_parent, R.string.onboarding_profile_parent_age, "Homemaker/Parent", Icons.Rounded.ChildCare),
        ProfileOption(R.string.onboarding_profile_retired, R.string.onboarding_profile_retired_age, "Retired", Icons.Rounded.Elderly),
    )
    var selectedProfile by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedProfile) {
        onProfileSelected(selectedProfile)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(id = R.string.onboarding_profile_title),
            style = typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.onboarding_profile_subtitle),
            style = typography.bodyLarge,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(profiles) { option ->
                val analyticsString = option.analyticsString
                val isSelected = selectedProfile == analyticsString

                val cardContainer by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.secondaryContainer else colorScheme.surface,
                    animationSpec = motionScheme.defaultEffectsSpec(),
                    label = "profileCardContainer"
                )
                val iconContainer by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerHighest,
                    animationSpec = motionScheme.defaultEffectsSpec(),
                    label = "profileIconContainer"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                    animationSpec = motionScheme.defaultEffectsSpec(),
                    label = "profileIconTint"
                )
                val iconBorder by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                    animationSpec = motionScheme.defaultEffectsSpec(),
                    label = "profileIconBorder"
                )

                ProfileCard(
                    option = option,
                    isSelected = isSelected,
                    cardContainer = cardContainer,
                    iconContainer = iconContainer,
                    iconTint = iconTint,
                    iconBorder = iconBorder
                ) {
                    selectedProfile = if (isSelected) null else analyticsString
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileCard(
    option: ProfileOption,
    isSelected: Boolean,
    cardContainer: Color,
    iconContainer: Color,
    iconTint: Color,
    iconBorder: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Icon container morphs from circle to squarish when selected.
    val iconCorner by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 28.dp,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "profileIconCorner"
    )

    // Expressive shape morph: squarish when selected, standard card when not.
    val corner by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 14.dp,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "profileCorner"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "profilePressScale"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            colorScheme.outlineVariant
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, iconBorder, RoundedCornerShape(iconCorner))
                        .background(iconContainer, RoundedCornerShape(iconCorner)),
                )
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.size(14.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(id = option.stringResId),
                    style = typography.titleMedium,
                    color = if (isSelected) colorScheme.onSecondaryContainer else colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.onboarding_profile_ages_format, stringResource(id = option.ageRangeStringResId)),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
            CheckBadge(isSelected)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CheckBadge(isSelected: Boolean) {
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "profileCheckScale"
    )
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        tint = colorScheme.onSecondaryContainer,
        modifier = Modifier.graphicsLayer {
            scaleX = checkScale
            scaleY = checkScale
            alpha = checkScale.coerceIn(0f, 1f)
        }
    )
}
