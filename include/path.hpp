#pragma once

#include <rain.hpp>

namespace Xena {
	class Path {
		public:
		using Point = std::pair<long double, long double>;

		private:
		static inline std::size_t ID_NEXT{0};

		std::vector<Point> points;

		public:
		std::size_t const ID;

		Path();

		std::vector<Point> const &getPoints() const;
		void addPoint(Point const &);
	};
}
