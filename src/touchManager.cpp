#include <main.hpp>
#include <touchManager.hpp>

namespace Xena {
	void TouchManager::onTouchDown(
		InteractSequence &sequence,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		// Rain::Log::verbose(
		// 	"TouchManager::onTouchMove: DOWN (", position.x, ", ", position.y,
		// ").");
	}
	void TouchManager::onTouchUp(
		InteractSequence &sequence,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		if (
			sequence.contactSizeMax >= InteractSequence::CONTACT_SIZE_THRESHOLD ||
			now - sequence.timeDown <= InteractSequence::IGNORE_SHORT_THRESHOLD) {
			return;
		}
		Rain::Log::verbose(
			"TouchManager::onTouchMove: UP (",
			position.x,
			", ",
			position.y,
			"), ",
			sequence.contactSizeMax,
			".");
	}
	void TouchManager::onTouchMove(
		InteractSequence &sequence,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		// Suspect palm touch if contact size is too large or event is too short.
		if (
			sequence.contactSizeMax >= InteractSequence::CONTACT_SIZE_THRESHOLD ||
			now - sequence.timeDown <= InteractSequence::IGNORE_SHORT_THRESHOLD) {
			return;
		}
		Rain::Log::verbose(
			"TouchManager::onTouchMove: MOVE (", position.x, ", ", position.y, ").");
		Main::brush = CreateSolidBrush(RGB(0, 255, 0));
	}
}
