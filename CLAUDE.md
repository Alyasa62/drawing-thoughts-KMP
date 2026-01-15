# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Drawing Thoughts is a Kotlin Multiplatform (KMP) whiteboard application targeting Android and Desktop (JVM). It provides an interactive canvas for drawing with various tools, shape manipulation, viewport controls, and persistent storage using Room database.

## Build and Run Commands

### Desktop (JVM)
```bash
# Windows
gradlew.bat :composeApp:run

# macOS/Linux
./gradlew :composeApp:run
```

### Android
```bash
# Windows
gradlew.bat :composeApp:assembleDebug

# macOS/Linux
./gradlew :composeApp:assembleDebug
```

### Other Build Tasks
```bash
# Run all tests
gradlew.bat test  # Windows
./gradlew test     # Unix

# Clean build
gradlew.bat clean build

# Generate Room schemas
# Room schemas are automatically generated to composeApp/schemas/
```

## Architecture

### Multiplatform Structure
- **commonMain**: Shared code across all platforms (UI, domain logic, data layer)
- **androidMain**: Android-specific implementations (database builder, image saver)
- **jvmMain**: Desktop (JVM) specific implementations
- **desktopMain**: Desktop-specific configurations (may be merged with jvmMain)

### Layer Architecture

**Presentation Layer** (`presentation/whiteboard/`)
- `WhiteBoardScreen`: Main composable UI
- `WhiteBoardViewModel`: Manages state and business logic using MVI pattern
- `WhiteBoardState`: Immutable state containing shapes, tools, viewport, selection
- `WhiteBoardEvent`: Sealed interface for all user interactions

**Domain Layer** (`domain/model/`)
- `DrawnShape`: Sealed class with `FreeHand` and `Geometric` subtypes
- `DrawingTool`: Enum of all drawing tools (PEN, ERASER, SELECTOR, shapes, etc.)

**Data Layer** (`data/`)
- `ShapeRepository`: Mediates between ViewModel and Room database
- `AppDatabase`: Room database with `ShapeDao`
- `ShapeEntity`: Room entity for persistence
- `DatabaseBuilder`: Platform-specific database initialization (expect/actual pattern)

### Key Components

**ViewportState** (`presentation/whiteboard/state/ViewportState.kt`)
- Manages zoom (0.1x-5x) and pan with clamping
- Defines world size (5000x5000 units)
- Provides `screenToWorld()` coordinate transformation
- Used for infinite canvas behavior

**Shape System**
- Shapes are rendered on an infinite grid with minimap
- Two shape types:
  - `FreeHand`: Path-based (PEN, HIGHLIGHTER, LASER_PEN, ERASER)
  - `Geometric`: Start/end point based (lines, rectangles, circles, arrows, triangles)
- Path smoothing via `PathSmoother` for better freehand rendering
- Shapes persist via Room database with auto-save (2-second debounce)

**Selection & Transformation**
- `SELECTOR` tool enables shape selection via hit testing (`HitTestUtil`)
- Selected shapes can be:
  - Dragged (repositioned)
  - Scaled/rotated via gesture transforms (pinch/rotate)
  - Resized via corner/edge handles (`TransformHandle`)
  - Deleted
- Transform operations use a transaction snapshot pattern for undo/redo

**Undo/Redo System**
- Stack-based with 50-operation history (`MAX_HISTORY_SIZE`)
- Snapshots taken before mutations (draw, delete, transform)
- `transactionSnapshot` used for multi-step transforms

**Eraser Modes**
- Standard eraser: Paints transparent strokes (uses `BlendMode.Clear`)
- Object eraser: Deletes entire shapes on touch (`isObjectEraserEnabled`)

### Database & Persistence

Room database with platform-specific builders:
- **Android**: Uses app context, stores in app database directory
- **JVM/Desktop**: Uses temp directory (`java.io.tmpdir/drawing_thoughts.db`)

Serialization:
- `FreeHand` shapes: Points serialized as `"x1,y1;x2,y2;..."` strings
- `Geometric` shapes: Store start/end coordinates
- Colors stored as Int (from `Color.value`)
- Paths reconstructed on load via `PathSmoother`

### UI Components

**Main Canvas Components** (`presentation/whiteboard/component/`)
- `WhiteboardCanvas`: Main drawing surface with gesture handling
- `InfiniteGrid`: Renders infinite grid background
- `Minimap`: Overview of canvas with viewport indicator
- `SelectionOverlay`: Shows selection bounds and transform handles
- `TopBar`: Undo/redo, zoom controls, save/export
- `DrawingToolFAB`: Floating action button for tool selection
- `CompactDock`/`DynamicHUD`: Tool palettes

**Inspector Panels**
- `UnifiedInspectorPanel`: Properties panel for canvas/tools/shapes
- `ResponsiveInspector`: Adaptive layout for different screen sizes
- Shows stroke width, color pickers, background, eraser mode toggle

### Platform-Specific Code

**Image Saving** (`utils/PlatformImageSaver.kt`)
- Expect/actual pattern for saving canvas to image
- `AndroidImageSaver`: Uses Android MediaStore API
- `DesktopImageSaver`: Uses Java file system

**Database Initialization**
- Android: Requires context initialization in `MainActivity` via `AndroidWrappedContext`
- Desktop: Uses file-based SQLite in system temp directory

## Development Notes

### Working with Shapes
- Always use `screenToWorld()` when handling touch events to account for zoom/pan
- New shapes get temporary IDs during drawing; finalized on `FinishDrawing`
- Database uses Long IDs; domain uses String (converted via `toLongOrNull()`)

### Adding New Drawing Tools
1. Add enum to `DrawingTool` with drawable resource
2. Determine if freehand or geometric (update `isFreeHandTool()` if freehand)
3. Add rendering logic in canvas component
4. Update inspector panel if tool needs custom properties

### Viewport/Camera Changes
- Viewport state is independent of shape coordinates (shapes in world space)
- Minimap shows viewport bounds relative to world
- Clamp pan to prevent scrolling beyond world boundaries

### Testing Database Changes
- Schema changes require migration or `fallbackToDestructiveMigration`
- Desktop: Clear temp directory DB to reset
- Android: Clear app data or uninstall/reinstall

### KSP & Room Code Generation
Room compiler runs via KSP for each platform:
- `kspCommonMainMetadata`: Common schema
- `kspAndroid`: Android-specific implementation
- `kspJvm`: JVM-specific implementation

Generated code includes `AppDatabase_Impl` (used in platform builders).
