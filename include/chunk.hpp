#pragma once

#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk {
		private:
		Gdiplus::Point const SIZE, OFFSET;

		public:
		HDC hDc;

		private:
		HBITMAP hBitmap, hOrigBitmap;
		Gdiplus::Graphics graphics;

		public:
		Chunk(
			HDC,
			Gdiplus::Point const &,
			Gdiplus::Point const &,
			Gdiplus::Brush const &);
		~Chunk();

		// Draw path with specified brush. To erase, use a blank or transparent
		// brush.
		void drawPath(std::shared_ptr<Path> const &, Gdiplus::Pen const &);
	};
}
