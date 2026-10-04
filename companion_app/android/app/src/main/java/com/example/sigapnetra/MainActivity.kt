package com.example.sigapnetra

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // WAJIB: Tanpa ini env(safe-area-inset-bottom) bernilai nol
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.allowUniversalAccessFromFileURLs = false
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: android.webkit.JsPromptResult?
                ): Boolean {
                    val input = android.widget.EditText(this@MainActivity)
                    input.setText(defaultValue)
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle(message ?: "Masukkan Nilai")
                        .setView(input)
                        .setPositiveButton("OK") { _, _ ->
                            result?.confirm(input.text.toString())
                        }
                        .setNegativeButton("Batal") { _, _ ->
                            result?.cancel()
                        }
                        .setOnCancelListener {
                            result?.cancel()
                        }
                        .show()
                    return true
                }

                override fun onJsAlert(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: android.webkit.JsResult?
                ): Boolean {
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setMessage(message ?: "")
                        .setPositiveButton("OK") { _, _ -> result?.confirm() }
                        .setOnCancelListener { result?.cancel() }
                        .show()
                    return true
                }

                override fun onJsConfirm(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: android.webkit.JsResult?
                ): Boolean {
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setMessage(message ?: "")
                        .setPositiveButton("Ya") { _, _ -> result?.confirm() }
                        .setNegativeButton("Batal") { _, _ -> result?.cancel() }
                        .setOnCancelListener { result?.cancel() }
                        .show()
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Panggil inisialisasi awal ke JavaScript begitu WebView siap
                    evaluateJavascript("if (window.Sigap) { Sigap.setelTema('light'); Sigap.muatBeranda(); Sigap.muatAntrean('SEMUA'); Sigap.muatRiwayat('SEMUA', 60); Sigap.muatUji(); Sigap.ujiKoneksi(); }", null)
                }
            }

            addJavascriptInterface(JembatanSigap(this@MainActivity, this), "Sigap")
            loadUrl("file:///android_asset/sigap.html")
        }

        setContentView(webView)

        // Penanganan tombol kembali perangkat (Hardware back button)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("if (typeof tumpukan !== 'undefined' && tumpukan.length > 1) { kembali(); 'sub_back'; } else { 'top_exit'; }") { result ->
                    if (result != null && result.contains("top_exit")) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }
}
