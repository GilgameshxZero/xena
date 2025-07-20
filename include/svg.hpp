#pragma once

#include <painter.hpp>

#include <rain.hpp>

namespace Xena {
	class Svg {
		private:
		static inline long double const COORDINATE_SCALE_PX{12.0l};
		long double const COORDINATE_SCALE_DP;
		long double const STROKE_WIDTH_PX;

		public:
		using PointL = Rain::Algorithm::Geometry::PointL;

		private:
		Painter &painter;
		std::string filePath;

		public:
		Svg(Painter &, std::string const &);

		void load();
		void save();
	};
}
