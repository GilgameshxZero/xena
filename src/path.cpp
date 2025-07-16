#include <path.hpp>

namespace Xena {
	Path::Path() : ID{Path::ID_NEXT++} {}

	std::vector<Gdiplus::PointF> const &Path::getPoints() const {
		return this->points;
	}
	Gdiplus::GraphicsPath const &Path::getPath() const {
		return this->path;
	}
	void Path::addPoint(Gdiplus::PointF const &point) {
		this->path.AddLine(this->points.back(), point);
		this->points.push_back(point);
	}
}
