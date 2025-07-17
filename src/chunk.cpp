#include <chunk.hpp>

namespace Xena {
	Chunk::Chunk(
		HDC hDc,
		Gdiplus::Point const &size,
		Gdiplus::Point const &position,
		HBRUSH hBrush)
			: SIZE{size},
				POSITION{position},
				hDc{Rain::Windows::validateSystemCall(CreateCompatibleDC(hDc))},
				hDcAA{Rain::Windows::validateSystemCall(CreateCompatibleDC(hDc))},
				hBitmap{Rain::Windows::validateSystemCall(
					CreateCompatibleBitmap(hDc, size.X, size.Y))},
				hOrigBitmap{static_cast<HBITMAP>(Rain::Windows::validateSystemCall(
					SelectObject(this->hDc, this->hBitmap)))},
				hBitmapAA{Rain::Windows::validateSystemCall(CreateCompatibleBitmap(
					hDc,
					size.X * Chunk::AA_SCALE,
					size.Y * Chunk::AA_SCALE))},
				hOrigBitmapAA{static_cast<HBITMAP>(Rain::Windows::validateSystemCall(
					SelectObject(this->hDcAA, this->hBitmapAA)))} {
		SetStretchBltMode(this->hDc, HALFTONE);
		RECT rect{0, 0, size.X, size.Y};
		Rain::Windows::validateSystemCall(FillRect(this->hDc, &rect, hBrush));
	}
	Chunk::~Chunk() {
		SelectObject(this->hDcAA, this->hOrigBitmapAA);
		DeleteObject(this->hBitmapAA);
		DeleteDC(this->hDcAA);
		SelectObject(this->hDc, this->hOrigBitmap);
		DeleteObject(this->hBitmap);
		DeleteDC(this->hDc);
	}

	void Chunk::drawPath(std::shared_ptr<Path const> const &path, HPEN hPen) {
		using Rain::Windows::validateSystemCall;
		std::vector<Path::Point> const &points{path->getPoints()};
		HPEN hOrigPen{
			static_cast<HPEN>(validateSystemCall(SelectObject(this->hDcAA, hPen)))};
		validateSystemCall(MoveToEx(
			this->hDcAA,
			(points[0].first - this->POSITION.X) * Chunk::AA_SCALE,
			(points[0].second - this->POSITION.Y) * Chunk::AA_SCALE,
			NULL));
		for (std::size_t i{1}; i < points.size(); i++) {
			validateSystemCall(LineTo(
				this->hDcAA,
				(points[i].first - this->POSITION.X) * Chunk::AA_SCALE,
				(points[i].second - this->POSITION.Y) * Chunk::AA_SCALE));
		}
		validateSystemCall(SelectObject(this->hDcAA, hOrigPen));
		validateSystemCall(StretchBlt(
			this->hDc,
			0,
			0,
			this->SIZE.X,
			this->SIZE.Y,
			this->hDcAA,
			0,
			0,
			this->SIZE.X * Chunk::AA_SCALE,
			this->SIZE.Y * Chunk::AA_SCALE,
			SRCCOPY));
	}
}
