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
				hBitmap{Rain::Windows::validateSystemCall(
					CreateCompatibleBitmap(this->hDc, size.X, size.Y))},
				hOrigBitmap{static_cast<HBITMAP>(Rain::Windows::validateSystemCall(
					SelectObject(this->hDc, this->hBitmap)))} {
		RECT rect{0, 0, size.X, size.Y};
		Rain::Windows::validateSystemCall(FillRect(this->hDc, &rect, hBrush));
	}
	Chunk::~Chunk() {
		SelectObject(this->hDc, this->hOrigBitmap);
		DeleteObject(this->hBitmap);
		DeleteDC(this->hDc);
	}

	void Chunk::drawPath(std::shared_ptr<Path const> const &path, HPEN hPen) {
		using Rain::Windows::validateSystemCall;
		std::vector<Gdiplus::PointF> const &pointFs{path->getPointFs()};
		HPEN hOrigPen{
			static_cast<HPEN>(validateSystemCall(SelectObject(this->hDc, hPen)))};
		validateSystemCall(MoveToEx(
			this->hDc,
			pointFs[0].X - this->POSITION.X,
			pointFs[0].Y - this->POSITION.Y,
			NULL));
		for (std::size_t i{1}; i < pointFs.size(); i++) {
			validateSystemCall(LineTo(
				this->hDc,
				pointFs[i].X - this->POSITION.X,
				pointFs[i].Y - this->POSITION.Y));
		}
		validateSystemCall(SelectObject(this->hDc, hOrigPen));
	}
}
