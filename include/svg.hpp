#pragma once

#include <chunk.hpp>
#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Svg {
		public:
		using Paths = std::unordered_map<
			std::size_t,
			std::pair<std::shared_ptr<Path const>, std::unordered_set<Gdiplus::Point>>>;

		private:
		std::string filePath;

		Gdiplus::Point &viewportPosition;
		Paths &paths;

		public:
		Svg(std::string const &, Gdiplus::Point &, Paths &);

		void load();
		void save();
	};
}
