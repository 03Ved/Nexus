package com.example.nexus.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nexus.domain.model.IntentType
import com.example.nexus.domain.model.Post
import com.example.nexus.ui.feed.components.IntentTypeBadge
import com.example.nexus.ui.feed.components.PostCard
import com.example.nexus.ui.theme.IntentAskingColor
import com.example.nexus.ui.theme.IntentHappeningColor
import com.example.nexus.ui.theme.IntentLookingForColor
import com.example.nexus.ui.theme.IntentOfferingColor
import com.example.nexus.ui.theme.NexusTypography

/**
 * FeedScreen
 * Main feed screen displaying posts with filtering, pull-to-refresh, and FAB for composing posts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit = {},
    onComposeClick: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel()
) {
    val feedState by viewModel.feedState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nexus Feed",
                        style = NexusTypography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onComposeClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Compose post",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (feedState) {
                is FeedUiState.Idle -> {
                    Text(
                        text = "Loading...",
                        modifier = Modifier.align(Alignment.Center),
                        style = NexusTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                is FeedUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is FeedUiState.Success -> {
                    val posts = (feedState as FeedUiState.Success).posts

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Filter Chips
                        FilterChipsRow(
                            selectedFilter = selectedFilter,
                            onFilterChange = { filterType ->
                                viewModel.setFilter(
                                    if (selectedFilter == filterType) null else filterType
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        // Posts List
                        if (posts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No posts here yet.\nBe the first to post!",
                                    style = NexusTypography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(
                                    items = posts,
                                    key = { post -> post.id }
                                ) { post ->
                                    PostCard(
                                        post = post,
                                        currentUserId = currentUserId,
                                        onLikeClick = { postId ->
                                            viewModel.markPostInterested(postId)
                                        },
                                        onCommentClick = {
                                            // Comments feature coming soon
                                        },
                                        onCardClick = {
                                            onPostClick(post.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                is FeedUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (feedState as FeedUiState.Error).message,
                            style = NexusTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Refresh button
            if (feedState !is FeedUiState.Loading) {
                Button(
                    onClick = { viewModel.refreshPosts() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("Refresh")
                }
            }
        }
    }
}

/**
 * FilterChipsRow
 * Horizontal scrollable row of filter chips for intent types
 */
@Composable
fun FilterChipsRow(
    selectedFilter: IntentType?,
    onFilterChange: (IntentType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IntentType.entries.forEach { intentType ->
            FilterChip(
                label = intentType.name.replace("_", " "),
                isSelected = selectedFilter == intentType,
                onClick = { onFilterChange(intentType) },
                intentType = intentType
            )
        }
    }
}

/**
 * FilterChip
 * Individual filter chip for intent type
 */
@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    intentType: IntentType,
    modifier: Modifier = Modifier
) {
    val (selectedBgColor, selectedTextColor, unselectedBgColor, unselectedTextColor) = when (intentType) {
        IntentType.LOOKING_FOR -> {
            Quadruple(
                IntentLookingForColor,
                androidx.compose.ui.graphics.Color.White,
                IntentLookingForColor.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.onSurface
            )
        }
        IntentType.HAPPENING -> {
            Quadruple(
                IntentHappeningColor,
                androidx.compose.ui.graphics.Color.White,
                IntentHappeningColor.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.onSurface
            )
        }
        IntentType.ASKING -> {
            Quadruple(
                IntentAskingColor,
                androidx.compose.ui.graphics.Color.White,
                IntentAskingColor.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.onSurface
            )
        }
        IntentType.OFFERING -> {
            Quadruple(
                IntentOfferingColor,
                androidx.compose.ui.graphics.Color.White,
                IntentOfferingColor.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.onSurface
            )
        }
    }

    Box(
        modifier = modifier
            .background(
                color = if (isSelected) selectedBgColor else unselectedBgColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = NexusTypography.labelSmall,
            color = if (isSelected) selectedTextColor else unselectedTextColor,
            modifier = Modifier
                .padding(vertical = 4.dp)
        )
    }
}

data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
