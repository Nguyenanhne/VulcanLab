package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.components.DecodeStepCard
import com.example.myapplication.ui.components.FinalKeyCard

/**
 * DoubleDecodeScreen – sử dụng ViewModel + StateFlow + SharedFlow
 *
 * ┌─────────────────────────────────────────────────────┐
 * │  StateFlow  → giữ UI state (input, result, error)   │
 * │  SharedFlow → one-time events (Toast, Snackbar...)  │
 * └─────────────────────────────────────────────────────┘
 *
 * collectAsState()  → subscribe StateFlow, tự recompose khi state thay đổi
 * LaunchedEffect    → collect SharedFlow để xử lý event 1 lần (Snackbar)
 */
@Preview(showBackground = true)
@Composable
fun DoubleDecodeScreen(
    // viewModel() tự tạo hoặc lấy lại ViewModel đã có trong scope
    viewModel: DoubleDecodeViewModel = viewModel()
) {
    // --- Collect StateFlow thành Compose State ---
    // collectAsState() subscribe uiState; mỗi khi ViewModel gọi _uiState.update { }
    // Compose sẽ tự recompose mà không cần remember/mutableStateOf thủ công
    val uiState by viewModel.uiState.collectAsState()

    // SnackbarHostState để hiển thị Snackbar từ SharedFlow event
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Collect SharedFlow (One-time Events) ---
    // LaunchedEffect chạy coroutine trong Compose lifecycle
    // events.collect sẽ nhận từng event DoubleDecodeEvent một lần duy nhất
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DoubleDecodeEvent.DecodeSuccess ->
                    snackbarHostState.showSnackbar("✅ Key tìm được: ${event.key}")

                is DoubleDecodeEvent.DecodeFailure ->
                    snackbarHostState.showSnackbar("❌ ${event.reason}")

                is DoubleDecodeEvent.InputCleared ->
                    snackbarHostState.showSnackbar("Input đã được xoá")

                is DoubleDecodeEvent.ComputingStarted -> {}
                is DoubleDecodeEvent.ComputingFinished -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DOUBLE DECODE KEY",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Đọc từ StateFlow: uiState.input
        // Ghi qua ViewModel: viewModel.processInput(it)
        OutlinedTextField(
            value = uiState.input,
            onValueChange = { viewModel.processInput(it) },
            label = { Text("Enter Base64URL Input") },
            modifier = Modifier.fillMaxWidth()
        )

        // Hiển thị trạng thái lỗi, loading, success từ computingState
        when (val state = uiState.computingState) {
            is DoubleDecodeUiState.ComputingState.Error -> {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
            is DoubleDecodeUiState.ComputingState.Loading -> {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.CircularProgressIndicator()
            }
            else -> {}
        }

        // Hiển thị các bước decode nếu có input và không phải đang loading/error
        if (uiState.input.isNotBlank() && uiState.computingState !is DoubleDecodeUiState.ComputingState.Error) {
            Spacer(modifier = Modifier.height(16.dp))

            // Bước 1: instruction sau decode lớp 1
            if (uiState.instruction.isNotEmpty()) {
                DecodeStepCard(
                    title = "Step 1: Intermediate Result (after decode)",
                    content = uiState.instruction,
                    isIntermediate = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bước 2: decode cipherText
            if (uiState.cipherTextBase64.isNotEmpty() || uiState.cipherText.isNotEmpty()) {
                DecodeStepCard(
                    title = "Step 2: Decode CipherText",
                    content = "Base64URL: \"${uiState.cipherTextBase64}\"\n→ CipherText: \"${uiState.cipherText}\"",
                    isIntermediate = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bước 3: Caesar decode
            if (uiState.shift != null) {
                DecodeStepCard(
                    title = "Step 3: Caesar Decode (shift=${uiState.shift})",
                    content = "\"${uiState.cipherText}\" → \"${uiState.finalKey}\"",
                    isIntermediate = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Kết quả cuối cùng: chỉ show khi Success
            if (uiState.computingState is DoubleDecodeUiState.ComputingState.Success) {
                Spacer(modifier = Modifier.height(8.dp))
                FinalKeyCard(finalKey = (uiState.computingState as DoubleDecodeUiState.ComputingState.Success).key)
            }
        } else if (uiState.computingState !is DoubleDecodeUiState.ComputingState.Error) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Enter a Base64URL encoded string above to start decoding",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            // Gọi ViewModel method thay vì xử lý logic trong Composable
            onClick = { viewModel.loadDefaultInput() },
            content = {
                Text(
                    text = "Default Input",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        )

        // Snackbar host để hiển thị event từ SharedFlow
        Spacer(modifier = Modifier.height(8.dp))
        SnackbarHost(hostState = snackbarHostState)
    }
}