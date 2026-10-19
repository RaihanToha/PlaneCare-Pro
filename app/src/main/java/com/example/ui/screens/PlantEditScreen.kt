package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HealthIssue
import com.example.i18n.I18nManager
import com.example.ui.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantEditScreen(
    plantId: Int, // -1 means adding new plant
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onComplete: (Int) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = plantId != -1

    // State placeholders for input values
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Fruits") }
    var summary by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf("") }
    var growingInstructions by remember { mutableStateOf("") }
    var careTips by remember { mutableStateOf("") }
    var sicknessInfo by remember { mutableStateOf("") }
    var remedies by remember { mutableStateOf("") }
    
    // In-memory list to accumulate custom health issues prior to saving
    val localHealthIssues = remember { mutableStateListOf<HealthIssue>() }
    
    // In-memory list to accumulate attached PDFs
    val localPdfs = remember { mutableStateListOf<com.example.data.PlantPdf>() }

    // State fields for the current nested issue being drafted
    var draftingIssueName by remember { mutableStateOf("") }
    var draftingRemedyDesc by remember { mutableStateOf("") }
    var draftingIssueImageBase64 by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Load initial data if in edit mode
    val plantState = if (isEditMode) {
        viewModel.getPlant(plantId).collectAsStateWithLifecycle(initialValue = null)
    } else {
        null
    }

    LaunchedEffect(Unit) {
        if (!isEditMode && viewModel.prefilledPlantName != null) {
            name = viewModel.prefilledPlantName ?: ""
            viewModel.prefilledPlantName = null
        }
    }

    LaunchedEffect(plantState?.value) {
        plantState?.value?.let { p ->
            name = p.name
            category = p.category
            summary = p.summary
            about = p.about
            imageBase64 = p.image
            growingInstructions = p.growingInstructions
            careTips = p.careTips
            sicknessInfo = p.sicknessInfo
            remedies = p.remedies
            localHealthIssues.clear()
            localHealthIssues.addAll(p.healthIssues)
            localPdfs.clear()
            localPdfs.addAll(p.pdfs)
        }
    }

    // Launchers for picking images from Android gallery
    val plantImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.processImageUriAsync(context, it) { base64 ->
                if (base64 != null) {
                    imageBase64 = base64
                } else {
                    Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val issueImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.processImageUriAsync(context, it) { base64 ->
                if (base64 != null) {
                    draftingIssueImageBase64 = base64
                } else {
                    Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Picker for multiple PDF files
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            try {
                // Query display name
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                var originalName = "Document.pdf"
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            originalName = it.getString(nameIndex)
                        }
                    }
                }
                
                // Copy stream to filesDir
                val uniqueDiskFilename = "pdf_${System.currentTimeMillis()}_${(1000..9999).random()}.pdf"
                val destFile = java.io.File(context.filesDir, uniqueDiskFilename)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Append records
                localPdfs.add(com.example.data.PlantPdf(name = originalName, filename = uniqueDiskFilename))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error reading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val categories = listOf("Fruits", "Vegetables", "Flowers", "Herbs")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = I18nManager.getString(if (isEditMode) "edit_plant" else "add_plant"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("edit_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = I18nManager.getString("back")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // 1. DRAG-CLICKABLE FILE INPUT AREA
            val mainImageBitmap = rememberBase64Image(imageBase64)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { plantImagePickerLauncher.launch("image/*") }
                    .testTag("image_picker_area"),
                contentAlignment = Alignment.Center
            ) {
                if (mainImageBitmap != null) {
                    Image(
                        bitmap = mainImageBitmap,
                        contentDescription = "Selected Plant Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Dark semi-transparent indicator overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = I18nManager.getString("photo_attached").uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    // Dashed outline styled box for uploading
                    val dashStrokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = dashStrokeColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        PlantIconPlaceholder(
                            modifier = Modifier.size(54.dp),
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = I18nManager.getString("upload_photo"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 2. INPUT: PLANT NAME
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(I18nManager.getString("plant_name")) },
                placeholder = { Text("e.g., Roma Tomato") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_plant_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 3. INPUT: CATEGORIS PILL SELECTION
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = I18nManager.getString("category"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("category_edit_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (cat in categories) {
                        val isSelected = category.equals(cat, ignoreCase = true)
                        val catKey = when (cat) {
                            "Fruits" -> "cat_fruits"
                            "Vegetables" -> "cat_vegetables"
                            "Flowers" -> "cat_flowers"
                            "Herbs" -> "cat_herbs"
                            else -> cat
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { category = cat }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = I18nManager.getString(catKey),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. INPUT: SUMMARY
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text(I18nManager.getString("summary")) },
                placeholder = { Text("e.g., Quick summary of plant behavior.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_summary"),
                shape = RoundedCornerShape(12.dp),
                minLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 5. INPUT: ABOUT
            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text(I18nManager.getString("about")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_about"),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 6. INPUT: GROWING
            OutlinedTextField(
                value = growingInstructions,
                onValueChange = { growingInstructions = it },
                label = { Text(I18nManager.getString("growing_instructions")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_instructions"),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 7. INPUT: CARE
            OutlinedTextField(
                value = careTips,
                onValueChange = { careTips = it },
                label = { Text(I18nManager.getString("care_tips")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_care"),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 8. INPUT: DESTRUCTIVE FALLBACK SICKNESS NOTES
            OutlinedTextField(
                value = sicknessInfo,
                onValueChange = { sicknessInfo = it },
                label = { Text(I18nManager.getString("sickness")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_sickness"),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            OutlinedTextField(
                value = remedies,
                onValueChange = { remedies = it },
                label = { Text(I18nManager.getString("remedies")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_remedies"),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))

            // 9. DYNAMIC NESTED "ISSUE & REMEDY" APPDENDER
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = I18nManager.getString("health_issues"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // List of already added issues locally
                if (localHealthIssues.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = I18nManager.getString("custom_issues").uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        localHealthIssues.forEachIndexed { idx, issue ->
                            val leafBmp = rememberBase64Image(issue.image)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (leafBmp != null) {
                                        Image(
                                            bitmap = leafBmp,
                                            contentDescription = "Symptom",
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = issue.issue,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = issue.remedy,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            maxLines = 1
                                        )
                                    }
                                    
                                    IconButton(onClick = { localHealthIssues.removeAt(idx) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove issue",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Append-nested sub form card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = draftingIssueName,
                            onValueChange = { draftingIssueName = it },
                            placeholder = { Text(I18nManager.getString("issue_name")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("draft_issue_input"),
                            minLines = 1,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        OutlinedTextField(
                            value = draftingRemedyDesc,
                            onValueChange = { draftingRemedyDesc = it },
                            placeholder = { Text(I18nManager.getString("remedy_desc")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("draft_remedy_input"),
                            minLines = 1,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Attach Issue image button
                        val leafBmpDraft = rememberBase64Image(draftingIssueImageBase64)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { issueImagePickerLauncher.launch("image/*") }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add image",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = I18nManager.getString("add_leaf_photo"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (leafBmpDraft != null) {
                                Image(
                                    bitmap = leafBmpDraft,
                                    contentDescription = "Draft Issue thumbnail",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Append row button
                        Button(
                            onClick = {
                                if (draftingIssueName.isNotBlank() && draftingRemedyDesc.isNotBlank()) {
                                    localHealthIssues.add(
                                        HealthIssue(
                                            issue = draftingIssueName.trim(),
                                            remedy = draftingRemedyDesc.trim(),
                                            image = if (draftingIssueImageBase64.isNotBlank()) draftingIssueImageBase64 else null
                                        )
                                    )
                                    // Clear drafting fields
                                    draftingIssueName = ""
                                    draftingRemedyDesc = ""
                                    draftingIssueImageBase64 = ""
                                } else {
                                    Toast.makeText(context, "Fill out draft name & remedy!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("append_issue_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = I18nManager.getString("add_issue"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))

            // 9b. MULTIPLE PDF FILES ATTACHMENT
            Column(
                modifier = Modifier.fillMaxWidth().testTag("pdf_attachments_section"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Attached Documents (PDFs)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (localPdfs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        localPdfs.forEachIndexed { idx, pdf ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📄",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pdf.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Internal: ${pdf.filename}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            try {
                                                java.io.File(context.filesDir, pdf.filename).delete()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            localPdfs.removeAt(idx)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete PDF",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add PDF Button
                Button(
                    onClick = {
                        try {
                            pdfPickerLauncher.launch("application/pdf")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error opening file picker: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add PDF",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attach PDF Documents",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 10. SAVE OR UPDATE PRIMARY BUTTON
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        Toast.makeText(context, I18nManager.getString("validation_error"), Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.savePlant(
                            id = if (isEditMode) plantId else 0,
                            name = name.trim(),
                            category = category,
                            summary = summary.trim().ifEmpty { "..." },
                            about = about.trim().ifEmpty { "..." },
                            image = imageBase64,
                            growingInstructions = growingInstructions.trim().ifEmpty { "..." },
                            careTips = careTips.trim().ifEmpty { "..." },
                            sicknessInfo = sicknessInfo.trim(),
                            remedies = remedies.trim(),
                            healthIssues = localHealthIssues.toList(),
                            pdfs = localPdfs.toList(),
                            onComplete = { savedId ->
                                val successMsg = if (isEditMode) "Plant updated!" else "Plant added successfully!"
                                Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                                onComplete(savedId)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = I18nManager.getString(if (isEditMode) "update" else "save"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 11. RED DESTRUCTIVE DELETE BUTTON IF EDITING
            if (isEditMode) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("delete_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = I18nManager.getString("delete"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Confirmation delete alert popup
    if (showDeleteConfirmDialog && plantState?.value != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = I18nManager.getString("delete"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = I18nManager.getString("confirm_delete"))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deletePlant(plantState.value!!) {
                            Toast.makeText(context, "Plant deleted successfully", Toast.LENGTH_SHORT).show()
                            onComplete(-1) // Signal deletion backstack jump
                        }
                    }
                ) {
                    Text(
                        text = I18nManager.getString("delete"),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}
