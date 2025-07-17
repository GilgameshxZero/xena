#pragma once

#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk {
		private:
		Gdiplus::Point const SIZE, POSITION;

		public:
		HDC const hDc;

		private:
		HBITMAP const hBitmap, hOrigBitmap;

		public:
		Chunk(HDC, Gdiplus::Point const &, Gdiplus::Point const &, HBRUSH);
		~Chunk();

		// Draw path with specified brush with the chunk offset. To erase, use a
		// brush with the same color as the background.
		void drawPath(std::shared_ptr<Path const> const &, HPEN);
	};
}
