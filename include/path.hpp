#pragma once

#include <point-ll.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk;

	class Path {
		private:
		static inline std::size_t ID_NEXT{0};
		std::size_t const ID;

		std::vector<POINT> points;

		std::pair<PointLl, PointLl> bounds;

		public:
		Path();

		std::vector<POINT> const &getPoints() const;

		void addPoint(POINT const &);
	};
}
