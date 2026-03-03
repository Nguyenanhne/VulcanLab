# VulcanLab - Double Decode App

This Android application demonstrates a double decode mechanism for Base64URL and Caesar cipher decoding.

## Project Structure

The project has been refactored to separate components into different files for better maintainability:

### 📁 File Structure
```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt                          # Main Activity (Entry point)
├── utils/
│   └── DecodeUtils.kt                       # Utility functions for decoding
└── ui/
    ├── components/
    │   ├── DecodeStepCard.kt               # Card component for decode steps
    │   └── FinalKeyCard.kt                 # Card component for final result
    ├── screens/
    │   ├── DoubleDecodeViewModel.kt        # ViewModel: StateFlow + SharedFlow
    │   └── DoubleDecodeScreen.kt           # Main screen composable
    └── theme/
        └── ...                             # Theme configuration
```

### 🔧 Components Overview

#### **MainActivity.kt**
- Entry point of the application
- Sets up the main theme and navigation
- Clean and minimal, only responsible for app initialization

#### **utils/DecodeUtils.kt**
Contains utility functions for decoding operations:
- `decodeBase64Url()` - Decodes Base64URL strings
- `caesarDecode()` - Performs Caesar cipher decoding
- `extractCipherText()` - Extracts cipher text from instruction
- `extractShift()` - Extracts shift value from instruction

#### **ui/components/DecodeStepCard.kt**
- Reusable card component for displaying decode steps
- Shows intermediate results with proper styling

#### **ui/components/FinalKeyCard.kt**
- Specialized card component for displaying the final decoded key
- Highlighted with primary theme colors

#### **ui/screens/DoubleDecodeScreen.kt**
- Main screen chứa toàn bộ giao diện decode
- Subscribe `StateFlow` qua `collectAsState()` để tự recompose khi state thay đổi
- Collect `SharedFlow` qua `LaunchedEffect` để xử lý one-time events (Snackbar)

#### **ui/screens/DoubleDecodeViewModel.kt**
- Quản lý toàn bộ business logic và state management
- **`StateFlow<DoubleDecodeUiState>`**: giữ UI state (input, kết quả từng bước, error)
- **`SharedFlow<DoubleDecodeEvent>`**: emit các sự kiện một lần (DecodeSuccess, DecodeFailure, InputCleared)

### ✨ Features

1. **Real-time Decoding**: Input is processed automatically as you type
2. **Step-by-step Visualization**: Shows each decode step clearly
3. **Error Handling**: Displays appropriate error messages for invalid inputs
4. **Hot Reload**: Support for Android development with instant preview
5. **Theme Support**: Uses Material Design 3 theming

### 🚀 Usage

1. Enter a Base64URL encoded string in the input field
2. The app will automatically:
   - Decode the Base64URL to get instructions
   - Extract the cipher text from the instructions
   - Decode the cipher text using Base64URL
   - Apply Caesar cipher decoding with the specified shift
   - Display the final key

---

## B. Android Practical Experience

### 🔍 If an Android app crashes on a user's device but cannot be reproduced on your device, what steps would you take to find the root cause?

Khi một ứng dụng Android gặp sự cố trên thiết bị của người dùng nhưng không thể tái hiện trên thiết bị của tôi, đây là các bước tôi sẽ thực hiện:

#### 1. **Thu thập thông tin từ người dùng**
- Yêu cầu thông tin chi tiết: Model thiết bị, phiên bản Android, RAM/Storage còn trống
- Hỏi các bước cụ thể dẫn đến crash
- Kiểm tra xem crash có xảy ra nhất quán hay ngẫu nhiên

#### 2. **Sử dụng Firebase Crashlytics hoặc công cụ tương tự**
```gradle
// build.gradle.kts
implementation("com.google.firebase:firebase-crashlytics-ktx")
```
- Tích hợp Crashlytics để tự động thu thập crash reports
- Phân tích stack traces và error logs từ production
- Xem crash frequency, affected devices, và OS versions

#### 3. **Kiểm tra logs từ xa**
- Sử dụng `adb logcat` nếu có thể kết nối thiết bị từ xa
- Implement custom logging với các công cụ như Timber hoặc Logcat
- Tạo debug build cho người dùng để thu thập logs chi tiết hơn

