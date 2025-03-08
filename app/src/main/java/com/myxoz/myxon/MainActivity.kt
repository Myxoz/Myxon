package com.myxoz.myxon

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myxoz.myxon.notification.NotificationHub
import com.myxoz.myxon.search.Search
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


//            val packageName = intent.getStringExtra("packageName")
//
//            val title = intent.getStringExtra("title")
//            println("MainActivity Received package name: $packageName")
//            println("MainActivity Received notification title: $title")

class MainActivity : ComponentActivity() {
    private lateinit var notificationHub: NotificationHub
    private lateinit var widgetIsland: WidgetIsland
    private lateinit var time: Time
    private lateinit var appDrawer: AppDrawer
    private lateinit var mPrefs: SharedPreferences
    private lateinit var mediaPlayer: MediaPlayer
    private val appDrawerSubscription = Subscription<Boolean, String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mediaPlayer= MediaPlayer(applicationContext)
        mPrefs = getSharedPreferences(localClassName, MODE_PRIVATE)
        notificationHub = NotificationHub(mPrefs)
        widgetIsland = WidgetIsland(applicationContext, mPrefs)
        time=Time()
        appDrawer= AppDrawer(mPrefs, applicationContext)

        registerReceiver(notificationHub.notificationReceiver, IntentFilter(NotificationHub.INTENTNAME), RECEIVER_EXPORTED)
        registerReceiver(this.widgetIsland.batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(this.widgetIsland.batteryReceiver, IntentFilter(Intent.ACTION_POWER_CONNECTED))
        registerReceiver(this.widgetIsland.batteryReceiver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
        notificationHub.context=applicationContext
        println("NotificationHub registered")
        setContent {
            val density = LocalDensity.current
            val screenHeight = with(LocalConfiguration.current){this.screenHeightDp.dp}
            var isFullScreenMusicPlayer by remember { mutableStateOf(false) }
            Scaffold(
                modifier = Modifier.fillMaxHeight(),
                containerColor = Color.Transparent,
            ) { innerPadding ->
                if(!isFullScreenMusicPlayer) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            Modifier
                                .padding(top = innerPadding.calculateTopPadding())
                                .fillMaxWidth(.9f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        )
                        {
                            time.Compose(density) {
                                launchIntentSafely(
                                    applicationContext,
                                    "android.intent.action.MAIN",
                                    "com.sec.android.daemonapp",
                                    FLAG_ACTIVITY_NEW_TASK,
                                    "com.sec.android.daemonapp",
                                    "com.sec.android.daemonapp.app.MainActivity"
                                )
                            }
//                        Spacer(modifier = Modifier.height(100.dp))
                            InlineMediaPlayerComposable(mediaPlayer){isFullScreenMusicPlayer=true}
                            notificationHub.NotificationHubComposable()
                            widgetIsland.Component(appDrawer)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(13.dp)
                            )
//                            GoogleSearch(context = applicationContext)
                        }
                    }
                    DraggableMenu(screenHeight.px(density) -
                            (
                                    50.dp.px(density) +
                                            ((
                                                    screenHeight.px(density) -
                                                            (
                                                                    75.sp.px(density) + // Time
                                                                            (
                                                                                    100 + // Spacer between Time and NotificationHub
                                                                                            60 * 3 + // NotificationHub... Elements
                                                                                            20 * 2 + // NotificationHub Padding
                                                                                            1 * 2 + // Spacer
                                                                                            10 * 4 + // VerticalArrangement
                                                                                            10*2+ // PaddingTop&bot
                                                                                            60 + // WidgetIsland size
                                                                                            20*2+ // WidgetIslandPadding
                                                                                            50 // SearchHeight
                                                                                    ).dp.px(
                                                                                    density
                                                                                )
                                                                    )
                                            ) / 6).apply { Toast.makeText(applicationContext, (this/density.density).toString(), Toast.LENGTH_SHORT).show() } // Elementcount +1
                                    ),
                        innerPadding.calculateTopPadding().px(density),
                        {
                            Modifier
                                .background(
                                    Color(0xFF181818).copy(alpha = it),
                                    RoundedCornerShape(40.dp, 40.dp)
                                )
                                .fillMaxWidth(.9f)
                                .fillMaxHeight()
                                .padding(
                                    30.dp,
                                    10.dp,
                                    30.dp,
                                    innerPadding.calculateBottomPadding() + 20.dp
                                )
                        }, {
                            Spacer(Modifier.height(20.dp))
                            Box(it) {
                                Search(applicationContext, appDrawerSubscription, appDrawer)
                            }
                        },
                        { progess ->
                            AppDrawerComposable(appDrawer, Modifier.alpha(progess)) { }
                        }, appDrawerSubscription
                    )
                } else {
                    FullScreenMediaPlayer(mediaPlayer) { isFullScreenMusicPlayer=false }
                }
            }
//            AnimatedVisibility(visible = isAppDrawerVisible, enter = fadeIn() , exit = fadeOut()) {
//            if(isAppDrawerVisible){
//                AppDrawerComposable(appDrawer) { isAppDrawerVisible=false }
//            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationHub.notificationReceiver)
        unregisterReceiver(widgetIsland.batteryReceiver)
    }
    override fun onResume() {
        super.onResume()
        time.reSync()
    }
}
class SharedPrefsKeys {
    companion object{
        const val PACKAGEOPENED="timesOpenedByPackage"
        const val HIDDENAPPS="hiddenApps"
        const val SNAPCHAT="snapchatDrawables"
        const val TELEGRAM="telegramMessage"
        const val WHATSAPP="whatsAppMessage"
        const val OPTIMALWATERLEVEL="optimalWaterLevel"
        const val TIMEDAPPS="timesApps"
    }
}

