#include <svg.hpp>

namespace Xena {
	Svg::Svg(std::string const &fileToLoad, UINT dpi)
			: filePath(fileToLoad),
				CHUNK_SIZE_PX{
					std::llroundl(
						static_cast<long double>(CHUNK_SIZE_DP.x) * dpi /
						USER_DEFAULT_SCREEN_DPI),
					std::llroundl(
						static_cast<long double>(CHUNK_SIZE_DP.y) * dpi /
						USER_DEFAULT_SCREEN_DPI)} {
		this->load();
	}

	void Svg::load() {
		std::ifstream in(this->filePath, std::ios::binary);
		std::stack<std::string> tags;
		std::string buffer;
		in >> buffer;
		while (in) {
			if (buffer[0] != '<') {
				throw std::ios_base::failure("Invalid starting tag.");
			}
			if (buffer[1] == '/') {
				if (tags.top() != buffer.substr(2)) {
					throw std::ios_base::failure("Invalid closing tag.");
				}
				tags.pop();
			}
		}
		in.close();
		Rain::Log::verbose(
			"Svg::load: Loaded ",
			this->paths.size(),
			" paths from \"",
			this->filePath,
			"\".");
	}
	void Svg::save() {
		Rain::Log::verbose(
			"Svg::load: Saved ",
			this->paths.size(),
			" paths to \"",
			this->filePath,
			"\".");
	}
}