#### 4. **Phân tích các điểm khác biệt**
- **API Level**: Kiểm tra compatibility với các phiên bản Android khác nhau
- **Hardware**: RAM thấp, CPU khác biệt, screen size/density
- **Locale & Language**: Vấn đề với encoding hoặc formatting
- **Network conditions**: Slow/unstable connections
- **Storage**: Insufficient space

#### 5. **Sử dụng Android Emulator với cấu hình tương tự**
```bash
# Tạo emulator với specs giống thiết bị người dùng
avdmanager create avd -n user_device_clone -k "system-images;android-30;google_apis;x86_64"
```
- Cấu hình RAM, screen resolution giống thiết bị gặp lỗi
- Test với các API levels khác nhau

#### 6. **Kiểm tra các vấn đề phổ biến**
- **Memory leaks**: Sử dụng LeakCanary
- **ANR (Application Not Responding)**: Kiểm tra operations trên Main Thread
- **Null pointer exceptions**: Thêm null-safety checks
- **Resource không tồn tại**: Missing resources cho configurations khác
- **Permissions**: Runtime permissions không được handle đúng

#### 7. **Beta Testing & Staged Rollouts**
- Sử dụng Google Play Console's staged rollouts (1% → 10% → 50% → 100%)
- Thu thập feedback từ beta testers với nhiều thiết bị khác nhau
- Monitor crash rates trước khi rollout toàn bộ

---

### 💡 While working on this demo task: At which step did you face confusion or errors? How did you debug and verify your solution?

#### **Bước 1: Hiểu yêu cầu đề bài - Confusion với Base64URL**
**Vấn đề gặp phải:**
- Ban đầu tôi không phân biệt được Base64 standard và Base64URL
- Thử dùng `Base64.decode()` trực tiếp nhưng gặp lỗi `IllegalArgumentException`

**Debug process:**
```kotlin
// ❌ Cách sai - không handle Base64URL format
fun decodeBase64(input: String): String {
    val decodedBytes = Base64.decode(input, Base64.DEFAULT)  // Crash với URL-safe chars
    return String(decodedBytes, Charsets.UTF_8)
}
```

**Giải pháp:**
- Research về Base64URL format: sử dụng `-` và `_` thay vì `+` và `/`
- Padding có thể bị bỏ đi trong Base64URL
- Implement conversion từ Base64URL sang Base64 standard:

```kotlin
// ✅ Cách đúng - convert Base64URL to standard Base64
fun decodeBase64Url(input: String): String {
    // Add padding nếu thiếu
    val paddedInput = when (input.length % 4) {
        2 -> "$input=="
        3 -> "$input="
        else -> input
    }
    // Convert URL-safe chars to standard Base64
    val base64Standard = paddedInput
        .replace('-', '+')
        .replace('_', '/')
    
    val decodedBytes = Base64.decode(base64Standard, Base64.DEFAULT)
    return String(decodedBytes, Charsets.UTF_8)
}
```

**Verification:**
```kotlin
// Test với input mẫu
val input = "QmFzZTY0VVJMLWRlY29kZSAi..."
println(decodeBase64Url(input))  // ✅ Success!
```

---

#### **Bước 2: Parse instruction string - Regex extraction errors**
**Vấn đề gặp phải:**
- Cần extract `"eXhvZmRx"` và `shift=3` từ instruction string
- Ban đầu dùng `split()` nhưng không robust với format khác nhau

**Debug process:**
```kotlin
// ❌ Approach đầu tiên - quá đơn giản
fun extractCipherText(instruction: String): String? {
    val parts = instruction.split("\"")  // Không handle edge cases
    return if (parts.size >= 2) parts[1] else null
}
```

**Giải pháp:**
- Sử dụng Regex để extract chính xác hơn:

```kotlin
// ✅ Sử dụng Regex cho độ chính xác cao
fun extractCipherText(instruction: String): String? {
    val regex = "\"([^\"]+)\"".toRegex()
    return regex.find(instruction)?.groupValues?.get(1)
}

fun extractShift(instruction: String): Int? {
    val regex = "shift\\s*=\\s*(\\d+)".toRegex(RegexOption.IGNORE_CASE)
    return regex.find(instruction)?.groupValues?.get(1)?.toIntOrNull()
}
```

