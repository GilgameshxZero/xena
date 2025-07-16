#include <painter.hpp>

namespace Xena {
	Painter::Painter(std::string const &fileToLoad, HWND hWnd)
			: hWnd{hWnd},
				hDc{Rain::Windows::validateSystemCall(GetDC(hWnd))},
				svg(fileToLoad, this->viewportOffset, this->paths) {}

	void Painter::rePaint() {
		Rain::Windows::validateSystemCall(InvalidateRect(this->hWnd, NULL, FALSE));
	}

	LRESULT Painter::onPaint(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		using Rain::Windows::validateSystemCall;

		static PAINTSTRUCT ps;

		validateSystemCall(BeginPaint(hWnd, &ps));
		Gdiplus::Graphics graphics(ps.hdc);

		if (isLightTheme) {
			graphics.FillRectangle(
				&this->whiteBrush,
				Gdiplus::Rect(
					ps.rcPaint.left,
					ps.rcPaint.top,
					ps.rcPaint.right - ps.rcPaint.left,
					ps.rcPaint.bottom - ps.rcPaint.top));
			graphics.DrawLine(
				&this->blackPen, Gdiplus::Point(100, 100), Gdiplus::Point(200, 200));
		} else {
			graphics.FillRectangle(
				&this->blackBrush,
				Gdiplus::Rect(
					ps.rcPaint.left,
					ps.rcPaint.top,
					ps.rcPaint.right - ps.rcPaint.left,
					ps.rcPaint.bottom - ps.rcPaint.top));
			graphics.DrawLine(
				&this->whitePen, Gdiplus::Point(100, 100), Gdiplus::Point(200, 200));
		}

		EndPaint(hWnd, &ps);
		Rain::Log::verbose("Painter::onPaint.");
		return 0;
	}

	void Painter::setTheme(bool isLightTheme) {
		this->isLightTheme = isLightTheme;
	}
	void Painter::refreshDpi() {
		this->STROKE_WIDTH_PX = Painter::STROKE_WIDTH_DP *
			Rain::Windows::validateSystemCall(GetDpiForWindow(this->hWnd)) /
			USER_DEFAULT_SCREEN_DPI;
		this->blackPen.SetWidth(this->STROKE_WIDTH_PX);
		this->whitePen.SetWidth(this->STROKE_WIDTH_PX);
		this->transparentPen.SetWidth(this->STROKE_WIDTH_PX * 1.5f);
		Rain::Log::verbose("Painter::refreshDpi.");
	}

	void Painter::addPath(std::shared_ptr<Path> const &path) {}
	void Painter::removePath(std::size_t pathId) {}
}
