#pragma once

#include <chunk.hpp>
#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Svg {
		private:
		std::string filePath;

		std::pair<int, int> &viewportOffset;
		std::unordered_map<
			std::size_t,
			std::pair<std::shared_ptr<Path>, std::unordered_set<std::pair<int, int>>>> &paths;

		public:
		Svg(
			std::string const &,
			std::pair<int, int> &,
			std::unordered_map<
				std::size_t,
				std::pair<std::shared_ptr<Path>, std::unordered_set<std::pair<int, int>>>> &);

		void load();
		void save();
	};
}
