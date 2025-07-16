#include <painter.hpp>

#include <cassert>

namespace Xena {
	Painter::Painter(std::string const &fileToLoad, HWND hWnd)
			: DPI_SCALE{static_cast<Gdiplus::REAL>(Rain::Windows::validateSystemCall(GetDpiForWindow(hWnd))) / USER_DEFAULT_SCREEN_DPI},
				STROKE_WIDTH_PX{Painter::STROKE_WIDTH_DP * this->DPI_SCALE},
				CHUNK_SIZE_PX{
					static_cast<int>(Painter::CHUNK_SIZE_DP.X * this->DPI_SCALE),
					static_cast<int>(Painter::CHUNK_SIZE_DP.Y * this->DPI_SCALE)},
				hWnd{hWnd},
				blackPen{Gdiplus::Color(0xff000000), this->STROKE_WIDTH_PX},
				whitePen{Gdiplus::Color(0xffffffff), this->STROKE_WIDTH_PX},
				transparentPen{
					Gdiplus::Color(0x00000000),
					this->STROKE_WIDTH_PX * 1.5f},
				svg(fileToLoad, this->viewportPosition, this->paths) {
		this->blackPen.SetStartCap(Gdiplus::LineCap::LineCapRound);
		this->whitePen.SetStartCap(Gdiplus::LineCap::LineCapRound);
		this->transparentPen.SetStartCap(Gdiplus::LineCap::LineCapRound);
		this->blackPen.SetEndCap(Gdiplus::LineCap::LineCapRound);
		this->whitePen.SetEndCap(Gdiplus::LineCap::LineCapRound);
		this->transparentPen.SetEndCap(Gdiplus::LineCap::LineCapRound);

		std::shared_ptr<Path> path(new Path);
		path->addPoint({100.0f, 100.0f});
		path->addPoint({1300.0f, 200.0f});
		this->addPath(path);
	}

	void Painter::rePaint() {
		Rain::Windows::validateSystemCall(InvalidateRect(this->hWnd, NULL, FALSE));
	}
	LRESULT Painter::onPaint(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		static PAINTSTRUCT ps;
		Rain::Windows::validateSystemCall(BeginPaint(hWnd, &ps));
		Gdiplus::Graphics graphics(ps.hdc);
		Gdiplus::Rect rcPaint(
			ps.rcPaint.left,
			ps.rcPaint.top,
			ps.rcPaint.right - ps.rcPaint.left,
			ps.rcPaint.bottom - ps.rcPaint.top);

		Rain::Windows::Gdiplus::validateGdiplusCall(graphics.FillRectangle(
			this->IS_LIGHT_THEME ? &this->GDIPLUS_WHITE_BRUSH
													 : &this->GDIPLUS_BLACK_BRUSH,
			rcPaint));
		auto chunkBegin{this->getChunkCoordinateForPoint(
			{static_cast<Gdiplus::REAL>(this->viewportPosition.X),
			 static_cast<Gdiplus::REAL>(this->viewportPosition.Y)})},
			chunkEnd{this->getChunkCoordinateForPoint(
				{static_cast<Gdiplus::REAL>(this->viewportPosition.X + rcPaint.Width),
				 static_cast<Gdiplus::REAL>(
					 this->viewportPosition.Y + rcPaint.Height)})};
		for (int i{chunkBegin.X}; i <= chunkEnd.X; i++) {
			for (int j{chunkBegin.Y}; j <= chunkEnd.Y; j++) {
				std::shared_ptr<Chunk> &chunk{this->getChunkPair({i, j}).first};
				Rain::Windows::Gdiplus::validateGdiplusCall(graphics.DrawImage(
					&chunk->bitmap,
					Gdiplus::Rect{
						i * this->CHUNK_SIZE_PX.X - this->viewportPosition.X,
						j * this->CHUNK_SIZE_PX.Y - this->viewportPosition.Y,
						this->CHUNK_SIZE_PX.X,
						this->CHUNK_SIZE_PX.Y}));
			}
		}

		EndPaint(hWnd, &ps);
		Rain::Log::verbose("Painter::onPaint.");
		return 0;
	}

	void Painter::addPath(std::shared_ptr<Path const> const &path) {
		// Adding an existing path ID is not allowed.
		assert(this->paths.count(path->ID) == 0);
		std::unordered_set<Gdiplus::Point> &containingChunks{
			this->paths
				.emplace(
					path->ID, std::make_pair(path, std::unordered_set<Gdiplus::Point>()))
				.first->second.second};

		// Compute all chunks which contain path.
		std::vector<Gdiplus::PointF> const &pointFs{path->getPointFs()};
		for (std::size_t i{1}; i < pointFs.size(); i++) {
			auto chunkBegin{Painter::getChunkCoordinateForPoint(pointFs[i - 1])},
				chunkEnd{Painter::getChunkCoordinateForPoint(pointFs[i])};
			for (int j{chunkBegin.X}; j <= chunkEnd.X; j++) {
				for (int k{chunkBegin.Y}; k <= chunkEnd.Y; k++) {
					containingChunks.emplace(j, k);
					auto &chunkPair{this->getChunkPair({j, k})};
					chunkPair.first->drawPath(
						path, this->IS_LIGHT_THEME ? this->blackPen : this->whitePen);
					chunkPair.second.emplace(path->ID);
					Rain::Log::verbose(
						"Painter::addPath: Added path ",
						path->ID,
						" to chunk (",
						j,
						", ",
						k,
						").");
				}
			}
		}
		this->rePaint();
	}
	void Painter::removePath(std::size_t pathId) {
		auto it{this->paths.find(pathId)};
		assert(it != this->paths.end());
		std::shared_ptr<Path const> &path{it->second.first};
		std::unordered_set<Gdiplus::Point> &containingChunks{it->second.second};
		for (Gdiplus::Point const &coordinate : containingChunks) {
			auto &chunkPair{this->getChunkPair(coordinate)};
			chunkPair.first->drawPath(path, this->transparentPen);
			chunkPair.second.erase(pathId);
		}
		this->paths.erase(it);
		this->rePaint();
	}

	Gdiplus::Point Painter::getChunkCoordinateForPoint(
		Gdiplus::PointF const &pointF) {
		return {
			static_cast<int>(std::floorf(pointF.X / this->CHUNK_SIZE_PX.X)),
			static_cast<int>(std::floorf(pointF.Y / this->CHUNK_SIZE_PX.Y))};
	}
	std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>> &
	Painter::getChunkPair(Gdiplus::Point const &coordinate) {
		auto it{this->chunks.find(coordinate)};
		if (it != this->chunks.end()) {
			return it->second;
		}
		return this->chunks
			.emplace(
				coordinate,
				std::make_pair(
					new Chunk(
						Painter::CHUNK_SIZE_PX,
						{Painter::CHUNK_SIZE_PX.X * coordinate.X,
						 Painter::CHUNK_SIZE_PX.Y * coordinate.Y},
						this->GDIPLUS_TRANSPARENT_BRUSH),
					std::unordered_set<std::size_t>{}))
			.first->second;
	}
}
