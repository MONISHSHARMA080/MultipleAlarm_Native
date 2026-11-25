package com.coolApps.MultipleAlarmClock.presentation.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.coolApps.MultipleAlarmClock.R
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.revenuecat.purchases.admob.nextgen.loadAndTrackAd

enum class BannerLoadState { Loading, Loaded, Failed }

@Composable
fun AlarmListBannerAdCard(
	adUnitId: String,
	placement: String,
	modifier: Modifier = Modifier,
) {
	var loadState by remember(adUnitId, placement) { mutableStateOf(BannerLoadState.Loading) }
	if (loadState == BannerLoadState.Failed) return

	val isLoaded = loadState == BannerLoadState.Loaded
	Card(
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 10.dp, vertical = if (isLoaded) 3.dp else 0.dp),
		colors = CardDefaults.cardColors(
			containerColor = if (isLoaded) colorScheme.surfaceContainerLow else Color.Transparent,
			contentColor = colorScheme.onSurfaceVariant,
		),
		shape = RoundedCornerShape(28.dp),
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = if (isLoaded) 10.dp else 0.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			if (isLoaded) {
				Text(
					text = stringResource(R.string.ad_label),
					style = typography.labelSmall,
					fontWeight = FontWeight.Medium,
					color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
					modifier = Modifier
						.align(Alignment.Start)
						.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
				)
			}
			AdMobBanner(
				adUnitId = adUnitId,
				placement = placement,
				onLoadStateChanged = { loadState = it },
			)
		}
	}
}

@Composable
fun AdMobBanner(
	adUnitId: String,
	placement: String,
	modifier: Modifier = Modifier,
	onLoadStateChanged: (BannerLoadState) -> Unit = {},
) {
	var loadState by remember(adUnitId, placement) { mutableStateOf(BannerLoadState.Loading) }
	if (loadState == BannerLoadState.Failed) return

	var loadedHeightPx by remember(adUnitId, placement) { mutableIntStateOf(0) }
	val context = LocalContext.current
	val density = LocalDensity.current
	val latestOnLoadStateChanged by rememberUpdatedState(onLoadStateChanged)
	val bannerHeight = with(density) { loadedHeightPx.toDp() }

	BoxWithConstraints(
		modifier = modifier
			.fillMaxWidth()
			.height(bannerHeight),
		contentAlignment = Alignment.Center,
	) {
		val adWidthDp = maxWidth.value.toInt()
		if (adWidthDp <= 0) return@BoxWithConstraints

		val adSize = remember(adWidthDp) {
			AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidthDp)
		}
		val adView = remember(adUnitId, adSize, placement) { AdView(context) }

		LaunchedEffect(adView) {
			val request = BannerAdRequest.Builder(adUnitId, adSize).build()
			adView.loadAndTrackAd(
				adRequest = request,
				placement = placement,
				loadCallback = object : AdLoadCallback<BannerAd> {
					override fun onAdLoaded(ad: BannerAd) {
						loadedHeightPx = ad.getAdSize().getHeightInPixels(context)
							.takeIf { it > 0 }
							?: adSize.getHeightInPixels(context)
						loadState = BannerLoadState.Loaded
						latestOnLoadStateChanged(BannerLoadState.Loaded)
					}

					override fun onAdFailedToLoad(adError: LoadAdError) {
						loadedHeightPx = 0
						loadState = BannerLoadState.Failed
						latestOnLoadStateChanged(BannerLoadState.Failed)
					}
				},
			)
		}

		DisposableEffect(adView) {
			onDispose { adView.destroy() }
		}

		key(adView) {
			AndroidView(
				factory = { adView },
				modifier = Modifier.fillMaxSize(),
			)
		}
	}
}
