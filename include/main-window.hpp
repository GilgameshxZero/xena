#pragma once

#include <eraser.hpp>
#include <interaction.hpp>
#include <mouse.hpp>
#include <painter.hpp>
#include <pen.hpp>
#include <touch.hpp>

#include <rain.hpp>

namespace Xena {
	class MainWindow {
		public:
		Painter painter;

		MainWindow(std::string const &);

		private:
		static LRESULT CALLBACK wndProc(HWND, UINT, WPARAM, LPARAM);

		std::unordered_map<UINT32, Interaction> interactions;
		Mouse mouse;
		Touch touch;
		Pen pen;
		Eraser eraser;

		HWND createWindow();

		LRESULT onDestroy(HWND, WPARAM, LPARAM);
		LRESULT onPaint(HWND, WPARAM, LPARAM);
		LRESULT onPointerDown(HWND, WPARAM, LPARAM);
		LRESULT onPointerUp(HWND, WPARAM, LPARAM);
		LRESULT onPointerUpdate(HWND, WPARAM, LPARAM);

		LRESULT onPointerEvent(HWND, WPARAM, LPARAM);
	};
}
