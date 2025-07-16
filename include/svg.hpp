#pragma once

#include <chunk.hpp>
#include <path.hpp>
#include <point-ll.hpp>

#include <rain.hpp>

namespace Xena {
	class Svg {
		private:
		std::string filePath;

		PointLl &viewportOffset;
		std::unordered_map<
			std::size_t,
			std::pair<std::shared_ptr<Path>, std::unordered_set<PointLl>>> &paths;

		public:
		Svg(
			std::string const &,
			PointLl &,
			std::unordered_map<
				std::size_t,
				std::pair<std::shared_ptr<Path>, std::unordered_set<PointLl>>> &);

		void load();
		void save();
	};
}
