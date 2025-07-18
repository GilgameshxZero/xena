#include <painter.hpp>

#include <cassert>

namespace Xena {
	Painter::Painter(std::string const &fileToLoad, HWND hWnd)
			: DP_TO_PX{static_cast<long double>(Rain::Windows::validateSystemCall(GetDpiForWindow(hWnd))) / USER_DEFAULT_SCREEN_DPI},
				STROKE_WIDTH_PX{Painter::STROKE_WIDTH_DP * this->DP_TO_PX},
				CHUNK_SIZE_PX{
					static_cast<int>(Painter::CHUNK_SIZE_DP.x * this->DP_TO_PX),
					static_cast<int>(Painter::CHUNK_SIZE_DP.y * this->DP_TO_PX)},
				hWnd{hWnd},
				size{
					Rain::Windows::getHWndSize(this->hWnd).x,
					Rain::Windows::getHWndSize(this->hWnd).y},
				hDc{Rain::Windows::validateSystemCall(GetDC(this->hWnd))},
				hTentativeDc{
					Rain::Windows::validateSystemCall(CreateCompatibleDC(this->hDc))},
				hTentativeBitmap{Rain::Windows::validateSystemCall(
					CreateCompatibleBitmap(this->hDc, this->size.x, this->size.y))},
				hOrigBitmap{static_cast<HBITMAP>(Rain::Windows::validateSystemCall(
					SelectObject(this->hTentativeDc, this->hTentativeBitmap)))},
				hDrawPen{Rain::Windows::validateSystemCall(CreatePen(
					PS_SOLID,
					this->STROKE_WIDTH_PX * Chunk::AA_SCALE,
					this->IS_LIGHT_THEME ? 0x00000000 : 0x00ffffff))},
				hErasePen{Rain::Windows::validateSystemCall(CreatePen(
					PS_SOLID,
					this->STROKE_WIDTH_PX * Chunk::AA_SCALE * 1.5f,
					this->IS_LIGHT_THEME ? 0x00ffffff : 0x00000000))},
				hTentativeDrawPen{Rain::Windows::validateSystemCall(CreatePen(
					PS_SOLID,
					this->STROKE_WIDTH_PX,
					this->IS_LIGHT_THEME ? 0x00000000 : 0x00ffffff))},
				hOrigPen{static_cast<HPEN>(Rain::Windows::validateSystemCall(
					SelectObject(this->hTentativeDc, this->hTentativeDrawPen)))},
				svg(fileToLoad, this->viewportPosition, this->paths) {
		this->tentativeClear();

		std::shared_ptr<Path> path(new Path);
		path->addPoint({100.0f, 100.0f});
		path->addPoint({1300.0f, 200.0f});
		this->addPath(path);
	}
	Painter::~Painter() {
		SelectObject(this->hTentativeDc, this->hOrigPen);
		SelectObject(this->hTentativeDc, this->hOrigBitmap);
		DeleteObject(this->hTentativeBitmap);
		DeleteObject(this->hTentativeDrawPen);
		DeleteDC(this->hTentativeDc);
		DeleteObject(this->hErasePen);
		DeleteObject(this->hDrawPen);
		DeleteDC(this->hDc);
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
		if (!this->isTentativeDirty) {
			auto chunkBegin{this->getChunkCoordinateForPoint(
				{this->viewportPosition.x, this->viewportPosition.y})},
				chunkEnd{this->getChunkCoordinateForPoint(
					{this->viewportPosition.x + rcPaint.Width,
					 this->viewportPosition.y + rcPaint.Height})};
			for (long long i{chunkBegin.x}; i <= chunkEnd.x; i++) {
				for (long long j{chunkBegin.y}; j <= chunkEnd.y; j++) {
					std::shared_ptr<Chunk> &chunk{this->getChunkPair({i, j}).first};
					BitBlt(
						ps.hdc,
						i * this->CHUNK_SIZE_PX.x - this->viewportPosition.x,
						j * this->CHUNK_SIZE_PX.y - this->viewportPosition.y,
						this->CHUNK_SIZE_PX.x,
						this->CHUNK_SIZE_PX.y,
						chunk->hDc,
						0,
						0,
						SRCCOPY);
				}
			}
		} else {
			BitBlt(
				ps.hdc,
				0,
				0,
				this->size.x,
				this->size.y,
				this->hTentativeDc,
				0,
				0,
				this->IS_LIGHT_THEME ? SRCAND : SRCPAINT);
		}
		EndPaint(hWnd, &ps);
		return 0;
	}

