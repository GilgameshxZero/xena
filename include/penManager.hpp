#include <interactSequence.hpp>

#include <rain.hpp>

namespace Xena {
	class PenManager {
		public:
		static void
		onPenDown(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
		static void
		onPenUp(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
		static void
		onPenMove(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
		static void
		onEraserDown(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
		static void
		onEraserUp(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
		static void
		onEraserMove(InteractSequence &, std::chrono::steady_clock::time_point, POINT);
	};
}
