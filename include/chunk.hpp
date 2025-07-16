#pragma once

#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk {
		private:
		Gdiplus::Point const SIZE, POSITION;

		public:
		Gdiplus::Bitmap bitmap;

		private:
		Gdiplus::Graphics graphics;

		public:
		Chunk(
			Gdiplus::Point const &,
			Gdiplus::Point const &,
			Gdiplus::Brush const &);

		// Draw path with specified brush with the chunk offset. To erase, use a
		// blank or transparent brush.
		void drawPath(std::shared_ptr<Path const> const &, Gdiplus::Pen const &);
	};
}
