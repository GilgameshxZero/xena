#include <point.hpp>

namespace Xena {
	bool Point::operator==(Point const &other) {
		return this->x == other.x && this->y == other.y;
	}
}

std::size_t std::hash<Xena::Point>::operator()(
	Xena::Point const &point) const noexcept {
	std::size_t h1{std::hash<std::size_t>{}(point.x)},
		h2{std::hash<std::size_t>{}(point.y)};
	return h1 ^ (h2 << 1);
}
