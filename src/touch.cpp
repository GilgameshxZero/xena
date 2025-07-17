#include <touch.hpp>

namespace Xena {
	Touch::Touch(Painter &painter) : painter(painter) {}
	void Touch::onTouchDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"Touch::onTouchDown: (", position.x, ", ", position.y, ").");

		this->origViewportPosition = this->painter.getViewportPosition();
		this->origPanPosition = position;
	}
	void Touch::onTouchUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		if (
			interaction.contactSizeMax >= Interaction::CONTACT_SIZE_THRESHOLD ||
			now - interaction.timeDown <= Interaction::IGNORE_SHORT_THRESHOLD) {
			Rain::Log::verbose("Touch::onTouchUp: IGNORE.");
			return;
		}
		Rain::Log::verbose(
			"Touch::onTouchUp: (", position.x, ", ", position.y, ").");

		this->painter.updateViewportPosition(
			{this->origViewportPosition.X +
				 static_cast<int>((this->origPanPosition.x - position.x)),
			 this->origViewportPosition.Y +
				 static_cast<int>((this->origPanPosition.y - position.y))});
		this->painter.rePaint();
	}
	void Touch::onTouchMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		// Suspect palm touch if contact size is too large or event is too short.
		if (
			interaction.contactSizeMax >= Interaction::CONTACT_SIZE_THRESHOLD ||
			now - interaction.timeDown <= Interaction::IGNORE_SHORT_THRESHOLD) {
			return;
		}

		this->painter.updateViewportPosition(
			{this->origViewportPosition.X +
				 static_cast<int>((this->origPanPosition.x - position.x)),
			 this->origViewportPosition.Y +
				 static_cast<int>((this->origPanPosition.y - position.y))});
		this->painter.rePaint();
	}
}
