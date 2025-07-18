#pragma once

#include <chunk.hpp>
#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Svg {
		public:
		using PointLl = Rain::Algorithm::Geometry::PointLl;
		using Paths = std::unordered_map<
			std::size_t,
			std::pair<std::shared_ptr<Path const>, std::unordered_set<PointLl>>>;

		private:
		std::string filePath;

		PointLl &viewportPosition;
		Paths &paths;

		public:
		Svg(std::string const &, PointLl &, Paths &);

		void load();
		void save();
	};
}
