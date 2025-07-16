#include <path.hpp>

namespace Xena {
	Path::Path() : ID{Path::ID_NEXT++} {
		Rain::Log::verbose("Path::Path: Created path ", this->ID, ".");
	}

	std::vector<Gdiplus::PointF> const &Path::getPointFs() const {
		return this->pointFs;
	}
	void Path::addPoint(Gdiplus::PointF const &pointF) {
		this->pointFs.emplace_back(pointF);
	}
}
