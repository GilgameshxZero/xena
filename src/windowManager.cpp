#include <windowManager.hpp>

namespace Xena {
	WindowManager::WindowManager(std::string const &fileToLoad)
			: fileManager(fileToLoad) {}

	void WindowManager::redraw() {
		InvalidateRect(hWnd, NULL, FALSE);
	}
}
