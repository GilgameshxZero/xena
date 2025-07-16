#include <eraser.hpp>

namespace Xena {
	Eraser::Eraser(Painter &painter) : painter(painter) {}

	void Eraser::onEraserDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"Eraser::onEraserDown: (", position.x, ", ", position.y, ").");
	}
	void Eraser::onEraserUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"Eraser::onEraserUp: (", position.x, ", ", position.y, ").");
	}
	void Eraser::onEraserMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {}
}
