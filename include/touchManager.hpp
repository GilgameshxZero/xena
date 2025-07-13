#include <interactSequence.hpp>

#include <rain.hpp>

namespace Xena {
	class TouchManager {
		public:
		static void onTouchDown(
			InteractSequence &,
			std::chrono::steady_clock::time_point,
			POINT);
		static void
		onTouchUp(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
		static void onTouchMove(
			InteractSequence &,
			std::chrono::steady_clock::time_point,
			POINT);
	};
}
