package com.danmo.kalimba.sheetlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.kalimba.R
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType
import com.danmo.kalimba.data.SheetMusicRepository
import com.danmo.kalimba.data.local.AuthDataStore
import com.danmo.kalimba.data.local.KalimbaDatabase
import com.danmo.kalimba.data.local.SheetMusicEntity
import com.danmo.kalimba.data.remote.KalimbaApiService
import com.danmo.kalimba.data.remote.SheetMusicDto
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetListScreen(
    onNavigateToEdit: (Long?) -> Unit,
    onNavigateToPractice: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { KalimbaDatabase.getDatabase(context) }
    val authDataStore = remember { AuthDataStore(context) }
    val accessibilityHelper = remember { AccessibilityHelper(context) }

    val repository: SheetMusicRepository = remember {
        val apiService = provideKalimbaApi()
        SheetMusicRepository(db.sheetMusicDao(), apiService, authDataStore)
    }

    var searchQuery by remember { mutableStateOf("") }
    var sheetToDelete by remember { mutableStateOf<SheetMusicEntity?>(null) }
    var sheetToUpload by remember { mutableStateOf<SheetMusicEntity?>(null) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }

    var showCloudBrowser by remember { mutableStateOf(false) }

    val sheetList by remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            db.sheetMusicDao().getAllSheets()
        } else {
            db.sheetMusicDao().searchSheets(searchQuery)
        }
    }.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    // 消息提示对话框
    uploadMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { uploadMessage = null },
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { uploadMessage = null }) {
                    Text("确定")
                }
            }
        )
    }

    // 本地删除确认
    sheetToDelete?.let { sheet ->
        AlertDialog(
            onDismissRequest = { sheetToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除本地的《${sheet.name}》吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            db.sheetMusicDao().deleteSheet(sheet)
                            accessibilityHelper.speak("已删除 ${sheet.name}")
                            sheetToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { sheetToDelete = null }) { Text("取消") }
            }
        )
    }

    // 上传确认
    sheetToUpload?.let { sheet ->
        AlertDialog(
            onDismissRequest = { sheetToUpload = null },
            title = { Text("同步到云端") },
            text = { Text("确定要将《${sheet.name}》备份到您的云端库吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val isLoggedIn = authDataStore.checkIsLoggedIn()
                            if (!isLoggedIn) {
                                sheetToUpload = null
                                onNavigateToLogin()
                            } else {
                                try {
                                    val result = repository.uploadSheet(sheet)
                                    if (result.isSuccess) {
                                        uploadMessage = "上传成功！分享码：${result.getOrNull()?.shareCode}"
                                    } else {
                                        uploadMessage = "上传失败：${result.exceptionOrNull()?.message}"
                                    }
                                } catch (e: Exception) {
                                    uploadMessage = "错误：${e.message}"
                                }
                                sheetToUpload = null
                            }
                        }
                    }
                ) { Text("确认上传") }
            },
            dismissButton = {
                TextButton(onClick = { sheetToUpload = null }) { Text("取消") }
            }
        )
    }

    // 云端浏览器
    if (showCloudBrowser) {
        CloudSheetBrowser(
            repository = repository,
            authDataStore = authDataStore,
            accessibilityHelper = accessibilityHelper,
            onDismiss = { showCloudBrowser = false },
            onNavigateToLogin = onNavigateToLogin,
            onDownloadSuccess = { sheetName ->
                uploadMessage = "已成功下载《$sheetName》"
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("我的简谱库", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(painterResource(R.drawable.ic_arrow_back), "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showCloudBrowser = true }) {
                            Icon(painterResource(R.drawable.ic_cloud_upload), "云端市场")
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索本地谱子...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEdit(null) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("创作简谱") }
            )
        }
    ) { padding ->
        if (sheetList.isEmpty()) {
            EmptyStateDisplay(padding, searchQuery.isNotEmpty())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(sheetList, key = { it.id }) { sheet ->
                    SheetMusicItem(
                        sheet = sheet,
                        onEdit = { onNavigateToEdit(it) },
                        onPractice = { onNavigateToPractice(it) },
                        onDelete = { sheetToDelete = it },
                        onUpload = { sheetToUpload = it },
                        accessibilityHelper = accessibilityHelper
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSheetBrowser(
    repository: SheetMusicRepository,
    authDataStore: AuthDataStore,
    accessibilityHelper: AccessibilityHelper,
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onDownloadSuccess: (String) -> Unit
) {
    var isLoggedIn by remember { mutableStateOf(false) }
    var cloudSheets by remember { mutableStateOf<List<SheetMusicDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shareCodeInput by remember { mutableStateOf("") }

    // 0: 公开广场, 1: 我的云端, 2: 分享码下载
    var selectedTab by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    // 核心加载逻辑
    val loadData = {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = if (selectedTab == 0) {
                    repository.getPublicSheets()
                } else {
                    repository.getMyCloudSheets()
                }

                result.onSuccess { cloudSheets = it }
                    .onFailure { errorMessage = "获取失败: ${it.message}" }
            } catch (e: Exception) {
                errorMessage = "网络错误"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTab) {
        isLoggedIn = authDataStore.checkIsLoggedIn()
        if (selectedTab < 2) { // 广场或个人云端
            if (selectedTab == 1 && !isLoggedIn) {
                // 如果选“我的”但未登录，不加载
            } else {
                loadData()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("云端简谱", fontWeight = FontWeight.Bold)
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("广场") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("我的") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("码下载") })
                }
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                if (selectedTab == 1 && !isLoggedIn) {
                    // 我的云端 - 未登录处理
                    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                        Text("请登录以同步您的云端简谱")
                        Button(onClick = { onDismiss(); onNavigateToLogin() }) { Text("前往登录") }
                    }
                } else if (selectedTab == 2) {
                    // 分享码下载页
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = shareCodeInput,
                            onValueChange = { shareCodeInput = it.uppercase() },
                            label = { Text("输入6位分享码") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isLoading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                } else {
                    // 列表展示（广场或我的）
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else if (cloudSheets.isEmpty()) {
                        Text("暂无数据", Modifier.align(Alignment.Center), color = Color.Gray)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(cloudSheets) { sheet ->
                                CloudSheetItem(sheet) {
                                    scope.launch {
                                        isLoading = true
                                        repository.downloadByShareCode(sheet.shareCode)
                                            .onSuccess {
                                                accessibilityHelper.speak("下载成功")
                                                onDownloadSuccess(it.name)
                                                onDismiss()
                                            }
                                            .onFailure { errorMessage = "下载失败" }
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 2) {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            repository.downloadByShareCode(shareCodeInput)
                                .onSuccess { onDownloadSuccess(it.name); onDismiss() }
                                .onFailure { errorMessage = "无效的分享码" }
                            isLoading = false
                        }
                    },
                    enabled = shareCodeInput.isNotBlank() && !isLoading
                ) { Text("确认下载") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun CloudSheetItem(sheet: SheetMusicDto, onDownload: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sheet.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("作者: ${sheet.author ?: "匿名"} · ${sheet.totalNotes}音符", style = MaterialTheme.typography.bodySmall)
                Text("分享码: ${sheet.shareCode}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDownload) {
                Icon(painterResource(R.drawable.ic_play), "下载", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SheetMusicItem(
    sheet: SheetMusicEntity,
    onEdit: (Long) -> Unit,
    onPractice: (Long) -> Unit,
    onDelete: (SheetMusicEntity) -> Unit,
    onUpload: (SheetMusicEntity) -> Unit,
    accessibilityHelper: AccessibilityHelper
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(40.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_music_note), null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sheet.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${sheet.totalNotes}个音符 · ${if(sheet.isUploaded) "已同步" else "未同步"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(painterResource(R.drawable.ic_cloud_upload), if(sheet.isUploaded) "同步" else "上传", Color(0xFF9C27B0), false) { onUpload(sheet) }
                ActionButton(painterResource(R.drawable.ic_edit), "编辑", Color(0xFF2196F3), false) { onEdit(sheet.id) }
                ActionButton(painterResource(R.drawable.ic_play), "练习", Color(0xFF4CAF50), true) { onPractice(sheet.id) }
                IconButton(onClick = { onDelete(sheet) }) { Icon(painterResource(R.drawable.ic_delete), null, tint = Color.Red) }
            }
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.painter.Painter, label: String, color: Color, isPrimary: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if(isPrimary) color else color.copy(alpha = 0.1f), contentColor = if(isPrimary) Color.White else color),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun EmptyStateDisplay(padding: PaddingValues, isSearching: Boolean) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(if (isSearching) "未找到匹配简谱" else "书架空空如也\n去广场看看或自己创作吧", textAlign = TextAlign.Center, color = Color.Gray)
    }
}

private fun provideKalimbaApi(): KalimbaApiService {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    return Retrofit.Builder()
        .baseUrl("https://guangji.online/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(KalimbaApiService::class.java)
}
