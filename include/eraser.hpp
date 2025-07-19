#pragma once

#include <interaction.hpp>
#include <painter.hpp>

#include <rain.hpp>

namespace Xena {
	class Eraser {
		public:
		using PointL = Rain::Algorithm::Geometry::PointL;

		private:
		Painter &painter;

		public:
		Eraser(Painter &);

		void onEraserDown(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
		void onEraserUp(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
		void onEraserMove(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
	};
}
