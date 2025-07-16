#include <chunk.hpp>

namespace Xena {
	Chunk::Chunk(HDC hDc, PointLl const &size, PointLl const &offset)
			: SIZE{size},
				OFFSET{offset},
				hDc{Rain::Windows::validateSystemCall(CreateCompatibleDC(hDc))},
				hBitmap{Rain::Windows::validateSystemCall(CreateCompatibleBitmap(
					this->hDc,
					static_cast<int>(size.x),
					static_cast<int>(size.y)))} {
		Rain::Windows::validateSystemCall(SelectObject(this->hDc, this->hBitmap));
	}
	Chunk::~Chunk() {
		DeleteObject(this->hBitmap);
		DeleteDC(this->hDc);
	}
}
