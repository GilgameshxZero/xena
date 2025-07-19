#pragma once

#include <eraser.hpp>
#include <interaction.hpp>
#include <mouse.hpp>
#include <painter.hpp>
#include <pen.hpp>
#include <touch.hpp>

#include <rain.hpp>

namespace Xena {
	class MainWindow : public Rain::Windows::Window {
		public:
		MainWindow(std::string const &);

		private:
		std::unordered_map<UINT32, Interaction> interactions;
		Painter painter;
		Mouse mouse;
		Touch touch;
		Pen pen;
		Eraser eraser;

		LRESULT onCreate(WPARAM, LPARAM) override;
		LRESULT onDestroy(WPARAM, LPARAM) override;
		LRESULT onPaint(WPARAM, LPARAM) override;
		LRESULT onPointerDown(WPARAM, LPARAM) override;
		LRESULT onPointerUp(WPARAM, LPARAM) override;
		LRESULT onPointerUpdate(WPARAM, LPARAM) override;

		LRESULT onPointerEvent(WPARAM, LPARAM);
	};
}
