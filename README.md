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
- Main screen containing the entire decode interface
- Real-time processing of user input
- State management for all decode steps
- Error handling for invalid inputs

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

