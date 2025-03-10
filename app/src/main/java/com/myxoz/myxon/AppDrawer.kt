package com.myxoz.myxon

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max


class AppDrawer(val prefs: SharedPreferences, val context: Context){
    val iconMap: MutableMap<String, FetchingAppIcon?> = mutableMapOf()
    private val pm: PackageManager = context.packageManager
    private val hiddenPackages=listOf(
        "com.samsung.android.incallui", // Call UI / Not openable
        "com.google.android.healthconnect.controller",
        "com.android.traceur", // Debug smth
        "com.android.stk2", // Sim
        "com.whatsapp",  // WhatsApp, already on main
        "com.google.android.googlequicksearchbox", // Google
        "com.myxoz.myxon", // This app, already open
        "com.samsung.android.app.watchmanager",
        "com.samsung.android.app.watchmanagerstub", // kp
        "com.samsung.accessibility",
        "com.android.settings",
        "com.samsung.android.gru", //Galaxy Resource?! Update
        "org.telegram.messenger",  // Telegram, already on main
        "com.snapchat.android", // Snapchat, already on main
        "com.samsung.android.lool", //Device Care
    )
    val allApps=Promise{
        val reusableTimesOpened=App.getReusableTimesOpened(prefs)
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != "null" && !hiddenPackages.contains(it.packageName)
            }
            .map {
                App(
                    it.packageName,
                    pm,
                    pm.getApplicationInfo(it.packageName, MATCH_UNINSTALLED_PACKAGES),
                    this,
                    prefs = prefs
                )
            }
            .also {
                prefs.edit().putStringSet(
                    SharedPrefsKeys.CACHEDAPPS,
                    it.map {"${it.packageName};${it.label}"}.toSet()
                )
            }
            .sortedBy { it.label }
            .sortedByDescending {
                it.getTimesOpened(reusableTimesOpened)
            }
//            .flatMap { listOf(it,it,it,it) }
    }
    init { // In a CoroutineScope due to performance reasons
        defaultDrawable=ContextCompat.getDrawable(context, R.drawable.noicon)!!.toBitmap(128-32,128-32).asImageBitmap()
        allApps.start()
    }
    companion object {
        var defaultDrawable = ImageBitmap(128-32, 128-32)
    }
}
fun saveHiddenApps(prefs: SharedPreferences, hiddenApps: List<String>){
    prefs.edit().putStringSet(SharedPrefsKeys.HIDDENAPPS, hiddenApps.toSet()).apply()
}
@Stable
@Composable
fun AppDrawerComposable(drawer: AppDrawer, cachedApps: List<App>, modifier: Modifier,closeDrawer: ()->Unit) {
    val allApps = remember {
        cachedApps.toMutableStateList().apply {
            drawer.allApps.get {
                clear(); addAll(it.sortedByDescending { it.getTimesOpened() })
            }
        }
    }
    var selectionMode by remember { mutableStateOf(false) }
    val appsSelected = remember { mutableStateListOf<App>() }
    val hiddenApps = remember {
        (drawer.prefs.getStringSet(SharedPrefsKeys.HIDDENAPPS, setOf())?: setOf()).toMutableStateList()
    }
    val selectionModeMap = remember { mutableStateMapOf<String, SingleSubscription<Boolean>>() }
    if(selectionMode){
        Row(horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically, modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
        )
        {
            IconButton(onClick = {
                hiddenApps.addAll(appsSelected.map { it.packageName })
                saveHiddenApps(drawer.prefs, hiddenApps)
            }) {
                Icon(ImageVector.vectorResource(id = R.drawable.hide), "Hide", tint = Colors.TERTIARY_FONT, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = {
                hiddenApps.removeAll(appsSelected.map { it.packageName })
                saveHiddenApps(drawer.prefs, hiddenApps)
            }) {
                Icon(ImageVector.vectorResource(id = R.drawable.show), "Show", tint = Colors.FONT, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = {
                appsSelected.forEach { it.uninstall(drawer.context) }
            }) {
                Icon(Icons.Rounded.Delete, "Delete", tint = Colors.FONT, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = {
                closeDrawer()
                selectionModeMap.values.forEach {
                    it.send(false)
                }
                appsSelected.clear()
                selectionMode=false
            },
                Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(Icons.Filled.Close, "Close", tint = Colors.FONT, modifier = Modifier.size(30.dp))
            }
        }
    } else {
        Spacer(Modifier.height(10.dp))
    }
    val tap: (App)-> Unit ={ app: App ->
        if (!selectionMode) {
            app.launch(drawer.context)
            async {
                delay(1000)
                closeDrawer()
            }
        } else {
            if (appsSelected.contains(app)) {
                selectionModeMap[app.packageName]?.send(false)
                appsSelected.remove(app)
                if (appsSelected.isEmpty()) {
                    selectionMode=false
                }
            } else {
                selectionModeMap[app.packageName]?.send(true)
                appsSelected.add(app)
            }
        }
    }
    val hold={ app: App ->
        if(appsSelected.contains(app)){
            app.openAppInfo(drawer.context)
            closeDrawer()
        } else {
            Toast.makeText(drawer.context, app.packageName, Toast.LENGTH_SHORT)
                .show()
            selectionModeMap[app.packageName]?.send(true)
            selectionMode=true
            appsSelected.add(app)
        }
    }
    val configuration= LocalConfiguration.current
    val elementWidth = ((configuration.screenWidthDp * .9 - 30*2)/4).dp
    val visibleApps = allApps.filter { !hiddenApps.contains(it.packageName)}.chunked(4)
    visibleApps.forEachIndexed { i, it ->
        Row(modifier.fillMaxWidth()){
            it.forEach {
                val sub = SingleSubscription<Boolean>()
                selectionModeMap[it.packageName] = sub
                AppComposable(it, Modifier.weight(1f), AppDrawer.defaultDrawable, appsSelected.contains(it), { hold(it) }, { tap(it) }, sub)
            }
            if(i == visibleApps.size-1){
                repeat(4-it.size){
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(modifier.height(20.dp))
    Spacer(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Colors.TERTIARY_FONT, CircleShape))
    Spacer(modifier.height(20.dp))
    val hiddenAppList = allApps.filter { hiddenApps.contains(it.packageName)}.chunked(4)
    hiddenAppList.forEachIndexed { i, it ->
        Row(modifier.fillMaxWidth()){
            it.forEach {
                val sub = SingleSubscription<Boolean>()
                selectionModeMap[it.packageName] = sub
                AppComposable(it, Modifier.weight(1f), AppDrawer.defaultDrawable, appsSelected.contains(it), { hold(it) }, { tap(it) }, sub)
            }
            if(i == hiddenAppList.size-1){
                repeat(4-it.size){
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
class FetchingAppIcon(
    val subscription: Subscription<ImageBitmap?, App>?,
    val icon: ImageBitmap?
)
class App(
    val packageName: String,
    private val pm: PackageManager,
    private val info: ApplicationInfo?,
    private val appDrawer: AppDrawer,
    val label: String=info?.loadLabel(pm)?.toString()?:"",
    private val prefs: SharedPreferences
){
    var icon: ImageBitmap?=null
    private val timeDownTicking=30*60*1000
    private val timeToOpen=5*60*1000
    private var hasIconRendered=false
    val iconPromise=Promise{
        if(!appDrawer.iconMap.contains(packageName)){
            val finalSubscription = Subscription<ImageBitmap?, App>()
            appDrawer.iconMap[packageName] = FetchingAppIcon(
                finalSubscription,
                null
            )
            icon=asyncAppIcon()
            appDrawer.iconMap[packageName] = FetchingAppIcon(
                null,
                icon
            )
            finalSubscription.send(icon)
            icon
        } else {
            (appDrawer.iconMap[packageName]!!).let {
                if(it.icon!=null) return@let it.icon
                val deferredValue: CompletableDeferred<ImageBitmap?> = CompletableDeferred()
                it.subscription!!.subscribe(
                    this,
                ) {
                    deferredValue.complete(it)
                }
                deferredValue.await()
            }
        }
    }.apply { start() }
    private fun asyncAppIcon(): ImageBitmap? {
        try {
            val drawable = pm.getApplicationIcon(packageName)
            if (drawable is AdaptiveIconDrawable) {
                val foreground: Drawable = drawable.monochrome?.apply {
                    setTint(-0x1 /*White*/)
                } ?: drawable.foreground.apply {
                    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                }
                val size=128; val padding=16
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGBA_1010102)
                foreground.setBounds(-padding,-padding,size+padding,size+padding)
                foreground.draw(Canvas(bitmap))
                return bitmap.asImageBitmap()
            } else if(drawable is BitmapDrawable) {
                return null
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }
    fun getTimesOpened(set: Set<String>?=null): Int=
        (set?:(prefs.getStringSet(SharedPrefsKeys.PACKAGEOPENED, setOf())?: setOf()))
            .find{it.split(";",limit=2).getOrNull(1)==packageName}
            ?.split(";",limit=2)
            ?.getOrNull(0)
            ?.toIntOrNull()
            ?:0
    fun launch(context: Context){
        if(isTimedApp()) {
            timedAppOpening(context)
        } else {
            try {
                context.startActivity(pm.getLaunchIntentForPackage(packageName))
                increaseOpeningTimes()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "App can't be launched",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun increaseOpeningTimes(){
        var openedApps = (prefs.getStringSet(SharedPrefsKeys.PACKAGEOPENED, setOf())?: setOf()).toMutableSet()
        if(openedApps.find { it.split(";",limit=2).getOrNull(1) == packageName }!=null){
            openedApps = openedApps.map {
                if(it.split(";",limit=2).getOrNull(1) == packageName)
                    "${(it.split(";",limit=2).getOrNull(0)?.toIntOrNull()?:0)+1};${it.split(";",limit=2).getOrNull(1)}"
                else
                    it
            }.toMutableSet()
        } else {
            openedApps.add("0;$packageName")
        }
        prefs.edit().putStringSet(SharedPrefsKeys.PACKAGEOPENED, openedApps.toMutableSet()).apply()
    }
    private fun timedAppOpening(context: Context){
        val timedAppData=prefs.getStringSet(SharedPrefsKeys.TIMEDAPPS,null)?.toMutableSet()?: mutableSetOf()
        var data = timedAppData.find{it.split(";")[0]==packageName}
        timedAppData.remove(data)
        val lastAppOpened = data?.split(";")?.getOrNull(1)?.toLongOrNull()?:0L
        if(data==null || System.currentTimeMillis() !in ((lastAppOpened+timeDownTicking)..lastAppOpened+(timeToOpen+timeDownTicking))) {
            // Not defined defined or outside of 5min timeframe
            if(data==null || System.currentTimeMillis() !in (lastAppOpened..lastAppOpened+(timeToOpen+timeDownTicking))){
                // Not defined or outside of 5min + 30min timeframe
                data = packageName+";"+System.currentTimeMillis()
            }
            prefs.edit().putStringSet(SharedPrefsKeys.TIMEDAPPS, timedAppData.apply { add(data) }).apply()
            val timeLeftInSeconds=(timeDownTicking-(System.currentTimeMillis()-(data.split(";").getOrNull(1)?.toLongOrNull()?:0L)))/1000L
            val timeInReadable="${timeLeftInSeconds/60L}:${if(timeLeftInSeconds%60<=9) "0" else ""}${timeLeftInSeconds%60}"
            Toast.makeText(context, "$timeInReadable left", Toast.LENGTH_LONG).show()
        } else {
            try {
                context.startActivity(pm.getLaunchIntentForPackage(packageName))
                increaseOpeningTimes()
            } catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "App can't be launched",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    fun timedAppProgressPercentage(): Float{
        val timedAppData=prefs.getStringSet(SharedPrefsKeys.TIMEDAPPS,null)?.toMutableSet()?: mutableSetOf()
        val lastOpeningTimestamp = timedAppData.find{it.split(";")[0]==packageName}?.split(";")?.get(1)?.toLongOrNull()?:0L
        return if(System.currentTimeMillis()-lastOpeningTimestamp in 0..timeDownTicking+timeToOpen)
                ((System.currentTimeMillis()-lastOpeningTimestamp)/timeDownTicking.toFloat()).coerceIn(0f,1f)
            else
                0f
    }
    fun isTimedApp(): Boolean = packageName in arrayOf("anddea.youtube", "com.instagram.android")
    fun uninstall(context: Context) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
        intent.data = Uri.parse("package:$packageName")
        context.startActivity(intent)
    }
    fun openAppInfo(context: Context){
        if(isTimedApp()) return
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        i.addCategory(Intent.CATEGORY_DEFAULT)
        i.setFlags(FLAG_ACTIVITY_NEW_TASK)
        i.setData(Uri.parse("package:$packageName"))
        context.startActivity(i)
    }
    companion object {
        fun getReusableTimesOpened(prefs: SharedPreferences)=(prefs.getStringSet(SharedPrefsKeys.PACKAGEOPENED, setOf())?: setOf())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppComposable(app: App, modifier: Modifier, defaultDrawable: ImageBitmap, selected: Boolean, hold: ()->Unit, tap: ()->Unit, singleSubscription: SingleSubscription<Boolean>): Unit{
    var appIcon: ImageBitmap? by remember(app.packageName) {
        mutableStateOf(app.iconPromise.getOrNull())
    }
    if(appIcon==null){
        app.iconPromise.get { appIcon=it?:defaultDrawable; }
    }
    var isSelected by remember(app.packageName) { mutableStateOf(selected) }
    singleSubscription.subscribe { isSelected=it }
    var timesAppPercentage by remember {
        mutableStateOf(if(app.isTimedApp()) app.timedAppProgressPercentage() else null)
    }

    if(app.isTimedApp()) {
        LaunchedEffect(app) {
            delay(10000)
            timesAppPercentage=app.timedAppProgressPercentage()
        }
    }

    Column(
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(0.dp, 10.dp)
            .combinedClickable(
                indication = null,
                interactionSource = null,
                onLongClick = if (appIcon != null) hold else { -> },
                onClick = if (appIcon != null) tap else { -> }
            )
    ) {
        if(appIcon!=null){
            if(isSelected)
                BadgedBox({
                    Icon(Icons.Filled.Check,"Tick",
                        Modifier
                            .size(20.dp)
                            .background(Colors.APPSELECTED, CircleShape),Colors.FONT)
                }){
                    Image(
                        appIcon!!, contentDescription = app.label,
                        Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Colors.SECONDARY)
                            .run {
                                if (timesAppPercentage != null) {
                                    circularBackgroundWithFraction(
                                        timesAppPercentage!!,
                                        Colors.TERTIARY
                                    )
                                } else {
                                    this
                                }
                            }
                    )
                }
            else
                Image(
                    appIcon!!, contentDescription = app.label,
                    Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Colors.SECONDARY)
                        .run {
                            if (timesAppPercentage != null) {
                                circularBackgroundWithFraction(
                                    timesAppPercentage!!,
                                    Colors.TERTIARY
                                )
                            } else {
                                this
                            }
                        }
                )
        } else {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Colors.SECONDARY)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(app.label, color = Colors.FONT, overflow = TextOverflow.Ellipsis, maxLines = 1)
    }
}
//
// Header item
fun async(coroutineScope: CoroutineScope?=null, func: suspend CoroutineScope.()->Unit){
    (coroutineScope ?: CoroutineScope(Dispatchers.Default)).launch {
        func()
    }
}
fun debug(func: ()->Unit){
    try {
        func()
    } catch (e: Exception){
        e.printStackTrace()
    }
}
fun toast(context: Context?, text: Any?){
    debug {
        Toast.makeText(context!!, text.toString(), Toast.LENGTH_LONG).show()
    }
}
fun launchIntentSafely(
    context: Context?,
    action: String="android.intent.action.MAIN",
    packageName: String?,
    flag: Int?,
    classPackageName: String?,
    className: String?,
    uri: String?=null,
    onError: ((e: Exception)->Unit)={}
){
    try{
        context ?: return
        val intent= if(uri==null) Intent(action) else Intent(action, Uri.parse(uri))
        if(packageName != null) intent.setPackage(packageName)
        if(classPackageName!=null && className!=null) intent.setClassName(classPackageName,className)
        if(flag!=null) intent.setFlags(flag)
        context.startActivity(intent)
    } catch (e: Exception){
        e.printStackTrace()
        onError.invoke(e)
    }
}
class Promise<T>(val func: suspend (triggerError: ()->Unit)->T){
    private var tryingToComplete=false
    private val asyncVal= CompletableDeferred<T>()
    private var onError: MutableList<((e: Exception?)->Unit)> = mutableListOf()
    private var onComplete: MutableList<(()->Unit)> = mutableListOf()
    private var hasFailed=false
    private var exception: Exception?=null
    suspend fun get(): T{
        if(asyncVal.isCompleted) return asyncVal.await()
        if(!tryingToComplete) {
            tryingToComplete=true
            run()
        }
        return asyncVal.await()
    }
    fun failed(onError: ((e: Exception?)->Unit)): Promise<T>{
        if(hasFailed) onError(exception) else this.onError.add(onError)
        return this
    }
    fun complete(onComplete: ()->Unit): Promise<T>{
        if(asyncVal.isCompleted) onComplete() else this.onComplete.add(onComplete)
        return this
    }
    fun get(onFinished: (T)->Unit): Promise<T>{
        async{
            onFinished(get())
        }
        return this
    }
    private suspend fun run(){
        try{
            val resolved=func{ onError.forEach{it(null)} }
            asyncVal.complete(resolved)
            onComplete.forEach { it.invoke() }
        } catch(e: Exception){
            e.printStackTrace()
            hasFailed=true
            exception=e
            onError.forEach{it(e)}
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getOrNull(): T?{
        return try {
            asyncVal.getCompleted()
        } catch (e: Exception){
            null
        }
    }
    fun start(){
        async { run() }
    }
}
@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun DraggableMenu(defaultOffsetPx: Float, fullyOpenedOffsetPx: Float, modifier: (Float)->Modifier= { Modifier }, handle: @Composable (Modifier)->Unit, content: @Composable (Float)->Unit, openingSubscription: Subscription<Boolean, String>){
    val density= LocalDensity.current
    var targetOffset by remember { mutableFloatStateOf(defaultOffsetPx) }
    openingSubscription.subscribe("draggableMenu"){if(targetOffset!= if(it) fullyOpenedOffsetPx else defaultOffsetPx ) targetOffset = if(it) fullyOpenedOffsetPx else defaultOffsetPx}
    var isDraggingDown by remember { mutableStateOf(false) }
    val offset by animateFloatAsState(targetOffset, label = "", animationSpec=if(isDraggingDown) tween(0) else spring())
    val draggableState = rememberDraggableState { delta -> targetOffset+=delta }
    val progress = ((offset-defaultOffsetPx)/(fullyOpenedOffsetPx-defaultOffsetPx)).coerceIn(0f, 1f)
    Column(Modifier
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                val centerX = constraints.maxWidth / 2 - placeable.width / 2
                val centerY = constraints.maxHeight - placeable.height
                placeable.placeRelative(centerX, centerY)
            }
        }
        .offset(
            0.dp,
            (offset.coerceIn(
                fullyOpenedOffsetPx,
                defaultOffsetPx
            ) / density.density).dp
        )
        .then(modifier(progress))
    ) {
        handle(Modifier.draggable(
            orientation = Orientation.Vertical,
            onDragStarted = {
                isDraggingDown=true
            },
            onDragStopped = { velocity ->
                isDraggingDown=false
                targetOffset = if (offset+velocity*.2f < (defaultOffsetPx+fullyOpenedOffsetPx)/2) fullyOpenedOffsetPx else defaultOffsetPx
                openingSubscription.send(offset+velocity*.2f < (defaultOffsetPx+fullyOpenedOffsetPx)/2)
            },
            state = draggableState
        ))
        var scrollDistance by remember { mutableFloatStateOf(0f) }
        var contentHeight by remember { mutableIntStateOf(100) }
        var selfHeight by remember { mutableIntStateOf(100) }
        var flingAnimation = remember {
            Animatable(scrollDistance)
        }
        val flingDecay = rememberSplineBasedDecay<Float>()
        val scrollState = rememberDraggableState { delta ->
            val scrolledDistance = (scrollDistance-delta).coerceIn(0f, max(0f, (contentHeight-selfHeight).toFloat()))
            if(targetOffset!=fullyOpenedOffsetPx || scrolledDistance==0f){
                isDraggingDown=true
                targetOffset=max(delta+targetOffset, fullyOpenedOffsetPx)
                scrollDistance=0f
            } else {
                scrollDistance= scrolledDistance
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = scrollState,
                    onDragStopped = { velocity ->
                        if (isDraggingDown) {
                            isDraggingDown = false
                            targetOffset =
                                if (offset + velocity * 0.2f < (defaultOffsetPx + fullyOpenedOffsetPx) / 2) fullyOpenedOffsetPx else defaultOffsetPx
                            openingSubscription.send(offset + velocity * 0.2f < (defaultOffsetPx + fullyOpenedOffsetPx) / 2)
                        } else {
                            // Fling effect for smooth deceleration
                            flingAnimation = Animatable(scrollDistance)
                            flingAnimation.animateDecay(
                                -velocity,
                                flingDecay
                            ) {
                                scrollDistance = value.coerceIn(
                                    0f,
                                    max(0f, (contentHeight - selfHeight).toFloat())
                                )
                            }
                        }
                    },
                    onDragStarted = {
                        flingAnimation.stop()
                    }
                )
                .onGloballyPositioned {
                    selfHeight = it.size.height
                }
                .clipToBounds()
        ) {
            Layout(content = {
                content(progress)
            }) { measurables, constraints ->
                val placeables = measurables.map { measurable ->
                    measurable.measure(constraints)
                }
                contentHeight = placeables.sumOf { it.height }
                val height = constraints.constrainHeight(contentHeight)
                layout(constraints.maxWidth, height) {
                    var yPosition = -scrollDistance.toInt()
                    placeables.forEach { placeable ->
                        placeable.placeRelative(x = 0, y = yPosition)
                        yPosition += placeable.height
                    }
                }
            }
        }
    }
}
class Subscription<T,K> {
    val mutiSubscriptions: MutableMap<K, (T)->Unit> = mutableMapOf()
    fun subscribe(key: K, func: (T)->Unit){
        mutiSubscriptions[key] = func
    }
    fun send(data: T){
        mutiSubscriptions.values.forEach{it(data)}
    }
}
class SingleSubscription<T> {
    private var subscription: ((T)->Unit)? = null
    fun subscribe(func: (T)->Unit){
        subscription=func
    }
    fun send(data: T){
        subscription?.invoke(data)
    }
}
fun Dp.px(density: Density): Float{
    return value*density.density
}
fun TextUnit.px(density: Density): Float{
    return density.fontScale*density.density*value
}
fun Float.dp(density: Density): Dp{
    return (this/density.density).dp
}
fun Modifier.circularBackgroundWithFraction(
    fraction: Float,
    circleColor: Color
): Modifier = this.then(
    Modifier.drawBehind {
        rotate(-90f) { // Make starting point from top-center
            drawArc(
                color = circleColor,
                startAngle = 0f,
                sweepAngle = fraction * 360f,
                useCenter = true, // Makes it a pie-chart style fill
                size = size
            )
        }
    }
)