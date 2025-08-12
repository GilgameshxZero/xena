#include <rain.hpp>

#include <svg.hpp>

namespace Xena {
	Svg::Svg(Painter &painter, std::string const &fileToLoad)
			: COORDINATE_SCALE_DP{Svg::COORDINATE_SCALE_PX / painter.DP_TO_PX},
				STROKE_WIDTH_PX{painter.STROKE_WIDTH_PX},
				painter{painter},
				filePath{fileToLoad} {
		this->load();
	}

	void Svg::load() {
		std::ifstream in(this->filePath, std::ios::binary);
		std::string buffer;
		std::size_t location, lagger;
		std::stringstream ss;

		std::getline(in, buffer);
		lagger = buffer.find("data-xena=\"");
		if (lagger == buffer.npos) {
			return;
		}
		location = buffer.find("\"", lagger + 11);
		Rain::Algorithm::Geometry::PointLd viewportPositionLd;
		ss << buffer.substr(lagger, location);
		ss >> viewportPositionLd.x >> viewportPositionLd.y;
		this->painter.updateViewportPosition(
			(viewportPositionLd * -160.0l * Svg::COORDINATE_SCALE_PX /
			 this->COORDINATE_SCALE_DP)
				.round<long>());

		std::getline(in, buffer);
		while (in) {
			lagger = buffer.find("d=\"");
			if (lagger == buffer.npos) {
				break;
			}
			while (buffer.back() != '\"') {
				buffer.pop_back();
			}
			buffer.pop_back();

			std::shared_ptr<Path> path(new Path);
			Rain::Algorithm::Geometry::PointLd point;
			auto const &points{path->getPoints()};
			location = buffer.find("l", lagger + 3);
			ss.str("");
			ss.clear();
			ss << buffer.substr(lagger + 4, location);
			ss >> point.x >> point.y;
			path->addPoint(point / this->COORDINATE_SCALE_DP);

			location++;
			while (location < buffer.size()) {
				lagger = std::min(
					buffer.find(' ', location + 1), buffer.find('-', location + 1));
				if (lagger == buffer.npos) {
					break;
				}
				point.x = std::stold(buffer.substr(location, lagger));
				location =
					std::min(buffer.find(' ', lagger + 1), buffer.find('-', lagger + 1));
				point.y = std::stold(buffer.substr(lagger, location));
				path->addPoint(points.back() + point / this->COORDINATE_SCALE_DP);
			}

			this->painter.addPath(path);
			std::getline(in, buffer);
		}

		Rain::Console::log(
			"Svg::load: Loaded ",
			this->painter.getPaths().size(),
			" paths from \"",
			this->filePath,
			"\".");
	}
	void Svg::save() {
		std::ofstream out(this->filePath, std::ios::binary);
		std::stringstream ss;
		Rain::Algorithm::Geometry::RectangleL bounds{
			LONG_MAX, LONG_MAX, LONG_MIN, LONG_MIN};
		for (auto &i : this->painter.getPaths()) {
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
				ss << (delta.x >= 0 ? " " : "") << delta.x;
				ss << (delta.y >= 0 ? " " : "") << delta.y;
				bounds.include((points[j] * this->COORDINATE_SCALE_DP).round<long>());
			}
			ss << "\"/>\n";
		}
		bounds.expand(
			std::lroundl(this->STROKE_WIDTH_PX * this->COORDINATE_SCALE_DP));
		auto viewportPositionRounded{
			(this->painter.getViewportPosition() * this->COORDINATE_SCALE_DP /
			 Svg::COORDINATE_SCALE_PX / 160.0l)
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
					 "black;stroke:white;}}</style>\n";
		out << ss.rdbuf();
		out.clear();
		out << "</svg>\n";

		Rain::Console::log(
			"Svg::save: Saved ",
			this->painter.getPaths().size(),
			" paths to \"",
			this->filePath,
			"\".");
	}
}
