#pragma once

#include <rain.hpp>

namespace Xena {
	class Point {
		public:
		std::size_t x, y;

		bool operator==(Point const &);
	};
}

template <>
struct std::hash<Xena::Point> {
	std::size_t operator()(Xena::Point const &) const noexcept;
};
