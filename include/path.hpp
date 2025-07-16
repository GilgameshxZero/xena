#pragma once

#include <rain.hpp>

namespace Xena {
	class Chunk;

	class Path {
		private:
		static inline std::size_t ID_NEXT{0};
		std::size_t const ID;

		std::vector<Gdiplus::PointF> points;
		Gdiplus::GraphicsPath path;
		Gdiplus::RectF bounds;

		public:
		Path();

		std::vector<Gdiplus::PointF> const &getPoints() const;
		Gdiplus::GraphicsPath const &getPath() const;
		void addPoint(Gdiplus::PointF const &);
	};
}
