#include <pen.hpp>

namespace Xena {
	Pen::Pen(Painter &painter)
			// TODO: Some devices do not have perfect 2540 here.
			: HIMETRIC_TO_PX{painter.DP_TO_PX * USER_DEFAULT_SCREEN_DPI / 2540},
				painter(painter) {}

	void Pen::onPenDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		Rain::Log::verbose("Pen::onPenDown: (", position.x, ", ", position.y, ").");
		auto const &viewportPosition{this->painter.getViewportPosition()};
		this->isDrawing = true;
		this->path.reset(new Path);
		this->path->addPoint(
			{viewportPosition.x + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.y + position.y * this->HIMETRIC_TO_PX});
		this->painter.tentativeMoveTo(
			{std::lroundl(position.x * this->HIMETRIC_TO_PX),
			 std::lroundl(position.y * this->HIMETRIC_TO_PX)});
	}
	void Pen::onPenUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
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
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		auto const &viewportPosition{this->painter.getViewportPosition()};
		this->path->addPoint(
			{viewportPosition.x + position.x * this->HIMETRIC_TO_PX,
			 viewportPosition.y + position.y * this->HIMETRIC_TO_PX});
		this->painter.tentativeLineTo(
			{std::lroundl(position.x * this->HIMETRIC_TO_PX),
			 std::lroundl(position.y * this->HIMETRIC_TO_PX)});
		this->painter.rePaint();
	}
}
