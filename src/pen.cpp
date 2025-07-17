#include <pen.hpp>

namespace Xena {
	Pen::Pen(Painter &painter) : painter(painter) {}

	void Pen::onPenDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose("Pen::onPenDown: (", position.x, ", ", position.y, ").");
		Gdiplus::Point const &viewportPosition{this->painter.getViewportPosition()};
		this->isDrawing = true;
		this->path.reset(new Path);
		this->path->addPoint(
			{viewportPosition.X + position.x, viewportPosition.Y + position.y});
		this->painter.tentativeMoveTo(
			{static_cast<LONG>(position.x), static_cast<LONG>(position.y)});
	}
	void Pen::onPenUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose("Pen::onPenUp: (", position.x, ", ", position.y, ").");
		Gdiplus::Point const &viewportPosition{this->painter.getViewportPosition()};
		this->isDrawing = false;
		this->path->addPoint(
			{viewportPosition.X + position.x, viewportPosition.Y + position.y});
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
			{viewportPosition.X + position.x, viewportPosition.Y + position.y});
		this->painter.tentativeLineTo(
			{static_cast<LONG>(position.x), static_cast<LONG>(position.y)});
		this->painter.rePaint();
	}
}
