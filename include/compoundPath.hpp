#pragma once

#include <point.hpp>

#include <rain.hpp>

namespace Xena {
	class CompoundPath {
		public:
		std::vector<POINT> points;

		private:
		std::vector<Point> containingChunks;
	};
}
