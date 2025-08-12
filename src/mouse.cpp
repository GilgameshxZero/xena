#include <rain.hpp>

#include <mouse.hpp>

namespace Xena {
	Mouse::Mouse(PanHandler &panHandler) : panHandler(panHandler) {}

	void Mouse::onMouseDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		this->panHandler.onPanBegin(interaction, now, position);
	}
	void Mouse::onMouseUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		this->panHandler.onPanEnd(interaction, now, position);
	}
	void Mouse::onMouseMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		this->panHandler.onPanUpdate(interaction, now, position);
	}
}