**Verification:**
- Test với nhiều formats:
```kotlin
extractCipherText("decode \"eXhvZmRx\" to get")  // ✅ "eXhvZmRx"
extractShift("shift=3")      // ✅ 3
extractShift("shift = 5")    // ✅ 5 (handle spaces)
```

---

#### **Bước 3: Caesar Cipher Decode - Off-by-one error**
**Vấn đề gặp phải:**
- Caesar decode ban đầu cho kết quả sai
- Nhầm lẫn giữa encode và decode (shift direction)

**Debug process:**
```kotlin
// ❌ Cách sai - shift theo hướng ngược lại
fun caesarDecode(input: String, shift: Int): String {
    return input.map { char ->
        when {
            char.isLetter() -> {
                val base = if (char.isUpperCase()) 'A' else 'a'
                val shifted = (char - base + shift) % 26  // ❌ Should be minus
                (base + shifted)
            }
            else -> char
        }
    }.joinToString("")
}
```

**Giải pháp:**
- Caesar DECODE phải subtract shift, không phải add
- Handle negative values với `+ 26`:

```kotlin
// ✅ Correct implementation
fun caesarDecode(input: String, shift: Int): String {
    return input.map { char ->
        when {
            char.isLetter() -> {
                val base = if (char.isUpperCase()) 'A' else 'a'
                val shifted = (char - base - shift + 26) % 26  // ✅ Subtract shift
                (base + shifted)
            }
            else -> char
        }
    }.joinToString("")
}
```

**Verification:**
```kotlin
caesarDecode("abc", 1)  // Expected: "zab" ✅
caesarDecode("xyz", 3)  // Expected: "uvw" ✅
```

---

#### **Bước 4: State Management — Chuyển từ Compose State sang ViewModel + StateFlow + SharedFlow**
**Vấn đề gặp phải:**
- Ban đầu dùng `remember { mutableStateOf() }` trực tiếp trong Composable
- Logic decode bị trộn lẫn với UI → khó test, khó mở rộng
- Khi cần emit sự kiện 1 lần (Snackbar, Toast), không có cơ chế rõ ràng

**Debug process:**
```kotlin
// ❌ Anti-pattern: state + logic nằm trong Composable
@Composable
fun DoubleDecodeScreen() {
    var input by remember { mutableStateOf("") }
    var finalKey by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    OutlinedTextField(
        value = input,
        onValueChange = {
            input = it
            // ❌ Logic decode nằm thẳng trong UI
            try { finalKey = decodeBase64Url(it) } catch (e: Exception) { errorMessage = e.message!! }
        }
    )
}
```

**Giải pháp — StateFlow cho UI State:**
```kotlin
// ✅ Data class gom toàn bộ UI state
data class DoubleDecodeUiState(
    val input: String = "",
    val instruction: String = "",
    val cipherTextBase64: String = "",
    val cipherText: String = "",
    val shift: Int? = null,
    val finalKey: String = "",
    val errorMessage: String = ""
)

class DoubleDecodeViewModel : ViewModel() {
    // _uiState private (chỉ ViewModel đọc/ghi)
    // uiState public read-only → UI chỉ subscribe
    private val _uiState = MutableStateFlow(DoubleDecodeUiState())
    val uiState: StateFlow<DoubleDecodeUiState> = _uiState.asStateFlow()

    fun processInput(newInput: String) {
        _uiState.update { DoubleDecodeUiState(input = newInput) }
        // ... decode logic, update state từng bước
        _uiState.update { it.copy(instruction = instruction) }
    }
}
```

**Giải pháp — SharedFlow cho One-time Events:**
```kotlin
// ✅ SharedFlow: replay=0 → không phát lại event cũ cho subscriber mới
// Phù hợp: Snackbar, Navigation, Toast (chỉ xảy ra 1 lần)
private val _events = MutableSharedFlow<DoubleDecodeEvent>(replay = 0)
val events = _events.asSharedFlow()

// Emit event khi decode thành công
viewModelScope.launch {
    _events.emit(DoubleDecodeEvent.DecodeSuccess(finalKey))
}
```

