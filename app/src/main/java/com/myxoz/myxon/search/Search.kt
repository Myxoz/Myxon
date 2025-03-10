package com.myxoz.myxon.search

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myxoz.myxon.AppComposable
import com.myxoz.myxon.AppDrawer
import com.myxoz.myxon.Colors
import com.myxoz.myxon.Promise
import com.myxoz.myxon.SharedPrefsKeys
import com.myxoz.myxon.SingleSubscription
import com.myxoz.myxon.Subscription
import com.myxoz.myxon.dp
import com.myxoz.myxon.px
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL


/* Intents:

android.intent.action.MAIN
com.google.android.googlequicksearchbox
com.google.android.googlequicksearchbox.GoogleAppImplicitMainInfoGatewayInternal
Main google

Search:
android.intent.action.MAIN
com.google.android.googlequicksearchbox
com.google.android.googlequicksearchbox.SearchActivity

Voice:
android.intent.action.CLASSIC_GSA_VOICE_SEARCH
com.google.android.googlequicksearchbox
com.google.android.apps.search.googleapp.activity.GoogleAppActivity

Lens:
In App

*/
class SearchEngine(
    val name: String,
    val autocompleteURL: String,
    val packageName: String,
    val instantResults: ((String, (InstantAnswer)->Unit)->Unit)? = null,
    val getResults: (String)->List<String>
)
class InstantAnswer(val title: String, val description: String, val url: String)

private fun String?.nullIfBlank(): String? = if(this?.isBlank()==true) null else this

