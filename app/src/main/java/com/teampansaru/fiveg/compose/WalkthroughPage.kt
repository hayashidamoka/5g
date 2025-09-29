package com.teampansaru.fiveg.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teampansaru.fiveg.R
import com.teampansaru.fiveg.WalkThroughType

@Composable
fun WalkthroughPage(
    walkThroughType: WalkThroughType,
    modifier: Modifier = Modifier
) {
    val pageData = when (walkThroughType) {
        WalkThroughType.First -> {
            WalkthroughPageData(
                titleRes = R.string.first_fragment_title,
                imageRes = R.drawable.phone_accept,
                descriptionRes = R.string.first_fragment_description,
                descriptionSize = 24.sp,
                imageScale = 1.0f
            )
        }
        WalkThroughType.Second -> {
            WalkthroughPageData(
                titleRes = R.string.second_fragment_title,
                imageRes = R.drawable.add_widget,
                descriptionRes = R.string.second_fragment_description,
                descriptionSize = 20.sp,
                imageScale = 2.0f
            )
        }
        WalkThroughType.Third -> {
            WalkthroughPageData(
                titleRes = R.string.third_fragment_title,
                imageRes = R.drawable.wifi_off,
                descriptionRes = R.string.third_fragment_description,
                descriptionSize = 24.sp,
                imageScale = 1.0f
            )
        }
        WalkThroughType.Fourth -> {
            WalkthroughPageData(
                titleRes = R.string.fourth_fragment_title,
                imageRes = R.drawable.five_g_dance,
                descriptionRes = R.string.fourth_fragment_description,
                descriptionSize = 24.sp,
                imageScale = 2.0f
            )
        }
        WalkThroughType.Fifth -> {
            WalkthroughPageData(
                titleRes = R.string.fifth_fragment_title,
                imageRes = R.drawable.five_g_pose,
                descriptionRes = R.string.fifth_fragment_description,
                descriptionSize = 20.sp,
                imageScale = 2.0f
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.walk_through_1)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = stringResource(id = pageData.titleRes),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 120.dp)
                    .fillMaxWidth()
            )

            // Image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = pageData.imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(pageData.imageScale.coerceAtMost(1f))
                        .padding(horizontal = if (pageData.imageScale > 1f) 0.dp else 32.dp)
                )
            }

            // Description
            Text(
                text = stringResource(id = pageData.descriptionRes),
                fontSize = pageData.descriptionSize,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 100.dp)
                    .fillMaxWidth()
            )
        }
    }
}

private data class WalkthroughPageData(
    val titleRes: Int,
    val imageRes: Int,
    val descriptionRes: Int,
    val descriptionSize: androidx.compose.ui.unit.TextUnit,
    val imageScale: Float
)