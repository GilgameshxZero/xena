#include <pen.hpp>

namespace Xena {
	Pen::Pen(Painter &painter) : painter(painter) {}

	void Pen::onPenDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"Pen::onPenDown: (", position.x, ", ", position.y, ").");
	}
	void Pen::onPenUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"Pen::onPenUp: (", position.x, ", ", position.y, ").");
	}
	void Pen::onPenMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {}
}
