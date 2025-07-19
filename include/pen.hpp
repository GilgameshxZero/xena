#pragma once

#include <interaction.hpp>
#include <painter.hpp>

#include <rain.hpp>

namespace Xena {
	class Pen {
		public:
		using PointL = Rain::Algorithm::Geometry::PointL;

		private:
		long double const HIMETRIC_TO_PX;

		Painter &painter;

		std::shared_ptr<Path> path;

		public:
		bool isDrawing{false};

		Pen(Painter &);

		void onPenDown(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
		void onPenUp(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
		void onPenMove(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
	};
}
