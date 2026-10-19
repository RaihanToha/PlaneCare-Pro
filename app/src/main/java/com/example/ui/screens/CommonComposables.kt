package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.I18nManager
import com.example.i18n.Language
import com.example.ui.theme.ForestGreenPrimary

@Composable
fun rememberBase64Image(base64Str: String?): ImageBitmap? {
    var imageBitmap by remember(base64Str) { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(base64Str) {
        if (base64Str.isNullOrEmpty()) {
            imageBitmap = null
            return@LaunchedEffect
        }
        
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cleanStr = if (base64Str.startsWith("data:image")) {
                    base64Str.substringAfter("base64,")
                } else {
                    base64Str
                }
                val decodedBytes = Base64.decode(cleanStr, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                
                // Switch back to Main for asImageBitmap, even though it's quick
                // actually asImageBitmap() does some internal caching but is fine here
                val ib = bitmap?.asImageBitmap()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    imageBitmap = ib
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    imageBitmap = null
                }
            }
        }
    }
    
    return imageBitmap
}

/**
 * PlantIconPlaceholder
 * A beautiful, organic, dynamically drawn leaf canvas used as a modern, premium branding placeholder.
 */
@Composable
fun PlantIconPlaceholder(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize(0.55f)) {
            val width = size.width
            val height = size.height
            
            // Draw stylized symmetrical leaf path
            val path = Path().apply {
                moveTo(width / 2f, height)
                cubicTo(
                    0f, height * 0.65f,
                    0f, height * 0.15f,
                    width / 2f, 0f
                )
                cubicTo(
                    width, height * 0.15f,
                    width, height * 0.65f,
                    width / 2f, height
                )
                close()
            }
            drawPath(path = path, color = iconColor)
            
            // Draw secondary venation midrib
            drawLine(
                color = backgroundColor,
                start = Offset(width / 2f, height),
                end = Offset(width / 2f, height * 0.15f),
                strokeWidth = width * 0.08f
            )
        }
    }
}

/**
 * MockShell
 * An elegant container that simulates a phone shell width (max 480.dp) when run on broad tablet/desktop screens.
 * Wraps content with generous spacing, subtle borders, and background color matching the design scheme.
 */
@Composable
fun MockShell(
    modifier: Modifier = Modifier,
    statusBarColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable BoxScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    val isExpanded = screenWidth > 600.dp
    val shellWidth = if (isExpanded) 450.dp else Dp.Unspecified
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                else MaterialTheme.colorScheme.background
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .let { if (isExpanded) it.width(shellWidth) else it.fillMaxWidth() }
                .fillMaxHeight()
                .shadow(
                    elevation = if (isExpanded) 16.dp else 0.dp,
                    shape = if (isExpanded) RoundedCornerShape(24.dp) else RoundedCornerShape(0.dp)
                )
                .clip(if (isExpanded) RoundedCornerShape(24.dp) else RoundedCornerShape(0.dp))
                .border(
                    width = if (isExpanded) 1.dp else 0.dp,
                    color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(24.dp)
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}

/**
 * ThemeModeToggle
 * Sleek, tactile theme selector pill that animates between light and dark modes.
 */
@Composable
fun ThemeModeToggle(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    onToggle: () -> Unit
) {
    val isDark = !MaterialTheme.colorScheme.primary.equals(ForestGreenPrimary)
    
    val containerBg = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    } else {
        Color(0x1F64748B) // bg-slate-200/50 style
    }
    
    val activeBg = if (isDark) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White
    }
    
    val activeTextColor = if (isDark) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        Color(0xFF047857) // text-emerald-700 style
    }
    
    val inactiveTextColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    } else {
        Color(0xFF64748B) // text-slate-500 style
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerBg)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Light selector
        val isLightSelected = !isDarkMode
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (isLightSelected) activeBg else Color.Transparent)
                .then(
                    if (isLightSelected && !isDark) {
                        Modifier.shadow(elevation = 1.dp, shape = RoundedCornerShape(50))
                    } else Modifier
                )
                .clickable { if (isDarkMode) onToggle() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "☀️",
                style = MaterialTheme.typography.labelMedium,
                color = if (isLightSelected) activeTextColor else inactiveTextColor,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Dark selector
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (isDarkMode) activeBg else Color.Transparent)
                .then(
                    if (isDarkMode && !isDark) {
                        Modifier.shadow(elevation = 1.dp, shape = RoundedCornerShape(50))
                    } else Modifier
                )
                .clickable { if (isLightSelected) onToggle() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌙",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDarkMode) activeTextColor else inactiveTextColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
