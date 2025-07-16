#include <point-ll.hpp>

namespace Xena {
	bool PointLl::operator==(PointLl const &pointLl) {
		return this->x == pointLl.x && this->y == pointLl.y;
	}
}

std::size_t std::hash<Xena::PointLl>::operator()(
	Xena::PointLl const &pointLl) const noexcept {
	std::size_t h1{std::hash<long long>{}(pointLl.x)},
		h2{std::hash<long long>{}(pointLl.y)};
	return h1 ^ (h2 << 1);
}
