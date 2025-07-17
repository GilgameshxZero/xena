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
		Gdiplus::Point const &viewportPosition{this->painter.getViewportPosition()};
		this->isDrawing = true;
		this->path.reset(new Path);
		this->path->addPoint(
			{viewportPosition.X + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.Y + position.y * this->HIMETRIC_TO_PX});
		this->painter.tentativeMoveTo(
			{static_cast<LONG>(position.x * this->HIMETRIC_TO_PX),
			 static_cast<LONG>(position.y * this->HIMETRIC_TO_PX)});
	}
	void Pen::onPenUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose("Pen::onPenUp: (", position.x, ", ", position.y, ").");
		Gdiplus::Point const &viewportPosition{this->painter.getViewportPosition()};
		this->isDrawing = false;
		this->path->addPoint(
			{viewportPosition.X + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.Y + position.y * this->HIMETRIC_TO_PX});
		this->painter.addPath(this->path);
		this->painter.tentativeClear();
		this->painter.rePaint();
	}
	void Pen::onPenMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Gdiplus::Point const &viewportPosition{this->painter.getViewportPosition()};
		this->path->addPoint(
			{viewportPosition.X + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.Y + position.y * this->HIMETRIC_TO_PX});
		this->painter.tentativeLineTo(
			{static_cast<LONG>(position.x * this->HIMETRIC_TO_PX),
			 static_cast<LONG>(position.y * this->HIMETRIC_TO_PX)});
		this->painter.rePaint();
	}
}