	void Painter::addPath(std::shared_ptr<Path const> const &path) {
		// Adding an existing path ID is not allowed.
		assert(this->paths.count(path->ID) == 0);
		std::unordered_set<PointLl> &containingChunks{
			this->paths
				.emplace(path->ID, std::make_pair(path, std::unordered_set<PointLl>()))
				.first->second.second};

		// Compute all chunks which contain path.
		std::vector<PointLd> const &points{path->getPoints()};
		for (std::size_t i{1}; i < points.size(); i++) {
			auto chunkBegin{Painter::getChunkCoordinateForPoint(
				{static_cast<long long>(points[i - 1].x),
				 static_cast<long long>(points[i - 1].y)})},
				chunkEnd{Painter::getChunkCoordinateForPoint(
					{static_cast<long long>(points[i].x),
					 static_cast<long long>(points[i].y)})};
			for (long long j{chunkBegin.x}; j <= chunkEnd.x; j++) {
				for (long long k{chunkBegin.y}; k <= chunkEnd.y; k++) {
					containingChunks.emplace(j, k);
				}
			}
		}

		for (auto const &i : containingChunks) {
			auto &chunkPair{this->getChunkPair(i)};
			chunkPair.first->drawPath(path, this->hDrawPen);
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
		std::unordered_set<PointLl> &containingChunks{it->second.second};
		for (PointLl const &coordinate : containingChunks) {
			auto &chunkPair{this->getChunkPair(coordinate)};
			chunkPair.first->drawPath(path, this->hErasePen);
			chunkPair.second.erase(pathId);
		}
		this->paths.erase(it);
		this->rePaint();
	}

	void Painter::updateViewportPosition(PointLl const &newViewportPosition) {
		this->viewportPosition.x = newViewportPosition.x;
		this->viewportPosition.y = newViewportPosition.y;
	}
	Rain::Algorithm::Geometry::PointLl const &Painter::getViewportPosition() {
		return this->viewportPosition;
	}

	void Painter::tentativeClear() {
		RECT rect{0, 0, this->size.x, this->size.y};
		Rain::Windows::validateSystemCall(
			FillRect(this->hTentativeDc, &rect, this->backgroundBrush));
		this->isTentativeDirty = false;
	}
	void Painter::tentativeMoveTo(PointLd const &point) {
		Rain::Windows::validateSystemCall(
			MoveToEx(this->hTentativeDc, point.x, point.y, NULL));
	}
	void Painter::tentativeLineTo(PointLd const &point) {
		Rain::Windows::validateSystemCall(
			LineTo(this->hTentativeDc, point.x, point.y));
		this->isTentativeDirty = true;
	}

	Rain::Algorithm::Geometry::PointLl Painter::getChunkCoordinateForPoint(
		PointLl const &point) {
		return {
			static_cast<int>(std::floorf(point.x / this->CHUNK_SIZE_PX.x)),
			static_cast<int>(std::floorf(point.y / this->CHUNK_SIZE_PX.y))};
	}
	std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>> &
	Painter::getChunkPair(PointLl const &coordinate) {
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
						this->hDc,
						Painter::CHUNK_SIZE_PX,
						{Painter::CHUNK_SIZE_PX.x * coordinate.x,
						 Painter::CHUNK_SIZE_PX.y * coordinate.y},
						this->backgroundBrush),
					std::unordered_set<std::size_t>{}))
			.first->second;
	}
}