**UI nhận state + event:**
```kotlin
@Composable
fun DoubleDecodeScreen(viewModel: DoubleDecodeViewModel = viewModel()) {
    // collectAsState(): subscribe StateFlow → tự recompose khi state thay đổi
    // KHÔNG cần remember/mutableStateOf thủ công
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // LaunchedEffect: collect SharedFlow one-time events
    // Chạy coroutine trong Compose lifecycle, nhận event đúng 1 lần
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DoubleDecodeEvent.DecodeSuccess ->
                    snackbarHostState.showSnackbar("✅ Key: ${event.key}")
                is DoubleDecodeEvent.DecodeFailure ->
                    snackbarHostState.showSnackbar("❌ ${event.reason}")
                is DoubleDecodeEvent.InputCleared ->
                    snackbarHostState.showSnackbar("Input đã được xoá")
            }
        }
    }
}
```

**So sánh StateFlow vs SharedFlow:**

| | StateFlow | SharedFlow |
|---|---|---|
| Giữ giá trị hiện tại | ✅ Có (latest state) | ❌ Không (replay=0) |
| Subscriber mới nhận được gì | Giá trị mới nhất ngay lập tức | Không nhận event cũ |
| Dùng cho | UI state (input, result, error) | One-time events (Toast, Nav) |
| Tương đương RxJava | BehaviorSubject | PublishSubject |

**Verification:**
- Nhập input → `uiState` cập nhật → UI recompose tự động ✅
- Decode thành công → `DecodeSuccess` event emit → Snackbar hiện 1 lần ✅
- Rotate màn hình → ViewModel sống sót, state không mất ✅
- Logic hoàn toàn tách khỏi UI → có thể unit test ViewModel riêng lẻ ✅

---

#### **Bước 5: Error Handling - App crashes với invalid input**
**Vấn đề gặp phải:**
- App crash khi nhập string không phải Base64URL hợp lệ
- Không có error messages cho người dùng

**Debug process:**
```kotlin
// ❌ No error handling
fun processInput(newInput: String) {
    instruction = decodeBase64Url(newInput)  // Crash nếu invalid
    cipherText = decodeBase64Url(cipherTextBase64)
    finalKey = caesarDecode(cipherText, shift!!)  // Crash nếu shift = null
}
```

**Giải pháp:**
- Wrap mỗi step trong try-catch
- Provide clear error messages:

```kotlin
// ✅ Comprehensive error handling
fun processInput(newInput: String) {
    errorMessage = ""
    
    try {
        instruction = decodeBase64Url(newInput)
    } catch (e: Exception) {
        errorMessage = "Failed: Invalid Base64URL format - Cannot decode input string"
        return
    }
    
    if (instruction.isBlank()) {
        errorMessage = "Failed: Decoded instruction is empty"
        return
    }
    
    cipherTextBase64 = extractCipherText(instruction) ?: ""
    if (cipherTextBase64.isEmpty()) {
        errorMessage = "Failed: No cipher text found - Expected format: \"cipherText\" in instruction"
        return
    }
    
    // ... similar validation cho các steps khác
}
```

**Verification:**
- Test với các invalid inputs:
  - Empty string ✅
  - Random text không phải Base64 ✅
  - Base64 valid nhưng không có cipher text ✅
  - Missing shift value ✅

---

#### **Bước 6: Code Organization - Refactoring for maintainability**
**Vấn đề gặp phải:**
- Tất cả code ban đầu nằm trong 1 file MainActivity.kt
- Khó maintain và test

**Giải pháp:**
- Tách thành nhiều files theo responsibility:
  - `DecodeUtils.kt` - Pure functions cho decode logic
  - `DoubleDecodeScreen.kt` - Main screen composable
  - `DecodeStepCard.kt`, `FinalKeyCard.kt` - Reusable UI components

**Benefits:**
- Dễ test từng function riêng lẻ
- Reusable components
- Clear separation of concerns
- Better code readability

---

### 🎯 Key Takeaways
1. **Always handle edge cases**: Invalid inputs, null values, empty strings
2. **Understand the specifications**: Base64 vs Base64URL, encode vs decode
3. **Use ViewModel + StateFlow instead of Compose state**: Logic tách khỏi UI, dễ test, survive configuration change
4. **StateFlow cho persistent state, SharedFlow cho one-time events**: Không dùng lẫn lộn
5. **Provide clear error messages**: Giúp debug và UX tốt hơn
6. **Test thoroughly**: Many different inputs và scenarios
7. **Code organization matters**: Separation of concerns từ đầu saves time later

