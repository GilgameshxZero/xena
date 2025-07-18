#pragma once

#include <rain.hpp>

namespace Xena {
	class Path {
		public:
		using PointLd = Rain::Algorithm::Geometry::PointLd;

		private:
		static inline std::size_t ID_NEXT{0};

		std::vector<PointLd> points;

		public:
		std::size_t const ID;

		Path();

		std::vector<PointLd> const &getPoints() const;
		void addPoint(PointLd const &);
	};
}
