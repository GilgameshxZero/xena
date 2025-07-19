#pragma once

#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Chunk {
		public:
		using PointL = Rain::Algorithm::Geometry::PointL;

		static inline long const AA_SCALE{2l};

		private:
		PointL const SIZE, POSITION;
		Rain::Windows::Bitmap const bitmap, bitmapAa;

		public:
		Rain::Windows::DeviceContextMemory dc;

		private:
		Rain::Windows::DeviceContextMemory dcAa;

		public:
		Chunk(
			HDC,
			PointL const &,
			PointL const &,
			Rain::Windows::SolidBrush const &);

		// Draw path with specified brush with the chunk offset. To erase, use a
		// brush with the same color as the background.
		void drawPath(
			std::shared_ptr<Path const> const &,
			Rain::Windows::SolidPen const &);
	};
}
