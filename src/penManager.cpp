#include <penManager.hpp>

namespace Xena {
	void PenManager::onPenDown(
		InteractSequence &sequence,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"PenManager::onPenMove: DOWN (", position.x, ", ", position.y, ").");
	}
	void PenManager::onPenUp(
		InteractSequence &sequence,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"PenManager::onPenMove: UP (", position.x, ", ", position.y, ").");
	}
	void PenManager::onPenMove(
		InteractSequence &sequence,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"PenManager::onPenMove: MOVE (", position.x, ", ", position.y, ").");
	}
	void PenManager::onEraserDown(
		InteractSequence &sequence,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"PenManager::onEraserDown: DOWN (", position.x, ", ", position.y, ").");
	}
	void PenManager::onEraserUp(
		InteractSequence &sequence,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"PenManager::onEraserUp: UP (", position.x, ", ", position.y, ").");
	}
	void PenManager::onEraserMove(
		InteractSequence &sequence,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose(
			"PenManager::onEraserMove: MOVE (", position.x, ", ", position.y, ").");
	}
}
