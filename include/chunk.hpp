#pragma once

#include <path.hpp>
#include <point-ll.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk {
		private:
		PointLl const SIZE, OFFSET;

		public:
		HDC hDc;
		HBITMAP hBitmap;

		Chunk(HDC, PointLl const &, PointLl const &, HBRUSH);
		~Chunk();

		// Draw path with specified brush. To erase, use a blank or transparent
		// brush.
		void drawPath(std::shared_ptr<Path> const &, HPEN);
	};
}
