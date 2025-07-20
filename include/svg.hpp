#pragma once

#include <chunk.hpp>
#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Svg {
		private:
		static inline long double const COORDINATE_SCALE_PX{12.0l};
		long double const COORDINATE_SCALE_DP;
		long double const STROKE_WIDTH_PX;

		public:
		using PointL = Rain::Algorithm::Geometry::PointL;
		using Paths = std::unordered_map<
			std::size_t,
			std::pair<std::shared_ptr<Path const>, std::unordered_set<PointL>>>;

		private:
		std::string filePath;

		PointL &viewportPosition;
		Paths &paths;

		public:
		Svg(long double, long double, std::string const &, PointL &, Paths &);

		void load();
		void save();
	};
}