val searchEngines = listOf(
    SearchEngine(
        "Google",
        "https://www.google.com/complete/search?q={s}&client=chrome",
        "com.google.android.googlequicksearchbox",
    ){
        Json.parseToJsonElement(it).jsonArray.getOrNull(1)?.jsonArray?.map { p -> p.jsonPrimitive.content }?: listOf()
    },
    SearchEngine(
        "DuckDuckGo",
        "https://ac.duckduckgo.com/ac/?q={s}",
        "com.duckduckgo.mobile.android",
        { query, response ->
            fetch("https://api.duckduckgo.com/?q=${query}&format=json").get {
                val text = it?:return@get
                Json.parseToJsonElement(text).jsonObject.let {
                    response(
                        InstantAnswer(
                            it.get("Heading")?.jsonPrimitive?.content?.nullIfBlank()?:return@get,
                            it.get("AbstractText")?.jsonPrimitive?.contentOrNull?.nullIfBlank()?:
                                it.get("Abstract")?.jsonPrimitive?.contentOrNull.nullIfBlank()?:
                                it.get("RelatedTopics")?.jsonArray?.getOrNull(0)?.jsonObject?.get("Text")?.jsonPrimitive?.contentOrNull?.let { "(First related):\n$it" }?:
                                return@get,
                            "https://duckduckgo.com/?q=${query} !"
                        )
                    )
                }
            }
        }
    ){
        Json.parseToJsonElement(it).jsonArray.mapNotNull{it.jsonObject.get("phrase")?.jsonPrimitive?.content}
    },
)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Search(context: Context, prefs: SharedPreferences, appDrawerSubscription: Subscription<Boolean, String>, drawer: AppDrawer){
    var isSearching by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<String>() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var instantAnswer: InstantAnswer? by remember { mutableStateOf(null) }
    var isInstantAnswerEnabled: Boolean by remember { mutableStateOf(
        prefs.getBoolean(SharedPrefsKeys.INSTANTANSWER, false)
    )}
    var didSelectSearchEngine by remember {
        mutableStateOf(
            prefs.getString(SharedPrefsKeys.SEARCHENGINE, null)!=null
        )
    }
    var searchEngine: SearchEngine? by remember {
        mutableStateOf(
            prefs.getString(SharedPrefsKeys.SEARCHENGINE,null)?.let { defSearchEngine ->
                if(defSearchEngine=="NONE"){ // Specifically chose not to use a search engine
                    null
                } else {
                    searchEngines.firstOrNull { it.name == defSearchEngine}
                }
            }
        )
    }
    val focusManager = LocalFocusManager.current
    appDrawerSubscription.subscribe("search") {if(!it) {
        results.clear()
        query=""
        focusManager.clearFocus()
        keyboardController?.hide()
    } }
    var searchIcon: ImageBitmap? by remember { mutableStateOf(null) }
    LaunchedEffect(searchEngine) {
        val pm = context.packageManager
        val nnSearchEngine = searchEngine ?: return@LaunchedEffect
        try {
            val icon = pm.getApplicationIcon(nnSearchEngine.packageName)
            if(icon is AdaptiveIconDrawable){
                val foreground = icon.monochrome?.apply { setTint(-0x1 /*White*/) } ?: return@LaunchedEffect
                val size=128; val padding=40
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGBA_1010102)
                foreground.setBounds(-padding,-padding,size+padding,size+padding)
                foreground.draw(Canvas(bitmap))
                searchIcon = bitmap.asImageBitmap()
            }
        } catch (_: NameNotFoundException) { }
    }
    Column (
        Modifier
            .background(Color(0xFF222222), RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .clickable(indication = null, interactionSource = null) { isSearching = true }
            .fillMaxWidth()
            .padding(13.dp)
            .animateContentSize()
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
            val nnIcon = searchIcon
            if(nnIcon!=null) {
                Image(
                    nnIcon,
                    "Search Icon",
                    Modifier
                        .size(24.dp)
                        .combinedClickable(onLongClick = {
                            searchEngine=null
                            didSelectSearchEngine=false
                            isSearching=true
                            appDrawerSubscription.send(true)
                        }) { startSearchEngineOrBrowser(null, searchEngine, context) }
                )
            } else {
                Icon(
                    Icons.Rounded.Search,
                    "Search",
                    tint= Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .combinedClickable(onLongClick = {
                            searchEngine=null
                            didSelectSearchEngine=false
                        }){}
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ){
                BasicTextField(
                    query,
                    {
                        query=it
                        val nnSE = searchEngine ?: return@BasicTextField
                        fetch(nnSE.autocompleteURL.replace("{s}",it)).get {response ->
                            response?:return@get
                            val responses = nnSE.getResults(response)
                            results.clear()
                            results.addAll(responses)
                            nnSE.instantResults?.let {
                                it(responses.getOrNull(0)?:return@let){
                                    instantAnswer=it
                                }
                            }
                        }
                    },
                    Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                appDrawerSubscription.send(true)
                                isSearching=true
                            } else {
                                isSearching=false
                            }
                        },
                    singleLine = true,
                    textStyle = TextStyle.Default.copy(
                        color = Colors.FONT,
                        textAlign = TextAlign.Left,
                        fontSize = 18.sp,
                    ),
                    cursorBrush = SolidColor(Colors.FONT),
                    keyboardActions = KeyboardActions(onDone = {
                        startSearchEngineOrBrowser(query, searchEngine, context)
                    }),
                    decorationBox = { content ->
                        if(!isSearching){
                            Text(
                                if(!didSelectSearchEngine) {
                                    "Select search engine"
                                } else if(searchEngine==null) {
                                    "Search apps"
                                } else {
                                    "Search apps or ${searchEngine?.name}"
                                },
                                fontSize = 18.sp,
                                color = Colors.SECONDARY_FONT
                            )
                        } else {
                            content()
                        }
                    }
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        val allAppResults=drawer.allApps.getOrNull()?.filter { it.label.lowercase().contains(query.lowercase()) }
        if(isSearching && !didSelectSearchEngine) {
            Spacer(Modifier.height(20.dp))
            Text("Select search engine", color = Colors.SECONDARY_FONT)
            Column(Modifier.fillMaxWidth()) {
                (searchEngines + listOf(null)).forEach {
                    SearchEngineOption(
                        it
                    ){
                        didSelectSearchEngine=true
                        searchEngine=it
                        searchIcon=null
                        instantAnswer=null
                        results.clear()
                        prefs
                            .edit()
                            .putString(SharedPrefsKeys.SEARCHENGINE, it.name)
                            .apply()
                    }
                }
                Row(
                    Modifier.
                        fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Instant answers (IA) in results",
                        style = MaterialTheme.typography.labelLarge.copy(Colors.FONT),
                    )
                    Switch(isInstantAnswerEnabled, {isInstantAnswerEnabled=it; prefs.edit().putBoolean(SharedPrefsKeys.INSTANTANSWER, it).apply()})
                }
                Text(
                    "You can always change this by holding down on the search icon",
                    style = MaterialTheme.typography.titleSmall.copy(Colors.SECONDARY_FONT)
                )
            }
        }
        if(isSearching && query.isNotEmpty()) {
            if(!allAppResults.isNullOrEmpty()) {
                Spacer(Modifier.height(20.dp))
                val elementWidth = ((LocalConfiguration.current.screenWidthDp * .9 - 30*2 - 13*2)/4).dp
                Text("Installed Apps:", color = Colors.SECONDARY_FONT)
                Row {
                    allAppResults.take(5).forEach{
                        AppComposable(it, Modifier.weight(1f), AppDrawer.defaultDrawable, false, {}, {it.launch(context)}, SingleSubscription())
                    }
                }
            }
            if(results.isNotEmpty()){
                Spacer(Modifier.height(20.dp))
                Text("${searchEngine?.name} results:", color = Colors.SECONDARY_FONT)
                Column {
                    if(isInstantAnswerEnabled){
                        instantAnswer?.also{
                            searchEngine?.also { se ->
                                InstantAnswerComposable(it, se, context)
                            }
                        }
                    }
                    results.forEach { Text(it, color = Colors.FONT, fontSize = 20.sp, modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            startSearchEngineOrBrowser(it, searchEngine, context)
                            Promise { delay(2000); appDrawerSubscription.send(false) }
                        }
                        .padding(5.dp, 10.dp, 0.dp, 10.dp)) }
                }
            }
        }
    }
}

