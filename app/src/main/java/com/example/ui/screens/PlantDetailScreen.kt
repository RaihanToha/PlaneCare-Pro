package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.I18nManager
import com.example.ui.PlantViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.theme.*
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantId: Int,
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onEditPlant: (Int) -> Unit
) {
    val context = LocalContext.current
    val plantState = viewModel.getPlant(plantId).collectAsStateWithLifecycle(initialValue = null)
    val plant = plantState.value
    val isDark = plant?.let { !MaterialTheme.colorScheme.primary.equals(ForestGreenPrimary) } ?: false

    // Record last accessed timestamp
    LaunchedEffect(plantId) {
        viewModel.updateLastAccessed(plantId)
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("tab_about", "tab_growing", "tab_care", "tab_health")

    if (plant == null) {
        // Simple loading or empty backup view
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val imgBitmap = rememberBase64Image(plant.image)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        androidx.compose.foundation.text.selection.SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
            // TALL SCENIC COVER IMAGE with Overlaid Action buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (imgBitmap != null) {
                    Image(
                        bitmap = imgBitmap,
                        contentDescription = plant.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Modern Gradient Scenic Background with a geometric outline
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        PlantIconPlaceholder(
                            modifier = Modifier.size(140.dp),
                            backgroundColor = Color.Transparent,
                            iconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                        )
                    }
                }
                
                // Overlay Gradient at the bottom to transition beautifully into typography
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                            )
                        )
                )

                // Navigation and Action Floating buttons on top of Scenic Cover
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = I18nManager.getString("back"),
                            tint = Color.White
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share as PDF Button
                        IconButton(
                            onClick = { PdfShareHelper.sharePlantPdf(context, plant) },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .testTag("detail_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Report as PDF",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { onEditPlant(plant.id) },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .testTag("detail_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = I18nManager.getString("edit_plant"),
                                tint = Color.White
                            )
                        }
                    }
                }

                // Plant category floating badge overlaid at cover corner bottom-left
                val categoryTranslationKey = when (plant.category) {
                    "Fruits" -> "cat_fruits"
                    "Vegetables" -> "cat_vegetables"
                    "Flowers" -> "cat_flowers"
                    "Herbs" -> "cat_herbs"
                    else -> plant.category
                }
                
                // Let's color-code each category badge exquisitely!
                val (badgeBg, badgeFg) = when (plant.category) {
                    "Fruits" -> Pair(Color(0xFFFFEDD5), Color(0xFFEA580C))
                    "Vegetables" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
                    "Flowers" -> Pair(Color(0xFFFCE7F3), Color(0xFFDB2777))
                    "Herbs" -> Pair(Color(0xFFD1FAE5), Color(0xFF059669))
                    else -> Pair(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), MaterialTheme.colorScheme.primary)
                }
                
                val finalBg = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else badgeBg
                val finalFg = if (isDark) MaterialTheme.colorScheme.primary else badgeFg

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(finalBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = I18nManager.getString(categoryTranslationKey).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = finalFg,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // PLANT HEADER DETAILS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.headlineLarge, // Slightly tighter modern size for elegant typography
                    color = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF0F172A),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plant.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // HIGH CONTRST TAB BAR WITH UNDERLINE INDICATORS
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    },
                    divider = {
                        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    },
                    modifier = Modifier.testTag("tab_bar")
                ) {
                    tabTitles.forEachIndexed { index, titleKey ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = I18nManager.getString(titleKey),
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ) 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // TAB CONTENT SECTIONS
                AnimatedVisibility(
                    visible = selectedTabIndex == 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // About Tab Description
                        DetailContentCard(
                            title = I18nManager.getString("about"),
                            body = plant.about
                        )
                        
                        // PDFs section below the about section
                        if (plant.pdfs.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "📄 Read Attachment Guides",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF0F172A)
                                )
                                
                                plant.pdfs.forEach { pdf ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                try {
                                                    val file = java.io.File(context.filesDir, pdf.filename)
                                                    if (file.exists()) {
                                                        val authority = "${context.packageName}.fileprovider"
                                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                                                        
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                            setDataAndType(uri, "application/pdf")
                                                            flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                        }
                                                        context.startActivity(intent)
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Attachment file not found", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    android.widget.Toast.makeText(context, "No PDF reader found to open this document", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .testTag("pdf_item_${pdf.filename}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = "📄", fontSize = 18.sp)
                                                }
                                                Column {
                                                    Text(
                                                        text = pdf.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Click to open document in full screen",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowRight,
                                                contentDescription = "Open",
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    // Direct PDF Renderer section underneath the attachment guide card
                                    val pdfBitmaps = remember(pdf.filename) {
                                        val bitmaps = mutableListOf<android.graphics.Bitmap>()
                                        try {
                                            val file = java.io.File(context.filesDir, pdf.filename)
                                            if (file.exists()) {
                                                val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                                                val renderer = android.graphics.pdf.PdfRenderer(pfd)
                                                val pageCount = renderer.pageCount
                                                for (i in 0 until pageCount) {
                                                    val page = renderer.openPage(i)
                                                    val width = 1000
                                                    val height = (width * page.height) / page.width
                                                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                                                    val canvas = android.graphics.Canvas(bitmap)
                                                    canvas.drawColor(android.graphics.Color.WHITE)
                                                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                    bitmaps.add(bitmap)
                                                    page.close()
                                                }
                                                renderer.close()
                                                pfd.close()
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        bitmaps
                                    }

                                    if (pdfBitmaps.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp, bottom = 20.dp, start = 4.dp, end = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "📖 Preview: ${pdf.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            pdfBitmaps.forEachIndexed { pageIdx, bmp ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                                ) {
                                                    androidx.compose.foundation.Image(
                                                        bitmap = bmp.asImageBitmap(),
                                                        contentDescription = "Page ${pageIdx + 1} of ${pdf.name}",
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                                                    )
                                                }
                                                Text(
                                                    text = "Page ${pageIdx + 1} of ${pdfBitmaps.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedTabIndex == 1,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    // Growing Instructions Tab
                    DetailContentCard(
                        title = I18nManager.getString("growing_instructions"),
                        body = plant.growingInstructions
                    )
                }

                AnimatedVisibility(
                    visible = selectedTabIndex == 2,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    // Care Tips Tab
                    DetailContentCard(
                        title = I18nManager.getString("care_tips"),
                        body = plant.careTips
                    )
                }

                AnimatedVisibility(
                    visible = selectedTabIndex == 3,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    // Health Tab (Timeline display of Custom Health Logs + fallback to Diseases/Remedies notes)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Custom Issues Section
                        Text(
                            text = I18nManager.getString("health_issues"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        if (plant.healthIssues.isNotEmpty()) {
                            // Timeline layout
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp)
                            ) {
                                plant.healthIssues.forEachIndexed { i, issue ->
                                    val leafImgBitmap = rememberBase64Image(issue.image)
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Timeline Line and Dot
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(28.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            // Render vertical connecting line unless it's the last item
                                            if (i < plant.healthIssues.size - 1) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.dp)
                                                        .height(90.dp)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Issue Detail Card body
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                // Leaf symptom photo check
                                                if (leafImgBitmap != null) {
                                                    Image(
                                                        bitmap = leafImgBitmap,
                                                        contentDescription = issue.issue,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(90.dp)
                                                            .clip(RoundedCornerShape(10.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                }

                                                Text(
                                                    text = "${I18nManager.getString("issue")}: ${issue.issue}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${I18nManager.getString("remedy")}: ${issue.remedy}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // General notes fallback for disease/remedies if no specific issues entered
                            if (plant.sicknessInfo.isNotEmpty() || plant.remedies.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (plant.sicknessInfo.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = I18nManager.getString("sickness"),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = plant.sicknessInfo,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    if (plant.remedies.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = I18nManager.getString("remedies"),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = plant.remedies,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // No health information recorded at all
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = I18nManager.getString("no_health_info"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(50.dp))
        }
        }
    }
}

@Composable
fun DetailContentCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = body.ifEmpty { "..." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            lineHeight = 22.sp
        )
    }
}