class Time {
    private val numberSpacing: Int = 40
    var reSync={}
    private val colonSpacing = 40
    private val colonWidth = 10
    private val lineWidth = 20
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Compose(density: Density, launchWeatherApp: () -> Unit) {
        var time by remember { mutableStateOf(LocalDateTime.now()) }
        var showSeconds by remember { mutableStateOf(false) }
        reSync={time = LocalDateTime.now()}
        LaunchedEffect(key1 = "time", key2 = showSeconds) {
//            println("At " + time.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " waited " + (60 * 1000 - System.currentTimeMillis() % (60 * 1000)))
//            delay(60 * 1000 - System.currentTimeMillis() % (60 * 1000))
//            println("At " + time.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " done " + (60 * 1000 - System.currentTimeMillis() % (60 * 1000)))
            val maxDelay=if(showSeconds) 1000 else 1000 * 60
            while (true) {
                delay(maxDelay - System.currentTimeMillis() % maxDelay)
                time = LocalDateTime.now()
            }
        }
        Box(Modifier.height((75*density.fontScale).dp)){
//            Text(
//                text = if(!showSeconds) time.format(DateTimeFormatter.ofPattern("HH:mm")) else time.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
//                color = Color.White,
//                fontSize = 75.sp,
//                fontWeight = FontWeight(1000),
//                modifier = Modifier.combinedClickable(onLongClick =  { launchWeatherApp() }, onClick = {showSeconds=!showSeconds; time= LocalDateTime.now()}, indication=null,interactionSource=remember{MutableInteractionSource()}),
//                style = TextStyle.Default
//            )
            Canvas(
                Modifier
                    .fillMaxWidth().fillMaxHeight(.75f).align(Alignment.Center)
                    .combinedClickable(onLongClick =  { launchWeatherApp() }, onClick = {showSeconds=!showSeconds; time= LocalDateTime.now()}, indication=null,interactionSource=remember{MutableInteractionSource()})
            ) {
                val width = size.width
                val height=size.height
                val lineHeight = height/2
                val formatedTime = if(!showSeconds) time.format(DateTimeFormatter.ofPattern("HH:mm")) else time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                val splatUp = formatedTime.toCharArray()
                val spacing = splatUp.let {
                    it.mapIndexed { index, value ->
                        if(index==it.size-1) 0 else
                        if(value== ':' || it[index+1]== ':') colonSpacing else numberSpacing
                    }
                }
                val wides = splatUp.let {
                    it.map { value ->
                        if(value== ':') colonWidth else lineHeight.toInt()
                    }
                }
                val totalWidth = (spacing.sum()+wides.sum())/2
                splatUp.forEachIndexed { index, value ->
                    val shift = width/2 - totalWidth + (spacing.subList(0,index).sum() + wides.subList(0,index).sum())
                    if(value==':'){
                        drawPath(Path().apply {
                            moveTo(shift+colonWidth/2, height*.25f)
                            lineTo(shift+colonWidth/2, height*.25f)
                            moveTo(shift+colonWidth/2, height*.75f)
                            lineTo(shift+colonWidth/2, height*.75f)
                        }, Colors.FONT, style = Stroke(lineWidth.toFloat(), cap = StrokeCap.Round))
                    } else {
                        drawNumber(value, -lineHeight, lineHeight, shift, height)
                    }
                }
            }
        }
    }
    private fun DrawScope.drawNumber(char: Char, lineHeight: Float, width: Float, x: Float, y: Float){
        when(char.digitToInt()){
            0-> {
                drawNumberPath(Path().apply {
                    arcTo(Rect(x, y+lineHeight*2f, x+width, y+lineHeight), 180f,180f, true)
                    lineTo(x+width, y+lineHeight*.5f)
                    arcTo(Rect(x, y+lineHeight*1f, x+width, y), 0f,180f, false)
                    lineTo(x, y+lineHeight*1.5f)
                })
            }
            1 -> {
                drawNumberPath(Path().apply {
                    moveTo(x+width*.75f, y)
                    lineTo(x+width*.75f, y+lineHeight*2)
                    lineTo(x+width*.25f, y+lineHeight*2f)
                })
            }
            2 -> {
                drawNumberPath(Path().apply {
                    arcTo(Rect(x, y+lineHeight*2f, x+width, y+lineHeight), -135f,180f, true)
                    lineTo(x,y)
                    lineTo(x+width,y)
                })
            }
            3 -> {
                drawNumberPath(Path().apply {
                    arcTo(Rect(x, y+lineHeight*2f, x+width, y+lineHeight), -135f,225f, true)
                    arcTo(Rect(x, y+lineHeight*1f, x+width, y), -90f,225f, true)
                })
            }
            4 -> {
                drawNumberPath(Path().apply {
                    moveTo(x,y+lineHeight*2)
                    lineTo(x,y+lineHeight)
                    lineTo(x+width,y+lineHeight)
                    lineTo(x+width,y+lineHeight*2-lineWidth/2) // Bevel Join
                    lineTo(x+width,y)
                })
            }
            5 -> {
                drawNumberPath(Path().apply {
                    moveTo(x+width,y+lineHeight*2)
                    lineTo(x,y+lineHeight*2)
                    lineTo(x,y+lineHeight)
                    lineTo(x+width/2-lineWidth/2,lineHeight+y) // Bevel Join
                    arcTo(Rect(x, y+lineHeight*1f, x+width, y), -90f,180f, true)
                    lineTo(x,y)
                })
            }
            6 -> {
                drawNumberPath(Path().apply {
                    arcTo(Rect(x, y+lineHeight*2f, x+width, y+lineHeight), 180f,135f, true)
                    arcTo(Rect(x, y+lineHeight*1f, x+width, y), -180f,359f, true)
                    lineTo(x,y+lineHeight*1.5f+lineWidth/2) // Bevel Join
                })
            }
            7 -> {
                drawNumberPath(Path().apply {
                    moveTo(x,y+lineHeight*2)
                    lineTo(x+width,y+lineHeight*2)
                    lineTo(x,y)
                })
            }
            8 -> {
                drawNumberPath(Path().apply {
                    arcTo(Rect(x, y+lineHeight*2f, x+width, y+lineHeight), 0f,359f, true)
                    arcTo(Rect(x, y+lineHeight*1f, x+width, y), 0f,359f, true)
                })
            }
            9 -> {
                drawNumberPath(Path().apply {
                    arcTo(Rect(x, y+lineHeight*1f, x+width, y), 0f,135f, true)
                    arcTo(Rect(x, y+lineHeight*2f, x+width, y+lineHeight), 0f,359f, true)
                    lineTo(x+width,y+lineHeight*.5f-lineWidth/2) // Bevel Join
                })
            }
        }
    }
    private fun DrawScope.drawNumberPath(path: Path){
        drawPath(path, color = Color.White, style = Stroke(lineWidth.toFloat(), cap = StrokeCap.Square, join = StrokeJoin.Bevel))
    }
//    private fun DrawScope.drawNumberArc(rect: Rect, angle)
}