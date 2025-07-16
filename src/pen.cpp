#include <pen.hpp>

namespace Xena {
	Pen::Pen(Painter &painter)
			: HIMETRIC_TO_PX{painter.DP_TO_PX * USER_DEFAULT_SCREEN_DPI * 0.0003937008f},
				painter(painter) {}

	void Pen::onPenDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose("Pen::onPenDown: (", position.x, ", ", position.y, ").");
		Gdiplus::Point const &viewportPosition{this->painter.getViewportPosition()};
		this->path.reset(new Path);
		this->path->addPoint(
			{viewportPosition.X + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.Y + position.y * this->HIMETRIC_TO_PX});
	}
	void Pen::onPenUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Rain::Log::verbose("Pen::onPenUp: (", position.x, ", ", position.y, ").");
		Gdiplus::Point const &viewportPosition{this->painter.getViewportPosition()};
		this->path->addPoint(
			{viewportPosition.X + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.Y + position.y * this->HIMETRIC_TO_PX});
		this->painter.addPath(this->path);
	}
	void Pen::onPenMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point now,
		POINT position) {
		Gdiplus::Point const &viewportPosition{this->painter.getViewportPosition()};
		this->path->addPoint(
			{viewportPosition.X + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.Y + position.y * this->HIMETRIC_TO_PX});
	}
}
