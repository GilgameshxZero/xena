#include <rain.hpp>

#include <pen.hpp>

namespace Xena {
	Pen::Pen(Painter &painter)
			// TODO: Why not exactly 2540?
			: HIMETRIC_TO_PX{painter.DP_TO_PX * 160.0l / 2520.0l},
				painter(painter) {}

	void Pen::onPenDown(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		Rain::Console::log("Pen::onPenDown: ", position, ".");
		auto const &viewportPosition{this->painter.getViewportPosition()};
		this->isDrawing = true;
		this->path.reset(new Path);
		this->path->addPoint(viewportPosition + position * this->HIMETRIC_TO_PX);
		this->painter.tentativeMoveTo(
			(position * this->HIMETRIC_TO_PX).round<long>());
	}
	void Pen::onPenUp(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		Rain::Console::log("Pen::onPenUp: ", position, ".");
		auto const &viewportPosition{this->painter.getViewportPosition()};
		this->isDrawing = false;
		this->path->addPoint(viewportPosition + position * this->HIMETRIC_TO_PX);
		this->painter.addPath(this->path);
		this->painter.tentativeClear();
		this->painter.rePaint();
	}
	void Pen::onPenMove(
		Interaction &interaction,
		std::chrono::steady_clock::time_point const &now,
		PointL const &position) {
		auto const &viewportPosition{this->painter.getViewportPosition()};
		this->path->addPoint(viewportPosition + position * this->HIMETRIC_TO_PX);
		this->painter.tentativeLineTo(
			(position * this->HIMETRIC_TO_PX).round<long>());
		this->painter.rePaint();
	}
}
