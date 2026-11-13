package com.thebifel.kadr

import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

private val Lime=Color(0xFFD2F680)
private val Bg=Color(0xFF101410)
private val Panel=Color(0xFF191F17)
private val Border=Color(0xFF36412F)
private val Muted=Color(0xFF9DAA91)
private val Ink=Color(0xFFEDF1E5)

@Composable fun KadrTheme(content: @Composable ()->Unit) {
    MaterialTheme(colorScheme=darkColorScheme(primary=Lime,onPrimary=Color(0xFF203014),background=Bg,surface=Panel,onSurface=Ink,onBackground=Ink,outline=Border,secondary=Color(0xFFB4CB96)),content=content)
}

class StudioModel: ViewModel() {
    val drafts=mutableMapOf<String,MutableState<Project>>()
}

class MainActivity: ComponentActivity() {
    private val studioModel by lazy { ViewModelProvider(this)[StudioModel::class.java] }
    private var incomingProject by mutableStateOf<String?>(null)
    private lateinit var repo: Repository
    private var imported by mutableStateOf<String?>(null)
    private var exportHtml=""
    private val openFile=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null)runCatching { contentResolver.openInputStream(uri)!!.use { stream -> val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=stream.read(buffer);if(n<0)break;require(out.size()+n<=500_000){"Сторінка завелика"};out.write(buffer,0,n)};out.toString("UTF-8") } }.onSuccess { imported=it }.onFailure { message(it.message ?: "Помилка файлу") }
    }
    private val saveFile=registerForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        if(uri!=null)runCatching { contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(exportHtml) } }.onSuccess { message("HTML збережено") }.onFailure { message("Не вдалося записати файл") }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);repo=Repository(this);incomingProject=intent.getStringExtra("project")
        val first=repo.ensure()
        Schedule.reconcile(this@MainActivity,false)
        if(repo.result(first.id).success==0L)Schedule.now(this@MainActivity,first)
        setContent { KadrTheme { Studio() } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent);setIntent(intent);incomingProject=intent.getStringExtra("project") }
    private fun message(text: String) { Toast.makeText(this,text,Toast.LENGTH_SHORT).show() }

    @Composable private fun Studio() {
        var selected by rememberSaveable { mutableStateOf(intent.getStringExtra("project") ?: repo.ensure().id) }
        var project by remember(selected) { studioModel.drafts.getOrPut(selected) { mutableStateOf(repo.get(selected) ?: repo.ensure()) } }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var dirty by rememberSaveable(selected) { mutableStateOf(false) }
        var frame by remember(selected) { mutableStateOf(repo.result(project.id)) }
        var listDialog by remember { mutableStateOf(false) }
        var templates by remember { mutableStateOf(false) }
        var menu by remember { mutableStateOf(false) }
        var requested by remember(selected) { mutableLongStateOf(0) }
        var help by remember { mutableStateOf(false) }
        val scroll=rememberScrollState()
        val image=remember(frame.file) { repo.bitmap(project.id)?.asImageBitmap() }
        fun edit(next: Project) { project=next;dirty=true }
        fun save(render: Boolean=true) {
            val current=repo.get(project.id)
            project=project.copy(revision=(current?.revision ?: 0)+1)
            repo.save(project);Schedule.configure(this@MainActivity,project);dirty=false
            if(render) { requested=System.currentTimeMillis();Schedule.now(this@MainActivity,project,replace=true) }
        }
        fun choose(next: Project) { if(dirty)save(false);selected=next.id;tab=0 }
        LaunchedEffect(incomingProject) { incomingProject?.let { id -> if(id!=selected && repo.get(id)!=null) { if(dirty)save(false);selected=id } } }
        LaunchedEffect(selected) {
            while(true) { frame=repo.result(project.id);delay(750) }
        }
        LaunchedEffect(imported) { imported?.let { edit(project.copy(html=it));tab=0;imported=null;message("Сторінку імпортовано. Натисни «Побудувати кадр».") } }
        val waiting=requested>0 && frame.attempt<requested
        Surface(Modifier.fillMaxSize(),color=Bg) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("▦ кадр",color=Lime,fontSize=27.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={listDialog=true}) { Text("Мої віджети ▾",fontSize=12.sp) }
                    Box { IconButton(onClick={menu=true}) { Text("⋮",fontSize=26.sp,color=Muted) };DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                        DropdownMenuItem(text={Text("Імпортувати HTML")},onClick={menu=false;openFile.launch(arrayOf("text/html","text/plain","application/xhtml+xml"))})
                        DropdownMenuItem(text={Text("Експортувати HTML")},onClick={menu=false;exportHtml=project.html;saveFile.launch("kadr.html")})
                        DropdownMenuItem(text={Text("Контракт Kadr SDK")},onClick={menu=false;help=true})
                    } }
                }
                HorizontalDivider(color=Border)
                Column(Modifier.weight(1f).verticalScroll(scroll).padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text("ПРОГРАМОВАНИЙ ВІДЖЕТ",fontSize=10.sp,color=Muted,letterSpacing=2.sp)
                        Text(project.name,fontSize=29.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.8).sp)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("HTML  ·  CSS  ·  JavaScript",color=Lime,fontSize=11.sp,fontFamily=FontFamily.Monospace)
                            Spacer(Modifier.weight(1f));Text(if(dirty)"Є зміни" else "● Збережено",color=if(dirty)Color(0xFFD5B785) else Muted,fontSize=10.sp)
                        }
                    }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF1C2419)).border(1.dp,Border,RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(17.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text("ПОПЕРЕДНІЙ ПЕРЕГЛЯД",fontSize=9.sp,color=Muted,letterSpacing=1.5.sp);Spacer(Modifier.weight(1f));Text("4 × 2",fontSize=10.sp,color=Lime,fontFamily=FontFamily.Monospace) }
                        val cardBg=Color(if(project.dark)0xFF222B1B else 0xFFF2F4E9)
                        val cardMuted=Color(if(project.dark)0xFFB8C9A4 else 0xFF69765B)
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(project.radius.dp)).background(cardBg)) {
                            if(image!=null)Image(image,"Останній успішний кадр",Modifier.fillMaxWidth().height(158.dp),contentScale=ContentScale.Fit)
                            else Box(Modifier.fillMaxWidth().height(158.dp),contentAlignment=Alignment.Center) { Text(if(waiting)"Будуємо перший кадр…" else "Побудуй свій перший кадр",fontSize=13.sp,color=cardMuted) }
                            Row(Modifier.fillMaxWidth().height(48.dp).padding(start=14.dp),verticalAlignment=Alignment.CenterVertically) {
                                Text("КАДР",color=cardMuted,fontSize=9.sp,letterSpacing=1.sp)
                                Spacer(Modifier.weight(1f))
                                Text(if(frame.success>0)"${if(frame.error.isEmpty())"Оновлено" else "Збій ·"} ${WidgetUi.stamp(frame.success)}" else "Ще немає кадру",color=cardMuted,fontSize=10.sp)
                                IconButton(onClick={if(dirty)save() else {requested=System.currentTimeMillis();Schedule.now(this@MainActivity,project)}},enabled=!waiting,modifier=Modifier.semantics { contentDescription="Оновити кадр" }) { Text("↻",fontSize=26.sp,color=Color(0xFF7E9C4D)) }
                            }
                        }
                        Text("Статичний кадр · без скролу й анімацій",fontSize=10.sp,color=Muted)
                    }
                    if(waiting)Text("Запит на оновлення надіслано…",color=Lime,fontSize=11.sp)
                    else if(frame.error.isNotEmpty())Notice("${frame.error}\nОстанній успішний кадр збережено.",warning=true)
                    else if(frame.success>0)Row(verticalAlignment=Alignment.CenterVertically) { Text("●",color=Lime,fontSize=9.sp);Spacer(Modifier.width(8.dp));Text("Кадр готовий · WebView закрито",fontSize=11.sp,color=Muted) }
                    TabRow(selectedTabIndex=tab,containerColor=Bg,contentColor=Lime,divider={HorizontalDivider(color=Border)}) {
                        listOf("Вміст","Вигляд","Оновлення").forEachIndexed { index,title -> Tab(selected=tab==index,onClick={tab=index},text={Text(title,fontSize=12.sp)},selectedContentColor=Lime,unselectedContentColor=Muted) }
                    }
                    when(tab) {
                        0 -> {
                            Text("Твоя сторінка. Твоя логіка.",fontSize=17.sp,fontWeight=FontWeight.Medium)
                            Text("Отримай дані через fetch, побудуй композицію й виклич Kadr.ready().",fontSize=12.sp,color=Muted,lineHeight=19.sp)
                            OutlinedTextField(value=project.name,onValueChange={if(it.length<=80)edit(project.copy(name=it))},label={Text("Назва віджета")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text("index.html",fontSize=12.sp,color=Lime,fontFamily=FontFamily.Monospace);Spacer(Modifier.weight(1f));TextButton(onClick={templates=true}) { Text("Почати з шаблону",fontSize=11.sp) } }
                            OutlinedTextField(value=project.html,onValueChange={if(it.length<=500_000)edit(project.copy(html=it))},modifier=Modifier.fillMaxWidth().height(320.dp).semantics { contentDescription="HTML CSS JavaScript" },textStyle=LocalTextStyle.current.copy(fontFamily=FontFamily.Monospace,fontSize=11.sp,lineHeight=18.sp,color=Color(0xFFC9DFB5)),keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.None,autoCorrectEnabled=false,keyboardType=KeyboardType.Ascii),shape=RoundedCornerShape(10.dp))
                            Notice("Vue вже включений: https://kadr.local/vue.global.prod.js\nОновлюй DOM, потім Vue.nextTick(() => Kadr.ready()).")
                        }
                        1 -> {
                            Text("Оформлення картки",fontSize=17.sp,fontWeight=FontWeight.Medium)
                            Text("Сторінка отримує тему та CSS-кольори через Kadr SDK.",fontSize=12.sp,color=Muted,lineHeight=19.sp)
                            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                FilterChip(selected=!project.dark,onClick={edit(project.copy(dark=false))},label={Text("◻ Світла")})
                                FilterChip(selected=project.dark,onClick={edit(project.copy(dark=true))},label={Text("◼ Темна")})
                            }
                            Text("Заокруглення",fontSize=12.sp,color=Muted)
                            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { listOf(0,16,24).forEach { r -> FilterChip(selected=project.radius==r,onClick={edit(project.copy(radius=r))},label={Text("$r dp")}) } }
                            Notice("Час і ↻ завжди поруч у нижньому правому кутку. Вони залишаються доступними незалежно від коду сторінки.")
                        }
                        2 -> {
                            Text("Коли перебудовувати кадр?",fontSize=17.sp,fontWeight=FontWeight.Medium)
                            Text("Бажаний інтервал",fontSize=12.sp,color=Muted)
                            Column {
                                listOf(listOf(0,15,30,60),listOf(180,360,1440)).forEach { row -> Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { row.forEach { minutes -> FilterChip(selected=project.interval==minutes,onClick={edit(project.copy(interval=minutes))},label={Text(intervalText(minutes),fontSize=11.sp)}) } } }
                            }
                            ToggleRow("Лише через Wi-Fi","Фонові й ручні запити чекають мережі без тарифікації.",project.wifi) { edit(project.copy(wifi=it)) }
                            ToggleRow("Берегти низький заряд","Фонове оновлення відкладається. Ручне доступне.",project.battery) { edit(project.copy(battery=it)) }
                            Notice("Android може відкласти роботу через енергозбереження. 15 хвилин — мінімальний період, а не точний таймер. Час на картці показує останній успішний результат.")
                            Text("Після перезавантаження",fontSize=14.sp,fontWeight=FontWeight.Medium)
                            Text("Кадр зберігається на пристрої. Після запуску й розблокування системи відновлюються показ і розклад. Примусова зупинка застосунку — окремий випадок.",fontSize=12.sp,color=Muted,lineHeight=20.sp)
                        }
                    }
                    Button(onClick={save()},modifier=Modifier.fillMaxWidth().height(50.dp),shape=RoundedCornerShape(10.dp)) { Text(if(waiting)"Зберегти й повторити оновлення  ↗" else "Зберегти й побудувати кадр  ↗",fontSize=13.sp,fontWeight=FontWeight.SemiBold) }
                    OutlinedButton(onClick={if(dirty)save(false);val mgr=AppWidgetManager.getInstance(this@MainActivity);if(mgr.isRequestPinAppWidgetSupported){getSharedPreferences("kadr",MODE_PRIVATE).edit().putString("pinProject",project.id).commit();val pinIntent=Intent(this@MainActivity,PinReceiver::class.java).putExtra("project",project.id).setData(android.net.Uri.parse("kadr://pin/${project.id}"));val callback=PendingIntent.getBroadcast(this@MainActivity,project.id.hashCode(),pinIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE);mgr.requestPinAppWidget(ComponentName(this@MainActivity,KadrWidget::class.java),null,callback)}else message("Додай «Кадр» через меню віджетів домашнього екрана")},modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(10.dp)) { Text("Додати на домашній екран",fontSize=12.sp) }
                    Text("КАДР / 0.1    ·    HTML → СТАТИЧНИЙ КАДР",color=Muted,fontFamily=FontFamily.Monospace,fontSize=9.sp,modifier=Modifier.padding(vertical=6.dp))
                }
            }
        }
        if(listDialog)AlertDialog(onDismissRequest={listDialog=false},title={Text("Мої віджети")},text={Column { repo.all().forEach { p -> TextButton(onClick={choose(p);listDialog=false}) { Text((if(p.id==project.id)"● " else "")+p.name) } } }},confirmButton={TextButton(onClick={if(dirty)save(false);val next=Project(name="Новий кадр",html=Templates.blank);repo.save(next);choose(next);Schedule.now(this@MainActivity,next);listDialog=false}) { Text("＋ Новий віджет") }},dismissButton={TextButton(onClick={listDialog=false}){Text("Закрити")}})
        if(templates)AlertDialog(onDismissRequest={templates=false},title={Text("Почати з шаблону")},text={Column { Text("Створюється окремий проєкт. Поточний код збережеться.",fontSize=12.sp,color=Muted);listOf("Світло · Vue-приклад" to Templates.power,"Погода · живе API" to Templates.weather,"Своя HTML-сторінка" to Templates.blank).forEach { (name,html) -> TextButton(onClick={if(dirty)save(false);val next=Project(name=name,html=html);repo.save(next);choose(next);Schedule.configure(this@MainActivity,next);Schedule.now(this@MainActivity,next);templates=false}) { Text(name) } } }},confirmButton={TextButton(onClick={templates=false}){Text("Закрити")}})
        if(help)AlertDialog(onDismissRequest={help=false},title={Text("Сторінка → кадр")},text={Text("Код виконується лише під час оновлення.\n\nKadr.ready() — кадр готовий.\nKadr.fail(message) — залишити попередній.\nKadr.theme — light або dark.\nKadr.width / height — розмір у CSS px.\n\nCSS: --kadr-bg, --kadr-fg, --kadr-muted, --kadr-accent.\n\nfetch працює через HTTPS за правилами CORS. За 15 секунд сторінка має завершити побудову. DOM-кнопки на знімку не клікабельні.",fontSize=13.sp,lineHeight=20.sp)},confirmButton={TextButton(onClick={help=false}){Text("Зрозуміло")}})
    }
}

