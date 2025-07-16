#include <path.hpp>

namespace Xena {
	Path::Path() : ID{Path::ID_NEXT++} {}

	std::vector<POINT> const &Path::getPoints() const {
		return this->points;
	}

	void Path::addPoint(POINT const &point) {
		this->points.push_back(point);
	}
}
