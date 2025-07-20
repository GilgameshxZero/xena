#include <eraser.hpp>

namespace Xena {
	Eraser::Eraser(Painter &painter) : painter(painter) {}

	void Eraser::onEraserDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		Rain::Log::verbose("Eraser::onEraserDown: ", position, ".");
	}
	void Eraser::onEraserUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		Rain::Log::verbose("Eraser::onEraserUp: ", position, ".");
	}
	void Eraser::onEraserMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {}
}
