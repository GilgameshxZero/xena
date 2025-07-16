#include <svg.hpp>

namespace Xena {
	Svg::Svg(
		std::string const &fileToLoad,
		Gdiplus::Point &viewportOffset,
		std::unordered_map<
			std::size_t,
			std::pair<std::shared_ptr<Path>, std::unordered_set<Gdiplus::Point>>>
			&paths)
			: filePath{fileToLoad}, viewportOffset{viewportOffset}, paths{paths} {
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
