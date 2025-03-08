package com.myxoz.myxon

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.myxoz.myxon.notification.NotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InlineMediaPlayerComposable(mediaPlayer: MediaPlayer, fullScreen: ()->Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    var isValidSession by remember { mutableStateOf(mediaPlayer.isValidSession()) }
    var canSessionPlay by remember { mutableStateOf(mediaPlayer.session?.metadata!=null) }
    var playbackPosition by remember { mutableLongStateOf(mediaPlayer.getPlaybackPosition()) }
    var duration by remember { mutableLongStateOf(mediaPlayer.duration) }

    var art by remember { mutableStateOf(mediaPlayer.art?.asImageBitmap()) }
    var dominantColor by remember { mutableStateOf(mediaPlayer.dominantColor) }
    var dominantColorFilter by remember { mutableStateOf(mediaPlayer.dominantColorFilter)}
    var title by remember { mutableStateOf(mediaPlayer.title) }
    var artist by remember { mutableStateOf(mediaPlayer.artist) }

    var isPlaying by remember { mutableStateOf(mediaPlayer.isPlaying) }
    var sliderValue by remember { mutableFloatStateOf(mediaPlayer.getPercentageProgress()) }
    var isDragging by remember { mutableStateOf(false) }

    var playerService by remember { mutableStateOf(mediaPlayer.playingAppInfo) }
    LaunchedEffect("registerListeners") {
        mediaPlayer.updateListener.add(object: MediaPlayerUpdates(){
            override fun updatePosition(position: Long) {
                playbackPosition=position
                if(!isDragging){
                    sliderValue=mediaPlayer.getPercentageProgress()
                }
            }
            override fun updateDuration(metaDuration: Long) {
                duration=metaDuration
            }
            override fun updateTitle(metaTitle: String?) {
                title=metaTitle
            }
            override fun updateArtist(metaArtist: String?) {
                artist=metaArtist
            }
            override fun updatePlayingAppInfo(info: PlayingAppInfo) {
                playerService=info
            }
            override fun updateValidSession(b: Boolean) {
                isValidSession=b
            }
            override fun canSessionPlay(b: Boolean) {
                canSessionPlay = b
            }
            override fun updateColors(
                backgroundArt: ImageBitmap?,
                metaDominantColor: Color?,
                metaDominantColorFilter: ColorFilter
            ) {
                art=backgroundArt
                dominantColor=metaDominantColor
                dominantColorFilter=metaDominantColorFilter
            }
            override fun updateIsPlaying(b: Boolean) {
                isPlaying=b
            }
        })
    }
    if(isValidSession)
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Colors.MAIN, RoundedCornerShape(25.dp))
                .clip(RoundedCornerShape(25.dp))
                .combinedClickable(interactionSource, ripple(), onClick = {mediaPlayer.launchPlayer()}, onLongClick = {fullScreen()}),
        ) {
            art?.let {
                Image(
                    bitmap = it,
                    contentDescription = "background art",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(25.dp)),
                    contentScale = ContentScale.Crop,
                    colorFilter = dominantColorFilter
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp, 8.dp, 15.dp, 0.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val density = LocalDensity.current
                val screenWidthPx = LocalConfiguration.current.screenWidthDp.dp.px(density)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        if(playerService?.icon!=null) {
                            playerService?.icon?.let {
                                Icon(it, playerService?.name, Modifier.size(20.dp), tint = Colors.FONT)
                            }
                            Text(playerService?.name?:"", color = Colors.FONT, fontSize = 14.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.previous),
                            "Previous",
                            Modifier
                                .pointerInput("prev"){
                                    detectTapGestures(onPress = {
                                        val modifiedY = 8.dp.px(density)
                                        val modifiedX = screenWidthPx*.9f - 15.dp.px(density) - 20.dp.px(density)*2 - 10.dp.px(density)
                                        val click=PressInteraction.Press(Offset(it.x+modifiedX, it.y+modifiedY))
                                        interactionSource.emit(click)
                                        tryAwaitRelease()
                                        interactionSource.emit(PressInteraction.Release(click))
                                    }){
                                        mediaPlayer.prev()
                                    }
                                }
                                .size(20.dp),
                            tint = dominantColor?:Colors.SECONDARY_FONT
                        )
                        Icon(
                            ImageVector.vectorResource(R.drawable.skip),
                            "Next",
                            Modifier
                                .pointerInput("skip"){
                                    detectTapGestures(onPress = {
                                        val modifiedY = 8.dp.px(density)
                                        val modifiedX = screenWidthPx*.9f - 15.dp.px(density) - 20.dp.px(density)
                                        val click=PressInteraction.Press(Offset(it.x+modifiedX, it.y+modifiedY))
                                        interactionSource.emit(click)
                                        tryAwaitRelease()
                                        interactionSource.emit(PressInteraction.Release(click))
                                    }){
                                        mediaPlayer.skip()
                                    }
                                }
                                .size(20.dp),
                            tint = dominantColor?:Colors.SECONDARY_FONT
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title ?: "Title", color = Colors.FONT, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(artist ?: "Artist", color = Colors.SECONDARY_FONT, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface (
                        Modifier
                            .size(50.dp)
                            .pointerInput("pause") {
                                detectTapGestures(
                                    onPress = {
                                        val modifiedY = 8.dp.px(density) + 20.dp.px(density) + 10.dp.px(density)
                                        val modifiedX = screenWidthPx*.9f - 15.dp.px(density) - 50.dp.px(density)
                                        val click=PressInteraction.Press(Offset(it.x+modifiedX, it.y+modifiedY))
                                        interactionSource.emit(click)
                                        tryAwaitRelease()
                                        interactionSource.emit(PressInteraction.Release(click))
                                    }
                                ){
                                    if(canSessionPlay)
                                        if(isPlaying)
                                            mediaPlayer.pause()
                                        else
                                            mediaPlayer.play()
                                    else
                                        mediaPlayer.launchPlayer()
                                }
                            },
                        color = dominantColor?:Colors.TERTIARY,
                        shape = CircleShape
                    ) {
                        Icon(
                            if(canSessionPlay)
                                if(isPlaying)
                                    ImageVector.vectorResource(R.drawable.pause)
                                else
                                    ImageVector.vectorResource(R.drawable.play)
                            else
                                Icons.AutoMirrored.Filled.ExitToApp
                            ,
                            "Play/Pause",
                            Modifier
                                .padding(10.dp)
                                .size(30.dp),
                            tint = Colors.FONT
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if(isDragging) (sliderValue * duration).toLong() / 1000 else (playbackPosition / 1000)).toMinute(),
                        Modifier.clickable(interactionSource= remember { MutableInteractionSource() }, indication = null) {
                            mediaPlayer.seekTo((mediaPlayer.getPlaybackPosition()-15000).coerceIn(0L, mediaPlayer.duration))
                        },
                        color = Colors.SECONDARY_FONT,
                        fontWeight = FontWeight.W600,
                        fontSize = 15.sp
                    )
                    WavySlider(
                        sliderValue,
                        { newValue ->
                            sliderValue = newValue
                            isDragging = true
                        },
                        {
                            async {
                                delay(1000)
                                isDragging = false
                            }
                            mediaPlayer.seekTo((sliderValue * duration).toLong())
                        },
                        Modifier.weight(1f),
                        isPlaying,
                        dominantColor?:Colors.FONT
                    )
                    Text(
                        (duration / 1000).toMinute(),
                        Modifier.clickable(interactionSource= remember { MutableInteractionSource() }, indication = null) {
                            mediaPlayer.seekTo((mediaPlayer.getPlaybackPosition()+15000).coerceIn(0L, mediaPlayer.duration))
                        },
                        color = Colors.SECONDARY_FONT,
                        fontWeight = FontWeight.W600,
                        fontSize = 15.sp
                    )
                }
            }
        } else Spacer(Modifier.height(50.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenMediaPlayer(mediaPlayer: MediaPlayer, exitFullScreen: ()->Unit) {
    val screenWidthDp = with(LocalConfiguration.current) {this.screenWidthDp.dp}

    var isValidSession by remember { mutableStateOf(mediaPlayer.isValidSession()) }
    var canSessionPlay by remember { mutableStateOf(mediaPlayer.session?.metadata!=null) }
    var playbackPosition by remember { mutableLongStateOf(mediaPlayer.getPlaybackPosition()) }
    var duration by remember { mutableLongStateOf(mediaPlayer.duration) }

    var art by remember { mutableStateOf(mediaPlayer.art?.asImageBitmap()) }
    var dominantColor by remember { mutableStateOf(mediaPlayer.dominantColor) }
    var title by remember { mutableStateOf(mediaPlayer.title) }
    var artist by remember { mutableStateOf(mediaPlayer.artist) }

    var isPlaying by remember { mutableStateOf(mediaPlayer.isPlaying) }
    var sliderValue by remember { mutableFloatStateOf(mediaPlayer.getPercentageProgress()) }
    var isDragging by remember { mutableStateOf(false) }

    var playerService by remember { mutableStateOf(mediaPlayer.playingAppInfo) }
    LaunchedEffect("registerListeners") {
        mediaPlayer.updateListener.add(object: MediaPlayerUpdates(){
            override fun updatePosition(position: Long) {
                playbackPosition=position
                if(!isDragging){
                    sliderValue=mediaPlayer.getPercentageProgress()
                }
            }
            override fun updateDuration(metaDuration: Long) {
                duration=metaDuration
            }
            override fun updateTitle(metaTitle: String?) {
                title=metaTitle
            }
            override fun updateArtist(metaArtist: String?) {
                artist=metaArtist
            }
            override fun updatePlayingAppInfo(info: PlayingAppInfo) {
                playerService=info
            }
            override fun updateValidSession(b: Boolean) {
                isValidSession=b
            }
            override fun canSessionPlay(b: Boolean) {
                canSessionPlay = b
            }
            override fun updateColors(
                backgroundArt: ImageBitmap?,
                metaDominantColor: Color?,
                metaDominantColorFilter: ColorFilter
            ) {
                art=backgroundArt
                dominantColor=metaDominantColor
            }
            override fun updateIsPlaying(b: Boolean) {
                isPlaying=b
            }
        })
    }
    if(isValidSession)
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if(playerService?.icon!=null) {
                        playerService?.icon?.let {
                            Icon(it, playerService?.name, Modifier.size(20.dp), tint = Colors.SECONDARY_FONT)
                        }
                        Text(playerService?.name?:"", color = Colors.SECONDARY_FONT, fontSize = 14.sp)
                    }
                }
                art?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "background art",
                        modifier = Modifier
                            .size((.9*screenWidthDp.value).dp)
                            .clip(RoundedCornerShape(25.dp))
                            .combinedClickable(remember { MutableInteractionSource() }, ripple(), onLongClick = {exitFullScreen()}) { mediaPlayer.launchPlayer() },
                    )
                }
                Row(Modifier.fillMaxWidth(.9f), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if(isDragging) (sliderValue * duration).toLong() / 1000 else (playbackPosition / 1000)).toMinute(),
                        Modifier.clickable(interactionSource= remember { MutableInteractionSource() }, indication = null) {
                            mediaPlayer.seekTo((mediaPlayer.getPlaybackPosition()-15000).coerceIn(0L, mediaPlayer.duration))
                        },
                        color = Colors.SECONDARY_FONT,
                        fontWeight = FontWeight.W600,
                        fontSize = 15.sp
                    )
                    WavySlider(
                        sliderValue,
                        { newValue ->
                            sliderValue = newValue
                            isDragging = true
                        },
                        {
                            async {
                                delay(1000)
                                isDragging = false
                            }
                            mediaPlayer.seekTo((sliderValue * duration).toLong())
                        },
                        Modifier.weight(1f),
                        isPlaying,
                        dominantColor?:Colors.FONT
                    )
                    Text(
                        (duration / 1000).toMinute(),
                        Modifier.clickable(interactionSource= remember { MutableInteractionSource() }, indication = null) {
                            mediaPlayer.seekTo((mediaPlayer.getPlaybackPosition()+15000).coerceIn(0L, mediaPlayer.duration))
                        },
                        color = Colors.SECONDARY_FONT,
                        fontWeight = FontWeight.W600,
                        fontSize = 15.sp
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title ?: "Title", color = Colors.FONT, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist ?: "Artist", color = Colors.SECONDARY_FONT, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.previous),
                        "Previous",
                        Modifier
                            .clickable(remember { MutableInteractionSource() }, ripple()) { mediaPlayer.prev() }
                            .size(30.dp),
                        tint = dominantColor?:Colors.SECONDARY_FONT
                    )
                    Spacer(Modifier.width(30.dp))
                    Surface (
                        Modifier
                            .size(60.dp)
                            .clickable (remember { MutableInteractionSource() }, ripple()){ if(canSessionPlay) if(isPlaying) mediaPlayer.pause() else mediaPlayer.play() else mediaPlayer.launchPlayer() },
                        color = dominantColor?:Colors.TERTIARY,
                        shape = CircleShape
                    ) {
                        Icon(
                            if(canSessionPlay)
                                if(isPlaying)
                                    ImageVector.vectorResource(R.drawable.pause)
                                else
                                    ImageVector.vectorResource(R.drawable.play)
                            else
                                Icons.AutoMirrored.Filled.ExitToApp
                            ,
                            "Play/Pause",
                            Modifier
                                .padding(10.dp)
                                .size(40.dp),
                            tint = Colors.FONT
                        )
                    }
                    Spacer(Modifier.width(30.dp))
                    Icon(
                        ImageVector.vectorResource(R.drawable.skip),
                        "Next",
                        Modifier
                            .clickable (remember { MutableInteractionSource() }, ripple()){ mediaPlayer.skip() }
                            .size(30.dp),
                        tint = dominantColor?:Colors.SECONDARY_FONT
                    )
                }
            }
        }
}

