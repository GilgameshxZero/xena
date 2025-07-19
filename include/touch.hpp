#pragma once

#include <interaction.hpp>
#include <painter.hpp>

#include <rain.hpp>

namespace Xena {
	class Touch {
		public:
		using PointL = Rain::Algorithm::Geometry::PointL;

		private:
		Painter &painter;
		PointL origViewportPosition, origPanPosition;

		public:
		Touch(Painter &);

		void onTouchDown(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
		void onTouchUp(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
		void onTouchMove(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
	};
}
