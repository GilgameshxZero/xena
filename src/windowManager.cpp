#include <windowManager.hpp>

namespace Xena {
	WindowManager::WindowManager(std::string const &fileToLoad)
			: fileToLoad(fileToLoad) {}

	void WindowManager::redraw() {
		InvalidateRect(hWnd, NULL, FALSE);
	}
}
