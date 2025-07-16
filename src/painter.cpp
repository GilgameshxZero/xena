#include <painter.hpp>

namespace Xena {
	Painter::Painter(std::string const &fileToLoad, HWND hWnd)
			: hWnd{hWnd},
				hDc{Rain::Windows::validateSystemCall(GetDC(hWnd))},
				svg(
					fileToLoad,
					Rain::Windows::validateSystemCall(GetDpiForWindow(this->hWnd))) {}
	Painter::~Painter() {
		DeleteObject(this->blackBrush);
		DeleteObject(this->whiteBrush);
		DeleteObject(this->blackPen);
		DeleteObject(this->whitePen);
	}

	void Painter::rePaint() {
		Rain::Windows::validateSystemCall(InvalidateRect(this->hWnd, NULL, FALSE));
	}

	LRESULT Painter::onPaint(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		using Rain::Windows::validateSystemCall;

		static PAINTSTRUCT ps;
		static RECT rect;

		validateSystemCall(BeginPaint(hWnd, &ps));

		if (isLightTheme) {
			validateSystemCall(FillRect(ps.hdc, &ps.rcPaint, this->whiteBrush));
			validateSystemCall(SelectObject(ps.hdc, this->blackPen));
		} else {
			validateSystemCall(FillRect(ps.hdc, &ps.rcPaint, this->blackBrush));
			validateSystemCall(SelectObject(ps.hdc, this->whitePen));
		}
		validateSystemCall(MoveToEx(ps.hdc, 100, 100, NULL));
		validateSystemCall(LineTo(ps.hdc, 200, 200));

		EndPaint(hWnd, &ps);
		Rain::Log::verbose("Painter::onPaint.");
		return 0;
	}

	void Painter::setTheme(bool isLightTheme) {
		this->isLightTheme = isLightTheme;
	}
	void Painter::refreshDpi() {
		this->STROKE_WIDTH_PX = std::lroundl(
			Painter::STROKE_WIDTH_DP *
			Rain::Windows::validateSystemCall(GetDpiForWindow(this->hWnd)) /
			USER_DEFAULT_SCREEN_DPI);

		Rain::Windows::validateSystemCall(DeleteObject(this->blackPen));
		Rain::Windows::validateSystemCall(DeleteObject(this->whitePen));
		this->blackPen = Rain::Windows::validateSystemCall(
			CreatePen(PS_SOLID, this->STROKE_WIDTH_PX, 0x00000000));
		this->whitePen = Rain::Windows::validateSystemCall(
			CreatePen(PS_SOLID, this->STROKE_WIDTH_PX, 0x00FFFFFF));

		Rain::Log::verbose("Painter::refreshDpi.");
	}
}
