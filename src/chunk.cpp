#include <chunk.hpp>

namespace Xena {
	Chunk::Chunk(
		HDC hDc,
		PointL const &size,
		PointL const &position,
		Rain::Windows::SolidBrush const &brush)
			: SIZE{size},
				POSITION{position},
				bitmap{hDc, size.x, size.y},
				bitmapAa{hDc, size.x * Chunk::AA_SCALE, size.y * Chunk::AA_SCALE},
				dc{hDc},
				dcAa{hDc} {
		Rain::Windows::validateSystemCall(SetStretchBltMode(this->dc, HALFTONE));

		this->dc.select(this->bitmap);
		this->dcAa.select(this->bitmapAa);

		this->dc.fillRect({{0, 0}, size}, brush);
		this->dcAa.fillRect(
			{0, 0, size.x * Chunk::AA_SCALE, size.y * Chunk::AA_SCALE}, brush);
	}

	void Chunk::drawPath(
		std::shared_ptr<Path const> const &path,
		Rain::Windows::SolidPen const &pen) {
		using Rain::Windows::validateSystemCall;
		auto const &points{path->getPoints()};

		this->dcAa.moveTo(
			{std::lroundl((points[0].x - this->POSITION.x) * Chunk::AA_SCALE),
			 std::lroundl((points[0].y - this->POSITION.y) * Chunk::AA_SCALE)});
		this->dcAa.select(pen);
		for (std::size_t i{1}; i < points.size(); i++) {
			this->dcAa.lineTo(
				{std::lroundl((points[i].x - this->POSITION.x) * Chunk::AA_SCALE),
				 std::lroundl((points[i].y - this->POSITION.y) * Chunk::AA_SCALE)});
		}
		this->dcAa.deselect();

		validateSystemCall(StretchBlt(
			this->dc,
			0,
			0,
			this->SIZE.x,
			this->SIZE.y,
			this->dcAa,
			0,
			0,
			this->SIZE.x * Chunk::AA_SCALE,
			this->SIZE.y * Chunk::AA_SCALE,
			SRCCOPY));
	}
}