@Composable
fun InstantAnswerComposable(instantAnswer: InstantAnswer, searchEngine: SearchEngine, context: Context) {
    var isExpanded by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()
    var linesAmount by remember { mutableIntStateOf(3) }
    val lineHeight = MaterialTheme.typography.titleSmall.fontSize.px(LocalDensity.current)
    var oneLineHeight by remember { mutableFloatStateOf(lineHeight) }
    val textHeight by animateFloatAsState(
        if(isExpanded) oneLineHeight*linesAmount else if(linesAmount>=3) 3f*oneLineHeight else lineHeight*linesAmount,
        spring(stiffness = Spring.StiffnessMedium)
    )
    println("$lineHeight $linesAmount $textHeight")
    Surface(
        {
            startSearchEngineOrBrowser(instantAnswer.title, searchEngine, context)
        },
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        color = Colors.SECONDARY,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier
                .padding(15.dp),
        ){
            Text(
                instantAnswer.title,
                style = MaterialTheme.typography.headlineSmall.copy(Colors.FONT),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            val smallTitle = MaterialTheme.typography.titleSmall
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { layoutCoordinates ->
                        val result = textMeasurer.measure(
                            instantAnswer.description,
                            style = smallTitle,
                            constraints = Constraints.fixedWidth(layoutCoordinates.size.width)
                        )
                        linesAmount = result.lineCount
                        oneLineHeight = result.size.height / linesAmount.toFloat()
                    }
            ) {
                Text(
                    instantAnswer.description,
                    Modifier.height(textHeight.dp(LocalDensity.current)),
                    style = MaterialTheme.typography.titleSmall.copy(Colors.SECONDARY_FONT),
                    overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
                )
            }

            if (linesAmount > 3) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Open first result",
                        style = MaterialTheme.typography.titleMedium.copy(Colors.LINK),
                        modifier = Modifier
                            .background(Colors.LINK.copy(.1f), CircleShape)
                            .clip(CircleShape)
                            .clickable(
                                remember { MutableInteractionSource() }, remember { ripple() }
                            ) {
                                openBrowser(instantAnswer.url, context)
                            }
                            .padding(10.dp, 5.dp)
                    )
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .clickable {
                                isExpanded = !isExpanded
                            }
                            .padding(10.dp, 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(
                            "Show ${if (isExpanded) "less" else "more"}",
                            style = MaterialTheme.typography.titleMedium.copy(Colors.FONT),
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            "Show more",
                            Modifier
                                .rotate(
                                    (textHeight-3f*oneLineHeight)/(oneLineHeight*(linesAmount-3f))*180f
                                ),
                            Colors.FONT
                        )
                    }
                }
            }
        }
    }
}

fun openBrowser(url: String, context: Context) {
    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    browserIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(browserIntent)
}

@Composable
fun SearchEngineOption(searchEngine: SearchEngine?, setAsSearchEngine: (SearchEngine) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ){
        Text(
            searchEngine?.name?.let {
                if(searchEngine.instantResults ==null){
                    "$it (No support for IA)"
                } else {
                    it
                }
            }?:"None",
            style = MaterialTheme.typography.titleMedium.copy(Colors.FONT)
        )
        FilledTonalButton(
            {
                setAsSearchEngine(searchEngine?:SearchEngine("NONE","","",null,{ listOf() }))
            }
        ) { Text(
            "Set",
            style = MaterialTheme.typography.titleSmall
        )}
    }
}

fun startSearchEngineOrBrowser(
    query: String?,
    searchEngine: SearchEngine?,
    context: Context
) {
    val packageManager = context.packageManager
    if(query==null){
        context.startActivity(packageManager.getLaunchIntentForPackage(searchEngine!!.packageName))
    } else {
        val launchDefaultSearchProvider = if(searchEngine!=null) {
            try {
                packageManager.getPackageInfo(searchEngine.packageName, 0);
                false
            } catch (e: NameNotFoundException) {
                true
            }
        } else {
            true
        }
        val intent = if(launchDefaultSearchProvider || searchEngine==null){
            Intent(Intent.ACTION_WEB_SEARCH)
        } else {
            Intent(Intent.ACTION_WEB_SEARCH).setPackage(searchEngine.packageName)
        }
        intent.putExtra(SearchManager.QUERY, query)
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }
}
fun fetch(fetchURL: String): Promise<String?> {
    return Promise {
        val url = URL(fetchURL)
        println("Fetching: $url")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "*/*")
        connection.setRequestProperty("Accept-Encoding", "identity")
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        )
        val responseCode = connection.responseCode
        val response = if (responseCode == HttpURLConnection.HTTP_OK) {
            println("Fetch success")
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
        connection.disconnect()
        response
    }
}
//}