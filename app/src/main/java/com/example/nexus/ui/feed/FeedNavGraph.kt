package com.example.nexus.ui.feed

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

const val FEED_ROUTE = "feed"
const val POST_DETAIL_ROUTE = "post_detail"

/**
 * Feed Navigation Graph
 * Defines routes for feed-related screens
 */
fun NavGraphBuilder.feedNavGraph(
    navController: NavHostController
) {
    composable(FEED_ROUTE) {
        FeedScreen(
            onPostClick = { postId ->
                navController.navigate("$POST_DETAIL_ROUTE/$postId")
            },
            onComposeClick = {
                navController.navigate("compose_post")
            }
        )
    }

    composable("$POST_DETAIL_ROUTE/{postId}") { backStackEntry ->
        val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
        PostDetailScreen(
            postId = postId,
            onBackClick = { navController.navigateUp() }
        )
    }
}
