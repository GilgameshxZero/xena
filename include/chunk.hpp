#pragma once

#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk {
		public:
		using PointLl = Rain::Algorithm::Geometry::PointLl;

		static inline long long const AA_SCALE{2};

		private:
		PointLl const SIZE, POSITION;

		public:
		HDC const hDc;

		private:
		HDC const hDcAA;
		HBITMAP const hBitmap, hOrigBitmap, hBitmapAA, hOrigBitmapAA;

		public:
		Chunk(
			HDC,
			PointLl const &,
			PointLl const &,
			Rain::Windows::SolidBrush const &);
		~Chunk();

		// Draw path with specified brush with the chunk offset. To erase, use a
		// brush with the same color as the background.
		void drawPath(std::shared_ptr<Path const> const &, HPEN);
	};
}
