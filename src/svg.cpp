#include <svg.hpp>

namespace Xena {
	Svg::Svg(
		std::string const &fileToLoad,
		PointL &viewportPosition,
		Paths &paths)
			: filePath{fileToLoad}, viewportPosition{viewportPosition}, paths{paths} {
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
		Rain::Log::verbose(
			"Svg::save: Saved ",
			this->paths.size(),
			" paths to \"",
			this->filePath,
			"\".");
	}
}
