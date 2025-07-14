#pragma once

#include <compoundPath.hpp>
#include <point.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk {
		private:
		Point const OFFSET;

		BITMAP bitmap;

		public:
		Chunk(Point const &);
	};
}