private fun Float.ifNan(replacement: Float): Float {
    return if(isNaN()) replacement else this
}

fun MediaMetadata.getArt(): Bitmap?{
    return getBitmap(MediaMetadata.METADATA_KEY_ART)?:getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)?:getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
}
fun Bitmap.getDominantColor(): Color? {
    return Palette.from(this).generate().run {
        vibrantSwatch?: // +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 -1
        mutedSwatch?: // +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 -1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1 +1
        lightVibrantSwatch?:// +1 +1 +1 +1 +1 +1 +1 -1
        lightMutedSwatch?: // +1 +1
        darkVibrantSwatch?: // +1 -1 +1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 +1 -1 +1 -1
        dominantSwatch?: // -1 -1 -1 -1 -1 -1 -1 -1 -1 +1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 +1
        darkMutedSwatch // -1 -1 -1 -1 -1 -1 +1 -1 -1 -1 -1 +1 -1 +1 -1 -1 +1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1
    }?.run { Color(this.rgb) }
}
fun Long.toMinute(): String{
    return "${this/60}:${if(this%60<10) "0" else ""}${this%60}"
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavySlider(
    value: Float,
    onValueChange: (Float)->Unit,
    onValueChangeFinished: (Float)->Unit,
    modifier: Modifier,
    showAnimation: Boolean,
    sliderColor: Color
)
{
    var isDragging by remember { mutableStateOf(false) }
    val spacing = 12
    val infiniteTransition = rememberInfiniteTransition(label = "WavySlider wave movement")
    val translateX by infiniteTransition.animateFloat(
        0f,
        spacing*2f*PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
            }
        ), label = ""
    )
    Slider(
        value,
        onValueChange = { onValueChange(it); isDragging=true },
        onValueChangeFinished= { isDragging=false; onValueChangeFinished(value) },
        modifier = modifier,
        thumb = {
            Box(
                Modifier
                    .size(20.dp)
                    .background(sliderColor, CircleShape))
        },
        track = {
            val animatedAmplitude by animateFloatAsState(
                if(showAnimation) if(isDragging) 2f else 6f else 0f, label = "", animationSpec = SpringSpec(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            val frontEndSpace = spacing*(PI.toFloat()/2)
            Canvas(modifier = Modifier.fillMaxWidth()){
                val centerY = size.height / 2f
                val endX = size.width * value
                val path = Path()
                for (posX in 0..endX.toInt()){
                    val x = posX.toFloat()
                    val y = sin((x-translateX) / spacing) *
                            animatedAmplitude *
                            (x / (frontEndSpace)).coerceIn(0f, 1f) * // Start flatten
                            (1- ( (x-endX+3*frontEndSpace) / (2*frontEndSpace)).coerceIn(0f, 1f)) // End flatten (unquote the times)
                    path.moveTo(x, y - centerY)
                    path.lineTo(x, y - centerY)
                }
                drawPath(path, sliderColor, style = Stroke(width = 8f, cap = StrokeCap.Round))
                drawLine(Colors.SECONDARY_FONT, Offset(endX, centerY), Offset(size.width, centerY),strokeWidth = 5F, cap = StrokeCap.Round)
            }
        }
    )
}
fun getAppIcon(info: ApplicationInfo, pm: PackageManager): ImageBitmap? {
    try {
        val drawable = info.loadIcon(pm)
        if (drawable is AdaptiveIconDrawable) {
            val foreground: Drawable = drawable.monochrome?.apply {
                setTint(-0x1 /*White*/)
            } ?: drawable.foreground.apply {
                colorFilter = ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
            }
            val size=32; val padding=4
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGBA_1010102)
            foreground.setBounds(-padding,-padding,size+padding,size+padding)
            foreground.draw(android.graphics.Canvas(bitmap))
            return bitmap.asImageBitmap()
        } else if(drawable is BitmapDrawable) {
            return null
        }
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return null
}
class MediaPlayer(private val context: Context){
    private val componentName=ComponentName(context, NotificationListenerService::class.java)
    private val packageManager: PackageManager = context.packageManager
    private val sessionManager = run {
        val notificationAccess = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationAccess.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }
    val updateListener: MutableList<MediaPlayerUpdates> = mutableListOf()
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val mediaCallBack = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            metaData=metadata ?: return updateListener.forEach { it.canSessionPlay(false) }
            if(!isValidSession()) {
                updateListener.forEach {
                    it.canSessionPlay(false)
                }
//                updateListener.forEach { TRY TO CONTINUE WITH SESSION
//                    it.updateValidSession(false)
//                }
                return
            }
            isPlaying=isPlayingCurrently()
            duration=duration()
            title=title()
            artist=artist()
            art = metadata.getArt()
            val backgroundArt = art?.asImageBitmap()
            dominantColor = dominantColor()
            dominantColorFilter = dominantColorFilter()
            updateListener.forEach {
                it.updatePosition(getPlaybackPosition())
                it.updateDuration(duration)
                it.updateIsPlaying(isPlaying)
                it.updateTitle(title)
                it.updateArtist(artist)
                it.updateColors(backgroundArt, dominantColor, dominantColorFilter)
                it.updateValidSession(true)
                it.canSessionPlay(true)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) { // Basically on Pausing and shit
            super.onPlaybackStateChanged(state)
            isPlaying = state?.isActive == true
            updateListener.forEach {
                it.updatePosition(getPlaybackPosition())
                println(state)
            }
            coroutineScope.launch {
                delay(200)
                updateListener.forEach{
                    it.updateIsPlaying(isPlayingCurrently())
                }
            }
        }
    }
    var session = sessionManager.getActiveSessions(componentName).firstOrNull()
        private set(s){
            println("Session set to $s")
            field = s
        }
    var metaData = session?.metadata
        private set
    var art = metaData?.getArt()
        private set
    var dominantColor = dominantColor()
        private set
    var dominantColorFilter = dominantColorFilter()
        private set
    var playingAppInfo: PlayingAppInfo? = null
        private set
    var duration: Long = duration()
        private set
    var isPlaying: Boolean=isPlayingCurrently()
        private set
    var title: String? = title()
        private set
    var artist: String? = artist()
        private set
    private var isPositionAsyncCheckingCancelled = true
    private var isSessionAsyncCheckingCancelled = true
    fun getPlaybackPosition(): Long {
        return session?.playbackState?.position ?: 0L
    }
    fun isValidSession():Boolean{
        return duration()>1L
    }
    private fun duration(): Long {
        return metaData?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 1L
    }
    private fun isPlayingCurrently(): Boolean{
        return session?.playbackState?.isActive == true
    }
    fun getPercentageProgress(): Float{
        return (getPlaybackPosition() / duration.toFloat()).ifNan(0f).coerceIn(0f, 1f)
    }
    private fun title(): String?{
        return metaData?.getString(MediaMetadata.METADATA_KEY_TITLE)
    }
    private fun artist(): String?{
        return metaData?.getString(MediaMetadata.METADATA_KEY_ARTIST)
    }
    private fun dominantColor(): Color?{
        return art?.getDominantColor()
    }
    private fun dominantColorFilter(): ColorFilter{
        return dominantColor.getFilter()
    }
    private fun playingAppInfo(packageName: String): PlayingAppInfo{
        val info = packageManager.getApplicationInfo(packageName, MATCH_UNINSTALLED_PACKAGES)
        return PlayingAppInfo(getAppIcon(info, packageManager),info.loadLabel(packageManager).toString())
    }
    private fun sessionChange(session: MediaController){
        this.session=session
        playingAppInfo=playingAppInfo(session.packageName)
        updateListener.forEach {
            it.updatePlayingAppInfo(playingAppInfo!!)
        }
        mediaCallBack.onMetadataChanged(session.metadata)
    }
    fun launchPlayer() {
        val packageName = session?.packageName ?: return
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(context, "App not installed or unavailable", Toast.LENGTH_SHORT).show()
        }
    }
    fun prev() {
        session?.transportControls?.skipToPrevious()
    }
    fun skip() {
        session?.transportControls?.skipToNext()
    }
    fun play() {
        ifNeededLaunchAsyncPositionChecking()
        ifNeededLaunchAsyncSessionChecking()
        isPlaying = true
        session?.transportControls?.play()
        updateListener.forEach { it.updateIsPlaying(true) }
    }
    fun pause() {
        isPlaying = false
        session?.transportControls?.pause()
        updateListener.forEach { it.updateIsPlaying(false) }
    }
    fun seekTo(position: Long) {
        session?.transportControls?.seekTo(position)
    }
    private fun ifNeededLaunchAsyncPositionChecking(){
        if(!isPositionAsyncCheckingCancelled) return
        println("Launching position checking")
        isPositionAsyncCheckingCancelled=false
        coroutineScope.launch {
            try {
                while (true){
                    if(isPlaying){
                        val position = getPlaybackPosition()
                        updateListener.forEach { it.updatePosition(position) }
                        delay(16)
                    } else {
                        delay(200)
                    }
                }
            } catch (e: Exception){
                println("Position checking cancelled")
                isPositionAsyncCheckingCancelled = true
            }
        }
    }
    private fun ifNeededLaunchAsyncSessionChecking(){
        if(!isSessionAsyncCheckingCancelled) return
        println("Launching Session checking")
        isSessionAsyncCheckingCancelled=false
        coroutineScope.launch {
            try {
                while (true){
                    sessionManager.getActiveSessions(componentName).firstOrNull()?.let {
                        if(!session.sessionEquals(it)) {
                            sessionChange(it)
                        }
                    }
                    delay(1000)
                }
            } catch (e: Exception){
                println("Session checking cancelled")
                isSessionAsyncCheckingCancelled = true
            }
        }
    }
    init {
        coroutineScope.launch {
            session?.packageName?.let {
                playingAppInfo=playingAppInfo(it)
                updateListener.forEach {
                    it.updatePlayingAppInfo(playingAppInfo!!)
                }
            }
        }
        sessionManager.addOnActiveSessionsChangedListener(
            { controllers ->
                session?.unregisterCallback(mediaCallBack)
                controllers?.firstOrNull()?.apply {
                    registerCallback(mediaCallBack)
                    sessionChange(this)
                }
            },
            componentName
        )
        session?.registerCallback(mediaCallBack)
        ifNeededLaunchAsyncPositionChecking()
        ifNeededLaunchAsyncSessionChecking()
    }
}

private fun Color?.getFilter(): ColorFilter {
    return this?.let{ColorFilter.tint(Color.Black.copy(alpha = .5f).compositeOver(it.copy(alpha = .25f)), BlendMode.SrcOver)}?:ColorFilter.tint(Color.Black.copy(alpha = 0.67f), BlendMode.SrcOver)
}

private fun MediaController?.sessionEquals(it: MediaController): Boolean {
    if(this==null) return false
    return this.sessionToken==it.sessionToken
}

abstract class MediaPlayerUpdates{
    abstract fun updatePosition(position: Long)
    abstract fun updateDuration(metaDuration: Long)
    abstract fun updateTitle(metaTitle: String?)
    abstract fun updateArtist(metaArtist: String?)
    abstract fun updatePlayingAppInfo(info: PlayingAppInfo)
    abstract fun updateColors(backgroundArt: ImageBitmap?, metaDominantColor: Color?, metaDominantColorFilter: ColorFilter)
    abstract fun updateIsPlaying(b: Boolean)
    abstract fun updateValidSession(b: Boolean)
    abstract fun canSessionPlay(b: Boolean)

}
class PlayingAppInfo(val icon: ImageBitmap?, val name: String)