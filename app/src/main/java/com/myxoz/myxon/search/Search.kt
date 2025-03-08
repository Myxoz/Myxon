package com.myxoz.myxon.search

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myxoz.myxon.AppComposable
import com.myxoz.myxon.AppDrawer
import com.myxoz.myxon.Colors
import com.myxoz.myxon.Promise
import com.myxoz.myxon.SingleSubscription
import com.myxoz.myxon.Subscription
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
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
@Composable
fun Search(context: Context, appDrawerSubscription: Subscription<Boolean, String>, drawer: AppDrawer){
    var isSearching by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<String>() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    appDrawerSubscription.subscribe("search") {if(!it) {
        results.clear()
        query=""
        focusManager.clearFocus()
        keyboardController?.hide()
    } }
    var searchIcon: ImageBitmap? by remember { mutableStateOf(null) }
    var searchMachineInstalled by remember { mutableStateOf("") }
    LaunchedEffect("fetchSearchIcon") {
        val pm = context.packageManager
        listOf(Pair("com.google.android.googlequicksearchbox", "Google")).forEach {
            try {
                val icon = pm.getApplicationIcon(it.first)
                if(icon is AdaptiveIconDrawable){
                    val foreground = icon.monochrome?.apply { setTint(-0x1 /*White*/) } ?: return@forEach
                    val size=128; val padding=50
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGBA_1010102)
                    foreground.setBounds(-padding,-padding,size+padding,size+padding)
                    foreground.draw(Canvas(bitmap))
                    searchIcon = bitmap.asImageBitmap()
                    searchMachineInstalled = it.second
                }
            } catch (_: NameNotFoundException) { }
        }
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
            val curIcon = searchIcon
            if(curIcon!=null) {
                Image(
                    curIcon,
                    "Search Icon",
                    Modifier
                        .size(24.dp)
                        .clickable { startGoogle(GoogleActivity.Main, context) }
                )
            } else {
                Icon(
                    Icons.Rounded.Search,
                    "Search",
                    tint= Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { startGoogle(GoogleActivity.Main, context) }
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
                        fetch("https://www.google.com/complete/search?q=$query&client=chrome").get {response ->
                            response?:return@get
                            val responses = Json.parseToJsonElement(response).jsonArray.getOrNull(1)?.jsonArray?.map { p -> p.jsonPrimitive.content }?: listOf()
                            results.clear()
                            results.addAll(responses)
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
                        startGoogle(GoogleActivity.Search, context, query)
                    }),
                    decorationBox = { content ->
                        if(!isSearching){
                            Text("Search ${if(searchMachineInstalled.isNotEmpty()) "with $searchMachineInstalled" else ""}", fontSize = 18.sp, color = Colors.SECONDARY_FONT)
                        } else {
                            content()
                        }
                    }
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        val allAppResults=drawer.allApps.getOrNull()?.filter { it.label.lowercase().contains(query.lowercase()) }
        if(isSearching && query.isNotEmpty()) {
            if(!allAppResults.isNullOrEmpty()) {
                Spacer(Modifier.height(20.dp))
                val elementWidth = ((LocalConfiguration.current.screenWidthDp * .9 - 30*2 - 13*2)/4).dp
                Text("Installed Apps:", color = Colors.SECONDARY_FONT)
                Row {
                    allAppResults.forEach{
                        AppComposable(it, elementWidth, AppDrawer.defaultDrawable, false, {}, {it.launch(context)}, SingleSubscription())
                    }
                }
            }
            if(results.isNotEmpty()){
                Spacer(Modifier.height(20.dp))
                Text("Search results:", color = Colors.SECONDARY_FONT)
                Column {
                    results.forEach { Text(it, color = Colors.FONT, fontSize = 20.sp, modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            startGoogle(
                                GoogleActivity.Search,
                                context,
                                query = it
                            ); Promise { delay(2000); appDrawerSubscription.send(false) }
                        }
                        .padding(5.dp, 10.dp, 0.dp, 10.dp)) }
                }
            }
        }
    }
}
enum class GoogleActivity{
    Search, Main
}
fun startGoogle(launch: GoogleActivity, context: Context, query: String?=null){
    when(launch){
        GoogleActivity.Main -> {
            val intent=Intent("android.intent.action.MAIN")
            intent.setPackage("com.google.android.googlequicksearchbox")
            intent.addCategory("android.intent.category.INFO")
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
            intent.setClassName("com.google.android.googlequicksearchbox","com.google.android.googlequicksearchbox.GoogleAppImplicitMainInfoGatewayInternal")
            context.startActivity(intent)
        }
        GoogleActivity.Search -> {
            val queryString = query?:"Moin"
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity")
            intent.putExtra("query", queryString)
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
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