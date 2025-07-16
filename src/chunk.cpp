#include <chunk.hpp>

namespace Xena {
	Chunk::Chunk(
		Gdiplus::Point const &size,
		Gdiplus::Point const &position,
		Gdiplus::Brush const &backgroundBrush)
			: SIZE{size},
				POSITION{position},
				bitmap(size.X, size.Y),
				graphics(&this->bitmap) {
		Rain::Windows::Gdiplus::validateGdiplusCall(
			this->graphics.SetSmoothingMode(Gdiplus::SmoothingModeAntiAlias));
		Rain::Windows::Gdiplus::validateGdiplusCall(this->graphics.FillRectangle(
			&backgroundBrush, Gdiplus::Rect(0, 0, size.X, size.Y)));
	}

	void Chunk::drawPath(
		std::shared_ptr<Path const> const &path,
		Gdiplus::Pen const &pen) {
		std::vector<Gdiplus::PointF> const &pointFs{path->getPointFs()};
		Gdiplus::GraphicsPath graphicsPath;

		for (std::size_t i{1}; i < pointFs.size(); i++) {
			Rain::Windows::Gdiplus::validateGdiplusCall(graphicsPath.AddLine(
				pointFs[i - 1].X - this->POSITION.X,
				pointFs[i - 1].Y - this->POSITION.Y,
				pointFs[i].X - this->POSITION.X,
				pointFs[i].Y - this->POSITION.Y));
		}
		Rain::Windows::Gdiplus::validateGdiplusCall(
			this->graphics.DrawPath(&pen, &graphicsPath));
	}
}
