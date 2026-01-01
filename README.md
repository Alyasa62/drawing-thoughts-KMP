# Drawing Thoughts KMP 🎨

![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin%20Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop-JVM-orange?style=for-the-badge&logo=java&logoColor=white)

**Drawing Thoughts** is a cross-platform application built using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. It allows users to [sketch ideas / take visual notes] seamlessly across Android devices and Desktop (Windows/macOS/Linux) environments using a single shared codebase.

##  About The Project

This project demonstrates the power of sharing UI and logic between mobile and desktop platforms. By leveraging Compose Multiplatform, **Drawing Thoughts** delivers a native-feeling experience on both targets without maintaining separate UI codebases. Drawing Thoughts is a powerful whiteboard application designed to give your ideas unlimited space. Built using Kotlin Multiplatform (KMP), it provides a seamless drawing experience on both Android and Desktop.

Unlike standard drawing apps with fixed boundaries, this project features an infinite canvas, allowing users to sketch without running out of room. To help navigate this vast space, it includes a built-in minimap. Users can also export their creations and save them directly to their device's gallery.

##  Features

- **Cross-Platform Support:** Runs on Android and Desktop (JVM).
- Infinite Canvas: Draw without limits on a canvas that expands as you create.
- Minimap Navigation: Easily orient yourself and navigate large drawings with a real-time minimap.
- Save to Gallery: Export your artwork and save it directly to your mobile gallery.
- **Shared UI:** 100% shared UI code using Compose Multiplatform.
- **Canvas Drawing:** Smooth drawing experience using Compose Canvas. 
- **Material Design 3:** Modern and adaptive interface.
- **Adaptive Layout:** UI adapts to different screen sizes (Phone vs. Desktop window).

##  Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/) (100%)
- **Framework:** [Kotlin Multiplatform (KMP)](https://kotlinlang.org/docs/multiplatform.html)
- **UI:** [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- **Build System:** Gradle (Kotlin DSL)
- **State Management:** generic `ViewModel` / `StateFlow`

##  How to Run

###  Android

To build and run the application on an Android device or emulator:

1. Open the project in **Android Studio**.
2. Select the `composeApp` configuration.
3. Select your target device.
4. Click **Run** (or run `./gradlew :composeApp:assembleDebug`).

###  Desktop (JVM)

To run the application on your desktop:

1. Open the **Gradle** tool window in IntelliJ/Android Studio.
2. Navigate to `composeApp > Tasks > compose desktop > run`.
3. Or run the following command in the terminal:
   ```bash
   ./gradlew :composeApp:run

## Project Structure
- composeApp: The main module containing the application code.

- commonMain: Shared code (UI, Logic, ViewModels) used by all platforms.

- androidMain: Android-specific implementations.

- jvmMain: Desktop-specific implementations.

## Contribution
Contributions are welcome! If you'd like to add iOS support or new drawing tools:

- Fork the repository.

- Create a feature branch.

- Submit a Pull Request.
