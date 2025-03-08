package com.myxoz.myxon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar


class WidgetIsland(private val context: Context, var prefs: SharedPreferences){
    private var listener: ((level: Float, time: Long, isCharging: Boolean) -> Unit)? = null
    val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val timeRemaining: Long
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val isPluggedIn = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            val chargeTime=bm.computeChargeTimeRemaining()
            timeRemaining=chargeTime
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            listener?.invoke(level / scale.toFloat(), timeRemaining, isPluggedIn!=0)
        }
    }
    @Composable
    fun Component(appDrawer: AppDrawer) {
        var batteryLevelPercent by remember {
            mutableFloatStateOf(0f)
        }
        var timeRemaining by remember {
            mutableLongStateOf(0L)
        }
        var isCharging by remember {
            mutableStateOf(false)
        }
        var currentDisplayingMode by remember {
            mutableStateOf(DisplayingMode.Battery)
        }
        listener={it, tr, ic-> batteryLevelPercent=it; timeRemaining=tr; isCharging=ic}
        val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        batteryLevelPercent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)/100f
        Row(
            Modifier
                .fillMaxWidth()
//                .background(Colors.MAIN, RoundedCornerShape(25.dp))
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            Box(Modifier.padding(20.dp).size(60.dp)){
                CircularProgressIndicator(
                    { batteryLevelPercent },
                    Modifier
                        .size(60.dp)
                        .clickable(
                            remember { MutableInteractionSource() },
                            null
                        ) {
                            currentDisplayingMode =
                                if (currentDisplayingMode == DisplayingMode.Battery) DisplayingMode.TimeRemaining else if (currentDisplayingMode == DisplayingMode.TimeRemaining) DisplayingMode.EstimatedEnd else DisplayingMode.Battery
                        },
                    //                    if(isCharging) Colors.CHARGING else Colors.FONT,
                    Color(1-batteryLevelPercent,batteryLevelPercent,0f,1f),
                    5.dp,
                    if(isCharging) Colors.CHARGING else Colors.DISCHARGING,
                    StrokeCap.Round
                )
                Text(
                    if(isCharging) {
                        when (currentDisplayingMode) {
                            DisplayingMode.Battery -> "${(batteryLevelPercent * 100).toInt()}%"
                            DisplayingMode.TimeRemaining -> convertToHumanReadableString(
                                timeRemaining / (60 * 60 * 1000),
                                (timeRemaining / (60 * 1000)) % 60
                            )

                            DisplayingMode.EstimatedEnd -> {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.HOUR, (timeRemaining / (60 * 60 * 1000)).toInt())
                                cal.add(
                                    Calendar.MINUTE,
                                    ((timeRemaining / (60 * 1000)) % 60).toInt()
                                )
                                convertToHumanReadableString(
                                    cal.get(Calendar.HOUR_OF_DAY).toLong(),
                                    cal.get(Calendar.MINUTE).toLong(),
                                    false
                                )
                            }
                        }
                    }else {
                        "${(batteryLevelPercent*100).toInt()}%"
                    },
                    modifier = Modifier.align(Alignment.Center),
                    color = Colors.FONT,
                    fontWeight = FontWeight.W900
                )
            }
            Row(Modifier.height(80.dp).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)){
                val appPrerenders = remember {
                    mutableStateListOf<App>().apply {
                        val popularApps= appDrawer.allApps.getOrNull() ?: return@apply
                        val reusableTimesOpened=App.getReusableTimesOpened(prefs)
                        addAll(popularApps
                            .sortedByDescending { it.getTimesOpened(reusableTimesOpened) }
                            .take(3)
                        )
                    }
                }
                appDrawer.allApps.get {
                    val reusableTimesOpened=App.getReusableTimesOpened(prefs)
                    val futureList=it
                        .sortedByDescending { it.getTimesOpened(reusableTimesOpened) }
                        .take(3)
                    if(appPrerenders.size!=0) return@get
                    appPrerenders.addAll(
                        futureList
                    )
                }
    //            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                appPrerenders.forEach {
                    var appIcon: ImageBitmap? by remember {
                        mutableStateOf(it.iconPromise.getOrNull())
                    }
                    if(appIcon==null){
                        it.iconPromise.get { appIcon=it?:AppDrawer.defaultDrawable}
                    }
                    Image(appIcon?:AppDrawer.defaultDrawable, contentDescription = it.label,
                        Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { it.launch(context) }
                    )
                }
            }
        }
    }
}
enum class DisplayingMode {
    Battery,
    TimeRemaining,
    EstimatedEnd
}
fun convertToHumanReadableString(hours: Long, min: Long, addEndLetter: Boolean=true): String{
    return "${if(hours>0) "$hours:" else ""}${if(hours!=0L && min<10) "0$min" else "$min"}${if(addEndLetter) { if (hours > 0) "h" else "m" } else ""}"
}