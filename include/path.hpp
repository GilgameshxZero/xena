#pragma once

#include <point-ll.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk;

	class Path {
		private:
		std::vector<POINT> points;
		std::vector<std::shared_ptr<Chunk>> containingChunks;

		std::pair<PointLl, PointLl> bounds;

		public:
		std::vector<POINT> const getPoints() const;
		std::vector<std::shared_ptr<Chunk> const> const getContainingChunks() const;
	};
}
