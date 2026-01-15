package com.project.analyzer.app

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.analyzer.navigation.api.Root

data class NavItem(
    val key: Root,
    val title: String,
    val icon: ImageVector
)

@Composable
fun Sidebar(
    topIcon: @Composable (() -> Unit)? = null,
    items: List<NavItem>,
    bottomItemsCount: Int = 0,
    selectedKey: Root,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val sidebarBg = Color(0xFF141C24)
    val selectedPill = Color(0xFF24384D)
    val hoverPill = Color(0xFF1B2A3A)
    val selectedText = Color(0xFF4D86FF)
    val normalText = Color(0xFF94A3B8)
    val normalIcon = Color(0xFF9AA7B5)

    Surface(
        modifier = modifier
            .width(88.dp)
            .fillMaxHeight(),
        color = sidebarBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (topIcon != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A2633)),
                    contentAlignment = Alignment.Center
                ) { topIcon() }

                Spacer(Modifier.height(18.dp))
            }

            items.forEachIndexed { index, item ->
                if (index == items.size - bottomItemsCount) {
                    Spacer(Modifier.weight(1f))
                }

                SidebarItem(
                    item = item,
                    selected = item.key == selectedKey,
                    selectedPill = selectedPill,
                    hoverPill = hoverPill,
                    selectedText = selectedText,
                    normalText = normalText,
                    normalIcon = normalIcon,
                    onClick = { onSelect(item) }
                )

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
    selectedText: Color,
    normalText: Color,
    normalIcon: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val pillColor by animateColorAsState(
        targetValue = when {
            selected -> selectedPill
            hovered -> hoverPill
            else -> Color.Transparent
        },
        animationSpec = tween(120)
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) selectedText else normalText,
        animationSpec = tween(120)
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) selectedText else normalIcon,
        animationSpec = tween(120)
    )

    Column(
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .hoverable(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = iconColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
