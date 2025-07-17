#include <path.hpp>

namespace Xena {
	Path::Path() : ID{Path::ID_NEXT++} {
		Rain::Log::verbose("Path::Path: Created path ", this->ID, ".");
	}

	std::vector<Path::Point> const &Path::getPoints() const {
		return this->points;
	}
	void Path::addPoint(Point const &point) {
		this->points.emplace_back(point);
	}
}
