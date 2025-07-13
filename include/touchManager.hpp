#pragma once

#include <interactSequence.hpp>
#include <windowManager.hpp>

#include <rain.hpp>

namespace Xena {
	class TouchManager {
		private:
		WindowManager &windowManager;

		public:
		TouchManager(WindowManager &);

		void onTouchDown(
			InteractSequence &,
			std::chrono::steady_clock::time_point,
			POINT);
		void
		onTouchUp(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
		void onTouchMove(
			InteractSequence &,
			std::chrono::steady_clock::time_point,
			POINT);
	};
}
