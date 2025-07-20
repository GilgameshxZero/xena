#include <path.hpp>

namespace Xena {
	Path::Path() : ID{Path::ID_NEXT++} {
		Rain::Console::log("Path::Path: Created path ", this->ID, ".");
	}

	std::vector<Path::PointLd> const &Path::getPoints() const {
		return this->points;
	}
	void Path::addPoint(PointLd const &point) {
		this->points.emplace_back(point);
	}
}
