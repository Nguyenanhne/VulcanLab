package com.example.myapplication.domain.model

/**
 * Domain Model – thuần Kotlin, không phụ thuộc Android hay Compose.
 *
 * Đây là kết quả cuối cùng sau khi xử lý toàn bộ pipeline double-decode.
 * Được trả về bởi UseCase và ánh xạ thành UiState ở tầng UI.
 */
data class DecodeResult(
    /** Input gốc người dùng cung cấp */
    val rawInput: String,

    /** Chuỗi instruction sau khi decode Base64URL lớp ngoài */
    val instruction: String,

    /** Chuỗi Base64URL của cipher text (trích xuất từ instruction) */
    val cipherTextBase64: String,

    /** Cipher text sau khi decode Base64URL lớp trong */
    val cipherText: String,

    /** Giá trị shift cho Caesar cipher (trích xuất từ instruction) */
    val shift: Int,

    /** Key cuối cùng sau khi Caesar decode */
    val finalKey: String
)
