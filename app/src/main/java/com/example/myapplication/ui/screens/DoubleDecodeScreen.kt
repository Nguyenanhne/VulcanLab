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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.DecodeStepCard
import com.example.myapplication.ui.components.FinalKeyCard
import com.example.myapplication.utils.caesarDecode
import com.example.myapplication.utils.decodeBase64Url
import com.example.myapplication.utils.extractCipherText
import com.example.myapplication.utils.extractShift

@Preview(showBackground = true)
@Composable
fun DoubleDecodeScreen() {
    var input by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("") }
    var cipherTextBase64 by remember { mutableStateOf("") }
    var cipherText by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf<Int?>(null) }
    var finalKey by remember { mutableStateOf("") }

    fun processInput(newInput: String) {
        input = newInput
        errorMessage = ""
        instruction = ""
        cipherTextBase64 = ""
        cipherText = ""
        shift = null
        finalKey = ""

        if (newInput.isBlank()) {
            return
        }

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

        try {
            cipherText = decodeBase64Url(cipherTextBase64)
        } catch (e: Exception) {
            errorMessage = "Failed: Invalid cipher Base64URL - Cannot decode \"$cipherTextBase64\""
            return
        }

        if (cipherText.isBlank()) {
            errorMessage = "Failed: Decoded cipher text is empty"
            return
        }

        shift = extractShift(instruction)
        if (shift == null) {
            errorMessage = "Failed: No shift value found - Expected format: shift=N in instruction"
            return
        }

        try {
            finalKey = caesarDecode(cipherText, shift!!)
        } catch (e: Exception) {
            errorMessage = "Failed: Caesar decode error - ${e.message}"
            return
        }

        if (finalKey.isBlank()) {
            errorMessage = "Final Result: Decoded key is empty"
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

        OutlinedTextField(
            value = input,
            onValueChange = { processInput(it) },
            label = { Text("Enter Base64URL Input") },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }

        // Show decode steps - display successful steps even if later steps fail
        if (input.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))

            if (instruction.isNotEmpty()) {
                DecodeStepCard(
                    title = "Step 1: Intermediate Result (after decode)",
                    content = instruction,
                    isIntermediate = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Step 2: Show if cipher text extraction was attempted
            if (cipherTextBase64.isNotEmpty() || cipherText.isNotEmpty()) {
                DecodeStepCard(
                    title = if (cipherText.isNotEmpty()) "Step 2: Decode CipherText" else "Step 2: Decode CipherText",
                    content = "Base64URL: \"$cipherTextBase64\"\n→ CipherText: \"$cipherText\"",
                    isIntermediate = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Step 3: Show if shift was found
            if (shift != null) {
                DecodeStepCard(
                    title = if (finalKey.isNotEmpty()) "Step 3: Caesar Decode (shift=$shift)" else "Step 3: Caesar Decode (shift=$shift)",
                    content = "\"$cipherText\" → \"$finalKey\"",
                    isIntermediate = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Final Key: Show only if we have a decoded result
            if (finalKey.isNotEmpty() && errorMessage.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FinalKeyCard(finalKey = finalKey)
            }
        } else if (errorMessage.isEmpty()) {
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
            onClick = {
                val defaultInput = "QmFzZTY0VVJMLWRlY29kZSAiZVhodlptUngiIHRvIGdldCBjaXBoZXJU" +
                        "ZXh0OyB0aGVuIENhZXNhci1kZWNvZGUgKHNoaWZ0PTMpIHRvIGdldCBL" +
                        "RVk"
                processInput(defaultInput)
            },
            content = {
                Text(
                    text = "Default Input",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        )
    }
}