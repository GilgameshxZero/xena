#pragma once

#include <rain.hpp>

namespace Xena {
	class WindowManager {
		public:
		HWND hWnd;
		HBRUSH brush{(HBRUSH)COLOR_WINDOW};
		std::string fileToLoad;

		WindowManager(std::string const &);

		void redraw();
	};
}
