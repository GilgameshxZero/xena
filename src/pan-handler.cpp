#include <pan-handler.hpp>

namespace Xena {
	PanHandler::PanHandler(Painter &painter) : painter(painter) {}

	void PanHandler::onPanBegin(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		Rain::Log::verbose("PanHandler::onPanBegin: ", position, ".");

		this->origViewportPosition = this->painter.getViewportPosition();
		this->origPanPosition = position;
	}
	void PanHandler::onPanEnd(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		if (
			interaction.contactSizeMax >= Interaction::CONTACT_SIZE_THRESHOLD ||
			now - interaction.timeDown <= Interaction::IGNORE_SHORT_THRESHOLD) {
			Rain::Log::verbose("PanHandler::onPanEnd: IGNORE.");
			return;
		}
		Rain::Log::verbose("PanHandler::onPanEnd: ", position, ".");

		this->painter.updateViewportPosition(
			this->origViewportPosition + this->origPanPosition - position);
		this->painter.rePaint();
	}
	void PanHandler::onPanUpdate(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		// Suspect palm touch if contact size is too large or event is too short.
		if (
			interaction.contactSizeMax >= Interaction::CONTACT_SIZE_THRESHOLD ||
			now - interaction.timeDown <= Interaction::IGNORE_SHORT_THRESHOLD) {
			return;
		}

		this->painter.updateViewportPosition(
			this->origViewportPosition + this->origPanPosition - position);
		this->painter.rePaint();
	}
}
