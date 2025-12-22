# Changelog

## TODO

1. Split up Makefile into easier-to-manage chunks.
2. Eraser size/velocity dependence.
3. Fix EXE command line parsing with input file being read as command line value.
4. Note that Windows EXE “open with” only works with release builds, for some reason.
5. Wayland/Linux native app.

## 1.5.7

1. Add total page count in PDF `ScribbleActivity`.
2. Allow prefix filtering in `FilePickerActivity`.
3. `FilePickerActivity` now retains listing page when entering/exiting. Listing page is not retained when moving up the directory tree; this may be changed in the future.

## 1.5.6

1. "Date" button uses `yyyy_MM_dd.svg` format instead of `yyyy-MM-dd.svg` format.
2. Add `.clangd` as part of migration to Codium OSS extensions.

## 1.5.5

1. Fix off-by-one bug for max pages count in file picker.
2. Swap `ConcurrentHashMap.keySet()` for `ConcurrentHashMap.keys()` so that it now runs on Android x86.
3. Misc:
	1. Standardize build process to hide intermediates and build artifacts.
	2. Track `R.java`.
	3. Standardize single-point paths on Android.
	4. Fix SVG view position loading on Windows.
	5. Update `rain`.

## 1.5.4

1. Update viewport offset UI to evaluate the half-page at the center of the screen.
2. All modals now automatically hide keyboard when closed.
3. Changing the page in `ScribbleActivity` will redraw immediately.
4. Raw drawing exclusions for controls bar is now offset on Y-axis correctly.

## 1.5.3

1. Changed settings to icon.
2. Standardized icon padding/size in ScribbleActivity.
3. Listing now shows two rows per item.
4. Added setting for flick distance.

## 1.5.2

1. Bugfix for loading non-xena SVGs on Windows.

## 1.5.1

1. More efficient SVG loading on Windows.
2. Enabled eraser on Windows.

## 1.5.0

1. Windows: standardized DPI, points.
2. Windows: enabled SVG save.

## 1.4.11

1. Alpha release for Windows, without SVG save/load, erase, and fast erasing.

## 1.4.10

1. Started changelog.
2. Added unified versioning between Windows and Android build.
