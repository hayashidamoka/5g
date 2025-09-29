package com.teampansaru.fiveg.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.*
import com.teampansaru.fiveg.R
import com.teampansaru.fiveg.WalkThroughType
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun WalkthroughScreen(
    onPermissionRequest: () -> Unit = {}
) {
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.walk_through_1))
    ) {
        // ページャー
        HorizontalPager(
            count = WalkThroughType.entries.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            WalkthroughPage(
                walkThroughType = WalkThroughType.entries[page],
                modifier = Modifier.fillMaxSize()
            )
        }

        // インジケーター（下部）
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(WalkThroughType.entries.size) { index ->
                PageIndicator(
                    isActive = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PageIndicator(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(
                color = if (isActive) {
                    colorResource(id = android.R.color.white)
                } else {
                    colorResource(id = android.R.color.darker_gray)
                },
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .clickable(onClick = onClick)
    )
}