#pragma once

#include <rain.hpp>

namespace Xena {
	class Chunk;

	class Path {
		private:
		static inline std::size_t ID_NEXT{0};
		std::size_t const ID;

		std::vector<Gdiplus::Point> points;
		Gdiplus::GraphicsPath path;

		std::pair<Gdiplus::Point, Gdiplus::Point> bounds;

		public:
		Path();

		std::vector<Gdiplus::Point> const &getPoints() const;

		void addPoint(Gdiplus::Point const &);
	};
}
