#include <chunk.hpp>

namespace Xena {
	Chunk::Chunk(
		HDC hDc,
		PointLl const &size,
		PointLl const &offset,
		HBRUSH backgroundBrush)
			: SIZE{size},
				OFFSET{offset},
				hDc{Rain::Windows::validateSystemCall(CreateCompatibleDC(hDc))},
				hBitmap{Rain::Windows::validateSystemCall(CreateCompatibleBitmap(
					this->hDc,
					static_cast<int>(size.x),
					static_cast<int>(size.y)))} {
		using Rain::Windows::validateSystemCall;

		RECT rect{0, 0, static_cast<LONG>(size.x), static_cast<LONG>(size.y)};

		validateSystemCall(SelectObject(this->hDc, this->hBitmap));
		validateSystemCall(FillRect(this->hDc, &rect, backgroundBrush));
	}
	Chunk::~Chunk() {
		DeleteObject(this->hBitmap);
		DeleteDC(this->hDc);
	}

	void Chunk::drawPath(std::shared_ptr<Path> const &path, HPEN pen) {
		using Rain::Windows::validateSystemCall;

		std::vector<POINT> const &points{path->getPoints()};
		validateSystemCall(
			MoveToEx(this->hDc, points.front().x, points.front().y, NULL));
		validateSystemCall(SelectObject(this->hDc, pen));
		for (std::size_t i{1}; i < points.size(); i++) {
			validateSystemCall(LineTo(this->hDc, points.front().x, points.front().y));
		}
	}
}
