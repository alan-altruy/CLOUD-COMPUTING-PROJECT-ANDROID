package be.atr_corp.imagesearchengine

import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import be.atr_corp.imagesearchengine.ui.theme.ImageSearchEngineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ImageSearchEngineTheme {
                ImageSearchEngineApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun ImageSearchEngineApp() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        WebScreen(
            url = "https://groupe12.tp-cloud.deepilia.com",
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun WebScreen(url: String, modifier: Modifier = Modifier) {

    val context = androidx.compose.ui.platform.LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(if (uris.isNotEmpty()) uris.toTypedArray() else null)
        filePathCallback = null
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->

            val swipe = androidx.swiperefreshlayout.widget.SwipeRefreshLayout(ctx)

            val webView = WebView(ctx).apply {
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        callback: ValueCallback<Array<Uri>>?,
                        params: FileChooserParams?
                    ): Boolean {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = callback
                        
                        val mimeTypes = params?.acceptTypes?.filter { it.isNotBlank() }?.toTypedArray()
                        filePickerLauncher.launch(mimeTypes ?: arrayOf("*/*"))
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {

                    override fun onPageFinished(view: WebView?, url: String?) {
                        CookieManager.getInstance().flush()
                        swipe.isRefreshing = false

                        // 🎨 Theme injection (light/dark)
                        val theme = if (isDarkTheme) "dark" else "light"

                        view?.evaluateJavascript(
                            """
                            document.documentElement.setAttribute('data-theme', '$theme');
                            """.trimIndent(),
                            null
                        )
                    }
                }

                loadUrl(url)
            }

            // 🔄 Swipe to refresh
            swipe.addView(webView)

            swipe.setOnRefreshListener {
                webView.reload()
            }

            swipe
        }
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ImageSearchEngineTheme {
        Greeting("Android")
    }
}