#include <svg.hpp>

namespace Xena {
	Svg::Svg(
		long double dpToPx,
		long double strokeWidthPx,
		std::string const &fileToLoad,
		PointL &viewportPosition,
		Paths &paths)
			: COORDINATE_SCALE_DP{Svg::COORDINATE_SCALE_PX / dpToPx},
				STROKE_WIDTH_PX{strokeWidthPx},
				filePath{fileToLoad},
				viewportPosition{viewportPosition},
				paths{paths} {
		this->load();
	}

	void Svg::load() {
		std::ifstream in(this->filePath, std::ios::binary);
		std::string buffer, tag;
		std::getline(in, buffer, '<');
		while (in) {
			std::getline(in, buffer, ' ');
			tag = buffer;
			Rain::Log::verbose("Svg::load: Found tag ", tag, ".");
			std::getline(in, buffer, '<');
		}
		Rain::Log::verbose(
			"Svg::load: Loaded ",
			this->paths.size(),
			" paths from \"",
			this->filePath,
			"\".");
	}
	void Svg::save() {
		std::ofstream out(this->filePath, std::ios::binary);
		std::stringstream ss;
		Rain::Algorithm::Geometry::RectangleL bounds{
			LONG_MAX, LONG_MAX, LONG_MIN, LONG_MIN};
		for (auto &i : this->paths) {
			std::shared_ptr<Path const> path{i.second.first};
			auto const &points{path->getPoints()};
			Rain::Algorithm::Geometry::PointLd error;
			Rain::Algorithm::Geometry::PointL delta;

			delta = (points[0] * this->COORDINATE_SCALE_DP).round<long>();
			error = points[0] * this->COORDINATE_SCALE_DP - delta;
			ss << "<path d=\"M" << delta.x << ' ' << delta.y << 'l';
			bounds.include(delta);

			for (std::size_t j{1}; j < points.size(); j++) {
				delta =
					((points[j] - points[j - 1]) * this->COORDINATE_SCALE_DP + error)
						.round<long>();
				error = (points[j] - points[j - 1]) * this->COORDINATE_SCALE_DP +
					error - delta;
				if (delta.x >= 0) {
					ss << ' ';
				}
				ss << delta.x;
				if (delta.y >= 0) {
					ss << ' ';
				}
				ss << delta.y;
				bounds.include((points[j] * this->COORDINATE_SCALE_DP).round<long>());
			}
			ss << "\"/>\n";
		}
		bounds.expand(
			std::lroundl(this->STROKE_WIDTH_PX * this->COORDINATE_SCALE_DP));
		auto viewportPositionRounded{
			(this->viewportPosition * this->COORDINATE_SCALE_DP /
			 this->COORDINATE_SCALE_PX / 160l)
				.round<long>()};
		out << "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"" << bounds.left
				<< ' ' << bounds.top << ' ' << bounds.width() << ' ' << bounds.height()
				<< "\" stroke=\"black\" stroke-width=\""
				<< std::lroundl(this->STROKE_WIDTH_PX * this->COORDINATE_SCALE_DP)
				<< "\" stroke-linecap=\"round\" stroke-linejoin=\"round\" "
					 "fill=\"none\" data-xena=\""
				<< -viewportPositionRounded.x << ' ' << -viewportPositionRounded.y
				<< " 0\">"
				<< "<style>@media(prefers-color-scheme:dark){svg{background-color:"
					 "black;stroke:white;}}</style>\n"
				<< ss.rdbuf() << "</svg>\n";

		Rain::Log::verbose(
			"Svg::save: Saved ",
			this->paths.size(),
			" paths to \"",
			this->filePath,
			"\".");
	}
}
