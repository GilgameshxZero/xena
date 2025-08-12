#include <rain.hpp>

#include <eraser.hpp>

namespace Xena {
	Eraser::Eraser(Painter &painter)
			: SHORT_DISTANCE_EPS_PX{Eraser::SHORT_DISTANCE_EPS_DP * painter.DP_TO_PX},
				painter(painter) {}

	void Eraser::onEraserDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		Rain::Console::log("Eraser::onEraserDown: ", position, ".");
		this->previousPoint = position + this->painter.getViewportPosition();
	}
	void Eraser::onEraserUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		Rain::Console::log("Eraser::onEraserUp: ", position, ".");

		this->onEraserMove(interaction, now, position);
	}
	void Eraser::onEraserMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		auto chunkBegin{this->painter.getChunkForPoint(this->previousPoint)},
			chunkEnd{this->painter.getChunkForPoint(
				position + this->painter.getViewportPosition())};
		if (chunkBegin.x > chunkEnd.x) {
			std::swap(chunkBegin.x, chunkEnd.x);
		}
		if (chunkBegin.y > chunkEnd.y) {
			std::swap(chunkBegin.y, chunkEnd.y);
		}
		std::unordered_set<std::size_t> toRemove;
		auto const &paths{this->painter.getPaths()};
		auto const &chunks{this->painter.getChunks()};
		for (auto i{chunkBegin.x}; i <= chunkEnd.x; i++) {
			for (auto j{chunkBegin.y}; j <= chunkEnd.y; j++) {
				auto const &containedPaths{chunks.at({i, j}).second};
				for (auto k : containedPaths) {
					auto const &points{paths.at(k).first->getPoints()};
					for (std::size_t l{1}; l < points.size(); l++) {
						if (Rain::Algorithm::Geometry::LineSegmentLd(
									points[l - 1], points[l])
									.intersects(
										Rain::Algorithm::Geometry::LineSegmentL(
											this->previousPoint,
											position + this->painter.getViewportPosition()))) {
							toRemove.insert(k);
							break;
						}
					}
					for (std::size_t l{0}; l < points.size(); l++) {
						if (
							points[l].distanceTo(this->previousPoint) <
								this->SHORT_DISTANCE_EPS_PX ||
							points[l].distanceTo(
								position + this->painter.getViewportPosition()) <
								this->SHORT_DISTANCE_EPS_PX) {
							toRemove.insert(k);
							break;
						}
					}
				}
			}
		}

		for (auto i : toRemove) {
			this->painter.removePath(i);
		}
		this->previousPoint = position + this->painter.getViewportPosition();
	}
}
