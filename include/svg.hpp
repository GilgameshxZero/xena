#pragma once

#include <chunk.hpp>
#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Svg {
		private:
		std::string filePath;

		Gdiplus::Point &viewportOffset;
		std::unordered_map<
			std::size_t,
			std::pair<std::shared_ptr<Path>, std::unordered_set<Gdiplus::Point>>>
			&paths;

		public:
		Svg(
			std::string const &,
			Gdiplus::Point &,
			std::unordered_map<
				std::size_t,
				std::pair<std::shared_ptr<Path>, std::unordered_set<Gdiplus::Point>>>
				&);

		void load();
		void save();
	};
}
