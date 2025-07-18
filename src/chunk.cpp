#include <chunk.hpp>

namespace Xena {
	Chunk::Chunk(
		HDC hDc,
		PointLl const &size,
		PointLl const &position,
		Rain::Windows::SolidBrush const &brush)
			: SIZE{size},
				POSITION{position},
				hDc{Rain::Windows::validateSystemCall(CreateCompatibleDC(hDc))},
				hDcAA{Rain::Windows::validateSystemCall(CreateCompatibleDC(hDc))},
				hBitmap{Rain::Windows::validateSystemCall(
					CreateCompatibleBitmap(hDc, size.x, size.y))},
				hOrigBitmap{static_cast<HBITMAP>(Rain::Windows::validateSystemCall(
					SelectObject(this->hDc, this->hBitmap)))},
				hBitmapAA{Rain::Windows::validateSystemCall(CreateCompatibleBitmap(
					hDc,
					size.x * Chunk::AA_SCALE,
					size.y * Chunk::AA_SCALE))},
				hOrigBitmapAA{static_cast<HBITMAP>(Rain::Windows::validateSystemCall(
					SelectObject(this->hDcAA, this->hBitmapAA)))} {
		Rain::Windows::validateSystemCall(SetStretchBltMode(this->hDc, HALFTONE));
		RECT rect{0, 0, size.x, size.y};
		Rain::Windows::validateSystemCall(FillRect(this->hDc, &rect, brush));
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
		std::vector<Path::PointLd> const &points{path->getPoints()};
		HPEN hOrigPen{
			static_cast<HPEN>(validateSystemCall(SelectObject(this->hDcAA, hPen)))};
		validateSystemCall(MoveToEx(
			this->hDcAA,
			(points[0].x - this->POSITION.x) * Chunk::AA_SCALE,
			(points[0].y - this->POSITION.y) * Chunk::AA_SCALE,
			NULL));
		for (std::size_t i{1}; i < points.size(); i++) {
			validateSystemCall(LineTo(
				this->hDcAA,
				(points[i].x - this->POSITION.x) * Chunk::AA_SCALE,
				(points[i].y - this->POSITION.y) * Chunk::AA_SCALE));
		}
		validateSystemCall(SelectObject(this->hDcAA, hOrigPen));
		validateSystemCall(StretchBlt(
			this->hDc,
			0,
			0,
			this->SIZE.x,
			this->SIZE.y,
			this->hDcAA,
			0,
			0,
			this->SIZE.x * Chunk::AA_SCALE,
			this->SIZE.y * Chunk::AA_SCALE,
			SRCCOPY));
	}
}
