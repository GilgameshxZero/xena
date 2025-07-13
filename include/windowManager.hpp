#pragma once

#include <fileManager.hpp>

#include <rain.hpp>

namespace Xena {
	class WindowManager {
		public:
		HWND hWnd;
		HBRUSH brush{(HBRUSH)COLOR_WINDOW};

		FileManager fileManager;

		WindowManager(std::string const &);

		void redraw();
	};
}
