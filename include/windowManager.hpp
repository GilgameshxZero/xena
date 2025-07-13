#pragma once

#include <fileManager.hpp>

#include <rain.hpp>

namespace Xena {
	class WindowManager {
		public:
		HWND hWnd;
		FileManager fileManager;

		WindowManager(std::string const &);

		void redraw();
		void onPaint(HDC);
	};
}
