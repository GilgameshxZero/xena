#include <touch.hpp>

namespace Xena {
	Touch::Touch(PanHandler &panHandler) : panHandler(panHandler) {}
	
	void Touch::onTouchDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		this->panHandler.onPanBegin(interaction, now, position);
	}
	void Touch::onTouchUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		this->panHandler.onPanEnd(interaction, now, position);
	}
	void Touch::onTouchMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		this->panHandler.onPanUpdate(interaction, now, position);
	}
}
