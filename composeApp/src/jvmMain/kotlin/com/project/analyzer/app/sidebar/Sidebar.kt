package com.project.analyzer.app.sidebar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.composeApp.Res.Res
import com.project.analyzer.composeApp.Res.app_nav_live
import com.project.analyzer.composeApp.Res.app_nav_session
import com.project.analyzer.composeApp.Res.app_nav_settings
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.icons.Live
import com.project.analyzer.ui.icons.Session
import com.project.analyzer.ui.icons.Settings
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
fun Sidebar(
    selectedKey: Root,
    items: ImmutableList<NavItem>,
    modifier: Modifier = Modifier,
    bottomItemsCount: Int = 0,
    showTopIcon: Boolean = false,
    onSelect: (NavItem) -> Unit = {},
    topIcon: @Composable () -> Unit = {},
) {
    val sidebarBg = SimAnalyzerTheme.material.surface
    val selectedPill = SimAnalyzerTheme.material.secondaryContainer
    val hoverPill = SimAnalyzerTheme.material.surfaceVariant
    val selectedColor = SimAnalyzerTheme.material.primary
    val normalText = SimAnalyzerTheme.material.onSurfaceVariant
    val normalIcon = SimAnalyzerTheme.material.onSurfaceVariant
    val boundedBottomItemsCount = bottomItemsCount.coerceIn(0, items.size)
    val mainItems = remember(items, boundedBottomItemsCount) {
        items.dropLast(boundedBottomItemsCount).toImmutableList()
    }
    val trailingItems = remember(items, boundedBottomItemsCount) {
        items.takeLast(boundedBottomItemsCount).toImmutableList()
    }

    Surface(
        modifier = modifier
            .uiTestTag(TestTags.Sidebar)
            .width(88.dp)
            .fillMaxHeight()
            .trackRecompositions(),
        color = sidebarBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showTopIcon) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(SimAnalyzerTheme.corners.card)
                        .background(SimAnalyzerTheme.material.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) { topIcon() }

                Spacer(Modifier.height(18.dp))
            }

            SidebarItemsGroup(
                items = mainItems,
                selectedKey = selectedKey,
                selectedPill = selectedPill,
                hoverPill = hoverPill,
                selectedColor = selectedColor,
                normalText = normalText,
                normalIcon = normalIcon,
                onSelect = onSelect,
            )

            if (trailingItems.isNotEmpty()) {
                Spacer(Modifier.weight(1f))
            }

            SidebarItemsGroup(
                items = trailingItems,
                selectedKey = selectedKey,
                selectedPill = selectedPill,
                hoverPill = hoverPill,
                selectedColor = selectedColor,
                normalText = normalText,
                normalIcon = normalIcon,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun SidebarItemsGroup(
    items: ImmutableList<NavItem>,
    selectedKey: Root,
    selectedPill: Color,
    hoverPill: Color,
    selectedColor: Color,
    normalText: Color,
    normalIcon: Color,
    onSelect: (NavItem) -> Unit,
) {
    items.forEachIndexed { index, item ->
        key(item.key) {
            SidebarItem(
                item = item,
                selected = item.key == selectedKey,
                selectedPill = selectedPill,
                hoverPill = hoverPill,
                selectedColor = selectedColor,
                normalText = normalText,
                normalIcon = normalIcon,
                onClick = { onSelect(item) },
            )

            if (index != items.lastIndex) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SidebarItem(
    item: NavItem,
    selected: Boolean,
    selectedPill: Color,
    hoverPill: Color,
    selectedColor: Color,
    normalText: Color,
    normalIcon: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val pillColor by animateColorAsState(
        targetValue = when {
            selected -> selectedPill
            hovered -> hoverPill
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 120),
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) selectedColor else normalText,
        animationSpec = tween(durationMillis = 120),
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) selectedColor else normalIcon,
        animationSpec = tween(durationMillis = 120),
    )
    val itemScale by animateScale(selected = selected, hovered = hovered)

    Column(
        modifier = Modifier
            .uiTestTag(TestTags.Sidebar.child(item.key))
            .width(56.dp)
            .scale(itemScale)
            .clip(SimAnalyzerTheme.corners.card)
            .hoverable(interaction)
            .clickable(
                enabled = !selected,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null,
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(SimAnalyzerTheme.corners.card)
                .background(pillColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = iconColor,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            color = textColor,
            style = SimAnalyzerTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun animateScale(selected: Boolean, hovered: Boolean): State<Float> {
    val target = when {
        selected -> 1.04f
        hovered -> 1.02f
        else -> 1f
    }
    return androidx.compose.animation.core.animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "sidebarItemScale",
    )
}

@Preview
@Composable
private fun SidebarItemPreview() {
    SimAnalyzerTheme {
        val items = persistentListOf(
            NavItem(Root.Live, stringResource(Res.string.app_nav_live), Icons.Filled.Live),
            NavItem(Root.Session, stringResource(Res.string.app_nav_session), Icons.Filled.Session),
//            NavItem(Root.Setup, "Setup", Icons.Outlined.Build),
            NavItem(Root.Settings, stringResource(Res.string.app_nav_settings), Icons.Filled.Settings),
        )

        Sidebar(
            selectedKey = Root.Live,
            items = items,
        )
    }
}
