#pragma once

#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk {
		private:
		Gdiplus::Point const SIZE, OFFSET;

		public:
		HDC hDc;
		HBITMAP hBitmap;

		Chunk(HDC, Gdiplus::Point const &, Gdiplus::Point const &, HBRUSH);
		~Chunk();

		// Draw path with specified brush. To erase, use a blank or transparent
		// brush.
		void drawPath(std::shared_ptr<Path> const &, HPEN);
	};
}
