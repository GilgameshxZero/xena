#include <painter.hpp>

#include <cassert>

namespace Xena {
	Painter::Painter(std::string const &fileToLoad, HWND hWnd)
			: window{hWnd},
				DP_TO_PX{
					static_cast<long double>(window.getDpi()) / USER_DEFAULT_SCREEN_DPI},
				STROKE_WIDTH_PX{Painter::STROKE_WIDTH_DP * this->DP_TO_PX},
				CHUNK_SIZE_PX{
					std::lroundl(Painter::CHUNK_SIZE_DP.x * this->DP_TO_PX),
					std::lroundl(Painter::CHUNK_SIZE_DP.y * this->DP_TO_PX)},
				size{this->getSizeFromHWnd(hWnd)},
				dc{Rain::Windows::validateSystemCall(GetDC(this->window))},
				tentativeDc{this->dc},
				tentativeBitmap{this->dc, this->size.x, this->size.y},
				drawPen{
					std::lroundl(this->STROKE_WIDTH_PX * Chunk::AA_SCALE),
					this->IS_LIGHT_THEME ? 0x00000000 : 0x00ffffff},
				erasePen{
					std::lroundl(this->STROKE_WIDTH_PX * Chunk::AA_SCALE * 1.5l),
					this->IS_LIGHT_THEME ? 0x00ffffff : 0x00000000},
				tentativeDrawPen{
					std::lroundl(this->STROKE_WIDTH_PX),
					this->IS_LIGHT_THEME ? 0x00000000 : 0x00ffffff},
				svg(fileToLoad, this->viewportPosition, this->paths) {
		this->tentativeDc.select(this->tentativeBitmap);
		this->tentativeDc.select(this->tentativeDrawPen);
		this->tentativeClear();

		std::shared_ptr<Path> path(new Path);
		path->addPoint({100.0f, 100.0f});
		path->addPoint({1300.0f, 200.0f});
		this->addPath(path);
	}
	Painter::~Painter() {
		DeleteDC(this->dc);
	}

	void Painter::rePaint() {
		this->window.invalidateClient();
	}
	LRESULT Painter::onPaint(WPARAM wParam, LPARAM lParam) {
		Rain::Windows::PaintStruct ps(this->window);
		if (!this->isTentativeDirty) {
			PointL chunkBegin{
				Painter::getChunkForPixel(this->viewportPosition.x),
				Painter::getChunkForPixel(this->viewportPosition.y)},
				chunkEnd{
					Painter::getChunkForPixel(
						this->viewportPosition.x + ps.rcPaint().width()),
					Painter::getChunkForPixel(
						this->viewportPosition.y + ps.rcPaint().height())};
			if (chunkBegin.x > chunkEnd.x) {
				std::swap(chunkBegin.x, chunkEnd.x);
			}
			if (chunkBegin.y > chunkEnd.y) {
				std::swap(chunkBegin.y, chunkEnd.y);
			}
			for (long i{chunkBegin.x}; i <= chunkEnd.x; i++) {
				for (long j{chunkBegin.y}; j <= chunkEnd.y; j++) {
					std::shared_ptr<Chunk> &chunk{this->getChunkPair({i, j}).first};
					BitBlt(
						ps.hDc(),
						i * this->CHUNK_SIZE_PX.x - this->viewportPosition.x,
						j * this->CHUNK_SIZE_PX.y - this->viewportPosition.y,
						this->CHUNK_SIZE_PX.x,
						this->CHUNK_SIZE_PX.y,
						chunk->dc,
						0,
						0,
						SRCCOPY);
				}
			}
		} else {
			BitBlt(
				ps.hDc(),
				0,
				0,
				this->size.x,
				this->size.y,
				this->tentativeDc,
				0,
				0,
				this->IS_LIGHT_THEME ? SRCAND : SRCPAINT);
		}
		return 0;
	}

