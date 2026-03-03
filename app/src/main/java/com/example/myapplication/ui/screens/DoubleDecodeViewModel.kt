package com.example.myapplication.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.utils.caesarDecode
import com.example.myapplication.utils.decodeBase64Url
import com.example.myapplication.utils.extractCipherText
import com.example.myapplication.utils.extractShift
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

// =========================================================
// UI State - data class
// Dùng khi màn hình cần giữ NHIỀU field cùng lúc (không loại trừ nhau)
// data class cho phép copy() → dễ update từng field mà không mất field khác
// =========================================================
data class DoubleDecodeUiState(
    val input: String = "",
    val instruction: String = "",
    val cipherTextBase64: String = "",
    val cipherText: String = "",
    val shift: Int? = null,
    val finalKey: String = "",
    val computingState: ComputingState = ComputingState.Idle
) {
    // Nested sealed class: chỉ phục vụ cho UiState → gom vào trong cho gọn
    // Dùng khi các trạng thái LOẠI TRỪ nhau (chỉ 1 trong 4 tại 1 thời điểm)
    sealed class ComputingState {
        object Idle : ComputingState()
        object Loading : ComputingState()
        data class Success(val key: String) : ComputingState()
        data class Error(val message: String) : ComputingState()
    }
}

// =========================================================
// One-time Events - dùng SharedFlow để emit sự kiện 1 lần
// Khác StateFlow: SharedFlow KHÔNG giữ lại giá trị cũ,
// phù hợp với: Toast, Navigation, Snackbar, v.v.
// =========================================================
sealed class DoubleDecodeEvent {
    object ComputingStarted : DoubleDecodeEvent()
    object ComputingFinished : DoubleDecodeEvent()
    object InputCleared : DoubleDecodeEvent()
    data class DecodeSuccess(val key: String) : DoubleDecodeEvent()
    data class DecodeFailure(val reason: String) : DoubleDecodeEvent()
}

class DoubleDecodeViewModel : ViewModel() {

    // --- StateFlow ---
    // _uiState là private MutableStateFlow (chỉ ViewModel mới có thể thay đổi)
    // uiState là public StateFlow read-only (UI chỉ đọc, không ghi trực tiếp)
    private val _uiState = MutableStateFlow(DoubleDecodeUiState())
    val uiState: StateFlow<DoubleDecodeUiState> = _uiState.asStateFlow()

    // --- SharedFlow ---
    // replay = 0 → không phát lại event cũ cho subscriber mới
    // Dùng để gửi sự kiện 1 lần như Toast, Snackbar
    private val _events = MutableSharedFlow<DoubleDecodeEvent>(replay = 0)
    val events = _events.asSharedFlow()

    // --- Raw Input Flow ---
    // Chỉ dùng để debounce: cập nhật ngay mỗi khi user gõ/xoá,
    // nhưng chỉ trigger processInputInternal() sau 500ms ngưng thao tác
    private val _rawInput = MutableStateFlow("")

    init {
        observeRawInputWithDebounce()
    }

    @OptIn(FlowPreview::class)
    private fun observeRawInputWithDebounce() {
        _rawInput
            .debounce(500L) // ← chờ 500ms sau khi ngưng gõ/xoá mới tính toán
            .onEach { input -> processInputInternal(input) }
            .launchIn(viewModelScope)
    }

    // Gọi từ UI khi TextField thay đổi:
    // - Cập nhật field input ngay lập tức (TextField không bị lag)
    // - Đẩy vào _rawInput để debounce xử lý sau
    fun processInput(newInput: String) {
        _uiState.update { it.copy(input = newInput) }
        _rawInput.value = newInput
    }

    // Logic tính toán thực sự – chỉ chạy sau debounce 500ms
    private suspend fun processInputInternal(newInput: String) {
        // Reset toàn bộ kết quả tính toán, giữ lại input đang hiển thị
        _uiState.update { current -> DoubleDecodeUiState(input = current.input) }

        if (newInput.isBlank()) {
            _uiState.update { it.copy(computingState = DoubleDecodeUiState.ComputingState.Idle) }
            _events.emit(DoubleDecodeEvent.InputCleared)
            return
        }

        // Bật trạng thái Loading
        _uiState.update { it.copy(computingState = DoubleDecodeUiState.ComputingState.Loading) }
        _events.emit(DoubleDecodeEvent.ComputingStarted)

        // Giả lập tính toán nặng mất 1 giây
        delay(1000L)

        // Helper: set Error state + emit event rồi return
        suspend fun failWith(msg: String) {
            _uiState.update { it.copy(computingState = DoubleDecodeUiState.ComputingState.Error(msg)) }
            _events.emit(DoubleDecodeEvent.ComputingFinished)
            _events.emit(DoubleDecodeEvent.DecodeFailure(msg))
        }

        // Bước 1: Decode Base64URL lớp ngoài → instruction
        val instruction = try {
            decodeBase64Url(newInput)
        } catch (_: Exception) {
            failWith("Failed: Invalid Base64URL format - Cannot decode input string")
            return
        }

        if (instruction.isBlank()) {
            failWith("Failed: Decoded instruction is empty")
            return
        }

        _uiState.update { it.copy(instruction = instruction) }

        // Bước 2: Trích xuất cipherTextBase64 từ instruction
        val cipherTextBase64 = extractCipherText(instruction) ?: run {
            failWith("Failed: No cipher text found - Expected format: \"cipherText\" in instruction")
            return
        }

        _uiState.update { it.copy(cipherTextBase64 = cipherTextBase64) }

        // Bước 2b: Decode Base64URL lớp trong → cipherText thực
        val cipherText = try {
            decodeBase64Url(cipherTextBase64)
        } catch (_: Exception) {
            failWith("Failed: Invalid cipher Base64URL - Cannot decode \"$cipherTextBase64\"")
            return
        }

        if (cipherText.isBlank()) {
            failWith("Failed: Decoded cipher text is empty")
            return
        }

        _uiState.update { it.copy(cipherText = cipherText) }

        // Bước 3: Lấy shift từ instruction
        val shift = extractShift(instruction) ?: run {
            failWith("Failed: No shift value found - Expected format: shift=N in instruction")
            return
        }

        _uiState.update { it.copy(shift = shift) }

        // Bước 4: Caesar decode → final key
        val finalKey = try {
            caesarDecode(cipherText, shift)
        } catch (_: Exception) {
            failWith("Failed: Caesar decode error")
            return
        }

        if (finalKey.isBlank()) {
            failWith("Final Result: Decoded key is empty")
            return
        }

        // Thành công: set Success state
        _uiState.update { it.copy(finalKey = finalKey, computingState = DoubleDecodeUiState.ComputingState.Success(finalKey)) }
        _events.emit(DoubleDecodeEvent.ComputingFinished)
        _events.emit(DoubleDecodeEvent.DecodeSuccess(finalKey))
    }

    // Nạp input mặc định (default demo)
    fun loadDefaultInput() {
        val defaultInput = "QmFzZTY0VVJMLWRlY29kZSAiZVhodlptUngiIHRvIGdldCBjaXBoZXJU" +
                "ZXh0OyB0aGVuIENhZXNhci1kZWNvZGUgKHNoaWZ0PTMpIHRvIGdldCBL" +
                "RVk"
        processInput(defaultInput)
    }
}
