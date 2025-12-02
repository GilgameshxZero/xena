# Changelog

## TODO

1. Split up Makefile into easier-to-manage chunks.
2. Display read percentage for PDFs.
3. Track directory page in file picker.
4. Fix off-by-one bug for pages in file picker.
5. Fix full screen refresh in regal mode when scrolling.
6. Eraser size/velocity dependence.
7. Fix EXE command line parsing with input file being read as command line value.
8. Note that Windows EXE “open with” only works with release builds, for some reason.
9. Ensure working with Android 9 (no method `ConcurrentHashMap.keySet()`).

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
