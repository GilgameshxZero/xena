#include <windowManager.hpp>

namespace Xena {
	WindowManager::WindowManager(std::string const &fileToLoad)
			: fileManager(fileToLoad) {}

	void WindowManager::redraw() {
		InvalidateRect(hWnd, NULL, FALSE);
	}

	void WindowManager::onPaint(HDC hDc) {
		MoveToEx(hDc, 0, 0, NULL);
		LineTo(hDc, 100, 100);
	}
}
