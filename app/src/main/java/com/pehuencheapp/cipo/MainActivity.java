package com.pehuencheapp.cipo;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.View;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.widget.ProgressBar;


import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.ValueCallback;
import android.provider.MediaStore;
import android.os.Environment;
import android.content.ContentValues;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

public class MainActivity extends androidx.appcompat.app.AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private String fcmTokenForWebView = "";
    private static final String WEBSITE_URL = "https://micronauta.dnsalias.net/web/urbano/?conf=PehuencheNQN";
    private SwipeRefreshLayout swipeRefresh;
    private ValueCallback<Uri[]> fileUploadCallback;
    private Uri cameraImageUri;
    private long backPressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set system bar colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int systemBarColor = Color.parseColor("#48E567");
            getWindow().setStatusBarColor(systemBarColor);
            getWindow().setNavigationBarColor(systemBarColor);
        }
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeColors(Color.parseColor("#FFFFFF"));
        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
        });
        
        setupWebView();
        
        
        
        
        // Handle deep link intent
        handleIntent(getIntent());
        
        // Load directly; ConnectivityManager can be unreliable on some devices/VPNs.
        // WebView will show its own error page if the connection is actually unavailable.
        webView.loadUrl(WEBSITE_URL);
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        // Force dark mode following system setting
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_AUTO);
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(webView.getSettings(), WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                // Custom JavaScript injection
                view.evaluateJavascript("window.addEventListener(\'load\', function() {   setInterval(function() {     document.querySelectorAll(\'object\').forEach(function(o) {       try {         var d = o.contentDocument || o.contentWindow.document;         if (d && !d.getElementById(\'custom-map-injected\')) {           var s = d.createElement(\'style\');           s.id = \'custom-map-injected\';           s.innerHTML = `             html, body { background-color: #0f172a !important; }           `;           d.head.appendChild(s);         }       } catch(e) {}     });   }, 1000); });", null);
                // Custom CSS injection via Base64 to avoid quote escaping issues
                String cssStr = "/* 1. Fondo global y tipografía sin romper márgenes */ html, body, #home {    background-color: #0f172a !important;    overflow-y: auto !important;    font-family: system-ui, sans-serif !important;  }  .pagina {    background-color: #0f172a !important;  }  #home .w3-white {    background-color: transparent !important;  }  /* 2. Ocultar botón de instalar */ #btnAyudaInstalar,  #home > div:has([link=\"instalar\"]),  #home [link=\"instalar\"] {    display: none !important;  }  /* 3. Tarjetas del menú principal */ #home .fondo.w3-btn {    background-color: #1e293b !important;    border: 1px solid rgba(255, 255, 255, 0.1) !important;    border-radius: 16px !important;    width: 145px !important;    height: 140px !important;    display: flex !important;    flex-direction: column !important;    justify-content: center !important;    align-items: center !important;  }  #home .fondo.w3-btn img {    width: 48px !important;    height: 48px !important;    margin: 0 0 8px 0 !important;    filter: none !important;  }  #home .fondo.w3-btn .textoicono {    margin: 0 !important;    width: 100% !important;    font-size: 13px !important;    text-align: center !important;    color: #fff !important;  }  #home > div:nth-child(5) {    margin: 0 auto !important;    float: none !important;  }  /* 4. Barra superior y menú lateral */ .U_opciones_menu {    background-color: #1e293b !important;    color: #f8fafc !important;  }  .U_opcione_menu_item {    color: #f8fafc !important;  }  .U_bar_menu_top, .bgMenu {    background-color: #0f172a !important;  }";
                String b64Css = android.util.Base64.encodeToString(cssStr.getBytes(), android.util.Base64.NO_WRAP);
                view.evaluateJavascript("(function(){var s=document.createElement('style');s.textContent=atob('" + b64Css + "');document.head.appendChild(s);})()", null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleWebViewUrl(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null && request.getUrl() != null) {
                    return handleWebViewUrl(view, request.getUrl().toString());
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) progressBar.setProgress(newProgress);
            }
            
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;

                // Camera intent
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "camera_photo");
                cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

                // File chooser intent
                Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                fileIntent.setType("*/*");

                // Combine into chooser
                Intent chooserIntent = Intent.createChooser(fileIntent, "Select file");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

                fileUploadLauncher.launch(chooserIntent);
                return true;
            }
        });

        
    }


    private boolean handleWebViewUrl(WebView view, String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.toLowerCase();
        if (lower.startsWith("tel:") || lower.startsWith("mailto:") || lower.startsWith("sms:") || lower.startsWith("smsto:") || lower.startsWith("whatsapp:") || lower.startsWith("market:") || lower.startsWith("intent:")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                startActivity(intent);
                return true;
            } catch (Exception ignored) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
                    return true;
                } catch (Exception ignoredAgain) {
                    return true;
                }
            }
        }
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return true;
        }
        try {
            java.net.URL baseUrl = new java.net.URL(WEBSITE_URL);
            java.net.URL targetUrl = new java.net.URL(url);
            if (targetUrl.getHost() != null && !targetUrl.getHost().equalsIgnoreCase(baseUrl.getHost())) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                startActivity(browserIntent);
                return true;
            }
        } catch (Exception e) { /* ignore, load in webview */ }
        view.loadUrl(url);
        return true;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            String deepUrl = intent.getData().toString();
            if (deepUrl.startsWith("http")) {
                webView.loadUrl(deepUrl);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private final ActivityResultLauncher<Intent> fileUploadLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (fileUploadCallback == null) return;
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                fileUploadCallback.onReceiveValue(new Uri[]{result.getData().getData()});
            } else if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                fileUploadCallback.onReceiveValue(new Uri[]{cameraImageUri});
            } else {
                fileUploadCallback.onReceiveValue(null);
            }
            fileUploadCallback = null;
        });

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            } else {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish();
                } else {
                    android.widget.Toast.makeText(this, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show();
                    backPressedTime = System.currentTimeMillis();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 999, 0, "Privacy Policy");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 999) {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://pehuenche.com.ar/app-pehuenche.html")));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}