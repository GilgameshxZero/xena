#pragma once

#include <interactSequence.hpp>
#include <penManager.hpp>
#include <touchManager.hpp>
#include <windowManager.hpp>

#include <rain.hpp>

namespace Xena {
	class MainWindow {
		public:
		WindowManager windowManager;

		MainWindow(std::string const &);

		private:
		static LRESULT CALLBACK wndProc(HWND, UINT, WPARAM, LPARAM);

		std::unordered_map<UINT32, InteractSequence> interactSequences;
		PenManager penManager;
		TouchManager touchManager;

		LRESULT onDestroy(HWND, WPARAM, LPARAM);
		LRESULT onPaint(HWND, WPARAM, LPARAM);
		LRESULT onPointer(HWND, WPARAM, LPARAM);
	};
}
