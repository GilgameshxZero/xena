#include <chunk.hpp>

namespace Xena {
	Chunk::Chunk(
		HDC hDc,
		Gdiplus::Point const &size,
		Gdiplus::Point const &offset,
		Gdiplus::Brush const &backgroundBrush)
			: SIZE{size},
				OFFSET{offset},
				hDc{Rain::Windows::validateSystemCall(CreateCompatibleDC(hDc))},
				hBitmap{Rain::Windows::validateSystemCall(
					CreateCompatibleBitmap(this->hDc, size.X, size.Y))},
				hOrigBitmap{static_cast<HBITMAP>(Rain::Windows::validateSystemCall(
					SelectObject(this->hDc, this->hBitmap)))},
				graphics(this->hDc) {
		Rain::Windows::Gdiplus::validateGdiplusCall(this->graphics.FillRectangle(
			&backgroundBrush, Gdiplus::Rect(0, 0, size.X, size.Y)));
	}
	Chunk::~Chunk() {
		SelectObject(this->hDc, this->hOrigBitmap);
		DeleteObject(this->hBitmap);
		DeleteDC(this->hDc);
	}

	void Chunk::drawPath(
		std::shared_ptr<Path> const &path,
		Gdiplus::Pen const &pen) {
		Rain::Windows::Gdiplus::validateGdiplusCall(
			this->graphics.DrawPath(&pen, &path->getPath()));
	}
}
