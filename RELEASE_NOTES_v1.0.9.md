# GeminiCraftChat v1.0.9 Release Notes

## 🚀 Major Update: Modern Minecraft Era (Paper 1.21.4 & Leaves API)

**Release Date**: March 2, 2026  
**Version**: 1.0.9  
**Repository**: https://github.com/geminicraftchat/gcc  
**Plugin Status**: Stable & Recommended for Production

---

## 📋 Release Summary

This release marks a significant milestone in the evolution of GeminiCraftChat. We have moved away from legacy Spigot APIs to embrace the modern **Paper 1.21.4** and **LeavesMC** ecosystem. This update introduces the **Adventure API** for rich message handling, migrates to the high-performance `AsyncChatEvent`, and resolves critical thread-safety issues for multi-model concurrent usage.

---

## 🌟 Major New Features

### 1. **Paper 1.21.4 & LeavesMC Native Support**
- **⚡ Native Integration**: Specifically optimized for the latest Paper 1.21.4 and LeavesMC server software.
- **🔄 Modern Event Flow**: Migrated from the legacy `AsyncPlayerChatEvent` to the modern, non-blocking `AsyncChatEvent`.
- **🍃 Leaves API**: Added dedicated support for LeavesMC's unique features and APIs.

### 2. **Adventure API Messaging System**
- **✨ Rich Components**: Complete migration to `net.kyori.adventure` for all in-game messages.
- **🎨 Enhanced Formatting**: Better support for RGB colors, hover events, and clickable components.
- **🖥️ Component Logger**: Native integration with Paper's `ComponentLogger` for standardized, formatted console output.

### 3. **Concurrency & Thread Safety Overhaul**
- **🛡️ Thread-Safe History**: Fixed `ConcurrentModificationException` by implementing `CopyOnWriteArrayList` for player chat histories.
- **⚡ Async Optimizations**: Improved the handling of AI responses in asynchronous contexts to ensure zero impact on server MSPT.

### 4. **Improved JSON Response Parsing**
- **🔍 Robust Path Extraction**: Enhanced the internal JSON path extractor to handle `JsonNull` and nested arrays more gracefully.
- **🛠️ Error Resilience**: Better error reporting when API responses don't match the expected configuration template.

---

## 🔧 Technical Improvements

### Build System & Dependencies
- **📦 Maven Updates**: Updated all core dependencies (Gson, OkHttp, SnakeYAML) to their latest stable versions.
- **🔌 API Decoupling**: Refactored the internal messenger components to be independent of legacy Minecraft chat strings.
- **🚀 Performance**: Reduced memory allocation patterns during message serialization.

---

## 📊 Feature Comparison

| Feature | v1.0.7 | v1.0.9 | Result |
|---------|--------|--------|--------|
| **Core API** | Spigot 1.13+ | Paper 1.21.4 / Leaves | ✅ Modernized |
| **Chat Event** | Legacy AsyncPlayerChat | Modern AsyncChatEvent | ✅ Non-Blocking |
| **Messaging** | String/Legacy Color | Adventure Components | ✅ Rich Experience |
| **Thread Safety** | Basic | Advanced (COW) | ✅ Rock Stable |
| **Parsing** | Strict JSON | Robust Path Logic | ✅ Failure Resilient |

---

## 🚀 Installation & Upgrade

### Upgrading from v1.0.8/v1.0.7
1. **JAR Swap**: Replace the old `geminicraftchat-1.0.7.jar` with `geminicraftchat-1.0.9.jar`.
2. **Config Check**: Your existing `config.yml` remains compatible. No manual changes required.
3. **Restart**: Restart your server (Recommended over `/reload`).

---

## 🐛 Bug Fixes

- **Fixed**: `ConcurrentModificationException` when clearing history during an active AI request.
- **Fixed**: Occasional `NullPointerException` when parsing malformed API responses.
- **Fixed**: AI trigger detection failing for messages starting with lowercase triggers in certain locales.
- **Fixed**: Legacy color codes not being properly stripped in certain console environments.

---

## 🙏 Acknowledgments

A special thank you to our community for pushing us towards modern API standards.

---

**Happy Crafting with Modern AI! 🎮🤖**

*GeminiCraftChat Team*  
*March 2, 2026*
