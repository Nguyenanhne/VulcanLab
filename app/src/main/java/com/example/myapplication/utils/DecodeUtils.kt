package com.example.myapplication.utils

import android.util.Base64

fun decodeBase64Url(input: String): String {
    val paddedInput = when (input.length % 4) {
        2 -> "$input=="
        3 -> "$input="
        else -> input
    }
    val base64Standard = paddedInput
        .replace('-', '+')
        .replace('_', '/')

    val decodedBytes = Base64.decode(base64Standard, Base64.DEFAULT)
    return String(decodedBytes, Charsets.UTF_8)
}

fun caesarDecode(input: String, shift: Int): String {
    return input.map { char ->
        when {
            char.isLetter() -> {
                val base = if (char.isUpperCase()) 'A' else 'a'
                val shifted = (char - base - shift + 26) % 26
                (base + shifted)
            }
            else -> char
        }
    }.joinToString("")
}

fun extractCipherText(instruction: String): String? {
    val regex = "\"([^\"]+)\"".toRegex()
    return regex.find(instruction)?.groupValues?.get(1)
}

fun extractShift(instruction: String): Int? {
    val regex = "shift\\s*=\\s*(\\d+)".toRegex(RegexOption.IGNORE_CASE)
    return regex.find(instruction)?.groupValues?.get(1)?.toIntOrNull()
}