fun intervalText(value: Int)=when { value==0 -> "Вручну";value==1440->"1 доба";value>=60->"${value/60} год";else->"$value хв" }
@Composable private fun Notice(text: String,warning: Boolean=false) {
    Text(text,fontSize=11.sp,lineHeight=18.sp,color=if(warning)Color(0xFFE4BA91) else Color(0xFFB8CBA3),modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if(warning)Color(0xFF34291D) else Color(0xFF242F1B)).border(1.dp,if(warning)Color(0xFF59442B) else Border,RoundedCornerShape(10.dp)).padding(14.dp))
}
@Composable private fun ToggleRow(title: String,description: String,checked: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) { Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) { Text(title,fontSize=13.sp);Text(description,fontSize=11.sp,color=Muted,lineHeight=17.sp) };Switch(checked=checked,onCheckedChange=onChange) }
}

class ConfigureActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED)
        if(id==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return}
        val repo=Repository(this);repo.ensure()
        val preferred=getSharedPreferences("kadr",MODE_PRIVATE).getString("pinProject",null)
        setContent { KadrTheme { Surface(Modifier.fillMaxSize(),color=Bg) { Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Text("▦ кадр",fontSize=31.sp,color=Lime,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(10.dp));Text("Сторінка для віджета",fontSize=26.sp,fontWeight=FontWeight.SemiBold)
            Text("Обери проєкт. На домашньому екрані буде його останній успішний кадр.",color=Muted,fontSize=13.sp,lineHeight=21.sp)
            repo.all().sortedByDescending { it.id==preferred }.forEach { project -> OutlinedButton(onClick={val p=repo.cloneForBinding(project);repo.bind(id,p.id);Schedule.configure(this@ConfigureActivity,p);WidgetUi.update(this@ConfigureActivity,id);Schedule.now(this@ConfigureActivity,p);setResult(RESULT_OK,Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish()},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp),shape=RoundedCornerShape(12.dp)) { Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(5.dp)) { Text(project.name,color=Ink,fontSize=15.sp);Text("HTML + CSS + JavaScript · ${intervalText(project.interval)}",color=Muted,fontSize=11.sp) } } }
        } } } }
    }
}
