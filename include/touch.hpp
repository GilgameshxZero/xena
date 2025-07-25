#pragma once

#include <interaction.hpp>
#include <pan-handler.hpp>

#include <rain.hpp>

namespace Xena {
	class Touch {
		public:
		using PointL = Rain::Algorithm::Geometry::PointL;

		private:
		PanHandler &panHandler;

		public:
		Touch(PanHandler &);

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
