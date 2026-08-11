package com.pecmi.studio.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.editor.EditorBottomTab
import com.pecmi.studio.ui.language.LocalAppStrings

@Composable
fun BottomNavigationBar(
    activeTab: EditorBottomTab,
    onTabSelected: (EditorBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bottom_navigation_bar"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 12.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Presets (Cut Corner Badge - Vibrant Purple/Violet)
            CustomNavItem(
                isSelected = activeTab == EditorBottomTab.BACKGROUND_EFFECTS,
                onClick = { onTabSelected(EditorBottomTab.BACKGROUND_EFFECTS) },
                icon = Icons.Default.Diamond,
                label = strings.tabPresets,
                activeColor = Color(0xFF8B5CF6),
                shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
                testTag = "tab_presets"
            )

            // Tab 2: Text (Circle / Rounded Capsule - Electric Cyan)
            CustomNavItem(
                isSelected = activeTab == EditorBottomTab.TEXT,
                onClick = { onTabSelected(EditorBottomTab.TEXT) },
                icon = Icons.Default.Style,
                label = strings.tabText,
                activeColor = Color(0xFF06B6D4),
                shape = CircleShape,
                testTag = "tab_text"
            )

            // Tab 3: Shapes (Diamond / Octagon Cut - Warm Amber)
            CustomNavItem(
                isSelected = activeTab == EditorBottomTab.SHAPES,
                onClick = { onTabSelected(EditorBottomTab.SHAPES) },
                icon = Icons.Default.Category,
                label = strings.tabShapes,
                activeColor = Color(0xFFF59E0B),
                shape = CutCornerShape(10.dp),
                testTag = "tab_shapes"
            )

            // Tab 4: Background / Layers (Asymmetric Soft Leaf - Emerald Green)
            CustomNavItem(
                isSelected = activeTab == EditorBottomTab.IMAGES,
                onClick = { onTabSelected(EditorBottomTab.IMAGES) },
                icon = Icons.Default.Bolt,
                label = strings.tabBackground,
                activeColor = Color(0xFF10B981),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomStart = 16.dp, topStart = 6.dp, bottomEnd = 6.dp),
                testTag = "tab_background"
            )

            // Tab 5: Effects (Futuristic Magic Wand - Crimson Rose)
            CustomNavItem(
                isSelected = activeTab == EditorBottomTab.DRAW,
                onClick = { onTabSelected(EditorBottomTab.DRAW) },
                icon = Icons.Default.AutoAwesome,
                label = strings.tabEffects,
                activeColor = Color(0xFFEC4899),
                shape = RoundedCornerShape(16.dp),
                testTag = "tab_effects"
            )
        }
    }
}

@Composable
private fun CustomNavItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    activeColor: Color,
    shape: Shape,
    testTag: String
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "tab_scale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.22f) else Color.Transparent,
        label = "tab_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else Color.Transparent,
        label = "tab_border"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        label = "tab_content_color"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(shape)
            .background(bgColor)
            .border(width = if (isSelected) 2.dp else 0.dp, color = borderColor, shape = shape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}




