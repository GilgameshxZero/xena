#pragma once

#include <interaction.hpp>
#include <pan-handler.hpp>

#include <rain.hpp>

namespace Xena {
	class Mouse {
		public:
		using PointL = Rain::Algorithm::Geometry::PointL;

		private:
		PanHandler &panHandler;

		public:
		Mouse(PanHandler &);

		void onMouseDown(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
		void onMouseUp(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
		void onMouseMove(
			Interaction &,
			std::chrono::steady_clock::time_point const &,
			PointL const &);
	};
}
