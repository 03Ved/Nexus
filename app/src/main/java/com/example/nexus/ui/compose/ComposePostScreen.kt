package com.example.nexus.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nexus.domain.model.IntentType
import com.example.nexus.ui.theme.NexusTypography
import androidx.compose.ui.platform.LocalContext
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.Locale

/**
 * ComposePostScreen
 * Full-screen composable for creating and publishing new posts
 * Includes form validation, image/location picker stubs, and post creation flow
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComposePostScreen(
    onBackClick: () -> Unit = {},
    onPostPublished: () -> Unit = {},
    viewModel: ComposePostViewModel = hiltViewModel()
) {
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedIntentType by viewModel.selectedIntentType.collectAsState()
    val selectedImagePath by viewModel.selectedImagePath.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val expiresInHours by viewModel.expiresInHours.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    val isPostCreated by viewModel.isPostCreated.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val (tagInput, setTagInput) = remember { mutableStateOf("") }

    // Image picker launcher
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val fileName = "post_image_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(context.filesDir, fileName)
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.setImagePath(file.absolutePath)
        }
    }

    // Handle post creation success
    if (isPostCreated) {
        onPostPublished()
        viewModel.resetPostCreated()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compose Post",
                        style = NexusTypography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxWidth()
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Error message if any
                    if (error != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = error ?: "",
                                    style = NexusTypography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Dismiss error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Intent Type Selection
                    Text(
                        text = "What's this post about?",
                        style = NexusTypography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IntentType.entries.forEach { intentType ->
                            IntentTypeSelector(
                                intentType = intentType,
                                isSelected = selectedIntentType == intentType,
                                onClick = { viewModel.setIntentType(intentType) }
                            )
                        }
                    }
                }

                item {
                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.setTitle(it) },
                        label = { Text("Post Title") },
                        placeholder = { Text("e.g., Looking for study partners") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        supportingText = {
                            Text(
                                text = "${title.length}/150",
                                style = NexusTypography.labelSmall
                            )
                        }
                    )
                }

                item {
                    // Description Input
                    OutlinedTextField(
                        value = description,
                        onValueChange = { viewModel.setDescription(it) },
                        label = { Text("Description") },
                        placeholder = { Text("Tell more about your post...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(height = 150.dp, width = 200.dp),
                        maxLines = 6,
                        supportingText = {
                            Text(
                                text = "${description.length}/2000",
                                style = NexusTypography.labelSmall
                            )
                        }
                    )
                }

                item {
                    // Image And Location Selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Image Picker Button
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = "Add image",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = if (selectedImagePath != null) "Image Added" else "Add Image",
                                style = NexusTypography.labelMedium
                            )
                        }

                        // Location Button — auto-captures GPS + reverse geocodes
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val fusedLocationClient = LocationServices
                                            .getFusedLocationProviderClient(context)
                                        fusedLocationClient.lastLocation
                                            .addOnSuccessListener { location ->
                                                if (location != null) {
                                                    scope.launch {
                                                        // Reverse geocode to get address
                                                        val geocoder = Geocoder(context, Locale.getDefault())
                                                        val addresses = try {
                                                            @Suppress("DEPRECATION")
                                                            geocoder.getFromLocation(
                                                                location.latitude,
                                                                location.longitude,
                                                                1
                                                            )
                                                        } catch (e: Exception) {
                                                            null
                                                        }
                                                        val label = if (!addresses.isNullOrEmpty()) {
                                                            val address = addresses[0]
                                                            buildString {
                                                                address.featureName?.let { append(it) }
                                                                address.subLocality?.let {
                                                                    if (isNotEmpty()) append(", ")
                                                                    append(it)
                                                                }
                                                                address.locality?.let {
                                                                    if (isNotEmpty()) append(", ")
                                                                    append(it)
                                                                }
                                                            }.ifEmpty { "Current Location" }
                                                        } else {
                                                            "Current Location"
                                                        }
                                                        viewModel.setLocation(
                                                            label,
                                                            location.latitude,
                                                            location.longitude
                                                        )
                                                    }
                                                } else {
                                                    viewModel.setLocation(
                                                        "Current Location",
                                                        null,
                                                        null
                                                    )
                                                }
                                            }
                                    } catch (e: Exception) {
                                        viewModel.setLocation("Current Location", null, null)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "Add location",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = if (selectedLocation != null) selectedLocation!! else "Location",
                                style = NexusTypography.labelMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Display selected image path (if any)
                    if (selectedImagePath != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Image: ${selectedImagePath?.substringAfterLast("/") ?: "unknown"}",
                                style = NexusTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Display selected location (if any)
                    if (selectedLocation != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Location: $selectedLocation",
                                style = NexusTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    // Tags Input
                    Text(
                        text = "Add Tags (${tags.size}/10)",
                        style = NexusTypography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(8.dp))

                    // Tag Input Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { setTagInput(it.take(20)) },
                            label = { Text("Add tag") },
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            supportingText = {
                                Text(
                                    text = "${tagInput.length}/20",
                                    style = NexusTypography.labelSmall
                                )
                            }
                        )
                        IconButton(
                            onClick = {
                                if (tagInput.isNotBlank()) {
                                    viewModel.addTag(tagInput)
                                    setTagInput("")
                                }
                            },
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .alpha(if (tagInput.isNotBlank() && tags.size < 10) 1f else 0.5f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add tag"
                            )
                        }
                    }

                    // Tag Chips
                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.size(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tags.forEach { tag ->
                                SuggestionChip(
                                    onClick = { viewModel.removeTag(tag) },
                                    label = { Text(tag, style = NexusTypography.labelSmall) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove tag",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Expiration Time Selection
                    Text(
                        text = "Post Expires In",
                        style = NexusTypography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(6, 12, 24, 48, 72).forEach { hours ->
                            FilterChipForCompose(
                                label = when (hours) {
                                    6 -> "6h"
                                    12 -> "12h"
                                    24 -> "1d"
                                    48 -> "2d"
                                    72 -> "3d"
                                    else -> "${hours}h"
                                },
                                isSelected = expiresInHours == hours,
                                onClick = { viewModel.setExpiresInHours(hours) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.discardPost(); onBackClick() },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Text("Discard", style = NexusTypography.labelLarge)
                        }

                        Button(
                            onClick = { viewModel.createPost() },
                            enabled = isFormValid && !isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.5f
                                )
                            )
                        ) {
                            Text("Post", style = NexusTypography.labelLarge)
                        }
                    }

                    // Form validity hint
                    if (!isFormValid) {
                        Text(
                            text = "Title (5+ chars) and description (10+ chars) required",
                            style = NexusTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * IntentTypeSelector
 * Selectable chip for choosing post intent type
 */
@Composable
fun IntentTypeSelector(
    intentType: IntentType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = when (intentType) {
        IntentType.LOOKING_FOR -> Triple(
            Color(0xFF7F77DD),
            Color.White,
            "Looking For"
        )
        IntentType.HAPPENING -> Triple(
            Color(0xFF1D9E75),
            Color.White,
            "Happening"
        )
        IntentType.ASKING -> Triple(
            Color(0xFFEF9F27),
            Color.White,
            "Asking"
        )
        IntentType.OFFERING -> Triple(
            Color(0xFFD85A30),
            Color.White,
            "Offering"
        )
    }

    Box(
        modifier = modifier
            .background(
                color = if (isSelected) backgroundColor else backgroundColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = NexusTypography.labelMedium,
            color = if (isSelected) textColor else backgroundColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * FilterChipForCompose
 * Selectable chip for expiration time
 */
@Composable
fun FilterChipForCompose(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = NexusTypography.labelSmall,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
