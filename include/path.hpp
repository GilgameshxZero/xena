#pragma once

#include <rain.hpp>

namespace Xena {
	class Chunk;

	class Path {
		private:
		static inline std::size_t ID_NEXT{0};

		std::vector<Gdiplus::PointF> pointFs;
		Gdiplus::RectF bounds;

		public:
		std::size_t const ID;

		Path();

		std::vector<Gdiplus::PointF> const &getPointFs() const;
		void addPoint(Gdiplus::PointF const &);
	};
}
