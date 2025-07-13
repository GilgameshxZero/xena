#pragma once

#include <interactSequence.hpp>
#include <windowManager.hpp>

#include <rain.hpp>

namespace Xena {
	class PenManager {
		private:
		WindowManager &windowManager;

		public:
		PenManager(WindowManager &);

		void
		onPenDown(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
		void
		onPenUp(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
		void
		onPenMove(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
		void onEraserDown(
			InteractSequence &,
			std::chrono::steady_clock::time_point,
			POINT);
		void onEraserUp(
			InteractSequence &,
			std::chrono::steady_clock::time_point,
			POINT);
		void onEraserMove(
			InteractSequence &,
			std::chrono::steady_clock::time_point,
			POINT);
	};
}
