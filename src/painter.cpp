#include <painter.hpp>

#include <cassert>

namespace Xena {
	Painter::Painter(std::string const &fileToLoad, HWND hWnd)
			: DP_TO_PX{static_cast<long double>(Rain::Windows::validateSystemCall(GetDpiForWindow(hWnd))) / USER_DEFAULT_SCREEN_DPI},
				STROKE_WIDTH_PX{Painter::STROKE_WIDTH_DP * this->DP_TO_PX},
				CHUNK_SIZE_PX{
					static_cast<int>(Painter::CHUNK_SIZE_DP.X * this->DP_TO_PX),
					static_cast<int>(Painter::CHUNK_SIZE_DP.Y * this->DP_TO_PX)},
				hWnd{hWnd},
				size{Rain::Windows::getHWndSize(this->hWnd)},
				hDc{Rain::Windows::validateSystemCall(GetDC(this->hWnd))},
				hTentativeDc{
					Rain::Windows::validateSystemCall(CreateCompatibleDC(this->hDc))},
				hTentativeBitmap{
					Rain::Windows::validateSystemCall(CreateCompatibleBitmap(
						this->hTentativeDc,
						this->size.x,
						this->size.y))},
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
		DeleteObject(this->hBackgroundBrush);
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
				{static_cast<Gdiplus::REAL>(this->viewportPosition.X),
				 static_cast<Gdiplus::REAL>(this->viewportPosition.Y)})},
				chunkEnd{this->getChunkCoordinateForPoint(
					{static_cast<Gdiplus::REAL>(this->viewportPosition.X + rcPaint.Width),
					 static_cast<Gdiplus::REAL>(
						 this->viewportPosition.Y + rcPaint.Height)})};
			for (int i{chunkBegin.X}; i <= chunkEnd.X; i++) {
				for (int j{chunkBegin.Y}; j <= chunkEnd.Y; j++) {
					std::shared_ptr<Chunk> &chunk{this->getChunkPair({i, j}).first};
					BitBlt(
						ps.hdc,
						i * this->CHUNK_SIZE_PX.X - this->viewportPosition.X,
						j * this->CHUNK_SIZE_PX.Y - this->viewportPosition.Y,
						this->CHUNK_SIZE_PX.X,
						this->CHUNK_SIZE_PX.Y,
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
		std::unordered_set<Gdiplus::Point> &containingChunks{
			this->paths
				.emplace(
					path->ID, std::make_pair(path, std::unordered_set<Gdiplus::Point>()))
				.first->second.second};

		// Compute all chunks which contain path.
		std::vector<Path::Point> const &points{path->getPoints()};
		for (std::size_t i{1}; i < points.size(); i++) {
			auto chunkBegin{Painter::getChunkCoordinateForPoint(points[i - 1])},
				chunkEnd{Painter::getChunkCoordinateForPoint(points[i])};
			for (int j{chunkBegin.X}; j <= chunkEnd.X; j++) {
				for (int k{chunkBegin.Y}; k <= chunkEnd.Y; k++) {
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
				i.X,
				", ",
				i.Y,
				").");
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
			chunkPair.first->drawPath(path, this->hErasePen);
			chunkPair.second.erase(pathId);
		}
		this->paths.erase(it);
		this->rePaint();
	}

	void Painter::updateViewportPosition(
		Gdiplus::Point const &newViewportPosition) {
		this->viewportPosition.X = newViewportPosition.X;
		this->viewportPosition.Y = newViewportPosition.Y;
	}
	Gdiplus::Point const &Painter::getViewportPosition() {
		return this->viewportPosition;
	}

	void Painter::tentativeClear() {
		RECT rect{0, 0, this->size.x, this->size.y};
		Rain::Windows::validateSystemCall(
			FillRect(this->hTentativeDc, &rect, this->hBackgroundBrush));
		this->isTentativeDirty = false;
	}
	void Painter::tentativeMoveTo(POINT const &point) {
		Rain::Windows::validateSystemCall(
			MoveToEx(this->hTentativeDc, point.x, point.y, NULL));
	}
	void Painter::tentativeLineTo(POINT const &point) {
		Rain::Windows::validateSystemCall(
			LineTo(this->hTentativeDc, point.x, point.y));
		this->isTentativeDirty = true;
	}

	Gdiplus::Point Painter::getChunkCoordinateForPoint(Path::Point const &point) {
		return {
			static_cast<int>(std::floorf(point.first / this->CHUNK_SIZE_PX.X)),
			static_cast<int>(std::floorf(point.second / this->CHUNK_SIZE_PX.Y))};
	}
	std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>> &
	Painter::getChunkPair(Gdiplus::Point const &coordinate) {
		auto it{this->chunks.find(coordinate)};
		if (it != this->chunks.end()) {
			return it->second;
		}
		Rain::Log::verbose(
			"Painter::getChunkPair: Created chunk (",
			coordinate.X,
			", ",
			coordinate.Y,
			").");
		return this->chunks
			.emplace(
				coordinate,
				std::make_pair(
					new Chunk(
						this->hDc,
						Painter::CHUNK_SIZE_PX,
						{Painter::CHUNK_SIZE_PX.X * coordinate.X,
						 Painter::CHUNK_SIZE_PX.Y * coordinate.Y},
						this->hBackgroundBrush),
					std::unordered_set<std::size_t>{}))
			.first->second;
	}
}
