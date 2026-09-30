package com.devbrowser

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var url: EditText
    private val prefs by lazy { getSharedPreferences("devbrowser", MODE_PRIVATE) }
    private val history = mutableListOf<String>()
    private val logs = mutableListOf<String>()
    private val home = "file:///android_asset/home.html"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
        web = findViewById(R.id.webView); url = findViewById(R.id.urlInput)
        loadHistory()
        val s = web.settings
        s.javaScriptEnabled = true; s.domStorageEnabled = true; s.databaseEnabled = true
        s.setGeolocationEnabled(true); s.allowFileAccess = true; s.allowContentAccess = true
        CookieManager.getInstance().setAcceptCookie(true)
        web.addJavascriptInterface(Bridge(), "DevBrowser")
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView?, u: String?, f: android.graphics.Bitmap?) { url.setText(u ?: "") }
            override fun onPageFinished(v: WebView?, u: String?) { url.setText(u ?: ""); u?.let { saveUrl(it) }; injectConsole() }
            override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean { return false }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(c: ConsoleMessage): Boolean { addLog("${c.messageLevel()}: ${c.message()} (${c.lineNumber()})"); return true }
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) callback?.invoke(origin, true, false)
                else { pendingGeo = callback to origin; ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 41) }
            }
        }
        web.loadUrl(home)
        findViewById<Button>(R.id.goButton).setOnClickListener { navigate(url.text.toString()) }
        findViewById<Button>(R.id.backButton).setOnClickListener { if (web.canGoBack()) web.goBack() }
        findViewById<Button>(R.id.forwardButton).setOnClickListener { if (web.canGoForward()) web.goForward() }
        findViewById<Button>(R.id.reloadButton).setOnClickListener { web.reload() }
        findViewById<Button>(R.id.homeButton).setOnClickListener { web.loadUrl(home) }
        findViewById<Button>(R.id.sitesButton).setOnClickListener { showSites() }
        findViewById<Button>(R.id.consoleButton).setOnClickListener { showConsole() }
        findViewById<Button>(R.id.gpsButton).setOnClickListener { showGps() }
        findViewById<Button>(R.id.dataButton).setOnClickListener { clearDataDialog() }
    }

    private var pendingGeo: Pair<GeolocationPermissions.Callback?, String?>? = null
    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) { super.onRequestPermissionsResult(r,p,g); if(r==41){ val ok=g.any{it==PackageManager.PERMISSION_GRANTED}; pendingGeo?.let{it.first?.invoke(it.second,ok,false)}; pendingGeo=null } }
    private fun navigate(raw:String){ var u=raw.trim(); if(u.isEmpty()) return; if(!u.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) u="https://$u"; web.loadUrl(u) }
    private fun injectConsole(){ web.evaluateJavascript("""(function(){if(window.__devHook)return;window.__devHook=1;['log','warn','error','info','debug'].forEach(function(k){let o=console[k];console[k]=function(){try{DevBrowser.log(k.toUpperCase()+': '+Array.from(arguments).map(x=>typeof x==='object'?JSON.stringify(x):String(x)).join(' '))}catch(e){};o.apply(console,arguments)}})})()""",null) }
    private fun addLog(s:String){ logs.add("${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}  $s"); if(logs.size>500) logs.removeAt(0) }
    private fun loadHistory(){ val a=JSONArray(prefs.getString("history","[]")); for(i in 0 until a.length()) history.add(a.getString(i)) }
    private fun saveUrl(u:String){ if(u.startsWith("file://")) return; history.remove(u); history.add(0,u); while(history.size>30) history.removeLast(); prefs.edit().putString("history",JSONArray(history).toString()).apply() }
    private fun showSites(){ val names=history.toTypedArray(); if(names.isEmpty()){Toast.makeText(this,"Todavía no hay sitios visitados",Toast.LENGTH_SHORT).show();return}; AlertDialog.Builder(this).setTitle("Sitios recientes").setItems(names){_,w->web.loadUrl(names[w])}.setNegativeButton("Cerrar",null).show() }
    private fun showConsole(){ val text=if(logs.isEmpty())"Sin mensajes todavía." else logs.joinToString("\n"); val box=ScrollView(this); val tv=TextView(this); tv.text=text; tv.textSize=12f; tv.setTextIsSelectable(true); tv.setPadding(24,16,24,16); box.addView(tv); AlertDialog.Builder(this).setTitle("Consola").setView(box).setNeutralButton("Copiar"){_,_-> val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText("Dev Browser Console",text));Toast.makeText(this,"Consola copiada",Toast.LENGTH_SHORT).show() }.setNegativeButton("Borrar"){_,_->logs.clear()}.setPositiveButton("Cerrar",null).show() }
    private fun showGps(){ val fine=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED; val coarse=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED; val t="Permiso fino: ${if(fine)"CONCEDIDO" else "NO"}\nPermiso aproximado: ${if(coarse)"CONCEDIDO" else "NO"}\n\nLa página puede solicitar geolocalización mediante navigator.geolocation."; AlertDialog.Builder(this).setTitle("Diagnóstico GPS").setMessage(t).setPositiveButton("Solicitar permiso"){_,_->ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),41)}.setNegativeButton("Cerrar",null).show() }
    private fun clearDataDialog(){ AlertDialog.Builder(this).setTitle("Limpiar datos").setMessage("Esto borrará caché, cookies, LocalStorage, Web SQL/IndexedDB y datos del WebView. Los sitios guardados en la lista NO se borrarán.").setPositiveButton("LIMPIAR TODO"){_,_->CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();web.clearCache(true);web.clearHistory();web.clearFormData();web.clearSslPreferences();web.evaluateJavascript("localStorage.clear();sessionStorage.clear();indexedDB.databases().then(ds=>ds.forEach(d=>indexedDB.deleteDatabase(d.name)))",null);Toast.makeText(this,"Datos del sitio limpiados",Toast.LENGTH_SHORT).show();web.reload()}.setNegativeButton("Cancelar",null).show() }
    inner class Bridge { @JavascriptInterface fun log(s:String){runOnUiThread{addLog(s)}} }
}
