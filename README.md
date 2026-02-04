# Brand Tools

---

<!-- Plugin description -->
## Overview
**Brand Tools** is a plugin that simplifies working with multi-brand / multi-theme repositories. It provides fast file copying and brand-aware navigation directly in the editor.

This plugin is designed for projects where the same file structure exists across **core**, **interlayer**, and multiple **brand themes**.

---
<!-- Plugin description end -->
## ✨ Features

### 1. Brand-aware file and folder copy
- Context menu actions in Project View and Editor:
    - **Copy to Core**
    - **Copy to Interlayer**
    - **Copy to \<Brand\>**
- Behavior:
    - Preserves relative path from source root
    - Automatically creates missing directories
    - Supports single files, multiple files, and entire folders (recursive copy)
- Smart blocking:
    - Copy actions disabled only if the file already exists in the target brand
    - Folder copy is never blocked, even if some files already exist

---

### 2. Editor Brand Switcher
- Displays a brand switcher on the right side of the editor (similar to WebStorm’s browser selectors).
- For each brand:
    - **Active brand (enabled):** file exists → click opens corresponding file
    - **Inactive brand (greyed):** file does not exist → click prompts:
      > “File does not exist in this brand. Do you want to copy it?”
- After copying:
    - New file opens automatically
    - Brand availability states update instantly
- Current brand is visually distinguished (highlighted/selected)

---

### 3. Correct Git & VFS integration
- All file operations use IntelliJ Virtual File System (VFS):
    - WebStorm immediately recognizes new files
    - Standard “Add file to Git?” dialog appears
    - Files remain fully editable, movable, and deletable without IDE restart

---

## 🧠 Why this plugin exists
In large multi-brand repositories:
- The same file often exists in several theme folders
- Switching between brand versions is slow
- Manual copying frequently breaks Git/VFS state

**Brand Tools** removes this friction by making brand navigation and copying one click away, directly inside WebStorm.

---

## ⚙️ Configuration
Configurable options:
- Core root folder name
- Interlayer root folder name
- Brand folder pattern
- Optional virtual/all-theme folder
- Brand list and display order

(Default values are provided for common setups.)

---

## 🛠 Technical Notes
- Target IDE: **WebStorm only**
- Uses public IntelliJ Platform APIs
- No blocking UI operations
- Correct coroutine usage (no model access in UI dispatcher)
- Designed for compatibility with future WebStorm versions

---

## 🚫 Out of Scope
- File diffing or merge tools
- Automatic mass synchronization between brands
- Custom Git operations

---

## 🧪 Development & Testing
Run plugin in sandbox:
```bash
./gradlew runIde
```

Build plugin distribution:
```bash
./gradlew buildPlugin
```

---

## 📌 Summary
**Brand Tools for WebStorm** makes multi-brand development:
- Faster
- Safer
- Less error-prone

by bringing brand-aware navigation and copy actions directly into the editor, using clear brand names instead of icons.

---
