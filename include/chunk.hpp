#pragma once

#include <path.hpp>
#include <point-ll.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk {
		private:
		PointLl const SIZE, OFFSET;

		std::unordered_set<std::shared_ptr<Path>> paths;

		HDC hDc;
		HBITMAP hBitmap;

		public:
		Chunk(HDC, PointLl const &, PointLl const &);
		~Chunk();
	};
}
