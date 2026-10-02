package com.nuvio.app.features.artwork

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingItem

@Composable fun rememberArtworkRevision(): Long {
    val profile by ProfileRepository.uiState.collectAsState()
    val revision by ArtworkRepository.store.revision.collectAsState()
    return revision + (profile.activeProfile?.profileIndex ?: ProfileRepository.activeProfileId).toLong()
}

fun MetaDetails.withArtwork(screen: ArtworkScreen = ArtworkScreen.DETAIL): MetaDetails = copy(
    poster = ArtworkRepository.store.resolve(type, id, screen, ArtworkKind.POSTER, poster),
    background = ArtworkRepository.store.resolve(type, id, screen, ArtworkKind.BACKGROUND, background),
    logo = ArtworkRepository.store.resolve(type, id, screen, ArtworkKind.LOGO, logo),
)
fun MetaPreview.withArtwork(screen: ArtworkScreen = ArtworkScreen.HOME): MetaPreview = copy(
    poster = ArtworkRepository.store.resolve(type, id, screen, ArtworkKind.POSTER, poster),
    banner = ArtworkRepository.store.resolve(type, id, screen, ArtworkKind.BACKGROUND, banner),
    logo = ArtworkRepository.store.resolve(type, id, screen, ArtworkKind.LOGO, logo),
)
fun ContinueWatchingItem.withArtwork(): ContinueWatchingItem = copy(
    poster = ArtworkRepository.store.resolve(parentMetaType, parentMetaId, ArtworkScreen.CONTINUE_WATCHING, ArtworkKind.POSTER, poster),
    background = ArtworkRepository.store.resolve(parentMetaType, parentMetaId, ArtworkScreen.CONTINUE_WATCHING, ArtworkKind.BACKGROUND, background),
    logo = ArtworkRepository.store.resolve(parentMetaType, parentMetaId, ArtworkScreen.CONTINUE_WATCHING, ArtworkKind.LOGO, logo),
)

@Composable
fun ArtworkAsyncImage(
    model: Any?, contentDescription: String?, modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: Painter? = null, error: Painter? = null, fallback: Painter? = error,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    var failed by remember(model) { mutableStateOf(false) }
    val original = (model as? String)?.let { ArtworkFallbacks.original(it) }
    AsyncImage(
        model = if (failed && original != null) original else model,
        contentDescription = contentDescription, modifier = modifier, contentScale = contentScale,
        placeholder = placeholder, error = error, fallback = fallback,
        onSuccess = onSuccess,
        onError = { state -> if (!failed && original != null) failed = true else onError?.invoke(state) },
    )
}
