#pragma once

#include <svg.hpp>

#include <rain.hpp>

namespace Xena {
	class Painter {
		private:
		static inline long double const STROKE_WIDTH_DP{2.5};
		int STROKE_WIDTH_PX{std::lroundl(Painter::STROKE_WIDTH_DP)};

		HWND hWnd;
		HDC hDc;

		bool isLightTheme{true};
		HBRUSH blackBrush{
			Rain::Windows::validateSystemCall(CreateSolidBrush(0x00000000))},
			whiteBrush{
				Rain::Windows::validateSystemCall(CreateSolidBrush(0x00FFFFFF))};
		HPEN blackPen{Rain::Windows::validateSystemCall(
			CreatePen(PS_SOLID, this->STROKE_WIDTH_PX, 0x00000000))},
			whitePen{Rain::Windows::validateSystemCall(
				CreatePen(PS_SOLID, this->STROKE_WIDTH_PX, 0x00FFFFFF))};

		public:
		Svg svg;

		Painter(std::string const &, HWND);
		~Painter();

		void rePaint();
		LRESULT onPaint(HWND, WPARAM, LPARAM);

		void setTheme(bool);
		void refreshDpi();
	};
}
