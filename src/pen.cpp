#include <pen.hpp>

namespace Xena {
	Pen::Pen(Painter &painter)
			// TODO: Why 2520 instead of 2540?
			: HIMETRIC_TO_PX{painter.DP_TO_PX * USER_DEFAULT_SCREEN_DPI / 2520},
				painter(painter) {}

	void Pen::onPenDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose("Pen::onPenDown: (", position.x, ", ", position.y, ").");
		auto const &viewportPosition{this->painter.getViewportPosition()};
		this->isDrawing = true;
		this->path.reset(new Path);
		this->path->addPoint(
			{viewportPosition.x + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.y + position.y * this->HIMETRIC_TO_PX});
		this->painter.tentativeMoveTo(
			{position.x * this->HIMETRIC_TO_PX, position.y * this->HIMETRIC_TO_PX});
	}
	void Pen::onPenUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose("Pen::onPenUp: (", position.x, ", ", position.y, ").");
		auto const &viewportPosition{this->painter.getViewportPosition()};
		this->isDrawing = false;
		this->path->addPoint(
			{viewportPosition.x + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.y + position.y * this->HIMETRIC_TO_PX});
		this->painter.addPath(this->path);
		this->painter.tentativeClear();
		this->painter.rePaint();
	}
	void Pen::onPenMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		auto const &viewportPosition{this->painter.getViewportPosition()};
		this->path->addPoint(
			{viewportPosition.x + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.y + position.y * this->HIMETRIC_TO_PX});
		this->painter.tentativeLineTo(
			{position.x * this->HIMETRIC_TO_PX, position.y * this->HIMETRIC_TO_PX});
		this->painter.rePaint();
	}
}
