#include <mouse.hpp>

namespace Xena {
	Mouse::Mouse(Painter &painter) : painter(painter) {}

	void Mouse::onMouseDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"Mouse::onMouseDown: (", position.x, ", ", position.y, ").");
	}
	void Mouse::onMouseUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"Mouse::onMouseUp: (", position.x, ", ", position.y, ").");
	}
	void Mouse::onMouseMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {}
}