	void Painter::addPath(std::shared_ptr<Path const> const &path) {
		// Adding an existing path ID is not allowed.
		assert(this->paths.count(path->ID) == 0);
		std::unordered_set<PointL> &containingChunks{
			this->paths
				.emplace(path->ID, std::make_pair(path, std::unordered_set<PointL>()))
				.first->second.second};

		// Compute all chunks which contain path.
		std::vector<PointLd> const &points{path->getPoints()};
		for (std::size_t i{1}; i < points.size(); i++) {
			PointL chunkBegin{
				Painter::getChunkForPixel(points[i - 1].x),
				Painter::getChunkForPixel(points[i - 1].y)},
				chunkEnd{
					Painter::getChunkForPixel(points[i].x),
					Painter::getChunkForPixel(points[i].y)};
			if (chunkBegin.x > chunkEnd.x) {
				std::swap(chunkBegin.x, chunkEnd.x);
			}
			if (chunkBegin.y > chunkEnd.y) {
				std::swap(chunkBegin.y, chunkEnd.y);
			}
			for (long j{chunkBegin.x}; j <= chunkEnd.x; j++) {
				for (long k{chunkBegin.y}; k <= chunkEnd.y; k++) {
					containingChunks.emplace(j, k);
				}
			}
		}

		for (auto const &i : containingChunks) {
			auto &chunkPair{this->getChunkPair(i)};
			chunkPair.first->drawPath(path, this->drawPen);
			chunkPair.second.emplace(path->ID);
			Rain::Log::verbose(
				"Painter::addPath: Added path ",
				path->ID,
				" to chunk (",
				i.x,
				", ",
				i.y,
				").");
		}
		this->rePaint();
	}
	void Painter::removePath(std::size_t pathId) {
		auto it{this->paths.find(pathId)};
		assert(it != this->paths.end());
		std::shared_ptr<Path const> &path{it->second.first};
		std::unordered_set<PointL> &containingChunks{it->second.second};
		for (PointL const &coordinate : containingChunks) {
			auto &chunkPair{this->getChunkPair(coordinate)};
			chunkPair.first->drawPath(path, this->erasePen);
			chunkPair.second.erase(pathId);
		}
		this->paths.erase(it);
		this->rePaint();
	}

	void Painter::updateViewportPosition(PointL const &newViewportPosition) {
		this->viewportPosition.x = newViewportPosition.x;
		this->viewportPosition.y = newViewportPosition.y;
	}
	Rain::Algorithm::Geometry::PointL const &Painter::getViewportPosition() {
		return this->viewportPosition;
	}

	void Painter::tentativeClear() {
		RECT rect{0, 0, this->size.x, this->size.y};
		Rain::Windows::validateSystemCall(
			FillRect(this->tentativeDc, &rect, this->backgroundBrush));
		this->isTentativeDirty = false;
	}
	void Painter::tentativeMoveTo(PointL const &point) {
		Rain::Windows::validateSystemCall(
			MoveToEx(this->tentativeDc, point.x, point.y, NULL));
	}
	void Painter::tentativeLineTo(PointL const &point) {
		Rain::Windows::validateSystemCall(
			LineTo(this->tentativeDc, point.x, point.y));
		this->isTentativeDirty = true;
	}

	Rain::Algorithm::Geometry::PointL Painter::getSizeFromHWnd(HWND hWnd) {
		RECT rect;
		Rain::Windows::validateSystemCall(GetWindowRect(hWnd, &rect));
		return {rect.right - rect.left, rect.bottom - rect.top};
	}
	template <typename PrecisionType>
	long Painter::getChunkForPixel(PrecisionType const &pixel) {
		return static_cast<long>(
			std::floorl(static_cast<long double>(pixel) / this->CHUNK_SIZE_PX.x));
	}
	std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>> &
	Painter::getChunkPair(PointL const &coordinate) {
		auto it{this->chunks.find(coordinate)};
		if (it != this->chunks.end()) {
			return it->second;
		}
		Rain::Log::verbose(
			"Painter::getChunkPair: Created chunk (",
			coordinate.x,
			", ",
			coordinate.y,
			").");
		return this->chunks
			.emplace(
				coordinate,
				std::make_pair(
					new Chunk(
						this->dc,
						Painter::CHUNK_SIZE_PX,
						{Painter::CHUNK_SIZE_PX.x * coordinate.x,
						 Painter::CHUNK_SIZE_PX.y * coordinate.y},
						this->backgroundBrush),
					std::unordered_set<std::size_t>{}))
			.first->second;
	}
}
