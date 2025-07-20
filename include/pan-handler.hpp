#pragma once

#include <interaction.hpp>
#include <painter.hpp>

#include <rain.hpp>

namespace Xena {
	class PanHandler {
		public:
		using PointL = Rain::Algorithm::Geometry::PointL;

		private:
		Painter &painter;
		PointL origViewportPosition, origPanPosition;

		public:
		PanHandler(Painter &);

		void onPanBegin(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
		void onPanEnd(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
		void onPanUpdate(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
	};
}
